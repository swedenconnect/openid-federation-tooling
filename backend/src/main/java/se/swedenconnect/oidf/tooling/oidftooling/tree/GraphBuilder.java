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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Building av tree of the federation
 *
 * @author Per Fredrik Plars
 */
@Slf4j
public class GraphBuilder {

  final Map<EntityID, EntityStatement> entityStatements = new ConcurrentHashMap<>();
  final List<Edges> edges = new ArrayList<>();
  final OidfServiceIntegration oidfServiceIntegration;
  LocalDateTime updated;

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
   * Start walking the federation
   *
   * @param trustAnchor Startingpoint
   */
  public synchronized void start(final EntityID trustAnchor) {
    if (!this.edges.isEmpty() && this.updated.isAfter(LocalDateTime.now().minusMinutes(5))) {
      return;
    }
    this.entityStatements.clear();
    this.edges.clear();
    final EntityStatement entityStatement = this.oidfServiceIntegration.entitConfiguration(trustAnchor);
    this.newNode(null, entityStatement);
    this.recursive(entityStatement);
    this.updated = LocalDateTime.now();
  }

  private void recursive(final EntityStatement entityStatementParent) {
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
                    this.newNode(entityStatementParent, entityConfiguration);
                    this.newEdge(entityStatementParent, entityStatement);
                    this.recursive(entityConfiguration);
                  }
                  catch (final Exception e) {
                    this.newEdge(entityStatementParent, entityStatement, e);
                    log.info("Error fetching entity statement for entity {}", entityID, e);
                  }

                });
          });
    }
  }

  private void newEdge(final EntityStatement parent, final EntityStatement entityStatement) {
    this.newEdge(parent, entityStatement, null);
  }

  private void newEdge(final EntityStatement parent, final EntityStatement entityStatement, final Exception e) {
    this.edges.add(new Edges(parent == null ? null : parent.getEntityID(), entityStatement.getEntityID(), e));
  }

  private void newNode(final EntityStatement parent, final EntityStatement entityStatement) {
    this.entityStatements.put(entityStatement.getEntityID(), entityStatement);
    log.info("Parent: {} Child: {}", parent == null ? null : parent.getEntityID(), entityStatement.getEntityID());
  }

  /**
   * Getting the graph representation
   *
   * @return Graph with nodes and edges.
   */
  public Graph getGraph() {
    final Graph graph = new Graph();
    this.entityStatements.forEach((k, v) ->
    {
      final String label = Optional.ofNullable(k.getValue()).map(s -> s.replace("https://", ""))
          .orElse("<NoName>");
      graph.addNode(new Graph.Node(k.getValue(), label));
    });

    this.edges.forEach(edges1 ->
        graph.addEdge(new Graph.Edge(edges1.parent.toString(), edges1.child.toString())));
    return graph;

  }

  /**
   * Represents a connection between two entities in a graph structure
   *
   * @param parent the identifier of the parent entity in the edge relationship
   * @param child the identifier of the child entity in the edge relationship
   * @param exception an exception associated with the edge, if applicable
   */
  public record Edges(EntityID parent, EntityID child, Exception exception) {
  }

}
