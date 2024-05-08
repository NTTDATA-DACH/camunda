/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.tasklist.webapp.management.dto;

public class TakeBackupRequestDto {

  private Long backupId;

  public Long getBackupId() {
    return backupId;
  }

  public TakeBackupRequestDto setBackupId(Long backupId) {
    this.backupId = backupId;
    return this;
  }
}
