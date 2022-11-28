package com.guavaapps.spotlight

import com.pixel.spotifyapi.Objects.UserPrivate
import android.graphics.Bitmap

class UserWrapper(
    var user: UserPrivate,
    var bitmap: Bitmap? = null,
)