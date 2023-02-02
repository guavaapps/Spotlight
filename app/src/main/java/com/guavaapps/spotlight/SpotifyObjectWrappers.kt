package com.guavaapps.spotlight

import android.graphics.Bitmap
import com.pixel.spotifyapi.Objects.*

data class TrackWrapper(
    var track: Track = Track(),
    var thumbnail: Bitmap? = null,
)

class UserWrapper(
    var user: UserPrivate,
    var bitmap: Bitmap? = null,
)

class PlaylistWrapper (
    var playlist: Playlist? = null,
    var thumbnail: Bitmap? = null
)

class PlaylistTrackWrapper(
    var track: PlaylistTrack? = null,
    var added_at: String? = null,
    var added_by: UserPublic? = null,
    var is_local: Boolean? = null,
    bitmap: Bitmap?
)

data class ArtistWrapper(
    var artist: Artist? = null,
    var thumbnail: Bitmap? = null,
)

class AlbumWrapper(
    var album: Album? = null,
    var bitmap: Bitmap? = null,
)