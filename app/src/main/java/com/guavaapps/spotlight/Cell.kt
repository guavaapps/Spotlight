package com.guavaapps.spotlight

import android.util.Log
import com.pixel.spotifyapi.Objects.AudioFeaturesTrack
import kotlin.math.tan
import kotlin.math.tanh

class Cell {
    private val TAG = "Cell"
    var c = 0f
    var h = 0f

    private var wi = floatArrayOf(0f, 0f)
    private var wf = floatArrayOf(0f, 0f)
    private var wo = floatArrayOf(0f, 0f)
    private var wc = floatArrayOf(0f, 0f)

    fun feed (y: Float) {
        val f = sigmoid.activation(y * wf [0] + h * wf [1])
        val C = tanh (y * wc [0] + h * wc [1])
        val i = sigmoid.activation(y * wi [0] + h * wi [1])
        val o = sigmoid.activation(y * wo [0] + h * wo [1])

        c = f * c + i * C
        h = o * tanh (c)

        Log.e(TAG, "state=$c hidden=$h")
    }
}