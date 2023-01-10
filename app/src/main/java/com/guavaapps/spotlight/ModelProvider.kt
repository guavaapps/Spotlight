package com.guavaapps.spotlight

import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "ModelProvider"

class ModelProvider {
    private val FUN = "https://3ofwdrmnxc2t2jz2xneowmk5ji0nvrum.lambda-url.us-east-1.on.aws/"

    fun get (user: String, timeline: FloatArray = floatArrayOf()): ModelWrapper {
        return getModel(user, timeline)
    }

    fun getOptimized (user: String, timeline: FloatArray = floatArrayOf()): ModelWrapper {
        return optimiseModel(user, timeline)
    }

    private fun optimiseModel(userId: String, timeline: FloatArray): ModelWrapper {
        val conn = openConnection()

        val requestObject = createOptimizeRequest(userId, timeline = timeline)

        val outputStream = conn.outputStream

        outputStream.write(requestObject.toByteArray())
        outputStream.flush()

        val inputStream = conn.inputStream

        val encoded = String(inputStream.readBytes(), Charsets.UTF_8)
        Log.e(TAG, "json - $encoded")

        val obj = Gson().fromJson(encoded, object {
            var statusCode: Int? = null
            var optimised: Boolean? = null
            var body = object {
                var model: String? = null
                var timestamp: Long? = null
                var version: String? = null
            }
        }::class.java)

        val tfliteModel = Base64.decode(obj.body.model!!, 0)

        val m = File.createTempFile("model", ".tflite")
        m.writeBytes(tfliteModel)

        inputStream.close()
        outputStream.close()

        val model = DLSTMPModel(m)

        return ModelWrapper(
            obj.body.version,
            obj.body.timestamp,
            model
        )
    }

    private fun getModel(userId: String, timeline: FloatArray): ModelWrapper {
        val conn = openConnection()

        val requestObject = createGetRequest(userId, timeline = timeline)

        val outputStream = conn.outputStream

        outputStream.write(requestObject.toByteArray())
        outputStream.flush()

        val inputStream = conn.inputStream

        val encoded = String(inputStream.readBytes(), Charsets.UTF_8)
        Log.e(TAG, "json - $encoded")

        val obj = Gson().fromJson(encoded, object {
            var statusCode: Int? = null
            var optimised: Boolean? = null
            var body = object {
                var model: String? = null
                var timestamp: Long? = null
                var version: String? = null
            }
        }::class.java)

        val tfliteModel = Base64.decode(obj.body.model!!, 0)

        val m = File.createTempFile("model", ".tflite")

        Log.e(TAG, "model saved at - ${m.absolutePath}")

        m.writeBytes(tfliteModel)

        inputStream.close()
        outputStream.close()

        val model = DLSTMPModel(m)

        return ModelWrapper(
            obj.body.version,
            obj.body.timestamp,
            model
        )
    }

    private fun openConnection(): HttpURLConnection {
        val conn = URL(FUN).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("Content-type", "*/*")
        conn.setRequestProperty("Accept", "*/*")
        conn.doInput = true
        conn.doOutput = true

        return conn
    }

    private fun createGetRequest(
        userId: String,
        lookBack: Int = 1,
        epochs: Int = 0,
        timeline: FloatArray
    ): String {
        val requestObject = Gson().toJson(
            object {
                var user_id = userId
                var action = "get"
                var look_back = lookBack
                var epochs = epochs
                var timeline = timeline
            }
        )

        return requestObject
    }

    private fun createOptimizeRequest(
        userId: String,
        lookBack: Int = 5,
        epochs: Int = 2,
        timeline: FloatArray
    ): String {
        val requestObject = Gson().toJson(
            object {
                var user_id = userId
                var action = "create"
                var look_back = lookBack
                var epochs = epochs
                var timeline = timeline
            }
        )

        return requestObject
    }

//    fun optimiseModel2(userId: String): DLSTMPModel {
//        val conn = URL(FUN).openConnection() as HttpURLConnection
//        conn.requestMethod = "GET"
//        conn.setRequestProperty("Content-type", "*/*")
//        conn.setRequestProperty("Accept", "*/*")
//        conn.doInput = true
//        conn.doOutput = true
//
//        val jsonConfig = Gson().toJson(
//            object {
//                var spotify_id = userId
//                var action = "create"
//                var look_back = 5
//                var epochs = 2
//            }
//        )
//
//        val outputStream = conn.outputStream
//        val inputStream = conn.inputStream
//
//        outputStream.write(jsonConfig.toByteArray())
//
//        val tfliteModel = Base64.decode(inputStream.readBytes(), 0)
//
//        val m = File.createTempFile("model", ".tflite")
//        m.writeBytes(tfliteModel)
//
//        return DLSTMPModel(m)
//    }
//
//    fun getModel2(userId: String): DLSTMPModel {
//        val conn = URL(FUN).openConnection() as HttpURLConnection
//        conn.requestMethod = "GET"
//        conn.connectTimeout = 10000
//        conn.readTimeout = 50000
//        conn.setRequestProperty("Content-type", "*/*")
//        conn.setRequestProperty("Accept", "*/*")
//        conn.doInput = true
//        conn.doOutput = true
//
//        val modelConfig = Gson().toJson(
//            object {
//                var user_id = userId
//                var action = "get"
//                var look_back = 1
//                var epochs = 2
//            }
//        )
//
//        val outputStream = conn.outputStream
//
//        outputStream.write(modelConfig.toByteArray())
//        outputStream.flush()
//
//        val inputStream = conn.inputStream
//
//        val encoded = String(inputStream.readBytes(), Charsets.UTF_8)
//        Log.e(TAG, "json - $encoded")
//
//        val obj = Gson().fromJson(encoded, object {
//            var statusCode: Int? = null
//            var optimised: Boolean? = null
//            var body = object {
//                var model: String? = null
//                var timestamp: Long? = null
//                var version: String? = null
//            }
//        }::class.java)
//
//        val tfliteModel = Base64.decode(obj.body.model!!, 0)
//
//        val m = File.createTempFile("model", ".tflite")
//        m.writeBytes(tfliteModel)
//
//        inputStream.close()
//        outputStream.close()
//
//        return DLSTMPModel(m)
//    }
}

data class ModelWrapper(
    var verion: String?,
    var timestamp: Long?,
    var model: DLSTMPModel,
)