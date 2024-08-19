/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
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
package io.camunda.process.test.api.assertions;

import java.util.Map;

/** The assertion object to verify a process instance. */
public interface ProcessInstanceAssert {

  /**
   * Verifies that the process instance is active. The verification fails if the process instance is
   * completed, terminated, or not created.
   *
   * <p>The assertion waits until the process instance is created.
   *
   * @return the assertion object
   */
  ProcessInstanceAssert isActive();

  /**
   * Verifies that the process instance is completed. The verification fails if the process instance
   * is active, terminated, or not created.
   *
   * <p>The assertion waits until the process instance is ended.
   *
   * @return the assertion object
   */
  ProcessInstanceAssert isCompleted();

  /**
   * Verifies that the process instance is terminated. The verification fails if the process
   * instance is active, completed, or not created.
   *
   * <p>The assertion waits until the process instance is ended.
   *
   * @return the assertion object
   */
  ProcessInstanceAssert isTerminated();

  /**
   * Verifies that the given BPMN elements are active. The verification fails if at least one
   * element is completed, terminated, or not entered.
   *
   * <p>The assertion waits until all elements are created.
   *
   * @param elementNames the BPMN element names
   * @return the assertion object
   */
  ProcessInstanceAssert hasActiveElements(String... elementNames);

  /**
   * Verifies that the given BPMN elements are completed. The verification fails if at least one
   * element is active, terminated, or not entered.
   *
   * <p>The assertion waits until all elements are left.
   *
   * @param elementNames the BPMN element names
   * @return the assertion object
   */
  ProcessInstanceAssert hasCompletedElements(String... elementNames);

  /**
   * Verifies that the given BPMN elements are terminated. The verification fails if at least one
   * element is active, completed, or not entered.
   *
   * <p>The assertion waits until all elements are left.
   *
   * @param elementNames the BPMN element names
   * @return the assertion object
   */
  ProcessInstanceAssert hasTerminatedElements(String... elementNames);

  /**
   * Verifies that the process instance has the given variables. The verification fails if at least
   * one variable doesn't exist.
   *
   * <p>The assertion waits until all variables exist.
   *
   * @param variableNames the variable names
   * @return the assertion object
   */
  ProcessInstanceAssert hasVariableNames(String... variableNames);

  /**
   * Verifies that the process instance has the variable with the given value. The verification
   * fails if the variable doesn't exist or has a different value.
   *
   * <p>The assertion waits until the variable exists and has the given value.
   *
   * @param variableName the variable name
   * @param variableValue the variable value
   * @return the assertion object
   */
  ProcessInstanceAssert hasVariable(String variableName, Object variableValue);

  /**
   * Verifies that the process instance has the given variables. The verification fails if at least
   * one variable doesn't exist or has a different value.
   *
   * <p>The assertion waits until all variables exist and have the given value.
   *
   * @param variables the expected variables
   * @return the assertion object
   */
  ProcessInstanceAssert hasVariables(Map<String, Object> variables);
}
