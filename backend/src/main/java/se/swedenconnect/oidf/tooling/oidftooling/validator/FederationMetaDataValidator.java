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
package se.swedenconnect.oidf.tooling.oidftooling.validator;

import se.swedenconnect.oidf.tooling.domain.ValidationResult;
import se.swedenconnect.oidf.tooling.validation.MetadataValidator;
import se.swedenconnect.oidf.tooling.validation.PingHttpValidator;
import se.swedenconnect.oidf.tooling.validation.PropertyValidators;
import se.swedenconnect.oidf.tooling.validation.VariabelValueResolver;

import java.util.Map;

/**
 * Validator for FederationMetaDataValidator metadata
 *
 * @author Per Fredrik Plars
 */
public class FederationMetaDataValidator implements MetadataValidator {

  final static String supportedMetadataType = "federation_entity";
  final PropertyValidators propertyValidators = new PropertyValidators();

  /**
   * Constructs an instance of FederationMetaDataValidator.
   */
  public FederationMetaDataValidator() {
    this.propertyValidators.registerValidator(new PingHttpValidator());
  }

  @Override
  public boolean supports(final String metadataNodeName) {
    return supportedMetadataType.equalsIgnoreCase(metadataNodeName);
  }

  /**
   * Validates the provided metadata map based on predefined validation rules and populates the given validation result
   * object with warnings and errors where applicable.
   *
   * @param metadata a map containing metadata to be validated, where each entry represents a specific metadata
   *     field and its corresponding value.
   * @param result an existing {@code ValidationResult} object used to store the outcomes of the validation process,
   *     including warnings and errors.
   * @return the updated {@code ValidationResult} object containing the results of the validation checks performed on
   *     the metadata.
   */
  public ValidationResult validate(final Map<String, Object> metadata, final ValidationResult result) {
    if (metadata == null) {
      return result;
    }
    final String baseFiledName = "metadata.%s.".formatted(supportedMetadataType);

    this.v().required()
        .build().eval(baseFiledName + "organization_name", metadata.get("organization_name")).ifPresent(result.warn());

    this.v().url()
        .build().eval(baseFiledName + "federation_discovery_endpoint", metadata.get("federation_discovery_endpoint"))
        .ifPresent(result.warn());

    this.v().url()
        .build().eval(baseFiledName + "federation_resolve_endpoint", metadata.get("federation_resolve_endpoint"))
        .ifPresent(result.warn());

    this.v().url()
        .build().eval(baseFiledName + "federation_list_endpoint", metadata.get("federation_list_endpoint"))
        .ifPresent(result.warn());

    return result;
  }

  private PropertyValidators.ValidationStringBuilder v() {
    return this.propertyValidators.builder(VariabelValueResolver.defaultResolver());
  }

}
