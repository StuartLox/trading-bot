import React, { Component } from 'react';
import { Chart } from 'react-chartjs-2';

class LineChart extends Component {
    constructor(props) {
        super(props);
        this.chartRef = React.createRef();
    }

    componentDidUpdate() {
        this.lineChart.data.labels = this.props.data.data.map(d => d.timestamp);
        this.lineChart.data.datasets[0].data = this.props.data.data.map(d => d.value);
        this.lineChart.update();
    }

    componentDidMount() {
        this.lineChart = new Chart(this.chartRef.current, {
            type: 'line',
            options: {
                maintainAspectRatio: false,
                title: {
                    display: true,
                    text: this.props.title,
                    fontSize: 25
                },
                legend: {
                    display: false
                }
            },
            data: {
                labels: this.props.data.data.map(d => d.timestamp),
                datasets: [{
                    label: this.props.title,
                    data: this.props.data.data.map(d => d.value),
                    fill: 'blue',
                    backgroundColor: this.props.color,
                    pointRadius: 2,
                    borderColor: this.props.color,
                    borderWidth: 1,
                    lineTension: 0
                }, {
                    label: 'Line Dataset',
                    data: this.props.data.data.map(d => d.value),
                    borderColor: this.props.color,
                    radius: 10,
                    backgroundColor: 'rgba(0, 0, 0, 0.1)',
                    // Changes this dataset to become a line
                    type: 'bubble',
                }]
            }
        });
    }

    render() {
        return <canvas ref={this.chartRef} />;
    }
}

export default LineChart;