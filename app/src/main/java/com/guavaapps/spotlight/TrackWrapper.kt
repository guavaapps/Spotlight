package com.guavaapps.spotlight

import android.graphics.Bitmap
import com.pixel.spotifyapi.Objects.Track

data class TrackWrapper(
    var track: Track = Track(),
    var thumbnail: Bitmap? = null,
)