/*
 * Copyright 2025 Sweden Connect
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package se.swedenconnect.oidf.tooling.oidftooling.tree;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityType;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;
import se.swedenconnect.oidf.tooling.integration.OidfServiceIntegration;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Building av tree of the federation
 *
 * @author Per Fredrik Plars
 */
@Slf4j
public class GraphBuilder {

  final OidfServiceIntegration oidfServiceIntegration;

  /** The graph currently served by {@link #getGraph()} - always readable without blocking. */
  private final AtomicReference<Snapshot> snapshot = new AtomicReference<>(Snapshot.EMPTY);

  /** Guards against starting more than one background refresh at a time. */
  private final AtomicBoolean refreshing = new AtomicBoolean(false);

  /**
   * Constructs a GraphBuilder that facilitates the creation and management of a graph representing relationships
   * between entities in an OpenID Connect Federation.
   *
   * @param oidfServiceIntegration an instance of OidfServiceIntegration to manage interactions with entity
   *     configurations and federation listings.
   */
  public GraphBuilder(final OidfServiceIntegration oidfServiceIntegration) {
    this.oidfServiceIntegration = oidfServiceIntegration;
  }

  /**
   * Triggers a walk of the federation if the currently served graph is missing or stale (older than 5 minutes). The
   * walk runs on a background virtual thread and never blocks the caller; {@link #getGraph()} keeps returning the
   * previous snapshot - even if the refresh fails - until a new one is ready. At most one refresh runs at a time; a
   * call made while a refresh is already in progress is a no-op.
   *
   * @param trustAnchor Startingpoint
   */
  public void start(final EntityID trustAnchor) {
    if (this.snapshot.get().isFresh()) {
      return;
    }
    if (!this.refreshing.compareAndSet(false, true)) {
      // A refresh is already in flight - the current snapshot keeps being served until it completes.
      return;
    }
    Thread.ofVirtual().name("graph-builder-refresh").start(() -> {
      try {
        this.refresh(trustAnchor);
      }
      catch (final Exception e) {
        log.warn("Failed to refresh federation graph for trust anchor {}", trustAnchor, e);
      }
      finally {
        this.refreshing.set(false);
      }
    });
  }

  private void refresh(final EntityID trustAnchor) {
    final Map<EntityID, EntityStatement> entityStatements = new HashMap<>();
    final List<Edges> edges = new ArrayList<>();

    final EntityStatement entityStatement = this.oidfServiceIntegration.entitConfiguration(trustAnchor);
    this.newNode(entityStatements, null, entityStatement);
    this.recursive(entityStatements, edges, entityStatement);

    this.snapshot.set(new Snapshot(Map.copyOf(entityStatements), List.copyOf(edges), LocalDateTime.now()));
  }

  private void recursive(final Map<EntityID, EntityStatement> entityStatements, final List<Edges> edges,
      final EntityStatement entityStatementParent) {
    if (entityStatementParent == null) {
      return;
    }
    final JSONObject fedMetadata = entityStatementParent.getClaimsSet().getMetadata(EntityType.FEDERATION_ENTITY);
    if (fedMetadata != null) {
      Optional.ofNullable(fedMetadata.get("federation_list_endpoint"))
          .map(String::valueOf)
          .map(URI::create)
          .map(this.oidfServiceIntegration::federationListing)
          .stream()
          .flatMap(List::stream)
          .forEach(entityID -> {
            Optional.ofNullable(fedMetadata.get("federation_fetch_endpoint"))
                .map(String::valueOf)
                .map(URI::create)
                .ifPresent(uri -> {
                  final EntityStatement entityStatement = this.oidfServiceIntegration.entityStatement(uri, entityID);

                  try {
                    final EntityStatement entityConfiguration = this.oidfServiceIntegration.entitConfiguration(
                        entityStatement.getClaimsSet().getSubjectEntityID());
                    //todo verify signature on edge pointing to entitystatement
                    this.newNode(entityStatements, entityStatementParent, entityConfiguration);
                    this.newEdge(edges, entityStatementParent, entityStatement);
                    this.recursive(entityStatements, edges, entityConfiguration);
                  }
                  catch (final Exception e) {
                    this.newEdge(edges, entityStatementParent, entityStatement, e);
                    log.info("Error fetching entity statement for entity {}", entityID, e);
                  }

                });
          });
    }
  }

  private void newEdge(final List<Edges> edges, final EntityStatement parent, final EntityStatement entityStatement) {
    this.newEdge(edges, parent, entityStatement, null);
  }

  private void newEdge(final List<Edges> edges, final EntityStatement parent, final EntityStatement entityStatement,
      final Exception e) {
    edges.add(new Edges(parent == null ? null : parent.getEntityID(), entityStatement.getEntityID(), entityStatement,
        e));
  }

  /**
   * Looks up the subordinate statement a parent entity issued about a specific child, as discovered during the last
   * federation walk. This is the statement fetched from the parent's {@code federation_fetch_endpoint} - not the
   * child's own entity configuration.
   *
   * @param parent the parent (issuer) entity id
   * @param child the child (subject) entity id
   * @return the subordinate statement for that edge, if the edge exists in the currently served graph
   */
  public Optional<EntityStatement> getSubordinateStatement(final EntityID parent, final EntityID child) {
    return this.snapshot.get().edges().stream()
        .filter(edge -> Objects.equals(edge.parent(), parent) && Objects.equals(edge.child(), child))
        .map(Edges::subordinateStatement)
        .findFirst();
  }

  private void newNode(final Map<EntityID, EntityStatement> entityStatements, final EntityStatement parent,
      final EntityStatement entityStatement) {
    entityStatements.put(entityStatement.getEntityID(), entityStatement);
    log.info("Parent: {} Child: {}", parent == null ? null : parent.getEntityID(), entityStatement.getEntityID());
  }

  /**
   * Getting the graph representation
   *
   * @return Graph with nodes and edges.
   */
  public Graph getGraph() {
    final Snapshot current = this.snapshot.get();
    final Graph graph = new Graph();
    current.entityStatements().forEach((k, v) ->
    {
      final String label = Optional.ofNullable(k.getValue()).map(s -> s.replace("https://", ""))
          .orElse("<NoName>");
      graph.addNode(new Graph.Node(k.getValue(), label));
    });

    current.edges().forEach(edge ->
        graph.addEdge(new Graph.Edge(edge.parent().toString(), edge.child().toString())));
    return graph;

  }

  /**
   * Represents a connection between two entities in a graph structure
   *
   * @param parent the identifier of the parent entity in the edge relationship
   * @param child the identifier of the child entity in the edge relationship
   * @param subordinateStatement the subordinate statement the parent issued about the child, fetched from the
   *     parent's {@code federation_fetch_endpoint}
   * @param exception an exception associated with the edge, if applicable
   */
  public record Edges(EntityID parent, EntityID child, EntityStatement subordinateStatement, Exception exception) {
  }

  /**
   * Immutable, point-in-time result of a federation walk. Swapped into {@link GraphBuilder#snapshot} only once a
   * refresh completes successfully, so concurrent readers never observe a partially rebuilt graph.
   *
   * @param entityStatements all entities discovered in the walk, keyed by entity id
   * @param edges the parent/child relationships discovered in the walk
   * @param updated when this snapshot was built, or {@code null} for {@link #EMPTY}
   */
  private record Snapshot(Map<EntityID, EntityStatement> entityStatements, List<Edges> edges,
      LocalDateTime updated) {

    private static final Snapshot EMPTY = new Snapshot(Map.of(), List.of(), null);

    boolean isFresh() {
      return !this.edges.isEmpty() && this.updated != null
          && this.updated.isAfter(LocalDateTime.now().minusMinutes(1));
    }
  }

}
