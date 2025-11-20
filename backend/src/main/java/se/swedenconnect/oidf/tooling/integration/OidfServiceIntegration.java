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
package se.swedenconnect.oidf.tooling.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.id.Identifier;
import com.nimbusds.oauth2.sdk.id.Subject;
import com.nimbusds.openid.connect.sdk.federation.api.ResolveStatement;
import com.nimbusds.openid.connect.sdk.federation.api.ResolveSuccessResponse;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityType;
import com.nimbusds.openid.connect.sdk.federation.trust.marks.TrustMarkEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * OIDF Service Integration
 *
 * @author Per Fredrik Plars
 */
@Slf4j
@Service
public class OidfServiceIntegration {

  private final RestClient restClient;
  private final ObjectMapper mapper = new ObjectMapper();

  /**
   * Constructs a new {@code OidfServiceIntegration} instance.
   *
   * @param restClient the REST client used to interact with external services
   */
  public OidfServiceIntegration(final RestClient restClient) {
    this.restClient = restClient;

  }

  /**
   * Retrieves a trust mark represented as a signed JWT from the specified trust mark issuer.
   *
   * @param trustMarkIsssuerUri the URI of the trust mark issuer endpoint
   * @param trustMarkId the unique identifier of the trust mark to be retrieved
   * @param sub the subject for which the trust mark is being fetched
   * @return a {@code TrustMarkEntry} containing the trust mark identifier and its signed JWT
   * @throws RuntimeException if there is an error parsing the JWT from the response
   */
  public TrustMarkEntry getTrustMark(final URI trustMarkIsssuerUri, final Identifier trustMarkId, final Subject sub) {
    final URI uri = UriComponentsBuilder.fromUri(trustMarkIsssuerUri)
        .queryParam("trust_mark_id", trustMarkId.getValue())
        .queryParam("sub", sub.getValue())
        .build()
        .toUri();
    log.debug("Calling trustmark endpoint: {}", uri);
    final ResponseEntity<String> response = this.restClient.get()
        .uri(uri)
        .retrieve()
        .toEntity(String.class);

    try {
      final SignedJWT trustmarkJot = SignedJWT.parse(Objects.requireNonNull(response.getBody()));
      return new TrustMarkEntry(trustMarkId, trustmarkJot);
    }
    catch (final ParseException e) {
      throw new RuntimeException(e);
    }

  }

  /**
   * Discovers a list of entity identifiers based on the provided discovery URI, trust anchor, optional entity type, and
   * trustmarks. This method constructs a query to a discovery endpoint, retrieves the data, and returns it as a list of
   * entity identifiers.
   *
   * @param discoverUri the URI of the discovery endpoint to query
   * @param trustAnchor the identifier of the trust anchor to be used in the discovery process
   * @param entityType the optional entity type to filter the results by
   * @param trustmarks a list of trustmark identifiers to filter the results by
   * @return a list of entity identifiers returned by the discovery endpoint
   */
  public List<EntityID> discover(final URI discoverUri,
      final EntityID trustAnchor,
      final Optional<EntityType> entityType,
      final List<EntityID> trustmarks) {

    final UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUri(discoverUri);
    entityType.ifPresent(et -> uriBuilder.queryParam("entity_type", et.getValue()));

    uriBuilder.queryParam("trust_anchor", trustAnchor.getValue());
    if (trustmarks != null && !trustmarks.isEmpty()) {
      uriBuilder.queryParam("trust_marks", trustmarks.stream().map(EntityID::getValue)
          .reduce((a, b) -> a + "," + b));
    }
    final URI uri = uriBuilder.build().toUri();
    log.debug("Calling discovery endpoint: {}", uri);
    final ResponseEntity<List<EntityID>> response = this.restClient.get()
        .uri(uri)
        .retrieve()
        .toEntity(new ParameterizedTypeReference<List<EntityID>>() {});
    return response.getBody().stream().sorted().toList();
  }

  /**
   * Resolves an OpenID Provider entity by making an HTTP request to a specified URI with query parameters and processes
   * the response to retrieve the resolution result.
   *
   * @param resolveUri the base URI to which the resolve request will be made
   * @param trustAnchor the identifier of the trust anchor to be queried
   * @param entityId the identifier of the entity to be resolved
   * @return a ResolverResult object containing the details of the resolved entity
   * @throws RuntimeException if an error occurs during processing of the JWT or JSON payload
   */
  public ResolveSuccessResponse resolve(final URI resolveUri, final EntityID trustAnchor, final EntityID entityId) {
    final UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUri(resolveUri)
        .queryParam("type", EntityType.OPENID_PROVIDER.getValue())
        .queryParam("sub", entityId.getValue())
        .queryParam("trust_anchor", trustAnchor.getValue());

    final URI uri = uriBuilder.build().toUri();
    log.debug("Calling resolve endpoint: {}", uri);
    try {
      final ResponseEntity<String> response = this.restClient.get()
          .uri(uri)
          .retrieve()
          .toEntity(String.class);

      final String responseBody = Objects.requireNonNull(response.getBody());
      return new ResolveSuccessResponse(ResolveStatement.parse(responseBody));
    }
    catch (final com.nimbusds.oauth2.sdk.ParseException e) {
      throw new RuntimeException(e);
    }
    catch (final HttpClientErrorException badRequestEx) {
      throw badRequestEx;
    }

  }

  /**
   * Configures and retrieves an entity statement for the provided EntityID.
   *
   * @param entityId the entity ID used to construct the URI for the entity statement.
   * @return an EntityStatement object containing the configured entity statement.
   */
  public EntityStatement entitConfiguration(final EntityID entityId) {
    final UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUri(entityId.toURI())
        .path("/.well-known/openid-federation");
    final URI uri = uriBuilder.build().toUri();
    return this.callEntityStatement(uri);
  }

  /**
   * Constructs an EntityStatement by fetching data from the specified URI and using a subordinate entity's ID as a
   * query parameter.
   *
   * @param fetchUrl the base URI to fetch the EntityStatement from
   * @param subordinate the subordinate entity containing the ID to be used as a query parameter
   * @return the constructed EntityStatement fetched using the specified URI and subordinate entity ID
   */
  public EntityStatement entityStatement(final URI fetchUrl, final EntityID subordinate) {
    final UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUri(fetchUrl)
        .queryParam("sub", subordinate.getValue());
    return this.callEntityStatement(uriBuilder.build().toUri());
  }

  private EntityStatement callEntityStatement(final URI uri) {

    log.debug("Getting entityStatment {}", uri);
    try {
      final ResponseEntity<String> response = this.restClient.get()
          .uri(uri)
          .retrieve()
          .toEntity(String.class);
      final MediaType contentType = response.getHeaders().getContentType();
      if (contentType == null ||
          !contentType.isCompatibleWith(MediaType.parseMediaType("application/entity-statement+jwt"))) {
        log.debug("Unexpected content type: " + contentType + " for::" + uri);
      }

      final String responseBody = Objects.requireNonNull(response.getBody());
      return EntityStatement.parse(responseBody);
    }
    catch (final com.nimbusds.oauth2.sdk.ParseException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Retrieves a list of federation entity IDs from a specified URL.
   *
   * @param listUrl the URI pointing to the federation listing resource
   * @return a list of EntityID objects parsed from the response at the given URI
   */
  public List<EntityID> federationListing(final URI listUrl) {

    final UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUri(listUrl);

    final URI uri = uriBuilder.build().toUri();
    log.debug("Getting entityStatment {}", uri);
    final ResponseEntity<String[]> response = this.restClient.get()
        .uri(uri)
        .retrieve()
        .toEntity(String[].class);

    return Arrays.stream(Objects.requireNonNull(response.getBody())).map(EntityID::new).toList();
  }

}
