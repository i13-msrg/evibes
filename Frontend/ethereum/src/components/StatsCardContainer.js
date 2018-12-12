import React, { Component } from 'react';
import './StatsCardContainer.css';
import StatsCard from './StatsCard';

class StatsCardContainer extends Component {
  render() {
    return (
        <div className="StatsCardContainer">
            <StatsCard name="LATEST BLOCK" value={this.props.global_data.blockNum.toPrecision(4)} unit="block num" iconName="box" color="#10a0de"/>
            <StatsCard name="UNCLES" value={this.props.global_data.uncleCount.toPrecision(4)} unit="(curr/last 50)" iconName="graph" color="#10a0de"/>
            <StatsCard name="AVG PENDING TX" unit="tx" value={this.props.global_data.pendingTx.toPrecision(5)} iconName="cloud-upload" color="#10a0de"/>
            <StatsCard name="AVG. BLOCK TIME" unit="sec" value={this.props.global_data.blockTime.toPrecision(5)} iconName="time" color="#10a0de"/>
            <StatsCard name="DIFFICULTY" unit="" value={this.props.global_data.difficulty.toPrecision(5)} iconName="cog" color="#10a0de"/>
            <StatsCard name="AVG PEERS" value={this.props.global_data.peers.toPrecision(5)} iconName="desktop" color="#10a0de"/>
            <StatsCard name="GAS SPENDING" unit="gwei" value={this.props.global_data.gasSpending.toPrecision(5)} iconName="euro" color="#10a0de"/>
            <StatsCard name="GAS LIMIT" unit="gas" value={this.props.global_data.gasLimit.toPrecision(5)} iconName="tag" color="#10a0de"/>
            <StatsCard name="AVG. NETWORK LATENCY" unit="ms" value={this.props.global_data.propTime.toPrecision(5)} iconName="cell-tower" color="#10a0de"/>
        </div> 
    );
  }
}

export default StatsCardContainer;
