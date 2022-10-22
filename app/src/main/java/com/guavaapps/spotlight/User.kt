package com.guavaapps.spotlight

import android.util.Log
import com.google.gson.Gson
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.Required
import org.bson.types.ObjectId
import java.io.InputStreamReader
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL
import java.nio.CharBuffer
import java.util.*
import java.util.concurrent.Executors

open class User(
    @PrimaryKey
    var _id: ObjectId? = null
) : RealmObject() {
    @Required
    var spotify_id: String? = null

    @Required
    var date_signed_up: Date? = null
    var last_login: Date? = null
    var model: Model? = null
}

open class Model(
    @PrimaryKey
    var _id: ObjectId? = null
) : RealmObject() {
    @Required
    var spotify_id: String? = null
}

open class Track(
    @PrimaryKey
    var _id: ObjectId?
) {
    @Required
    var id: String? = null
}