/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.dto.optimize.rest.report.measure;

import io.camunda.optimize.dto.optimize.query.report.single.result.ResultType;
import io.camunda.optimize.dto.optimize.query.report.single.result.hyper.HyperMapResultEntryDto;
import java.util.List;

public class HyperMapMeasureResponseDto extends MeasureResponseDto<List<HyperMapResultEntryDto>> {

  protected HyperMapMeasureResponseDto() {}

  // overridden to make sure the type is always available and correct for these classes
  @Override
  public ResultType getType() {
    return ResultType.HYPER_MAP;
  }
}
