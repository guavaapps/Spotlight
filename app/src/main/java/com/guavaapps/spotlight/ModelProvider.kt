package com.guavaapps.spotlight

import android.util.Base64
import com.google.gson.Gson
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "ModelProvider"

class ModelProvider {
    // aws lambda function url of the model optimiser
    private val FUN = "https://3ofwdrmnxc2t2jz2xneowmk5ji0nvrum.lambda-url.us-east-1.on.aws/"

    // request a model instance built from the latest existing configuration
    fun get(user: String): ModelWrapper {
        return getModel(user)
    }

    // request a new model instance optimized for the given timeline
    fun getOptimized(user: String, timeline: Array<FloatArray> = emptyArray()): ModelWrapper {
        return optimiseModel(user, timeline)
    }

    // internal
    private fun optimiseModel(userId: String, timeline: Array<FloatArray>): ModelWrapper {
        // open a connection to the lambda function
        val conn = openConnection()

        // apply a min-max scaling function to the timeline
        val timeline = FeatureScaler.scale(timeline)

        // create the query map
        val requestObject = createOptimizeRequest(userId, 1, 2, timeline = timeline)

        // get the output stream of the connection and write the query map
        val outputStream = conn.outputStream

        outputStream.write(requestObject.toByteArray())
        outputStream.flush()

        // listen for the response on the input stream
        val inputStream = conn.inputStream

        // convert bytes to string
        val encoded = String(inputStream.readBytes(), Charsets.UTF_8)

        // convert the json string to an object
        val obj = Gson().fromJson(encoded, object {
            var statusCode: Int? = null
            var status: Int? = null
            var body = object {
                var model: String? = null
                var timestamp: Long? = null
                var version: String? = null
            }
        }::class.java)

        // 'model' contains the tf lite file needed to create the local model
        // it is base64 encoded
        val tfliteModel = Base64.decode(obj.body.model!!, 0)

        // create a temporary file containing the tf lite model
        val m = File.createTempFile("model", ".tflite")
        m.writeBytes(tfliteModel)

        // close the connection
        inputStream.close()
        outputStream.close()

        // create a new dlstmp model from the tf lite file
        val model = DlstmpModel(m)

        return ModelWrapper(
            obj.body.version,
            obj.body.timestamp,
            model
        )
    }

    private fun getModel(userId: String): ModelWrapper {
        val conn = openConnection()

        val requestObject = createGetRequest(userId)

        val outputStream = conn.outputStream

        outputStream.write(requestObject.toByteArray())
        outputStream.flush()

        val inputStream = conn.inputStream

        val encoded = String(inputStream.readBytes(), Charsets.UTF_8)

        val obj = Gson().fromJson(encoded, object {
            var statusCode: Int? = null
            var status: Int? = null
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

        val model = DlstmpModel(m)

        return ModelWrapper(
            obj.body.version,
            obj.body.timestamp,
            model
        )
    }

    // open an http connection to the aws lambda function
    private fun openConnection(): HttpURLConnection {
        val conn = URL(FUN).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("Content-type", "*/*")
        conn.setRequestProperty("Accept", "*/*")
        conn.doInput = true
        conn.doOutput = true

        return conn
    }

    // create a query map with all the necessary params to build
    // a model from an exiting config
    private fun createGetRequest(
        userId: String,
        lookBack: Int = 1,
        epochs: Int = 0,
    ): String {
        val requestObject = Gson().toJson(
            object {
                // spotify id of the current user
                var user_id = userId
                // 'GET' to build from config
                var action = "GET"
                // number of timesteps to use for each training batch
                var look_back = lookBack
                // number of epochs to train over
                var epochs = epochs
                // tracks to train on
                var timeline = emptyArray<Array<FloatArray>>()
            }
        )

        return requestObject
    }

    // create a query map with the params to build and optimize a
    // new model
    private fun createOptimizeRequest(
        userId: String,
        lookBack: Int = 1,
        epochs: Int = 2,
        timeline: Array<FloatArray>,
    ): String {
        val requestObject = Gson().toJson(
            object {
                var user_id = userId
                var action = "CREATE"
                var look_back = lookBack
                var epochs = epochs
                var timeline = timeline
            }
        )

        return requestObject
    }
}

// contains the dlstmp model and its metadata
data class ModelWrapper(
    var verion: String?,
    var timestamp: Long?,
    var model: DlstmpModel?,
)