/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.rest;

import com.google.common.collect.Lists;
import org.camunda.optimize.AbstractIT;
import org.camunda.optimize.dto.optimize.DecisionDefinitionOptimizeDto;
import org.camunda.optimize.dto.optimize.DefinitionType;
import org.camunda.optimize.dto.optimize.ProcessDefinitionOptimizeDto;
import org.camunda.optimize.dto.optimize.TenantDto;
import org.camunda.optimize.dto.optimize.query.collection.CollectionScopeEntryDto;
import org.camunda.optimize.dto.optimize.query.definition.DefinitionKeyResponseDto;
import org.camunda.optimize.dto.optimize.rest.DefinitionVersionResponseDto;
import org.camunda.optimize.dto.optimize.rest.TenantResponseDto;
import org.camunda.optimize.exception.OptimizeIntegrationTestException;
import org.camunda.optimize.service.TenantService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.camunda.optimize.dto.optimize.DefinitionType.DECISION;
import static org.camunda.optimize.test.it.extension.EmbeddedOptimizeExtension.DEFAULT_ENGINE_ALIAS;
import static org.camunda.optimize.upgrade.es.ElasticsearchConstants.DECISION_DEFINITION_INDEX_NAME;
import static org.camunda.optimize.upgrade.es.ElasticsearchConstants.PROCESS_DEFINITION_INDEX_NAME;
import static org.camunda.optimize.upgrade.es.ElasticsearchConstants.TENANT_INDEX_NAME;

public class DefinitionRestServiceWithCollectionScopeIT extends AbstractIT {

  private static final String TENANT_NOT_DEFINED_ID = TenantService.TENANT_NOT_DEFINED.getId();
  private static final String TENANT_ID_1 = "tenant1";
  private static final String TENANT_ID_2 = "tenant2";
  private static final String TENANT_ID_3 = "tenant3";

  @ParameterizedTest
  @EnumSource(DefinitionType.class)
  public void getDefinitionKeysByType_invalidCollectionId(final DefinitionType type) {
    // when
    final Response response = embeddedOptimizeExtension
      .getRequestExecutor()
      .buildGetDefinitionKeysByType(type.getId(), "invalid")
      .execute();

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
  }

  @ParameterizedTest
  @EnumSource(DefinitionType.class)
  public void getDefinitionKeysByType_multipleInScope(final DefinitionType type) {
    // given
    final String definitionKey1 = "definitionKey1";
    createDefinition(type, definitionKey1, "1", null, "the name");
    final String definitionKey2 = "definitionKey2";
    createDefinition(type, definitionKey2, "1", null, "the name");
    // one definition that will not be in the scope
    createDefinition(type, "otherKey", "1", null, "the name");
    final String collectionId = collectionClient.createNewCollection();
    final List<String> scopeTenantIds = Collections.singletonList(TENANT_NOT_DEFINED_ID);
    collectionClient.addScopeEntryToCollection(
      collectionId, new CollectionScopeEntryDto(type, definitionKey1, scopeTenantIds)
    );
    collectionClient.addScopeEntryToCollection(
      collectionId, new CollectionScopeEntryDto(type, definitionKey2, scopeTenantIds)
    );

    elasticSearchIntegrationTestExtension.refreshAllOptimizeIndices();

    // when
    final List<DefinitionKeyResponseDto> definitionKeys = definitionClient.getDefinitionKeysByType(type, collectionId);

    // then
    assertThat(definitionKeys)
      .extracting(DefinitionKeyResponseDto::getKey)
      .containsExactlyInAnyOrder(definitionKey1, definitionKey2);
  }

  @ParameterizedTest
  @EnumSource(DefinitionType.class)
  public void getDefinitionKeysByType_deletedNotIncludedInResponse(final DefinitionType type) {
    // given
    final String definitionKey1 = "definitionKey1";
    createDefinition(type, definitionKey1, "1", null, "the name", false);
    // deleted definition type will be excluded
    final String definitionKey2 = "definitionKey2";
    createDefinition(type, definitionKey2, "2", null, "the name", true);
    final String collectionId = collectionClient.createNewCollection();
    final List<String> scopeTenantIds = Collections.singletonList(TENANT_NOT_DEFINED_ID);
    collectionClient.addScopeEntryToCollection(
      collectionId, new CollectionScopeEntryDto(type, definitionKey1, scopeTenantIds)
    );
    collectionClient.addScopeEntryToCollection(
      collectionId, new CollectionScopeEntryDto(type, definitionKey2, scopeTenantIds)
    );

    elasticSearchIntegrationTestExtension.refreshAllOptimizeIndices();

    // when
    final List<DefinitionKeyResponseDto> definitionKeys = definitionClient.getDefinitionKeysByType(type, collectionId);

    // then
    assertThat(definitionKeys)
      .extracting(DefinitionKeyResponseDto::getKey)
      .containsExactlyInAnyOrder(definitionKey1);
  }

  @ParameterizedTest
  @EnumSource(DefinitionType.class)
  public void getDefinitionKeysByType_sharedDefinitionOnlySpecificTenantInScope(final DefinitionType type) {
    // given
    createTenant(TENANT_ID_1);

    final String definitionKey = "definitionKey1";
    createDefinition(type, definitionKey, "1", null, "the name");

    // we have a collection for which only a specific tenant is in the scope
    final List<String> scopeTenantIds = Lists.newArrayList(TENANT_ID_1);
    final String collectionId = collectionClient.createNewCollection();
    collectionClient.addScopeEntryToCollection(
      collectionId,
      new CollectionScopeEntryDto(type, definitionKey, scopeTenantIds)
    );

    elasticSearchIntegrationTestExtension.refreshAllOptimizeIndices();

    // when we get definition keys for the scope of the collection
    final List<DefinitionKeyResponseDto> definitionKeys = definitionClient.getDefinitionKeysByType(type, collectionId);

    // then the definition key is still returned although the definition only exists for the not defined tenant
    assertThat(definitionKeys)
      .extracting(DefinitionKeyResponseDto::getKey)
      .containsExactlyInAnyOrder(definitionKey);
  }

  @Test
  public void getDefinitionKeysByType_camundaEventImportedOnlyNotAllowedInCollectionContext() {
    // given
    final String collectionId = collectionClient.createNewCollection();

    // when
    final Response response = embeddedOptimizeExtension.getRequestExecutor()
      .buildGetDefinitionKeysByType(DECISION.getId(), collectionId, true)
      .execute();

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
  }

  @ParameterizedTest
  @EnumSource(DefinitionType.class)
  public void getDefinitionKeysByType_emptyScope(final DefinitionType type) {
    // given
    final String definitionKey1 = "definitionKey1";
    createDefinition(type, definitionKey1, "1", null, "the name");
    final String collectionId = collectionClient.createNewCollection();

    elasticSearchIntegrationTestExtension.refreshAllOptimizeIndices();

    // when
    final List<DefinitionKeyResponseDto> definitionKeys = definitionClient.getDefinitionKeysByType(type, collectionId);

    // then
    assertThat(definitionKeys).isEmpty();
  }

  @ParameterizedTest
  @EnumSource(DefinitionType.class)
  public void getDefinitionKeysByType_typesAreIsolated(final DefinitionType type) {
    // given
    final String definitionKey1 = "definitionKey1";
    createDefinition(type, definitionKey1, "1", null, "process");
    // also create a definition of another type
    final DefinitionType otherDefinitionType = Arrays.stream(DefinitionType.values())
      .filter(value -> !type.equals(value))
      .findFirst()
      .orElseThrow(OptimizeIntegrationTestException::new);
    createDefinition(otherDefinitionType, definitionKey1, "1", null, "other");
    final String collectionId = collectionClient.createNewCollection();
    final List<String> scopeTenantIds = Collections.singletonList(TENANT_NOT_DEFINED_ID);
    collectionClient.addScopeEntryToCollection(
      collectionId, new CollectionScopeEntryDto(type, definitionKey1, scopeTenantIds)
    );

    elasticSearchIntegrationTestExtension.refreshAllOptimizeIndices();

    // when definition keys of the other type are requested
    final List<DefinitionKeyResponseDto> definitionKeys = definitionClient
      .getDefinitionKeysByType(otherDefinitionType, collectionId);

    // then none are returned
    assertThat(definitionKeys).isEmpty();
  }

  @ParameterizedTest
  @EnumSource(DefinitionType.class)
  public void getDefinitionVersionsByKeyAndType_invalidCollectionId(final DefinitionType type) {
    // given
    final String definitionKey1 = "definitionKey1";
    createDefinition(type, definitionKey1, "1", null, "the name");

    // when
    final Response response = embeddedOptimizeExtension
      .getRequestExecutor()
      .buildGetDefinitionVersionsByTypeAndKeyRequest(type.getId(), definitionKey1, "invalid")
      .execute();

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
  }

  @ParameterizedTest
  @EnumSource(DefinitionType.class)
  public void getDefinitionVersionsByKeyAndType_multipleInScope(final DefinitionType type) {
    // given
    final String definitionKey1 = "definitionKey1";
    createDefinition(type, definitionKey1, "1", null, "the name");
    createDefinition(type, definitionKey1, "2", null, "the name");
    createDefinition(type, "otherKey", "1", null, "the name");
    // create more versions of other type, should not affect result
    final DefinitionType otherDefinitionType = Arrays.stream(DefinitionType.values())
      .filter(value -> !type.equals(value))
      .findFirst()
      .orElseThrow(OptimizeIntegrationTestException::new);
    createDefinition(otherDefinitionType, definitionKey1, "1", null, "other");
    createDefinition(otherDefinitionType, definitionKey1, "2", null, "other");
    createDefinition(otherDefinitionType, definitionKey1, "3", null, "other");
    final String collectionId = collectionClient.createNewCollection();
    final List<String> scopeTenantIds = Collections.singletonList(TENANT_NOT_DEFINED_ID);
    collectionClient.addScopeEntryToCollection(
      collectionId, new CollectionScopeEntryDto(type, definitionKey1, scopeTenantIds)
    );
    collectionClient.addScopeEntryToCollection(
      collectionId, new CollectionScopeEntryDto(otherDefinitionType, definitionKey1, scopeTenantIds)
    );
    elasticSearchIntegrationTestExtension.refreshAllOptimizeIndices();

    // when
    final List<DefinitionVersionResponseDto> versions = definitionClient
      .getDefinitionVersionsByTypeAndKey(type, definitionKey1, collectionId);

    // then
    assertThat(versions).extracting(DefinitionVersionResponseDto::getVersion).containsExactly("2", "1");
  }

  @ParameterizedTest
  @EnumSource(DefinitionType.class)
  public void getDefinitionVersionsByKeyAndType_deletedDefinitionsExcludedFromResult(final DefinitionType type) {
    // given
    final String definitionKey1 = "definitionKey1";
    createDefinition(type, definitionKey1, "1", null, "the name", true);
    createDefinition(type, definitionKey1, "2", null, "the name", false);

    final String collectionId = collectionClient.createNewCollection();
    final List<String> scopeTenantIds = Collections.singletonList(TENANT_NOT_DEFINED_ID);
    collectionClient.addScopeEntryToCollection(
      collectionId, new CollectionScopeEntryDto(type, definitionKey1, scopeTenantIds)
    );
    elasticSearchIntegrationTestExtension.refreshAllOptimizeIndices();

    // when
    final List<DefinitionVersionResponseDto> versions = definitionClient
      .getDefinitionVersionsByTypeAndKey(type, definitionKey1, collectionId);

    // then
    assertThat(versions).extracting(DefinitionVersionResponseDto::getVersion).containsExactly("2");
  }

  @ParameterizedTest
  @EnumSource(DefinitionType.class)
  public void getDefinitionVersionsByKeyAndType_multiTenant_specificDefinitions(final DefinitionType type) {
    // given
    createTenant(TENANT_ID_1);
    createTenant(TENANT_ID_2);
    createTenant(TENANT_ID_3);
    final String definitionKey1 = "definitionKey1";
    createDefinition(type, definitionKey1, "1", TENANT_ID_1, "the name");
    createDefinition(type, definitionKey1, "1", TENANT_ID_2, "the name");
    createDefinition(type, definitionKey1, "2", TENANT_ID_2, "the name");
    createDefinition(type, definitionKey1, "3", TENANT_ID_3, "the name");
    final String collectionId = collectionClient.createNewCollection();
    final List<String> scopeTenantIds = Lists.newArrayList(TENANT_ID_1, TENANT_ID_2);
    collectionClient.addScopeEntryToCollection(
      collectionId, new CollectionScopeEntryDto(type, definitionKey1, scopeTenantIds)
    );
    elasticSearchIntegrationTestExtension.refreshAllOptimizeIndices();

    // when
    final List<DefinitionVersionResponseDto> versions = definitionClient
      .getDefinitionVersionsByTypeAndKey(type, definitionKey1, collectionId);

    // then
    assertThat(versions).extracting(DefinitionVersionResponseDto::getVersion).containsExactly("2", "1");
  }

  @ParameterizedTest
  @EnumSource(DefinitionType.class)
  public void getDefinitionVersionsByKeyAndType_multiTenant_sharedDefinition(final DefinitionType type) {
    // given
    createTenant(TENANT_ID_1);
    createTenant(TENANT_ID_2);
    createTenant(TENANT_ID_3);
    final String definitionKey1 = "definitionKey1";
    createDefinition(type, definitionKey1, "1", null, "the name");
    createDefinition(type, definitionKey1, "2", null, "the name");
    createDefinition(type, definitionKey1, "3", null, "the name");
    createDefinition(type, definitionKey1, "4", null, "the name");
    final String collectionId = collectionClient.createNewCollection();
    final List<String> scopeTenantIds = Lists.newArrayList(TENANT_ID_1, TENANT_ID_2);
    collectionClient.addScopeEntryToCollection(
      collectionId, new CollectionScopeEntryDto(type, definitionKey1, scopeTenantIds)
    );
    elasticSearchIntegrationTestExtension.refreshAllOptimizeIndices();

    // when
    final List<DefinitionVersionResponseDto> versions = definitionClient
      .getDefinitionVersionsByTypeAndKey(type, definitionKey1, collectionId);

    // then
    assertThat(versions).extracting(DefinitionVersionResponseDto::getVersion).containsExactly("4", "3", "2", "1");
  }

  @ParameterizedTest
  @EnumSource(DefinitionType.class)
  public void getDefinitionVersionsByKeyAndType_multiTenant_sharedAndSpecificDefinitions(final DefinitionType type) {
    // given
    createTenant(TENANT_ID_1);
    createTenant(TENANT_ID_2);
    createTenant(TENANT_ID_3);
    final String definitionKey1 = "definitionKey1";
    createDefinition(type, definitionKey1, "1", null, "the name");
    createDefinition(type, definitionKey1, "1", TENANT_ID_1, "the name");
    createDefinition(type, definitionKey1, "1", TENANT_ID_2, "the name");
    createDefinition(type, definitionKey1, "2", null, "the name");
    createDefinition(type, definitionKey1, "2", TENANT_ID_1, "the name");
    createDefinition(type, definitionKey1, "2", TENANT_ID_2, "the name");
    createDefinition(type, definitionKey1, "3", TENANT_ID_1, "the name");
    createDefinition(type, definitionKey1, "4", TENANT_ID_3, "the name");
    final String collectionId = collectionClient.createNewCollection();
    final List<String> scopeTenantIds = Lists.newArrayList(TENANT_ID_1, TENANT_ID_2);
    collectionClient.addScopeEntryToCollection(
      collectionId, new CollectionScopeEntryDto(type, definitionKey1, scopeTenantIds)
    );
    elasticSearchIntegrationTestExtension.refreshAllOptimizeIndices();

    // when
    final List<DefinitionVersionResponseDto> versions = definitionClient
      .getDefinitionVersionsByTypeAndKey(type, definitionKey1, collectionId);

    // then
    assertThat(versions).extracting(DefinitionVersionResponseDto::getVersion).containsExactly("3", "2", "1");
  }

  @ParameterizedTest
  @EnumSource(DefinitionType.class)
  public void getDefinitionVersionsByKeyAndType_keyNotInScope(final DefinitionType type) {
    // given
    final String definitionKey1 = "definitionKey1";
    createDefinition(type, definitionKey1, "1", null, "the name");
    final String definitionKey2 = "definitionKey2";
    createDefinition(type, definitionKey2, "1", null, "the name");
    // create definitionKey2 of other type, should not affect result
    final DefinitionType otherDefinitionType = Arrays.stream(DefinitionType.values())
      .filter(value -> !type.equals(value))
      .findFirst()
      .orElseThrow(OptimizeIntegrationTestException::new);
    createDefinition(otherDefinitionType, definitionKey2, "1", null, "other");
    final String collectionId = collectionClient.createNewCollection();
    final List<String> scopeTenantIds = Lists.newArrayList(TENANT_NOT_DEFINED_ID);
    collectionClient.addScopeEntryToCollection(
      collectionId, new CollectionScopeEntryDto(type, definitionKey2, scopeTenantIds)
    );
    elasticSearchIntegrationTestExtension.refreshAllOptimizeIndices();

    // when trying to get versions for a definition key that is not in the scope
    final Response responseForWrongKey = embeddedOptimizeExtension
      .getRequestExecutor()
      .buildGetDefinitionVersionsByTypeAndKeyRequest(type.getId(), definitionKey1, collectionId)
      .execute();

    // then a 404 is returned
    assertThat(responseForWrongKey.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());

    // when trying to get versions for a definition type that is not in the scope but key that is in the scope
    final Response responseForWrongType = embeddedOptimizeExtension
      .getRequestExecutor()
      .buildGetDefinitionVersionsByTypeAndKeyRequest(otherDefinitionType.getId(), definitionKey2, collectionId)
      .execute();

    // then a 404 is returned
    assertThat(responseForWrongType.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
  }

  @ParameterizedTest
  @EnumSource(DefinitionType.class)
  public void getDefinitionTenantsByTypeKeyAndVersions_invalidCollectionId(final DefinitionType type) {
    // given
    final String definitionKey1 = "definitionKey1";
    createDefinition(type, definitionKey1, "1", null, "the name");

    // when
    final Response response = embeddedOptimizeExtension
      .getRequestExecutor()
      .buildResolveDefinitionTenantsByTypeKeyAndVersionsRequest(
        type.getId(), definitionKey1, Lists.newArrayList("1"), "invalid"
      )
      .execute();

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
  }

  @ParameterizedTest
  @EnumSource(DefinitionType.class)
  public void getDefinitionTenantsByTypeKeyAndVersions_tenantSpecificDefinitions_specificVersions_fullScope(final DefinitionType definitionType) {
    // given
    createTenant(TENANT_ID_1);
    createTenant(TENANT_ID_2);
    createTenant(TENANT_ID_3);
    final String definitionKey = "key";
    createDefinition(definitionType, definitionKey, "1", TENANT_ID_1, "the name");
    createDefinition(definitionType, definitionKey, "2", TENANT_ID_2, "the name");
    // also create a definition of another type, should not affect result
    final DefinitionType otherDefinitionType = Arrays.stream(DefinitionType.values())
      .filter(value -> !definitionType.equals(value))
      .findFirst()
      .orElseThrow(OptimizeIntegrationTestException::new);
    createDefinition(otherDefinitionType, definitionKey, "1", TENANT_ID_3, "other");
    createDefinition(otherDefinitionType, definitionKey, "2", TENANT_ID_3, "other");
    final String collectionId = collectionClient.createNewCollection();
    final List<String> scopeTenantIds = Lists.newArrayList(TENANT_ID_1, TENANT_ID_2);
    collectionClient.addScopeEntryToCollection(
      collectionId, new CollectionScopeEntryDto(definitionType, definitionKey, scopeTenantIds)
    );
    collectionClient.addScopeEntryToCollection(
      collectionId, new CollectionScopeEntryDto(otherDefinitionType, definitionKey, scopeTenantIds)
    );

    // when all versions are included
    final List<TenantResponseDto> tenantsForAllVersions =
      definitionClient.resolveDefinitionTenantsByTypeKeyAndVersions(
        definitionType, definitionKey, Lists.newArrayList("1", "2"), collectionId
      );

    // then all tenants are returned
    assertThat(tenantsForAllVersions)
      .extracting(TenantResponseDto::getId)
      .containsExactly(TENANT_ID_1, TENANT_ID_2);

    // when only some versions are included
    final List<TenantResponseDto> tenantsForVersion1 =
      definitionClient.resolveDefinitionTenantsByTypeKeyAndVersions(
        definitionType, definitionKey, Lists.newArrayList("1"), collectionId
      );

    // then only the tenants belonging to those versions are included
    assertThat(tenantsForVersion1)
      .extracting(TenantResponseDto::getId)
      .containsExactly(TENANT_ID_1);
  }

  @ParameterizedTest
  @EnumSource(DefinitionType.class)
  public void getDefinitionTenantsByTypeKeyAndVersions_tenantSpecificDefinitions_specificVersions_reducedScope(final DefinitionType definitionType) {
    // given
    createTenant(TENANT_ID_1);
    createTenant(TENANT_ID_2);
    createTenant(TENANT_ID_3);
    final String definitionKey = "key";
    createDefinition(definitionType, definitionKey, "1", TENANT_ID_1, "the name");
    createDefinition(definitionType, definitionKey, "2", TENANT_ID_2, "the name");
    final String collectionId = collectionClient.createNewCollection();
    // only tenant1 is in the scope
    final List<String> scopeTenantIds = Lists.newArrayList(TENANT_ID_1);
    collectionClient.addScopeEntryToCollection(
      collectionId, new CollectionScopeEntryDto(definitionType, definitionKey, scopeTenantIds)
    );

    // when all versions are included
    // (this is an artificial case as usually no more versions are provided than within the scope, still it ensures
    // the scope cannot be breached by the versions provided by the user)
    final List<TenantResponseDto> tenantsForAllVersions =
      definitionClient.resolveDefinitionTenantsByTypeKeyAndVersions(
        definitionType, definitionKey, Lists.newArrayList("1", "2"), collectionId
      );

    // then still only the tenant within the scope is returned
    assertThat(tenantsForAllVersions)
      .extracting(TenantResponseDto::getId)
      .containsExactly(TENANT_ID_1);
  }

  @ParameterizedTest
  @EnumSource(DefinitionType.class)
  public void getDefinitionTenantsByTypeKeyAndVersions_tenantSpecificDefinitions_allVersion_reducedScope(final DefinitionType definitionType) {
    // given
    createTenant(TENANT_ID_1);
    createTenant(TENANT_ID_2);
    createTenant(TENANT_ID_3);
    final String definitionKey = "key";
    createDefinition(definitionType, definitionKey, "1", TENANT_ID_1, "the name");
    createDefinition(definitionType, definitionKey, "2", TENANT_ID_2, "the name");
    final String collectionId = collectionClient.createNewCollection();
    // only tenant2 is in the scope
    final List<String> scopeTenantIds = Lists.newArrayList(TENANT_ID_2);
    collectionClient.addScopeEntryToCollection(
      collectionId, new CollectionScopeEntryDto(definitionType, definitionKey, scopeTenantIds)
    );

    // when the "all" version is included
    final List<TenantResponseDto> tenantsForAllVersions =
      definitionClient.resolveDefinitionTenantsByTypeKeyAndVersions(
        definitionType, definitionKey, Lists.newArrayList("all"), collectionId
      );

    // then still only the tenant within the scope is returned
    assertThat(tenantsForAllVersions)
      .extracting(TenantResponseDto::getId)
      .containsExactly(TENANT_ID_2);
  }

  @ParameterizedTest
  @EnumSource(DefinitionType.class)
  public void getDefinitionTenantsByTypeKeyAndVersions_tenantSpecificDefinitions_latestVersion_reducedScope(final DefinitionType definitionType) {
    // given
    createTenant(TENANT_ID_1);
    createTenant(TENANT_ID_2);
    createTenant(TENANT_ID_3);
    final String definitionKey = "key";
    createDefinition(definitionType, definitionKey, "1", TENANT_ID_1, "the name");
    createDefinition(definitionType, definitionKey, "2", TENANT_ID_2, "the name");
    final String collectionId = collectionClient.createNewCollection();
    // only tenant1 is in the scope
    final List<String> scopeTenantIds = Lists.newArrayList(TENANT_ID_1);
    collectionClient.addScopeEntryToCollection(
      collectionId, new CollectionScopeEntryDto(definitionType, definitionKey, scopeTenantIds)
    );

    // when the "latest" version is included
    final List<TenantResponseDto> tenantsForAllVersions =
      definitionClient.resolveDefinitionTenantsByTypeKeyAndVersions(
        definitionType, definitionKey, Lists.newArrayList("latest"), collectionId
      );

    // then still only the tenant within the scope is returned, latest version is now 1
    assertThat(tenantsForAllVersions)
      .extracting(TenantResponseDto::getId)
      .containsExactly(TENANT_ID_1);
  }

  @ParameterizedTest
  @EnumSource(DefinitionType.class)
  public void getDefinitionTenantsByTypeKeyAndVersions_sharedDefinition_specificVersions_fullScope(final DefinitionType definitionType) {
    // given
    createTenant(TENANT_ID_1);
    createTenant(TENANT_ID_2);
    createTenant(TENANT_ID_3);
    final String definitionKey = "key";
    createDefinition(definitionType, definitionKey, "1", null, "the name");
    createDefinition(definitionType, definitionKey, "2", null, "the name");
    final String collectionId = collectionClient.createNewCollection();
    // all tenants are in scope
    final List<String> scopeTenantIds = Lists.newArrayList(
      TENANT_NOT_DEFINED_ID,
      TENANT_ID_1,
      TENANT_ID_2,
      TENANT_ID_3
    );
    collectionClient.addScopeEntryToCollection(
      collectionId, new CollectionScopeEntryDto(definitionType, definitionKey, scopeTenantIds)
    );

    // when all versions are included
    final List<TenantResponseDto> tenantsForAllVersions =
      definitionClient.resolveDefinitionTenantsByTypeKeyAndVersions(
        definitionType, definitionKey, Lists.newArrayList("1", "2"), collectionId
      );

    // then all tenants are returned
    assertThat(tenantsForAllVersions)
      .extracting(TenantResponseDto::getId)
      .containsExactly(TENANT_NOT_DEFINED_ID, TENANT_ID_1, TENANT_ID_2, TENANT_ID_3);
  }

  @ParameterizedTest
  @EnumSource(DefinitionType.class)
  public void getDefinitionTenantsByTypeKeyAndVersions_sharedDefinition_specificVersions_reducedScope(final DefinitionType definitionType) {
    // given
    createTenant(TENANT_ID_1);
    createTenant(TENANT_ID_2);
    createTenant(TENANT_ID_3);
    final String definitionKey = "key";
    createDefinition(definitionType, definitionKey, "1", null, "the name");
    createDefinition(definitionType, definitionKey, "2", null, "the name");
    final String collectionId = collectionClient.createNewCollection();
    // only some tenants are in the scope
    final List<String> scopeTenantIds = Lists.newArrayList(TENANT_NOT_DEFINED_ID, TENANT_ID_1);
    collectionClient.addScopeEntryToCollection(
      collectionId, new CollectionScopeEntryDto(definitionType, definitionKey, scopeTenantIds)
    );

    // when all versions are included
    final List<TenantResponseDto> tenantsForAllVersions =
      definitionClient.resolveDefinitionTenantsByTypeKeyAndVersions(
        definitionType, definitionKey, Lists.newArrayList("1", "2"), collectionId
      );

    // then only the scope tenants are returned
    assertThat(tenantsForAllVersions)
      .extracting(TenantResponseDto::getId)
      .containsExactly(TENANT_NOT_DEFINED_ID, TENANT_ID_1);
  }

  @ParameterizedTest
  @EnumSource(DefinitionType.class)
  public void getDefinitionTenantsByTypeKeyAndVersions_sharedDefinition_specificVersions_notDefinedNotInScope(final DefinitionType definitionType) {
    // given
    createTenant(TENANT_ID_1);
    createTenant(TENANT_ID_2);
    createTenant(TENANT_ID_3);
    final String definitionKey = "key";
    createDefinition(definitionType, definitionKey, "1", null, "the name");
    createDefinition(definitionType, definitionKey, "2", null, "the name");
    final String collectionId = collectionClient.createNewCollection();
    // only tenant1 is in scope
    final List<String> scopeTenantIds = Lists.newArrayList(TENANT_ID_1);
    collectionClient.addScopeEntryToCollection(
      collectionId, new CollectionScopeEntryDto(definitionType, definitionKey, scopeTenantIds)
    );

    // when all versions are included
    final List<TenantResponseDto> tenantsForAllVersions =
      definitionClient.resolveDefinitionTenantsByTypeKeyAndVersions(
        definitionType, definitionKey, Lists.newArrayList("1", "2"), collectionId
      );

    // then only the scope tenants are returned
    assertThat(tenantsForAllVersions)
      .extracting(TenantResponseDto::getId)
      .containsExactly(TENANT_ID_1);
  }

  @ParameterizedTest
  @EnumSource(DefinitionType.class)
  public void getDefinitionTenantsByTypeKeyAndVersions_sharedAndTenantSpecificDefinitions_specificVersions_reducedScope(final DefinitionType definitionType) {
    // given
    createTenant(TENANT_ID_1);
    createTenant(TENANT_ID_2);
    createTenant(TENANT_ID_3);
    final String definitionKey = "key";
    createDefinition(definitionType, definitionKey, "1", null, "the name");
    createDefinition(definitionType, definitionKey, "1", TENANT_ID_1, "the name");
    createDefinition(definitionType, definitionKey, "1", TENANT_ID_3, "the name");
    createDefinition(definitionType, definitionKey, "2", TENANT_ID_2, "the name");
    createDefinition(definitionType, definitionKey, "2", null, "the name");
    createDefinition(definitionType, definitionKey, "3", TENANT_ID_3, "the name");
    final String collectionId = collectionClient.createNewCollection();
    // only some tenants are in the scope
    final List<String> scopeTenantIds = Lists.newArrayList(TENANT_NOT_DEFINED_ID, TENANT_ID_1, TENANT_ID_2);
    collectionClient.addScopeEntryToCollection(
      collectionId, new CollectionScopeEntryDto(definitionType, definitionKey, scopeTenantIds)
    );

    // when all versions are included
    final List<TenantResponseDto> tenantsForAllVersions =
      definitionClient.resolveDefinitionTenantsByTypeKeyAndVersions(
        definitionType, definitionKey, Lists.newArrayList("1", "2", "3"), collectionId
      );

    // then only the scope tenants are returned
    assertThat(tenantsForAllVersions)
      .extracting(TenantResponseDto::getId)
      .containsExactly(TENANT_NOT_DEFINED_ID, TENANT_ID_1, TENANT_ID_2);
  }

  @ParameterizedTest
  @EnumSource(DefinitionType.class)
  public void getDefinitionTenantsByTypeKeyAndVersions_definitionNotInScope(final DefinitionType type) {
    // given
    final String definitionKey1 = "definitionKey1";
    createDefinition(type, definitionKey1, "1", null, "the name");
    final String definitionKey2 = "definitionKey2";
    createDefinition(type, definitionKey2, "1", null, "the name");
    // create definitionKey2 of other type, should not affect result
    final DefinitionType otherDefinitionType = Arrays.stream(DefinitionType.values())
      .filter(value -> !type.equals(value))
      .findFirst()
      .orElseThrow(OptimizeIntegrationTestException::new);
    createDefinition(otherDefinitionType, definitionKey2, "1", null, "other");
    final String collectionId = collectionClient.createNewCollection();
    final List<String> scopeTenantIds = Lists.newArrayList(TENANT_NOT_DEFINED_ID);
    collectionClient.addScopeEntryToCollection(
      collectionId, new CollectionScopeEntryDto(type, definitionKey2, scopeTenantIds)
    );
    elasticSearchIntegrationTestExtension.refreshAllOptimizeIndices();

    // when trying to get tenants for a definition key that is not in the scope
    final Response responseForWrongKey = embeddedOptimizeExtension
      .getRequestExecutor()
      .buildResolveDefinitionTenantsByTypeKeyAndVersionsRequest(
        type.getId(), definitionKey1, Lists.newArrayList("1"), "invalid"
      )
      .execute();

    // then a 404 is returned
    assertThat(responseForWrongKey.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());

    // when trying to get tenants for a definition type that is not in the scope but key that is in the scope
    final Response responseForWrongType = embeddedOptimizeExtension
      .getRequestExecutor()
      .buildResolveDefinitionTenantsByTypeKeyAndVersionsRequest(
        otherDefinitionType.getId(), definitionKey2, Lists.newArrayList("1"), "invalid"
      )
      .execute();

    // then a 404 is returned
    assertThat(responseForWrongType.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());

    // when trying to get tenants for a definition version that is not in the scope but key that is in the scope
    final Response responseForWrongVersion = embeddedOptimizeExtension
      .getRequestExecutor()
      .buildResolveDefinitionTenantsByTypeKeyAndVersionsRequest(
        type.getId(), definitionKey2, Lists.newArrayList("99"), "invalid"
      )
      .execute();

    // then a 404 is returned
    assertThat(responseForWrongVersion.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
  }

  @ParameterizedTest
  @EnumSource(DefinitionType.class)
  public void getDefinitionTenantsByTypeKeyAndVersions_tenantsExcludedForDeletedDefinitions(final DefinitionType definitionType) {
    // given
    createTenant(TENANT_ID_1);
    createTenant(TENANT_ID_2);
    createTenant(TENANT_ID_3);
    final String definitionKey = "key";
    createDefinition(definitionType, definitionKey, "1", TENANT_ID_1, "the name", false);
    createDefinition(definitionType, definitionKey, "2", TENANT_ID_2, "the name", false);
    createDefinition(definitionType, definitionKey, "2", TENANT_ID_3, "deleted", true);

    final String collectionId = collectionClient.createNewCollection();
    final List<String> scopeTenantIds = Lists.newArrayList(TENANT_ID_1, TENANT_ID_2, TENANT_ID_3);
    collectionClient.addScopeEntryToCollection(
      collectionId, new CollectionScopeEntryDto(definitionType, definitionKey, scopeTenantIds)
    );

    // when all versions are included
    final List<TenantResponseDto> tenantsForAllVersions =
      definitionClient.resolveDefinitionTenantsByTypeKeyAndVersions(
        definitionType, definitionKey, Lists.newArrayList("1", "2"), collectionId
      );

    // then only the tenants for the non deleted definitions are returned
    assertThat(tenantsForAllVersions)
      .extracting(TenantResponseDto::getId)
      .containsExactly(TENANT_ID_1, TENANT_ID_2);
  }

  private void createDefinition(final DefinitionType definitionType,
                                final String key,
                                final String version,
                                final String tenantId,
                                final String name) {
    createDefinition(definitionType, key, version, tenantId, name, false);
  }

  private void createDefinition(final DefinitionType definitionType,
                                final String key,
                                final String version,
                                final String tenantId,
                                final String name,
                                final boolean deleted) {
    switch (definitionType) {
      case PROCESS:
        addProcessDefinitionToElasticsearch(key, version, tenantId, name, deleted);
        return;
      case DECISION:
        addDecisionDefinitionToElasticsearch(key, version, tenantId, name, deleted);
        return;
      default:
        throw new OptimizeIntegrationTestException("Unsupported definition type: " + definitionType);
    }
  }

  private void addDecisionDefinitionToElasticsearch(final String key,
                                                    final String version,
                                                    final String tenantId,
                                                    final String name,
                                                    final boolean deleted) {
    final DecisionDefinitionOptimizeDto decisionDefinitionDto = DecisionDefinitionOptimizeDto.builder()
      .id(key + "-" + version + "-" + tenantId)
      .key(key)
      .version(version)
      .tenantId(tenantId)
      .engine(DEFAULT_ENGINE_ALIAS)
      .name(name)
      .deleted(deleted)
      .dmn10Xml("id-" + key + "-version-" + version + "-" + tenantId)
      .build();
    elasticSearchIntegrationTestExtension.addEntryToElasticsearch(
      DECISION_DEFINITION_INDEX_NAME, decisionDefinitionDto.getId(), decisionDefinitionDto
    );
  }

  private void addProcessDefinitionToElasticsearch(final String key,
                                                   final String version,
                                                   final String tenantId,
                                                   final String name, final boolean deleted) {
    final ProcessDefinitionOptimizeDto expectedDto = ProcessDefinitionOptimizeDto.builder()
      .id(key + "-" + version + "-" + tenantId)
      .key(key)
      .name(name)
      .version(version)
      .tenantId(tenantId)
      .engine(DEFAULT_ENGINE_ALIAS)
      .deleted(deleted)
      .bpmn20Xml(key + version + tenantId)
      .build();
    elasticSearchIntegrationTestExtension.addEntryToElasticsearch(
      PROCESS_DEFINITION_INDEX_NAME,
      expectedDto.getId(),
      expectedDto
    );
  }

  protected void createTenant(final String tenantId) {
    final TenantDto tenantDto = new TenantDto(tenantId, tenantId, DEFAULT_ENGINE_ALIAS);
    elasticSearchIntegrationTestExtension.addEntryToElasticsearch(TENANT_INDEX_NAME, tenantId, tenantDto);
  }

}
