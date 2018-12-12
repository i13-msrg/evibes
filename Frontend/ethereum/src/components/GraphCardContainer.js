import React, { Component } from 'react';
import './GraphCardContainer.css';
import GraphCard from './GraphCard';

class GraphCardContainer extends Component {
  render() {
    return (
        <div className="GraphCardContainer">
            <GraphCard name="BLOCKTIME" chartData={this.props.graph_data["blockTime"]}/>
            <GraphCard name="DIFFICULTY" chartData={this.props.graph_data["difficulty"]}/>
            <GraphCard name="GAS LIMIT" chartData={this.props.graph_data["gasLimit"]}/>
            <GraphCard name="PENDING TX" chartData={this.props.graph_data["pendingTx"]}/>
            <GraphCard name="GAS SPENDING" chartData={this.props.graph_data["gasSpending"]}/>
        </div>
    );
  }
}

export default GraphCardContainer;
