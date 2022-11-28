package com.guavaapps.spotlight;

import android.graphics.Bitmap;

import com.pixel.spotifyapi.Objects.Album;

public class AlbumWrapper {
    public Album album;
    public Bitmap bitmap;

    public AlbumWrapper () {
    }

    public AlbumWrapper (Album album, Bitmap bitmap) {
        this.album = album;
        this.bitmap = bitmap;
    }
}