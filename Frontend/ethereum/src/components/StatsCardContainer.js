import React, { Component } from 'react';
import './StatsCardContainer.css';
import StatsCard from './StatsCard';

class StatsCardContainer extends Component {

  componentDidMount() {
    
  }
  render() {
    return (
        <div className="StatsCardContainer">
            <StatsCard name="BEST BLOCK" value="100" unit="block num" iconName="box" color="#10a0de"/>
            <StatsCard name="UNCLES" value="100" unit="(curr/last 50)" iconName="graph" color="#10a0de"/>
            <StatsCard name="LAST BLOCK" unit="sec ago" value="100" iconName="cloud-upload" color="#f74b4b"/>
            <StatsCard name="AVG. BLOCK TIME" unit="sec" value="100" iconName="time" color="#ffd162"/>
            <StatsCard name="DIFFICULTY" unit="" value="100" iconName="cog" color="#ff8a00"/>
            <StatsCard name="TX GENERATION RATE" unit="per sec" value="100" iconName="settings" color="#f74b4b"/>
            <StatsCard name="ACTIVE NODES" value="100" iconName="desktop" color="#7bcc3a"/>
            <StatsCard name="GAS PRICE" unit="gwei" value="100" iconName="euro" color="#10a0de"/>
            <StatsCard name="GAS LIMIT" unit="gas" value="100" iconName="tag" color="#10a0de"/>
            <StatsCard name="AVG. NETWORK LATENCY" unit="ms" value="100" iconName="cell-tower" color="#ffd162"/>
        </div> 
    );
  }
}

export default StatsCardContainer;
