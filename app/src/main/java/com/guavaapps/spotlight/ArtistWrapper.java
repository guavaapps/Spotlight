package com.guavaapps.spotlight;

import android.graphics.Bitmap;

import com.pixel.spotifyapi.Objects.Artist;

public class ArtistWrapper {
    public Artist artist;
    public Bitmap thumbnail;

    public ArtistWrapper () {
    }

    public ArtistWrapper (Artist artist, Bitmap thumbnail) {
        this.artist = artist;
        this.thumbnail = thumbnail;
    }
}