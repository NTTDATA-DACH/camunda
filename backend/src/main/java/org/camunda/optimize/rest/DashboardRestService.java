package org.camunda.optimize.rest;

import org.camunda.optimize.dto.optimize.query.IdDto;
import org.camunda.optimize.dto.optimize.query.dashboard.DashboardDefinitionDto;
import org.camunda.optimize.rest.providers.Secured;
import org.camunda.optimize.rest.queryparam.adjustment.QueryParamAdjustmentUtil;
import org.camunda.optimize.rest.util.AuthenticationUtil;
import org.camunda.optimize.service.es.reader.DashboardReader;
import org.camunda.optimize.service.es.writer.DashboardWriter;
import org.camunda.optimize.service.security.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.List;

import static org.camunda.optimize.rest.util.RestResponseUtil.buildServerErrorResponse;

@Secured
@Path("/dashboard")
@Component
public class DashboardRestService {

  @Autowired
  private DashboardWriter dashboardWriter;

  @Autowired
  private DashboardReader dashboardReader;

  @Autowired
  private TokenService tokenService;

  /**
   * Creates an empty dashboard.
   * @return the id of the dashboard
   */
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public IdDto createNewDashboard(@Context ContainerRequestContext requestContext) {
    String token = AuthenticationUtil.getToken(requestContext);
    String userId = tokenService.getTokenIssuer(token);
    return dashboardWriter.createNewDashboardAndReturnId(userId);
  }

  /**
   * Updates the given fields of a dashboard to the given id.
   * @param dashboardId the id of the dashboard
   * @param updatedDashboard dashboard that needs to be updated. Only the fields that are defined here are actually updated.
   */
  @PUT
  @Path("/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public Response updateDashboard(@Context ContainerRequestContext requestContext,
                           @PathParam("id") String dashboardId,
                           DashboardDefinitionDto updatedDashboard) {
    updatedDashboard.setId(dashboardId);
    String token = AuthenticationUtil.getToken(requestContext);
    String userId = tokenService.getTokenIssuer(token);
    updatedDashboard.setLastModifier(userId);
    try {
      dashboardWriter.updateDashboard(updatedDashboard);
      return Response.noContent().build();
    } catch (Exception e) {
      return buildServerErrorResponse(e);
    }
  }

  /**
   * Get a list of all available dashboards.
   * @throws IOException If there was a problem retrieving the dashboards from Elasticsearch.
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getStoredDashboards(@Context UriInfo uriInfo) throws IOException {
    try {
      List<DashboardDefinitionDto> dashboards = dashboardReader.getAllDashboards();
      MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters();
      dashboards = QueryParamAdjustmentUtil.adjustResultListAccordingToQueryParameters(dashboards, queryParameters);
      return Response.ok(dashboards, MediaType.APPLICATION_JSON).build();
    } catch (Exception e) {
      return buildServerErrorResponse(e);
    }
  }

  /**
   * Retrieve the dashboard to the specified id.
   */
  @GET
  @Path("/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getDashboards(@PathParam("id") String dashboardId) {
    try {
      return Response.ok(dashboardReader.getDashboard(dashboardId), MediaType.APPLICATION_JSON).build();
    } catch (Exception e) {
      return buildServerErrorResponse(e);
    }
  }

  /**
   * Delete the dashboard to the specified id.
   */
  @DELETE
  @Path("/{id}")
  public void deleteDashboard(@PathParam("id") String dashboardId) {
    dashboardWriter.deleteDashboard(dashboardId);
  }


}

