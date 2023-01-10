package com.guavaapps.spotlight.realm

import android.graphics.Bitmap
import com.guavaapps.spotlight.MatchWith
import com.pixel.spotifyapi.Objects.*
import io.realm.*
import io.realm.annotations.*
import org.bson.types.ObjectId
import java.nio.ByteBuffer
import java.util.*

@RealmModule(classes = [AppUser::class, Model::class, TrackModel::class, ModelParam::class])
open class RealmObjectsModule

// your very pretty
//gay <3

// user
@RealmClass("User")
open class AppUser(
    @PrimaryKey
    @RealmField("_id")
    var spotify_id: String = "",
    var created: Date? = null,
    var last_login: Date? = null,
    var locale: String? = null,
    var playlist: String? = null,
    var timeline: RealmList<TrackModel> = RealmList(),
) : RealmObject()

@RealmClass(embedded = true)
open class TrackModel(
    var id: String? = null,
    var uri: String? = null,
    //var genres: RealmList<String> = RealmList(),
    @Required
    var timestamp: Long? = null,
    @Required
    var features: RealmList<Float> = RealmList(),
) : RealmObject()

fun d (){
    val t = Track ()


}

// model
open class Model(
    @PrimaryKey
    @RealmField("_id")
    var spotify_id: String = "",
    var version: String? = null,
    var timestamp: Long? = null,
    var model_params: RealmList<ModelParam> = RealmList(),
) : RealmObject()

@RealmClass(embedded = true)
open class ModelParam(
    @Required
    var params: RealmList<Float> = RealmList(),

    @Required
    var shape: RealmList<Int> = RealmList(),
) : RealmObject() {}

@MatchWith([UserPublic::class])
open class RealmUserPublic(
    var display_name: String? = null,
    var external_urls: RealmDictionary<String>? = null,
    var followers: RealmFollowers? = null,
    var href: String? = null,
    @RealmField ("_id")
    var id: String? = null,
    var images: RealmList<RealmImage>? = null,
    var type: String? = null,
    var uri: String? = null,
) : RealmObject()


@MatchWith([Playlist::class])
open class RealmPlaylist(
    var collaborative: Boolean? = null,
    var external_urls: RealmDictionary<String>? = null,
    var href: String? = null,
    @RealmField ("_id")
    var id: String? = null,
    var images: RealmList<RealmImage?>? = null,
    var name: String? = null,
    var owner: RealmUserPublic? = null,
    var is_public: Boolean? = null,
    var snapshot_id: String? = null,
    var type: String? = null,
    var uri: String? = null,
    var description: String? = null,
    var followers: RealmFollowers? = null,
    var tracks: RealmPlaylistTrackPager? = null,
) : RealmObject()

@RealmClass (embedded = true)
open class RealmPlaylistTrack(
    var added_at: String? = null,
    var added_by: RealmUserPublic? = null,
    var track: RealmTrack? = null,
    var is_local: Boolean? = null,
): RealmObject ()

// spotify
@MatchWith([Track::class])
open class RealmTrack(
) : RealmObject() {
    @PrimaryKey
    @RealmField("_id")
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

open class RealmTrackSimple() : RealmObject() {
    @PrimaryKey
    @RealmField("_id")
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

open class RealmAlbumSimple(
) : RealmObject() {
    var album_type: String? = null
    var available_markets: RealmList<String>? = null
    var external_urls: RealmDictionary<String>? = null
    var href: String? = null

    @PrimaryKey
    @RealmField("_id")
    var id: String? = null
    var images: RealmList<RealmImage>? = null
    var name: String? = null
    var type: String? = null
    var uri: String? = null
}

@RealmClass(embedded = true)
open class RealmBitmap : RealmObject() {
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
@MatchWith([Album::class])
open class RealmAlbum : RealmObject() {
    var album_type: String? = null
    var available_markets: RealmList<String>? = null
    var external_urls: RealmDictionary<String>? = null
    var href: String? = null

    @PrimaryKey
    @RealmField("_id")
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
    var tracks: RealmTrackSimplePager? = null
}

@RealmClass(embedded = true)
open class RealmCopyright : RealmObject() {
    var text: String? = null
    var type: String? = null
}

open class RealmArtist(
    @PrimaryKey
    var _id: ObjectId = ObjectId(),
) : RealmObject() {
    var external_urls = RealmDictionary<String>()
    var href: String? = null
    var id: String? = null
    var name: String? = null
    var type: String? = null
    var uri: String? = null

    var followers: RealmFollowers? = null
    var genres = RealmList<String>()
    var images = RealmList<RealmImage>()
    var popularity: Int? = null
}

open class RealmArtistSimple : RealmObject() {
    var external_urls = RealmDictionary<String>()
    var href: String? = null

    @PrimaryKey
    @RealmField("_id")
    var id: String? = null
    var name: String? = null
    var type: String? = null
    var uri: String? = null
}

@RealmClass(embedded = true)
open class RealmFollowers : RealmObject() {
    var href: String? = null
    var total: Int? = null
}

@RealmClass(embedded = true)
open class RealmImage : RealmObject() {
    var width: Int? = null
    var height: Int? = null
    var url: String? = null
}

//@RealmClass(embedded = true)
//open class RealmPager : RealmObject() {
//    var href: String? = null
//    var items: RealmList<RealmObject> = RealmList()
//    var limit = 0
//    var next: String? = null
//    var offset = 0
//    var previous: String? = null
//    var total = 0
//}

@RealmClass(embedded = true)
open class RealmTrackSimplePager : RealmObject() {
    var href: String? = null
    var items: RealmList<RealmTrackSimple> = RealmList()
    var limit = 0
    var next: String? = null
    var offset = 0
    var previous: String? = null
    var total = 0
}

@RealmClass(embedded = true)
open class RealmPlaylistTrackPager : RealmObject() {
    var href: String? = null
    var items: RealmList<RealmPlaylistTrack> = RealmList()
    var limit = 0
    var next: String? = null
    var offset = 0
    var previous: String? = null
    var total = 0
}

open class RealmTrackWrapper(
    @PrimaryKey
    var _id: ObjectId = ObjectId(),
) : RealmObject() {
    var track: RealmTrack? = null
    var bitmap: RealmBitmap? = null
}

//
open class RealmAlbumWrapper(
    @PrimaryKey
    var _id: ObjectId = ObjectId(),
) : RealmObject() {
    var album: RealmTrack? = null
    var bitmap: RealmBitmap? = null
}

open class RealmArtistWrapper(
    @PrimaryKey
    var _id: ObjectId = ObjectId(),
) : RealmObject() {
    var artist: RealmArtist? = null
    var bitmap: RealmBitmap? = null
}

@RealmClass(embedded = true)
open class RealmLinkedTrack : RealmObject() {
    var external_urls: RealmDictionary<String>? = null
    var href: String? = null
    var type: String? = null
    var uri: String? = null
    var id: String? = null
}