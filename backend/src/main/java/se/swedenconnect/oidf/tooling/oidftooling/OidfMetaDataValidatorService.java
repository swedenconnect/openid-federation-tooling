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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.SignedJWT;
import se.swedenconnect.oidf.tooling.domain.JWTDecoded;
import se.swedenconnect.oidf.tooling.domain.ValidationResult;
import se.swedenconnect.oidf.tooling.oidftooling.validator.DefaultMetaDataValidator;
import se.swedenconnect.oidf.tooling.oidftooling.validator.FederationMetaDataValidator;
import se.swedenconnect.oidf.tooling.oidftooling.validator.OPMetaDataValidator;
import se.swedenconnect.oidf.tooling.oidftooling.validator.RPMetaDataValidator;
import se.swedenconnect.oidf.tooling.oidftooling.validator.SamlMetaDataValidator;
import se.swedenconnect.oidf.tooling.validation.MetadataValidator;
import se.swedenconnect.oidf.tooling.validation.PropertyValidationFailException;
import se.swedenconnect.oidf.tooling.validation.PropertyValidator;
import se.swedenconnect.oidf.tooling.validation.PropertyValidators;
import se.swedenconnect.oidf.tooling.validation.VariabelValueResolver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Validator for OIDF metadata
 *
 * @author Per Fredrik Plars
 */
public class OidfMetaDataValidatorService {

  final static public String allowedSignAlg = "^(RS(256|384|512)|ES(256|384|512)|PS(256|384|512))$";
  final static String httpPattern = "^(http|https)://.*";
  final static String jwkPattern = "^[\\w-]+\\.[\\w-]+\\.[\\w-]+$";
  final static String jsonPattern = "^\\{[\\s\\S]*}$";
  final static ObjectMapper mapper = new ObjectMapper();

  final PropertyValidators propertyValidators = new PropertyValidators();
  final List<MetadataValidator> metadataValidators = List.of(new RPMetaDataValidator(),
      new OPMetaDataValidator(),
      new SamlMetaDataValidator(),
      new FederationMetaDataValidator(),
      new DefaultMetaDataValidator());

  /**
   * Validates the given metadata based on its format. The method identifies the type of metadata (URL, JWT, or JSON)
   * and applies the appropriate validation logic. Throws an exception if the metadata is null, empty, or in an unknown
   * format.
   *
   * @param metadata the metadata that needs to be validated. It should be a non-null string complying with one of
   *     the recognized formats such as URL, JWT, or plain JSON.
   * @return a {@link ValidationResult} object, containing the validation results, including success status and any
   *     applicable sub-results with warnings or errors.
   * @throws IllegalArgumentException if the input metadata is null, empty, or unrecognized.
   */
  public ValidationResult validate(final String metadata) {
    if (metadata == null || metadata.trim().isEmpty()) {
      throw new IllegalArgumentException("Input data to validate is expected");
    }

    if (metadata.matches(httpPattern)) {
      return this.validateMetadataOnline(metadata.trim());
    }

    if (metadata.matches(jwkPattern)) {
      return this.validateMetadataJWT(metadata.trim());
    }

    if (metadata.matches(jsonPattern)) {
      return this.validateMetadataPlainJson(metadata.trim());
    }

    throw new IllegalArgumentException(
        "Unknown input: " + metadata.substring(0, Math.min(metadata.length(), 50)) + "...");

  }

  protected ValidationResult validateMetadataOnline(final String entityId) {
    final String key = "url";
    final ValidationResult result = new ValidationResult();
    this.v().startsWith("https://").entityid().build().eval(key, entityId).ifPresent(result.error());
    if (!result.isSuccess()) {
      return result;
    }

    HttpURLConnection connection = null;
    try {
      final URL url = URI.create(entityId + "/.well-known/openid-federation").toURL();
      connection = (HttpURLConnection) url.openConnection();
      connection.setInstanceFollowRedirects(false);
      connection.setRequestMethod("GET");
      connection.setConnectTimeout(3000);
      connection.setReadTimeout(3000);
      connection.connect();

      final int status = connection.getResponseCode();
      if (status != 200) {
        result.addResult(ValidationResult.Level.ERROR, key,
            "Expected 200 OK but got " + status, "");
        return result;
      }

      final String contentType = connection.getContentType();
      this.v().required().matches("^application/entity-statement+jwt.*$").build().eval(key, contentType)
          .ifPresent(result.warn());
      return this.validateMetadataJWT(this.readLimitedBody(connection.getInputStream(), 10000));

    }
    catch (final IOException e) {
      result.addResult(ValidationResult.Level.ERROR, key,
          "Unable to call url:" + e.getMessage(), "");
      return result;
    }
    finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
  }

  private String readLimitedBody(final InputStream in, final int maxBytes) throws IOException {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
      final StringBuilder sb = new StringBuilder();
      int totalRead = 0;
      int c;
      while ((c = reader.read()) != -1) {
        sb.append((char) c);
        totalRead++;
        if (totalRead >= maxBytes) {
          sb.append("...");
          break;
        }
      }
      return sb.toString();
    }
  }

  protected ValidationResult validateMetadataJWT(final String jwt) {
    final String fieldName = "jwt";
    final ValidationResult result = new ValidationResult();
    this.v().length(100, 50_000).matches(jwkPattern).jwt().build().eval(fieldName, jwt).ifPresent(result.error());
    if (!result.isSuccess()) {
      return result;
    }

    try {
      final SignedJWT signedJWT = SignedJWT.parse(jwt);
      result.setValidatedData(JWTDecoded.builder()
          .payload(signedJWT.getPayload().toJSONObject())
          .header(signedJWT.getHeader().toJSONObject())
          .signature(signedJWT.getSignature().toString())
          .build());

      final JWSHeader header = signedJWT.getHeader();
      final String headerFieldName = fieldName.concat(".header");
      this.v().required()
          .matches("^entity-statement\\+jwt$")
          .build()
          .eval(headerFieldName.concat(".typ"), this.doIfNotNull(header.getType(), JOSEObjectType::getType))
          .ifPresent(result.warn());

      this.v().required()
          .matches(allowedSignAlg)
          .build()
          .eval(fieldName.concat(".alg"), this.doIfNotNull(header.getAlgorithm(), Algorithm::getName))
          .ifPresent(result.warn());

      final String kid = header.getKeyID();
      this.v().required()
          .build()
          .eval(fieldName.concat(".kid"), kid)
          .ifPresent(result.warn());

      this.validateOIDFMetadataPayload(signedJWT.getPayload().toJSONObject(), result);

      if (!result.isSuccess()) {
        return result;
      }

      final JWKSet jwkSet = JWKSet.parse((Map<String, Object>) signedJWT.getPayload().toJSONObject().get("jwks"));
      final JWKSelector selector = new JWKSelector(new JWKMatcher.Builder()
          .keyID(header.getKeyID())
          .build());

      final Optional<JWK> jwk = selector
          .select(jwkSet)
          .stream()
          .findFirst();

      if (jwk.isEmpty()) {
        result.addResult(ValidationResult.Level.ERROR, fieldName, "Unable to find key for kid", "");
      }

      final JWSVerifier verifier = switch (jwk.get().getKeyType().getValue()) {
        case "EC" -> new ECDSAVerifier(jwk.get().toECKey());
        case "RSA" -> new RSASSAVerifier(jwk.get().toRSAKey());
        case null, default -> null;
      };

      if (!signedJWT.verify(verifier)) {
        result.addResult(ValidationResult.Level.WARNING,
            fieldName.concat(".signature"), "Signature validation failed", "");

      }
    }
    catch (final ParseException e) {
      result.addResult(ValidationResult.Level.ERROR, fieldName,
          "JWTParseError" + e.getErrorOffset() + e.getMessage(), "");
    }
    catch (final JOSEException e) {
      result.addResult(ValidationResult.Level.ERROR, fieldName,
          "Signature verification problem" +
              e.getMessage(), "");

    }
    return result;

  }

  protected ValidationResult validateMetadataPlainJson(final String metadata) {

    final ValidationResult validationResult = new ValidationResult();

    try {
      final Map<String, Object> parsedMetadata =
          mapper.readValue(metadata, new TypeReference<Map<String, Object>>() {});
      validationResult.setValidatedData(JWTDecoded.builder().payload(parsedMetadata).build());
      return this.validateOIDFMetadataPayload(parsedMetadata, validationResult);
    }
    catch (final JsonProcessingException e) {
      validationResult.addResult(ValidationResult.Level.ERROR,
          "metadata", "InvalidJson: " + e.getMessage(), "");
      return validationResult;
    }

  }

  protected ValidationResult validateOIDFMetadataPayload(final Map<String, Object> metadata,
      final ValidationResult result) {
    final PropertyValidator entityID = this.v().required().entityid().build();
    final PropertyValidator req = this.v().required().build();

    entityID.eval("sub", metadata.get("sub"))
        .ifPresent(result.warn());

    entityID
        .eval("iss", metadata.get("iss"))
        .ifPresent(result.warn());

    req.eval("exp", metadata.get("exp"))
        .ifPresent(result.warn());

    req.eval("iat", metadata.get("iat"))
        .ifPresent(result.warn());

    final Optional<Long> exp = Optional.ofNullable(metadata.get("exp"))
        .map(String::valueOf)
        .map(Long::parseLong);

    final Optional<Long> iat = Optional.ofNullable(metadata.get("iat"))
        .map(String::valueOf)
        .map(Long::parseLong);

    exp.filter(expInSec -> expInSec * 1000 < System.currentTimeMillis())
        .map(aLong -> new PropertyValidationFailException("exp",
            new Date(aLong * 1000).toString(), "JWT is expired"))
        .ifPresent(result.warn());

    iat.filter(expInSec -> expInSec * 1000 > System.currentTimeMillis())
        .map(aLong -> new PropertyValidationFailException("iat",
            new Date(aLong * 1000).toString(), "JWT is issued in the future"))
        .ifPresent(result.warn());

    // todo issue a info when TTL is shorter then 24h

    try {
      this.v().required().jwks().build().eval("jwks", mapper.writeValueAsString(metadata.get("jwks")))
          .ifPresent(result.error());
    }
    catch (final JsonProcessingException e) {
      result.addResult(ValidationResult.Level.WARNING,
          "jwks", "InvalidJson: " + e.getMessage());
      return result;
    }
    if (!metadata.containsKey("metadata")) {
      result.addResult(ValidationResult.Level.ERROR, "metadata", "Missing Metadata");
    }
    else {
      final Map<String, Object> subMD = (Map<String, Object>) metadata.get("metadata");
      subMD.forEach((k, v) -> {
        this.findMetadataValidator(k).validate((Map<String, Object>) v, result);
      });

      final Object opMetadata = subMD.get("openid_provider");
      if (opMetadata instanceof final Map<?, ?> opMap) {
        final Object issuer = opMap.get("issuer");
        final Object entityId = Objects.requireNonNullElse(metadata.get("sub"), metadata.get("iss"));
        if (issuer != null && entityId != null && !issuer.equals(entityId)) {
          result.addResult(ValidationResult.Level.WARNING, "metadata.openid_provider.issuer",
              "issuer must match the Entity Identifier: " + entityId, String.valueOf(issuer));
        }
      }
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

  private MetadataValidator findMetadataValidator(final String metadataNodeName) {
    return this.metadataValidators.stream()
        .filter(v -> v.supports(metadataNodeName))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("No validator found for metadata node: " + metadataNodeName));
  }

}
