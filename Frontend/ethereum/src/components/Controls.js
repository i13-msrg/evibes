import React, { Component } from 'react';
import './Controls.css';
import {Elevation, Card } from "@blueprintjs/core";
import { Button, Divider } from "@blueprintjs/core";
import PlusMinusControl from './PlusMinusControl'

class Controls extends Component {
    render() {
        return (
            <div>
                <Card interactive={true} elevation={Elevation.FOUR} className="Controls">
                <div className="StartStopControl">
                    <Button icon="play">PLAY</Button>
                    <Button icon="stop">STOP</Button>
                </div>
                <Divider className="Divider"/>
                <div className="NodeControl">
                    <PlusMinusControl name="NODES"/>
                </div>
                <div className="TxRateControl">
                    <PlusMinusControl name="TX. RATE"/>
                </div>
                <div className="MinerRateControl">
                    <PlusMinusControl name="MINER GAS LIMIT"/>
                </div>
                </Card>
            </div>
        );
    }
}

export default Controls;
