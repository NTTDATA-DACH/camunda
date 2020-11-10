/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.dto.optimize.query.event.autogeneration;

import lombok.Builder;
import lombok.Getter;
import org.camunda.optimize.dto.optimize.query.event.process.EventTypeDto;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class AutogeneratedEventGraphDto {
  private List<EventTypeDto> startEvents;
  private List<EventTypeDto> endEvents;
  private Map<EventTypeDto, AdjacentEventTypesDto> adjacentEventTypesDtoMap;
}
