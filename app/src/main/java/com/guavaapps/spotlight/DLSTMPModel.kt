package com.guavaapps.spotlight

import android.util.Log
import androidx.annotation.FloatRange
import org.tensorflow.lite.Interpreter
import java.io.File

private const val TAG = "DLSTMP"

class DLSTMPModel(model: File) {
    private val LOOK_BACK = 1

    private var interpreter: Interpreter
    val inputShape: IntArray
        get() = interpreter.getInputTensor(0).shape()

    val outputShape: IntArray
        get() = interpreter.getOutputTensor(0).shape()

    init {
        interpreter = Interpreter(model)
    }

    fun getNext(timeline: Array<FloatArray>): FloatArray {
        interpreter.allocateTensors()

        val modelInputShape = interpreter.getInputTensor(0).shape().joinToString()
        val inputShape = intArrayOf(timeline.size, timeline.first().size).joinToString()

        Log.e(TAG, "input shape check - modelInputShape[$modelInputShape] inputShape=[$inputShape]")

        val output = arrayOf(
            FloatArray(timeline.first().size)
        )

        val scaledTimeline = FeatureScaler.scale(
            timeline
                .takeLast(LOOK_BACK)
                .reversed()
                .toTypedArray()
        )

        interpreter.run(arrayOf(scaledTimeline), output)

        return FeatureScaler.invert(output).first()
    }

    private fun checkInputShape(interpreter: Interpreter, timeline: Array<FloatArray>) {
        val inputShape =
            interpreter.getInputTensor(0).shape().slice(1..1) // batch size (at index 0) is always 1

        if (inputShape[0] != timeline.size || inputShape[1] != timeline.first().size) throw IllegalArgumentException(
            "Timeline doesn't match required input shape - inputShape=[$inputShape] timeline=[${timeline.first().size}]"
        )
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