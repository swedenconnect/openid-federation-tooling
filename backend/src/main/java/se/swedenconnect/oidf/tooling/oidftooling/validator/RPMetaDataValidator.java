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
import java.util.Set;

/**
 * Validator for RelyingParty metadata, implementing the RP-specific requirements of the Sweden Connect OIDC
 * metadata profile (https://docs.swedenconnect.se/federation/oidc-metadata-requirements.html). Fields common to
 * both RP and OP live in {@link SwedenConnectMetadataRules}.
 *
 * @author Per Fredrik Plars
 */
public class RPMetaDataValidator implements MetadataValidator {

  final static String supportedMetadataType = "openid_relying_party";
  final PropertyValidators propertyValidators = new PropertyValidators();

  /**
   * Constructs an instance of RPMetaDataValidator.
   */
  public RPMetaDataValidator() {
    this.propertyValidators.registerValidator(new PingHttpValidator());
  }

  @Override
  public boolean supports(final String metadataNodeName) {
    return supportedMetadataType.equalsIgnoreCase(metadataNodeName);
  }

  /**
   * Validates the provided metadata map against the RP requirements of the Sweden Connect OIDC metadata profile.
   * Any validation issues are added as warnings to the provided ValidationResult instance.
   *
   * @param metadata a map containing key-value pairs representing the metadata to be validated
   * @param result an instance of ValidationResult used to collect validation outcomes, including warnings for any
   *     detected issues
   * @return the updated ValidationResult instance with collected warnings and validation results
   */
  public ValidationResult validate(final Map<String, Object> metadata, final ValidationResult result) {
    if (metadata == null) {
      return result;
    }
    final String baseFiledName = "metadata.%s.".formatted(supportedMetadataType);

    SwedenConnectMetadataRules.validateCommonFields(metadata, baseFiledName, this.propertyValidators, result);
    SwedenConnectMetadataRules.validateMultilingual(metadata, baseFiledName, "client_name",
        this.propertyValidators, result);
    SwedenConnectMetadataRules.validateKeyMaterial(metadata, baseFiledName, this.propertyValidators,
        true, false, result);

    this.v().required().url().build()
        .eval(baseFiledName + "client_uri", metadata.get("client_uri")).ifPresent(result.warn());

    this.v().required().url().build()
        .eval(baseFiledName + "redirect_uris", metadata.get("redirect_uris")).ifPresent(result.warn());

    this.v().url().build()
        .eval(baseFiledName + "post_logout_redirect_uris", metadata.get("post_logout_redirect_uris"))
        .ifPresent(result.warn());

    SwedenConnectMetadataRules.requireSuperset(metadata, "response_types", Set.of("code"), baseFiledName, result);
    SwedenConnectMetadataRules.requireSuperset(metadata, "grant_types", Set.of("authorization_code"),
        baseFiledName, result);

    // Only "private_key_jwt" is allowed - no alternatives per the Sweden Connect profile.
    this.v().required().matches("^private_key_jwt$").build()
        .eval(baseFiledName + "token_endpoint_auth_method", metadata.get("token_endpoint_auth_method"))
        .ifPresent(result.warn());

    this.v().matches(SwedenConnectMetadataRules.MANDATORY_SIGNING_ALG_PATTERN).build()
        .eval(baseFiledName + "token_endpoint_auth_signing_alg", metadata.get("token_endpoint_auth_signing_alg"))
        .ifPresent(result.warn());

    // The trust anchor metadata policy nulls out these single-valued response algorithm claims in resolved
    // metadata - presence here indicates the policy was not applied.
    if (metadata.get("id_token_signed_response_alg") != null) {
      result.addResult(ValidationResult.Level.WARNING, baseFiledName + "id_token_signed_response_alg",
          "This field is removed by the trust anchor metadata policy and should not appear in resolved metadata",
          String.valueOf(metadata.get("id_token_signed_response_alg")));
    }
    if (metadata.get("userinfo_signed_response_alg") != null) {
      result.addResult(ValidationResult.Level.WARNING, baseFiledName + "userinfo_signed_response_alg",
          "This field is removed by the trust anchor metadata policy and should not appear in resolved metadata",
          String.valueOf(metadata.get("userinfo_signed_response_alg")));
    }

    this.v().required().build()
        .eval(baseFiledName + "id_token_signing_alg_values_supported",
            metadata.get("id_token_signing_alg_values_supported"))
        .ifPresent(result.warn());
    SwedenConnectMetadataRules.requireSuperset(metadata, "id_token_signing_alg_values_supported",
        SwedenConnectMetadataRules.MANDATORY_SIGNING_ALGS, baseFiledName, result);
    SwedenConnectMetadataRules.requireExcludes(metadata, "id_token_signing_alg_values_supported",
        Set.of("none"), baseFiledName, result);

    this.v().required().build()
        .eval(baseFiledName + "userinfo_signing_alg_values_supported",
            metadata.get("userinfo_signing_alg_values_supported"))
        .ifPresent(result.warn());
    SwedenConnectMetadataRules.requireSuperset(metadata, "userinfo_signing_alg_values_supported",
        SwedenConnectMetadataRules.MANDATORY_SIGNING_ALGS, baseFiledName, result);
    SwedenConnectMetadataRules.requireExcludes(metadata, "userinfo_signing_alg_values_supported",
        Set.of("none"), baseFiledName, result);

    this.validateEncryptionPair(metadata, baseFiledName, "id_token_encrypted_response_alg",
        "id_token_encrypted_response_enc", result);
    this.validateEncryptionPair(metadata, baseFiledName, "userinfo_encrypted_response_alg",
        "userinfo_encrypted_response_enc", result);

    this.v().matches(SwedenConnectMetadataRules.MANDATORY_SIGNING_ALG_PATTERN).build()
        .eval(baseFiledName + "request_object_signing_alg", metadata.get("request_object_signing_alg"))
        .ifPresent(result.warn());

    this.v().matches(SwedenConnectMetadataRules.JWE_ALG_PATTERN).build()
        .eval(baseFiledName + "request_object_encryption_alg", metadata.get("request_object_encryption_alg"))
        .ifPresent(result.warn());
    this.v().matches(SwedenConnectMetadataRules.JWE_ENC_PATTERN).build()
        .eval(baseFiledName + "request_object_encryption_enc", metadata.get("request_object_encryption_enc"))
        .ifPresent(result.warn());
    SwedenConnectMetadataRules.requireIfPresentThenPresent(metadata, "request_object_encryption_enc",
        "request_object_encryption_alg", baseFiledName, result);

    this.v().matches("^(?:public|pairwise)$").build()
        .eval(baseFiledName + "subject_type", metadata.get("subject_type")).ifPresent(result.warn());

    if (metadata.get("require_auth_time") != null) {
      SwedenConnectMetadataRules.requireValueEquals(metadata, "require_auth_time", true, baseFiledName, result);
    }

    return result;
  }

  private void validateEncryptionPair(final Map<String, Object> metadata, final String baseFiledName,
      final String algKey, final String encKey, final ValidationResult result) {
    this.v().matches(SwedenConnectMetadataRules.JWE_ALG_PATTERN).build()
        .eval(baseFiledName + algKey, metadata.get(algKey)).ifPresent(result.warn());
    this.v().matches(SwedenConnectMetadataRules.JWE_ENC_PATTERN).build()
        .eval(baseFiledName + encKey, metadata.get(encKey)).ifPresent(result.warn());
    SwedenConnectMetadataRules.requireIfPresentThenPresent(metadata, encKey, algKey, baseFiledName, result);
  }

  private PropertyValidators.ValidationStringBuilder v() {
    return this.propertyValidators.builder(VariabelValueResolver.defaultResolver());
  }

}
