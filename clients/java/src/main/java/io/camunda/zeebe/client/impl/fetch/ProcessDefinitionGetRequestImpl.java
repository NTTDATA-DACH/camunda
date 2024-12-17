/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.camunda.zeebe.client.impl.fetch;

import io.camunda.client.protocol.rest.ProcessDefinitionItem;
import io.camunda.zeebe.client.api.ZeebeFuture;
import io.camunda.zeebe.client.api.command.FinalCommandStep;
import io.camunda.zeebe.client.api.fetch.ProcessDefinitionGetRequest;
import io.camunda.zeebe.client.api.search.response.ProcessDefinition;
import io.camunda.zeebe.client.impl.http.HttpClient;
import io.camunda.zeebe.client.impl.http.HttpZeebeFuture;
import io.camunda.zeebe.client.impl.search.SearchResponseMapper;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.apache.hc.client5.http.config.RequestConfig;

public class ProcessDefinitionGetRequestImpl implements ProcessDefinitionGetRequest {

  private final HttpClient httpClient;
  private final RequestConfig.Builder httpRequestConfig;
  private final long processDefinitionKey;

  public ProcessDefinitionGetRequestImpl(
      final HttpClient httpClient, final long processDefinitionKey) {
    this.httpClient = httpClient;
    httpRequestConfig = httpClient.newRequestConfig();
    this.processDefinitionKey = processDefinitionKey;
  }

  @Override
  public FinalCommandStep<ProcessDefinition> requestTimeout(final Duration requestTimeout) {
    httpRequestConfig.setResponseTimeout(requestTimeout.toMillis(), TimeUnit.MILLISECONDS);
    return this;
  }

  @Override
  public ZeebeFuture<ProcessDefinition> send() {
    final HttpZeebeFuture<ProcessDefinition> result = new HttpZeebeFuture<>();
    httpClient.get(
        String.format("/process-definitions/%d", processDefinitionKey),
        httpRequestConfig.build(),
        ProcessDefinitionItem.class,
        SearchResponseMapper::toProcessDefinitionGetResponse,
        result);
    return result;
  }
}
