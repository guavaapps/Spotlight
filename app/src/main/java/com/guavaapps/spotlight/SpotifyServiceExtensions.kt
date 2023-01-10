package com.guavaapps.spotlight

import android.os.Parcel
import android.os.Parcelable
import com.pixel.spotifyapi.Objects.Track
import com.pixel.spotifyapi.Objects.TrackSimple
import com.pixel.spotifyapi.SpotifyService
import java.util.*

open class Context : Parcelable {


    constructor(parcel: Parcel) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<RecentlyPlayedTrack> {
        override fun createFromParcel(parcel: Parcel): RecentlyPlayedTrack {
            return RecentlyPlayedTrack(parcel)
        }

        override fun newArray(size: Int): Array<RecentlyPlayedTrack?> {
            return arrayOfNulls(size)
        }
    }
}

open class RecentlyPlayedTrack : Parcelable {
    var track: Track? = null
    var played_at: String? = null
    var context: Context? = null

    constructor(parcel: Parcel) {
        this.track = parcel.readParcelable(Track::class.java.classLoader)
        this.played_at = parcel.readString()
        this.context = parcel.readParcelable(Context::class.java.classLoader)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(track, 0)
        parcel.writeString(played_at)
        parcel.writeParcelable(context, 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<RecentlyPlayedTrack> {
        override fun createFromParcel(parcel: Parcel): RecentlyPlayedTrack {
            return RecentlyPlayedTrack(parcel)
        }

        override fun newArray(size: Int): Array<RecentlyPlayedTrack?> {
            return arrayOfNulls(size)
        }
    }
}