package com.stuartloxton.cryptoservice.streams


import com.stuartloxton.cryptoservice.ATREvent
import com.stuartloxton.cryptoservice.AveragePriceEvent
import com.stuartloxton.cryptoservice.CryptoMetricEvent
import com.stuartloxton.cryptoservice.CryptoMetricEventWindow
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Service


@Service
class Consumer {
   private val logger = LoggerFactory.getLogger(javaClass)
   var inputVector = mutableListOf<List<Double>>()

   @Autowired
   private lateinit var inference: Inference

   @KafkaListener(
       topics = ["\${application.kafka.btc-metrics-topic}"],
       containerFactory = "kafkaListenerContainerFactory"
   )
   fun consume(
       averagePrices: List<ConsumerRecord<CryptoMetricEventWindow, CryptoMetricEvent>>,
       ack: Acknowledgment
   ) {
       var commitOffsets = false

       try {
            averagePrices.forEach{
               if (it.key() != null) {
                   val key = it.key()
                   val value = it.value()
                   commitOffsets = processEvent(key, value)
               }
           }
       } catch (e: Exception) {
           commitOffsets = false
       } finally {
           if (commitOffsets) {
               ack.acknowledge()
           }
       }
   }

   fun setFeatures(value: CryptoMetricEvent): List<Double> {
       // Extracts features
       val avg = value.getAvgPrice() as AveragePriceEvent
       val atr = value.getAtr() as ATREvent
       return listOf(avg.getAveragePrice(), atr.getAverageTrueRange())
   }

   fun processEvent(key: CryptoMetricEventWindow, value: CryptoMetricEvent): Boolean {
       val features = setFeatures(value)
       inputVector.add(features)

       val prediction = inference.getPrediction(inputVector)
       val datetime = DateTime(key.getWindowEnd()).toLocalDateTime()
       logger.info("Inference: $prediction, Items: ${inputVector.size}, windowEnd: $datetime")

       var commitOffsets = false
       if (prediction != -1.0) {
           inputVector.removeAt(0)
           logger.info("Removing first elem of in memory db: ${inputVector.size}")
           commitOffsets = true
       }
       return commitOffsets
   }
}