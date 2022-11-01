package com.guavaapps.spotlight

import android.util.Log
import org.tensorflow.lite.Interpreter
import retrofit.http.Body
import retrofit.http.POST
import java.io.File
import kotlin.math.sign

private const val TAG = "Service"

interface LambdaService {
    @POST("/")
    fun getModel(@Body data: String): Any
}

class LambdaModel {
    var model: String? = null
    var weights: FloatArray? = null
    var states: FloatArray? = null
}

class DLSTMPModel(model: File) {
    private val LOOK_BACK = 1

    private var interpreter: Interpreter

    init {
        interpreter = Interpreter(model)
    }

    fun getNextTest(): FloatArray {
        // create a 4-timestep, 2-feature timeline and run on test model

        val testTimeline = Array(4) { FloatArray(2) }

        return getNext(testTimeline)
    }

    fun testOn5 (): FloatArray {
        val testTimeline = arrayOf(
            floatArrayOf(0.15f, 0.7f),
            floatArrayOf(0.15f, 0.7f),
            floatArrayOf(0.05f, 0.6f),
            floatArrayOf(0.05f, 0.5f),
            floatArrayOf(0.05f, 0.45f),
        )

        return getNext(testTimeline)
    }

    fun getNext(timeline: Array<FloatArray>): FloatArray {
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

        val timelineScaler = scaleMinMax(timeline)
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
        val inputShape = interpreter.getInputTensor(0).shape().slice(1..1) // batch size (at index 0) is always 1

        if (inputShape [0] != timeline.size || inputShape [1] != timeline.first().size) throw IllegalArgumentException("Timeline doesn't match required input shape - inputShape=[$inputShape] timeline=[${timeline.first().size}]")
    }

    private fun createBatches(timeline: FloatArray): Array<FloatArray> {
//        val scaled = scaleMinMax(timeline)
        var batches = mutableListOf<FloatArray>()

        for (i in 0..timeline.size - LOOK_BACK - 1) {
            val a = timeline.slice(i..i + LOOK_BACK - 1).toFloatArray()
            batches.add(a)
        }

        return batches.toTypedArray()
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
        var max: Float
    )
}

// POST
// values: [[Float]]

// values
// [[f1, f2, f3, ... , f4, f5, f6], [ ... ], [ ... ]]

// aaaaaaaaaaaaaa
// either save tflite :lambda
// get tflite :app
// rebuild model (stateless)
// get history :mongodb
// feed history :app
// get next
// post error :lambda
// train
// get states :app
// post states :mongodb

// or stateful model
// get states
// post states
// get next
// error?
// post error
// train
// get states
// post states

// aaaaaaa idk what to do
// statesless lstm

class LambdaStructure {

}