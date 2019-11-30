// assume that API service is published on same server that is the server of this HTML file
var source = new EventSource("../updates");
source.onmessage = function (data) {
    var message = JSON.parse(data.data);
    var table = document.getElementById("stockTable");
    var row = table.insertRow(1); // after header

    var symbolCell = row.insertCell(0);
    var timestampCell = row.insertCell(1);
    var averagePriceCell = row.insertCell(2);
    
    averagePriceCell.innerHTML = message.averagePrice;
    timestampCell.innerHTML = new Date(message.windowEnd).toTimeString();
    symbolCell.innerHTML = "BTC-AUD";
};//onMessage