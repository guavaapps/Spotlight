package com.guavaapps.spotlight

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.guavaapps.spotlight.Matcha.Companion.derealmify
import com.guavaapps.spotlight.Matcha.Companion.realmify
import com.guavaapps.spotlight.realm.RealmTrackWrapper
import com.pixel.spotifyapi.Objects.LinkedTrack
import com.pixel.spotifyapi.Objects.Track
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.RealmModel
import io.realm.RealmObject
import io.realm.annotations.RealmField
import java.nio.ByteBuffer

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
            .deleteRealmIfMigrationNeeded()
            .build()

        realm = Realm.getInstance(config)
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

        return (realm.where(E::class.java)
            .equalTo("_id", id)
            .findFirst() != null)
    }

    fun <E : RealmModel> get(
        clazz: Class<E>,
        id: String,
    ): E? {
        val realmObject = realm.where(clazz)
            .equalTo("_id", id)
            .findFirst()

        return realm.copyFromRealm(realmObject)
    }

    fun <E : RealmObject> put(obj: E) {
        realm.executeTransaction {
            it.insertOrUpdate(obj)
        }
    }

    fun put(obj: Any) {
        realm.executeTransaction {
            if (RealmObject::class.java.isAssignableFrom(obj::class.java)) {
                it.insertOrUpdate(obj as RealmObject)
            } else {
                val clazz = realm.configuration.realmObjectClasses.find {
                    it.getAnnotation(MatchWith::class.java)?.classes?.contains(obj::class) ?: false
                }

                val realmObject = obj.realmify(clazz,
                    ::resolveRealmObject) as RealmModel// ?: return@executeTransaction

                it.insertOrUpdate(realmObject)
            }
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

    companion object {
        fun d(localRealm: LocalRealm) = with(localRealm) {
            deleteAll(RealmTrackWrapper::class.java)

            val b = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
            val p = ByteBuffer.allocate(b.height * b.rowBytes)
            p.put(0, 69)
            b.copyPixelsFromBuffer(p)

            val w = TrackWrapper(
                Track().apply {
                    id = "track_id"
                    linked_from = LinkedTrack().apply { uri = "linked_uri" }
                },
                b
            )

            put(w)

            val r = get(RealmTrackWrapper::class.java, "track_id")
            val s = r?.derealmify(resolver = ::resolveSpotifyObject) as TrackWrapper

            Log.e(TAG, "s=${s.track.id} ${s.track.linked_from.uri}")
        }
    }
}