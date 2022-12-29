package com.guavaapps.spotlight

import android.os.Parcel
import android.os.Parcelable
import com.pixel.spotifyapi.Objects.Track
import com.pixel.spotifyapi.Objects.TrackSimple
import java.util.*

open class RecentlyPlayedTrack : Parcelable {
    var track: Track? = null
    var played_at: String? = null
    var context: String? = null

    constructor(parcel: Parcel) {
        this.track = parcel.readParcelable(Track::class.java.classLoader)
        this.played_at = parcel.readString()
        this.context = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(track, 0)
        parcel.writeString(played_at)
        parcel.writeString(context)
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