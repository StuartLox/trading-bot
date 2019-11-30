// assume that API service is published on same server that is the server of this HTML file
var source = new EventSource("../updates");
source.onmessage = function (data) {
    console.log(data)
    console.log("Hello")
    var table = document.getElementById("stockTable");
    var row = table.insertRow(1); // after header

    // var symbolCell = row.insertCell(0);
    // var timestampCell = row.insertCell(1);
    // var openCell = row.insertCell(2);
    // var highCell = row.insertCell(3);
    // var lowCell = row.insertCell(4);
    // var closeCell = row.insertCell(5);
    // var volumeCell = row.insertCell(6);


    // volumeCell.innerHTML = data.volume;
    // timestampCell.innerHTML = data.timestamp;
    // openCell.innerHTML = data.open;
    // highCell.innerHTML = data.high;
    // lowCell.innerHTML = data.low;
    // closeCell.innerHTML = data.close;
    // symbolCell.innerHTML = data.symbol;
};//onMessage