package com.guavaapps.spotlight.realm

import android.graphics.Bitmap
import io.realm.*
import io.realm.annotations.PrimaryKey
import io.realm.annotations.Required
import org.bson.types.ObjectId
import java.nio.ByteBuffer
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

open class RealmTrack(
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
    var linked_from: RealmLinkedTrack? = null
    var artists = RealmList<RealmArtistSimple>()

    var album: RealmAlbumSimple? = null
    var external_ids = RealmDictionary<String>()
    var popularity: Int? = null
}

open class RealmTrackSimple(
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
    var linked_from: RealmLinkedTrack? = null
    var artists = RealmList<RealmArtist>()
}

open class RealmAlbumSimple(var _id: ObjectId = ObjectId()) : RealmObject() {
    var album_type: String? = null
    var available_markets: RealmList<String>? = null
    var external_urls: RealmDictionary<String>? = null
    var href: String? = null
    var id: String? = null
    var images: RealmList<RealmImage>? = null
    var name: String? = null
    var type: String? = null
    var uri: String? = null
}

open class RealmBitmap(
    var _id: ObjectId = ObjectId(),
) : RealmObject() {
    var bytes: ByteArray? = null
    var width: Int? = null
    var height: Int? = null
    var config: String? = null
}

fun toRealmBitmap(bitmap: Bitmap) {
    val b = ByteBuffer.allocate(bitmap.rowBytes * bitmap.height)
    bitmap.copyPixelsToBuffer(b)

    val realmBitmap = RealmBitmap().apply {
        bytes = b.array()
        width = bitmap.width
        height = bitmap.height
        config = bitmap.config.name
    }
}

fun ky(realmBitmap: RealmBitmap) {
    val bitmap = Bitmap.createBitmap(
        realmBitmap.width!!,
        realmBitmap.height!!,
        Bitmap.Config.valueOf(realmBitmap.config!!)
    )

    bitmap.copyPixelsToBuffer(
        ByteBuffer.wrap(realmBitmap.bytes)
    )
}

open class RealmAlbum(
    var _id: ObjectId = ObjectId(),
) : RealmObject() {
    var album_type: String? = null
    var available_markets: RealmList<String>? = null
    var external_urls: RealmDictionary<String>? = null
    var href: String? = null
    var id: String? = null
    var images: RealmList<RealmImage>? = null
    var name: String? = null
    var type: String? = null
    var uri: String? = null

    var artists: RealmList<RealmArtistSimple> = RealmList()
    var copyrights: RealmList<RealmCopyright>? = null
    var external_ids: RealmDictionary<String>? = null
    var genres: RealmList<String>? = null
    var popularity: Int? = null
    var release_date: String? = null
    var release_date_precision: String? = null
    var tracks: RealmPager? = null
}


open class RealmCopyright(
    var _id: ObjectId = ObjectId(),
) : RealmObject() {
    var text: String? = null
    var type: String? = null
}

open class RealmArtist(
    var _id: ObjectId = ObjectId(),
) : RealmObject() {
    var external_urls = RealmDictionary<String>()
    var href: String? = null
    @Required
    @PrimaryKey
    var id: String? = null
    var name: String? = null
    var type: String? = null
    var uri: String? = null

    var followers: RealmFollowers? = null
    var genres = RealmList<String>()
    var images = RealmList<RealmImage>()
    var popularity: Int? = null
}

open class RealmArtistSimple(var _id: ObjectId = ObjectId()) : RealmObject() {
    var external_urls = RealmDictionary<String>()
    var href: String? = null
    var id: String? = null
    var name: String? = null
    var type: String? = null
    var uri: String? = null
}

open class RealmFollowers(
    var _id: ObjectId = ObjectId(),
) : RealmObject() {
    var href: String? = null
    var total: Int? = null
}

open class RealmImage(
    var _id: ObjectId = ObjectId(),
) : RealmObject() {
    var width: Int? = null
    var height: Int? = null
    var url: String? = null
}

open class RealmPager(
    @PrimaryKey
    var _id: ObjectId = ObjectId(),
) : RealmObject() {
    var href: String? = null
    var items: RealmList<RealmAny> = RealmList()
    var limit = 0
    var next: String? = null
    var offset = 0
    var previous: String? = null
    var total = 0
}

open class RealmTrackWrapper(var _id: ObjectId = ObjectId()) : RealmObject() {
    var track: RealmTrack? = null
    var bitmap: RealmBitmap? = null
}

open class RealmAlbumWrapper(var _id: ObjectId = ObjectId()) : RealmObject() {
    var album: RealmTrack? = null
    var bitmap: RealmBitmap? = null
}

open class RealmArtistWrapper(var _id: ObjectId = ObjectId()) : RealmObject() {
    var artist: RealmArtist? = null
    var bitmap: RealmBitmap? = null
}

open class RealmLinkedTrack(var _id: ObjectId = ObjectId()) : RealmObject() {
    var external_urls: RealmDictionary<String>? = null
    var href: String? = null
    var type: String? = null
    var uri: String? = null
    var id: String? = null
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