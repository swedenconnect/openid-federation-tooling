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

import java.util.Map;

/**
 * Validator for DefaultMetaDataValidator metadata
 *
 * @author Per Fredrik Plars
 */
public class DefaultMetaDataValidator implements MetadataValidator {

  final PropertyValidators propertyValidators = new PropertyValidators();
  private String supportedMetadataType = "";

  /**
   * Constructs an instance of DefaultMetaDataValidator.
   */
  public DefaultMetaDataValidator() {
    this.propertyValidators.registerValidator(new PingHttpValidator());
  }

  @Override
  public boolean supports(final String metadataNodeName) {
    this.supportedMetadataType = metadataNodeName;
    return true;
  }

  /**
   * Validates the provided metadata and updates the validation result with appropriate messages. If the metadata is
   * null, the method returns the given result without modifications. Otherwise, a default informational message is
   * added to the result indicating that validation is not supported for the given metadata type.
   *
   * @param metadata a map of metadata to validate; can be null
   * @param result an existing validation result object to which the validation outcomes are added
   * @return the updated validation result object
   */
  public ValidationResult validate(final Map<String, Object> metadata, final ValidationResult result) {
    if (metadata == null) {
      return result;
    }

    final String baseFiledName = "metadata.%s.".formatted(this.supportedMetadataType);
    result.addResult(ValidationResult.Level.INFO,
        baseFiledName,
        "Validation is not supported for this metadata type. ", this.supportedMetadataType);
    return result;
  }

}
