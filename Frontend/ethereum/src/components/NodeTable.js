import React, { Component } from 'react';
import {Column, Table } from "@blueprintjs/table";
 

class NodeTable extends Component {
    render() {
      return (
        <div className="NodeTable">
            <Table numRows={10}>
                <Column name="NAME"/>
                <Column name="NODE TYPE"/>
                <Column name="LATENCY"/>
                <Column name="isMINING"/>
                <Column name="PEERS"/>
                <Column name="PENDING-TX"/>
                <Column name="LAST BLOCK"/>
                <Column name="AVG. PROPOGATION TIME"/>
                <Column name="TOTAL DIFFICULTY"/>
                <Column name="PROPOGATION TIME GRAPH"/>
            </Table>
        </div>
      );
    }
  }
  
  export default NodeTable;

