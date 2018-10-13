import React, { Component } from 'react';
import ReactTable from 'react-table'
import {BarChart, Bar} from 'recharts';
import update from 'immutability-helper';


const data1 = [
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

const columns = [{
  Header: 'Name',
  accessor: 'id'
},{
  Header: 'nodeType',
  accessor: 'nodeType'
},
{
  Header: 'latency',
  accessor: 'latency'
},{
  Header: 'evmState',
  accessor: 'evmState'
},{
  Header: 'poolState',
  accessor: 'poolState'
},{
  Header: 'nodeState',
  accessor: 'nodeState'
},{
  Header: 'peers',
  accessor: 'peers'
},{
  Header: 'pendingTx',
  accessor: 'pendingTx'
},{
  Header: 'lastBlock',
  accessor: 'lastBlock'
},{
  Header: 'avgPropTime',
  accessor: 'avgPropTime'
},{
  Header: 'totalDifficulty',
  accessor: 'totalDifficulty'
}]


class NodeTable extends Component {
  constructor(props) {
    super(props)
    this.state = {data: [], map: {}}
    this.eventSource = new EventSource('http://localhost:8080/events');
  }

  componentDidMount() {
    //this.eventSource.onmessage = (e) => console.log(JSON.parse(e.data));
    this.eventSource.onmessage = (e) => this.updateData(e.data);
  }

  updateData(nodeState) {
    let temp = {}
    let mapElem = {} 
    let state = {}
    let pos = -1
    try{
      state = JSON.parse(nodeState)
      console.log(nodeState)
      console.log(this.state.data)
      temp["id"] = state["id"]
      temp[state["attr"]] = state["value"]
    }
    catch{console.log("empty data packet")}

    try{
      if(state["id"] in this.state.map) {
        pos = this.state.map[state["id"]]
      }
      if(pos === -1) {
        this.setState({data: this.state.data.concat(temp)})
        let mapTemp = Object.assign({}, this.state.map)
        mapTemp[state["id"]] = this.state.data.length -1 
        this.setState({map: mapTemp})
      } else {
        //Update existing element
        if(state["attr"] in this.state.data[pos]) {
          this.setState({
            data: update(this.state.data, {[pos]: {[state["attr"]]: {$set: state["value"]}}})
          })
        } else {
          //add the attribute
          let nodeTemp = Object.assign({}, this.state.data[pos])
          nodeTemp[state["attr"]] = state["val"]
          this.setState({
            data: update(this.state.data, {[pos]: {$set: nodeTemp}})
          })
        }
        
      } 
    } catch(e) {console.log(e)}
  }

  render() {
    return(
    <ReactTable
      data={this.state.data}
      columns={columns}
    />
    )}
  }
  
  export default NodeTable;





  

  