package com.guavaapps.spotlight

import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.File

private const val TAG = "DLSTMP"

class DLSTMPModel(model: File) {
    private val LOOK_BACK = 1

    private var interpreter: Interpreter

    init {
        interpreter = Interpreter(model)
    }

    fun getNext(timeline: Array<FloatArray>, scaleInput: Boolean = true): FloatArray {
        interpreter.allocateTensors()

//        checkInputShape(interpreter, timeline)

        val DEBUG_TIMESTAMP = System.currentTimeMillis()

        Log.d(
            TAG,
            "running inference - timestamp=$DEBUG_TIMESTAMP timeline=[${timeline.size} ${timeline.first().size}] inputShape=[${
                interpreter.getInputTensor(0).shape().joinToString(" ")
            }]"
        )

        val output = arrayOf(
            FloatArray(timeline.first().size)
        )

        val timelineScaler = scaleMinMax(
            timeline
                .takeLast(LOOK_BACK)
                .reversed()
                .toTypedArray()
        )
        val scaledTimeline = timelineScaler.result

        interpreter.run(arrayOf(scaledTimeline), output)

        interpreter.close()

        return invertMinMax(timelineScaler.apply {
            result = output
        })[0].also {
            Log.d(
                TAG,
                "result for timeline - size=${timeline.size} features=${timeline.first().size} result=[${
                    it.joinToString(" ")
                }]"
            )
        }
    }

    private fun checkInputShape(interpreter: Interpreter, timeline: Array<FloatArray>) {
        val inputShape =
            interpreter.getInputTensor(0).shape().slice(1..1) // batch size (at index 0) is always 1

        if (inputShape[0] != timeline.size || inputShape[1] != timeline.first().size) throw IllegalArgumentException(
            "Timeline doesn't match required input shape - inputShape=[$inputShape] timeline=[${timeline.first().size}]"
        )
    }

    private fun scaleMinMax(data: Array<FloatArray>): Scaler {
        var min = Float.MIN_VALUE
        var max = Float.MAX_VALUE

        data.forEach { x ->
            val mn = x.min()
            val mx = x.max()

            if (mn < min) min = mn
            if (mx < max) max = mx
        }

        var scaled = Array(data.size) { i ->
            val d = max - min

            FloatArray(data[i].size) { j ->
                (data[i][j] - min) / d
            }
        }


        return Scaler(
            scaled,
            min,
            max
        )
    }

    private fun invertMinMax(data: Scaler): Array<FloatArray> {
        val delta = data.max - data.min

        var inverted = Array(data.result.size) { i ->

            FloatArray(data.result[i].size) { j ->
                data.result[i][j] * delta + data.min
            }
        }

        return inverted
    }

    private data class Scaler(
        var result: Array<FloatArray>,
        var min: Float,
        var max: Float,
    )
}