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
package se.swedenconnect.oidf.tooling.domain;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import se.swedenconnect.oidf.tooling.validation.PropertyValidationFailException;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Collects result data from validation
 *
 * @author Per Fredrik Plars
 */
@ToString
public class ValidationResult {
  @Getter
  private final List<SubResult> subResults = new ArrayList<>();

  @Getter
  private boolean success = true;

  @Getter
  @Setter
  private JWTDecoded validatedData;

  /**
   * Provides a {@link Consumer} implementation that handles a {@link PropertyValidationFailException} by recording an
   * error-level subresult using the field name and message from the exception.
   *
   * @return a {@link Consumer} that processes {@link PropertyValidationFailException} and records an error-level result
   */
  public Consumer<PropertyValidationFailException> error() {
    return validationResult -> {
      this.addResult(Level.ERROR, validationResult.getFiledName(),
          validationResult.getValidationFailMessage(),
          validationResult.getInputValue());
    };
  }

  /**
   * Provides a {@link Consumer} implementation that processes a {@link PropertyValidationFailException} by recording a
   * warning-level subresult. The field name and message from the exception are extracted and used to record the
   * validation result.
   *
   * @return a {@link Consumer} that handles {@link PropertyValidationFailException} and adds a warning-level result
   */
  public Consumer<PropertyValidationFailException> warn() {
    return validationResult -> {
      this.addResult(Level.WARNING, validationResult.getFiledName(),
          validationResult.getValidationFailMessage(),
          validationResult.getInputValue());
    };
  }

  /**
   * Provides a {@link Consumer} implementation that processes a {@link PropertyValidationFailException} by recording an
   * info-level subresult. The field name and message from the exception are extracted and used to record the validation
   * result.
   *
   * @return a {@link Consumer} that handles {@link PropertyValidationFailException} and adds an info-level result
   */
  public Consumer<PropertyValidationFailException> info() {
    return validationResult -> {
      this.addResult(Level.INFO, validationResult.getFiledName(),
          validationResult.getValidationFailMessage(),
          validationResult.getInputValue());
    };
  }

  /**
   * Adds a validation result to the list of sub-results and updates the overall success state based on the severity
   * level of the result.
   *
   * @param level the severity level of the sub-result (e.g., INFO, WARNING, ERROR)
   * @param type the type or category of the validation result
   * @param validationMessage a detailed message describing the validation result
   * @param originalInputValue originalInputValue that has bin validated
   */
  public void addResult(final Level level,
      final String type,
      final String validationMessage,
      final String originalInputValue) {
    this.subResults.add(new SubResult(level, type, validationMessage, originalInputValue));
    if (level == Level.ERROR) {
      this.success = false;
    }
  }

  /**
   * Adds a validation result to the list of sub-results and updates the overall success state based on the severity
   * level of the result.
   *
   * @param level the severity level of the sub-result (e.g., INFO, WARNING, ERROR)
   * @param type the type or category of the validation result
   * @param validationMessage a detailed message describing the validation result
   */
  public void addResult(final Level level,
      final String type,
      final String validationMessage) {
    this.subResults.add(new SubResult(level, type, validationMessage, ""));
  }

  /**
   * Represents the severity level of a validation result. This enumeration is used to categorize validation outcomes
   * into three levels: INFO, WARNING, and ERROR.
   *
   * INFO: Indicates informational messages or minor issues that do not affect the overall validation success.
   *
   * WARNING: Denotes potential issues that may require attention, but do not cause a validation failure.
   *
   * ERROR: Represents serious issues that result in a failure of the validation.
   */
  public enum Level {INFO, WARNING, ERROR}

  /**
   * Represents a sub-result of a validation process, containing information about the severity level, type, and
   * detailed message of the validation outcome.
   *
   * This class is primarily used within the {@code ValidationResult} to capture and log detailed information about
   * specific validation checks that were performed. Each sub-result is categorized by its level, such as INFO, WARNING,
   * or ERROR, to indicate the severity of the outcome.
   *
   * @param level the severity level of the validation sub-result, indicating the importance or impact
   * @param type the type or category of the validation issue or result
   * @param message a detailed message describing the validation sub-result
   * @param originalInputValue orginal input value that has bin validated
   */
  public record SubResult(Level level,
      String type,
      String message,
      String originalInputValue) {}

}
