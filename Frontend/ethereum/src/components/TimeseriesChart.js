import React from 'react'
import PropTypes from 'prop-types'
import moment from 'moment'
import {linearGradient, AreaChart,Area } from 'recharts';
import {
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'


const TimeSeriesChart = ({ chartData }) => (
  <ResponsiveContainer width = {300} height = {200} margin={{ top: 10, right: 30, left: 0, bottom: 0 }}>
    <AreaChart data={chartData}>
        <XAxis
            dataKey = 'time'
            domain = {['auto', 'auto']}
            name = 'Time'
            tickFormatter = {(unixTime) => moment(unixTime).format('HH:mm Do')}
            type = 'number'
        />
        <YAxis dataKey = 'value' name = 'Value' />
        <defs>
            <linearGradient id="colorPv" x1="0" y1="0" x2="0" y2="1">
            <stop offset="5%" stopColor="#82ca9d" stopOpacity={0.8}/>
            <stop offset="95%" stopColor="#82ca9d" stopOpacity={0}/>
            </linearGradient>
        </defs>
        <Tooltip />
        <Area type="monotone" dataKey="value" stroke="#82ca9d" fillOpacity={1} fill="url(#colorPv)" />
    </AreaChart>
  </ResponsiveContainer>
)

TimeSeriesChart.propTypes = {
  chartData: PropTypes.arrayOf(
    PropTypes.shape({
      time: PropTypes.number,
      value: PropTypes.number
    })
  ).isRequired
}

export default TimeSeriesChart


