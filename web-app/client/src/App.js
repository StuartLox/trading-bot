import React from 'react';
import LineChart from "./components/LineChart";
import DoughnutChart from "./components/DoughnutChart";
import { getInitialPriceData } from './DataProvider';
import './App.css';


class App extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      data: getInitialPriceData()
    };
    this.eventSource = new EventSource('http://localhost:5000/events');
  }

  componentDidMount() {
    const util = require('util');
    util.inspect(Date.now())
    this.eventSource.addEventListener('priceStateUpdate', (e) =>
      this.updatePriceState(JSON.parse(e.data)));
  }

  updatePriceState(priceState) {
   

    const date = new Date(priceState.timestamp).toLocaleTimeString()
    const averagePrice = parseFloat(priceState.averagePrice).toFixed(2)
    const record = { timestamp: date, value: averagePrice }

    this.state.data.push(record)
    this.setState(Object.assign({ data: this.state.data }));
  }

  render() {
    return (
      <div className="App">
        <div className="main chart-wrapper">
          <LineChart
            title="Bitcoin Price"
            data={this.state}
            color="#70CAD1"
          />
        </div>
        <div className="sub chart-wrapper">
          <DoughnutChart
            title="Stock Returns"
            colors={['#a8e0ff', '#8ee3f5', '#70cad1', '#3e517a', '#b08ea2', '#BBB6DF']}
          />
        </div>
        <div className="sub chart-wrapper">
          <LineChart
            title="Portfolio Returns"
            data={this.state}
            color="#70CAD1"
          />
        </div>
        <div className="sub chart-wrapper">
          <LineChart
            title="Average Bitcoin Price"
            data={this.state}
            color="#70CAD1"
          />
        </div>
      </div>
    );
  }
}

export default App;
