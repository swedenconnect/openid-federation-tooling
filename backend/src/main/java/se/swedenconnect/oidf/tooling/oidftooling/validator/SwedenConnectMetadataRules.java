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
import se.swedenconnect.oidf.tooling.domain.ValidationResult;
import se.swedenconnect.oidf.tooling.validation.PropertyValidators;
import se.swedenconnect.oidf.tooling.validation.VariabelValueResolver;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Shared metadata validation rules for the Sweden Connect federation profile, reused by both
 * {@link RPMetaDataValidator} and {@link OPMetaDataValidator}.
 *
 * @author Per Fredrik Plars
 */
public final class SwedenConnectMetadataRules {

  /** JWS signature algorithms mandated by SC.Security Section 3 (excludes {@code PS*}). */
  public static final String MANDATORY_SIGNING_ALG_PATTERN = "^(RS256|RS384|RS512|ES256|ES384|ES512)$";

  /** Allowed JWE key management algorithms per SC.Security Section 3. */
  public static final String JWE_ALG_PATTERN = "^(RSA-OAEP|RSA-OAEP-256|ECDH-ES)$";

  /** Allowed JWE content encryption algorithms per SC.Security Section 3. */
  public static final String JWE_ENC_PATTERN = "^(A128CBC-HS256|A256CBC-HS512|A128GCM|A256GCM)$";

  /** ISO/IEC 6523 GLUE-URI format for a Swedish ten-digit organization number. */
  public static final String ORGANIZATION_IDENTIFIER_PATTERN = "^urn:glue:iso6523:0007:\\d{10}$";

  /** Mandatory algorithms added by the trust anchor metadata policy to *_signing_alg_values_supported fields. */
  public static final Set<String> MANDATORY_SIGNING_ALGS =
      Set.of("RS256", "RS384", "RS512", "ES256", "ES384", "ES512");

  /**
   * Scope-to-claims register, sourced from the Swedish OIDC claims specification
   * (https://www.oidc.se/specifications/swedish-oidc-claims-specification-1_0.html). An empty claim set marks a
   * scope whose claim list is not yet known (see TODOs below) - presence of the scope is still checked elsewhere,
   * but claims-completeness is skipped for it.
   */
  public static final Map<String, Set<String>> SCOPE_CLAIMS = Map.ofEntries(
      Map.entry("openid", Set.of("sub")),
      Map.entry("https://id.oidc.se/scope/naturalPersonInfo",
          Set.of("family_name", "given_name", "middle_name", "name", "birthdate")),
      Map.entry("https://id.oidc.se/scope/naturalPersonNumber",
          Set.of("https://id.oidc.se/claim/personalIdentityNumber",
              "https://id.oidc.se/claim/coordinationNumber")),
      Map.entry("https://id.oidc.se/scope/naturalPersonOrgId",
          Set.of("name", "https://id.oidc.se/claim/orgAffiliation",
              "https://id.oidc.se/claim/orgName", "https://id.oidc.se/claim/orgNumber")),
      // TODO: claim-lista saknas i tillgänglig spec-data (eIDAS-anslutningsprofilen) - fyll i vid behov.
      Map.entry("https://id.swedenconnect.se/scope/eidasNaturalPersonIdentity", Set.of()),
      // TODO: se OIDC.Sweden.Sign Section 3.2 - claim-lista saknas i tillgänglig spec-data.
      Map.entry("https://id.oidc.se/scope/sign", Set.of()),
      Map.entry("https://id.oidc.se/scope/signApproval", Set.of()));

  private SwedenConnectMetadataRules() {
  }

  @SuppressWarnings("unchecked")
  private static Collection<Object> asCollection(final Object value) {
    if (value == null) {
      return List.of();
    }
    if (value instanceof final Collection<?> collection) {
      return (Collection<Object>) collection;
    }
    return List.of(value);
  }

  private static Set<String> asStringSet(final Object value) {
    final Set<String> result = new LinkedHashSet<>();
    for (final Object element : asCollection(value)) {
      if (element != null) {
        result.add(element.toString());
      }
    }
    return result;
  }

  private static PropertyValidators.ValidationStringBuilder v(final PropertyValidators propertyValidators) {
    return propertyValidators.builder(VariabelValueResolver.defaultResolver());
  }

  /**
   * Warns for every value in {@code requiredValues} that is missing from the array-valued metadata field
   * {@code key}. If the field is entirely absent this yields one warning per required value.
   *
   * @param metadata the metadata map to read {@code key} from
   * @param key the array-valued metadata field to check
   * @param requiredValues the values that must all be present in the field
   * @param baseFieldName prefix used when reporting the field name
   * @param result the validation result to append warnings to
   */
  public static void requireSuperset(final Map<String, Object> metadata, final String key,
      final Set<String> requiredValues, final String baseFieldName, final ValidationResult result) {
    final Set<String> actual = asStringSet(metadata.get(key));
    for (final String required : requiredValues) {
      if (!actual.contains(required)) {
        result.addResult(ValidationResult.Level.WARNING, baseFieldName + key,
            "Expected value to be present in list: " + required, String.valueOf(metadata.get(key)));
      }
    }
  }

  /**
   * Warns for every value in {@code forbiddenValues} that is present in the array-valued metadata field
   * {@code key}.
   *
   * @param metadata the metadata map to read {@code key} from
   * @param key the array-valued metadata field to check
   * @param forbiddenValues the values that must not be present in the field
   * @param baseFieldName prefix used when reporting the field name
   * @param result the validation result to append warnings to
   */
  public static void requireExcludes(final Map<String, Object> metadata, final String key,
      final Set<String> forbiddenValues, final String baseFieldName, final ValidationResult result) {
    final Set<String> actual = asStringSet(metadata.get(key));
    for (final String forbidden : forbiddenValues) {
      if (actual.contains(forbidden)) {
        result.addResult(ValidationResult.Level.WARNING, baseFieldName + key,
            "Value is not allowed in list: " + forbidden, String.valueOf(metadata.get(key)));
      }
    }
  }

  /**
   * Warns unless the metadata field {@code key} is present and equal to {@code expected}.
   *
   * @param metadata the metadata map to read {@code key} from
   * @param key the metadata field to check
   * @param expected the value {@code key} must equal
   * @param baseFieldName prefix used when reporting the field name
   * @param result the validation result to append warnings to
   */
  public static void requireValueEquals(final Map<String, Object> metadata, final String key,
      final Object expected, final String baseFieldName, final ValidationResult result) {
    final Object actual = metadata.get(key);
    if (!Objects.equals(actual, expected)) {
      result.addResult(ValidationResult.Level.WARNING, baseFieldName + key,
          "Expected value to be: " + expected, String.valueOf(actual));
    }
  }

  /**
   * Warns if {@code presentKey} has a non-blank value but {@code alsoRequiredKey} does not (e.g.
   * {@code id_token_encrypted_response_enc} requires {@code id_token_encrypted_response_alg}).
   *
   * @param metadata the metadata map to read both keys from
   * @param presentKey the field whose presence triggers the dependency
   * @param alsoRequiredKey the field that must also be present when {@code presentKey} is
   * @param baseFieldName prefix used when reporting the field name
   * @param result the validation result to append warnings to
   */
  public static void requireIfPresentThenPresent(final Map<String, Object> metadata, final String presentKey,
      final String alsoRequiredKey, final String baseFieldName, final ValidationResult result) {
    final Object presentValue = metadata.get(presentKey);
    if (presentValue == null || presentValue.toString().isBlank()) {
      return;
    }
    final Object alsoRequiredValue = metadata.get(alsoRequiredKey);
    if (alsoRequiredValue == null || alsoRequiredValue.toString().isBlank()) {
      result.addResult(ValidationResult.Level.WARNING, baseFieldName + alsoRequiredKey,
          "Field is required when '" + presentKey + "' is present", "");
    }
  }

  /**
   * For every scope declared in {@code scopeKey} that has a known (non-empty) entry in {@link #SCOPE_CLAIMS}, warns
   * about any of its claims missing from {@code claimsKey}.
   *
   * @param metadata the metadata map to read both keys from
   * @param scopeKey the array-valued metadata field listing declared scopes
   * @param claimsKey the array-valued metadata field listing declared claims
   * @param baseFieldName prefix used when reporting the field name
   * @param result the validation result to append warnings to
   */
  public static void requireScopeClaims(final Map<String, Object> metadata, final String scopeKey,
      final String claimsKey, final String baseFieldName, final ValidationResult result) {
    final Set<String> declaredScopes = asStringSet(metadata.get(scopeKey));
    final Set<String> declaredClaims = asStringSet(metadata.get(claimsKey));
    for (final String scope : declaredScopes) {
      final Set<String> requiredClaims = SCOPE_CLAIMS.get(scope);
      if (requiredClaims == null || requiredClaims.isEmpty()) {
        continue;
      }
      for (final String claim : requiredClaims) {
        if (!declaredClaims.contains(claim)) {
          result.addResult(ValidationResult.Level.WARNING, baseFieldName + claimsKey,
              "Expected claim '" + claim + "' to be present since scope '" + scope + "' is declared",
              String.valueOf(metadata.get(claimsKey)));
        }
      }
    }
  }

  /**
   * Validates a multilingual claim: the base claim plus its {@code #sv} and {@code #en} variants must all be
   * present (per Sweden Connect's multilingual parameter rule).
   *
   * @param metadata the metadata map to read the claim from
   * @param baseFieldName prefix used when reporting the field name
   * @param claimName the base name of the multilingual claim, e.g. {@code organization_name}
   * @param propertyValidators the validator factory to build field checks with
   * @param result the validation result to append warnings to
   */
  public static void validateMultilingual(final Map<String, Object> metadata, final String baseFieldName,
      final String claimName, final PropertyValidators propertyValidators, final ValidationResult result) {
    final var req = v(propertyValidators).required().length(1, 1000).build();
    req.eval(baseFieldName + claimName, metadata.get(claimName)).ifPresent(result.warn());
    req.eval(baseFieldName + claimName + "#en", metadata.get(claimName + "#en")).ifPresent(result.warn());
    req.eval(baseFieldName + claimName + "#sv", metadata.get(claimName + "#sv")).ifPresent(result.warn());
  }

  /**
   * Validates the metadata fields required from both RPs and OPs: {@code organization_name} (multilingual),
   * {@code organization_identifier}, {@code organization_uri}, {@code display_name}, {@code logo_uri},
   * {@code contacts}, {@code description}, {@code policy_uri} and {@code information_uri}.
   *
   * @param metadata the metadata map to read the common fields from
   * @param baseFieldName prefix used when reporting field names
   * @param propertyValidators the validator factory to build field checks with
   * @param result the validation result to append warnings to
   */
  public static void validateCommonFields(final Map<String, Object> metadata, final String baseFieldName,
      final PropertyValidators propertyValidators, final ValidationResult result) {

    validateMultilingual(metadata, baseFieldName, "organization_name", propertyValidators, result);

    v(propertyValidators).required().matches(ORGANIZATION_IDENTIFIER_PATTERN).build()
        .eval(baseFieldName + "organization_identifier", metadata.get("organization_identifier"))
        .ifPresent(result.warn());

    v(propertyValidators).url().build()
        .eval(baseFieldName + "organization_uri", metadata.get("organization_uri"))
        .ifPresent(result.warn());

    v(propertyValidators).required().length(1, 1000).build()
        .eval(baseFieldName + "display_name", metadata.get("display_name"))
        .ifPresent(result.warn());

    v(propertyValidators).required().url().ping().build()
        .eval(baseFieldName + "logo_uri", metadata.get("logo_uri"))
        .ifPresent(result.warn());

    v(propertyValidators).required().email().build()
        .eval(baseFieldName + "contacts", metadata.get("contacts"))
        .ifPresent(result.warn());

    v(propertyValidators).length(1, 2000).build()
        .eval(baseFieldName + "description", metadata.get("description"))
        .ifPresent(result.warn());

    v(propertyValidators).url().build()
        .eval(baseFieldName + "policy_uri", metadata.get("policy_uri"))
        .ifPresent(result.warn());

    v(propertyValidators).url().build()
        .eval(baseFieldName + "information_uri", metadata.get("information_uri"))
        .ifPresent(result.warn());
  }

  /**
   * Validates that entity keys are published as required ({@code jwksRequired}/{@code jwksUriRequired} express
   * which of {@code jwks}/{@code jwks_uri} is the primary requirement for the calling entity type - at least one of
   * them must always be present), plus the optional {@code signed_jwks_uri} and, when {@code jwks} is present, its
   * key material (kid presence, no private keys).
   *
   * @param metadata the metadata map to read the key fields from
   * @param baseFieldName prefix used when reporting field names
   * @param propertyValidators the validator factory to build field checks with
   * @param jwksRequired whether {@code jwks} is the entity type's primary key publication requirement
   * @param jwksUriRequired whether {@code jwks_uri} is the entity type's primary key publication requirement
   * @param result the validation result to append warnings to
   */
  public static void validateKeyMaterial(final Map<String, Object> metadata, final String baseFieldName,
      final PropertyValidators propertyValidators, final boolean jwksRequired, final boolean jwksUriRequired,
      final ValidationResult result) {

    final Object jwks = metadata.get("jwks");
    final Object jwksUri = metadata.get("jwks_uri");
    final boolean hasJwks = jwks != null;
    final boolean hasJwksUri = jwksUri != null && !jwksUri.toString().isBlank();

    if ((jwksRequired || jwksUriRequired) && !hasJwks && !hasJwksUri) {
      result.addResult(ValidationResult.Level.WARNING, baseFieldName + (jwksUriRequired ? "jwks_uri" : "jwks"),
          "Expected one of the fields to be present: jwks or jwks_uri", "");
    }

    v(propertyValidators).entityid().ping().build()
        .eval(baseFieldName + "jwks_uri", jwksUri)
        .ifPresent(result.warn());

    v(propertyValidators).url().build()
        .eval(baseFieldName + "signed_jwks_uri", metadata.get("signed_jwks_uri"))
        .ifPresent(result.warn());

    if (hasJwks) {
      try {
        v(propertyValidators).jwks().build()
            .eval(baseFieldName + "jwks", PropertyValidators.mapper.writeValueAsString(jwks))
            .ifPresent(result.warn());
      }
      catch (final JsonProcessingException e) {
        result.addResult(ValidationResult.Level.WARNING, baseFieldName + "jwks", "InvalidJson: " + e.getMessage());
      }
    }
  }
}
