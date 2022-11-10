package com.guavaapps.spotlight

import android.util.Log
import kotlin.math.max
import kotlin.math.tanh

abstract class Node(
    val inputSize: Int = 0,
    val activationFunction: ActivationFunction = identity
) {
    var a = 0f
    var gradient = 0f

    abstract fun feed(inputs: FloatArray): Any
}

open class ActivatedNode(
    inputSize: Int = 0,
    activationFunction: ActivationFunction = identity
) : Node(
    inputSize,
    activationFunction
) {
    private val weights = FloatArray(inputSize) {
        0.33f
    }

    override fun feed(inputs: FloatArray): Float {
        val sigma = inputs.mapIndexed { i, input -> input * weights[i] }.sum()

        return activationFunction.activation(sigma).also { a = it }
    }
}

//open class LSTMSHNode : Node(
//    1,
//)

open class LSTMNode2 : Node() {
    private val TAG = "Cell"
    var c = 0f

    var prevC = 0f
    var prevH = 0f

    private var wi = floatArrayOf(0.33f, 0.33f)
    private var wf = floatArrayOf(0.33f, 0.33f)
    private var wo = floatArrayOf(0.33f, 0.33f)
    private var wc = floatArrayOf(0.33f, 0.33f)

    private var sigmaF = 0f
    private var f = 0f

    private var sigmaC = 0f
    private var C = 0f

    private var sigmaI = 0f
    private var i = 0f

    private var sigmaO = 0f
    private var o = 0f

//    fun create(count: Int) {
//        wi = arrayOf(
//            FloatArray(count) { 0.33f }, floatArrayOf(0.33f)
//        )
//
//        wf = arrayOf(
//            FloatArray(count) { 0.33f }, floatArrayOf(0.33f)
//        )
//
//        wc = arrayOf(
//            FloatArray(count) { 0.33f }, floatArrayOf(0.33f)
//        )
//
//        wo = arrayOf(
//            FloatArray(count) { 0.33f }, floatArrayOf(0.33f)
//        )
//    }

//    override fun feed(input: Float): Float = throw NotImplementedError("Use feed (FloatArray)")

    override fun feed(input: FloatArray): Float {
        val y = input[0]
        sigmaF = y * wf[0] + a * wf[1]
//        sigmaF = y * wf[0] + a * wf[1]
        f = sigmoid.activation(sigmaF)

        sigmaC = y * wc[0] + a * wc[1]
//        sigmaC = y * wc[0] + a * wc[1]
        C = tanh(sigmaC)

        sigmaI = y * wi[0] + a * wi[1]
//        sigmaI = y.mapIndexed { i, n -> n * wi[0][i] }.sum() + a * wi[1][0]
        i = sigmoid.activation(sigmaO)

        sigmaO = y * wo[0] + a * wo[1]
//        sigmaO = y.mapIndexed { i, n -> n * wo[0][i] }.sum() + a * wo[1][0]
        o = sigmoid.activation(sigmaO)

        prevC = c
        prevH = a

        c = f * c + i * C
        a = o * tanh(c)

        Log.e(TAG, "state=$c hidden=$a")

        return a
    }

    // C -> tanh Zg
    // wc =
    //
    // i -> sigmoid Zj

    fun computeWeights(y: Float) {
        // raw val dump

        // TODO cleanup this code these variable name are stoopid
        // TODO Make an index
        // TODO implement in LSTMLayer class
        // goddamn fuck you <3

        val dEdo = gradient * tanh(c)
        val dEdc = gradient * a * tanh.derivative(c) * y

        val dEdi = gradient * a * tanh.derivative(c) * C
        val dEdg = gradient * a * tanh.derivative(c) * i

        val dEdf = gradient * a * tanh.derivative(c) * prevC

        val dEdprevC = gradient * o * tanh.derivative(c) * f

        val dEdwo0 =
            gradient * tanh(c) * sigmoid.activation(sigmaO) * (1 - sigmoid.derivative(sigmaO)) * y
        val dEdwo1 =
            gradient * tanh(c) * sigmoid.activation(sigmaO) * (1 - sigmoid.derivative(sigmaO)) * prevH
// i fucking love this man beyond anything ive ever felt before. he is my everything and i dont wanna ever loose him. Sometimes i think it would
        // be too much asRealmObject call hime my love and my life but hell sometimes it feels like i wanna make him my whole world
        // goddamn i fucking love this man
        val dEdwf0 =
            gradient * tanh(c) * sigmoid.activation(sigmaF) * (1 - sigmoid.derivative(sigmaF)) * y
        val dEdwf1 =
            gradient * tanh(c) * sigmoid.activation(sigmaF) * (1 - sigmoid.derivative(sigmaF)) * prevH

        val dEdwi0 =
            gradient * tanh(c) * C * sigmoid.activation(sigmaI) * (1 - sigmoid.derivative(
                sigmaI
            )) * y
        val dEdwi1 =
            gradient * tanh(c) * C * sigmoid.activation(sigmaI) * (1 - sigmoid.derivative(
                sigmaI
            )) * prevH

        val dEdwc0 = gradient * a * tanh.derivative(c) * i * tanh.derivative(sigmaC) * y
        val dEdwc1 = gradient * a * tanh.derivative(c) * i * tanh.derivative(sigmaC) * prevH

        // w
        // deez nuts
        val RATE = 0.01f

        Log.e(TAG, "gradients for input$i")

        Log.e(TAG, "    dE/dwo - y=$dEdwo0 a=$dEdwo1")
        Log.e(TAG, "    dE/dwi - y=$dEdwi0 a=$dEdwi1")
        Log.e(TAG, "    dE/dwc - y=$dEdwc0 a=$dEdwc1")
        Log.e(TAG, "    dE/dwf - y=$dEdwf0 a=$dEdwf1")

        wi[0] -= RATE * dEdwi0
        wi[1] -= RATE * dEdwi1

        wo[0] -= RATE * dEdwo0
        wo[1] -= RATE * dEdwo1

        wf[0] -= RATE * dEdwf0
        wf[1] -= RATE * dEdwf1

        wc[0] -= RATE * dEdwc0
        wc[1] -= RATE * dEdwc1
    }
}

open class LSTMNode : Node() {
    private val TAG = "Cell"

    private val nodeStates = mutableListOf<NodeState>()

    var c = 0f

    var prevC = 0f
    var prevH = 0f

    private var wi = floatArrayOf(0.33f, 0.33f)//(0f, 0f)
    private var wf = floatArrayOf(0.33f, 0.33f)//(0f, 0f)
    private var wo = floatArrayOf(0.33f, 0.33f)//(0f, 0f)
    private var wc = floatArrayOf(0.33f, 0.33f)//(0f, 0f)

    private var sigmaF = 0f
    private var f = 0f

    private var sigmaC = 0f
    private var C = 0f

    private var sigmaI = 0f
    private var i = 0f

    private var sigmaO = 0f
    private var o = 0f

    override fun feed(input: FloatArray): Float {
        val y = input[0]

        sigmaF = y * wf[0] + a * wf[1]
        f = sigmoid.activation(sigmaF)

        sigmaC = y * wc[0] + a * wc[1]
        C = tanh(sigmaC)

        sigmaI = y * wi[0] + a * wi[1]
        i = sigmoid.activation(sigmaO)

        sigmaO = y * wo[0] + a * wo[1]
        o = sigmoid.activation(sigmaO)

        prevC = c
        prevH = a

        c = f * c + i * C
        a = o * tanh(c)

        Log.e(TAG, "state=$c hidden=$a")

        nodeStates.add(
            NodeState (y, a, c)
        )

        return a
    }

    // C -> tanh Zg
    // wc =
    //
    // i -> sigmoid Zj

    fun computeWeights(y: Float) {
        // raw val dump

        // TODO cleanup this code these variable name are stoopid
        // TODO Make an index
        // TODO implement in LSTMLayer class
        // goddamn fuck you <3
        val dEdo = gradient * tanh(c)
        val dEdc = gradient * a * tanh.derivative(c) * y

        val dEdi = gradient * a * tanh.derivative(c) * C
        val dEdg = gradient * a * tanh.derivative(c) * i

        val dEdf = gradient * a * tanh.derivative(c) * prevC

        val dEdprevC = gradient * o * tanh.derivative(c) * f

        val dEdwo0 =
            gradient * tanh(c) * sigmoid.activation(sigmaO) * (1 - sigmoid.derivative(sigmaO)) * y
        val dEdwo1 =
            gradient * tanh(c) * sigmoid.activation(sigmaO) * (1 - sigmoid.derivative(sigmaO)) * prevH
// i fucking love this man beyond anything ive ever felt before. he is my everything and i dont wanna ever loose him. Sometimes i think it would
        // be too much asRealmObject call hime my love and my life but hell sometimes it feels like i wanna make him my whole world
        // goddamn i fucking love this man
        val dEdwf0 =
            gradient * tanh(c) * sigmoid.activation(sigmaF) * (1 - sigmoid.derivative(sigmaF)) * y
        val dEdwf1 =
            gradient * tanh(c) * sigmoid.activation(sigmaF) * (1 - sigmoid.derivative(sigmaF)) * prevH

        val dEdwi0 =
            gradient * tanh(c) * C * sigmoid.activation(sigmaI) * (1 - sigmoid.derivative(sigmaI)) * y
        val dEdwi1 =
            gradient * tanh(c) * C * sigmoid.activation(sigmaI) * (1 - sigmoid.derivative(sigmaI)) * prevH

        val dEdwc0 = gradient * a * tanh.derivative(c) * i * tanh.derivative(sigmaC) * y
        val dEdwc1 = gradient * a * tanh.derivative(c) * i * tanh.derivative(sigmaC) * prevH

        // w
        // deez nuts
        val RATE = 0.01f

        Log.e(TAG, "dE/dwo - y=$dEdwo0 a=$dEdwo1")
        Log.e(TAG, "dE/dwi - y=$dEdwi0 a=$dEdwi1")
        Log.e(TAG, "dE/dwc - y=$dEdwc0 a=$dEdwc1")
        Log.e(TAG, "dE/dwf - y=$dEdwf0 a=$dEdwf1")

        wi[0] -= RATE * dEdwi0
        wi[1] -= RATE * dEdwi1

        wo[0] -= RATE * dEdwo0
        wo[1] -= RATE * dEdwo1

        wf[0] -= RATE * dEdwf0
        wf[1] -= RATE * dEdwf1

        wc[0] -= RATE * dEdwc0
        wc[1] -= RATE * dEdwc1
    }

    private data class NodeState(
        var input: Float,
        var output: Float,
        var c: Float
    )
}

open class DenseNode(val activationFunction: ActivationFunction) {
    var sigma = 0f
    open var a = 0f
    var d = 0f

    open fun feedThrough(input: Float): Float =
        activationFunction.activation(input).also { a = it }
}

fun reLu(input: Float): Float {
    return max(0f, input)
}

fun dReLu(input: Float) = if (input > 0) 1f else 0f

fun noActivation(input: Float) = input

val reLu = object : ActivationFunction {
    override fun activation(input: Float): Float {
        return max(0f, input)
    }

    override fun derivative(input: Float): Float {
        return if (input > 0) 1f else 0f
    }

}

val identity = object : ActivationFunction {
    override fun activation(input: Float): Float = input

    override fun derivative(input: Float): Float = 1f
}

val sigmoid = object : ActivationFunction {
    override fun activation(input: Float): Float {
        return 1 / (1 + Math.exp((-input).toDouble())).toFloat()
    }

    override fun derivative(input: Float): Float = input * (1 - input)
}

val tanh = object : ActivationFunction {
    override fun activation(input: Float): Float = tanh(input)

    override fun derivative(input: Float): Float = 1 - tanh(input) * tanh(input)
}

interface ActivationFunction {
    fun activation(input: Float): Float

    fun derivative(input: Float): Float
}