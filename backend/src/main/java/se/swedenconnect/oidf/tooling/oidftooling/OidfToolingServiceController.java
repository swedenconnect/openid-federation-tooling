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

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityType;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.swedenconnect.oidf.tooling.domain.JWTDecoded;
import se.swedenconnect.oidf.tooling.dto.JwtContentDto;
import se.swedenconnect.oidf.tooling.oidftooling.tree.Graph;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Support for OIDF tooling
 *
 * @author Per Fredrik Plars
 */
@RestController
@RequestMapping("/api")
public class OidfToolingServiceController {

  private final OidfToolingService oidfToolingService;

  /**
   * Constructor for the TestServiceController class.
   *
   * @param oidfToolingService an instance of oidfToolingService
   */
  public OidfToolingServiceController(final OidfToolingService oidfToolingService) {
    this.oidfToolingService = oidfToolingService;
  }

  /**
   * Retrieves the entity configuration (the self-issued entity statement) of an entity.
   *
   * @param subject the entity id to fetch the entity configuration for
   * @return the entity configuration's header, payload and signature
   * @throws ParseException if the entity id cannot be parsed
   */
  @GetMapping(value = "/entity-statement", produces = MediaType.APPLICATION_JSON_VALUE)
  public JwtContentDto entityStatement(
      @RequestParam(required = true, name = "sub") final String subject) throws ParseException {

    final JWTDecoded jwtDecoded = this.oidfToolingService.getEntityConfiguration(EntityID.parse(subject));
    return JwtContentDto.builder()
        .header(jwtDecoded.getHeader())
        .payload(jwtDecoded.getPayload())
        .signature(jwtDecoded.getSignature())
        .build();
  }

  /**
   * Resolves an entity through the configured resolver.
   *
   * @param subject A required subject identifier to resolve.
   * @return the resolve response's header, payload and signature.
   */
  @GetMapping(value = "/resolve", produces = MediaType.APPLICATION_JSON_VALUE)
  public JwtContentDto resolve(
      @RequestParam(required = true, name = "sub") final String subject) throws ParseException {

    final JWTDecoded jwtDecoded = this.oidfToolingService.getResolverResponse(EntityID.parse(subject));
    return JwtContentDto.builder()
        .header(jwtDecoded.getHeader())
        .payload(jwtDecoded.getPayload())
        .signature(jwtDecoded.getSignature())
        .build();
  }

  /**
   * Handles the discovery endpoint and returns the entity ids found, narrowed by the optional filters.
   *
   * @param entityid optional text; only entity ids containing it (case-insensitive) are returned
   * @param entityType optional entity type to filter on
   * @param trustmark optional trust mark type to filter on
   * @return a list of entity ids derived from the discovery response
   */
  @GetMapping(value = "/discovery", produces = MediaType.APPLICATION_JSON_VALUE)
  public List<String> discovery(
      @RequestParam(required = false, name = "entityid") final String entityid,
      @RequestParam(required = false, name = "entityType") final String entityType,
      @RequestParam(required = false, name = "trustMark") final String trustmark
  ) {
    final String needle = Optional.ofNullable(entityid).map(String::trim).filter(s -> !s.isEmpty())
        .map(s -> s.toLowerCase(Locale.ROOT)).orElse(null);

    return this.oidfToolingService.getDiscoverResponse(
            Optional.ofNullable(entityType).filter(s -> !s.isBlank()).map(EntityType::new),
            Optional.ofNullable(trustmark).filter(s -> !s.isBlank()).map(List::of).orElse(Collections.emptyList()))
        .stream()
        .map(EntityID::getValue)
        .filter(id -> needle == null || id.toLowerCase(Locale.ROOT).contains(needle))
        .toList();
  }

  /**
   * Retrieves the tree structure of nodes as a graph.
   *
   * @return a Graph object representing the node structure.
   */
  @GetMapping(value = "/tree", produces = MediaType.APPLICATION_JSON_VALUE)
  public Graph tree() {
    return this.oidfToolingService.getNodeStructure();
  }

  /**
   * Retrieves the subordinate statement a parent entity issued about a specific child, i.e. the data behind one
   * edge in the graph returned by {@link #tree()}.
   *
   * @param parent the parent (issuer) entity id of the edge
   * @param sub the child (subject) entity id of the edge
   * @return the subordinate statement's header, payload and signature
   * @throws ParseException if either entity id cannot be parsed
   */
  @GetMapping(value = "/subordinate-statement", produces = MediaType.APPLICATION_JSON_VALUE)
  public JwtContentDto subordinateStatement(
      @RequestParam(required = true, name = "parent") final String parent,
      @RequestParam(required = true, name = "sub") final String sub) throws ParseException {

    final JWTDecoded jwtDecoded =
        this.oidfToolingService.getSubordinateStatement(EntityID.parse(parent), EntityID.parse(sub));
    return JwtContentDto.builder()
        .header(jwtDecoded.getHeader())
        .payload(jwtDecoded.getPayload())
        .signature(jwtDecoded.getSignature())
        .build();
  }

}



