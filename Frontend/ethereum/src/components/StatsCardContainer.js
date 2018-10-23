import React, { Component } from 'react';
import './StatsCardContainer.css';
import StatsCard from './StatsCard';

class StatsCardContainer extends Component {
  render() {
    return (
        <div className="StatsCardContainer">
            <StatsCard name="LATEST BLOCK" value={this.props.global_data.blockNum} unit="block num" iconName="box" color="#10a0de"/>
            <StatsCard name="UNCLES" value={this.props.global_data.uncleCount} unit="(curr/last 50)" iconName="graph" color="#10a0de"/>
            <StatsCard name="AVG PENDING TX" unit="tx" value={this.props.global_data.pendingTx} iconName="cloud-upload" color="#f74b4b"/>
            <StatsCard name="AVG. BLOCK TIME" unit="sec" value={this.props.global_data.blockTime} iconName="time" color="#ffd162"/>
            <StatsCard name="DIFFICULTY" unit="" value={this.props.global_data.difficulty} iconName="cog" color="#ff8a00"/>
            <StatsCard name="Tx COST" unit="per sec" value={this.props.global_data.txCost} iconName="settings" color="#f74b4b"/>
            <StatsCard name="AVG PEERS" value={this.props.global_data.peers} iconName="desktop" color="#7bcc3a"/>
            <StatsCard name="GAS SPENDING" unit="gwei" value={this.props.global_data.gasSpending} iconName="euro" color="#10a0de"/>
            <StatsCard name="GAS LIMIT" unit="gas" value={this.props.global_data.gasLimit} iconName="tag" color="#10a0de"/>
            <StatsCard name="AVG. NETWORK LATENCY" unit="ms" value={this.props.global_data.propTime} iconName="cell-tower" color="#ffd162"/>
        </div> 
    );
  }
}

export default StatsCardContainer;
