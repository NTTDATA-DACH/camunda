/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package io.camunda.operate.webapp.rest.dto.dmn;

import io.camunda.operate.entities.dmn.DecisionInstanceState;

public enum DecisionInstanceStateDto {

  FAILED,
  EVALUATED,
  UNKNOWN,
  UNSPECIFIED;

  public static DecisionInstanceStateDto getState(DecisionInstanceState state) {
    if (state == null) {
      return UNSPECIFIED;
    }
    DecisionInstanceStateDto stateDto = valueOf(state.name());
    if (stateDto == null) {
      return UNKNOWN;
    }
    return stateDto;
  }

}
