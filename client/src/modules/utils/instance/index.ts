/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */

/**
 * @returns a boolean showing if the current instance has an incident
 * @param {*} instance object with full instance data
 */
const hasIncident = (instance: Pick<ProcessInstanceEntity, 'state'>) => {
  return instance.state === 'INCIDENT';
};

/**
 * @returns a boolean showing if the current instance is running.
 * @param {*} instance object with full instance data
 */
const isRunning = (instance: Pick<ProcessInstanceEntity, 'state'>) => {
  return instance.state === 'ACTIVE' || instance.state === 'INCIDENT';
};

const getProcessName = (instance: ProcessInstanceEntity | null) => {
  if (instance === null) {
    return '';
  }

  const {processName, bpmnProcessId} = instance;
  return processName || bpmnProcessId || '';
};

const createOperation = (
  operationType: OperationEntityType,
): InstanceOperationEntity => {
  return {
    type: operationType,
    state: 'SCHEDULED',
    errorMessage: null,
  };
};

export {hasIncident, isRunning, getProcessName, createOperation};
