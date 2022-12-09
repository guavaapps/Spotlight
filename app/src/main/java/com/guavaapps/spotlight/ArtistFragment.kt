package com.guavaapps.spotlight

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.guavaapps.components.listview.ListView
import com.guavaapps.components.timestring.TimeString
import com.guavaapps.spotlight.R
import com.pixel.spotifyapi.Objects.Artist
import com.pixel.spotifyapi.Objects.ArtistSimple
import org.w3c.dom.Text
import kotlin.math.min

private const val TAG = "ArtistFragment"

class ArtistFragment(private val wrappedArtist: Artist?) :
    Fragment(R.layout.fragment_artist) {
    private val viewModel: ContentViewModel by activityViewModels { ContentViewModel.Factory }

    private lateinit var artist: ImageView
    private lateinit var artistName: TextView

    private lateinit var artistTracks: ListView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        linkNestedScrollableHost()

        artist = view.findViewById(R.id.artist)
        artistName = view.findViewById(R.id.artist_name)

        artistTracks = view.findViewById(R.id.tracks)
        artistTracks.canScroll = false
        artistTracks.requestDisallowInterceptTouchEvent(true)

        apply(ArtistWrapper(wrappedArtist))

        viewModel.artists.observe(viewLifecycleOwner) {
            val artist = it?.find { it.artist?.id == wrappedArtist?.id }
            apply(artist)
        }

        viewModel.artistTracks.observe(viewLifecycleOwner) {
            val tracks = it[wrappedArtist?.id] ?: return@observe

            val views = tracks.map {
                TrackView.create(requireContext(), it) {
                    Log.e(TAG, "clicked artist track - ${it.track.name}")
                }
            }

            artistTracks.add(views)
        }
    }

    private fun apply(wrappedArtist: ArtistWrapper?) {
        artist.setImageBitmap(wrappedArtist?.thumbnail)
        artistName.text = wrappedArtist?.artist?.name
    }

    override fun onResume() {
        super.onResume()
        Log.e(TAG, "onResume ---------")

        viewModel.getArtistTracks(wrappedArtist!!)

    }

    override fun onPause() {
        super.onPause()

        Log.e(TAG, "onPause ---------")
    }

    private fun linkNestedScrollableHost() {
        val view = requireView()

        val host = view.findViewById<NestedScrollableHost>(R.id.nested_scrollable_host)
        host.post { host.init(requireContext(), R.id.pager) }
    }
}

object TrackView {
    fun create(context: Context, track: TrackWrapper, onClick: (TrackWrapper) -> Unit): View {
        val view = LayoutInflater.from(context).inflate(R.layout.track_view, null)
        view.layoutParams = ViewGroup.LayoutParams(-1, -2)

        val bitmap = view.findViewById<ImageView>(R.id.bitmap)
        val name = view.findViewById<TextView>(R.id.name)
        val artists = view.findViewById<TextView>(R.id.artists)
        val duration = view.findViewById<TextView>(R.id.duration)

        bitmap.setImageBitmap(track.thumbnail)
        name.text = track.track.name
        artists.text = track.track.artists.joinToString { it.name }
        duration.text = TimeString(track.track.duration_ms).apply {
            minutes()
            separator(":")
            seconds("%02d")
        }.toString()

        view.setOnClickListener { onClick(track) }

        return view
    }
}