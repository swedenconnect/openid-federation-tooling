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
package se.swedenconnect.oidf.tooling;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Default (Template) Project
 *
 * @author Per Fredrik Plars
 */

@ConfigurationPropertiesScan
@SpringBootApplication
public class OidfToolingApplication {

  /**
   * The main method serves as the entry point for spring Application.
   *
   * @param args an array of command-line arguments passed to the application
   */
  public static void main(final String[] args) {
    SpringApplication.run(OidfToolingApplication.class, args);
  }

}