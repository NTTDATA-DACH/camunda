package org.camunda.optimize.dto.optimize.query.report.single.decision.group;

import com.fasterxml.jackson.annotation.JsonValue;

import static org.camunda.optimize.dto.optimize.ReportConstants.GROUP_BY_EVALUATION_DATE_TYPE;
import static org.camunda.optimize.dto.optimize.ReportConstants.GROUP_BY_INPUT_VARIABLE_TYPE;
import static org.camunda.optimize.dto.optimize.ReportConstants.GROUP_BY_NONE_TYPE;
import static org.camunda.optimize.dto.optimize.ReportConstants.GROUP_BY_OUTPUT_VARIABLE_TYPE;

public enum DecisionGroupByType {
  EVALUATION_DATE(GROUP_BY_EVALUATION_DATE_TYPE),
  NONE(GROUP_BY_NONE_TYPE),
  INPUT_VARIABLE(GROUP_BY_INPUT_VARIABLE_TYPE),
  OUTPUT_VARIABLE(GROUP_BY_OUTPUT_VARIABLE_TYPE),
  ;

  private final String id;

  DecisionGroupByType(final String id) {
    this.id = id;
  }

  @JsonValue
  public String getId() {
    return id;
  }

  @Override
  public String toString() {
    return getId();
  }
}
