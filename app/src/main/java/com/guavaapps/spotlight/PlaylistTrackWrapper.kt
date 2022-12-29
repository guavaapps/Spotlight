package com.guavaapps.spotlight

import android.graphics.Bitmap
import com.pixel.spotifyapi.Objects.*

class PlaylistTrackWrapper(
    var track: PlaylistTrack? = null,
    var added_at: String? = null,
    var added_by: UserPublic? = null,
    var is_local: Boolean? = null,
    bitmap: Bitmap?
)