/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.zeebe.engine.processing.processinstance.migration;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.security.configuration.ConfiguredUser;
import io.camunda.zeebe.engine.util.EngineRule;
import io.camunda.zeebe.model.bpmn.Bpmn;
import io.camunda.zeebe.protocol.record.Assertions;
import io.camunda.zeebe.protocol.record.RejectionType;
import io.camunda.zeebe.protocol.record.intent.ProcessInstanceMigrationIntent;
import io.camunda.zeebe.protocol.record.intent.UserIntent;
import io.camunda.zeebe.protocol.record.value.AuthorizationResourceType;
import io.camunda.zeebe.protocol.record.value.PermissionType;
import io.camunda.zeebe.test.util.record.RecordingExporter;
import io.camunda.zeebe.test.util.record.RecordingExporterTestWatcher;
import java.util.List;
import java.util.UUID;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;

public class ProcessInstanceMigrationMigrateAuthorizationTest {
  public static final String JOB_TYPE = "jobType";
  public static final String SOURCE_TASK = "sourceTask";
  public static final String TARGET_TASK = "targetTask";
  private static final String PROCESS_ID = "processId";
  private static final String TARGET_PROCESS_ID = "targetProcessId";
  private static final ConfiguredUser DEFAULT_USER =
      new ConfiguredUser(
          UUID.randomUUID().toString(),
          UUID.randomUUID().toString(),
          UUID.randomUUID().toString(),
          UUID.randomUUID().toString());

  @ClassRule
  public static final EngineRule ENGINE =
      EngineRule.singlePartition()
          .withoutAwaitingIdentitySetup()
          .withSecurityConfig(cfg -> cfg.getAuthorizations().setEnabled(true))
          .withSecurityConfig(cfg -> cfg.getInitialization().setUsers(List.of(DEFAULT_USER)));

  private static long defaultUserKey = -1L;
  private static long targetProcDefKey = -1L;
  @Rule public final TestWatcher recordingExporterTestWatcher = new RecordingExporterTestWatcher();

  @BeforeClass
  public static void beforeAll() {
    defaultUserKey =
        RecordingExporter.userRecords(UserIntent.CREATED)
            .withUsername(DEFAULT_USER.getUsername())
            .getFirst()
            .getKey();

    final var deployment =
        ENGINE
            .deployment()
            .withXmlResource(
                "process.bpmn",
                Bpmn.createExecutableProcess(PROCESS_ID)
                    .startEvent()
                    .serviceTask(SOURCE_TASK, t -> t.zeebeJobType(JOB_TYPE))
                    .endEvent()
                    .done())
            .withXmlResource(
                "targetProcess.bpmn",
                Bpmn.createExecutableProcess(TARGET_PROCESS_ID)
                    .startEvent()
                    .serviceTask(TARGET_TASK, t -> t.zeebeJobType(JOB_TYPE))
                    .endEvent()
                    .done())
            .deploy(defaultUserKey)
            .getValue();
    targetProcDefKey =
        deployment.getProcessesMetadata().stream()
            .filter(p -> p.getBpmnProcessId().equals(TARGET_PROCESS_ID))
            .findFirst()
            .get()
            .getProcessDefinitionKey();
  }

  @Test
  public void shouldBeAuthorizedToMigrateProcessInstanceWithDefaultUser() {
    // given
    final var processInstanceKey = createProcessInstance();

    // when
    ENGINE
        .processInstance()
        .withInstanceKey(processInstanceKey)
        .migration()
        .withTargetProcessDefinitionKey(targetProcDefKey)
        .addMappingInstruction(SOURCE_TASK, TARGET_TASK)
        .migrate(defaultUserKey);

    // then
    assertThat(
            RecordingExporter.processInstanceMigrationRecords(
                    ProcessInstanceMigrationIntent.MIGRATED)
                .withProcessInstanceKey(processInstanceKey)
                .exists())
        .isTrue();
  }

  @Test
  public void shouldBeAuthorizedToMigrateProcessInstanceWithUser() {
    // given
    final var processInstanceKey = createProcessInstance();
    final var userKey = createUser();
    addPermissionsToUser(
        userKey,
        AuthorizationResourceType.PROCESS_DEFINITION,
        PermissionType.UPDATE_PROCESS_INSTANCE,
        PROCESS_ID);

    // when
    ENGINE
        .processInstance()
        .withInstanceKey(processInstanceKey)
        .migration()
        .withTargetProcessDefinitionKey(targetProcDefKey)
        .addMappingInstruction(SOURCE_TASK, TARGET_TASK)
        .migrate(userKey);

    // then
    assertThat(
            RecordingExporter.processInstanceMigrationRecords(
                    ProcessInstanceMigrationIntent.MIGRATED)
                .withProcessInstanceKey(processInstanceKey)
                .exists())
        .isTrue();
  }

  @Test
  public void shouldBeUnauthorizedToMigrateProcessInstanceIfNoPermissions() {
    // given
    final var processInstanceKey = createProcessInstance();
    final var userKey = createUser();

    // when
    ENGINE
        .processInstance()
        .withInstanceKey(processInstanceKey)
        .migration()
        .withTargetProcessDefinitionKey(targetProcDefKey)
        .addMappingInstruction(SOURCE_TASK, TARGET_TASK)
        .expectRejection()
        .migrate(userKey);

    // then
    Assertions.assertThat(
            RecordingExporter.processInstanceMigrationRecords().onlyCommandRejections().getFirst())
        .hasRejectionType(RejectionType.FORBIDDEN)
        .hasRejectionReason(
            "Insufficient permissions to perform operation 'UPDATE_PROCESS_INSTANCE' on resource 'PROCESS_DEFINITION', required resource identifiers are one of '[*, %s]'"
                .formatted(PROCESS_ID));
  }

  private static long createUser() {
    return ENGINE
        .user()
        .newUser(UUID.randomUUID().toString())
        .withPassword(UUID.randomUUID().toString())
        .withName(UUID.randomUUID().toString())
        .withEmail(UUID.randomUUID().toString())
        .create()
        .getKey();
  }

  private void addPermissionsToUser(
      final long userKey,
      final AuthorizationResourceType authorization,
      final PermissionType permissionType,
      final String... resourceIds) {
    ENGINE
        .authorization()
        .permission()
        .withOwnerKey(userKey)
        .withResourceType(authorization)
        .withPermission(permissionType, resourceIds)
        .add(defaultUserKey);
  }

  private long createProcessInstance() {
    return ENGINE.processInstance().ofBpmnProcessId(PROCESS_ID).create(defaultUserKey);
  }
}
