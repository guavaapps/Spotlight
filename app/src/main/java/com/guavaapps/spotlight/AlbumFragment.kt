package com.guavaapps.spotlight

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.graphics.Bitmap
import com.guavaapps.spotlight.ColorSet.Companion.create
import android.view.LayoutInflater
import android.view.ViewGroup
import android.os.Bundle
import android.widget.TextView
import com.pixel.spotifyapi.Objects.TrackSimple
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.guavaapps.components.listview.ListView
import com.guavaapps.components.listview.NestedScrollableHost
import com.pixel.spotifyapi.Objects.Album
import com.pixel.spotifyapi.Objects.Track

private const val TAG = "AlbumFragment"

class AlbumFragment : Fragment() {
    private val viewModel: ContentViewModel by activityViewModels { ContentViewModel.Factory }

    private val items = mutableListOf<View>()
    private var index = 0

    private lateinit var nestedScrollableHost: NestedScrollableHost
    private lateinit var listView: ListView

    private lateinit var colorSet: ColorSet

    private var onSelectedBlock: (TrackSimple?) -> Unit = {}

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
            nestedScrollableHost.init(context, R.id.pager)
        }
        listView = view.findViewById(R.id.list_view)
        viewModel.album.observe(viewLifecycleOwner) {
            if (it?.album == null) {
                index = 0
                listView.removeAllViews()
                listView.clear()

                return@observe
            }

            val track = viewModel.track.value?.track

            setAlbum(it)
            makeSelection(track, it.album)
        }

        viewModel.track.observe(viewLifecycleOwner) {
            val album = viewModel.album.value?.album ?: return@observe

            makeSelection(it?.track, album)

            applyColors(it?.thumbnail!!)
        }
    }

    private fun makeSelection(track: Track?, album: Album?) {
        if (track?.album?.id != album?.id) return

        unselectView(index)
        index = album?.tracks?.items?.indexOfFirst { t -> t.id == track?.id } ?: 0
        selectView(index)
    }

    private fun selectView(index: Int) {
        val view = items[index]

        val titleView = view.findViewById<TextView>(R.id.title_view)
        val durationView = view.findViewById<TextView>(R.id.duration_view)

        titleView.setTextColor(colorSet.primary)
        durationView.setTextColor(colorSet.primary)
    }

    private fun unselectView(index: Int) {
        if (index > items.lastIndex) return

        val view = items[index]

        val titleView = view.findViewById<TextView>(R.id.title_view)
        val durationView = view.findViewById<TextView>(R.id.duration_view)

        titleView.setTextColor(colorSet.secondary)
        durationView.setTextColor(colorSet.secondary)
    }

    private fun createItem(track: TrackSimple): View {
        val item = LayoutInflater.from(context).inflate(R.layout.album_view_item, null, false)

        item.layoutParams = ViewGroup.LayoutParams(-1, -2)

        val typedArray =
            requireContext().theme.obtainStyledAttributes(android.R.style.Theme_Material_NoActionBar,
                intArrayOf(android.R.attr.selectableItemBackground))

        val ripple =
            resources.getDrawable(typedArray.getResourceId(0, 0), requireContext().theme)
                .mutate()

        ripple.setTint(colorSet.ripple)

        item.background = ripple
        item.isClickable = true

        item.setOnClickListener {
            unselectView(this.index)
            this.index = items.indexOf(it)
            selectView(this.index)

            val selected = viewModel.album.value?.album?.tracks?.items?.get(items.indexOf(it))

            onSelectedBlock(selected)
        }

        val titleView = item.findViewById<TextView>(R.id.title_view)
        val artistsView = item.findViewById<TextView>(R.id.artists_view)
        val durationView = item.findViewById<TextView>(R.id.duration_view)

        titleView.text = track.name

        val duration = TimeString(track.duration_ms).apply {
            minutes()
            separator(":")
            seconds("%02d")
        }.toString()

        durationView.text = duration
        artistsView.text = track.artists.joinToString { it.name }

        titleView.setTextColor(colorSet!!.secondary)
        artistsView.setTextColor(colorSet!!.secondary)
        durationView.setTextColor(colorSet!!.secondary)

        return item
    }

    private fun applyColors(bitmap: Bitmap) {
        val colorSet = create(bitmap)

        items.forEach {
            val typedArray =
                requireContext().theme.obtainStyledAttributes(android.R.style.Theme_Material_NoActionBar,
                    intArrayOf(android.R.attr.selectableItemBackground))

            val ripple =
                resources.getDrawable(typedArray.getResourceId(0, 0), requireContext().theme)
                    .mutate()

            val rippleAnimator = ValueAnimator.ofObject(
                ArgbEvaluator(),
                this.colorSet.ripple,
                colorSet.ripple
            )

            with(rippleAnimator) {
                duration = 350
                addUpdateListener { v ->
                    this@AlbumFragment.colorSet.ripple = v.animatedValue as Int

                    ripple.setTint(this@AlbumFragment.colorSet.ripple)

                    it.background = ripple
                }
            }


            val titleView = it.findViewById<TextView>(R.id.title_view)
            val artistsView = it.findViewById<TextView>(R.id.artists_view)
            val durationView = it.findViewById<TextView>(R.id.duration_view)

            val secondaryAnimator = ValueAnimator.ofObject(
                ArgbEvaluator(),
                this.colorSet.secondary,
                colorSet.secondary
            )

            with(secondaryAnimator) {
                duration = 350
                addUpdateListener {
                    this@AlbumFragment.colorSet.secondary = it.animatedValue as Int

                    titleView.setTextColor(this@AlbumFragment.colorSet.secondary)
                    artistsView.setTextColor(this@AlbumFragment.colorSet.secondary)
                    durationView.setTextColor(this@AlbumFragment.colorSet.secondary)

                }
            }

            rippleAnimator.start()
            secondaryAnimator.start()
        }
    }

    private fun setAlbum(album: AlbumWrapper) {
        colorSet = create(album.bitmap)
        items.clear()
        listView.clear()

        val tracks = album.album?.tracks?.items

        tracks?.forEach {
            val item = createItem(it)
            items.add(item)
        }

        listView.add(items)
    }

    fun onTrackSelected(onSelected: (track: TrackSimple?) -> Unit) {
        onSelectedBlock = onSelected
    }
}