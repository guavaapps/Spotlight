package com.guavaapps.spotlight

import android.graphics.Bitmap
import com.guavaapps.components.bitmap.BitmapTools
import com.pixel.spotifyapi.Objects.Album
import com.pixel.spotifyapi.Objects.Playlist
import com.pixel.spotifyapi.Objects.PlaylistTrack
import com.pixel.spotifyapi.Objects.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun Track.wrap(bitmap: Bitmap? = null) = TrackWrapper(
    this,
    bitmap ?: withContext(Dispatchers.IO) { BitmapTools.from(this@wrap.album.images.firstOrNull()?.url) }
)

suspend fun Album.wrap(bitmap: Bitmap? = null) = AlbumWrapper(
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
    bitmap ?: withContext(Dispatchers.IO) { BitmapTools.from(this@wrap.track.album.images.firstOrNull()?.url) }
)