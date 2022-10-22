package com.guavaapps.spotlight

import android.util.Log
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.reflect.KClass

// TODO mongoDB implementation
// TODO use Realm to access backend database
// TODO create user data class
// TODO create user Realm object
// TODO aws s3 implementation
// TODO store user models in s3

// TODO nodes with adjacent weights and defined sigma rules
// TODO redesign Network class for new node architecture
// TODO DO THE FUCKING DATABASE THING ALREADY

open class Network {
    private val LEARNING_RATE = 0.01f

    private val layers = mutableListOf<Array<out Node>>()//Array<Array<DenseNode>>
    private val weights = mutableListOf<Array<FloatArray>>()

    fun addDense(size: Int, activationFunction: ActivationFunction = identity) {
//        layers.add(
            //Array(size) { ActivatedNode(activationFunction) }
//        )

        weights.add(
            createWeightSpace(layers.last().size, size)
        )
    }

    fun addLSTM(size: Int) {
        weights.add(
            createWeightSpace(layers.last().size, size)
        )

        layers.add(
            Array(size) { LSTMNode() }
        )
    }

    fun feed(vararg inputs: Float): Array<Float> {
        for (i in inputs.indices) {
            val node = layers.first()[i]
            //node.feed(inputs[i])
        }

        for (i in 1..layers.lastIndex) {
            val l = i - 1;

            for ((j, node) in layers[i].filterNot { node -> layers[i] != layers.last() && node == layers[i].last() }
                .withIndex()) {
                var weightedSum = 0f

                for ((k, pNode) in layers[i - 1].withIndex()) {
                    weightedSum += pNode.a * weights[l][j][k]
                }

                //node.feed(weightedSum)
            }
        }

        return layers.last().map { node -> node.a }.toTypedArray()
            .also { Log.e("output", "a - ${it.joinToString(", ")}") }
    }

    fun computeGradients(vararg y: Float) {
        computeGradientForLastLayer(y)
        computeGradientForHiddenLayers()

    }

    private fun computeGradientForLastLayer(y: FloatArray) {
        val lastLayer = layers.last()
        val prevLayer = layers[lastLayer.lastIndex - 1]

        // A - dC/da // Etotal/out o1
        // Y - da/ds // dout o1 / dnet o1
        // W - ds/dw // dnet o1 / dw5

        // delta = AY

        for ((i, node) in lastLayer.withIndex()) {
            for ((j, prevNode) in prevLayer.withIndex()) {
                val A = -2 * (y[i] - node.a)
                val Y = (node as ActivatedNode).activationFunction.derivative(node.a)
                val W = prevNode.a

                node.gradient = A * Y

                val gradient = A * Y * W

                if (node !is LSTMNode) weights.last()[i][j] -= LEARNING_RATE * gradient
            }
        }
    }

    private fun computeGradientForHiddenLayers() {
        for ((l, layer) in layers.withIndex().reversed()) {
            if (l == 0 || l == layers.lastIndex) {
                continue
            }

            val prevLayer = layers[l - 1]

            // A - dC/da // Etotal/out o1
            // Y - da/ds // dout o1 / dnet o1
            // W - ds/dw // dnet o1 / dw5
            // Aj - Eo1/out h1
            // Ak - .../out h2
            //

            for ((i, node) in layer.filterNot { it == layer.last() }.withIndex()) {

                var delta = 0f

                // get delta sum (first part) // error gradient with respect to this a
                for ((p, nextLayer) in layers.filter { layers.indexOf(it) > l && it != layers.last() }
                    .withIndex()) {
                    for ((q, nextNode) in nextLayer.filterNot { it == nextLayer.last() }
                        .withIndex()) {
                        val cWeights = weights[p + l /* + 1 */][q]

                        for (w in cWeights) {
                            delta += nextNode.gradient * w
                        }
                    }
                }

                delta *= node.activationFunction.derivative(node.a)

                node.gradient = delta

                for ((j, prevNode) in prevLayer.withIndex()) {
                    val W = prevNode.a

                    val gradient = delta * W

                    weights[l - 1][i][j] -= LEARNING_RATE * gradient
                }
            }
        }
    }

    private fun createWeightSpace(from: Int, to: Int): Array<FloatArray> {
        val weights = Array(to) {
            FloatArray(from) {
                0.33f
            }
        }

        return weights
    }

}

open class NetworkDense /*private*/ constructor(
    private val layers: Array<Array<DenseNode>>,
    private val weights: Array<Array<Array<Float>>>
) {
    private val LEARNING_RATE = 0.01f

    fun feed(vararg inputs: Float): Array<DenseNode> {
        for (i in inputs.indices) {
            val node = layers.first()[i]
            node.feedThrough(inputs[i])
        }

        for (i in 1..layers.lastIndex) {
            val l = i - 1;

            for ((j, node) in layers[i].filterNot { node -> layers[i] != layers.last() && node == layers[i].last() }
                .withIndex()) {
                var weightedSum = 0f

                for ((k, pNode) in layers[i - 1].withIndex()) {
                    weightedSum += pNode.a * weights[l][j][k]
                }

                node.feedThrough(weightedSum)
            }
        }

        return layers.last()
    }

    fun computeGradients(vararg y: Float) {
        computeGradientForLastLayer(y)
        computeGradientForHiddenLayers()
    }

    private fun computeGradientForLastLayer(y: FloatArray) {
        val lastLayer = layers.last()
        val prevLayer = layers[lastLayer.lastIndex - 1]

        // A - dC/da // Etotal/out o1
        // Y - da/ds // dout o1 / dnet o1
        // W - ds/dw // dnet o1 / dw5

        // delta = AY

        for ((i, node) in lastLayer.withIndex()) {
            for ((j, prevNode) in prevLayer.withIndex()) {
                val A = -2 * (y[i] - node.a)
                val Y = node.activationFunction.derivative(node.a)
                val W = prevNode.a

                node.d = A * Y

                val gradient = A * Y * W

                weights.last()[i][j] -= LEARNING_RATE * gradient
            }
        }
    }

    private fun computeGradientForHiddenLayers() {
        for ((l, layer) in layers.withIndex().reversed()) {
            if (l == 0 || l == layers.lastIndex) {
                continue
            }

            val prevLayer = layers[l - 1]

            // A - dC/da // Etotal/out o1
            // Y - da/ds // dout o1 / dnet o1
            // W - ds/dw // dnet o1 / dw5
            // Aj - Eo1/out h1
            // Ak - .../out h2
            //

            for ((i, node) in layer.filterNot { it == layer.last() }.withIndex()) {

                var delta = 0f

                // get delta sum (first part) // error gradient with respect to this a
                for ((p, nextLayer) in layers.filter { layers.indexOf(it) > l && it != layers.last() }
                    .withIndex()) {
                    for ((q, nextNode) in nextLayer.filterNot { it == nextLayer.last() }
                        .withIndex()) {
                        val cWeights = weights[p + l /* + 1 */][q]

                        for (w in cWeights) {
                            delta += nextNode.d * w
                        }
                    }
                }

                delta *= node.activationFunction.derivative(node.a)

                node.d = delta

                for ((j, prevNode) in prevLayer.withIndex()) {
                    val W = prevNode.a

                    val gradient = delta * W

                    weights[l - 1][i][j] -= LEARNING_RATE * gradient
                }
            }
        }
    }

    companion object {
        private const val TAG = "NetworkDense"

        /**
         * create NetworkDense with l.size layers
         */
        fun create(
            activationFunction: ActivationFunction,
            vararg l: Int
        ): NetworkDense {
            val layers = Array(l.size) { i ->
                if (i != l.lastIndex)
                    Array(l[i] + 1) { j ->
                        if (j < l[i]) {
                            DenseNode(activationFunction)
                        } else {
                            DenseNode(identity).apply {
                                a = 1f
                            }
                        }
                    }
                else
                    Array(l[i]) {
                        DenseNode(activationFunction)
                    }
            }

            val weights = initW(l)

            return NetworkDense(layers, weights)
        }

        private fun initW(l: IntArray): Array<Array<Array<Float>>> {
            val weights = Array(l.size - 1) { i ->
                Array(l[i + 1]) {
                    Array(l[i] + 1) { j ->
                        val limit = sqrt(6.toDouble()) / sqrt((l[i + 1] + l[i]).toDouble())
                        Random.nextDouble(-limit, limit).toFloat()
                    }
                }
            }

            return weights
        }
    }
}

class RN : NetworkDense(
    arrayOf(
        Array(7) { DenseNode(identity) },
        Array<DenseNode>(7) { LSTMNode() },
        Array(7) { DenseNode(identity) },
        Array<DenseNode>(7) { LSTMNode() },
        Array(7) { DenseNode(identity) }
    ),

    Array(5) {
        Array(7) {
            Array(7) { 0.33f }
        }
    }
) {
    private class LSTMNode : DenseNode(
        identity
    ) {
        val cell = Cell()

        override fun feedThrough(input: Float): Float {
            cell.feed(input)

            a = cell.c

            return cell.c
        }
    }
}

// training functions
const val TRAINING_TAG = "Training"

val network = NetworkDense.create(sigmoid, 4, 5, 5, 3)

const val BATCH_COUNT = 1000

fun batchTrain() {
    for (i in 1..BATCH_COUNT) {
        hits = 0

        seqTrain()

        Log.e(
            TRAINING_TAG,
            "batch $i of $BATCH_COUNT - hits=${100f * (hits.toFloat() / trainingSet.size.toFloat())}% ($hits / ${trainingSet.size})"
        )
    }

    Log.e(
        TRAINING_TAG, "performing random check - result=${
            if (randomTrain()) "hit"
            else "miss"
        }"
    )
}

var hits = 0

fun randomTrain(): Boolean {
    val index = Random.nextInt(0..trainingSet.lastIndex)

    val entry = trainingSet[index]

    return singleTrain(entry)
}

fun singleTrain(entry: TrainingEntry): Boolean {
    val o = network.feed(
        entry.sepalLength,
        entry.sepalWidth,
        entry.petalLength,
        entry.petalWidth
    )

    var max = 0
    var m = Float.MIN_VALUE

    for ((i, output) in o.withIndex()) {
        if (output.a > m) {
            m = output.a
            max = i
        }
    }

    val label = if (max == 0) "setosa"
    else if (max == 1) "versicolor"
    else "virginica"

//        Log.e(TRAINING_TAG, "output=$label expected=${entry.label} -> ${if (label == entry.label) "hit".also { hitCount ++ } else "miss"}")

    val trainingValues = map[entry.label]!!

    network.computeGradients(
        trainingValues[0],
        trainingValues[1],
        trainingValues[2]
    )

    if (label == entry.label) {
        hits++
        return true
    }

    return false
}

fun seqTrain() {
    for (entry in trainingSet) {
        singleTrain(entry)
    }
}

// repeat on full set
data class TrainingEntry(
    val sepalLength: Float,
    val sepalWidth: Float,
    val petalLength: Float,
    val petalWidth: Float,
    val label: String
)

val map: LinkedHashMap<String, FloatArray> = linkedMapOf(
    "setosa" to floatArrayOf(1f, 0f, 0f),
    "versicolor" to floatArrayOf(0f, 1f, 0f),
    "virginica" to floatArrayOf(0f, 0f, 1f)
)

val trainingSet = arrayOf(
    TrainingEntry(5.1f, 3.5f, 1.4f, 0.2f, "setosa"),
    TrainingEntry(4.9f, 3.0f, 1.4f, 0.2f, "setosa"),
    TrainingEntry(4.7f, 3.2f, 1.3f, 0.2f, "setosa"),
    TrainingEntry(4.6f, 3.1f, 1.5f, 0.2f, "setosa"),
    TrainingEntry(5.0f, 3.6f, 1.4f, 0.2f, "setosa"),
    TrainingEntry(5.4f, 3.9f, 1.7f, 0.4f, "setosa"),
    TrainingEntry(4.6f, 3.4f, 1.4f, 0.3f, "setosa"),
    TrainingEntry(5.0f, 3.4f, 1.5f, 0.2f, "setosa"),
    TrainingEntry(4.4f, 2.9f, 1.4f, 0.2f, "setosa"),
    TrainingEntry(4.9f, 3.1f, 1.5f, 0.1f, "setosa"),
    TrainingEntry(5.4f, 3.7f, 1.5f, 0.2f, "setosa"),
    TrainingEntry(4.8f, 3.4f, 1.6f, 0.2f, "setosa"),
    TrainingEntry(4.8f, 3.0f, 1.4f, 0.1f, "setosa"),
    TrainingEntry(4.3f, 3.0f, 1.1f, 0.1f, "setosa"),
    TrainingEntry(5.8f, 4.0f, 1.2f, 0.2f, "setosa"),
    TrainingEntry(5.7f, 4.4f, 1.5f, 0.4f, "setosa"),
    TrainingEntry(5.4f, 3.9f, 1.3f, 0.4f, "setosa"),
    TrainingEntry(5.1f, 3.5f, 1.4f, 0.3f, "setosa"),
    TrainingEntry(5.7f, 3.8f, 1.7f, 0.3f, "setosa"),
    TrainingEntry(5.1f, 3.8f, 1.5f, 0.3f, "setosa"),
    TrainingEntry(5.4f, 3.4f, 1.7f, 0.2f, "setosa"),
    TrainingEntry(5.1f, 3.7f, 1.5f, 0.4f, "setosa"),
    TrainingEntry(4.6f, 3.6f, 1.0f, 0.2f, "setosa"),
    TrainingEntry(5.1f, 3.3f, 1.7f, 0.5f, "setosa"),
    TrainingEntry(4.8f, 3.4f, 1.9f, 0.2f, "setosa"),
    TrainingEntry(5.0f, 3.0f, 1.6f, 0.2f, "setosa"),
    TrainingEntry(5.0f, 3.4f, 1.6f, 0.4f, "setosa"),
    TrainingEntry(5.2f, 3.5f, 1.5f, 0.2f, "setosa"),
    TrainingEntry(5.2f, 3.4f, 1.4f, 0.2f, "setosa"),
    TrainingEntry(4.7f, 3.2f, 1.6f, 0.2f, "setosa"),
    TrainingEntry(4.8f, 3.1f, 1.6f, 0.2f, "setosa"),
    TrainingEntry(5.4f, 3.4f, 1.5f, 0.4f, "setosa"),
    TrainingEntry(5.2f, 4.1f, 1.5f, 0.1f, "setosa"),
    TrainingEntry(5.5f, 4.2f, 1.4f, 0.2f, "setosa"),
    TrainingEntry(4.9f, 3.1f, 1.5f, 0.1f, "setosa"),
    TrainingEntry(5.0f, 3.2f, 1.2f, 0.2f, "setosa"),
    TrainingEntry(5.5f, 3.5f, 1.3f, 0.2f, "setosa"),
    TrainingEntry(4.9f, 3.1f, 1.5f, 0.1f, "setosa"),
    TrainingEntry(4.4f, 3.0f, 1.3f, 0.2f, "setosa"),
    TrainingEntry(5.1f, 3.4f, 1.5f, 0.2f, "setosa"),
    TrainingEntry(5.0f, 3.5f, 1.3f, 0.3f, "setosa"),
    TrainingEntry(4.5f, 2.3f, 1.3f, 0.3f, "setosa"),
    TrainingEntry(4.4f, 3.2f, 1.3f, 0.2f, "setosa"),
    TrainingEntry(5.0f, 3.5f, 1.6f, 0.6f, "setosa"),
    TrainingEntry(5.1f, 3.8f, 1.9f, 0.4f, "setosa"),
    TrainingEntry(4.8f, 3.0f, 1.4f, 0.3f, "setosa"),
    TrainingEntry(5.1f, 3.8f, 1.6f, 0.2f, "setosa"),
    TrainingEntry(4.6f, 3.2f, 1.4f, 0.2f, "setosa"),
    TrainingEntry(5.3f, 3.7f, 1.5f, 0.2f, "setosa"),
    TrainingEntry(5.0f, 3.3f, 1.4f, 0.2f, "setosa"),
    TrainingEntry(7.0f, 3.2f, 4.7f, 1.4f, "versicolor"),
    TrainingEntry(6.4f, 3.2f, 4.5f, 1.5f, "versicolor"),
    TrainingEntry(6.9f, 3.1f, 4.9f, 1.5f, "versicolor"),
    TrainingEntry(5.5f, 2.3f, 4.0f, 1.3f, "versicolor"),
    TrainingEntry(6.5f, 2.8f, 4.6f, 1.5f, "versicolor"),
    TrainingEntry(5.7f, 2.8f, 4.5f, 1.3f, "versicolor"),
    TrainingEntry(6.3f, 3.3f, 4.7f, 1.6f, "versicolor"),
    TrainingEntry(4.9f, 2.4f, 3.3f, 1.0f, "versicolor"),
    TrainingEntry(6.6f, 2.9f, 4.6f, 1.3f, "versicolor"),
    TrainingEntry(5.2f, 2.7f, 3.9f, 1.4f, "versicolor"),
    TrainingEntry(5.0f, 2.0f, 3.5f, 1.0f, "versicolor"),
    TrainingEntry(5.9f, 3.0f, 4.2f, 1.5f, "versicolor"),
    TrainingEntry(6.0f, 2.2f, 4.0f, 1.0f, "versicolor"),
    TrainingEntry(6.1f, 2.9f, 4.7f, 1.4f, "versicolor"),
    TrainingEntry(5.6f, 2.9f, 3.6f, 1.3f, "versicolor"),
    TrainingEntry(6.7f, 3.1f, 4.4f, 1.4f, "versicolor"),
    TrainingEntry(5.6f, 3.0f, 4.5f, 1.5f, "versicolor"),
    TrainingEntry(5.8f, 2.7f, 4.1f, 1.0f, "versicolor"),
    TrainingEntry(6.2f, 2.2f, 4.5f, 1.5f, "versicolor"),
    TrainingEntry(5.6f, 2.5f, 3.9f, 1.1f, "versicolor"),
    TrainingEntry(5.9f, 3.2f, 4.8f, 1.8f, "versicolor"),
    TrainingEntry(6.1f, 2.8f, 4.0f, 1.3f, "versicolor"),
    TrainingEntry(6.3f, 2.5f, 4.9f, 1.5f, "versicolor"),
    TrainingEntry(6.1f, 2.8f, 4.7f, 1.2f, "versicolor"),
    TrainingEntry(6.4f, 2.9f, 4.3f, 1.3f, "versicolor"),
    TrainingEntry(6.6f, 3.0f, 4.4f, 1.4f, "versicolor"),
    TrainingEntry(6.8f, 2.8f, 4.8f, 1.4f, "versicolor"),
    TrainingEntry(6.7f, 3.0f, 5.0f, 1.7f, "versicolor"),
    TrainingEntry(6.0f, 2.9f, 4.5f, 1.5f, "versicolor"),
    TrainingEntry(5.7f, 2.6f, 3.5f, 1.0f, "versicolor"),
    TrainingEntry(5.5f, 2.4f, 3.8f, 1.1f, "versicolor"),
    TrainingEntry(5.5f, 2.4f, 3.7f, 1.0f, "versicolor"),
    TrainingEntry(5.8f, 2.7f, 3.9f, 1.2f, "versicolor"),
    TrainingEntry(6.0f, 2.7f, 5.1f, 1.6f, "versicolor"),
    TrainingEntry(5.4f, 3.0f, 4.5f, 1.5f, "versicolor"),
    TrainingEntry(6.0f, 3.4f, 4.5f, 1.6f, "versicolor"),
    TrainingEntry(6.7f, 3.1f, 4.7f, 1.5f, "versicolor"),
    TrainingEntry(6.3f, 2.3f, 4.4f, 1.3f, "versicolor"),
    TrainingEntry(5.6f, 3.0f, 4.1f, 1.3f, "versicolor"),
    TrainingEntry(5.5f, 2.5f, 4.0f, 1.3f, "versicolor"),
    TrainingEntry(5.5f, 2.6f, 4.4f, 1.2f, "versicolor"),
    TrainingEntry(6.1f, 3.0f, 4.6f, 1.4f, "versicolor"),
    TrainingEntry(5.8f, 2.6f, 4.0f, 1.2f, "versicolor"),
    TrainingEntry(5.0f, 2.3f, 3.3f, 1.0f, "versicolor"),
    TrainingEntry(5.6f, 2.7f, 4.2f, 1.3f, "versicolor"),
    TrainingEntry(5.7f, 3.0f, 4.2f, 1.2f, "versicolor"),
    TrainingEntry(5.7f, 2.9f, 4.2f, 1.3f, "versicolor"),
    TrainingEntry(6.2f, 2.9f, 4.3f, 1.3f, "versicolor"),
    TrainingEntry(5.1f, 2.5f, 3.0f, 1.1f, "versicolor"),
    TrainingEntry(5.7f, 2.8f, 4.1f, 1.3f, "versicolor"),
    TrainingEntry(6.3f, 3.3f, 6.0f, 2.5f, "virginica"),
    TrainingEntry(5.8f, 2.7f, 5.1f, 1.9f, "virginica"),
    TrainingEntry(7.1f, 3.0f, 5.9f, 2.1f, "virginica"),
    TrainingEntry(6.3f, 2.9f, 5.6f, 1.8f, "virginica"),
    TrainingEntry(6.5f, 3.0f, 5.8f, 2.2f, "virginica"),
    TrainingEntry(7.6f, 3.0f, 6.6f, 2.1f, "virginica"),
    TrainingEntry(4.9f, 2.5f, 4.5f, 1.7f, "virginica"),
    TrainingEntry(7.3f, 2.9f, 6.3f, 1.8f, "virginica"),
    TrainingEntry(6.7f, 2.5f, 5.8f, 1.8f, "virginica"),
    TrainingEntry(7.2f, 3.6f, 6.1f, 2.5f, "virginica"),
    TrainingEntry(6.5f, 3.2f, 5.1f, 2.0f, "virginica"),
    TrainingEntry(6.4f, 2.7f, 5.3f, 1.9f, "virginica"),
    TrainingEntry(6.8f, 3.0f, 5.5f, 2.1f, "virginica"),
    TrainingEntry(5.7f, 2.5f, 5.0f, 2.0f, "virginica"),
    TrainingEntry(5.8f, 2.8f, 5.1f, 2.4f, "virginica"),
    TrainingEntry(6.4f, 3.2f, 5.3f, 2.3f, "virginica"),
    TrainingEntry(6.5f, 3.0f, 5.5f, 1.8f, "virginica"),
    TrainingEntry(7.7f, 3.8f, 6.7f, 2.2f, "virginica"),
    TrainingEntry(7.7f, 2.6f, 6.9f, 2.3f, "virginica"),
    TrainingEntry(6.0f, 2.2f, 5.0f, 1.5f, "virginica"),
    TrainingEntry(6.9f, 3.2f, 5.7f, 2.3f, "virginica"),
    TrainingEntry(5.6f, 2.8f, 4.9f, 2.0f, "virginica"),
    TrainingEntry(7.7f, 2.8f, 6.7f, 2.0f, "virginica"),
    TrainingEntry(6.3f, 2.7f, 4.9f, 1.8f, "virginica"),
    TrainingEntry(6.7f, 3.3f, 5.7f, 2.1f, "virginica"),
    TrainingEntry(7.2f, 3.2f, 6.0f, 1.8f, "virginica"),
    TrainingEntry(6.2f, 2.8f, 4.8f, 1.8f, "virginica"),
    TrainingEntry(6.1f, 3.0f, 4.9f, 1.8f, "virginica"),
    TrainingEntry(6.4f, 2.8f, 5.6f, 2.1f, "virginica"),
    TrainingEntry(7.2f, 3.0f, 5.8f, 1.6f, "virginica"),
    TrainingEntry(7.4f, 2.8f, 6.1f, 1.9f, "virginica"),
    TrainingEntry(7.9f, 3.8f, 6.4f, 2.0f, "virginica"),
    TrainingEntry(6.4f, 2.8f, 5.6f, 2.2f, "virginica"),
    TrainingEntry(6.3f, 2.8f, 5.1f, 1.5f, "virginica"),
    TrainingEntry(6.1f, 2.6f, 5.6f, 1.4f, "virginica"),
    TrainingEntry(7.7f, 3.0f, 6.1f, 2.3f, "virginica"),
    TrainingEntry(6.3f, 3.4f, 5.6f, 2.4f, "virginica"),
    TrainingEntry(6.4f, 3.1f, 5.5f, 1.8f, "virginica"),
    TrainingEntry(6.0f, 3.0f, 4.8f, 1.8f, "virginica"),
    TrainingEntry(6.9f, 3.1f, 5.4f, 2.1f, "virginica"),
    TrainingEntry(6.7f, 3.1f, 5.6f, 2.4f, "virginica"),
    TrainingEntry(6.9f, 3.1f, 5.1f, 2.3f, "virginica"),
    TrainingEntry(5.8f, 2.7f, 5.1f, 1.9f, "virginica"),
    TrainingEntry(6.8f, 3.2f, 5.9f, 2.3f, "virginica"),
    TrainingEntry(6.7f, 3.3f, 5.7f, 2.5f, "virginica"),
    TrainingEntry(6.7f, 3.0f, 5.2f, 2.3f, "virginica"),
    TrainingEntry(6.3f, 2.5f, 5.0f, 1.9f, "virginica"),
    TrainingEntry(6.5f, 3.0f, 5.2f, 2.0f, "virginica"),
    TrainingEntry(6.2f, 3.4f, 5.4f, 2.3f, "virginica"),
    TrainingEntry(5.9f, 3.0f, 5.1f, 1.8f, "virginica"),
)


val rn = RN()

fun trainRNN() {
    val nodes = rn.feed(0f, 1f, 4f, 2f, 3f, 5f, 6f)

    var n = ""
    for (node in nodes) {
        n += " ${node.a}"
    }

    Log.e(TRAINING_TAG, "nodes - $n")
}

fun sampleData(rows: Int, cols: Int) {
    val data = Array(rows) {
        FloatArray(cols) {
            Random.nextFloat()
        }
    }

    for (row in data) {
        Log.e("sample", "data - ${row.joinToString(", ")}")
    }
}