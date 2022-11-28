package com.guavaapps.spotlight

import com.guavaapps.spotlight.ColorSet.Companion.create
import android.view.LayoutInflater
import android.view.ViewGroup
import android.os.Bundle
import android.widget.TextView
import com.pixel.spotifyapi.Objects.TrackSimple
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.guavaapps.components.listview.ListView
import java.util.ArrayList

class AlbumFragment : Fragment() {
    private val viewModel: ContentViewModel by activityViewModels{ ContentViewModel.Factory }

    var album: AlbumWrapper? = null
        private set

    private val items: MutableList<View> = ArrayList()
    private val ids: MutableList<String?> = ArrayList()

    private var id: String? = null

    private lateinit var nestedScrollableHost: NestedScrollableHost
    private lateinit var listView: ListView

    private lateinit var colorSet: ColorSet

    private var listener: Listener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_album, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        nestedScrollableHost = view.findViewById(R.id.host)
        nestedScrollableHost.post {
            nestedScrollableHost.init(context,
                R.id.pager)
        } //pager2));
        listView = view.findViewById(R.id.list_view)
        viewModel.album.observe(viewLifecycleOwner) { albumWrapper ->
            val track = viewModel.track.value!!.track.id
            setAlbum(albumWrapper!!, track)
        }
    }

    private fun setCurrentTrack(id: String) {
        val i = ids.indexOf(id)
        val j = ids.indexOf(id)
        this.id = id
        if (i != -1) {
            val v1 = items[i]
            val titleView1 = v1.findViewById<TextView>(R.id.title_view)
            val titleView2 = v1.findViewById<TextView>(R.id.duration_view)
            titleView1.setTextColor(colorSet!!.secondary)
            titleView2.setTextColor(colorSet!!.secondary)
        }
        val v2 = items[j]
        val titleView2 = v2.findViewById<TextView>(R.id.title_view)
        val titleView3 = v2.findViewById<TextView>(R.id.duration_view)
        titleView2.setTextColor(colorSet!!.primary)
        titleView3.setTextColor(colorSet!!.primary)
        listView!!.scrollToPosition(j)
    }

    fun setAlbum(album: AlbumWrapper, id: String) {
        this.album = album
        colorSet = create(album.bitmap)
        ids.clear()
        items.clear()
        listView!!.clear()
        for (track in album.album!!.tracks.items) {
            ids.add(track.id)
            val item = LayoutInflater.from(context).inflate(R.layout.album_view_item, null, false)
            item.layoutParams = ViewGroup.LayoutParams(-1, -2)
            val typedArray =
                requireContext().theme.obtainStyledAttributes(android.R.style.Theme_Material_NoActionBar,
                    intArrayOf(android.R.attr.selectableItemBackground))
            val ripple =
                resources.getDrawable(typedArray.getResourceId(0, 0), requireContext().theme)
                    .mutate()
            ripple.setTint(colorSet!!.ripple)
            item.background = ripple
            item.isClickable = true
            item.setOnClickListener { v: View? ->
                if (listener != null) {
                    listener!!.onClick(track)
                    setCurrentTrack(track.id)
                }
            }
            val titleView = item.findViewById<TextView>(R.id.title_view)
            val artistsView = item.findViewById<TextView>(R.id.artists_view)
            val durationView = item.findViewById<TextView>(R.id.duration_view)
            titleView.text = track.name
//            val duration = TimeString.Builder(track.duration_ms)
//                .minutes()
//                .separator(":")
//                .seconds("%02d")
//                .build()
//                .toString()

            val duration = TimeString(track.duration_ms).apply {
                minutes()
                separator(":")
                seconds("%02d")
            }.toString()

            durationView.text = duration
            val artists: MutableList<String> = ArrayList()
            for (artist in track.artists) {
                artists.add(artist.name)
            }

            artistsView.text = java.lang.String.join(", ", artists)
            titleView.setTextColor(colorSet!!.secondary)
            artistsView.setTextColor(colorSet!!.secondary)
            durationView.setTextColor(colorSet!!.secondary)
            items.add(item)
        }
        listView!!.add(items)
        setCurrentTrack(id)
    }

    fun setListener(l: Listener?) {
        listener = l
    }

    interface Listener {
        fun onClick(track: TrackSimple?)
    }

    companion object {
        private const val TAG = "AlbumFragment"
    }
}