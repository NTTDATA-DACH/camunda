/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */

const mockDecisionInstances = {
  decisionInstances: [
    {
      id: '2251799813689541',
      decisionName: 'test decision instance 1',
      decisionVersion: 1,
      evaluationDate: '2022-02-07T10:01:51.293+0000',
      processInstanceId: '2251799813689544',
      state: 'EVALUATED',
    },
    {
      id: '2251799813689542',
      decisionName: 'test decision instance 2',
      decisionVersion: 1,
      evaluationDate: '2022-02-07T10:01:51.293+0000',
      processInstanceId: '2251799813689544',
      state: 'FAILED',
    },
    {
      id: '2251799813689543',
      decisionName: 'test decision instance 3',
      decisionVersion: 1,
      evaluationDate: '2022-02-07T10:01:51.293+0000',
      processInstanceId: null,
      state: 'EVALUATED',
    },
    {
      id: '2251799813689544',
      decisionName: 'test decision instance 4',
      decisionVersion: 1,
      evaluationDate: '2022-02-07T10:01:51.293+0000',
      processInstanceId: '2251799813689544',
      state: 'FAILED',
    },
    {
      id: '2251799813689545',
      decisionName: 'test decision instance 5',
      decisionVersion: 1,
      evaluationDate: '2022-02-07T10:01:51.293+0000',
      processInstanceId: '2251799813689544',
      state: 'EVALUATED',
    },
    {
      id: '2251799813689546',
      decisionName: 'test decision instance 6',
      decisionVersion: 1,
      evaluationDate: '2022-02-07T10:01:51.293+0000',
      processInstanceId: '2251799813689544',
      state: 'FAILED',
    },
    {
      id: '2251799813689547',
      decisionName: 'test decision instance 7',
      decisionVersion: 1,
      evaluationDate: '2022-02-07T10:01:51.293+0000',
      processInstanceId: '2251799813689544',
      state: 'EVALUATED',
    },
    {
      id: '2251799813689548',
      decisionName: 'test decision instance 8',
      decisionVersion: 1,
      evaluationDate: '2022-02-07T10:01:51.293+0000',
      processInstanceId: '2251799813689544',
      state: 'FAILED',
    },
    {
      id: '2251799813689549',
      decisionName: 'test decision instance 9',
      decisionVersion: 1,
      evaluationDate: '2022-02-07T10:01:51.293+0000',
      processInstanceId: '2251799813689544',
      state: 'EVALUATED',
    },
    {
      id: '2251799813689550',
      decisionName: 'test decision instance 10',
      decisionVersion: 1,
      evaluationDate: '2022-02-07T10:01:51.293+0000',
      processInstanceId: '2251799813689544',
      state: 'FAILED',
    },
    {
      id: '2251799813689551',
      decisionName: 'test decision instance 11',
      decisionVersion: 1,
      evaluationDate: '2022-02-07T10:01:51.293+0000',
      processInstanceId: '2251799813689544',
      state: 'EVALUATED',
    },
    {
      id: '2251799813689552',
      decisionName: 'test decision instance 12',
      decisionVersion: 1,
      evaluationDate: '2022-02-07T10:01:51.293+0000',
      processInstanceId: '2251799813689544',
      state: 'FAILED',
    },
    {
      id: '2251799813689553',
      decisionName: 'test decision instance 13',
      decisionVersion: 1,
      evaluationDate: '2022-02-07T10:01:51.293+0000',
      processInstanceId: '2251799813689544',
      state: 'EVALUATED',
    },
    {
      id: '2251799813689554',
      decisionName: 'test decision instance 14',
      decisionVersion: 1,
      evaluationDate: '2022-02-07T10:01:51.293+0000',
      processInstanceId: '2251799813689544',
      state: 'FAILED',
    },
    {
      id: '2251799813689555',
      decisionName: 'test decision instance 15',
      decisionVersion: 1,
      evaluationDate: '2022-02-07T10:01:51.293+0000',
      processInstanceId: '2251799813689544',
      state: 'EVALUATED',
    },
    {
      id: '2251799813689556',
      decisionName: 'test decision instance 16',
      decisionVersion: 1,
      evaluationDate: '2022-02-07T10:01:51.293+0000',
      processInstanceId: '2251799813689544',
      state: 'FAILED',
    },
    {
      id: '2251799813689557',
      decisionName: 'test decision instance 17',
      decisionVersion: 1,
      evaluationDate: '2022-02-07T10:01:51.293+0000',
      processInstanceId: '2251799813689544',
      state: 'EVALUATED',
    },
    {
      id: '2251799813689558',
      decisionName: 'test decision instance 18',
      decisionVersion: 1,
      evaluationDate: '2022-02-07T10:01:51.293+0000',
      processInstanceId: '2251799813689544',
      state: 'FAILED',
    },
    {
      id: '2251799813689559',
      decisionName: 'test decision instance 19',
      decisionVersion: 1,
      evaluationDate: '2022-02-07T10:01:51.293+0000',
      processInstanceId: '2251799813689544',
      state: 'EVALUATED',
    },
    {
      id: '2251799813689560',
      decisionName: 'test decision instance 20',
      decisionVersion: 1,
      evaluationDate: '2022-02-07T10:01:51.293+0000',
      processInstanceId: '2251799813689544',
      state: 'FAILED',
    },
    {
      id: '2251799813689561',
      decisionName: 'test decision instance 21',
      decisionVersion: 1,
      evaluationDate: '2022-02-07T10:01:51.293+0000',
      processInstanceId: '2251799813689544',
      state: 'EVALUATED',
    },
    {
      id: '2251799813689562',
      decisionName: 'test decision instance 22',
      decisionVersion: 1,
      evaluationDate: '2022-02-07T10:01:51.293+0000',
      processInstanceId: '2251799813689544',
      state: 'FAILED',
    },
    {
      id: '2251799813689563',
      decisionName: 'test decision instance 23',
      decisionVersion: 1,
      evaluationDate: '2022-02-07T10:01:51.293+0000',
      processInstanceId: '2251799813689544',
      state: 'EVALUATED',
    },
    {
      id: '2251799813689564',
      decisionName: 'test decision instance 24',
      decisionVersion: 1,
      evaluationDate: '2022-02-07T10:01:51.293+0000',
      processInstanceId: '2251799813689544',
      state: 'FAILED',
    },
    {
      id: '2251799813689565',
      decisionName: 'test decision instance 25',
      decisionVersion: 1,
      evaluationDate: '2022-02-07T10:01:51.293+0000',
      processInstanceId: '2251799813689544',
      state: 'EVALUATED',
    },
    {
      id: '2251799813689566',
      decisionName: 'test decision instance 26',
      decisionVersion: 1,
      evaluationDate: '2022-02-07T10:01:51.293+0000',
      processInstanceId: '2251799813689544',
      state: 'FAILED',
    },
    {
      id: '2251799813689567',
      decisionName: 'test decision instance 27',
      decisionVersion: 1,
      evaluationDate: '2022-02-07T10:01:51.293+0000',
      processInstanceId: '2251799813689544',
      state: 'EVALUATED',
    },
    {
      id: '2251799813689568',
      decisionName: 'test decision instance 28',
      decisionVersion: 1,
      evaluationDate: '2022-02-07T10:01:51.293+0000',
      processInstanceId: '2251799813689544',
      state: 'FAILED',
    },
  ],
  totalCount: 2,
} as const;

export {mockDecisionInstances};
