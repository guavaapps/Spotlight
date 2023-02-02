package com.guavaapps.spotlight

import android.content.Context
import android.util.Log
import com.guavaapps.spotlight.Matcha.Companion.asDocument
import com.guavaapps.spotlight.Matcha.Companion.asObject
import io.realm.*
import io.realm.annotations.Ignore
import io.realm.annotations.RealmClass
import io.realm.annotations.RealmField
import io.realm.mongodb.*
import io.realm.mongodb.App
import io.realm.mongodb.mongo.MongoClient
import io.realm.mongodb.mongo.MongoCollection
import io.realm.mongodb.mongo.MongoDatabase
import io.realm.mongodb.mongo.options.FindOneAndModifyOptions
import org.bson.Document
import java.lang.reflect.ParameterizedType
import java.util.*

private const val TAG = "Matcha"
private const val MONGODB_ATLAS = "mongodb-atlas"

class Match<E : RealmObject>(
    private val collection: MongoCollection<Document>,
    private val clazz: Class<E>,
    private val filter: Document = Document(),
) {
    private var watcher: RealmEventStreamAsyncTask<Document>? = null

    fun equalTo(field: String, value: String): Match<E> {
        filter[field] = value

        return this
    }

    fun findFirst(): E? {
        val obj = collection.findOne(filter).get()

        return obj?.asObject(clazz)
    }

    fun findFirstAsync(onResult: (E) -> Unit) {
        collection.findOne(filter).getAsync {
            if (it.isSuccess) {
                onResult(it.get().asObject(clazz) as E)
            }
        }
    }

    fun findAll(): List<E> {
        val objects = mutableListOf<E>()

        collection.find(filter)
            .iterator()
            .get()
            .forEach {
                objects.add(it.asObject(clazz) as E)
            }

        return objects
    }

    fun findAllAsync(onResult: (List<E>) -> Unit) {
        val objects = mutableListOf<E>()

        collection.find(filter)
            .iterator()
            .getAsync {
                if (it.isSuccess) {
                    while (it.get().hasNext()) {
                        objects.add(it.get().next().asObject(clazz) as E)
                    }

                    onResult(objects)
                }
            }

    }

    fun update(obj: E) {
        val document = obj.asDocument()

        collection.updateOne(filter, document).get()
    }

    fun insert(obj: E) {
        val document = obj.asDocument()

        Log.e(TAG, "[INSERT] ${document.toJson()}")

        collection.insertOne(document).getAsync {
            Log.e(TAG,
                "[INSERT] result -> ${if (it.isSuccess) "success" else "error {${it.error.message}"}")
        }
    }

    fun insertAll(vararg obj: E) {
        val documents = obj.map { it.asDocument() }

        collection.insertMany(documents)
    }

    fun upsert(obj: E) {
        val document = obj.asDocument()

        val options = FindOneAndModifyOptions().apply {
            upsert(true)
        }

        collection.findOneAndUpdate(
            filter,
            document,
            options
        ).get()
    }

    fun delete() {
        collection.deleteOne(filter)
    }

    fun deleteAll() {
        collection.deleteMany(filter)
    }

    fun stopWatching() {
        watcher?.cancel() ?: run { watcher = null }
    }

    fun watch(result: (obj: E) -> Unit) {
        var watcher: RealmEventStreamAsyncTask<Document>? = null

        if (watcher != null) return

        watcher = collection.watchAsync()
        watcher!!.get { d ->
            if (d.isSuccess) {
                var isMatch = true

                filter.forEach { f, v ->
                    isMatch = isMatch && d.get().fullDocument!![f] == v
                }

                if (!isMatch) {
                    Log.e(TAG, "not a match - id=${d.get().fullDocument!!["_id"]}")
                    return@get
                }

                result(d.get().fullDocument?.asObject(clazz) as E)
            } else {
                Log.e(TAG, "watcher error - ${d.error.message}")
            }
        }
    }
}

class Matcha(
    context: Context,
    config: AppConfiguration,
    private val database: String,
) {
    private var app: App
    private lateinit var mongoDatabase: MongoDatabase
    private lateinit var user: User
    private lateinit var client: MongoClient

    var currentUser: User? = null
        get() = app.currentUser()
        private set

    init {
        Realm.init(context)

        app = App(config)
    }

    fun login(credentials: Credentials) {
        user = app.login(credentials)
        client = user.getMongoClient(MONGODB_ATLAS)
        mongoDatabase = client.getDatabase(database)
    }

    fun logout() {
        app.currentUser()?.logOut()
    }

    // initiate query for that match objects of the given class
    fun <E : RealmObject> where(clazz: Class<E>): Match<E> {
        // use class name as collection name or the explicit specified
        // collection name if one was provided by annotating the class
        val name = clazz.getAnnotation(RealmClass::class.java)
            ?.value
            ?.ifEmpty { clazz.simpleName }

        val collection = mongoDatabase.getCollection(name)

        // return a match with an empty filter
        return Match(collection, clazz)
    }

    companion object {
        fun init(
            context: Context,
            app: String,
            database: String,
        ) = Matcha(
            context,
            AppConfiguration.Builder(app)
                .build(),
            database
        )

        // convert bson doc to a java object by matching the doc's field name with
        // the object's field names
        fun <E> Document.asObject(clazz: Class<E>): E {
            val obj = clazz.newInstance() as E

            clazz.declaredFields.forEach {
                it.isAccessible = true
                val fieldName = it.getAnnotation(RealmField::class.java)?.value ?: it.name

                if (isPrimitive(it.type)) {
                    it.set(obj, this.get(fieldName, it.type))

                } else if (List::class.java.isAssignableFrom(it.type)) {
                    val fieldClass =
                        (it.genericType as ParameterizedType).actualTypeArguments.first() as Class<*>


                    if (isPrimitive(fieldClass)) {
                        val objects = (this.get(fieldName) as List<*>).map {
                            if (it != null) {
                                val valueClass = it::class.java

                                val methodName = if (fieldClass.simpleName == "Integer") "int"
                                else fieldClass.simpleName.lowercase()

                                val converter = valueClass.getMethod("${methodName}Value")

                                val convertedValue = converter.invoke(it)

                                return@map convertedValue
                            }

                            return@map null
                        }

                        it.set(obj, RealmList(*objects.toTypedArray()))
                    } else {
                        val objects = this.getList(fieldName, Document::class.java)
                            .map { o: Document -> o.asObject(fieldClass) }

                        it.set(obj, RealmList(*objects.toTypedArray()))
                    }
                } else {
                    it.set(obj, this.get(fieldName, Document::class.java).asObject(it.type))
                }
            }

            return obj
        }

        // opposite of Any.asObject()
        fun Any.asDocument(): Document {
            val clazz = this::class.java
            val obj = Document()

            clazz.declaredFields.forEach {
                it.isAccessible = true

                if (it.isAnnotationPresent(Ignore::class.java)) return@forEach

                val fieldName = it.getAnnotation(RealmField::class.java)?.value ?: it.name
                val v = it.get(this)

                if (isPrimitive(it.type)) {
                    obj[fieldName] = v
                } else if (List::class.java.isAssignableFrom(it.type)) {
                    val fieldClass =
                        (it.genericType as ParameterizedType).actualTypeArguments.first() as Class<*>

                    if (isPrimitive(fieldClass)) {
                        val objects = (v as List<*>)
                        obj[fieldName] = objects
                    } else {
                        val objects = (v as List<*>).map { it?.asDocument() }
                        obj[fieldName] = objects
                    }
                } else {
                    it.set(obj, v?.asDocument())
                }
            }

            return obj
        }

        fun Any.realmify(
            realmClass: Class<*>? = null,
            resolver: (Class<*>) -> Class<*> = ::resolveRealmObject,
        ): Any {
            val c = this::class.java
            val realmClass = realmClass ?: resolver(c)
            val obj = realmClass.newInstance()!!

            c.allFields.forEach {
                it.isAccessible = true

                realmClass.allFields.find { r ->
                    r.name == it.name
                } ?: return@forEach
                val v = it.get(this) ?: return@forEach

                val r = realmClass.getDeclaredField(it.name)
                r.isAccessible = true

                val isBoolean = it.type.simpleName == "Boolean"

                if (isPrimitive(it.type) || isBoolean) {
                    r.set(obj, v)
                } else if (List::class.java.isAssignableFrom(it.type)) {
                    r.apply {
                        isAccessible = true

                        val list = (v as List<*>).map {
//                            if (it == null) return@map

                            val listClass = it!!::class.java

                            if (isPrimitive(listClass)) {
                                it
                            } else {
                                it.realmify(resolver = resolver)
                            }
                        }

                        set(obj, RealmList(*list.toTypedArray()))
                    }
                }
                // is map
                else if (Map::class.java.isAssignableFrom(it.type)) {
                    r.set(obj, RealmDictionary(v as Map<String, *>))
                } else {
                    val f = if (it in c.declaredFields) {
                        c.getDeclaredField(it.name)
                    } else {
                        c.superclass.getDeclaredField(it.name)
                    }

                    if (v::class.java.simpleName == "Pager") {
                        val pagerClass = v::class.java

                        val enc = pagerClass.enclosingClass

                        f.toString()
                    }

                    if (v::class.java.simpleName == "Pager") {
                        var pagerClass = if (f.declaringClass.simpleName == "Playlist") {
                            Class.forName("com.guavaapps.spotlight.realm.RealmPlaylistTrackPager")
                        } else {
                            Class.forName("com.guavaapps.spotlight.realm.RealmTrackSimplePager")
                        }

                        r.set(obj, v.realmify(pagerClass, resolver))

                        return@forEach
                    }

                    r.set(obj, v.realmify(resolver(v::class.java), resolver))
                }
            }

            return obj
        }

        fun Any.derealmify(
            clazz: Class<*>? = null,
            resolver: (Class<*>) -> Class<*> = ::resolveSpotifyObject,
        ): Any {
            val c = this::class.java//resolveProxy(this::class.java)
            val clazz = clazz ?: resolver(c)
            val obj = clazz.newInstance()!!

            c.allFields.forEach {
                it.isAccessible = true
//                clazz.allFields.find { r -> r.name == it.name } ?: return@forEach

                val v = it.get(this) ?: return@forEach

                val r = clazz.allFields.find { field -> field.name == it.name } ?: return@forEach
                r.isAccessible = true

                val isBoolean = it.type.simpleName == "Boolean"

                if (isPrimitive(it.type) || isBoolean) {
                    r.set(obj, v)
                } else if (RealmList::class.java.isAssignableFrom(it.type)) {
                    r.apply {
                        isAccessible = true
                        set(obj, (v as RealmList<*>).map {
                            val listClass = it::class.java

                            if (isPrimitive(listClass)) {
                                it
                            } else {
                                it.derealmify(resolver = resolver)
                            }
                        })
                    }
                }
                // is map
                else if (RealmMap::class.java.isAssignableFrom(it.type)) {
                    r.set(obj, (v as RealmDictionary<*>).map {
                        if (isPrimitive(it.value::class.java)) {
                            it.toPair()
                        } else {
                            it.key to it.value.derealmify(resolver = resolver)
                        }
                    }.toMap())
                } else {
                    val f = if (it in c.declaredFields) {
                        c.getDeclaredField(it.name)
                    } else {
                        c.superclass.getDeclaredField(it.name)
                    }

                    Log.e(TAG, "f - ${f.name}")
                    Log.e(TAG, "v - ${v::class.java.simpleName}")

                    if (v::class.java.simpleName.endsWith("Pager")) {
                        //val pagerClass = if (f.declaringClass.simpleName == "RealmPlaylist") ""
                        val pagerClass = Class.forName("com.pixel.spotifyapi.Objects.Pager")

                        r.set(obj, v.derealmify(pagerClass, resolver))

                        return@forEach
                    }

                    r.set(obj, v.derealmify(resolver(v::class.java), resolver))
                }
            }

            return obj
        }
    }
}

val Class<*>.allFields
    get() = setOf(*this.fields, *this.declaredFields)

// check if a given class is a bson primitive type, primitive if class is:
// if it.isPrimitive() is true,
// is boolean, string, kotlin.Number or java.util.Date
private fun isPrimitive(clazz: Class<*>) =
    clazz.isPrimitive || Boolean::class.java.isAssignableFrom(clazz) || Date::class.java.isAssignableFrom(
        clazz) || Number::class.java.isAssignableFrom(
        clazz) || String::class.java.isAssignableFrom(clazz)

// get the corresponding realm clazz for the given Spotify object class
// e.g. Track -> RealmTrack, Artist -> RealmArtist
private fun resolveRealmObject(clazz: Class<*>): Class<*> {
    // RealmObjectRealmProxy classes inherit the realm object
    // autogenerated in io.realm.[class name]
    // treat as superclass (RealmObject)
    val clazz = if (clazz.packageName == "io.realm") clazz.superclass else clazz

    // is a realm object was passed return it
    return if (RealmObject::class.java.isAssignableFrom(clazz)) clazz

    // if the class is an item pager (Pager<ItemClass>) determine what type of pager it is
    // since realm doesn't support generic class params use item-specific realm pagers
    // only Pager<TrackSimple> (RealmTrackSimplePager) and Pager<PlaylistTrack> (RealmPlaylistTrackPager) are ever used
    else if (clazz.simpleName == "Pager") {
        val t = if (clazz.enclosingClass.simpleName == "RealmAlbum") {
            "TrackSimple"
        } else {
            "PlaylistTrack"
        }

        Class.forName("com.guavaapps.spotlight.realm.Realm${t}${clazz.simpleName}")
    } else {
        Class.forName("com.guavaapps.spotlight.realm.Realm${clazz.simpleName}")
            .also { Log.e(TAG, "resolver - to $it") }
    }
}

// opposite of resolveRealmObject()
private fun resolveSpotifyObject(clazz: Class<*>): Class<*> {
    val clazz = if (clazz.packageName == "io.realm") clazz.superclass else clazz

    return Class.forName("com.pixel.spotifyapi.Objects.${clazz.simpleName.removePrefix("Realm")}")
}