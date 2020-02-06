/* eslint-disable import/no-unresolved */
/* eslint-disable jsx-a11y/anchor-is-valid */
import React from 'react';
import PropTypes from 'prop-types';
import SearchDatapoints from './SearchDatapoints';
import AssignmentContext from '../../assignment-context';

export default class AssignDatapoints extends React.Component {
  state = {
    currentSubTab: '',
  };

  changeTab = tab => {
    this.setState({
      currentSubTab: tab,
    });
  };

  getAssignmentData = () => {
    const { selectedDeviceId } = this.props;
    const { devices, datapointAssignments } = this.context.data;

    const deviceData = devices.find(device => device.id === selectedDeviceId);
    const datapointAssignment = datapointAssignments.find(
      dp => dp.deviceId === parseInt(selectedDeviceId, 10)
    );

    let datapointsData = [];

    if (datapointAssignment) {
      datapointsData = datapointAssignment.datapoints;
    }

    return {
      deviceData,
      datapointsData,
    };
  };

  renderDatapoint = dp => {
    return (
      <div key={dp.id} className="datapoint">
        <p>{dp.name}</p>
        <span>{dp.id}</span>
      </div>
    );
  };

  render() {
    const { strings } = this.context;
    const { deviceData, datapointsData } = this.getAssignmentData();
    const datapointsCount = datapointsData.length;

    return (
      <div className="devices-action-page assign-datapoints">
        <div>
          <div className="header">
            <div className="device-details">
              <p>{deviceData.name}</p>
              <p>
                <span>
                  {datapointsCount} {strings.datapointAssigned}
                </span>
                <span className="divider">.</span>
                <a
                  className={datapointsCount ? undefined : 'disabled'}
                  href="#"
                  onClick={
                    datapointsCount
                      ? () => this.props.changeTab('EDIT_DATAPOINTS', deviceData.id)
                      : undefined
                  }
                >
                  {strings.edit}
                </a>
              </p>
            </div>

            <button onClick={() => this.changeTab('SEARCH_DATAPOINTS')} type="button">
              {strings.assignDatapointByNameOrId}
            </button>
          </div>

          <div className="body">{datapointsData.map(this.renderDatapoint)}</div>
        </div>

        <div>
          {this.state.currentSubTab === 'SEARCH_DATAPOINTS' && (
            <SearchDatapoints deviceId={this.props.selectedDeviceId} changeTab={this.changeTab} />
          )}
        </div>
      </div>
    );
  }
}

AssignDatapoints.contextType = AssignmentContext;
AssignDatapoints.propTypes = {
  selectedDeviceId: PropTypes.string.isRequired,
  changeTab: PropTypes.func.isRequired,
};