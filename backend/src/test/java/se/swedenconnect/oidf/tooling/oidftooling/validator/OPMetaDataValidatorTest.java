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

import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import org.junit.jupiter.api.Test;
import se.swedenconnect.oidf.tooling.domain.ValidationResult;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OPMetaDataValidatorTest {

  private static final String ID_TOKEN_SIGNING_ALGS_FIELD =
      "metadata.openid_provider.id_token_signing_alg_values_supported";
  private static final String TOKEN_AUTH_METHODS_FIELD =
      "metadata.openid_provider.token_endpoint_auth_methods_supported";
  private static final String CODE_CHALLENGE_FIELD = "metadata.openid_provider.code_challenge_methods_supported";
  private static final String CLAIMS_SUPPORTED_FIELD = "metadata.openid_provider.claims_supported";

  final OPMetaDataValidator validator = new OPMetaDataValidator();

  @Test
  void supportsOnlyOpenidProvider() {
    assertTrue(this.validator.supports("openid_provider"));
    assertFalse(this.validator.supports("openid_relying_party"));
  }

  @Test
  void validate_happyPath_doesNotFailOverallValidation() {
    final ValidationResult result = this.validator.validate(this.happyPathMetadata(), new ValidationResult());
    assertTrue(result.isSuccess());
    assertFalse(this.hasWarningFor(result, TOKEN_AUTH_METHODS_FIELD));
    assertFalse(this.hasWarningFor(result, CODE_CHALLENGE_FIELD));
    assertFalse(this.hasWarningFor(result, ID_TOKEN_SIGNING_ALGS_FIELD));
  }

  @Test
  void validate_missingPrivateKeyJwt_warns() {
    final Map<String, Object> metadata = this.happyPathMetadata();
    metadata.put("token_endpoint_auth_methods_supported", List.of("client_secret_basic"));

    final ValidationResult result = this.validator.validate(metadata, new ValidationResult());

    assertTrue(this.hasWarningFor(result, TOKEN_AUTH_METHODS_FIELD));
  }

  @Test
  void validate_plainCodeChallengeMethod_warns() {
    final Map<String, Object> metadata = this.happyPathMetadata();
    metadata.put("code_challenge_methods_supported", List.of("S256", "plain"));

    final ValidationResult result = this.validator.validate(metadata, new ValidationResult());

    assertTrue(this.hasWarningFor(result, CODE_CHALLENGE_FIELD));
  }

  @Test
  void validate_naturalPersonInfoScopeWithoutMatchingClaims_warns() {
    final Map<String, Object> metadata = this.happyPathMetadata();
    metadata.put("scopes_supported", List.of("openid", "https://id.oidc.se/scope/naturalPersonInfo"));

    final ValidationResult result = this.validator.validate(metadata, new ValidationResult());

    assertTrue(this.hasWarningFor(result, CLAIMS_SUPPORTED_FIELD));
  }

  private boolean hasWarningFor(final ValidationResult result, final String type) {
    return result.getSubResults().stream().anyMatch(sub -> sub.type().equals(type));
  }

  private Map<String, Object> happyPathMetadata() {
    final Map<String, Object> metadata = new HashMap<>();
    metadata.put("organization_name", "Test Org");
    metadata.put("organization_name#en", "Test Org");
    metadata.put("organization_name#sv", "Test Org");
    metadata.put("organization_identifier", "urn:glue:iso6523:0007:1234567890");
    metadata.put("organization_uri", "https://example.com/org");
    metadata.put("display_name", "Test OP");
    metadata.put("logo_uri", "https://example.com/logo.svg");
    metadata.put("contacts", List.of("op@example.com"));
    metadata.put("jwks", this.publicJwks());
    metadata.put("issuer", "https://example.com/op");
    metadata.put("authorization_endpoint", "https://example.com/op/authorize");
    metadata.put("token_endpoint", "https://example.com/op/token");
    metadata.put("userinfo_endpoint", "https://example.com/op/userinfo");
    metadata.put("ui_locales_supported", List.of("en", "sv"));
    metadata.put("scopes_supported", List.of("openid"));
    metadata.put("claims_supported", List.of("sub"));
    metadata.put("response_types_supported", List.of("code"));
    metadata.put("acr_values_supported", List.of("https://id.oidc.se/loa/loa3"));
    metadata.put("subject_types_supported", List.of("public", "pairwise"));
    metadata.put("token_endpoint_auth_methods_supported", List.of("private_key_jwt"));
    metadata.put("claims_parameter_supported", true);
    metadata.put("request_parameter_supported", true);
    metadata.put("request_uri_parameter_supported", true);
    metadata.put("code_challenge_methods_supported", List.of("S256"));
    metadata.put("client_registration_types_supported", List.of("automatic"));
    metadata.put("id_token_signing_alg_values_supported",
        new ArrayList<>(List.of("RS256", "RS384", "RS512", "ES256", "ES384", "ES512")));
    metadata.put("token_endpoint_auth_signing_alg_values_supported",
        new ArrayList<>(List.of("RS256", "RS384", "RS512", "ES256", "ES384", "ES512")));
    return metadata;
  }

  private Map<String, Object> publicJwks() {
    try {
      final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
      keyGen.initialize(Curve.P_256.toECParameterSpec());
      final KeyPair keyPair = keyGen.generateKeyPair();
      final ECKey publicKey = new ECKey.Builder(Curve.P_256, (ECPublicKey) keyPair.getPublic())
          .keyID("test-op-kid")
          .build();
      return new JWKSet(publicKey).toJSONObject();
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }
}
