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

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URL;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SsrfGuardTest {

  private final SsrfGuard defaultGuard = new SsrfGuard();

  @Test
  void blocksNonHttpsScheme() {
    assertThrows(SecurityException.class, () -> this.defaultGuard.assertSafeToConnect(url("http://8.8.8.8/")));
  }

  @Test
  void blocksLoopback() {
    assertThrows(SecurityException.class, () -> this.defaultGuard.assertSafeToConnect(url("https://127.0.0.1/")));
  }

  @Test
  void blocksRfc1918PrivateRanges() {
    assertThrows(SecurityException.class, () -> this.defaultGuard.assertSafeToConnect(url("https://10.0.0.1/")));
    assertThrows(SecurityException.class, () -> this.defaultGuard.assertSafeToConnect(url("https://172.16.0.1/")));
    assertThrows(SecurityException.class, () -> this.defaultGuard.assertSafeToConnect(url("https://192.168.1.1/")));
  }

  @Test
  void blocksLinkLocalCloudMetadataAddress() {
    assertThrows(SecurityException.class,
        () -> this.defaultGuard.assertSafeToConnect(url("https://169.254.169.254/")));
  }

  @Test
  void blocksMulticast() {
    assertThrows(SecurityException.class, () -> this.defaultGuard.assertSafeToConnect(url("https://224.0.0.1/")));
  }

  @Test
  void blocksIpv6UniqueLocalAndLinkLocal() {
    assertThrows(SecurityException.class, () -> this.defaultGuard.assertSafeToConnect(url("https://[fc00::1]/")));
    assertThrows(SecurityException.class, () -> this.defaultGuard.assertSafeToConnect(url("https://[fd12::1]/")));
    assertThrows(SecurityException.class, () -> this.defaultGuard.assertSafeToConnect(url("https://[fe80::1]/")));
  }

  @Test
  void allowsPublicAddresses() {
    assertDoesNotThrow(() -> this.defaultGuard.assertSafeToConnect(url("https://8.8.8.8/")));
    assertDoesNotThrow(() -> this.defaultGuard.assertSafeToConnect(url("https://[2001:4860:4860::8888]/")));
  }

  @Test
  void enableLocalIpAddressRangesAllowsPrivateAddresses() {
    final SsrfGuard permissiveGuard = new SsrfGuard(true, List.of());
    assertDoesNotThrow(() -> permissiveGuard.assertSafeToConnect(url("https://127.0.0.1/")));
    assertDoesNotThrow(() -> permissiveGuard.assertSafeToConnect(url("https://10.0.0.1/")));
    assertDoesNotThrow(() -> permissiveGuard.assertSafeToConnect(url("https://169.254.169.254/")));
  }

  @Test
  void enableLocalIpAddressRangesStillEnforcesHttps() {
    final SsrfGuard permissiveGuard = new SsrfGuard(true, List.of());
    assertThrows(SecurityException.class, () -> permissiveGuard.assertSafeToConnect(url("http://10.0.0.1/")));
  }

  @Test
  void blockHostnameDeniesMatchingHost() {
    final SsrfGuard guard = new SsrfGuard(false, List.of("\\.internal\\.example\\.com$"));
    assertThrows(SecurityException.class,
        () -> guard.assertSafeToConnect(url("https://service.internal.example.com/")));
  }

  @Test
  void blockHostnameAllowsNonMatchingHost() {
    final SsrfGuard guard = new SsrfGuard(false, List.of("\\.internal\\.example\\.com$"));
    assertDoesNotThrow(() -> guard.assertSafeToConnect(url("https://8.8.8.8/")));
  }

  private static URL url(final String value) throws Exception {
    return URI.create(value).toURL();
  }
}
