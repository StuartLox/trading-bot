package com.stuartloxton.bitcoinprice.streams

import org.deeplearning4j.nn.modelimport.keras.KerasModelImport
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.factory.Nd4j
import org.nd4j.linalg.io.ClassPathResource
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class Inference {
    private val mPath = "model.h5"
    private val lstm = ClassPathResource(mPath).getFile().getPath()
    private val model = KerasModelImport.importKerasSequentialModelAndWeights(lstm, false)
    private val logger = LoggerFactory.getLogger(javaClass)

    private fun buildTensor(): INDArray {
        val timeSteps = 50
        val rows = 1
        val columns = 2
        return  Nd4j.zeros(rows, columns, timeSteps)
    }

    fun getPrediction(data: List<List<Double>>): Double {
        val features = buildTensor()
        if (data.size == 50) {
            data.forEachIndexed { index, it ->
                logger.info("AVG Price - ${it.get(0)}")
                features.putScalar(intArrayOf(0, 0, index), it.get(0))
                features.putScalar(intArrayOf(0, 1, index), it.get(1))
            }
            logger.info("Size - ${data.size} Performing prediction")
            return model.output(features).getDouble(0)
        }
        else {
            logger.info("Size - ${data.size} Not enough data to perform prediction")
            return -1.0
         }
    }
}