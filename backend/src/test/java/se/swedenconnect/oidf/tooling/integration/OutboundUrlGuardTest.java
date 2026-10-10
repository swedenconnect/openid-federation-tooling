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

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OutboundUrlGuardTest {

  private final OutboundUrlGuard guard = new OutboundUrlGuard(false);

  @Test
  void rejectsPlainHttp() {
    assertThrows(SecurityException.class, () -> this.guard.assertSafeToConnect(URI.create("http://example.com/x")));
  }

  @Test
  void rejectsLoopbackAndPrivateAddresses() {
    for (final String url : new String[] {
        "https://localhost/x", "https://127.0.0.1/x", "https://10.1.2.3/x", "https://192.168.0.1/x",
        "https://172.16.0.1/x", "https://169.254.169.254/latest/meta-data", "https://0.0.0.0/x",
        "https://[::1]/x", "https://[fd00::1]/x", "https://[fe80::1]/x" }) {
      assertThrows(SecurityException.class, () -> this.guard.assertSafeToConnect(URI.create(url)), url);
    }
  }

  @Test
  void rejectsMissingHostAndScheme() {
    assertThrows(SecurityException.class, () -> this.guard.assertSafeToConnect(URI.create("/relative")));
    assertThrows(SecurityException.class, () -> this.guard.assertSafeToConnect(null));
  }

  @Test
  void allowsPublicHttpsAddress() {
    assertDoesNotThrow(() -> this.guard.assertSafeToConnect(URI.create("https://8.8.8.8/x")));
  }

  @Test
  void allowLocalAddressesDisablesTheGuard() {
    final OutboundUrlGuard permissive = new OutboundUrlGuard(true);
    assertDoesNotThrow(() -> permissive.assertSafeToConnect(URI.create("http://localhost:8080/x")));
  }

}
