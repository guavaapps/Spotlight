package com.guavaapps.spotlight

import android.graphics.Bitmap
import com.pixel.spotifyapi.Objects.Artist

data class ArtistWrapper(
    var artist: Artist? = null,
    var thumbnail: Bitmap? = null,
)