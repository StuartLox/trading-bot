import React, { Component } from 'react';
import { Line } from 'react-chartjs-2';
import { getInitialPriceData } from '../DataProvider';


class Chart extends Component {
  constructor(props) {
    super(props);
    this.state = {
      data: getInitialPriceData()
    };
    this.eventSource = new EventSource('http://localhost:5000/events');
    this.chart = React.createRef();
  }

  componentDidMount() {
    this.eventSource.addEventListener('priceStateUpdate', (e) =>
      this.updatePriceState(JSON.parse(e.data)));
  }

  updatePriceState(priceState) {
    const date = new Date(priceState.timestamp).toLocaleTimeString()
    const averagePrice = parseFloat(priceState.averagePrice).toFixed(2)
    this.state.data.labels.push(date)
    this.state.data.datasets[0].data.push(averagePrice)
    this.setState(Object.assign({ data: this.state.data }));
  }

  render() {
    return (
      <div>
        <h2>Average Bitcoin Price</h2>
        <Line
          data={this.state.data}
          width={1500}
          height={500}
          options={{
            maintainAspectRatio: false
          }}
        />
      </div>
    );
  }
}

export default Chart;