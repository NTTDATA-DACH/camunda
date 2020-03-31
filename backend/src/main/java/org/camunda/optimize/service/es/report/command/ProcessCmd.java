/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.service.es.report.command;

import org.camunda.optimize.dto.optimize.query.report.single.process.ProcessReportDataDto;
import org.camunda.optimize.dto.optimize.query.report.single.process.SingleProcessReportDefinitionDto;
import org.camunda.optimize.dto.optimize.query.report.single.process.result.ProcessReportResultDto;
import org.camunda.optimize.service.es.report.command.exec.ProcessReportCmdExecutionPlan;
import org.camunda.optimize.service.es.report.command.exec.builder.ReportCmdExecutionPlanBuilder;
import org.elasticsearch.search.aggregations.metrics.Stats;

import java.util.Optional;

public abstract class ProcessCmd<R extends ProcessReportResultDto> implements Command<SingleProcessReportDefinitionDto> {

  protected final ProcessReportCmdExecutionPlan<R> executionPlan;

  public ProcessCmd(final ReportCmdExecutionPlanBuilder builder) {
    this.executionPlan = buildExecutionPlan(builder);
  }

  protected abstract ProcessReportCmdExecutionPlan<R> buildExecutionPlan(final ReportCmdExecutionPlanBuilder builder);

  public Optional<Stats> calculateDateRangeForAutomaticGroupByDate(final SingleProcessReportDefinitionDto reportDefinitionDto) {
    ProcessReportDataDto reportDataDto = reportDefinitionDto.getData();
    return executionPlan.calculateDateRangeForAutomaticGroupByDate(reportDataDto);
  }

  @Override
  public String createCommandKey() {
    return executionPlan.generateCommandKey();
  }
}
