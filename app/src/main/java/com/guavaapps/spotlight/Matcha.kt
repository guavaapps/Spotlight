package com.guavaapps.spotlight

import android.content.Context
import android.util.Log
import com.google.gson.reflect.TypeToken
import com.guavaapps.spotlight.Matcha.Companion.asMatchaObject
import com.guavaapps.spotlight.Matcha.Companion.fromMatchaObject
import com.guavaapps.spotlight.Matcha.Companion.realmify
import io.realm.*
import io.realm.annotations.Ignore
import io.realm.annotations.RealmField
import io.realm.mongodb.*
import io.realm.mongodb.mongo.MongoClient
import io.realm.mongodb.mongo.MongoCollection
import io.realm.mongodb.mongo.MongoDatabase
import io.realm.mongodb.mongo.iterable.MongoCursor
import org.bson.Document
import java.lang.reflect.ParameterizedType
import javax.security.auth.login.LoginException
import kotlin.reflect.KVisibility

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

    fun findFirst(): E {
//        val clazz = object : TypeToken<E>() {}.rawType

        val d = mutableListOf<Document>()

        return collection.findOne(filter).get().asMatchaObject(clazz) as E
    }

    fun findFirstAsync(onResult: (E) -> Unit) {
//        val clazz = object : TypeToken<E>() {}.rawType

        val d = mutableListOf<Document>()

        collection.findOne(filter).getAsync {
            if (it.isSuccess) {
                onResult(it.get().asMatchaObject(clazz) as E)
            }
        }
    }

    fun findAll(): List<E> {
        val objects = mutableListOf<E>()

//        val clazz = object : TypeToken<E>() {}.rawType

        collection.find(filter)
            .iterator()
            .get()
            .forEach {
                objects.add(it.asMatchaObject(clazz) as E)
            }

        return objects
    }

    fun findAllAsync(onResult: (List<E>) -> Unit) {
        val objects = mutableListOf<E>()

//        val clazz = object : TypeToken<E>() {}.rawType

        collection.find(filter)
            .iterator()
            .getAsync {
                if (it.isSuccess) {
                    while (it.get().hasNext()) {
                        objects.add(it.get().next().asMatchaObject(clazz) as E)
                    }

                    onResult(objects)
                }
            }

    }

    fun update(obj: E) {
        val document = obj.fromMatchaObject()

        collection.updateOne(filter, document)
    }

    fun insert(obj: E) {
        val document = obj.fromMatchaObject()

        collection.insertOne(document)
    }

    fun insertAll(vararg obj: E) {
        val documents = obj.map { it.fromMatchaObject() }

        collection.insertMany(documents)
    }

    fun upsert(obj: E) {
        val document = obj.fromMatchaObject()

        collection.findOneAndReplace(filter, document)
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

                result(d.get().fullDocument?.asMatchaObject(clazz) as E)
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

    init {
        Realm.init(context)//clicky clack clack cliciki  i click tap love cilimy tzp you

        app = App(config)
    }

    fun login(credentials: Credentials) {
        user = app.login(credentials)
        client = user.getMongoClient("mongodb-atlas")
        mongoDatabase = client.getDatabase(database)
    }

    fun logout() {
        app.currentUser()?.logOut()
    }

    fun <E : RealmObject> where(clazz: Class<E>): Match<E> {
        val collection = mongoDatabase.getCollection(clazz.simpleName)

        return Match(collection, clazz)
    }

    companion object {
        fun init(
            context: Context,
            app: String,
            database: String,
        ): Matcha {
            val matcha = Matcha(
                context,
                AppConfiguration.Builder(app)
                    .build(),
                database
            )

            return matcha
        }

        fun <E> Document.asMatchaObject(clazz: Class<E>): E {
            val obj = clazz.newInstance() as E

            val allFields = arrayOf(
                *clazz.fields,
                *clazz.declaredFields
            )

            clazz.declaredFields.forEach {
                if (it.type.isPrimitive || String::class.java.isAssignableFrom(it.type)) {
                    it.isAccessible = true
                    val fieldName = it.getAnnotation(RealmField::class.java)?.value ?: it.name
                    it.set(obj, this.get(fieldName, it.type))
                }
            }

            return obj
        }

        fun <E : RealmObject> E.fromMatchaObject(): Document {
            val clazz = this::class.java
            val obj = Document()

            clazz.fields.forEach {
                it.isAccessible = true

                if (it.isAnnotationPresent(Ignore::class.java)) return@forEach

                val fieldName = it.getAnnotation(RealmField::class.java)?.name ?: it.name
                val v = it.get(this)

                obj[fieldName] = v
            }

            return obj
        }

        fun Any.realmify(
            realmClass: Class<*>? = null,
            resolver: (Class<*>) -> Class<*>,
        ): Any {

            val c = this::class.java
            val realmClass = realmClass ?: resolver(c)
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

                    r.set(obj, v.realmify(resolver(v::class.java)))
                }
            }

            return obj
        }

        fun Any.derealmify(
            clazz: Class<*>? = null,
            resolver: (Class<*>) -> Class<*>,
        ): Any {

            val c = this::class.java
            val clazz = clazz ?: resolver(c)
            val obj = clazz.newInstance()!!

            c.fields.forEach {
                clazz.declaredFields.find { r -> r.name == it.name } ?: return@forEach

                val v = it.get(this) ?: return@forEach

                Log.e(TAG, "${it.name} - isPrimitive=${it.type.isPrimitive}")

                val r = clazz.getDeclaredField(it.name)
                r.isAccessible = true

                if (it.type.isPrimitive || String::class.java.isAssignableFrom(it.type)) {
                    Log.e(TAG, "primitive or string")

                    r.set(obj, v)
                } else if (RealmList::class.java.isAssignableFrom(it.type)) {
                    Log.e(TAG, "list")

                    r.apply {
                        isAccessible = true
                        set(obj, (v as RealmList<*>).toList())
                    }
                }
                // is map
                else if (RealmMap::class.java.isAssignableFrom(it.type)) {
                    Log.e(TAG, "map")
                    r.set(obj, (v as RealmDictionary<*>).toMap())
                } else {
                    Log.e(TAG, "realmifiable")

                    val f = if (it in c.declaredFields) {
                        c.getDeclaredField(it.name)
                    } else {
                        c.superclass.getDeclaredField(it.name)
                    }

                    r.set(obj, v.realmify(resolver(v::class.java)))
                }
            }

            return obj
        }
    }

    interface Resolver {
        fun resolveClass(clazz: Class<*>): Class<*>
    }

    private object RealmResolver : Resolver {
        override fun resolveClass(clazz: Class<*>) =
            Class.forName("com.guavaapps.spotlight.realm.Realm${clazz.simpleName}")

        fun resolveRealmList(clazz: Class<*>) {}
    }
}

object SpotifyRealmifier {
    fun Any.toRealm(): Any {
        return this.realmify { Class.forName("Realm${this::class.java.simpleName}") }
    }

    fun <E : RealmObject> E.fromRealm(): Any {
        return this.realmify { Class.forName(this::class.java.simpleName.removePrefix("Realm")) }
    }
}