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
 * Validator for OpenID Provider metadata, implementing the OP-specific requirements of the Sweden Connect OIDC
 * metadata profile (https://docs.swedenconnect.se/federation/oidc-metadata-requirements.html). Fields common to
 * both RP and OP live in {@link SwedenConnectMetadataRules}.
 *
 * @author Per Fredrik Plars
 */
public class OPMetaDataValidator implements MetadataValidator {

  final static String supportedMetadataType = "openid_provider";
  final PropertyValidators propertyValidators = new PropertyValidators();

  /**
   * Constructs an instance of OPMetaDataValidator.
   */
  public OPMetaDataValidator() {
    this.propertyValidators.registerValidator(new PingHttpValidator());
  }

  @Override
  public boolean supports(final String metadataNodeName) {
    return supportedMetadataType.equalsIgnoreCase(metadataNodeName);
  }

  /**
   * Validates the provided metadata map against the OP requirements of the Sweden Connect OIDC metadata profile.
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
    SwedenConnectMetadataRules.validateKeyMaterial(metadata, baseFiledName, this.propertyValidators,
        false, true, result);

    this.v().required().entityid().build()
        .eval(baseFiledName + "issuer", metadata.get("issuer")).ifPresent(result.warn());

    this.v().required().url().build()
        .eval(baseFiledName + "authorization_endpoint", metadata.get("authorization_endpoint"))
        .ifPresent(result.warn());
    this.v().required().url().build()
        .eval(baseFiledName + "token_endpoint", metadata.get("token_endpoint")).ifPresent(result.warn());
    this.v().required().url().build()
        .eval(baseFiledName + "userinfo_endpoint", metadata.get("userinfo_endpoint")).ifPresent(result.warn());

    SwedenConnectMetadataRules.requireSuperset(metadata, "ui_locales_supported", Set.of("en", "sv"),
        baseFiledName, result);

    this.v().url().build()
        .eval(baseFiledName + "service_documentation", metadata.get("service_documentation"))
        .ifPresent(result.warn());

    SwedenConnectMetadataRules.requireSuperset(metadata, "scopes_supported", Set.of("openid"),
        baseFiledName, result);

    this.v().required().build()
        .eval(baseFiledName + "claims_supported", metadata.get("claims_supported")).ifPresent(result.warn());
    SwedenConnectMetadataRules.requireScopeClaims(metadata, "scopes_supported", "claims_supported",
        baseFiledName, result);

    SwedenConnectMetadataRules.requireSuperset(metadata, "response_types_supported", Set.of("code"),
        baseFiledName, result);

    this.v().required().build()
        .eval(baseFiledName + "acr_values_supported", metadata.get("acr_values_supported")).ifPresent(result.warn());

    SwedenConnectMetadataRules.requireSuperset(metadata, "subject_types_supported", Set.of("public"),
        baseFiledName, result);
    final Object subjectTypesSupported = metadata.get("subject_types_supported");
    if (subjectTypesSupported == null || !subjectTypesSupported.toString().contains("pairwise")) {
      result.addResult(ValidationResult.Level.WARNING, baseFiledName + "subject_types_supported",
          "It is recommended that 'pairwise' is also supported", String.valueOf(subjectTypesSupported));
    }

    SwedenConnectMetadataRules.requireSuperset(metadata, "token_endpoint_auth_methods_supported",
        Set.of("private_key_jwt"), baseFiledName, result);

    SwedenConnectMetadataRules.requireValueEquals(metadata, "claims_parameter_supported", true,
        baseFiledName, result);
    SwedenConnectMetadataRules.requireValueEquals(metadata, "request_parameter_supported", true,
        baseFiledName, result);
    if (!Boolean.TRUE.equals(metadata.get("request_uri_parameter_supported"))) {
      result.addResult(ValidationResult.Level.WARNING, baseFiledName + "request_uri_parameter_supported",
          "It is recommended that this is set to true to support signature requests",
          String.valueOf(metadata.get("request_uri_parameter_supported")));
    }

    SwedenConnectMetadataRules.requireSuperset(metadata, "code_challenge_methods_supported", Set.of("S256"),
        baseFiledName, result);
    SwedenConnectMetadataRules.requireExcludes(metadata, "code_challenge_methods_supported", Set.of("plain"),
        baseFiledName, result);

    this.v().required().build()
        .eval(baseFiledName + "client_registration_types_supported",
            metadata.get("client_registration_types_supported")).ifPresent(result.warn());

    SwedenConnectMetadataRules.requireSuperset(metadata, "id_token_signing_alg_values_supported",
        SwedenConnectMetadataRules.MANDATORY_SIGNING_ALGS, baseFiledName, result);
    SwedenConnectMetadataRules.requireExcludes(metadata, "id_token_signing_alg_values_supported",
        Set.of("none"), baseFiledName, result);

    SwedenConnectMetadataRules.requireSuperset(metadata, "token_endpoint_auth_signing_alg_values_supported",
        SwedenConnectMetadataRules.MANDATORY_SIGNING_ALGS, baseFiledName, result);
    SwedenConnectMetadataRules.requireExcludes(metadata, "token_endpoint_auth_signing_alg_values_supported",
        Set.of("none"), baseFiledName, result);

    this.validateOptionalSigningAlgs(metadata, baseFiledName, "userinfo_signing_alg_values_supported", true, result);
    this.validateOptionalSigningAlgs(metadata, baseFiledName, "request_object_signing_alg_values_supported", false,
        result);

    this.validateOptionalEncryptionAlgs(metadata, baseFiledName, "id_token_encryption_alg_values_supported",
        "id_token_encryption_enc_values_supported", result);
    this.validateOptionalEncryptionAlgs(metadata, baseFiledName, "userinfo_encryption_alg_values_supported",
        "userinfo_encryption_enc_values_supported", result);
    this.validateOptionalEncryptionAlgs(metadata, baseFiledName, "request_object_encryption_alg_values_supported",
        "request_object_encryption_enc_values_supported", result);

    if (metadata.get("https://id.oidc.se/disco/userMessageSupportedMimeTypes") != null) {
      this.v().build()
          .eval(baseFiledName + "https://id.oidc.se/disco/userMessageSupportedMimeTypes",
              metadata.get("https://id.oidc.se/disco/userMessageSupportedMimeTypes"))
          .ifPresent(result.warn());
    }

    return result;
  }

  /**
   * Validates an optional {@code *_signing_alg_values_supported} array: when present, the mandatory signing
   * algorithms must be a subset of it, and (unless {@code noneAllowed} is false) {@code none} must not appear.
   */
  private void validateOptionalSigningAlgs(final Map<String, Object> metadata, final String baseFiledName,
      final String key, final boolean excludeNone, final ValidationResult result) {
    if (metadata.get(key) == null) {
      return;
    }
    SwedenConnectMetadataRules.requireSuperset(metadata, key, SwedenConnectMetadataRules.MANDATORY_SIGNING_ALGS,
        baseFiledName, result);
    if (excludeNone) {
      SwedenConnectMetadataRules.requireExcludes(metadata, key, Set.of("none"), baseFiledName, result);
    }
  }

  /** Validates an optional JWE {@code alg}/{@code enc} values-supported pair when either is present. */
  private void validateOptionalEncryptionAlgs(final Map<String, Object> metadata, final String baseFiledName,
      final String algKey, final String encKey, final ValidationResult result) {
    if (metadata.get(algKey) != null) {
      SwedenConnectMetadataRules.requireSuperset(metadata, algKey, Set.of("RSA-OAEP", "RSA-OAEP-256", "ECDH-ES"),
          baseFiledName, result);
    }
    if (metadata.get(encKey) != null) {
      SwedenConnectMetadataRules.requireSuperset(metadata, encKey,
          Set.of("A128CBC-HS256", "A256CBC-HS512", "A128GCM", "A256GCM"), baseFiledName, result);
    }
  }

  private PropertyValidators.ValidationStringBuilder v() {
    return this.propertyValidators.builder(VariabelValueResolver.defaultResolver());
  }

}
