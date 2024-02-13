/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under one or more contributor license agreements.
 * Licensed under a proprietary license. See the License.txt file for more information.
 * You may not use this file except in compliance with the proprietary license.
 */
package org.camunda.optimize.service.security;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import org.camunda.optimize.AbstractPlatformIT;
import org.camunda.optimize.dto.optimize.query.security.CredentialsRequestDto;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.google.common.net.HttpHeaders.CONTENT_SECURITY_POLICY;
import static com.google.common.net.HttpHeaders.STRICT_TRANSPORT_SECURITY;
import static com.google.common.net.HttpHeaders.X_CONTENT_TYPE_OPTIONS;
import static com.google.common.net.HttpHeaders.X_FRAME_OPTIONS;
import static com.google.common.net.HttpHeaders.X_XSS_PROTECTION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.camunda.optimize.AbstractIT.OPENSEARCH_PASSING;
import static org.camunda.optimize.rest.RestTestConstants.DEFAULT_PASSWORD;
import static org.camunda.optimize.rest.RestTestConstants.DEFAULT_USERNAME;

@Tag(OPENSEARCH_PASSING)
public class SecurityResponseHeadersIT extends AbstractPlatformIT {

  @Test
  public void xFrameOptionsDisabled() {
    // when
    final Response authResponse = embeddedOptimizeExtension.rootTarget("/").request().get();

    // then
    assertThat(authResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    assertThat(authResponse.getHeaderString(X_FRAME_OPTIONS)).isNull();
  }

  @Test
  public void responseContainsSecurityHeaders_https() {
    // given
    final CredentialsRequestDto entity = new CredentialsRequestDto(DEFAULT_USERNAME, DEFAULT_PASSWORD);

    // when
    Response authResponse = embeddedOptimizeExtension.securedRootTarget()
      .path("api/authentication")
      .request()
      .post(Entity.json(entity));

    // then
    defaultSecurityHeadersAreSet(authResponse);

    // then
    assertThat(authResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    assertThat(authResponse.getHeaderString(STRICT_TRANSPORT_SECURITY)).isNotNull();
  }

  @Test
  public void responseContainsSecurityHeaders_http() {
    // when
    Response authResponse = embeddedOptimizeExtension.authenticateUserRequest(DEFAULT_USERNAME, DEFAULT_PASSWORD);

    // then
    assertThat(authResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    defaultSecurityHeadersAreSet(authResponse);
  }

  private void defaultSecurityHeadersAreSet(final Response authResponse) {
    assertThat(authResponse.getHeaderString(X_XSS_PROTECTION))
      .isNotNull()
      .isEqualTo(
        embeddedOptimizeExtension.getConfigurationService()
          .getSecurityConfiguration().getResponseHeaders().getXsssProtection()
      );

    assertThat(authResponse.getHeaderString(X_CONTENT_TYPE_OPTIONS))
      .isEqualTo("nosniff");

    assertThat(authResponse.getHeaderString(CONTENT_SECURITY_POLICY))
      .isNotNull()
      .isEqualTo(
        embeddedOptimizeExtension.getConfigurationService()
          .getSecurityConfiguration().getResponseHeaders().getContentSecurityPolicy()
      );
  }


}
