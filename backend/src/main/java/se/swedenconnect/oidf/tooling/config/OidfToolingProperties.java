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
import java.time.Duration;

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

  /**
   * Connect timeout for outbound federation calls (entity configurations, listings, fetch, resolve, discovery).
   * Kept short so one unresponsive entity does not stall the whole federation walk.
   */
  /**
   * By default, outbound requests to URLs taken from federation data must use HTTPS and must not resolve to
   * loopback/link-local/private addresses. Note: setting this to true opens a security issue - the service can then
   * be used to reach internal network addresses. Intended for local development or testing only.
   */
  private boolean allowLocalAddresses;

  private Duration httpConnectTimeout = Duration.ofSeconds(2);

  /**
   * Read timeout for outbound federation calls.
   */
  private Duration httpReadTimeout = Duration.ofSeconds(3);

  /**
   * Mandatory field validation
   */
  @PostConstruct
  public void validate() {
    Assert.notNull(this.resolverUri, "resolverUri must be set");
    Assert.notNull(this.discoveryUri, "discoveryUri must be set");
    Assert.notNull(this.trustAnchorEntityId, "trustAnchorEntityId must be set");
  }

}
