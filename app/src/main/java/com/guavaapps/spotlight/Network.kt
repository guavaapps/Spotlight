package com.guavaapps.spotlight

import android.util.Log
import java.lang.Exception

typealias Layer = Array<Node>
typealias WeightPool = Array<Float>

class Network private constructor(
    private val layers: Array<Layer>,
    private val weights: Array<Array<WeightPool>>
) {
    private var inputLayer = Array(0) {Node (::noActivation) }

    private val RATE = 0.001f

    fun feed(vararg inputs: Float): Array<Node> {
        // feed into first layer
        inputLayer = Array (inputs.size) { i -> Node (::noActivation).apply {
            a = inputs [i]
        }}
        for ((i, node) in layers [0].withIndex()) {
            var sum = 0f

            for ((j, input) in inputs.withIndex()) {
                val w = weights [0] [i] [j]

                sum += input * w
            }

            node.feedThrough(sum)
        }

        // feed through to next layers
        for ((i, layer) in layers.withIndex()) {
            if (i == 0) continue

            for (j in 0..layer.lastIndex) {
                // pull prev layer
                val node = layer [j]
                var sigma = 0f

                val prevLayer = if (i > 0) layers [i - 1] else inputLayer
                for ((k, node) in prevLayer.withIndex()) {
                    val weight =
                        weights[i][j][k] // layer, node in previous layer, for node in current layer

                    sigma += node.a * weight
                }

                var activated = node.feedThrough(sigma)

                layers[i][j].a = activated
            }
        }

        // debug

        for ((i, weightLayer) in weights.withIndex().reversed()) {
            for ((j, weightPool) in weightLayer.withIndex()) {
                for ((k, weight) in weightPool.withIndex()) {
                }
            }
        }

        for ((i, layer) in layers.withIndex()) {
            var nodes = ""

            for (node in layer) {
                nodes += "${node.a} "
            }

            Log.e(TAG, "layer $i: $nodes")
        }

        return layers.last()
    }

    fun loss(a: Float, y: Float) = (y - a) * (y - a)

    fun loss(vararg y: Float): Float {
        var loss = 0f

        for ((i, node) in layers.last().withIndex()) {
            loss += loss(y[i], node.a)
        }

        return loss
    }

    fun n(vararg y: Float) {
        val outputLayer = layers.last()
        val prevLayer = layers[layers.lastIndex - 1]
        val losses = Array(outputLayer.size) { i -> loss(outputLayer[i].a, y[i]) }
        val lossSum = losses.sum()

        Log.e(TAG, "output layer")

        for ((j, loss) in losses.withIndex()) {
            for ((i, prevNode) in prevLayer.withIndex()) {
                val a = outputLayer.last().a
                val dEda = -2 * loss
                val dzda = if (a < 0) 0f else if (a > 0) 1f else throw Exception("")
                val w = weights.last()[i][j]
                val dzdw = prevNode.a

                val dEdw = dEda * dzda * dzdw

                layers.last()[j].d = dEda// * dzda

                val newW = w - RATE * dEdw

                Log.e(TAG, "dEda=$dEda from=$j@${layers.lastIndex} to=$i@${layers.lastIndex - 1}")

                // apply

                weights.last()[i][j] = newW
            }
        }


    }

    // TODO clean up this code wtf are these variable names

    // D = dC/da

    fun costGradient(l: Int, n: Int): Float {
        val nextLayer = layers[l + 1]

        Log.d(TAG, "    getting grad of layer$l nod$n")

        var sum = 0f

        for ((j, node) in nextLayer.withIndex()) {
            // w * sigma' * d
            val w = weights[l + 1][j][n]
            val dSigma = dReLu(node.sigma)

            sum += w * dSigma * node.d
        }
        Log.d(TAG, "        dE/da is $sum")

        layers [l] [n].d = sum

        return sum
    }

    fun weightGradient (l: Int, n: Int, p: Int): Float {
        // a>l sigma' cG
        val layer = layers [l]
        val prevLayer = if (l > 0) layers [l - 1] else {
            inputLayer
        }
        val a = prevLayer [p].a
        val sigma = dReLu(layer [n].sigma)
        val costGradient = costGradient(l, n)

        return a * sigma * costGradient
    }

    fun h () {
        for (i in weights.lastIndex - 1..0) {
            val wLayer = weights [i]

            if (i == weights.lastIndex) {
                continue
            }

            for ((j, wPool) in wLayer.withIndex()) {
                for ((k, weight) in wPool.withIndex()) {
                    val cost = weightGradient(i, j, k)

                    Log.e(TAG, "    grad between node$j in layer$i and node$k in layer${i - 1} - $cost")
                    Log.e(TAG, "    new weight is ${weight - RATE * cost}")
                }
            }
        }
    }

    fun h2() {
        for (i in layers.lastIndex - 1..1) {
            val layer = layers[i]
            val nextLayer = layers[i + 1]
            val prevLayer = layers[i - 1]

            for ((j, node) in layer.withIndex()) {
                val nextWeights = weights[i][j]

                // dSigma == dadz
                val dSigma = if (node.sigma > 0) 1f else 0f

                var sumE = 0f

                for ((k, node) in nextLayer.withIndex()) {
                    val w = nextWeights[k]
                    val s = if (node.sigma > 0) 1f else 0f
                    val e = node.d * w
//                    val e = d[i + 1][k][j] * s * w

                    sumE += e
                }

                node.d = sumE * dSigma

                for ((l, prevNode) in prevLayer.withIndex()) {
                    // dzdw
                    val prevA = prevNode.a

                    // dCdw
                    val e = sumE * dSigma * prevA;

                    weights[i - 1][j][l] -= 0.01f * e
                }
            }
        }
    }

    companion object {
        private val TAG = "Network"

        fun create(vararg l: Int): Network {
            val layers = Array(l.size - 1) { i ->
                Array(l[i + 1]) {
                    Node(::reLu)
                }
            }

            val weights = Array(l.size - 1) { i ->
                Array(l[i + 1]) {
                    Array(l[i]) {
                        0.33f//(it + 1) / 2f
                    }
                }
            }

            Log.e(TAG, "first layer size = ${weights [0].size}")
            Log.e(TAG, "first layer size node1 = ${weights [0] [0].size}")

            val network = Network(layers, weights);

            return network
        }
    }
}