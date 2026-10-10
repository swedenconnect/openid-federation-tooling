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
 * limitations under the License.
 */
package se.swedenconnect.oidf.tooling.integration;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

/**
 * Guards outbound federation requests against Server-Side Request Forgery (SSRF). URLs taken from other entities'
 * statements and metadata must use HTTPS and must not resolve to loopback, link-local, site-local, unique-local,
 * any-local or multicast addresses, so the service cannot be used to reach or scan internal networks.
 *
 * @author Per Fredrik Plars
 */
public class OutboundUrlGuard {

  private final boolean allowLocalAddresses;

  /**
   * Constructs the guard.
   *
   * @param allowLocalAddresses if {@code true}, plain HTTP and local/private addresses are allowed. Only intended
   *     for local development or testing.
   */
  public OutboundUrlGuard(final boolean allowLocalAddresses) {
    this.allowLocalAddresses = allowLocalAddresses;
  }

  /**
   * Verifies that a request to the given URI is allowed.
   *
   * @param uri the URI about to be called
   * @throws SecurityException if the URI is not allowed
   */
  public void assertSafeToConnect(final URI uri) {
    if (this.allowLocalAddresses) {
      return;
    }
    if (uri == null || !"https".equalsIgnoreCase(uri.getScheme())) {
      throw new SecurityException("Only https URLs are allowed: " + uri);
    }
    final String host = uri.getHost();
    if (host == null || host.isBlank()) {
      throw new SecurityException("URL has no host: " + uri);
    }
    final InetAddress[] addresses;
    try {
      addresses = InetAddress.getAllByName(host);
    }
    catch (final UnknownHostException e) {
      throw new SecurityException("Unable to resolve host: " + host);
    }
    for (final InetAddress address : addresses) {
      if (isLocal(address)) {
        throw new SecurityException("Host resolves to a local or private address: " + host);
      }
    }
  }

  private static boolean isLocal(final InetAddress address) {
    if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
        || address.isSiteLocalAddress() || address.isMulticastAddress()) {
      return true;
    }
    final byte[] raw = address.getAddress();
    // IPv6 unique local addresses, fc00::/7
    return raw.length == 16 && (raw[0] & 0xFE) == 0xFC;
  }

}
