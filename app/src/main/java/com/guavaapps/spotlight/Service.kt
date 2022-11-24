package com.guavaapps.spotlight

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import com.guavaapps.spotlight.realm.AppUser
import com.guavaapps.spotlight.realm.Model
import com.guavaapps.spotlight.realm.RealmTrack
import com.pixel.spotifyapi.Objects.*
import io.realm.*
import io.realm.annotations.RealmField
import io.realm.mongodb.App
import io.realm.mongodb.AppConfiguration
import io.realm.mongodb.Credentials
import io.realm.mongodb.User
import io.realm.mongodb.sync.*
import org.bson.Document
import org.tensorflow.lite.Interpreter
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

private const val TAG = "Service"

class DLSTMPModel(model: File) {
    private val LOOK_BACK = 1

    private var interpreter: Interpreter

    init {
        interpreter = Interpreter(model)
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
        var max: Float,
    )
}

class UserRepository(
    private val userId: String,
    private val mongoClient: MongoClient,
) {
    fun get(): AppUser {
        val user = mongoClient.getUser()

        return user
    }

    fun update(user: AppUser) {
        mongoClient.updateUser(user)
    }
}

class ModelRepository(
    private val userId: String,
    mongoClient: MongoClient,
    private val remoteModelDataSource: RemoteModelDataSource,
) {
    var model: DLSTMPModel
        private set

    var modelConfig: Model? = null

    init {
        model = loadModel()

        mongoClient.watchModel {
            if (modelConfig != null && it.timestamp!! > modelConfig!!.timestamp!!) {
                modelConfig = it

                Log.e(TAG, "model changed - ${it.spotify_id} ${it.timestamp}")
            }
        }

//        Handler.createAsync(Looper.getMainLooper())
//            .post {
//                mongoClient.login(userId, object : MongoClient.Callback<Realm> {
//                    override fun onResult(realm: Realm) {
//                        modelConfig = realm.where(Model::class.java)
//                            .equalTo("_id", userId)
//                            .findFirst()
//
//                        Log.e(TAG, "model - ${modelConfig?.spotify_id} ${modelConfig?.timestamp}")
//
//                        modelConfig?.addChangeListener { t: Model, changeSet ->
//                            Log.e(TAG, "model changed - ${t.spotify_id} ${t.timestamp}")
//                        }
//                    }
//
//                })
//            }

//        Handler.createAsync(Looper.getMainLooper())
//            .post {
//                val realm = mongoClient.getRealm(userId, appUser.customData)
//
//                modelConfig = realm.where(Model::class.java)
//                    .equalTo("_id", userId)
//                    .findFirstAsync()
//                modelConfig?.addChangeListener(RealmObjectChangeListener<Model> { t, changeSet ->
//                    Log.e(TAG, "model changed - ${t.spotify_id} ${t.timestamp} ${t.model_params.size}")
//                })
//
//                Handler.createAsync(Looper.getMainLooper())
//                    .postDelayed({
//                        val timestamp = modelConfig?.timestamp
//                        Log.e(TAG, "model - $timestamp")
//                    }, 50000)
//
//
////                val client = appUser.getMongoClient("mongodb-atlas")
////                client.getDatabase("Spotlight")
////                    .getCollection("Model")
////                    .watchAsync (BsonString (userId))
////                    .get {
////                        val changedKey = it.get().documentKey
////
////                        Log.e(TAG, "changedKey - $changedKey")
////                    }
//
//                //(RealmObjectChangeListener { t, changeSet ->
////                    Log.e(TAG, "model updated")
//
//                //loadModel()
////                })
//
////                realm.executeTransaction {
////                    modelConfig!!.timestamp = 0L
////                }
//            }
    }

    fun createModel(): DLSTMPModel {
        return remoteModelDataSource.createModel(userId)
    }

    private fun loadModel(): DLSTMPModel {
        return remoteModelDataSource.getModel(userId)
    }
}

class LocalRealm(context: Context) {
    private val realm: Realm

    init {
        Realm.init(context)

        val config = RealmConfiguration.Builder()
            .name("Spotlight")
            .build()

        realm = Realm.getInstance(config)
    }

    fun <E : RealmObject> get(
        clazz: Class<E>,
        id: String,
        derealm: (r: E?) -> Any? = { it as E },
    ): Any? {
        val realmObject = realm.where(clazz)
            .equalTo("id", id)
            .findFirst()

        return derealm(realmObject)
    }

    fun <E> put(obj: E, realmifier: (o: E) -> RealmObject = { obj as RealmObject }) {
        realm.executeTransaction {
            val realmObject = realmifier(obj)

            val id = obj!!::class.java.getField("id").get(obj) as String

            it.where(realmObject::class.java)
                .equalTo("id", id)
                .findAll()
                .removeFirst()

            it.insert(realmObject)
        }
    }

    fun <E> Any.mapTo(clazz: Class<E>, supportedTypes: List<Class<*>>) {
        val c = this::class.java
        val obj = clazz.newInstance()

        c.fields.forEach {
            val v = it.get(this)

            if (it.type.isPrimitive) {
                clazz.getField(it.name).set(obj, v)
            } else {
                clazz.getField(it.name).set(obj,
                    v.realmify(
                        supportedTypes.first { clazz -> clazz.name == "Realm${c.name}" }
                    )
                )
            }
        }
    }

    companion object {
    }

//    fun <E> Any.realmify(realmClass: Class<E>) {
//        val c = this::class.java
//        val obj = realmClass.newInstance()
//
//        c.fields.forEach {
//            val v = it.get(this)
//
//            if (it.type.isPrimitive) {
//                realmClass.getField(it.name).set(obj, v)
//            } else if (Collection::class.java.isAssignableFrom(it.type)) {
//                // is list
//                if (List::class.java.isAssignableFrom(it.type)) {
//                    realmClass.getField(it.name).set(obj, RealmList(*(v as Array<*>)))
//                }
//                // is map
//                else if (Map::class.java.isAssignableFrom(it.type)) {
//                    realmClass.getField(it.name).set(obj, RealmDictionary(v as Map<String, *>))
//                } else {
//                    throw IllegalArgumentException("unsupported collection type - ${it.name}")
//                }
//            } else {
//                realmClass.getField(it.name).set(obj,
//                    v.realmify(
//                        supportedRealmTypes.first { clazz -> clazz.name == "Realm${c.name}" }
//                    )
//                )
//            }
//        }
//    }
//
//    fun <E> Any.dickdock(clazz: Class<E>) {
//        val c = this::class.java
//        val obj = clazz.newInstance()
//
//        c.fields.forEach {
//            val v = it.get(this)
//
//            if (it.type.isPrimitive) {
//                clazz.getField(it.name).set(obj, v)
//            } else {
//                clazz.getField(it.name).set(obj,
//                    v.realmify(
//                        supportedTypes.first { cl -> cl.name == c.name.removePrefix("Realm") }
//                    )
//                )
//            }
//        }
//    }

    private fun drealm(r: RealmTrack?): Track? {
        r ?: return null

        return Track().apply {
            id = r?.id
        }
    }

    private fun realmifyTrack(track: Track) = RealmTrack().apply {
        id = track.id
        name = track.name
    }

    fun d() {
        put(Track())

    }
}

fun <E> Any.derealmify(objectClass: Class<E>): E {
    val c = this::class.java
    val obj = objectClass.newInstance()!!

    Log.e(TAG, "fields - ${c.fields.size} ${c.declaredFields.size}")
    c.declaredFields.forEach {
        objectClass.fields.find { r -> r.name == it.name } ?: return@forEach

        it.isAccessible = true
        val v = it.get(this) ?: return@forEach

        Log.e(TAG, "${it.name} - isPrimitive=${it.type.isPrimitive}")
        Log.e(TAG, "type - ${it.type.name}")

        val r = objectClass.getField(it.name)
        r.isAccessible = true

        if (it.type.isPrimitive || String::class.java.isAssignableFrom(it.type) || Number::class.java.isAssignableFrom(
                it.type)
        ) {
            Log.e(TAG, "primitive or string")

            r.set(obj, v)
        } else if (RealmList::class.java.isAssignableFrom(it.type)) {
            Log.e(TAG, "list")

            r.apply {
                isAccessible = true
                val items = (v as RealmList<*>).toMutableList()

                set(obj, items)
            }
        }
        // is map
        else if (RealmDictionary::class.java.isAssignableFrom(it.type)) {
            Log.e(TAG, "map")
            val entries =
                (v as RealmDictionary<*>).entries.toTypedArray().map { it.toPair() }.toTypedArray()
            r.set(obj, mutableMapOf(*entries))
        } else {
            Log.e(TAG, "realmifiable")

            val f = if (it in c.declaredFields) {
                c.getDeclaredField(it.name)
            } else {
                c.superclass.getDeclaredField(it.name)
            }

            r.set(obj,
                v.realmify(
                    Class.forName(f.type.simpleName.removePrefix("Realm"))
                )
            )
        }
    }

    return obj
}

fun <E> Any.realmify(realmClass: Class<E>): E {
    val c = this::class.java
    val obj = realmClass.newInstance()!!

    c.fields.forEach {
        realmClass.declaredFields.find { r -> r.name == it.name } ?: return@forEach

        val v = it.get(this) ?: return@forEach

        Log.e(TAG, "${it.name} - isPrimitive=${it.type.isPrimitive}")

        val r = realmClass.getDeclaredField(it.name)
        r.isAccessible = true

        if (it.type.isPrimitive || String::class.java.isAssignableFrom(it.type)) {
            Log.e(TAG, "primitive or string")

            r.set(obj, v)
        } else if (List::class.java.isAssignableFrom(it.type)) {
            Log.e(TAG, "list")

            r.apply {
                isAccessible = true
                set(obj, RealmList(*(v as MutableList<*>).toTypedArray()))
            }
        }
        // is map
        else if (Map::class.java.isAssignableFrom(it.type)) {
            Log.e(TAG, "map")
            r.set(obj, RealmDictionary(v as Map<String, *>))
        } else {
            Log.e(TAG, "realmifiable")

            val f = if (it in c.declaredFields) {
                c.getDeclaredField(it.name)
            } else {
                c.superclass.getDeclaredField(it.name)
            }

            r.set(obj,
                v.realmify(
                    Class.forName("Realm${f.type.simpleName}")
                )
            )
        }
    }

    return obj
}

//fun <E> mapTo(o: Any, objectClass: Class<E>, depthResolver: (o: Any, clazz: Class<*>) ->): E {
//    val c = o::class.java
//    val obj = objectClass.newInstance()!!
//
//    c.fields.forEach {
//        objectClass.declaredFields.find { r -> r.name == it.name } ?: return@forEach
//
//        val v = it.get(o) ?: return@forEach
//
//        Log.e(TAG, "${it.name} - isPrimitive=${it.type.isPrimitive}")
//
//        val r = objectClass.getDeclaredField(it.name)
//        r.isAccessible = true
//
//        if (it.type.isPrimitive || String::class.java.isAssignableFrom(it.type)) {
//            Log.e(TAG, "primitive or string")
//
//            r.set(obj, v)
//        } else if (List::class.java.isAssignableFrom(it.type)) {
//            Log.e(TAG, "list")
//
//            r.apply {
//                isAccessible = true
//                set(obj, RealmList(*(v as MutableList<*>).toTypedArray()))
//            }
//        }
//        // is map
//        else if (Map::class.java.isAssignableFrom(it.type)) {
//            Log.e(TAG, "map")
//            r.set(obj, RealmDictionary(v as Map<String, *>))
//        } else {
//            Log.e(TAG, "realmifiable")
//
//            val f = if (it in c.declaredFields) {
//                c.getDeclaredField(it.name)
//            } else {
//                c.superclass.getDeclaredField(it.name)
//            }
//
//            r.set(obj,
//                v.realmify(
//                    supportedRealmTypes.first { clazz ->
//                        clazz.simpleName == "Realm${f.type.simpleName}"
//                    }
//                )
//            )
//        }
//    }
//
//    return obj
//}

class RemoteModelDataSource(
) {
    private val FUN = "https://3ofwdrmnxc2t2jz2xneowmk5ji0nvrum.lambda-url.us-east-1.on.aws/"
    private val FUN2 = "https://uusruczagjhr7xgbbi7upuh5t40yrret.lambda-url.us-east-1.on.aws/"

    fun createModel(userId: String): DLSTMPModel {
        val conn = URL(FUN).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
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
        conn.requestMethod = "GET"
        conn.connectTimeout = 10000
        conn.readTimeout = 50000
        conn.setRequestProperty("Content-type", "*/*")
        conn.setRequestProperty("Accept", "*/*")
        conn.doInput = true
        conn.doOutput = true

        val modelConfig = Gson().toJson(
            object {
                var user_id = userId
                var action = "get"
                var look_back = 1
                var epochs = 2
            }
        )

        val outputStream = conn.outputStream

        outputStream.write(modelConfig.toByteArray())
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

        return DLSTMPModel(m)
    }
}

class MongoClient(val context: Context) {
    private val APP = "spotlight-gnmnp"//"spotlight-kceqr"

    private val USERS = "user_subscription"
    private val MODELS = "model_subscription"

    private val SPOTIFY_ID = "spotify_id"

    private lateinit var app: App
    private lateinit var appUser: User
    private lateinit var realm: Realm
    private lateinit var user: String
    private var timestamp = 0L


    fun login(user: String) {
        this.user = user

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

        appUser = app.login(credentials)!!
    }

    fun getUser(): AppUser {
        val user = Document(mapOf(
            "_id" to "hehe",
            "spotify_id" to "jfdlsjfkd",
            "created" to Date(System.currentTimeMillis())
        ))

        Handler.createAsync(Looper.getMainLooper()).post {
            appUser.getMongoClient("mongodb-atlas")
                .getDatabase("Spotlight")
                .getCollection("users")
                .findOne(Document(
                    mapOf(
                        "spotify_id" to this.user
                    )
                ))
                .getAsync {
                    if (it.isSuccess) {
                        Log.e(TAG, "HOW IS IT FUCKING NULL ${it == null}")
                        try {
                            val id = it.get()["spotify_id"]
                            Log.e(TAG, "user id - $id")
                        } catch (e: Exception) {
                        }
                    } else {
                        Log.e(TAG, "user error - ${it.error.message}")
                    }
                }

            appUser.getMongoClient("mongodb-atlas")
                .getDatabase("Spotlight")
                .getCollection("Model")
                .findOne(Document(
                    mapOf(
                        "_id" to this.user
                    )
                ))
                .getAsync {
                    if (it.isSuccess) {
                        Log.e(TAG, "HOW IS IT FUCKING NULL ${it == null}")
                        val id = it.get()["_id"]
                        Log.e(TAG, "model id - $id")
                    } else {
                        Log.e(TAG, "model error - ${it.error.message}")
                    }
                }
        }

        Log.e(TAG, "mongoClient user - $user")

        val clazz = AppUser::class.java
        val obj = clazz.newInstance()

        clazz.fields.forEach {
            it.isAccessible = true

            val fieldName = it.getAnnotation(RealmField::class.java)?.value ?: it.name
            val v = user[fieldName]

            it.set(obj, v)
        }

        return obj
    }

    fun updateUser(u: AppUser) {
        val users = appUser.getMongoClient("mongodb-atlas")
            .getDatabase("Spotlight")
            .getCollection("users")

        val obj = Document()

        val clazz = u::class.java

        clazz.fields.forEach {
            it.isAccessible = true

            val fieldName = it.getAnnotation(RealmField::class.java)?.value ?: it.name
            val v = it.get(u)

            obj[fieldName] = v
        }

        users.updateOne(Document("_id", user), obj)
    }

    fun watchModel(result: (model: Model) -> Unit) {
        val models = appUser.getMongoClient("mongodb-atlas")
            .getDatabase("Spotlight")
            .getCollection("Model")

//        watchTillSuccess(models, result)

        // TODO integrate with Matcha and replace MongoClient with instance of Matcha
//        models.watchAsync(BsonString(user)).get {
        models.watchAsync().get {
            Log.e(TAG,
                "watchAsync changed - isSuccess=${it.isSuccess} ${it.get()?.operationType} ${it.get()?.documentKey} ${it.get()?.updateDescription}")

            if (!it.isSuccess) {
                Handler.createAsync(Looper.getMainLooper()).post {
                    models.findOne()
                        .getAsync {
                            if (!it.isSuccess) Log.e(TAG, "watcher error - ${it.error.message}")
                            else Log.e(TAG,
                                "model watcher error, checking config - update? ${timestamp != it.get()["timestamp"]}")
                        }

                }

                return@get
            }

            val modelConfig = it.get().fullDocument!!

            if (modelConfig["_id"] != user) return@get

            val clazz = Model::class.java
            val m = clazz.newInstance()

            clazz.fields.forEach {
                it.isAccessible = true

                val fieldName = it.getAnnotation(RealmField::class.java)?.value ?: it.name

                it.set(m, modelConfig[fieldName])
            }

            Log.e(TAG, "model config - timestamp=${m.timestamp} ${modelConfig["timestamp"]}")

            result(m)
        }
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