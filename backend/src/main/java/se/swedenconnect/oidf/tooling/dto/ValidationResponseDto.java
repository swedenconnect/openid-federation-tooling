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
package se.swedenconnect.oidf.tooling.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Validation response from OIDC validator
 *
 * @author Per Fredrik Plars
 */
@Data
@Builder
public class ValidationResponseDto {

  private List<ValidationStatus> subResults;

  private boolean success;

  private JwtContent validatedData;

  /**
   * Represents the validation status details of an OIDC validation process.
   *
   * This class provides information about the level, type, and message of the validation result, which can be used to
   * understand the outcome of the validation operation.
   */
  @Data
  @Builder
  public static class ValidationStatus {
    String level;
    String type;
    String message;
    String originalInputValue;
  }

  /**
   * Presentation of a JWT
   */
  @Data
  @Builder
  public static class JwtContent {
    Map<String, Object> header;
    Map<String, Object> payload;
    String signature;
  }

}
