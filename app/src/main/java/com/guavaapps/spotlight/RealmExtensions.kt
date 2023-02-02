package com.guavaapps.spotlight

import io.realm.Realm
import io.realm.RealmModel
import io.realm.RealmObject
import io.realm.RealmQuery
import io.realm.annotations.RealmField

inline infix fun <reified E : RealmObject> Realm.hasAll(objects: Array<E>) =
    objects.all { this has it }

inline infix fun <reified E : RealmObject> Realm.hasAny(objects: Array<E>) =
    objects.any { this has it }

inline infix fun <reified E : RealmObject> Realm.has(obj: E): Boolean {
    val fields =
        E::class.java.allFields

    val id = fields.find {
        it.isAccessible = true
        it.getAnnotation(RealmField::class.java)?.value == "_id" || it.name == "_id"
    }?.get(obj) as String?
        ?: return false

    return (this.where(E::class.java)
        .equalTo("_id", id)
        .findFirst() != null)
}

// realm returns an object proxies as a result of a query
// get the unmanaged realm objects
fun <E : RealmModel> RealmQuery<E>.findAllCopied() = realm.copyFromRealm(this.findAll().toMutableList())

fun <E : RealmModel> RealmQuery<E>.findFirstCopied(): E? {
    val obj = this.findFirst()

    return if (obj == null) null else realm.copyFromRealm(obj)
}