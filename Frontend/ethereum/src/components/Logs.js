import React, { Component } from 'react';
import './Logs.css';
import {Card, Elevation} from "@blueprintjs/core";


class Logs extends Component {
    render() {
        return (
            <div  className="Logs">
                <Card interactive={true} elevation={Elevation.TWO}>
                    <h3 className="center">Logs from the eVIBES server</h3>
                    <div className="Terminal">

                    </div>
                </Card>
            </div>        
        );
    }
}

export default Logs;