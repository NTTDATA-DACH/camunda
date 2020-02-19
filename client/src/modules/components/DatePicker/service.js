/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */

import moment from 'moment';
export const DATE_FORMAT = 'YYYY-MM-DD';

export function isDateValid(date) {
  const momentDate = moment(date, DATE_FORMAT);
  return momentDate.isValid() && momentDate.format(DATE_FORMAT) === date;
}
