package com.guavaapps.spotlight

import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import com.google.gson.Gson
import retrofit.http.POST
import retrofit.http.Body
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL

interface LambdaService {
    @POST("/")
    fun getModel(@Body data: String): Any
}

class LambdaModel {
    var model: String? = null
    var weights: FloatArray? = null
    var states: FloatArray? = null
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

class LambdaStructure {

}