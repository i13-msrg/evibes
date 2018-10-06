import React, { Component } from 'react';
import './StatsCard.css';
import {Elevation, Card } from "@blueprintjs/core";
import { Icon, Intent } from "@blueprintjs/core";
import { IconNames } from "@blueprintjs/icons";


class StatsCard extends Component {
    render() {
        return (
            <Card interactive={false} elevation={Elevation.ONE} className="StatsCard">
                <div className="statContainer" style={{color:this.props.color}}>
                    <Icon className="statContainer-icon" icon={this.props.iconName} iconSize={50}/>
                    <div className="statContainer-displayContainer">   
                        <div className="statContainer-name"><span>{this.props.name}</span></div>
                        <div className="statContainer-stat">
                            <div className="statContainer-value"><span>{this.props.value}</span></div>
                            <div className="statContainer-unit"><span>{this.props.unit}</span></div>
                        </div>
                    </div>
                </div>
            </Card>
        );
    }
}

export default StatsCard;
