/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.it.auth;

import static io.camunda.client.protocol.rest.PermissionTypeEnum.ACCESS;
import static io.camunda.client.protocol.rest.ResourceTypeEnum.APPLICATION;
import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.client.CamundaClient;
import io.camunda.it.utils.CamundaMultiDBExtension;
import io.camunda.qa.util.auth.Authenticated;
import io.camunda.qa.util.auth.Permissions;
import io.camunda.qa.util.auth.User;
import io.camunda.qa.util.auth.UserDefinition;
import io.camunda.qa.util.cluster.TestSimpleCamundaApplication;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.List;
import org.apache.hc.core5.http.ProtocolException;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.extension.RegisterExtension;

@Tag("multi-db-test")
@DisabledIfSystemProperty(named = "test.integration.camunda.database.type", matches = "rdbms")
class ApplicationAuthorizationIT {

  private static final String PATH_OPERATE = "operate";
  private static final String PATH_OPERATE_WEBAPP_USER = PATH_OPERATE + "/user";
  private static final TestSimpleCamundaApplication STANDALONE_CAMUNDA =
      new TestSimpleCamundaApplication()
          .withBasicAuth()
          .withAuthorizationsEnabled()
          .withProperty("spring.profiles.active", "consolidated-auth,identity,broker,operate");

  @RegisterExtension
  private static final CamundaMultiDBExtension EXTENSION =
      new CamundaMultiDBExtension(STANDALONE_CAMUNDA);

  private static final String RESTRICTED = "restricted-user";
  private static final String ADMIN = "admin";
  private static final String DEFAULT_PASSWORD = "password";

  @UserDefinition
  private static final User ADMIN_USER =
      new User(
          ADMIN, DEFAULT_PASSWORD, List.of(new Permissions(APPLICATION, ACCESS, List.of("*"))));

  @UserDefinition
  private static final User RESTRICTED_USER =
      new User(
          RESTRICTED,
          DEFAULT_PASSWORD,
          List.of(new Permissions(APPLICATION, ACCESS, List.of("tasklist"))));

  @AutoClose
  private static final HttpClient HTTP_CLIENT =
      HttpClient.newBuilder().followRedirects(Redirect.NEVER).build();

  @Test
  void accessAppUserWithoutAppAccessNotAllowed(
      @Authenticated(RESTRICTED) final CamundaClient restrictedClient)
      throws IOException, ProtocolException, URISyntaxException, InterruptedException {
    // when
    final HttpRequest request =
        HttpRequest.newBuilder()
            .uri(createUri(restrictedClient, PATH_OPERATE_WEBAPP_USER))
            .header("Authorization", basicAuthentication(RESTRICTED))
            .build();
    final HttpResponse<String> response =
        HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

    // then
    assertRedirectToForbidden(response);
  }

  @Test
  void accessAppNoUserAllowed(@Authenticated(ADMIN) final CamundaClient client)
      throws IOException, URISyntaxException, InterruptedException {
    // when
    final HttpRequest request =
        HttpRequest.newBuilder().uri(createUri(client, PATH_OPERATE_WEBAPP_USER)).build();
    final HttpResponse<String> response =
        HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

    // then
    assertThat(response.statusCode()).isEqualTo(HttpURLConnection.HTTP_OK);
  }

  @Test
  void accessApiUserWithoutAppAccessAllowed(
      @Authenticated(RESTRICTED) final CamundaClient restrictedClient)
      throws IOException, URISyntaxException, InterruptedException {
    // when
    final HttpRequest request =
        HttpRequest.newBuilder()
            .uri(createUri(restrictedClient, "v2/topology"))
            .header("Authorization", basicAuthentication(RESTRICTED))
            .build();
    final HttpResponse<String> response =
        HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

    // then
    assertThat(response.statusCode()).isEqualTo(HttpURLConnection.HTTP_OK);
  }

  @Test
  void accessStaticUserWithoutAppAccessAllowed(
      @Authenticated(RESTRICTED) final CamundaClient restrictedClient)
      throws IOException, URISyntaxException, InterruptedException {
    // when
    final HttpRequest request =
        HttpRequest.newBuilder()
            .uri(createUri(restrictedClient, PATH_OPERATE + "/image.svg"))
            .header("Authorization", basicAuthentication(RESTRICTED))
            .build();
    final HttpResponse<String> response =
        HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

    // we expect not found here as frontend resources are not packaged for integration tests
    assertThat(response.statusCode()).isEqualTo(HttpURLConnection.HTTP_NOT_FOUND);
  }

  @Test
  void accessAppUserWithSpecificAppAccessAllowed(
      @Authenticated(RESTRICTED) final CamundaClient restrictedClient)
      throws IOException, URISyntaxException, InterruptedException {
    // when
    final HttpRequest request =
        HttpRequest.newBuilder()
            .uri(createUri(restrictedClient, "tasklist/user"))
            .header("Authorization", basicAuthentication(RESTRICTED))
            .build();
    final HttpResponse<String> response =
        HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

    // then
    assertThat(response.statusCode()).isEqualTo(HttpURLConnection.HTTP_OK);
  }

  @Test
  void accessAppUserWithAppWildcardAccessAllowed(
      @Authenticated(ADMIN) final CamundaClient adminClient)
      throws IOException, URISyntaxException, InterruptedException {
    // when
    final HttpRequest request =
        HttpRequest.newBuilder()
            .uri(createUri(adminClient, PATH_OPERATE_WEBAPP_USER))
            .header("Authorization", basicAuthentication(ADMIN))
            .build();
    final HttpResponse<String> response =
        HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

    // then
    assertThat(response.statusCode()).isEqualTo(HttpURLConnection.HTTP_OK);
  }

  private static void assertRedirectToForbidden(final HttpResponse<String> response) {
    assertThat(response.statusCode()).isEqualTo(HttpURLConnection.HTTP_MOVED_TEMP);
    assertThat(response.headers().firstValue("Location"))
        .isPresent()
        .get()
        .satisfies(location -> assertThat(location).endsWith(PATH_OPERATE + "/forbidden"));
  }

  private static String basicAuthentication(final String user) {
    return "Basic "
        + Base64.getEncoder().encodeToString((user + ":" + DEFAULT_PASSWORD).getBytes());
  }

  private static URI createUri(final CamundaClient client, final String path)
      throws URISyntaxException {
    return new URI("%s%s".formatted(client.getConfiguration().getRestAddress(), path));
  }
}
