package com.guavaapps.spotlight;

import android.graphics.Bitmap;

import com.pixel.spotifyapi.Objects.UserPrivate;

public class UserWrapper {
    public UserPrivate user;
    public Bitmap thumbnail;

    public UserWrapper () {
    }

    public UserWrapper (UserPrivate user, Bitmap thumbnail) {
        this.user = user;
        this.thumbnail = thumbnail;
    }
}