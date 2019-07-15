/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.service.importing;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.optimize.rest.engine.dto.ProcessInstanceEngineDto;
import org.camunda.optimize.test.it.rule.ElasticSearchIntegrationTestRule;
import org.camunda.optimize.test.it.rule.EmbeddedOptimizeRule;
import org.camunda.optimize.test.it.rule.EngineDatabaseRule;
import org.camunda.optimize.test.it.rule.EngineIntegrationRule;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Rule;
import org.junit.rules.RuleChain;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.camunda.optimize.service.es.schema.OptimizeIndexNameHelper.getOptimizeIndexAliasForType;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public abstract class AbstractImportIT {

  public EngineIntegrationRule engineRule = new EngineIntegrationRule();
  public ElasticSearchIntegrationTestRule elasticSearchRule = new ElasticSearchIntegrationTestRule();
  public EmbeddedOptimizeRule embeddedOptimizeRule = new EmbeddedOptimizeRule();
  public EngineDatabaseRule engineDatabaseRule = new EngineDatabaseRule();

  @Rule
  public RuleChain chain = RuleChain
    .outerRule(elasticSearchRule).around(engineRule).around(embeddedOptimizeRule).around(engineDatabaseRule);

  protected void allEntriesInElasticsearchHaveAllData(String elasticsearchType) throws IOException {
    allEntriesInElasticsearchHaveAllDataWithCount(elasticsearchType, 1L, Collections.emptySet());
  }

  protected void allEntriesInElasticsearchHaveAllData(String elasticsearchType, final Set<String> excludedFields) throws
                                                                                                                IOException {
    allEntriesInElasticsearchHaveAllDataWithCount(elasticsearchType, 1L, excludedFields);
  }


  protected void allEntriesInElasticsearchHaveAllDataWithCount(final String elasticsearchType,
                                                             final long count) throws IOException {
    allEntriesInElasticsearchHaveAllDataWithCount(elasticsearchType, count, Collections.emptySet());
  }

  protected void allEntriesInElasticsearchHaveAllDataWithCount(final String elasticsearchType,
                                                               final long count,
                                                               final Set<String> nullValueFields)
    throws IOException {
    SearchResponse idsResp = getSearchResponseForAllDocumentsOfType(elasticsearchType);

    assertThat(idsResp.getHits().getTotalHits(), is(count));
    for (SearchHit searchHit : idsResp.getHits().getHits()) {
      assertAllFieldsSet(nullValueFields, searchHit);
    }
  }

  protected void assertAllFieldsSet(final Set<String> nullValueFields, final SearchHit searchHit) {
    for (Map.Entry searchHitField : searchHit.getSourceAsMap().entrySet()) {
      if (nullValueFields.contains(searchHitField.getKey())) {
        assertThat(searchHitField.getValue(), is(nullValue()));
      } else {
        String errorMessage = "Something went wrong during fetching of field: " + searchHitField.getKey() +
          ". Should actually have a value!";
        assertThat(errorMessage, searchHitField.getValue(), is(notNullValue()));
        if (searchHitField.getValue() instanceof String) {
          String value = (String) searchHitField.getValue();
          assertThat(errorMessage, value.isEmpty(), is(false));
        }
      }
    }
  }

  protected SearchResponse getSearchResponseForAllDocumentsOfType(String elasticsearchType) throws IOException {
    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
      .query(matchAllQuery())
      .size(100);

    SearchRequest searchRequest = new SearchRequest()
      .indices(getOptimizeIndexAliasForType(elasticsearchType))
      .types(elasticsearchType)
      .source(searchSourceBuilder);

    return elasticSearchRule.getEsClient().search(searchRequest, RequestOptions.DEFAULT);
  }

  protected ProcessInstanceEngineDto deployAndStartSimpleServiceTask() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("aVariable", "aStringVariables");
    return deployAndStartSimpleServiceTaskWithVariables(variables);
  }

  protected ProcessInstanceEngineDto deployAndStartSimpleServiceTaskWithVariables(Map<String, Object> variables) {
    BpmnModelInstance processModel = createSimpleProcessDefinition();
    return engineRule.deployAndStartProcessWithVariables(processModel, variables);
  }

  protected BpmnModelInstance createSimpleProcessDefinition() {
    // @formatter:off
    return Bpmn.createExecutableProcess("aProcess")
      .camundaVersionTag("aVersionTag")
      .name("aProcessName")
      .startEvent()
      .serviceTask()
        .camundaExpression("${true}")
      .endEvent()
      .done();
    // @formatter:on
  }

  protected ProcessInstanceEngineDto deployAndStartUserTaskProcess() {
    // @formatter:off
    BpmnModelInstance processModel = Bpmn.createExecutableProcess("aProcess")
      .startEvent()
      .userTask()
      .endEvent()
      .done();
    // @formatter:on
    Map<String, Object> variables = new HashMap<>();
    variables.put("aVariable", "aStringVariable");
    return engineRule.deployAndStartProcessWithVariables(processModel, variables);
  }
}
