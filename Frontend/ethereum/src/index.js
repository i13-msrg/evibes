import React from 'react';
import ReactDOM from 'react-dom';
import './index.css';
import App from './App';
import * as serviceWorker from './serviceWorker';
import StatsCardContainer from './components/StatsCardContainer';
import GraphCardContainer from './components/GraphCardContainer';
import NodeTable from './components/NodeTable'
import Controls from './components/Controls'

import {Navbar, Button, Alignment} from "@blueprintjs/core";

ReactDOM.render(
<div>
    <Navbar>
        <Navbar.Group align={Alignment.LEFT}>
            <Navbar.Heading>eVIBES</Navbar.Heading>
            <Navbar.Divider />
            <Button className="bp3-minimal" icon="home" text="Home" />
            <Button className="bp3-minimal" icon="document" text="Logs" />
        </Navbar.Group>
    </Navbar>
    <div className="Root">
        <br></br>
        <StatsCardContainer/>
        <br></br>
        <GraphCardContainer/>
        <NodeTable/>
        <div className="FixedControl">
            <Controls/>
        </div>
    </div>
</div>
, document.getElementById('root'));

// If you want your app to work offline and load faster, you can change
// unregister() to register() below. Note this comes with some pitfalls.
// Learn more about service workers: http://bit.ly/CRA-PWA
serviceWorker.unregister();
