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

package se.swedenconnect.oidf.tooling.errorhandling;

import java.net.URI;

/**
 * Define error type constants
 *
 * @author Per Fredrik Plars
 */
public enum ErrorTypes {
  INVALID_PARAMETER("https://oidf-registry.swedenconnect.se/errors/invalid-parameter"),
  ENTITY_NOT_FOUND("https://oidf-registry.swedenconnect.se/errors/entity-not-found"),
  UNAUTHORIZED("https://oidf-registry.swedenconnect.se/errors/unauthorized"),
  FORBIDDEN("https://oidf-registry.swedenconnect.se/errors/forbidden"),
  INTERNAL_ERROR("https://oidf-registry.swedenconnect.se/errors/internal-error"),
  VALIDATION_ERROR("https://oidf-registry.swedenconnect.se/errors/validation-error"),
  JWT_SIGNING_ERROR("https://oidf-registry.swedenconnect.se/errors/jwt-signing-error");

  public final URI errorURI;

  ErrorTypes(final String errorURI) {
    this.errorURI = URI.create(errorURI);
  }

}
