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

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Config classes
 *
 * @author Per Fredrik Plars
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "openid.federation.tooling")
@Slf4j
public class OidfToolingProperties {
  private URI resolverUri;
  private URI discoveryUri;
  private EntityID trustAnchorEntityId;
  private String trustBundleName;
  private SsrfProtectionProperties ssrfProtection = new SsrfProtectionProperties();

  /**
   * Mandatory field validation
   */
  @PostConstruct
  public void validate() {
    Assert.notNull(this.resolverUri, "resolverUri must be set");
    Assert.notNull(this.discoveryUri, "discoveryUri must be set");
    Assert.notNull(this.trustAnchorEntityId, "trustAnchorEntityId must be set");
    this.ssrfProtection.validate();
  }

  /**
   * Controls the SSRF protection applied to outbound calls the service makes on behalf of user-supplied metadata
   * URLs (entity ids, {@code jwks_uri}, {@code logo_uri}, SAML {@code metadata_endpoint}, ...), so the service
   * cannot be used to reach or scan internal/private networks.
   */
  @Getter
  @Setter
  public static class SsrfProtectionProperties {
    /**
     * By default, no local/private/link-local IP address ranges can be resolved. Note: setting this to true opens a
     * security issue - the service can then be used to reach internal network addresses. Intended for local
     * development or testing only.
     */
    private boolean enableLocalIpAddressRanges;

    /**
     * Regexp block list; if the hostname of a URL the service is about to fetch matches any entry, the request is
     * denied.
     */
    private List<String> blockHostname;

    void validate() {
      Optional.ofNullable(this.blockHostname).ifPresent(patterns -> patterns.forEach(regex -> {
        try {
          Pattern.compile(regex);
        }
        catch (final PatternSyntaxException e) {
          throw new IllegalArgumentException(
              "Invalid regex in openid.federation.tooling.ssrf-protection.block-hostname: " + regex, e);
        }
      }));
    }
  }

}
