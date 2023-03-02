package com.guavaapps.spotlight

import org.tensorflow.lite.Interpreter
import java.io.File

private const val TAG = "DLSTMP"

// wrapper for a tf lite interpreter
// used to recommend the next set of track features
class DlstmpModel(model: File) {
    // number of timestamps/batch
    private val LOOK_BACK = 1

    // tensorflow lite interpreter
    private var interpreter: Interpreter

    // input shape of the model in the form [batches, timesteps, features]
    val inputShape: IntArray
        get() = interpreter.getInputTensor(0).shape()

    val outputShape: IntArray
        get() = interpreter.getOutputTensor(0).shape()

    init {
        // create an interpreter from a tf lite model file
        interpreter = Interpreter(model)
    }

    // get the next track features
    fun getNext(timeline: Array<FloatArray>): FloatArray {
        interpreter.allocateTensors()

        // create an empty array to hold the interpreter output
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

        // invert back to standard ranges
        return FeatureScaler.invert(output).first()
    }
}

// min-max scaler
object FeatureScaler {
    // different features have different possible ranges
    private val SHORT_RANGE = 0f..1f
    private val WIDE_RANGE = Float.MIN_VALUE..Float.MAX_VALUE

    private val ranges = arrayOf(
        SHORT_RANGE, // acousticness
        SHORT_RANGE, // danceability
        SHORT_RANGE, // energy
        SHORT_RANGE, // instrumentallness
        SHORT_RANGE, // liveness
        WIDE_RANGE, // loudness
        SHORT_RANGE, // speechiness
        WIDE_RANGE, // tempo
        SHORT_RANGE // valence
    )

    // apply min-max scaling function
    fun scale(features: Array<FloatArray>) = features.map {
        it.mapIndexed { i, it ->
            val min = ranges[i].start
            val max = ranges[i].endInclusive
            val delta = max - min

            (it - min) / delta
        }.toFloatArray()
    }.toTypedArray()

    // invert scaling function
    fun invert(features: Array<FloatArray>) = features.map {
        it.mapIndexed { i, it ->
            val min = ranges[i].start
            val max = ranges[i].endInclusive
            val delta = max - min

            it * delta + min
        }.toFloatArray()
    }.toTypedArray()
}