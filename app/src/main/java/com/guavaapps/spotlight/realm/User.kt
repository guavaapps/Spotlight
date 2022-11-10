package com.guavaapps.spotlight.realm

import android.graphics.Bitmap
import com.pixel.spotifyapi.Objects.AlbumSimple
import com.pixel.spotifyapi.Objects.Copyright
import com.pixel.spotifyapi.Objects.LinkedTrack
import io.realm.Realm
import io.realm.RealmDictionary
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.Required
import org.bson.types.ObjectId
import java.util.*

open class User(
    @PrimaryKey
    var _id: ObjectId? = null,
) : RealmObject() {
    @Required
    var spotify_id: String? = null

    @Required
    var date_signed_up: Long? = null
    var last_login: Long? = null
    var locale: String? = Locale.UK.isO3Country

    var timeline: RealmList<TrackModel> = RealmList()
}

class TimelineTrack(
    var _id: ObjectId = ObjectId(),
) {
    var tracks: RealmList<TrackModel> = RealmList()
    var timestamps: Date = Date()
}

open class Model(
    @PrimaryKey
    var _id: ObjectId = ObjectId(),
) : RealmObject() {
    @Required
    var spotify_id: String? = null
    var model_params: RealmList<ModelParam> = RealmList()
}

open class ModelParam(
    @PrimaryKey
    var _id: ObjectId = ObjectId(),
) : RealmObject() {
    var params: RealmList<Float> = RealmList()
    var shapes: RealmList<Int> = RealmList()
}

open class Track(
    var _id: ObjectId = ObjectId(),
) : RealmObject() {
    @Required
    var id: String? = null
    var name: String? = null
    var uri: String? = null
    var type: String? = null
    var preview_url: String? = null
    var href: String? = null
    var track_number: Int? = null
    var explicit: Boolean? = null
    var duration_ms: Long? = null
    var is_playable: Boolean? = null
    var disc_number: Int? = null
    var available_markets = RealmList<String>()
    var external_urls = RealmDictionary<String>()
    var linked_from: LinkedTrack? = null
    var artists = RealmList<Artist>()

    var album: AlbumSimple? = null
    var external_ids = RealmDictionary<String>()
    var popularity: Int? = null
}

open class TrackSimple(
    var _id: ObjectId = ObjectId(),
) : RealmObject() {
    @Required
    var id: String? = null
    var name: String? = null
    var uri: String? = null
    var type: String? = null
    var preview_url: String? = null
    var href: String? = null
    var track_number: Int? = null
    var explicit: Boolean? = null
    var duration_ms: Long? = null
    var is_playable: Boolean? = null
    var disc_number: Int? = null
    var available_markets = RealmList<String>()
    var external_urls = RealmDictionary<String>()
    var linked_from: LinkedTrack? = null
    var artists = RealmList<Artist>()
}

open class AlbumSimple(var _id: ObjectId = ObjectId()) : RealmObject() {
    var album_type: String? = null
    var available_markets: List<String>? = null
    var external_urls: Map<String, String>? = null
    var href: String? = null
    var id: String? = null
    var images: List<Image>? = null
    var name: String? = null
    var type: String? = null
    var uri: String? = null
}

open class Album(
    var _id: ObjectId = ObjectId(),
) : RealmObject() {
    var album_type: String? = null
    var available_markets: List<String>? = null
    var external_urls: Map<String, String>? = null
    var href: String? = null
    var id: String? = null
    var images: List<Image>? = null
    var name: String? = null
    var type: String? = null
    var uri: String? = null

    var artists: RealmList<ArtistSimple> = RealmList()
    var copyrights: RealmList<Copyright>? = null
    var external_ids: RealmDictionary<String>? = null
    var genres: RealmList<String>? = null
    var popularity: Int? = null
    var release_date: String? = null
    var release_date_precision: String? = null
    var tracks: Pager<TrackSimple>? = null
}

open class Copyright(
    var _id: ObjectId = ObjectId(),
): RealmObject () {

}

open class Artist(
    var _id: ObjectId = ObjectId(),
) : RealmObject() {
    var external_urls = RealmDictionary<String>()
    var href: String? = null
    var id: String? = null
    var name: String? = null
    var type: String? = null
    var uri: String? = null

    var followers: Followers? = null
    var genres = RealmList<String>()
    var images = RealmList<Image>()
    var popularity: Int? = null
}

open class ArtistSimple(var _id: ObjectId = ObjectId()) : RealmObject() {
    var external_urls = RealmDictionary<String>()
    var href: String? = null
    var id: String? = null
    var name: String? = null
    var type: String? = null
    var uri: String? = null
}

open class Followers(
    var _id: ObjectId = ObjectId(),
) : RealmObject() {
    var href: String? = null
    var total: Int? = null
}

open class Image(
    var _id: ObjectId = ObjectId(),
) : RealmObject() {
    var width: Int? = null
    var height: Int? = null
    var url: String? = null
}

open class Pager<T : RealmObject>(
    var _id: ObjectId = ObjectId(),
) : RealmObject() {
    var href: String? = null
    var items: List<T>? = null
    var limit = 0
    var next: String? = null
    var offset = 0
    var previous: String? = null
    var total = 0
}

open class TrackWrapper(var _id: ObjectId = ObjectId()) : RealmObject() {
    var track: Track? = null
    var bitmap: Bitmap? = null
}

open class AlbumWrapper(var _id: ObjectId = ObjectId()) : RealmObject() {
    var album: Track? = null
    var bitmap: Bitmap? = null
}

open class ArtistWrapper(var _id: ObjectId = ObjectId()) : RealmObject() {
    var artist: Artist? = null
    var bitmap: Bitmap? = null
}

open class LinkedTrack(var _id: ObjectId = ObjectId()) : RealmObject() {
    var external_urls: Map<String, String>? = null
    var href: String? = null
    var type: String? = null
    var uri: String? = null
}

open class TrackModel(
    @PrimaryKey
    var _id: ObjectId = ObjectId(),
) : RealmObject() {
    var spotify_id: String? = null
    var track_id: String? = null
    var features: RealmList<Float> = RealmList()
    var timestamp: Long? = null
}

fun a() {
    val rObject = TrackWrapper() realmifyAs Track::class.java
}

val typeMap = mapOf(
    com.guavaapps.spotlight.TrackWrapper::class.java to TrackWrapper::class.java,
    com.guavaapps.spotlight.AlbumWrapper::class.java to AlbumWrapper::class.java,
    com.guavaapps.spotlight.ArtistWrapper::class.java to ArtistWrapper::class.java,

    com.pixel.spotifyapi.Objects.Track::class.java to Track::class.java,
    com.pixel.spotifyapi.Objects.Album::class.java to Album::class.java,
    com.pixel.spotifyapi.Objects.Artist::class.java to Artist::class.java,

    com.pixel.spotifyapi.Objects.TrackSimple::class.java to TrackSimple::class.java,
    com.pixel.spotifyapi.Objects.AlbumSimple::class.java to com.guavaapps.spotlight.realm.AlbumSimple::class.java,
    com.pixel.spotifyapi.Objects.ArtistSimple::class.java to ArtistSimple::class.java,

    com.pixel.spotifyapi.Objects.Followers::class.java to Followers::class.java,
    com.pixel.spotifyapi.Objects.Image::class.java to Image::class.java,
    LinkedTrack::class.java to com.guavaapps.spotlight.realm.LinkedTrack::class.java,
    com.pixel.spotifyapi.Objects.Pager::class.java to Pager::class.java,

    List::class.java to RealmList::class.java
)

inline infix fun <reified E, T : RealmObject> E.realmifyAs(clazz: Class<T>): T {
    val instance = clazz.getConstructor(ObjectId::class.java)
        .newInstance(ObjectId())

    val objClass = E::class.java

    for (field in clazz.fields) {
        val f = objClass.getDeclaredField(field.name)
        val v = f.get(this)

        if (field.type.isPrimitive) {
            field.set(field.name, v)
        } else {
            val c = typeMap[objClass]
//            field.set(field.name, v as c )
        }
    }

    return instance
}

infix fun <E : RealmObject, T> E.derealmifyTo(clazz: Class<T>): T {
    return Any() as T
}