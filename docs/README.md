# Trading Bot

### Overview

Using Kafka Streams to build a realtime inference engine for stock trading. Each bitcoin price is the 1 second OHLC price for a one second interval. The  

---

## Architecture

TODO

## Kafka Streams
___
### Generate Average Crypto Close Price

Generates the average price for a cypto currency from a raw crypto price event. 
The key will be a custom serde of the `symbol` and the `windowEnd`.
* WindowEnd - Is calculated from `timestamp` and denotes the daily window the raw event took place. 

![image](assets/BitcoinStreamsTopology.png)
--

Hi Van, would you like to see this as a time series or as a life to date figure? Also would you like to know transaction volume or total amount? 