import React, { Component } from 'react';
import ReactTable from 'react-table'

const columns = [{
  Header: 'Name',
  accessor: 'id'
},{
  Header: 'nodeType',
  accessor: 'nodeType'
},{
  Header: 'latency',
  accessor: 'propTime'
},{
  Header: 'AvgLatency',
  accessor: 'avgPropTime'
},{
  Header: 'peers',
  accessor: 'peers'
},{
  Header: 'AvgPeers',
  accessor: 'avgPeers'
},{
  Header: 'pendingTx',
  accessor: 'pendingTx'
},{
  Header: 'AvgPendingTx',
  accessor: 'avgPendingTx'
},{
  Header: 'GasAccmulated',
  accessor: 'poolGasAcc'
},{
  Header: 'lastBlock',
  accessor: 'blockNum'
},{
  Header: 'lastBlockTime',
  accessor: 'blockTime'
},{
  Header: 'AvgBlockTime',
  accessor: 'avgBlockTime'
},{
  Header: 'avgPropTime',
  accessor: 'avgPropTime'
},{
  Header: 'Difficulty',
  accessor: 'difficulty'
},{
  Header: 'AvgDifficulty',
  accessor: 'avgDifficulty'
},{
  Header: 'TxCost',
  accessor: 'txCost'
},{
  Header: 'AvgTxCost',
  accessor: 'avgTxCost'
},{
  Header: 'GasSpending',
  accessor: 'gasSpending'
},{
  Header: 'AvgGasSpending',
  accessor: 'avgGasSpending'
},{
  Header: 'GasLimit',
  accessor: 'gasLimit'
},{
  Header: 'AvgGasLimit',
  accessor: 'avgGasLimit'
},{
  Header: 'evmState',
  accessor: 'evmState'
},{
  Header: 'poolState',
  accessor: 'poolState'
},{
  Header: 'nodeState',
  accessor: 'nodeState'
}]


class NodeTable extends Component {
  render() {
    return(
    <ReactTable
      data={this.props.state_data}
      columns={columns}
    />
    )}
  }
  
  export default NodeTable;
  