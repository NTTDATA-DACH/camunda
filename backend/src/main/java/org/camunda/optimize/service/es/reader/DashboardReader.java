package org.camunda.optimize.service.es.reader;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.camunda.optimize.dto.optimize.query.dashboard.DashboardDefinitionDto;
import org.camunda.optimize.service.exceptions.OptimizeException;
import org.camunda.optimize.service.util.configuration.ConfigurationService;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class DashboardReader {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Autowired
  private Client esclient;
  @Autowired
  private ConfigurationService configurationService;
  @Autowired
  private ObjectMapper objectMapper;

  public DashboardDefinitionDto getDashboard(String dashboardId) throws IOException, OptimizeException {
    logger.debug("Fetching dashboard with id [{}]", dashboardId);
    GetResponse getResponse = esclient
      .prepareGet(
        configurationService.getOptimizeIndex(),
        configurationService.getDashboardType(),
        dashboardId
      )
      .setRealtime(false)
      .get();

    if (getResponse.isExists()) {
      String responseAsString = getResponse.getSourceAsString();
      return objectMapper.readValue(responseAsString, DashboardDefinitionDto.class);
    } else {
      logger.error("Was not able to retrieve dashboard with id [{}] from Elasticsearch.", dashboardId);
      throw new OptimizeException("Dashboard does not exist!");
    }
  }

  public List<DashboardDefinitionDto> getAllDashboards() throws IOException {
    logger.debug("Fetching all available dashboards");
    SearchResponse scrollResp = esclient
      .prepareSearch(configurationService.getOptimizeIndex())
      .setTypes(configurationService.getDashboardType())
      .setScroll(new TimeValue(configurationService.getElasticsearchScrollTimeout()))
      .setQuery(QueryBuilders.matchAllQuery())
      .setSize(100)
      .get();
    List<DashboardDefinitionDto> storedDashboards = new ArrayList<>();

    do {
      for (SearchHit hit : scrollResp.getHits().getHits()) {
        String responseAsString = hit.getSourceAsString();
        storedDashboards.add(objectMapper.readValue(responseAsString, DashboardDefinitionDto.class));
      }

      scrollResp = esclient
        .prepareSearchScroll(scrollResp.getScrollId())
        .setScroll(new TimeValue(configurationService.getElasticsearchScrollTimeout()))
        .get();
    } while (scrollResp.getHits().getHits().length != 0);

    return storedDashboards;
  }

}
