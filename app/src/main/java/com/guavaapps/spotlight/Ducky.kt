package com.guavaapps.spotlight

import android.content.Context
import android.util.Log
import android.widget.Toast
import retrofit.Callback

private const val TAG = "Ducky"

object Ducky {
    fun quack(context: Context) {
        Toast.makeText(context, "quack!", Toast.LENGTH_SHORT)
            .show()
    }
}