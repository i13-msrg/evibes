import React, { Component } from 'react';
import './GraphCardContainer.css';
import GraphCard from './GraphCard';

class GraphCardContainer extends Component {
  render() {
    return (
        <div className="GraphCardContainer">
            <GraphCard name="BLOCKTIME"/>
            <GraphCard name="DIFFICULTY"/>
            <GraphCard name="BLOCK PROPOGATION"/>
            <GraphCard name="GAS LIMIT"/>
            <GraphCard name="UNCLE COUNT"/>
            <GraphCard name="TX. GENERATION RATE"/>
            <GraphCard name="GAS SPENDING"/>
            <GraphCard name="TX. PROCESSING RATE"/>
        </div>
    );
  }
}

export default GraphCardContainer;
