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
package se.swedenconnect.oidf.tooling.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import org.junit.jupiter.api.Test;
import se.swedenconnect.oidf.tooling.domain.ValidationResult;
import se.swedenconnect.oidf.tooling.oidftooling.OidfMetaDataValidatorService;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPublicKey;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * development-lab
 *
 * @author Per Fredrik Plars
 */
class OidfMetaDataValidatorServiceTest {

  final OidfMetaDataValidatorService validator = new OidfMetaDataValidatorService();
  @Test
  void validate() {
    final String metadata = "eyJraWQiOiI1MTljZGUzYi0xZTcyLTQxZGItYTY5NC04ZmQxZjNjNGYxMmMiLCJ0eXAiOiJlbnRpdHktc3RhdGVtZW50K2p3dCIsImFsZyI6IlJTMjU2In0.eyJzdWIiOiJodHRwczovL2Rldi5zd2VkZW5jb25uZWN0LnNlL29pZGYtdGVzdC9zZXJ2aWNlcSIsIm1ldGFkYXRhIjp7Im9wZW5pZF9yZWx5aW5nX3BhcnR5Ijp7InRva2VuX2VuZHBvaW50X2F1dGhfc2lnbmluZ19hbGciOiJSUzI1NiIsInBvc3RfbG9nb3V0X3JlZGlyZWN0X3VyaXMiOlsiaHR0cHM6Ly9kZXYuc3dlZGVuY29ubmVjdC5zZS9vaWRmLXRlc3Qvc2VydmljZXEvbG9nby5wbmciXSwiZ3JhbnRfdHlwZXMiOlsiYXV0aG9yaXphdGlvbl9jb2RlIl0sImp3a3MiOnsia2V5cyI6W3sia3R5IjoiUlNBIiwiZSI6IkFRQUIiLCJ1c2UiOiJzaWciLCJraWQiOiJhMTUyZjFjZC1mMjViLTQ3ZmUtOGIwNi0zYWVjOTUzNTdmZjgiLCJ4NWMiOlsiTUlJQzZ6Q0NBZE9nQXdJQkFnSUpBTmE2RDRxWHRCTFBNQTBHQ1NxR1NJYjNEUUVCQ3dVQU1EVXhFekFSQmdOVkJBTU1DbE5sYkdaVGFXZHVaV1F4RVRBUEJnTlZCQW9NQ0U5cFpHWlVaWE4wTVFzd0NRWURWUVFHRXdKVFJUQWVGdzB5TlRFd01UVXhNVFF5TVRkYUZ3MHlOekV3TVRVeE1UUXpNVGRhTURVeEV6QVJCZ05WQkFNTUNsTmxiR1pUYVdkdVpXUXhFVEFQQmdOVkJBb01DRTlwWkdaVVpYTjBNUXN3Q1FZRFZRUUdFd0pUUlRDQ0FTSXdEUVlKS29aSWh2Y05BUUVCQlFBRGdnRVBBRENDQVFvQ2dnRUJBS0dLaGVhRmZwYU1qZTJTdWF5VXdFa0ZTN0tCYnJCNjg4MmRYWUxrS0JNLzMvbXVtYWRRaVM0MlRyaWdqL2ExcjlSd1g5b0RxVEtwb094WEN5eU43cW9tN1F5YVNhTXJXT0dvVEs0UmJyb2ZDYmc1a1NJVG9WcndweGNOUjJwVXVoSkxnYTFyU1ZDbzYyNnBpaksrbzRsVDlhV2hkWnJRVkNZVkZIem9ES0xkT0lOZVFBZkNJZkdheW53YTJ2Y1FWci9QSHZVV2lmQmxnWGUwcWlnK1M4ZHBJdDJaZTh0RzFvUWdjSGhGMWRoSzcveGlIYnNYMVhKNzlIZkVpTUJaQW1BSzZGdElUTm1rbE9CODNZbU4zSENJZ1FteUZ4S2NxL05WaCtKSWQyODd4SS9FckVHazVNVDBPUWhSanNpSVVqbFNqWXhrM3VMTVpJZkpDcEtlb1RrQ0F3RUFBVEFOQmdrcWhraUc5dzBCQVFzRkFBT0NBUUVBRnlqQjFqcUh2enJDZ3JkM05xTm5LZkZPdTRVL0ROaHZ3L01vVlRYQTVvWGVkbGJEaEx5dUlvSjFhUEswNHhGT2cycTJhajU3OWNoc2w4L0hPN3lUY0QvNndJaXIzTFhsM25hdDhRZkFHYmhRTkxOaG9EUzJTVHdlVmg2V0J2T3VqV0R2cVF0UTZ3K1lkekE1Z25vbUpUbHlIUmFDeGpDRmovUEE0VndZMlU3QXRGMXYyQVBxemhXNDhtTCtEb1krbWs2UDZ4MEJsZUJpdU1FRE5HTTJ5R1JIT3VLdS9sUHZBTEREVENEVTVaQ3RCTVhqYVFNVkIzZERFcndMU3F4RFJiRjZpb0VaNTVVNDk1eTlBUFA1blpCYjdhTEIvM1p0Q0dXeTdiOU4vNklrcXhxQ1dxOElmZFNxTkdYKzFSWGR4cTlhOU5zcFEwaHF4SFZPbnJKSnVRPT0iXSwiYWxnIjoiUlMyNTYiLCJuIjoib1lxRjVvVi1sb3lON1pLNXJKVEFTUVZMc29GdXNIcnp6WjFkZ3VRb0V6X2YtYTZacDFDSkxqWk91S0NQOXJXdjFIQmYyZ09wTXFtZzdGY0xMSTN1cWlidERKcEpveXRZNGFoTXJoRnV1aDhKdURtUkloT2hXdkNuRncxSGFsUzZFa3VCcld0SlVLanJicW1LTXI2amlWUDFwYUYxbXRCVUpoVVVmT2dNb3QwNGcxNUFCOEloOFpyS2ZCcmE5eEJXdjg4ZTlSYUo4R1dCZDdTcUtENUx4MmtpM1psN3kwYldoQ0J3ZUVYVjJFcnZfR0lkdXhmVmNudjBkOFNJd0ZrQ1lBcm9XMGhNMmFTVTRIemRpWTNjY0lpQkNiSVhFcHlyODFXSDRraDNienZFajhTc1FhVGt4UFE1Q0ZHT3lJaFNPVktOakdUZTRzeGtoOGtLa3A2aE9RIn1dfSwiYXBwbGljYXRpb25fdHlwZSI6IndlYiIsImxvZ29fdXJpIjoiaHR0cHM6Ly9kZXYuc3dlZGVuY29ubmVjdC5zZS9vaWRmLXRlc3Qvc2VydmljZXEvbG9nby5wbmciLCJyZWRpcmVjdF91cmlzIjpbImh0dHBzOi8vZGV2LnN3ZWRlbmNvbm5lY3Quc2Uvb2lkZi10ZXN0L3NlcnZpY2VxL3Rva2VuIl0sInRva2VuX2VuZHBvaW50X2F1dGhfbWV0aG9kIjoicHJpdmF0ZV9rZXlfand0IiwidXNlcmluZm9fc2lnbmVkX3Jlc3BvbnNlX2FsZyI6IlJTMjU2IiwiY2xpZW50X25hbWUiOiJUZXN0IFNlcnZpY2U6c2VydmljZXEiLCJjb250YWN0cyI6WyJzZXJ2aWNlcUBkaWdnLnNlIl0sInJlc3BvbnNlX3R5cGVzIjpbImNvZGUiXSwiaWRfdG9rZW5fc2lnbmVkX3Jlc3BvbnNlX2FsZyI6IlJTMjU2In19LCJqd2tzIjp7ImtleXMiOlt7Imt0eSI6IlJTQSIsImUiOiJBUUFCIiwidXNlIjoic2lnIiwia2lkIjoiNTE5Y2RlM2ItMWU3Mi00MWRiLWE2OTQtOGZkMWYzYzRmMTJjIiwieDVjIjpbIk1JSUM2akNDQWRLZ0F3SUJBZ0lJZjA3M3pBQVMrdXd3RFFZSktvWklodmNOQVFFTEJRQXdOVEVUTUJFR0ExVUVBd3dLVTJWc1psTnBaMjVsWkRFUk1BOEdBMVVFQ2d3SVQybGtabFJsYzNReEN6QUpCZ05WQkFZVEFsTkZNQjRYRFRJMU1UQXhOVEV4TkRJeE4xb1hEVEkzTVRBeE5URXhORE14TjFvd05URVRNQkVHQTFVRUF3d0tVMlZzWmxOcFoyNWxaREVSTUE4R0ExVUVDZ3dJVDJsa1psUmxjM1F4Q3pBSkJnTlZCQVlUQWxORk1JSUJJakFOQmdrcWhraUc5dzBCQVFFRkFBT0NBUThBTUlJQkNnS0NBUUVBbk5zQkI0VVhqVmF2T25lVE1oMk05MUNLazU5Y09FSU4wQ1R4NGFLSlY5VDVWRVRuNzNxVCtCbEJpSmsrSVFwSWM1U2xzVzRsL1VmdnRkVG1SdEgwTnFsVzc1RG1NNlBlU29qS3oxUkNuemlDT05KZWxHMkdLY0ozUEl3TnlEbnZZaU4vaG93L3kreDJkNXhGcVVJM25WMWFWemk1NmNpc0lTRmlidThnRC9SM1FpZFBLY0V6QWFLazNlemYyc3IyZis4WUd0bWdaeCtRbkNYNUxTY0pHOVRIZmpsc2ZlUmJxUWp5V25aUXh5U0RGWVNsN01xSzVUbHJ6VE5PbURoRDljVmZZWjlUK2s3UnFuRnE1Z3hPSW1yOWNsZTlpZTZQMXQ5NmIxZkhPWGtUUU9ha1lkV3F5bno5OUZPNlE1OU13bVdmQ1djWmJJSjgyd1VzamR2NEVRSURBUUFCTUEwR0NTcUdTSWIzRFFFQkN3VUFBNElCQVFBWEtMSGZnbHA0UEtwbTZGNENSTUF0VElVSU4xNGxmNFMyT2kvNXZibjQvOHhQdTZod1JaV29FU2lWc2d2bzRoMzVuMHdydEJwczd2YktOb2crMzhlV1RQQnRuQjkzYzNvamFrTnJZNmRlVytYenY2VE51a3M0RE1XMksrMnFZQUU1UWI3VWtuemNRNVRuZkFTelJPQ3B4b2I5ckM2ZzY1eTMzL0NDczdBTXBxK2lhdGtBTFBYZWtRZExXRGZYSVJjTTA1Z0tsajJGVEtDaUwyT1hNY2hodXlvMGlCYlJINXlqN2V1enIrN2oydXdqVTloVkE1V3kwSyttYjl4Z1dCMzY4cFFDZUF6RWN0UExOVkJXZzZhOGRMUUw4bmV2elF6dkk5UUxWSnEwK0FIY0hRbXhsMDlVVnpuQ3p4TDhtbUt6ODEzTk9SMzdWTHNwN1JadTkwMzgiXSwiYWxnIjoiUlMyNTYiLCJuIjoibk5zQkI0VVhqVmF2T25lVE1oMk05MUNLazU5Y09FSU4wQ1R4NGFLSlY5VDVWRVRuNzNxVC1CbEJpSmstSVFwSWM1U2xzVzRsX1VmdnRkVG1SdEgwTnFsVzc1RG1NNlBlU29qS3oxUkNuemlDT05KZWxHMkdLY0ozUEl3TnlEbnZZaU5faG93X3kteDJkNXhGcVVJM25WMWFWemk1NmNpc0lTRmlidThnRF9SM1FpZFBLY0V6QWFLazNlemYyc3IyZi04WUd0bWdaeC1RbkNYNUxTY0pHOVRIZmpsc2ZlUmJxUWp5V25aUXh5U0RGWVNsN01xSzVUbHJ6VE5PbURoRDljVmZZWjlULWs3UnFuRnE1Z3hPSW1yOWNsZTlpZTZQMXQ5NmIxZkhPWGtUUU9ha1lkV3F5bno5OUZPNlE1OU13bVdmQ1djWmJJSjgyd1VzamR2NEVRIn1dfSwiaXNzIjoiaHR0cHM6Ly9kZXYuc3dlZGVuY29ubmVjdC5zZS9vaWRmLXRlc3Qvc2VydmljZXEiLCJhdXRob3JpdHlfaGludHMiOlsiaHR0cHM6Ly9kZXYuc3dlZGVuY29ubmVjdC5zZS9vaWRmL2Rvcm90ZWEvaW0iXSwiZXhwIjoxNzYyMjU1NTM2LCJpYXQiOjE3NjIyNTQ2MzYsInRydXN0X21hcmtzIjpbeyJ0cnVzdF9tYXJrIjoiZXlKcmFXUWlPaUpUUzNZNGFqUllhblp2ZDAxdlZITlpOamRqYURaSFRWTk1OWFpRY1hOSFl6Vk9jMnRmVGt3dGQxUnJJaXdpZEhsd0lqb2lkSEoxYzNRdGJXRnlheXRxZDNRaUxDSmhiR2NpT2lKRlV6STFOaUo5LmV5SnBjM01pT2lKb2RIUndjem92TDJSbGRpNXpkMlZrWlc1amIyNXVaV04wTG5ObEwyOXBaR1l2YzJNdmRHMXBJaXdpYzNWaUlqb2lhSFIwY0hNNkx5OWtaWFl1YzNkbFpHVnVZMjl1Ym1WamRDNXpaUzl2YVdSbUxYUmxjM1F2YzJWeWRtbGpaWEVpTENKMGNuVnpkRjl0WVhKclgybGtJam9pYUhSMGNITTZMeTlrWlhZdWMzZGxaR1Z1WTI5dWJtVmpkQzV6WlM5dmFXUm1MM1J0TDJOc2FXVnVkQ0lzSW1WNGNDSTZNVGMyTWpJMU9ESXpOaXdpYVdGMElqb3hOell5TWpVME5qTTJMQ0pxZEdraU9pSm1Zak01TXpobVl6YzJZbUl5WWpsaE5qVmlaak5tWm1VMll6Y3haREZtTUNKOS5oTHF4WVEyMjJINGJyOWw1Y2x3eThEOGlQM01vbzNXSEtpY3hYNXd6c2dJZUVLMUhTbzJGbzl0dS1fQlVJRjJnMVhvdjRuMElaWllhZHp0TXVSSm8zUSIsImlkIjoiaHR0cHM6Ly9kZXYuc3dlZGVuY29ubmVjdC5zZS9vaWRmL3RtL2NsaWVudCJ9XX0.K2E3LIB7PRfW67GDopRTnSSHiU3kftgx7Khp_PXzno6lhd89QkZdx0EkxMS14lwIrLPbRW1kOQKZqmdZ_jayxsmkZ1XwlWud4Ga2ZNu2eW787HXqX2LzIXoLgTILXlTsQhbzti6IEZOJwihYwrzTLeQ2qatCSWc-cOLp0oTwvdgCXIf-VvGoQqnUl8J8TZ4ch9T6VhgWr0DXeEByTWPcpu6I8N64SIsMPlWeZCZe0VZi0CBBuIT-mFo_XoQgolhbjaNkCJc4dJUVrt2TU4TzFRvLN0pjoolK0NvNvYYtZR43fNaT4zQp0KyY5MHpFdd9ymnyEQnzNol0kiEvPvGH1Q";
    final ValidationResult res = validator.validate(metadata);
    assertNotNull(res);
    assertTrue(res.isSuccess());
    System.out.println(res);



  }

  @Test
  void validate_openidProvider() throws Exception {
    final ObjectMapper mapper = new ObjectMapper();
    final long now = Instant.now().getEpochSecond();

    final Map<String, Object> opMetadata = new HashMap<>();
    opMetadata.put("organization_name", "Test Org");
    opMetadata.put("organization_name#en", "Test Org");
    opMetadata.put("organization_name#sv", "Test Org");
    opMetadata.put("organization_identifier", "urn:glue:iso6523:0007:1234567890");
    opMetadata.put("display_name", "Test OP");
    opMetadata.put("logo_uri", "https://example.com/logo.svg");
    opMetadata.put("contacts", List.of("op@example.com"));
    opMetadata.put("jwks_uri", "https://example.com/op/jwks");
    opMetadata.put("issuer", "https://example.com/op");
    opMetadata.put("authorization_endpoint", "https://example.com/op/authorize");
    opMetadata.put("token_endpoint", "https://example.com/op/token");
    opMetadata.put("userinfo_endpoint", "https://example.com/op/userinfo");
    opMetadata.put("ui_locales_supported", List.of("en", "sv"));
    opMetadata.put("scopes_supported", List.of("openid"));
    opMetadata.put("claims_supported", List.of("sub"));
    opMetadata.put("response_types_supported", List.of("code"));
    opMetadata.put("acr_values_supported", List.of("https://id.oidc.se/loa/loa3"));
    opMetadata.put("subject_types_supported", List.of("public", "pairwise"));
    opMetadata.put("token_endpoint_auth_methods_supported", List.of("private_key_jwt"));
    opMetadata.put("claims_parameter_supported", true);
    opMetadata.put("request_parameter_supported", true);
    opMetadata.put("request_uri_parameter_supported", true);
    opMetadata.put("code_challenge_methods_supported", List.of("S256"));
    opMetadata.put("client_registration_types_supported", List.of("automatic"));
    opMetadata.put("id_token_signing_alg_values_supported",
        List.of("RS256", "RS384", "RS512", "ES256", "ES384", "ES512"));
    opMetadata.put("token_endpoint_auth_signing_alg_values_supported",
        List.of("RS256", "RS384", "RS512", "ES256", "ES384", "ES512"));

    final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
    keyGen.initialize(Curve.P_256.toECParameterSpec());
    final KeyPair keyPair = keyGen.generateKeyPair();
    final ECKey publicKey = new ECKey.Builder(Curve.P_256, (ECPublicKey) keyPair.getPublic())
        .keyID("test-kid").build();

    final Map<String, Object> entityStatement = new HashMap<>();
    entityStatement.put("sub", "https://example.com/op");
    entityStatement.put("iss", "https://example.com/op");
    entityStatement.put("exp", now + 3600);
    entityStatement.put("iat", now - 60);
    entityStatement.put("jwks", new JWKSet(publicKey).toJSONObject());
    entityStatement.put("metadata", Map.of("openid_provider", opMetadata));

    final String metadata = mapper.writeValueAsString(entityStatement);
    final ValidationResult res = validator.validate(metadata);

    assertNotNull(res);
    assertTrue(res.isSuccess());
  }
}