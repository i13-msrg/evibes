import React, { Component } from 'react';
import './PlusMinusControl.css';
import { Button } from "@blueprintjs/core";


class PlusMinusControl extends Component {
    render() {
        return (
            <div  className="PlusMinusControl">
                    <div className="AddIcon"><Button icon="add"/></div>
                    <div className="NameTag"><span>{this.props.name}</span></div>
                    <div className="RemoveIcon"><Button icon="remove"/></div>
            </div>        
        );
    }
}

export default PlusMinusControl;
