import React, { Component } from 'react';
import './Home.css';
import { Button, Card, Elevation,FormGroup, NumericInput, ControlGroup} from "@blueprintjs/core";


class Home extends Component {
    render() {
        return (
            <div  className="Home">
                <Card interactive={true} elevation={Elevation.TWO}>
                    <h3 className="center">Edit the configuration to start the eVIBES simulator</h3>
                    <div className="Settings">
                        <div className="Group top_margin"> 
                            <h5 className="center">Boot node configuration</h5>
                            <ControlGroup fill={true} vertical={false} className="center-components">
                                <FormGroup
                                    helperText="Initial number of nodes that will be initialized using the Genesis block"
                                    label="Number of Boot nodes"
                                    labelInfo="(required)"
                                    className="FormGrp"
                                >
                                    <NumericInput placeholder="Enter a number..."/>
                                </FormGroup>
                                <FormGroup
                                    helperText="Initial number of Accounts that will be initialized using the Genesis block"
                                    label="Number of Initial Accounts"
                                    labelInfo="(required)"
                                    className="FormGrp"
                                >
                                    <NumericInput placeholder="Enter a number..."/>
                                </FormGroup>
                            </ControlGroup>
                        </div>
                        

                        <div className="Group top_margin"> 
                            <h5 className="center">Transaction configuration</h5>
                            <ControlGroup fill={true} vertical={false} className="center-components">
                                <FormGroup
                                    helperText="These are the total number of transactions that will be generated combined in all the batches"
                                    label="Number of Transaction"
                                    labelInfo="(required)"
                                    className="FormGrp"
                                >
                                    <NumericInput placeholder="Enter a number..."/>
                                </FormGroup>
                                <FormGroup
                                    helperText="Based will be created based on the on the transaction rate."
                                    label="Transactions in a batch"
                                    labelInfo="(required)"
                                    className="FormGrp"
                                >
                                    <NumericInput placeholder="Enter a number..."/>
                                </FormGroup>
                            </ControlGroup>
                        </div>
                        <div className="Group top_margin"> 
                            <ControlGroup fill={true} vertical={false} className="center-components">
                                <FormGroup
                                    helperText="Rate at which transactions will be generated. i.e Time for generating a batch of transactions"
                                    label="Rate of transactions"
                                    labelInfo="(required)"
                                    className="FormGrp"
                                >
                                    <NumericInput placeholder="Enter a number..."/>
                                </FormGroup>
                            </ControlGroup>
                        </div>

                        <div className="Group top_margin"> 
                            <h5 className="center">Post Initialization configuration</h5>
                            <ControlGroup fill={true} vertical={false} className="center-components">
                                <FormGroup
                                    helperText="These are the total number of nodes in the simulation"
                                    label="Number of Nodes"
                                    labelInfo="(required)"
                                    className="FormGrp"
                                >
                                    <NumericInput placeholder="Enter a number..."/>
                                </FormGroup>
                                <FormGroup
                                    helperText="Accounts that will be generated during the simulation"
                                    label="Number of Accounts"
                                    labelInfo="(required)"
                                    className="FormGrp"
                                >
                                    <NumericInput placeholder="Enter a number..."/>
                                </FormGroup>
                            </ControlGroup>
                        </div>
                        <div className="Group top_margin"> 
                            <ControlGroup fill={true} vertical={false} className="center-components">
                                <FormGroup
                                    helperText="Number of peer connections per node"
                                    label="Min-Max Peers per Node"
                                    labelInfo="(required)"
                                    className="FormGrp"
                                >
                                    <NumericInput placeholder="Enter a number..."/>
                                </FormGroup>
                                <FormGroup
                                        helperText="Initial Block gas limit for the blockchain"
                                        label="BlockGasLimit"
                                        labelInfo="(required)"
                                        className="FormGrp"
                                    >
                                    <NumericInput placeholder="Enter a number..."/>
                                </FormGroup>
                            </ControlGroup>
                        </div>
                        <div className="Group top_margin"> 
                            <h5 className="center">Genesis Block configuration</h5>
                            <div className="Group top_margin"> 
                            <ControlGroup fill={true} vertical={false} className="center-components">
                                <FormGroup
                                    helperText="Hash of the parent Block"
                                    label="Parent Hash"
                                    labelInfo="(default)"
                                    className="FormGrp"
                                >
                                    <input type="text" disabled={true} class="bp3-input" placeholder="Filter histogram..." />
                                </FormGroup>
                                <FormGroup
                                        helperText="Hash of the list of Ommer Blocks"
                                        label="Ommer Hash"
                                        labelInfo="(default)"
                                        className="FormGrp"
                                    >
                                    <input type="text" disabled={true} class="bp3-input" placeholder="Filter histogram..." />
                                </FormGroup>
                            </ControlGroup>
                        </div>
                        <div className="Group top_margin"> 
                            <ControlGroup fill={true} vertical={false} className="center-components">
                                <FormGroup
                                    helperText="Benificiary account address"
                                    label="Benificiary"
                                    labelInfo="(default)"
                                    className="FormGrp"
                                >
                                    <input type="text" disabled={true} class="bp3-input" placeholder="Filter histogram..." />
                                </FormGroup>
                                <FormGroup
                                        helperText="Initial difficulty for genesis block"
                                        label="Difficulty"
                                        labelInfo="(required)"
                                        className="FormGrp"
                                    >
                                    <NumericInput placeholder="Enter a number..."/>
                                </FormGroup>
                            </ControlGroup>
                        </div>
                        <div className="Group top_margin"> 
                            <ControlGroup fill={true} vertical={false} className="center-components">
                                <FormGroup
                                    helperText="Initial Gas limit for genesis block"
                                    label="Gas Limit"
                                    labelInfo="(required)"
                                    className="FormGrp"
                                >
                                    <NumericInput placeholder="Enter a number..."/>
                                </FormGroup>
                                <FormGroup
                                        helperText="Initial Gas used for genesis block"
                                        label="Gas Used"
                                        labelInfo="(required)"
                                        className="FormGrp"
                                    >
                                    <NumericInput placeholder="Enter a number..."/>
                                </FormGroup>
                            </ControlGroup>
                        </div>
                        <div className="Group top_margin"> 
                            <ControlGroup fill={true} vertical={false} className="center-components">
                                <FormGroup
                                    helperText="Initial block number"
                                    label="Number"
                                    labelInfo="(required)"
                                    className="FormGrp"
                                >
                                    <NumericInput disabled={true} placeholder="Enter a number..."/>
                                </FormGroup>
                            </ControlGroup>
                        </div>
                        </div>
                        <div className="button">
                            <Button leftIcon="build" intent="primary" text="Start the Simulation"/>
                        </div>
                    </div>
                </Card>
            </div>        
        );
    }
}

export default Home;