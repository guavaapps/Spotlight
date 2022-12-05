package com.guavaapps.spotlight

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.guavaapps.spotlight.Matcha.Companion.asMatchaObject
import com.guavaapps.spotlight.Matcha.Companion.fromMatchaObject
import com.guavaapps.spotlight.Matcha.Companion.realmify
import com.guavaapps.spotlight.realm.RealmTrack
import io.realm.*
import io.realm.annotations.Ignore
import io.realm.annotations.RealmClass
import io.realm.annotations.RealmField
import io.realm.mongodb.*
import io.realm.mongodb.mongo.MongoClient
import io.realm.mongodb.mongo.MongoCollection
import io.realm.mongodb.mongo.MongoDatabase
import org.bson.Document
import java.lang.annotation.Inherited
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.reflect.ParameterizedType
import java.util.*

private const val TAG = "Matcha"
private const val MONGODB_ATLAS = "mongodb-atlas"

@Retention(RetentionPolicy.RUNTIME)
@Target(AnnotationTarget.CLASS)
@Inherited
annotation class MatchaClass(val name: String = "")

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

        return obj?.asMatchaObject(clazz)
    }

    fun findFirstAsync(onResult: (E) -> Unit) {
        collection.findOne(filter).getAsync {
            if (it.isSuccess) {
                onResult(it.get().asMatchaObject(clazz) as E)
            }
        }
    }

    fun findAll(): List<E> {
        val objects = mutableListOf<E>()

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

    var currentUser: User? = null
        get() = app.currentUser()
        private set

    var users: Array<User>? = null
        get() = app.allUsers().values.toTypedArray()
        private set

    fun getAllUsers(): MutableMap<String, User>? {
        return app.allUsers()
    }

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
//        val name = clazz.getAnnotation(MatchaClass::class.java)?.name ?: clazz.simpleName
        val name = clazz.getAnnotation(RealmClass::class.java)?.value ?: clazz.simpleName

        val collection = mongoDatabase.getCollection(name)

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

        private fun isPrimitive(clazz: Class<*>) =
            clazz.isPrimitive || Date::class.java.isAssignableFrom(clazz) || Number::class.java.isAssignableFrom(
                clazz) || String::class.java.isAssignableFrom(clazz)

        fun <E> Document.asMatchaObject(clazz: Class<E>): E {
            val obj = clazz.newInstance() as E

            Log.e(TAG, "document - $this")

            clazz.declaredFields.forEach {
                it.isAccessible = true
                val fieldName = it.getAnnotation(RealmField::class.java)?.value ?: it.name

                if (isPrimitive(it.type)) {
                    it.set(obj, this.get(fieldName, it.type))
                } else if (List::class.java.isAssignableFrom(it.type)) {
                    val fieldClass =
                        (it.genericType as ParameterizedType).actualTypeArguments.first() as Class<*>

                    Log.e(TAG, "fieldName=$fieldName fieldClass=$fieldClass")

                    if (isPrimitive(fieldClass)) {
                        val objects = this.get(fieldName) as List<*>

                        it.set(obj, RealmList(*objects.toTypedArray()))
                    } else {
                        val objects = this.getList(fieldName, Document::class.java)
                            .map { o: Document -> o.asMatchaObject(fieldClass) }

                        it.set(obj, RealmList(*objects.toTypedArray()))
                    }
                } else {
                    it.set(obj, this.get(fieldName, Document::class.java).asMatchaObject(it.type))
                }
            }

            return obj
        }

        fun Any.fromMatchaObject(): Document {
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
                        val objects = (v as List<*>).map { it?.fromMatchaObject() }
                        obj[fieldName] = objects
                    }
                } else {
                    it.set(obj, v?.fromMatchaObject())
                }
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

            c.allFields.forEach {
                it.isAccessible = true

                realmClass.allFields.find { r -> r.name == it.name } ?: return@forEach
                val v = it.get(this) ?: return@forEach

                val r = realmClass.getDeclaredField(it.name)
                r.isAccessible = true

                if (isPrimitive(it.type)) {
                    Log.e(TAG, "[primitive] ${it.name} -> $v")

                    r.set(obj, v)
                } else if (List::class.java.isAssignableFrom(it.type)) {
                    Log.e(TAG, "[list] ${it.name}")

                    r.apply {
                        isAccessible = true
                        set(obj, RealmList(*(v as MutableList<*>).toTypedArray()))
                    }
                }
                // is map
                else if (Map::class.java.isAssignableFrom(it.type)) {
                    Log.e(TAG, "[map] ${it.name}")
                    r.set(obj, RealmDictionary(v as Map<String, *>))
                } else {
                    Log.e(TAG, "[object] ${it.name} -> {realmifiable}")

                    val f = if (it in c.declaredFields) {
                        c.getDeclaredField(it.name)
                    } else {
                        c.superclass.getDeclaredField(it.name)
                    }

                    r.set(obj, v.realmify(resolver(v::class.java), resolver))
                }
            }

            return obj
        }

        fun Any.derealmify(
            clazz: Class<*>? = null,
            resolver: (Class<*>) -> Class<*>,
        ): Any {
            val c = resolveProxy(this::class.java)
            val o = c.cast(this as RealmObject)
            val clazz = clazz ?: resolver(c)
            val obj = clazz.newInstance()!!

            Log.e(TAG, "from=${o::class.java.simpleName}")

            Log.e(TAG,
                "derealmifier - from=${c.simpleName}(${c.allFields.size}) to=${clazz.simpleName}(${clazz.allFields.size})")

            c.allFields.forEach {
                it.isAccessible = true
//                clazz.allFields.find { r -> r.name == it.name } ?: return@forEach

                Log.e(TAG, "[looking at] ${it.name}: ${it.type.simpleName} -> {${it.get(o)}}")

                val v = it.get(this) ?: return@forEach

                val r = clazz.getDeclaredField(it.name)
                r.isAccessible = true

                if (isPrimitive(it.type)) {
                    Log.e(TAG, "[primitive] ${it.name} -> $v")

                    r.set(obj, v)
                } else if (RealmList::class.java.isAssignableFrom(it.type)) {
                    Log.e(TAG, "[list] ${it.name}}")

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
                    Log.e(TAG, "[map] ${it.name}")

                    r.set(obj, (v as RealmDictionary<*>).map {
                        if (isPrimitive(it.value::class.java)) {
                            it
                        } else {
                            it.key to it.value.derealmify(resolver = resolver)
                        }
                    })
                } else {
                    Log.e(TAG, "[object] ${it.name} -> {realmifiable}")

                    val f = if (it in c.declaredFields) {
                        c.getDeclaredField(it.name)
                    } else {
                        c.superclass.getDeclaredField(it.name)
                    }

                    r.set(obj, v.realmify(resolver(v::class.java), resolver))
                }
            }

            return obj
        }

        private fun resolveProxy (clazz: Class<*>): Class<*> {
            return if (clazz.packageName == "io.realm") clazz.superclass else clazz
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

val Class<*>.allFields
    get() = setOf(*this.fields, *this.declaredFields)