package com.guavaapps.spotlight

import android.view.LayoutInflater
import android.view.ViewGroup
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.guavaapps.components.listview.ListView
import com.guavaapps.spotlight.R

private const val TAG = "PlaylistFragment"

class PlaylistFragment (private val id: String) : Fragment(R.layout.fragment_playlist) {
    private val viewModel: ContentViewModel by activityViewModels { ContentViewModel.Factory }

    private lateinit var listView: ListView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listView = view.findViewById(R.id.list_view)

        viewModel.playlistTracks.observe(viewLifecycleOwner) {
            val tracks = it [id] ?: return@observe

            Log.e(TAG, "items - ${tracks.joinToString { it.track?.track?.name ?: "" }}")

            val views = tracks.map {
                val textView = TextView (requireContext()).apply {
                    text = it.track?.track?.name
                }

                val lp = textView.layoutParams ?: ViewGroup.LayoutParams(-1, 200)
                lp.width = -1
                lp.height = 200

                textView.layoutParams = lp

                textView
            }

            listView.add(views)
        }
    }
}