package com.guavaapps.spotlight

import android.util.Log
import org.tensorflow.lite.Interpreter
import retrofit.http.Body
import retrofit.http.POST
import java.io.File

const val TAG = "Service"

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
    private val MODEL = "model"
    private val TF_LITE = ".tflite"

    private val LOOK_BACK = 1

    private var interpreter: Interpreter

    init {
        interpreter = Interpreter(model)
    }

    fun getNextTest() {
        // create a 4-timestep, 2-feature timeline and run on test model

        val testTimeline = Array(4) { FloatArray(2) }

        return getNext(testTimeline)
    }

    fun getNext(timeline: Array<FloatArray>) {
        interpreter.allocateTensors()

        val output = arrayOf(
            FloatArray(2)
        )

        val inputShape = interpreter.getInputTensor(0).shape()
        Log.e(TAG, "inputShape - (${inputShape.joinToString(", ")})")

        interpreter.run(arrayOf(timeline), output)

        val o = output[0][0]
        Log.e(TAG, "output - $o")

        interpreter.close()
    }

    private fun createBatches(timeline: FloatArray): Array<FloatArray> {
        val scaled = scaleMinMax(timeline)
        var batches = mutableListOf<FloatArray>()

        for (i in 0..timeline.size - LOOK_BACK - 1) {
            val a = timeline.slice(i..i + LOOK_BACK - 1).toFloatArray()
            batches.add(a)
        }

        return batches.toTypedArray()
    }

    private fun load(): Interpreter {
        val model: File = File.createTempFile(MODEL, TF_LITE)

        // TODO write output stream to model file

        return Interpreter(model)
    }

    private fun loadFromFile(model: File): Interpreter {
        return Interpreter(model)
    }

    private fun scaleMinMax(data: FloatArray): FloatArray {
        var scaled = FloatArray(data.size)
        val min = data.min()
        val max = data.max()
        val d = max - min

        data.forEachIndexed { i, x -> scaled[i] = (x - min) / d }

        return scaled
    }

    private fun invertMinMax() {

    }
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