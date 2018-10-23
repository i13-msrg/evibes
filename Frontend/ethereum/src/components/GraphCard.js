import React, { Component } from 'react';
import './GraphCard.css';
import {Elevation, Card } from "@blueprintjs/core";
import TimeSeriesChart from './TimeseriesChart';


class GraphCard extends Component {
  render() {
    return (
        <Card interactive={false} elevation={Elevation.ONE} className="GraphCard">
            <div className="graphContainer">
                <div className="graphContainer-name"><span>{this.props.name}</span></div>
                <div className="graphContainer-graph">
                    <span>
                    <TimeSeriesChart chartData={this.props.chartData}/>
                    </span></div>
            </div>
        </Card>
    );
  }
}

export default GraphCard;