package com.guavaapps.spotlight

import android.content.Context
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import com.guavaapps.components.bitmap.BitmapTools
import com.guavaapps.spotlight.realm.ArtistWrapper
import com.guavaapps.spotlight.realm.Model
import com.guavaapps.spotlight.realm.SpotifyObject
import com.guavaapps.spotlight.realm.Track
import com.guavaapps.spotlight.realm.User
import io.realm.*
import io.realm.mongodb.App
import io.realm.mongodb.AppConfiguration
import io.realm.mongodb.Credentials
import io.realm.mongodb.sync.*
import org.bson.Document
import org.bson.types.ObjectId
import org.tensorflow.lite.Interpreter
import retrofit.http.Body
import retrofit.http.POST
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import java.lang.Class as Class

private const val TAG = "Service"

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
    private val LOOK_BACK = 1

    private var interpreter: Interpreter

    init {
        interpreter = Interpreter(model)
    }

    fun getNextTest(): FloatArray {
        // create a 4-timestep, 2-feature timeline and run on test model

        val testTimeline = Array(4) { FloatArray(2) }

        return getNext(testTimeline)
    }

    fun testOn5(): FloatArray {
        val testTimeline = arrayOf(
            floatArrayOf(0.15f, 0.7f),
            floatArrayOf(0.15f, 0.7f),
            floatArrayOf(0.05f, 0.6f),
            floatArrayOf(0.05f, 0.5f),
            floatArrayOf(0.05f, 0.45f),
        )

        return getNext(testTimeline)
    }

    fun getNext(timeline: Array<FloatArray>): FloatArray {
        interpreter.allocateTensors()

//        checkInputShape(interpreter, timeline)

        val DEBUG_TIMESTAMP = System.currentTimeMillis()

        Log.d(
            TAG,
            "running inference - timestamp=$DEBUG_TIMESTAMP timeline=[${timeline.size} ${timeline.first().size}] inputShape=[${
                interpreter.getInputTensor(0).shape().joinToString(" ")
            }]"
        )

        val output = arrayOf(
            FloatArray(timeline.first().size)
        )

        val timelineScaler = scaleMinMax(
            timeline
                .takeLast(LOOK_BACK)
                .reversed()
                .toTypedArray()
        )
        val scaledTimeline = timelineScaler.result

        interpreter.run(arrayOf(scaledTimeline), output)

        interpreter.close()

        return invertMinMax(timelineScaler.apply {
            result = output
        })[0].also {
            Log.d(
                TAG,
                "result for timeline - size=${timeline.size} features=${timeline.first().size} result=[${
                    it.joinToString(" ")
                }]"
            )
        }
    }

    private fun checkInputShape(interpreter: Interpreter, timeline: Array<FloatArray>) {
        val inputShape =
            interpreter.getInputTensor(0).shape().slice(1..1) // batch size (at index 0) is always 1

        if (inputShape[0] != timeline.size || inputShape[1] != timeline.first().size) throw IllegalArgumentException(
            "Timeline doesn't match required input shape - inputShape=[$inputShape] timeline=[${timeline.first().size}]"
        )
    }

    private fun createBatches(timeline: FloatArray): Array<FloatArray> {
//        val scaled = scaleMinMax(timeline)
        var batches = mutableListOf<FloatArray>()

        for (i in 0..timeline.size - LOOK_BACK - 1) {
            val a = timeline.slice(i..i + LOOK_BACK - 1).toFloatArray()
            batches.add(a)
        }

        return batches.toTypedArray()
    }

    private fun scaleMinMax(data: Array<FloatArray>): Scaler {
        var min = Float.MIN_VALUE
        var max = Float.MAX_VALUE

        data.forEach { x ->
            val mn = x.min()
            val mx = x.max()

            if (mn < min) min = mn
            if (mx < max) max = mx
        }

        var scaled = Array(data.size) { i ->
            val d = max - min

            FloatArray(data[i].size) { j ->
                (data[i][j] - min) / d
            }
        }


        return Scaler(
            scaled,
            min,
            max
        )
    }

    private fun invertMinMax(data: Scaler): Array<FloatArray> {
        val delta = data.max - data.min

        var inverted = Array(data.result.size) { i ->

            FloatArray(data.result[i].size) { j ->
                data.result[i][j] * delta + data.min
            }
        }

        return inverted
    }

    private data class Scaler(
        var result: Array<FloatArray>,
        var min: Float,
        var max: Float
    )
}

class UserRepository(
    private val userId: String,
    mongoClient: MongoClient
) {
    private var realm: Realm

    init {
        realm = mongoClient.login(userId)
    }

    fun get(): User {
        return realm.where(User::class.java)
            .equalTo("spotify_id", userId)
            .findFirst()!!
    }

    fun update(user: User) {
        realm.executeTransaction {
            it.where(User::class.java)
                .equalTo("spotify_id", user.spotify_id)
                .findAll()
                .deleteAllFromRealm()

            it.insert(user)
        }
    }
}

class ModelRepository(
    private val userId: String,
    mongoClient: MongoClient,
    private val remoteModelDataSource: RemoteModelDataSource
) {
    var model: DLSTMPModel
        private set

    init {
        model = loadModel()

        mongoClient.login(userId).where(Model::class.java)
            .equalTo("spotify_id", userId)
            .findFirst()
            ?.addChangeListener(RealmObjectChangeListener { t, changeSet ->
                loadModel()
            })
    }

    private fun loadModel(): DLSTMPModel {
        return remoteModelDataSource.getModel(userId)
    }
}

class LocalRealm(context: Context) {
    private val realm: Realm

    init {
        val config = RealmConfiguration.Builder()
            .name("Spotlight")
            .build()

        realm = Realm.getInstance(config)
    }

    fun <E : SpotifyObject> get(clazz: Class<E>, id: String): E? {
        return realm.where(clazz)
            .equalTo("id", id)
            .findFirst()
    }

    fun <E : SpotifyObject> put(obj: E) {
        realm.executeTransaction {
            it.where(obj::class.java)
                .equalTo("id", obj.id)
                .findAll()

            it.insert(obj)
        }
    }
}

class RemoteModelDataSource(
) {
    private val FUN = ""

    fun createModel(userId: String): DLSTMPModel {
        val conn = URL(FUN).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-type", "*/*")
        conn.setRequestProperty("Accept", "*/*")
        conn.doInput = true
        conn.doOutput = true

        val jsonConfig = Gson().toJson(
            object {
                var spotify_id = userId
                var action = "create"
                var look_back = 5
                var epochs = 2
            }
        )
        Log.e(TAG, "jsonConfig - $jsonConfig")

        val outputStream = conn.outputStream
        val inputStream = conn.inputStream

        outputStream.write(jsonConfig.toByteArray())

        val tfliteModel = Base64.decode(inputStream.readBytes(), 0)

        val m = File.createTempFile("model", ".tflite")
        m.writeBytes(tfliteModel)

        return DLSTMPModel(m)
    }

    fun getModel(userId: String): DLSTMPModel {
        val conn = URL(FUN).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-type", "*/*")
        conn.setRequestProperty("Accept", "*/*")
        conn.doInput = true
        conn.doOutput = true

        val jsonConfig = Gson().toJson(
            object {
                var spotify_id = userId
                var action = "get"
                var look_back = 5
                var epochs = 2
            }
        )
        Log.e(TAG, "jsonConfig - $jsonConfig")

        val outputStream = conn.outputStream
        val inputStream = conn.inputStream

        outputStream.write(jsonConfig.toByteArray())

        val tfliteModel = Base64.decode(inputStream.readBytes(), 0)

        val m = File.createTempFile("model", ".tflite")
        m.writeBytes(tfliteModel)

        return DLSTMPModel(m)
    }
}

class MongoClient(val context: Context) {
    private val APP = "spotlight-kceqr"

    private val USERS = "user_subscription"
    private val MODELS = "user_subscription"

    private val SPOTIFY_ID = "spotify_id"

    private lateinit var app: App
    private var appUser: io.realm.mongodb.User? = null
    private var realm: Realm? = null

    fun login(user: String, callback: Callback<Realm>) {
        Realm.init(context)

        app = App(
            AppConfiguration.Builder(APP)
                .defaultSyncClientResetStrategy(object : DiscardUnsyncedChangesStrategy {
                    override fun onBeforeReset(realm: Realm) {
                        Log.e(TAG, "client reset onBeforeReset - realmPath=${realm.path}")
                    }

                    override fun onAfterReset(before: Realm, after: Realm) {
                        Log.e(
                            TAG,
                            "client reset onAfterReset - beforePath=${before.path} afterPath=${after.path}"
                        )
                    }

                    override fun onError(session: SyncSession, error: ClientResetRequiredError) {
                        Log.e(TAG, "client reset onError - error=${error.message}")
                    }

                })
                .build()
        )

        val credentials = Credentials.customFunction(
            Document(
                mapOf(
                    SPOTIFY_ID to user
                )
            )
        )

        app.loginAsync(credentials) {
            if (it.isSuccess) {
                val appUser = app.currentUser()!!

                val userData = appUser.customData
                Log.e(TAG, "userData: $userData")

                val configuration = SyncConfiguration.Builder(appUser)
                    .allowWritesOnUiThread(true)
                    .allowQueriesOnUiThread(true)
                    .initialSubscriptions { realm, subscriptions ->
//                        subscriptions.removeAll()
                        subscriptions.addOrUpdate(
                            Subscription.create(
                                USERS,
                                realm.where(User::class.java)
                                    .equalTo(SPOTIFY_ID, user)
                            )
                        )
                    }
                    .build()

                Realm.getInstanceAsync(configuration, object : Realm.Callback() {
                    override fun onSuccess(realm: Realm) {
                        Log.e(TAG, "realm instance obtained")

                        callback.onResult(realm)
                    }

                    override fun onError(exception: Throwable) {
                        Log.e(TAG, "realm sync error - ${exception.message}")
                    }
                })
            } else {
                Log.e(TAG, "${it.error.message}")
            }
        }
    }

    fun login(user: String): Realm {
        if (appUser != null && appUser!!.customData.get("spotify_id", String::class.java)
                .equals(user)
        ) {
            return realm!!
        }

        Realm.init(context)

        app = App(
            AppConfiguration.Builder(APP)
                .defaultSyncClientResetStrategy(object : DiscardUnsyncedChangesStrategy {
                    override fun onBeforeReset(realm: Realm) {
                        Log.e(TAG, "client reset onBeforeReset - realmPath=${realm.path}")
                    }

                    override fun onAfterReset(before: Realm, after: Realm) {
                        Log.e(
                            TAG,
                            "client reset onAfterReset - beforePath=${before.path} afterPath=${after.path}"
                        )
                    }

                    override fun onError(session: SyncSession, error: ClientResetRequiredError) {
                        Log.e(TAG, "client reset onError - error=${error.message}")
                    }

                })
                .build()
        )

        val credentials = Credentials.customFunction(
            Document(
                mapOf(
                    SPOTIFY_ID to user,
                )
            )
        )

        appUser = app.login(credentials)

        val userData = appUser!!.customData
        Log.e(TAG, "userData: $userData")

        val configuration = SyncConfiguration.Builder(appUser)
            .allowWritesOnUiThread(true)
            .allowQueriesOnUiThread(true)
            .initialSubscriptions { realm, subscriptions ->
//                        subscriptions.removeAll()
                subscriptions.addOrUpdate(
                    Subscription.create(
                        USERS,
                        realm.where(User::class.java)
                            .equalTo(SPOTIFY_ID, user)
                    )
                )

                subscriptions.addOrUpdate(
                    Subscription.create(
                        MODELS,
                        realm.where(Model::class.java)
                            .equalTo(SPOTIFY_ID, user)
                    )
                )
            }
            .build()

        realm = Realm.getInstance(configuration)

        // TODO move asRealmObject mongoDB scheduled fun
        realm!!.executeTransaction {
            val firstLogin = it.where(User::class.java)
                .equalTo("spotify_id", user)
                .count() == 0L

            val user = User(ObjectId()).apply {
                spotify_id = user
                last_login = System.currentTimeMillis()
            }

            if (firstLogin) {
                it.insert(user)
                user.date_signed_up = user.last_login
            } else {
                it.insertOrUpdate(user)
            }
        }

        return realm!!
    }

    interface Callback<T> {
        fun onResult(result: T)
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

// aaaaaaa idk what asRealmObject do
// statesless lstm

class LambdaStructure {

}