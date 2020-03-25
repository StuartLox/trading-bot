import React, { Component } from 'react';
import ReactTable from 'react-table';
import { getInitialPriceData } from './DataProvider';
import 'react-table/react-table.css';

class App extends Component {
  constructor(props) {
    super(props);
    this.state = {
      data: getInitialPriceData()
    };

    this.columns = [{
      Header: 'Symbol',
      accessor: 'symbol'
    }, {
      Header: 'Timestamp',
      accessor: 'timestamp'
    }, {
      Header: 'AveragePrice',
      accessor: 'averagePrice'
    }];

    this.eventSource = new EventSource('http://localhost:8123/updates');
  }

  componentDidMount() {
    this.eventSource.addEventListener('priceStateUpdate', (e) => 
    this.updatePriceState(JSON.parse(e.data)));
  }

  updatePriceState(priceState) {
    console.log("Checkpoint1")
    console.log(priceState)
    this.setState(Object.assign({ data: this.state.data.concat(priceState) }));
    console.log(this.state)
  }

  stopUpdates() {
    this.eventSource.close();
  }

  render() {
    return (
      <div className="App">
        <button onClick={() => this.stopUpdates()}>Stop updates</button>
        <ReactTable
          data={this.state.data}
          columns={this.columns}
        />
      </div>
    );
  }
}

export default App;
