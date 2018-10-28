
import React, { Component } from 'react';
import './App.css';

import Home from './components/Home.js'
import StatsCardContainer from './components/StatsCardContainer';
import GraphCardContainer from './components/GraphCardContainer';
import AgTable from './components/AgTable'
import Controls from './components/Controls'
import update from 'immutability-helper';
import 'react-table/react-table.css'
import {Navbar, Button, Alignment} from "@blueprintjs/core";
import Logs from './components/Logs';


class App extends Component {
    constructor(props) {
        super(props)
        this.state = {
            data: [], map: {}, 
            local_data: {}, 
            global_data: {
                'blockNum' : 0,
                'uncleCount' : 0,
                'blockTime' : 0,
                'txCost' : 0,
                'gasSpending' : 0,
                'gasLimit' : 0,
                'peers' : 0,
                'pendingTx' : 0,
                'propTime' : 0,
                'difficulty' : 0,
            },
            graph_data: {
                blockTime:[],
                difficulty:[],
                propTime:[],
                gasLimit:[],
                pendingTx:[],
                gasSpending:[],
                txCost:[],
            },
            visibility : {
                home: false,
                logs: true,
                dashboard: false   
            }
        }

        this.globalEventSource = new EventSource('http://localhost:8080/global-events');
        //this.localEventSource = new EventSource('http://localhost:8080/local-events');
        //this.stateEventSource = new EventSource('http://localhost:8080/state-events');
    }

    componentDidMount() {
        this.globalEventSource.onmessage = (e) => this.globalEventData(e.data);
        //this.localEventSource.onmessage = (e) => this.localEventData(e.data);
        //this.stateEventSource.onmessage = (e) => this.stateEventData(e.data);
    }

    globalEventData(nodeState) {
        let state = {}
        try{
          state = JSON.parse(nodeState)
          for (var attr in state) {
              this.setState({
                global_data: update(this.state.global_data,  {[attr]: {$set: state[attr]}})
              })
          }
          this.graphDataHandler(state)
        }
        catch{console.log("empty data packet")}
    }

    graphDataHandler(state) {
        console.log(state)
        try{
            let temp_graph_data = Object.assign({}, this.state.graph_data)
            for(var attr in temp_graph_data) {
                var nodeTemp = {"value":state[attr], "time": state["timestamp"]}
                temp_graph_data[attr] = temp_graph_data[attr].concat(nodeTemp)
            }
            this.setState({graph_data: temp_graph_data})
        } catch(e) {console.log(e)}
    }

    stateEventData(nodeState) {
        let temp = {}
        let state = {}
        let pos = -1
        try{
          state = JSON.parse(nodeState)
          console.log(nodeState)
          //console.log(this.state.data)
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
        //console.log(this.state.data) 
        } catch(e) {console.log(e)}
    }

    localEventData(nodeState) {
        let state = {}
        let pos = -1

        try{
          state = JSON.parse(nodeState)
          var id = state["clientId"]
          delete state["clientId"]
          state["id"] = id
          if(state["id"] in this.state.map) {
            pos = this.state.map[state["id"]]
          }
          if(pos === -1) {
            this.setState({data: this.state.data.concat(state)})
            let mapTemp = Object.assign({}, this.state.map)
            mapTemp[state["id"]] = this.state.data.length -1 
            this.setState({map: mapTemp})
          } else {
            var temp = Object.assign({}, this.state.data[pos], state)
            this.setState({
                data: update(this.state.data, {[pos]: {$set: temp}})
                })
            }
        } catch{console.log("empty data packet for local stats")}
    }

    handleHomeClick() {
        this.setState({visibility:{home: true}})
    }
    handleLogsClick() {
        this.setState({visibility:{logs: true}})
    }
    handleDashboardClick() {
        this.setState({visibility:{dashboard: true}})
    }

    render() {
        return (
            <div className="App">
                <Navbar>
                    <Navbar.Group align={Alignment.LEFT}>
                        <Navbar.Heading>eVIBES</Navbar.Heading>
                        <Navbar.Divider />
                        {this.state.visibility.dashboard &&  <Button className="bp3-minimal" onClick={this.handleHomeClick.bind(this)} icon="home" text="Home" />}
                        {this.state.visibility.dashboard &&  <Button className="bp3-minimal" onClick={this.handleLogsClick.bind(this)} icon="document" text="Logs" />}
                        {this.state.visibility.logs && <Button className="bp3-minimal" onClick={this.handleDashboardClick.bind(this)} icon="dashboard" text="Dashboard" />}
                    </Navbar.Group>
                </Navbar>
                <div className="Root">
                    {this.state.visibility.home && <div className="Home">
                        <Home/>
                    </div>}
                    {this.state.visibility.logs &&  <div className="Logs">
                        <Logs/>
                    </div>}
                    {this.state.visibility.dashboard &&  <div className="Dashboard">
                        <br></br>
                        <StatsCardContainer global_data={this.state.global_data}/>
                        <br></br>
                        <GraphCardContainer graph_data={this.state.graph_data}/>
                        <div className="tableClass">
                            <AgTable state_data={this.state.data}/>
                        </div>
                        <div className="FixedControl">
                            <Controls/>
                        </div>
                    </div>}
                </div>
            </div>
        );
    }
}

export default App;
