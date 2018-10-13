import React, { Component } from 'react';
import './NodeRow.css'
import {BarChart, Bar} from 'recharts';

const data = [
  {name: 'Page A', uv: 40, pv: 2400, amt: 2400},
  {name: 'Page B', uv: 30, pv: 1398, amt: 2210},
  {name: 'Page C', uv: 20, pv: 9800, amt: 2290},
  {name: 'Page D', uv: 27, pv: 3908, amt: 2000},
  {name: 'Page E', uv: 18, pv: 4800, amt: 2181},
  {name: 'Page F', uv: 23, pv: 3800, amt: 2500},
  {name: 'Page G', uv: 34, pv: 4300, amt: 2100},
  {name: 'Page H', uv: 14, pv: 1300, amt: 3100},
  {name: 'Page A', uv: 40, pv: 2400, amt: 2400},
  {name: 'Page B', uv: 30, pv: 1398, amt: 2210},
  {name: 'Page C', uv: 20, pv: 9800, amt: 2290},
  {name: 'Page D', uv: 27, pv: 3908, amt: 2000},
  {name: 'Page E', uv: 18, pv: 4800, amt: 2181},
  {name: 'Page F', uv: 23, pv: 3800, amt: 2500},
  {name: 'Page G', uv: 34, pv: 4300, amt: 2100},
  {name: 'Page H', uv: 14, pv: 1300, amt: 3100},
  {name: 'Page A', uv: 40, pv: 2400, amt: 2400},
  {name: 'Page B', uv: 30, pv: 1398, amt: 2210},
  {name: 'Page C', uv: 20, pv: 9800, amt: 2290},
  {name: 'Page D', uv: 27, pv: 3908, amt: 2000},
  {name: 'Page E', uv: 18, pv: 4800, amt: 2181},
  {name: 'Page F', uv: 23, pv: 3800, amt: 2500},
  {name: 'Page G', uv: 34, pv: 4300, amt: 2100},
  {name: 'Page H', uv: 14, pv: 1300, amt: 3100},
];


class NodeRow extends Component {
    constructor(props, attr) {
        super(props)
        this.state.name = attr.name
        this.state.nodeType = attr.nodeType
        this.state.latency = attr.latency
        this.state.poolerState = attr.poolerState
        this.state.evmState = attr.evmState
        this.state.nodeState = attr.nodeState
        this.state.peers = attr.peers
        this.state.pendingTx = attr.pendingTx
        this.state.lastBlock = attr.lastBlock
        this.state.avgPropTime = attr.avgPropTime
        this.state.totalDifficulty = attr.totalDifficulty
    }
    
    render() {
      return (
        <tr>
            <td>{this.state.name}</td>
            <td>{this.state.nodeType}</td>
            <td>{this.state.latency}</td>
            <td>{this.state.poolerState}</td>
            <td>{this.state.evmState}</td>
            <td>{this.state.nodeState}</td>
            <td>{this.state.peers}</td>
            <td>{this.state.pendingTx}</td>
            <td>{this.state.lastBlock}</td>
            <td>{this.state.avgPropTime}</td>
            <td>{this.state.totalDifficulty}</td>
        </tr>
      );
    }
  }
  
  export default NodeRow;

