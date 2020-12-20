# Bitcoin Price Prediction

## Summary 

For many online services much of the value comes from taking an action in response to a business event or user behaviour. Unfortunatley, most machine learning systems do not operate in the same way, as they have to rely on after the fact batch processes, due to the immense challenge of reliably cleaning, transforming and merging multiple sources of data to buld a single feature vector to perform model inference. For this reason, it is much simplier to perform all this tasks as an offline batch process. 




<details>
<summary>Metric Interface</summary>
<br>
```kotlin
interface Metric<T> {
    fun identity(): T
    fun aggregator(newStock: Stock, current: Any): T
}
```
</details>

# This

For example, high-frequency crypto-currency, forex, and stock market price. Any "information arbitrage" - i.e leveraging short-term pricing information to predict market movements


Alternativly, a realtime approach can be achieved with realtime events event-sourcing and using streaming platforms


# Architecture

* **K8s** - Container Orchestraion
* **Kafka** - Event Store
* **Kafka Streams** - Stream processing
* **S3** - File System, Landing Zone for streaming data and store for model artefacts.
* **Sagemaker** - Hosted Notebooks for model training, evaluation and deployment of new models.
* **React** - Front-end framework

![image](assets/BitcoinStreamsTopology.png)

## Data Sources

Kotlin service listens to a websocket from `binance.com` and streams realtime pricing data to the `adapter.bicoin-price-events` topic on Kafka. These event.
## Data Transformation


## Model

* Use an LTSM to perform multivariate time series prediction on a . 

![BitcoinPrice](assets/BitcoinPrice.gif)
