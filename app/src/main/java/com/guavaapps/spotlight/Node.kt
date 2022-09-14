package com.guavaapps.spotlight

import kotlin.math.max

class Node(val activation: (Float) -> Float) {
    var sigma = 0f
    var a = 0f
    var d = 0f

    fun feedThrough(vararg inputs: Float): Float {
        sigma = inputs.sum()
        a = activation (sigma)

        return a
    }
}

fun reLu(input: Float): Float {
    return max(0f, input)
}

fun dReLu (input: Float) = if (input > 0) 1f else 0f

fun noActivation (input: Float) = input
