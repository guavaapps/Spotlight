package com.guavaapps.spotlight

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.guavaapps.spotlight.Matcha.Companion.derealmify
import com.guavaapps.spotlight.Matcha.Companion.realmify
import com.guavaapps.spotlight.realm.RealmTrackWrapper
import com.pixel.spotifyapi.Objects.LinkedTrack
import com.pixel.spotifyapi.Objects.Track
import io.realm.*
import io.realm.annotations.RealmField
import io.realm.mongodb.User
import java.nio.ByteBuffer
import kotlin.contracts.ContractBuilder
import kotlin.contracts.contract

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
        val fields =
            E::class.java.allFields //arrayOf(*E::class.java.declaredFields, *E::class.java.fields)

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

        return realmObject.findFirstCopied()
    }

    fun <E : RealmModel> getAll(
        clazz: Class<E>,
        id: String? = null,
    ): List<E?> {
        val realmObjects = realm.where(clazz)
            .equalTo("_id", id)
            .findAllCopied()

        return realmObjects
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

                val realmObject = obj.realmify() as RealmObject// ?: return@executeTransaction

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

    fun <E : RealmModel> RealmQuery<E>.findAllCopied() = realm.copyFromRealm(this.findAll().toMutableList())

    fun <E : RealmModel> RealmQuery<E>.findFirstCopied(): E? {
        val obj = this.findFirst()

        return if (obj == null) null else realm.copyFromRealm(obj)
    }

    fun close() {
        realm.close()
    }
}

inline infix fun <reified E : RealmObject> Realm.hasAll(objects: Array<E>) =
    objects.all { this has it }

inline infix fun <reified E : RealmObject> Realm.hasAny(objects: Array<E>) =
    objects.any { this has it }

inline infix fun <reified E : RealmObject> Realm.has(obj: E): Boolean {
    val fields =
        E::class.java.allFields //arrayOf(*E::class.java.declaredFields, *E::class.java.fields)

    val id = fields.find {
        it.isAccessible = true
        it.getAnnotation(RealmField::class.java)?.value == "_id" || it.name == "_id"
    }?.get(obj) as String?
        ?: return false

    return (this.where(E::class.java)
        .equalTo("_id", id)
        .findFirst() != null)
}

fun <E : RealmModel> RealmQuery<E>.findAllCopied() = realm.copyFromRealm(this.findAll().toMutableList())

fun <E : RealmModel> RealmQuery<E>.findFirstCopied(): E? {
    val obj = this.findFirst()

    return if (obj == null) null else realm.copyFromRealm(obj)
}