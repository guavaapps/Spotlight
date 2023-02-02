package com.guavaapps.spotlight

import android.graphics.Bitmap
import com.guavaapps.components.bitmap.BitmapTools
import com.pixel.spotifyapi.Objects.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// extension functions for Spotify objects that contain 'thumbnail' images
// e.g. the Track object contains cover art image urls
// use [Spotify object].wrap() to resolve the image url into a bitmap and create
// a Spotify object wrapper with the object and the resolved bitmap
suspend fun Track.wrap(bitmap: Bitmap? = null) = TrackWrapper(
    this,
    bitmap
        ?: withContext(Dispatchers.IO) { BitmapTools.from(this@wrap.album.images.firstOrNull()?.url) }
)

suspend fun Album.wrap(bitmap: Bitmap? = null) = AlbumWrapper(
    this,
    bitmap ?: withContext(Dispatchers.IO) { BitmapTools.from(this@wrap.images.firstOrNull()?.url) }
)

suspend fun Artist.wrap(bitmap: Bitmap? = null) = ArtistWrapper(
    this,
    bitmap ?: withContext(Dispatchers.IO) { BitmapTools.from(this@wrap.images.firstOrNull()?.url) }
)

suspend fun Playlist.wrap(bitmap: Bitmap? = null) = PlaylistWrapper(
    this,
    bitmap ?: withContext(Dispatchers.IO) { BitmapTools.from(this@wrap.images?.firstOrNull()?.url) }
)

suspend fun PlaylistTrack.wrap(bitmap: Bitmap? = null) = PlaylistTrackWrapper(
    this,
    added_at,
    added_by,
    is_local,
    bitmap
        ?: withContext(Dispatchers.IO) { BitmapTools.from(this@wrap.track.album.images.firstOrNull()?.url) }
)