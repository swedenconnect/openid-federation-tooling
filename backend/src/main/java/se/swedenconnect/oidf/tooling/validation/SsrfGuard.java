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
package se.swedenconnect.oidf.tooling.validation;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Guards outbound HTTP calls made on behalf of user-supplied metadata URLs (e.g. {@code logo_uri},
 * {@code jwks_uri}, entity ids) against being used to reach internal/private networks (SSRF). Mirrors the
 * address-class checks used by the {@code jwksLoaderRestClient} route planner in the oidf-entity-registry
 * project (including its {@code enableLocalIpAddressRanges} escape hatch and {@code blockHostname} regexp
 * list), plus IPv4/IPv6 link-local addresses (e.g. cloud metadata endpoints such as 169.254.169.254), which
 * that route planner does not block.
 *
 * @author Per Fredrik Plars
 */
public final class SsrfGuard {

  private final boolean enableLocalIpAddressRanges;
  private final List<Pattern> blockHostnamePatterns;

  /**
   * Creates a guard with the default, most restrictive configuration: local/private address ranges are always
   * blocked and no hostname block list is applied.
   */
  public SsrfGuard() {
    this(false, List.of());
  }

  /**
   * Creates a guard configured from {@code openid.federation.tooling.ssrf-protection}.
   *
   * @param enableLocalIpAddressRanges if {@code true}, loopback/link-local/site-local/unique-local addresses are
   *     allowed through - this opens a security issue (the service can then be used for internal network calls)
   *     and should only be enabled for local development or testing
   * @param blockHostnamePatterns regular expressions matched against the hostname of a URL about to be fetched;
   *     a match denies the request
   */
  public SsrfGuard(final boolean enableLocalIpAddressRanges, final List<String> blockHostnamePatterns) {
    this.enableLocalIpAddressRanges = enableLocalIpAddressRanges;
    this.blockHostnamePatterns = Optional.ofNullable(blockHostnamePatterns).orElse(List.of()).stream()
        .map(Pattern::compile)
        .toList();
  }

  /**
   * Verifies that {@code url} is safe for this service to connect to: HTTPS only, its hostname does not match any
   * configured block pattern, and (unless local ranges are enabled) it resolves to an address that is neither
   * loopback, wildcard, link-local, site-local (RFC 1918), multicast, nor IPv6 unique-local ({@code fc00::/7}).
   *
   * @param url the URL that is about to be fetched
   * @throws SecurityException if the URL uses a disallowed scheme, matches a block pattern, or resolves to a
   *     blocked address
   */
  public void assertSafeToConnect(final URL url) {
    if (!"https".equalsIgnoreCase(url.getProtocol())) {
      throw new SecurityException("Only HTTPS is allowed, got: " + url.getProtocol());
    }

    final String hostname = url.getHost();
    for (final Pattern pattern : this.blockHostnamePatterns) {
      if (pattern.matcher(hostname).find()) {
        throw new SecurityException("Match in block list: " + hostname + " -> " + pattern.pattern());
      }
    }

    if (this.enableLocalIpAddressRanges) {
      return;
    }

    final InetAddress address;
    try {
      address = InetAddress.getByName(hostname);
    }
    catch (final UnknownHostException e) {
      throw new SecurityException("Unable to resolve host: " + hostname);
    }

    if (address.isAnyLocalAddress()
        || address.isLoopbackAddress()
        || address.isLinkLocalAddress()
        || address.isSiteLocalAddress()
        || address.isMulticastAddress()) {
      throw new SecurityException("Blocked internal/reserved address: " + address.getHostAddress());
    }

    if (address instanceof final Inet6Address ipv6Address) {
      final byte[] bytes = ipv6Address.getAddress();
      // Unique local address range fc00::/7
      if ((bytes[0] & 0xFE) == 0xFC) {
        throw new SecurityException("Blocked internal address: " + address.getHostAddress());
      }
    }
  }
}
