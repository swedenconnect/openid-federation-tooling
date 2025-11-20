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
package se.swedenconnect.oidf.tooling.config;

import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClient;
import se.swedenconnect.oidf.tooling.integration.OidfServiceIntegration;
import se.swedenconnect.oidf.tooling.oidftooling.OidfMetaDataValidatorService;
import se.swedenconnect.oidf.tooling.oidftooling.OidfToolingService;

import javax.net.ssl.SSLContext;
import java.net.http.HttpClient;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

/**
 * development-lab
 *
 * @author Per Fredrik Plars
 */
@Slf4j
@Configuration
public class OidfToolingConfig {

  /**
   * Configures and provides a {@link RestClient.Builder} bean that is used to create REST clients with custom HTTP and
   * SSL configurations based on the application settings.
   *
   * @param ssl the {@link SslBundles} instance used to obtain SSL configurations.
   * @param observationRegistry the {@link ObservationRegistry} instance for monitoring and observability.
   * @param properties thecontaining configuration properties for the registry admin, including the trust bundle
   *     alias.
   * @return a configured {@link RestClient.Builder} instance for creating customized REST clients.
   * @throws Exception if the SSL configuration fails due to invalid trust bundle alias or other SSL setup errors.
   */
  @Bean
  @Primary
  RestClient.Builder restClientBuilder(final SslBundles ssl,
      final ObservationRegistry observationRegistry,
      final OidfToolingProperties properties) throws Exception {

    final HttpClient.Builder httpClientBuilder = HttpClient.newBuilder();
    final String trustBundleAlias = properties.getTrustBundleName();

    this.settingSSLTrustContext(ssl, httpClientBuilder, trustBundleAlias);

    return RestClient.builder()
        .observationRegistry(observationRegistry)
        .requestFactory(new JdkClientHttpRequestFactory(httpClientBuilder.build()));
  }

  @Bean
  RestClient restClient(final RestClient.Builder builder) {
    return builder.build();
  }

  @Bean
  OidfToolingService oidfToolingService(final OidfToolingProperties properties,
      final OidfServiceIntegration oidfServiceIntegration) {
    return new OidfToolingService(oidfServiceIntegration, properties);
  }

  @Bean
  OidfMetaDataValidatorService oidfMetaDataValidator() {
    return new OidfMetaDataValidatorService();
  }

  private void settingSSLTrustContext(
      final SslBundles ssl,
      final HttpClient.Builder httpClientBuilder,
      final String trustBundleAlias) throws NoSuchAlgorithmException, KeyManagementException {
    if (trustBundleAlias != null && !trustBundleAlias.isBlank()) {
      final SSLContext sslContext = SSLContext.getInstance("TLS");
      final SslBundle bundle = ssl.getBundle(trustBundleAlias);
      Assert.notNull(bundle, "No spring.ssl.bundle found for alias:'%s'".formatted(trustBundleAlias));
      sslContext.init(null, bundle.getManagers().getTrustManagerFactory().getTrustManagers(),
          new java.security.SecureRandom());
      httpClientBuilder.sslContext(sslContext);
    }
    else {
      log.info("No trust bundle alias found, using plain http connector");
    }
  }

}
