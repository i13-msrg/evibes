import React, { Component } from 'react';
import {AgGridReact} from 'ag-grid-react';

import 'ag-grid/dist/styles/ag-grid.css';
import 'ag-grid/dist/styles/ag-theme-material.css';

const columns = [{
  headerName: 'NAME',
  field: 'id'
},{
  headerName: 'PEERS',
  field: 'peers'
},{
  headerName: 'AVG. PEERS',
  field: 'avgPeers'
},{
  headerName: 'PENDING TRANSACTIONS',
  field: 'pendingTx'
},{
  headerName: 'AVG. PENDING TREANSACTIONS',
  field: 'avgPendingTx'
},{
  headerName: 'GAS ACCUMULATED',
  field: 'poolGasAcc'
},{
  headerName: 'LAST BLOCK MINED',
  field: 'blockNum'
},{
  headerName: 'LAST MINED BLOCKTIME',
  field: 'blockTime'
},{
  headerName: 'AVG BLOCK TIME',
  field: 'avgBlockTime'
},{
  headerName: 'DIFFICULTY',
  field: 'difficulty'
},{
  headerName: 'AVG. DIFFICULTY',
  field: 'avgDifficulty'
},{
  headerName: 'TRANSACTION COST',
  field: 'txCost'
},{
  headerName: 'AVG TRANSACTION COST',
  field: 'avgTxCost'
},{
  headerName: 'GAS SPENDING',
  field: 'gasSpending'
},{
  headerName: 'AVG. GAS SPENDING',
  field: 'avgGasSpending'
},{
  headerName: 'GAS LIMIT',
  field: 'gasLimit'
},{
  headerName: 'AVG GAS LIMIT',
  field: 'avgGasLimit'
},{
  headerName: 'PROPOGATION TIME',
  field: 'propTime'
},{
  headerName: 'AVG. PROPOGATION TIME',
  field: 'avgPropTime'
},{
  headerName: 'NODE TYPE',
  field: 'nodeType'
},{
  headerName: 'EVM STATE',
  field: 'evmState'
},{
  headerName: 'POOL STATE',
  field: 'poolState'
},{
  headerName: 'NODE STATE',
  field: 'nodeState'
}]


class AgTable extends Component {
  render() {
    return(
      <div
        className="ag-theme-material"
        style={{ 
        height: '400px', 
        width: '100%' }} >
      <AgGridReact
          columnDefs={columns}
          rowData={this.props.state_data}
          enableColResize={true}
          autoSizeColumns={true}
      />
      </div>
    )}
  }
  
  export default AgTable;
  