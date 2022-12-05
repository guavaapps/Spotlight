package com.guavaapps.spotlight

import android.content.Context
import android.util.Log
import androidx.lifecycle.viewmodel.viewModelFactory
import com.guavaapps.spotlight.Matcha.Companion.derealmify
import com.guavaapps.spotlight.Matcha.Companion.realmify
import com.guavaapps.spotlight.realm.RealmAlbum
import com.guavaapps.spotlight.realm.RealmLinkedTrack
import com.guavaapps.spotlight.realm.RealmTrack
import com.pixel.spotifyapi.Objects.Album
import com.pixel.spotifyapi.Objects.Track
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.RealmModel
import io.realm.RealmObject
import io.realm.annotations.RealmField
import io.realm.kotlin.where
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import javax.xml.xpath.XPathVariableResolver

private const val TAG = "LocalRealm"

class LocalRealm(context: Context) {
    var realm: Realm
        private set

    init {
        Realm.init(context)

        val config = RealmConfiguration.Builder()
            .name("Spotlight")
            .allowQueriesOnUiThread(true)
            .allowWritesOnUiThread(true)
            .build()

        realm = Realm.getInstance(config)
    }

    fun d() {
        val trackArray =
            Array(10) {
                RealmTrack().apply {
                    id = "track_id::$it"
                    linked_from = RealmLinkedTrack().apply {
                        uri = "uri"
                    }
                }
            }

        val albumArray = Array(10) { RealmAlbum().apply { id = "album_id::$it" } }

        put(trackArray[1])
        put(trackArray[2])

        val objects = arrayOf(*trackArray)//, *albumArray)

        val realmTrack = RealmTrack().apply { id = "track_id::2" }
        Log.e(TAG, "realmtrack - ${realmTrack.id}")

        val has = this has realmTrack
        val hasAll = this hasAll objects.slice(1 until 2).toTypedArray()
        val hasAny = this hasAny objects

        Log.e("LocalRealm", "has=$has hasAll=$hasAll hasAny=$hasAny")

        val r = this.get(RealmTrack::class.java, "track_id::2")
        val s = r.derealmify(resolver = ::resolveSpotifyObject) as Track

        Log.e(TAG, "r=${r!!::class.java} s=$s")
        Log.e(TAG, "id=${s.id} uri=${s.linked_from?.uri} s=$s")

        deleteAll(RealmTrack::class.java)

        val has2 = this has RealmTrack().apply { id = "track_id::2" }
        val hasAll2 = this hasAll objects
        val hasAny2 = this hasAny objects

        Log.e("LocalRealm", "has=$has2 hasAll=$hasAll2 hasAny=$hasAny2")
    }

    inline infix fun <reified E : RealmObject> hasAll(objects: Array<E>) =
        objects.all { this has it }

    inline infix fun <reified E : RealmObject> hasAny(objects: Array<E>) =
        objects.any { this has it }

    inline infix fun <reified E : RealmObject> has(obj: E): Boolean {
        val fields = arrayOf(*E::class.java.declaredFields, *E::class.java.fields)

        val id = fields.find {
            it.isAccessible = true
            it.getAnnotation(RealmField::class.java)?.value == "_id" || it.name == "_id"
        }?.get(obj) as String?
            ?: return false

//        realm.where(E::class.java)
//            .equalTo("_id", id)
//            .findAll()
//            .forEach {
//                Log.e("LocalRealm", "foreach - ${it::class.java.simpleName}")
//            }

        return (realm.where(E::class.java)
            .equalTo("_id", id)
            .findFirst() != null).also {
            Log.e("LocalRealm",
                "has() - class=${E::class.java} id=$id -> $it")
        }
    }

    fun <E : RealmObject> get(
        clazz: Class<E>,
        id: String,
    ): E {
        Log.e(TAG, "clazz hehe - $clazz")

        val realmObject = realm.where(clazz)
            .equalTo("_id", id)
            .findFirst()

        val objects = realm.where(clazz)
            .equalTo("_id", id)
            .findAll()

        Log.e(TAG, "realm hehe - ${clazz.cast(realmObject!!)::class.java}")
        Log.e(TAG, "realm hehe - ${objects!!::class.java}")

        Log.e(TAG, "realm object - o=${realmObject} id=${(realmObject as RealmTrack).id}")

        return realmObject//?.derealmify(resolver = ::resolveSpotifyObject)
    }

    fun <E : RealmObject> put(obj: E) {
        realm.executeTransaction {
            val realmObject = obj!!.realmify(resolver = ::resolveRealmObject) as RealmObject

            it.insertOrUpdate(realmObject)
        }
    }

    fun <E : RealmObject> delete(clazz: Class<E>, id: String) {
        realm.executeTransaction {
            it.where(clazz)
                .equalTo("_id", id)
                .findAll()
                .deleteAllFromRealm()
        }
    }

    fun <E : RealmObject> deleteAll(clazz: Class<E>) {
        realm.executeTransaction {
            it.where(clazz)
                .findAll()
                .deleteAllFromRealm()
        }
    }

    fun close() {
        realm.close()
    }

    private fun resolveRealmObject(clazz: Class<*>): Class<*> {
        val clazz = if (clazz.packageName == "io.realm") clazz.superclass else clazz

        return if (RealmObject::class.java.isAssignableFrom(clazz)) clazz.also {
            Log.e(TAG,
                "resolver - no change $it")
        }
        else Class.forName("com.guavaapps.spotlight.realm.Realm${clazz.simpleName}")
            .also { Log.e(TAG, "resolver - to $it") }
    }

    private fun resolveSpotifyObject(clazz: Class<*>): Class<*> {
        val clazz = if (clazz.packageName == "io.realm") clazz.superclass else clazz

        return Class.forName("com.pixel.spotifyapi.Objects.${clazz.simpleName.removePrefix("Realm")}")
    }
}