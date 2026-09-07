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

import org.junit.jupiter.api.Test;
import se.swedenconnect.oidf.tooling.domain.ValidationResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SwedenConnectMetadataRulesTest {

  private static final String BASE = "metadata.openid_provider.";

  @Test
  void requireSuperset_warnsForEachMissingValue() {
    final Map<String, Object> metadata = Map.of("scopes_supported", List.of("openid"));
    final ValidationResult result = new ValidationResult();
    SwedenConnectMetadataRules.requireSuperset(metadata, "scopes_supported", Set.of("openid", "profile"), BASE,
        result);
    assertEquals(1, result.getSubResults().size());
    assertEquals(BASE + "scopes_supported", result.getSubResults().get(0).type());
  }

  @Test
  void requireSuperset_noWarningWhenAllPresent() {
    final Map<String, Object> metadata = Map.of("scopes_supported", List.of("openid", "profile"));
    final ValidationResult result = new ValidationResult();
    SwedenConnectMetadataRules.requireSuperset(metadata, "scopes_supported", Set.of("openid", "profile"), BASE,
        result);
    assertTrue(result.getSubResults().isEmpty());
  }

  @Test
  void requireExcludes_warnsWhenForbiddenValuePresent() {
    final Map<String, Object> metadata = Map.of("code_challenge_methods_supported", List.of("S256", "plain"));
    final ValidationResult result = new ValidationResult();
    SwedenConnectMetadataRules.requireExcludes(metadata, "code_challenge_methods_supported", Set.of("plain"), BASE,
        result);
    assertEquals(1, result.getSubResults().size());
  }

  @Test
  void requireExcludes_noWarningWhenForbiddenValueAbsent() {
    final Map<String, Object> metadata = Map.of("code_challenge_methods_supported", List.of("S256"));
    final ValidationResult result = new ValidationResult();
    SwedenConnectMetadataRules.requireExcludes(metadata, "code_challenge_methods_supported", Set.of("plain"), BASE,
        result);
    assertTrue(result.getSubResults().isEmpty());
  }

  @Test
  void requireValueEquals_warnsOnMismatch() {
    final Map<String, Object> metadata = Map.of("claims_parameter_supported", false);
    final ValidationResult result = new ValidationResult();
    SwedenConnectMetadataRules.requireValueEquals(metadata, "claims_parameter_supported", true, BASE, result);
    assertEquals(1, result.getSubResults().size());
  }

  @Test
  void requireValueEquals_noWarningOnMatch() {
    final Map<String, Object> metadata = Map.of("claims_parameter_supported", true);
    final ValidationResult result = new ValidationResult();
    SwedenConnectMetadataRules.requireValueEquals(metadata, "claims_parameter_supported", true, BASE, result);
    assertTrue(result.getSubResults().isEmpty());
  }

  @Test
  void requireIfPresentThenPresent_warnsWhenDependencyMissing() {
    final Map<String, Object> metadata = new HashMap<>();
    metadata.put("id_token_encrypted_response_enc", "A128GCM");
    final ValidationResult result = new ValidationResult();
    SwedenConnectMetadataRules.requireIfPresentThenPresent(metadata, "id_token_encrypted_response_enc",
        "id_token_encrypted_response_alg", BASE, result);
    assertEquals(1, result.getSubResults().size());
  }

  @Test
  void requireIfPresentThenPresent_noWarningWhenBothPresent() {
    final Map<String, Object> metadata = new HashMap<>();
    metadata.put("id_token_encrypted_response_enc", "A128GCM");
    metadata.put("id_token_encrypted_response_alg", "RSA-OAEP");
    final ValidationResult result = new ValidationResult();
    SwedenConnectMetadataRules.requireIfPresentThenPresent(metadata, "id_token_encrypted_response_enc",
        "id_token_encrypted_response_alg", BASE, result);
    assertTrue(result.getSubResults().isEmpty());
  }

  @Test
  void requireIfPresentThenPresent_noWarningWhenNeitherPresent() {
    final Map<String, Object> metadata = Map.of();
    final ValidationResult result = new ValidationResult();
    SwedenConnectMetadataRules.requireIfPresentThenPresent(metadata, "id_token_encrypted_response_enc",
        "id_token_encrypted_response_alg", BASE, result);
    assertTrue(result.getSubResults().isEmpty());
  }

  @Test
  void requireScopeClaims_warnsForEachMissingClaim() {
    final Map<String, Object> metadata = Map.of(
        "scopes_supported", List.of("https://id.oidc.se/scope/naturalPersonInfo"),
        "claims_supported", List.of("family_name"));
    final ValidationResult result = new ValidationResult();
    SwedenConnectMetadataRules.requireScopeClaims(metadata, "scopes_supported", "claims_supported", BASE, result);
    assertEquals(4, result.getSubResults().size());
  }

  @Test
  void requireScopeClaims_noWarningWhenAllClaimsPresent() {
    final Map<String, Object> metadata = Map.of(
        "scopes_supported", List.of("openid"),
        "claims_supported", List.of("sub"));
    final ValidationResult result = new ValidationResult();
    SwedenConnectMetadataRules.requireScopeClaims(metadata, "scopes_supported", "claims_supported", BASE, result);
    assertTrue(result.getSubResults().isEmpty());
  }

  @Test
  void requireScopeClaims_skipsScopesWithoutKnownClaimList() {
    final Map<String, Object> metadata = Map.of(
        "scopes_supported", List.of("https://id.oidc.se/scope/sign"),
        "claims_supported", List.of());
    final ValidationResult result = new ValidationResult();
    SwedenConnectMetadataRules.requireScopeClaims(metadata, "scopes_supported", "claims_supported", BASE, result);
    assertTrue(result.getSubResults().isEmpty());
  }
}
