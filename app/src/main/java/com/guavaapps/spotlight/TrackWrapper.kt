package com.guavaapps.spotlight;

import android.graphics.Bitmap;

import com.pixel.spotifyapi.Objects.Track;

public class TrackWrapper {
    public Track track;
    public Bitmap thumbnail;

    public TrackWrapper () {
    }

    public TrackWrapper (Track track, Bitmap thumbnail) {
        this.track = track;
        this.thumbnail = thumbnail;
    }
}