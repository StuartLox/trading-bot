import React, { Component } from 'react';
import { Chart } from 'react-chartjs-2';

class LineChart extends Component {
    constructor(props) {
        super(props);
        this.chartRef = React.createRef();
    }

    // componentDidUpdate() {
    //     this.lineChart.data.labels = this.props.data.data.map(d => d.timestamp);
    //     this.lineChart.data.datasets[0].data = this.props.data.data.map(d => d.value);
    //     this.lineChart.update();
    // }

    componentDidMount() {
        this.lineChart = new Chart(this.chartRef.current, {
            type: 'doughnut',
            options: {
                maintainAspectRatio: false,
                title: {
                    display: true,
                    text: 'Portfolio',
                    fontSize: 25
                }
            },
            // data: {
            //     labels: this.props.data.data.map(d=>d.timestamp),
            //     datasets: [{
            //         data: this.props.data.data.map(d => d.value),
            //         backgroundColor: this.props.color
            //     }]
            // }
            data: {
                labels: ["BTC", "APPL", "ETH", "TSLA"],
                datasets: [{
                    data: [121, 54, 35, 92],
                    backgroundColor: this.props.colors
                }]
            }
        });
    }

    render() {
        return <canvas ref={this.chartRef} />;
    }
}

export default LineChart;