import React from 'react';
import update from 'immutability-helper';

import Viewer from 'bpmn-js/lib/NavigatedViewer';

import TargetValueDiagramBehavior from './TargetValueDiagramBehavior';

import {Button, Modal, BPMNDiagram, Table, Input, Select, TargetValueBadge} from 'components';
import {formatters} from 'services';

import './TargetValueModal.css';

export default class TargetValueComparison extends React.Component {
  constructor(props) {
    super(props);

    this.inputRefs = {};

    this.state = {
      focus: null,
      values: {},
      nodeNames: {}
    };
  }

  getConfig = () => {
    return this.props.configuration.targetValue || {};
  };

  async componentWillReceiveProps(nextProps) {
    if (this.props.open !== nextProps.open) {
      if (nextProps.open) {
        const {values, nodeNames} = await this.constructValues();

        this.setState({
          focus: null,
          values,
          nodeNames
        });
      } else {
        this.setState({
          values: {},
          nodeNames: {}
        });
      }
    }
  }

  confirmModal = () => {
    this.props.onConfirm(this.cleanUpValues());
  };

  cleanUpValues = () => {
    // this function removes all entries without value and converts values into numbers
    const values = {};

    Object.keys(this.state.values).forEach(key => {
      const entry = this.state.values[key];
      if (entry && entry.value.trim()) {
        values[key] = {
          value: parseFloat(entry.value),
          unit: entry.unit
        };
      }
    });

    return values;
  };

  constructValues = () => {
    return new Promise(resolve => {
      const viewer = new Viewer();
      viewer.importXML(this.props.configuration.xml, () => {
        const predefinedValues = this.getConfig().values || {};
        const values = {};
        const nodeNames = {};

        new Set(
          viewer
            .get('elementRegistry')
            .filter(element => element.businessObject.$instanceOf('bpmn:FlowNode'))
            .map(element => element.businessObject)
        ).forEach(element => {
          values[element.id] = this.copyObjectIfExistsAndStringifyValue(
            predefinedValues[element.id]
          );
          nodeNames[element.id] = element.name || element.id;
        });

        resolve({values, nodeNames});
      });
    });
  };

  copyObjectIfExistsAndStringifyValue = obj => {
    if (obj) {
      return {
        ...obj,
        value: '' + obj.value
      };
    }
    return obj;
  };

  setTarget = (type, id) => ({target: {value}}) => {
    if (this.state.values[id]) {
      this.setState(
        update(this.state, {
          values: {
            [id]: {
              [type]: {$set: value}
            }
          }
        })
      );
    } else {
      this.setState(
        update(this.state, {
          values: {
            [id]: {
              $set: {
                value: '0',
                unit: 'hours',
                [type]: value
              }
            }
          }
        })
      );
    }
  };

  storeInputReferenceFor = id => input => {
    this.inputRefs[id] = input;
  };

  constructTableBody = () => {
    return Object.keys(this.state.values).map(id => {
      const settings = this.state.values[id] || {value: '', unit: 'hours'};
      return [
        this.state.nodeNames[id],
        formatters.duration(this.props.reportResult.result[id] || 0),
        <React.Fragment>
          <div className="TargetValueModal__selection">
            <Input
              value={settings.value}
              reference={this.storeInputReferenceFor(id)}
              onChange={this.setTarget('value', id)}
              onFocus={() => {
                this.updateFocus(id);
              }}
              onBlur={() => {
                this.updateFocus(null);
              }}
              className="TargetValueModal__selection--input"
            />
            <Select
              value={settings.unit}
              onChange={evt => {
                this.setTarget('unit', id)(evt);
                this.updateFocus(id);
              }}
              className="TargetValueModal__selection--select"
            >
              <Select.Option value="millis">Milliseconds</Select.Option>
              <Select.Option value="seconds">Seconds</Select.Option>
              <Select.Option value="minutes">Minutes</Select.Option>
              <Select.Option value="hours">Hours</Select.Option>
              <Select.Option value="days">Days</Select.Option>
              <Select.Option value="weeks">Weeks</Select.Option>
              <Select.Option value="months">Months</Select.Option>
              <Select.Option value="years">Years</Select.Option>
            </Select>
          </div>
        </React.Fragment>
      ];
    });
  };

  validChanges = () => {
    return this.hasSomethingChanged() && this.areAllFieldsNumbers();
  };

  hasSomethingChanged = () => {
    const prev = this.getConfig().values || {};
    const now = this.cleanUpValues();

    return JSON.stringify(prev) !== JSON.stringify(now);
  };

  areAllFieldsNumbers = () => {
    return Object.keys(this.state.values)
      .filter(key => this.state.values[key])
      .every(key => {
        const entry = this.state.values[key];
        const value = entry && entry.value;

        return value.trim() === '' || (!isNaN(value.trim()) && +value > 0);
      });
  };

  updateFocus = focus => this.setState({focus});

  componentDidUpdate(_, prevState) {
    if (this.state.focus && this.state.focus !== prevState.focus) {
      this.inputRefs[this.state.focus].focus();
      this.inputRefs[this.state.focus].select();
    }
  }

  render() {
    return (
      <Modal
        open={this.props.open}
        onClose={this.props.onClose}
        className="TargetValueModal__Modal"
        size="large"
      >
        <Modal.Header>Target Value Comparison</Modal.Header>
        <Modal.Content>
          <div className="TargetValueModal__DiagramContainer">
            <BPMNDiagram xml={this.props.configuration.xml}>
              <TargetValueDiagramBehavior onClick={this.updateFocus} focus={this.state.focus} />
              <TargetValueBadge values={this.state.values} />
            </BPMNDiagram>
          </div>
          <Table
            head={['Activity', 'Actual Value', 'Target Value']}
            body={this.constructTableBody()}
            foot={[]}
            className="TargetValueModal__Table"
          />
        </Modal.Content>
        <Modal.Actions>
          <Button onClick={this.props.onClose}>Cancel</Button>
          <Button
            type="primary"
            color="blue"
            onClick={this.confirmModal}
            disabled={!this.validChanges()}
          >
            Apply
          </Button>
        </Modal.Actions>
      </Modal>
    );
  }
}
