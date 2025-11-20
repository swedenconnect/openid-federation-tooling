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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.swedenconnect.oidf.tooling.domain.JWTDecoded;
import se.swedenconnect.oidf.tooling.domain.ValidationResult;
import se.swedenconnect.oidf.tooling.dto.ValidationResponseDto;
import se.swedenconnect.oidf.tooling.oidftooling.tree.Graph;

import java.util.Collections;
import java.util.List;
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
  private final OidfMetaDataValidatorService oidfMetaDataValidatorService;

  /**
   * Constructor for the TestServiceController class.
   *
   * @param oidfToolingService an instance of oidfToolingService
   * @param oidfMetaDataValidatorService an instance of OIDFMetaDataValidator used to validate metadata.
   */
  public OidfToolingServiceController(final OidfToolingService oidfToolingService,
      final OidfMetaDataValidatorService oidfMetaDataValidatorService) {
    this.oidfToolingService = oidfToolingService;
    this.oidfMetaDataValidatorService = oidfMetaDataValidatorService;
  }

  /**
   * Validates the provided metadata.
   *
   * @param metadata the metadata string to be validated
   * @return a ResponseEntity containing a ValidationResponseDto object with the validation results
   */
  @PostMapping(value = "/validator", produces = MediaType.APPLICATION_JSON_VALUE)
  public ValidationResponseDto validateMetadata(@RequestBody final String metadata) {
    final ValidationResult result = this.oidfMetaDataValidatorService.validate(metadata);
    final JWTDecoded jwtDecoded = Optional.ofNullable(result.getValidatedData()).orElse(JWTDecoded.builder().build());

    return ValidationResponseDto.builder()
        .validatedData(ValidationResponseDto.JwtContent.builder()
            .header(jwtDecoded.getHeader())
            .payload(jwtDecoded.getPayload())
            .signature(jwtDecoded.getSignature())
            .build())
        .success(result.isSuccess())
        .subResults(result.getSubResults()
            .stream()
            .map(subResult -> ValidationResponseDto.ValidationStatus.builder()
                .level(subResult.level().name())
                .type(subResult.type())
                .message(subResult.message())
                .originalInputValue(subResult.originalInputValue())
                .build())
            .toList())
        .build();

  }

  /**
   * Resolves a validation request by performing metadata validation and returning the results.
   *
   * @param subject A required subject identifier used during the validation process.
   * @return A {@link ValidationResponseDto} object containing the validation results, including success status,
   *     validated data, and any sub-results of the validation process.
   */
  @GetMapping(value = "/resolve", produces = MediaType.APPLICATION_JSON_VALUE)
  public ValidationResponseDto.JwtContent resolve(
      @RequestParam(required = true, name = "sub") final String subject) throws ParseException {

    final JWTDecoded jwtDecoded = this.oidfToolingService.getResolverResponse(EntityID.parse(subject));
    return ValidationResponseDto.JwtContent.builder()
        .header(jwtDecoded.getHeader())
        .payload(jwtDecoded.getPayload())
        .signature(jwtDecoded.getSignature())
        .build();
  }

  /**
   * Handles the discovery endpoint and returns a list of string values based on the provided parameters.
   *
   * @param entityid optional parameter specifying the entity ID
   * @param entityType optional parameter specifying the type of the entity
   * @param trustmark optional list of trustmarks for the discovery process
   * @return a list of strings derived from the discovery response
   */
  @GetMapping(value = "/discovery", produces = MediaType.APPLICATION_JSON_VALUE)
  public List<String> discovery(
      @RequestParam(required = false, name = "entityid") final String entityid,
      @RequestParam(required = false, name = "entityType") final String entityType,
      @RequestParam(required = false, name = "trustmarks") final List<String> trustmark
  ) {

    return this.oidfToolingService.getDiscoverResponse(
        Optional.ofNullable(entityType).filter(s -> !s.isBlank()).map(EntityType::new),
        Collections.emptyList()).stream().map(EntityID::getValue).toList();

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

}



