/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.tasklist.zeebeimport.v850.processors.common;

import io.camunda.tasklist.entities.TaskVariableEntity;
import io.camunda.tasklist.property.TasklistProperties;
import io.camunda.tasklist.zeebeimport.v850.record.Intent;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.value.UserTaskRecordValue;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserTaskRecordToVariableEntityMapper {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(UserTaskRecordToTaskEntityMapper.class);

  @Autowired private TasklistProperties tasklistProperties;

  public List<TaskVariableEntity> mapVariables(Record<UserTaskRecordValue> record) {
    final List<TaskVariableEntity> variables = new ArrayList<>();

    if (record.getIntent().equals(Intent.COMPLETED)) {
      final UserTaskRecordValue recordValue = record.getValue();

      final Map<String, Object> variablesMap = recordValue.getVariables();
      for (Map.Entry<String, Object> varMap : variablesMap.entrySet()) {
        final String varValue = String.valueOf(varMap.getValue());

        final TaskVariableEntity variableEntity = new TaskVariableEntity();
        variableEntity.setId(
            TaskVariableEntity.getIdBy(
                String.valueOf(recordValue.getUserTaskKey()), varMap.getKey()));
        variableEntity.setName(varMap.getKey());
        variableEntity.setTaskId(String.valueOf(recordValue.getUserTaskKey()));
        variableEntity.setValue(String.valueOf(varMap.getValue()));
        variableEntity.setPartitionId(record.getPartitionId());
        variableEntity.setTenantId(recordValue.getTenantId());
        variableEntity.setFullValue(varValue);
        if (varValue.length() > tasklistProperties.getImporter().getVariableSizeThreshold()) {
          // store preview
          variableEntity.setValue(
              varValue.substring(0, tasklistProperties.getImporter().getVariableSizeThreshold()));
          variableEntity.setIsPreview(true);
        } else {
          variableEntity.setValue(varValue);
        }
        variables.add(variableEntity);
      }
    }
    return variables;
  }
}
