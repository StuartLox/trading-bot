# Trading Bot

### Overview

Experimenting with trading bot that periodically gets bitcoin data from yahoo finance and writes to Bitcoin Kafka Topic.

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