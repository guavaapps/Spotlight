package com.guavaapps.spotlight

import org.tensorflow.lite.Interpreter
import java.io.File

private const val TAG = "DLSTMP"

class DLSTMPModel(model: File) {
    private val LOOK_BACK = 1

    // tensorflow lite model returned by the model optimiser
    private var interpreter: Interpreter

    // input shape of the model in the form [batches, timesteps, features]
    val inputShape: IntArray
        get() = interpreter.getInputTensor(0).shape()

    val outputShape: IntArray
        get() = interpreter.getOutputTensor(0).shape()

    init {
        interpreter = Interpreter(model)
    }

    // get the next
    fun getNext(timeline: Array<FloatArray>): FloatArray {
        interpreter.allocateTensors()

        val output = arrayOf(
            FloatArray(timeline.first().size)
        )

        // scale the timeline
        val scaledTimeline = FeatureScaler.scale(
            timeline
                .takeLast(LOOK_BACK)
                .reversed()
                .toTypedArray()
        )

        interpreter.run(arrayOf(scaledTimeline), output)

        return FeatureScaler.invert(output).first()
    }
}


object FeatureScaler {
    private val STANDARD_RANGE = 0f..1f
    private val FULL_RANGE = Float.MIN_VALUE..Float.MAX_VALUE

    private val ranges = arrayOf(
        STANDARD_RANGE, // acousticness
        STANDARD_RANGE, // danceability
        STANDARD_RANGE, // energy
        STANDARD_RANGE, // instrumentallness
        STANDARD_RANGE, // liveness
        FULL_RANGE, // loudness
        STANDARD_RANGE, // speechiness
        FULL_RANGE, // tempo
        STANDARD_RANGE // valence
    )

    fun scale(features: Array<FloatArray>) = features.map {
        it.mapIndexed { i, it ->
            val min = ranges[i].start
            val max = ranges[i].endInclusive
            val delta = max - min

            (it - min) / delta
        }.toFloatArray()
    }.toTypedArray()

    fun invert(features: Array<FloatArray>) = features.map {
        it.mapIndexed { i, it ->
            val min = ranges[i].start
            val max = ranges[i].endInclusive
            val delta = max - min

            it * delta + min
        }.toFloatArray()
    }.toTypedArray()
}