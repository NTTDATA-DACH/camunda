/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.service.util.mapper;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.camunda.optimize.dto.optimize.query.report.ReportDefinitionDto;
import org.camunda.optimize.dto.optimize.rest.AuthorizedEntityDto;
import org.camunda.optimize.dto.optimize.rest.AuthorizedReportDefinitionResponseDto;

import java.io.IOException;

public class CustomAuthorizedReportDefinitionDeserializer extends StdDeserializer<AuthorizedReportDefinitionResponseDto> {

  private ObjectMapper objectMapper;
  private CustomReportDefinitionDeserializer reportDefinitionDeserializer;

  public CustomAuthorizedReportDefinitionDeserializer(final ObjectMapper objectMapper) {
    this(ReportDefinitionDto.class);
    this.objectMapper = objectMapper;
    this.reportDefinitionDeserializer = new CustomReportDefinitionDeserializer(objectMapper);
  }

  public CustomAuthorizedReportDefinitionDeserializer(Class<?> vc) {
    super(vc);
  }

  @Override
  public AuthorizedReportDefinitionResponseDto deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
    final JsonNode node = jp.readValueAsTree();
    final ReportDefinitionDto reportDefinitionDto = reportDefinitionDeserializer.deserialize(jp, node);
    final AuthorizedEntityDto authorizedEntityDto = objectMapper.readValue(node.toString(), AuthorizedEntityDto.class);
    return new AuthorizedReportDefinitionResponseDto(reportDefinitionDto, authorizedEntityDto.getCurrentUserRole());
  }

}
