import React, { Component } from 'react';
import './PlusMinusControl.css';
import { Button, Card, Elevation } from "@blueprintjs/core";


class PlusMinusControl extends Component {
    render() {
        return (
            <div  className="PlusMinusControl">
                    <div className="AddIcon"><Button icon="add" iconSize={20}/></div>
                    <div className="NameTag"><span>{this.props.name}</span></div>
                    <div className="RemoveIcon"><Button icon="remove" iconSize={20}/></div>
            </div>        
        );
    }
}

export default PlusMinusControl;
