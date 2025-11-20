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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import se.swedenconnect.oidf.tooling.domain.ValidationResult;
import se.swedenconnect.oidf.tooling.oidftooling.OidfMetaDataValidatorService;
import se.swedenconnect.oidf.tooling.validation.MetadataValidator;
import se.swedenconnect.oidf.tooling.validation.PingHttpValidator;
import se.swedenconnect.oidf.tooling.validation.PropertyValidator;
import se.swedenconnect.oidf.tooling.validation.PropertyValidators;
import se.swedenconnect.oidf.tooling.validation.VariabelValueResolver;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Validator for RelyingParty metadata
 *
 * @author Per Fredrik Plars
 */
public class RPMetaDataValidator implements MetadataValidator {

  final static ObjectMapper mapper = new ObjectMapper();
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
   * Validates the provided metadata map against predefined property validation rules. This method uses multiple
   * property validators to check fields for required constraints, length limits, and URL format correctness. Any
   * validation issues are added as warnings to the provided ValidationResult instance.
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
    final PropertyValidator req = this.v().required().length(1, 1000).build();
    final PropertyValidator reqUrl = this.v().required().url().build();
    req.eval(baseFiledName + "organization_name", metadata.get("organization_name")).ifPresent(result.warn());
    req.eval(baseFiledName + "organization_name#en", metadata.get("organization_name#en")).ifPresent(result.warn());
    req.eval(baseFiledName + "organization_name#sv", metadata.get("organization_name#sv")).ifPresent(result.warn());

    req.eval(baseFiledName + "client_name", metadata.get("client_name")).ifPresent(result.warn());
    req.eval(baseFiledName + "client_name#en", metadata.get("client_name#en")).ifPresent(result.warn());
    req.eval(baseFiledName + "client_name#sv", metadata.get("client_name#sv")).ifPresent(result.warn());

    req.eval(baseFiledName + "description", metadata.get("description")).ifPresent(result.warn());

    req.eval(baseFiledName + "display_name", metadata.get("display_name")).ifPresent(result.warn());
    reqUrl.eval(baseFiledName + "redirect_uris", metadata.get("redirect_uris")).ifPresent(result.warn());

    this.v().required().matches("^(?:code|code\\s+id_token|code\\s+token|code\\s+id_token\\s+token)$")
        .build().eval(baseFiledName + "response_types", metadata.get("response_types")).ifPresent(result.warn());

    this.v().required()
        .matches(OidfMetaDataValidatorService.allowedSignAlg)
        .build()
        .eval(baseFiledName + "token_endpoint_auth_signing_alg", metadata.get("token_endpoint_auth_signing_alg"))
        .ifPresent(result.warn());

    this.v().required()
        .email()
        .build().eval(baseFiledName + "contacts", metadata.get("contacts"))
        .ifPresent(result.warn());

    reqUrl.eval(baseFiledName + "post_logout_redirect_uris", metadata.get("post_logout_redirect_uris"))
        .ifPresent(result.warn());

    this.v().required()
        .matches(OidfMetaDataValidatorService.allowedSignAlg)
        .build().eval(baseFiledName + "userinfo_signed_response_alg", metadata.get("userinfo_signed_response_alg"))
        .ifPresent(result.warn());

    this.v().required()
        .matches(OidfMetaDataValidatorService.allowedSignAlg)
        .build().eval(baseFiledName + "id_token_signed_response_alg", metadata.get("id_token_signed_response_alg"))
        .ifPresent(result.warn());

    this.v().required().url().ping().build().eval(baseFiledName + "logo_uri", metadata.get("logo_uri"))
        .ifPresent(result.warn());

    this.v().required()
        .matches("^(?:client_secret_basic|client_secret_post|client_secret_jwt|private_key_jwt|none|"
            + "tls_client_auth|self_signed_tls_client_auth)$").build()
        .eval(baseFiledName + "token_endpoint_auth_method",
            metadata.get("token_endpoint_auth_method")).ifPresent(result.warn());

    this.v().email().build().eval(baseFiledName + "contacts", metadata.get("contacts")).ifPresent(result.warn());

    if (!metadata.containsKey("jwks_uri") && !metadata.containsKey("jwks")) {
      result.addResult(ValidationResult.Level.ERROR,
          "baseFiledName", "Expects one of the fields to be pressent: jwks or jwks_uri");
    }

    this.v().entityid()
        .ping()
        .build()
        .eval(baseFiledName + "jwks_uri", metadata.get("jwks_uri"))
        .ifPresent(result.warn());
    try {
      this.v().jwks().build().eval(baseFiledName + "jwks", mapper.writeValueAsString(metadata.get("jwks")))
          .ifPresent(result.warn());
    }
    catch (final JsonProcessingException e) {
      result.addResult(ValidationResult.Level.WARNING,
          baseFiledName + "jwks", "InvalidJson: " + e.getMessage());
    }

    return result;
  }

  private PropertyValidators.ValidationStringBuilder v() {
    return this.propertyValidators.builder(VariabelValueResolver.defaultResolver());
  }

  private <T> String doIfNotNull(final T value, final Function<T, String> f) {
    if (Objects.nonNull(value)) {
      return f.apply(value);
    }
    return null;
  }

}
