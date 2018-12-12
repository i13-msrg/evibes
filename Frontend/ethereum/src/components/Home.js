import React, { Component } from 'react';
import './Home.css';
import { Button, Card, Elevation,FormGroup, NumericInput, ControlGroup} from "@blueprintjs/core";


class Home extends Component {
    constructor(props) {
        super(props)
        this.state = {
            bootNodes : 3,
            nodesNum : 3,
            txNum : 20000,
            accountsNum : 10,
            bootAccNum : 10,
            txBatch : 20,
            blockGasLimit : 10,
            poolSize : 100,
            minConn : 2,
            maxConn : 5,
            difficulty : 17179869184,
            gasLimit : 63000,
            gasUsed : 0
        };
    
        this.handleSubmit = this.handleSubmit.bind(this);
        this.handleBootNodesChange = this.handleBootNodesChange.bind(this);
        this.handleNodesNumChange = this.handleNodesNumChange.bind(this);
        this.handleTxNumChange = this.handleTxNumChange.bind(this);
        this.handleAccountsNumChange = this.handleAccountsNumChange.bind(this); 
        this.handleBootAccNumChange = this.handleBootAccNumChange.bind(this);
        this.handleBlockGasLimitChange = this.handleBlockGasLimitChange.bind(this);        
        this.handlePoolSizeChange = this.handlePoolSizeChange.bind(this);
        this.handleMinConnChange = this.handleMinConnChange.bind(this);
        this.handleMaxConnChange = this.handleMaxConnChange.bind(this);
        this.handleDifficultyChange = this.handleDifficultyChange.bind(this);
        this.handleGasLimitChange = this.handleGasLimitChange.bind(this);
        this.handleGasUsedChange = this.handleGasUsedChange.bind(this);
    }

    handleBootNodesChange(e) {
        this.setState({
            bootNodes: e.target.value
        });
    }

    handleNodesNumChange(e) {
        this.setState({
            nodesNum: e.target.value
        });
    }

    handleTxNumChange(e) {
        this.setState({
            txNum: e.target.value
        });
    }

    handleTxBatchChange(e) {
        this.setState({
            txBatch: e.target.value
        });
    }

    handleAccountsNumChange(e) {
        this.setState({
            accountsNum: e.target.value
        });
    }

    handleBootAccNumChange(e) {
        this.setState({
            txBatch: e.target.value
        });
    }

    handleBlockGasLimitChange(e) {
        this.setState({
            blockGasLimit: e.target.value
        });
    }

    handlePoolSizeChange(e) {
        this.setState({
            poolSize: e.target.value
        });
    }

    handleMinConnChange(e) {
        this.setState({
            minConn: e.target.value
        });
    }

    handleMaxConnChange(e) {
        this.setState({
            maxConn: e.target.value
        });
    }

    handleDifficultyChange(e) {
        this.setState({
            difficulty: e.target.value
        });
    }

    handleGasLimitChange(e) {
        this.setState({
            gasLimit: e.target.value
        });
    }

    handleGasUsedChange(e) {
        this.setState({
            gasUsed: e.target.value
        });
    }


    handleSubmit(event) {
        console.log("HERE")
        event.preventDefault();
        console.log(this.state)
        fetch('http://localhost:8080/input', {
          method: 'POST',
          body: JSON.stringify(this.state),
          headers: new Headers({
            'Content-Type': 'application/json'
        })
        });

    }
    
    render() {
        return (
            <div  className="Home">
                <Card interactive={false} elevation={Elevation.TWO}>
                    <h3 className="center">Edit the configuration to start the eVIBES simulator</h3>
                    <form onSubmit={this.handleSubmit}>
                        <div className="Settings">
                        <div className="Group top_margin"> 
                            <h5 className="center">Boot node configuration</h5>
                            <ControlGroup fill={true} vertical={false} className="center-components">
                                <FormGroup
                                    helperText="Initial number of nodes that will be initialized using the Genesis block"
                                    label="Number of Boot nodes"
                                    labelInfo="(required)"
                                    className="FormGrp"
                                    name="bootNodes"
                                >
                                    <NumericInput value={this.state.bootNodes} onChange={this.handleBootNodesChange}/>
                                </FormGroup>
                                <FormGroup
                                    helperText="Initial number of Accounts that will be initialized using the Genesis block"
                                    label="Number of Initial Accounts"
                                    labelInfo="(required)"
                                    className="FormGrp"
                                    name="initAcc"
                                >
                                    <NumericInput value={this.state.accountsNum} onChange={this.handleAccountsNumChange}/>
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
                                    name="txNum"
                                >
                                    <NumericInput value={this.state.txNum} onChange={this.handleTxNumChange}/>
                                </FormGroup>
                                <FormGroup
                                    helperText="Based will be created based on the on the transaction rate."
                                    label="Transactions in a batch"
                                    labelInfo="(required)"
                                    className="FormGrp"
                                    name="txBatch"
                                >
                                    <NumericInput value={this.state.txBatch} onChange={this.handleTxBatchChange}/>
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
                                    name="txRate"
                                >
                                    <NumericInput value={0.5}/>
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
                                    name="nodesNum"
                                >
                                    <NumericInput value={this.state.nodesNum} onChange={this.handleNodesNumChange}/>
                                </FormGroup>
                                <FormGroup
                                    helperText="Accounts that will be generated during the simulation"
                                    label="Number of Accounts"
                                    labelInfo="(required)"
                                    className="FormGrp"
                                    name="accNum"
                                >
                                    <NumericInput value={this.state.accountsNum} onChange={this.handleAccountsNumChange}/>
                                </FormGroup>
                            </ControlGroup>
                        </div>
                        <div className="Group top_margin"> 
                            <ControlGroup fill={true} vertical={false} className="center-components">
                                <FormGroup
                                    helperText="Number of peer connections per node"
                                    label="Min Peers per Node"
                                    labelInfo="(required)"
                                    className="FormGrp"
                                    name="minPeers"
                                >
                                    <NumericInput value={this.state.minConn} onChange={this.handleMinConnChange}/>
                                </FormGroup>
                                <FormGroup
                                        helperText="Number of peer connections per nod"
                                        label="Max Peers per node"
                                        labelInfo="(required)"
                                        className="FormGrp"
                                        name="maxPeers"
                                    >
                                    <NumericInput value={this.state.maxConn} onChange={this.handleMaxConnChange}/>
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
                                        name="genDiff"
                                    >
                                    <NumericInput value={this.state.difficulty} onChange={this.handleDifficultyChange}/>
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
                                    name="genGasLimit"
                                >
                                    <NumericInput value={this.state.gasLimit} onChange={this.handleGasLimitChange}/>
                                </FormGroup>
                                <FormGroup
                                        helperText="Initial Gas used for genesis block"
                                        label="Gas Used"
                                        labelInfo="(required)"
                                        className="FormGrp"
                                        name="genGasUsed"
                                    >
                                    <NumericInput value={this.state.gasUsed} onChange={this.handleGasUsedChange}/>
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
                            <Button leftIcon="build" intent="primary" type="submit" text="Save the settings"/>
                        </div>
                    </div>
                    </form>
                </Card>
            </div>        
        );
    }
}

export default Home;