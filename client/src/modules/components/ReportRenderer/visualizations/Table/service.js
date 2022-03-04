/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */

import {post} from 'request';
import {NoDataNotice} from 'components';
import {reportConfig, formatters} from 'services';
import {t} from 'translation';

const {formatReportResult, getRelativeValue, duration} = formatters;

export function getFormattedLabels(reportsLabels, reportsNames, reportsIds) {
  return reportsLabels.reduce(
    (prev, reportLabels, i) => [
      ...prev,
      {
        label: reportsNames[i],
        id: reportsIds[i],
        columns: reportLabels.slice(1),
      },
    ],
    []
  );
}

export function getBodyRows({
  unitedResults,
  allKeys,
  displayRelativeValue,
  instanceCount,
  displayAbsoluteValue,
  flowNodeNames = {},
  groupedByDuration,
}) {
  const rows = allKeys.map((key, idx) => {
    const row = [groupedByDuration ? duration(key) : flowNodeNames[key] || key];
    unitedResults.forEach((measures, i) => {
      measures.forEach((measure) => {
        const value = measure.data[idx].value;
        if (measure.property === 'frequency') {
          if (displayAbsoluteValue) {
            row.push(
              formatters.frequency(typeof value !== 'undefined' && value !== null ? value : '')
            );
          }
          if (displayRelativeValue) {
            row.push(getRelativeValue(value, instanceCount[i]));
          }
        } else {
          row.push(duration(typeof value !== 'undefined' && value !== null ? value : ''));
        }
      });
    });
    return row;
  });
  return rows;
}

export function getCombinedTableProps(
  reportResult,
  reports,
  displayRelativeValue,
  displayAbsoluteValue
) {
  const initialData = {
    labels: [],
    reportsNames: [],
    reportsIds: [],
    combinedResult: [],
    instanceCount: [],
  };

  const combinedProps = reports.reduce((prevReport, {id}) => {
    const report = reportResult[id];
    const {data, result, name} = report;

    // build 2d array of all labels
    const viewString = reportConfig.process.view.find(({matcher}) => matcher(data)).label();

    const viewLabels = result.measures
      .map((measure) => {
        if (measure.property === 'frequency') {
          const frequencyColumns = [];
          if (displayAbsoluteValue) {
            frequencyColumns.push(viewString + ': ' + t('report.view.count'));
          }
          if (displayRelativeValue) {
            frequencyColumns.push(t('report.table.relativeFrequency'));
          }
          return frequencyColumns;
        } else if (measure.property === 'duration') {
          console.log(measure.aggregationType);
          return [
            viewString +
              ': ' +
              (data.view.entity === 'incident'
                ? t('report.view.resolutionDuration')
                : t('report.view.duration')) +
              (measure.aggregationType
                ? ` - ${t('report.config.aggregationShort.' + measure.aggregationType.type, {
                    value: measure.aggregationType.value,
                  })}`
                : '') +
              (measure.userTaskDurationTime
                ? ` (${t('report.config.userTaskDuration.' + measure.userTaskDurationTime)})`
                : ''),
          ];
        }
        return '';
      })
      .flat();
    const groupByLabel = reportConfig.process.group.find(({matcher}) => matcher(data)).label();
    const labels = [...prevReport.labels, [groupByLabel, ...viewLabels]];

    // 2d array of all names
    const reportsNames = [...prevReport.reportsNames, name];

    // 2d array of all ids
    const reportsIds = [...prevReport.reportsIds, id];

    // 2d array of all results
    const reportsResult = [
      ...prevReport.combinedResult,
      result.measures.map((measure) => ({
        ...measure,
        data: formatReportResult(data, measure.data),
      })),
    ];

    // 2d array of all process instances count
    const reportsInstanceCount = [...prevReport.instanceCount, result.instanceCount];

    return {
      labels,
      reportsNames,
      reportsIds,
      combinedResult: reportsResult,
      instanceCount: reportsInstanceCount,
    };
  }, initialData);

  return combinedProps;
}

export function sortColumns(head, body, columnOrder) {
  if (!columnOrder.length) {
    return {sortedHead: head, sortedBody: body};
  }

  const sortedHead = head.slice().sort(byOrder(columnOrder));

  const sortedBody = body.map((row) => row.map(valueForNewColumnPosition(head, sortedHead)));

  return {sortedHead, sortedBody};
}

function byOrder(order) {
  return function (a, b) {
    let indexA = order.indexOf(a.id || a);
    let indexB = order.indexOf(b.id || b);

    // put columns without specified order at end
    if (indexA === -1) {
      indexA = Infinity;
    }
    if (indexB === -1) {
      indexB = Infinity;
    }

    return indexA - indexB;
  };
}

function valueForNewColumnPosition(head, sortedHead) {
  const flattendHead = flatten(head);
  const flattendSortedHead = flatten(sortedHead);

  return function (_, newPosition, cells) {
    const headerAtNewPosition = flattendSortedHead[newPosition];
    const originalPosition = flattendHead.indexOf(headerAtNewPosition);

    return cells[originalPosition];
  };
}

function flatten(head) {
  const flattendHead = head.reduce((arr, el) => {
    let headColumns = [el];
    if (el.columns) {
      headColumns = el.columns.map((col) => el.id + col);
    }

    return arr.concat(headColumns);
  }, []);

  return flattendHead;
}

export const getNoDataMessage = () => ({
  head: [],
  body: [],
  noData: <NoDataNotice type="info">{t('report.table.noData')}</NoDataNotice>,
});

export function cockpitLink(endpoints, instance, type) {
  const content = instance[type + 'InstanceId'];
  const {endpoint, engineName} = endpoints[instance.engineName] || {};
  if (endpoint) {
    return (
      <a
        href={`${endpoint}/app/cockpit/${engineName}/#/${type}-instance/${content}`}
        target="_blank"
        rel="noopener noreferrer"
      >
        {content}
      </a>
    );
  }
  return content;
}

export function isVisibleColumn(column, {excludedColumns, includedColumns, includeNewVariables}) {
  if (includeNewVariables) {
    return !excludedColumns.includes(column);
  } else {
    return includedColumns.includes(column);
  }
}

export function getLabelWithType(name, type) {
  return (
    <>
      <span className="variableExtension">{t('report.table.rawData.' + type)}: </span>
      {name}
    </>
  );
}

export async function loadObjectValues(
  name,
  processInstanceId,
  processDefinitionKey,
  processDefinitionVersions,
  tenantIds
) {
  const response = await post(`api/variables/values`, {
    name,
    processInstanceId,
    processDefinitionKey,
    processDefinitionVersions,
    tenantIds,
    type: 'object',
  });

  const values = await response.json();
  return values[0];
}
