package com.guavaapps.spotlight;

import android.graphics.Bitmap;

import com.pixel.spotifyapi.Objects.PlaylistSimple;

public class PlaylistWrapper {
    public PlaylistSimple playlist;
    public Bitmap thumbnail;

    public PlaylistWrapper () {
    }

    public PlaylistWrapper (PlaylistSimple playlist, Bitmap thumbnail) {
        this.playlist = playlist;
        this.thumbnail = thumbnail;
    }
}