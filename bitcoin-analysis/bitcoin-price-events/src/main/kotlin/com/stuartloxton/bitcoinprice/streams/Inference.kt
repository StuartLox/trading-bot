package com.stuartloxton.bitcoinprice.streams

import org.deeplearning4j.nn.modelimport.keras.KerasModelImport
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.factory.Nd4j
import org.nd4j.linalg.io.ClassPathResource


class Inference {
    private val simpleMlp = ClassPathResource(
        "games.h5").getFile().getPath()
    private val model = KerasModelImport
        .importKerasSequentialModelAndWeights(simpleMlp)

    fun buildTensor(): INDArray {
        val inputs = 10
        val rank = 1
        return Nd4j.zeros(rank, inputs)
    }

    fun getPrediction(): Double {
        val features = buildTensor()
        for (j in 0..9) {
            var x = 1.0
            if (Math.random() < 0.5)
                x = 0.0
            features.putScalar(intArrayOf(0, j), x)
        }
        val prediction = model.output(features).getDouble(0)
        return prediction
    }
}
//
//fun main(args: Array<String>) {
//    val inf = Inference()
//    val prediction = inf.getPrediction()
//    println(prediction)
//}