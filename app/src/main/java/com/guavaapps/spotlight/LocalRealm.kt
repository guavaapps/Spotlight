package com.guavaapps.spotlight

import android.content.Context
import com.guavaapps.spotlight.Matcha.Companion.derealmify
import com.guavaapps.spotlight.Matcha.Companion.realmify
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.RealmObject

class LocalRealm(context: Context) {
    private var realm: Realm? = null

    init {
        Realm.init(context)

        val config = RealmConfiguration.Builder()
            .name("Spotlight")
            .build()

//        realm = Realm.getInstance(config)
    }

    fun <E : RealmObject> get(
        clazz: Class<E>,
        id: String,
    ): Any? {
        val realmObject = realm!!.where(clazz)
            .equalTo("_id", id)
            .findFirst()
            ?.derealmify(resolver = ::resolveSpotifyObject)

        return realmObject
    }

    fun <E> put(obj: E) {
        realm!!.executeTransaction {
            val realmObject = obj!!.realmify(resolver = ::resolveRealm) as RealmObject

            it.insertOrUpdate(realmObject)
        }
    }

    fun close() {
        realm!!.close()
    }

    private fun resolveRealm(clazz: Class<*>): Class<*> {
        return if (RealmObject::class.java.isAssignableFrom(clazz)) clazz
        else Class.forName("com.guavaapps.spotlight.realm.Realm${clazz.simpleName}")
    }

    private fun resolveSpotifyObject(clazz: Class<*>): Class<*> {
        return Class.forName("com.pixel.spotifyapi.Objects.${clazz.simpleName.removePrefix("Realm")}")
    }

//    fun <E> Any.mapTo(clazz: Class<E>, supportedTypes: List<Class<*>>) {
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
//                        supportedTypes.first { clazz -> clazz.name == "Realm${c.name}" }
//                    )
//                )
//            }
//        }
//    }
//
//    private fun drealm(r: RealmTrack?): Track? {
//        r ?: return null
//
//        return Track().apply {
//            id = r?.id
//        }
//    }
//
//    private fun realmifyTrack(track: Track) = RealmTrack().apply {
//        id = track.id
//        name = track.name
//    }
}