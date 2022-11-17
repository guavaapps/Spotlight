package com.guavaapps.spotlight

import android.content.Context
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import com.guavaapps.spotlight.realm.*
import com.pixel.spotifyapi.Objects.*
import io.realm.*
import io.realm.annotations.RealmClass
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
import java.lang.Exception
import java.lang.reflect.Type
import java.net.HttpURLConnection
import java.net.URL
import java.security.spec.ECField
import java.lang.Class as Class

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
    mongoClient: MongoClient,
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
    private val remoteModelDataSource: RemoteModelDataSource,
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

    fun testPut () {
        val obj = Track ()

        put(obj) {o -> o.realmify(supportedRealmTypes.first { it.simpleName == "Realm${o::class.java.simpleName}" }) }
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
        fun test() {
            val track = Track().apply {
                id = "fksdjflksdj"
                name = "track_name"
                linked_from = LinkedTrack().apply {
                    id = ""
                    href = ""
                    type = ""
                    uri = ""
                    external_urls = mutableMapOf("" to "")
                }
                artists = listOf(
                    ArtistSimple().apply {
                        name = "artist_1"
                    },
                    ArtistSimple().apply {
                        name = "artist_2"
                    }
                )
            }

            val realmTrack = track.realmify(RealmTrack::class.java)

            for (f in realmTrack::class.java.fields) {
                Log.e(TAG, "${f.name}=${f.get(realmTrack)}")
            }
        }
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

    val supportedRealmTypes = listOf(
        RealmTrack::class.java,
        RealmTrackSimple::class.java,
        RealmAlbumSimple::class.java,
        RealmArtistSimple::class.java,
        RealmArtist::class.java,
        RealmAlbum::class.java,
        RealmLinkedTrack::class.java,
        RealmImage::class.java,
        RealmPager::class.java,
        RealmCopyright::class.java,
        RealmFollowers::class.java
    )

    val supportedTypes = listOf(
        Track::class.java,
        TrackSimple::class.java,
        AlbumSimple::class.java,
        ArtistSimple::class.java,
        Artist::class.java,
        Album::class.java,
        LinkedTrack::class.java,
        Image::class.java,
        Pager::class.java,
        Copyright::class.java,
        Followers::class.java
    )

    fun testPutTrack() {
        val track = Track()
        val album = Album()

        put(track, this::realmifyTrack)

        val a = get(RealmTrack::class.java, track.id)// { it as Track }
    }

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

val supportedRealmTypes = listOf(
    RealmTrack::class.java,
    RealmTrackSimple::class.java,
    RealmAlbumSimple::class.java,
    RealmArtistSimple::class.java,
    RealmArtist::class.java,
    RealmAlbum::class.java,
    RealmLinkedTrack::class.java,
    RealmImage::class.java,
    RealmPager::class.java,
    RealmCopyright::class.java,
    RealmFollowers::class.java
)

fun <E> Any.realmify(realmClass: Class<E>): E {
    val c = this::class.java
    val obj = realmClass.newInstance()

    c.fields.forEach {
        if (it.name == "CREATOR") return@forEach

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
            }
            else {
                c.superclass.getDeclaredField(it.name)
            }

            r.set(obj,
                v.realmify(
                    supportedRealmTypes.first { clazz ->
                        clazz.simpleName == "Realm${f.type.simpleName}"
                    }
                )
            )
        }
    }

    return obj
}

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

        val jsonConfig = Gson().toJson(
            object {
                var user_id = userId
                var action = "get"
                var look_back = 1
                var epochs = 2
            }
        )

        val outputStream = conn.outputStream

        outputStream.write(jsonConfig.toByteArray())
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