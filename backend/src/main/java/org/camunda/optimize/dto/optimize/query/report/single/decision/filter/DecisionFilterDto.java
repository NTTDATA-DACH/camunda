package org.camunda.optimize.dto.optimize.query.report.single.decision.filter;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.camunda.optimize.dto.optimize.query.report.single.filter.data.FilterDataDto;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = EvaluationDateFilterDto.class, name = "evaluationDate"),
    @JsonSubTypes.Type(value = InputVariableFilterDto.class, name = "inputVariable"),
    @JsonSubTypes.Type(value = OutputVariableFilterDto.class, name = "outputVariable"),
}
)
public abstract class DecisionFilterDto<DATA extends FilterDataDto> {
  protected DATA data;

  public DATA getData() {
    return data;
  }

  public void setData(DATA data) {
    this.data = data;
  }

  @Override
  public String toString() {
    return "DecisionFilter=" + getClass().getSimpleName();
  }
}
