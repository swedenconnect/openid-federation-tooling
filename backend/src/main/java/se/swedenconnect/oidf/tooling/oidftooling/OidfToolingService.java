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
package se.swedenconnect.oidf.tooling.oidftooling;

import com.nimbusds.openid.connect.sdk.federation.api.ResolveSuccessResponse;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityType;
import se.swedenconnect.oidf.tooling.config.OidfToolingProperties;
import se.swedenconnect.oidf.tooling.domain.JWTDecoded;
import se.swedenconnect.oidf.tooling.integration.OidfServiceIntegration;
import se.swedenconnect.oidf.tooling.oidftooling.tree.Graph;
import se.swedenconnect.oidf.tooling.oidftooling.tree.GraphBuilder;

import java.util.List;
import java.util.Optional;

/**
 * Implements tooling operations
 *
 * @author Per Fredrik Plars
 */
public class OidfToolingService {

  final OidfServiceIntegration integration;
  final OidfToolingProperties oidfTooling;
  final GraphBuilder graphBuilder;

  /**
   * Constructs a new {@code OidfToolingService} instance.
   *
   * @param integration an instance of {@link OidfServiceIntegration} used for service integration purposes.
   * @param oidfTooling the configuration properties for OIDF tooling, represented as
   *     {@link OidfToolingProperties}.
   */
  public OidfToolingService(final OidfServiceIntegration integration,
      final OidfToolingProperties oidfTooling) {
    this.integration = integration;
    this.oidfTooling = oidfTooling;
    this.graphBuilder = new GraphBuilder(integration);
  }

  /**
   * Retrieves and decodes a JWT response from the resolver endpoint for the given subject.
   *
   * @param subject the {@link EntityID} that represents the unique identifier of the subject to be resolved by the
   *     path resolver.
   * @return an instance of {@link JWTDecoded} containing the decoded JWT from the resolver response, including its
   *     header, payload, and signature.
   */
  public JWTDecoded getResolverResponse(final EntityID subject) {
    final ResolveSuccessResponse resolverResponse = this.integration.resolve(this.oidfTooling.getResolverUri(),
        this.oidfTooling.getTrustAnchorEntityId(), subject);

    return JWTDecoded.builder()
        .header(resolverResponse.getResolveStatement().getSignedStatement().getHeader().toJSONObject())
        .signature(resolverResponse.getResolveStatement().getSignedStatement().getSignature().toString())
        .payload(resolverResponse.getResolveStatement().getClaimsSet().toJSONObject())
        .build();
  }

  /**
   * Retrieves a list of discovered entity IDs based on the given optional entity type and list of trustmark IDs. This
   * method invokes the discovery endpoint URI using the integration service to fetch the results.
   *
   * @param entityType an {@link Optional} containing the {@link EntityType} to filter the discovery process; if
   *     empty, all entity types will be considered.
   * @param trustmarkids a {@link List} of {@link EntityID} representing the trustmarks used for discovery
   *     filtering.
   * @return a {@link List} of {@link EntityID} containing the discovered entities matching the provided criteria.
   */
  public List<EntityID> getDiscoverResponse(final Optional<EntityType> entityType, final List<EntityID> trustmarkids) {
    return this.integration.discover(this.oidfTooling.getDiscoveryUri(),
        this.oidfTooling.getTrustAnchorEntityId(), entityType, trustmarkids);

  }

  /**
   * Will traverse the federation and return it in a format of nodes and edges.
   *
   * @return Graph
   */
  public Graph getNodeStructure() {
    this.graphBuilder.start(this.oidfTooling.getTrustAnchorEntityId());
    return this.graphBuilder.getGraph();

  }

}
