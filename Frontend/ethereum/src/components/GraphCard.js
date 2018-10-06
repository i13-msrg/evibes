import React, { Component } from 'react';
import './GraphCard.css';
import {Elevation, Card } from "@blueprintjs/core";
import {linearGradient, AreaChart,CartesianGrid,XAxis,YAxis,Tooltip,Area } from 'recharts';

const data = [
    {name: 'Page A', uv: 4000, pv: 2400, amt: 2400},
    {name: 'Page B', uv: 3000, pv: 1398, amt: 2210},
    {name: 'Page C', uv: 2000, pv: 9800, amt: 2290},
    {name: 'Page D', uv: 2780, pv: 3908, amt: 2000},
    {name: 'Page E', uv: 1890, pv: 4800, amt: 2181},
    {name: 'Page F', uv: 2390, pv: 3800, amt: 2500},
    {name: 'Page G', uv: 3490, pv: 4300, amt: 2100},
    {name: 'Page H', uv: 1490, pv: 1300, amt: 3100},
];

class GraphCard extends Component {
  render() {
    return (
        <Card interactive={false} elevation={Elevation.ONE} className="GraphCard">
            <div className="graphContainer">
                <div className="graphContainer-name"><span>{this.props.name}</span></div>
                <div className="graphContainer-graph">
                    <span>
                    <AreaChart width={300} height={200} data={data}
                        margin={{ top: 10, right: 30, left: 0, bottom: 0 }}>
                        <defs>
                            <linearGradient id="colorPv" x1="0" y1="0" x2="0" y2="1">
                            <stop offset="5%" stopColor="#82ca9d" stopOpacity={0.8}/>
                            <stop offset="95%" stopColor="#82ca9d" stopOpacity={0}/>
                            </linearGradient>
                        </defs>
                        <Tooltip />
                        <Area type="monotone" dataKey="pv" stroke="#82ca9d" fillOpacity={1} fill="url(#colorPv)" />
                    </AreaChart>
                    </span></div>
            </div>
        </Card>
    );
  }
}

export default GraphCard;