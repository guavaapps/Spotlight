package com.guavaapps.spotlight

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.google.android.material.button.MaterialButton
import com.guavaapps.spotlight.ColorSet.Companion.create
import com.spotify.android.appremote.api.PlayerApi
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.protocol.types.PlayerState

class TrackSmallFragment : Fragment() {
    private val viewModel: ContentViewModel by viewModels { ContentViewModel.Factory }

    private var isPlaying = false

    private var mSpotifyAppRemote: SpotifyAppRemote? = null
    private var mPlayerApi: PlayerApi? = null

    private var mTrack: TrackWrapper? = null
    private var mColorSet = ColorSet()

    private lateinit var mPlayDrawable: Drawable
    private lateinit var mPauseDrawable: Drawable
    private lateinit var mPlayButton: MaterialButton
    private lateinit var mTrackNameView: TextView
    private lateinit var mTrackArtistsView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_track_small, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mPlayDrawable = resources.getDrawable(R.drawable.ic_play_24, requireContext().theme)
        mPauseDrawable = resources.getDrawable(R.drawable.ic_pause_24, requireContext().theme)
        mPlayButton = view.findViewById(R.id.play)
        mTrackNameView = view.findViewById(R.id.track_name)
        mTrackArtistsView = view.findViewById(R.id.artists)

        with(mPlayButton) {
            setOnClickListener { v: View? ->
                if (isPlaying) {
                    isPlaying = false

                    icon = mPlayDrawable

                    viewModel.pause()
                } else {
                    isPlaying = true

                    icon = mPauseDrawable

                    viewModel.play()
                }
            }
        }

        viewModel.track.observe(viewLifecycleOwner) { trackWrapper: TrackWrapper? ->
            if (trackWrapper != null) {
                mTrack = trackWrapper
                mTrackNameView.text = trackWrapper.track.name
                val artists: MutableList<String> = ArrayList()
                for (artistSimple in trackWrapper.track.artists) {
                    artists.add(artistSimple.name)
                }
                mTrackArtistsView.text = java.lang.String.join(", ", artists)
                mTrackNameView.isSelected = true
                nextTrack(trackWrapper)
            }
        }

        viewModel.spotifyAppRemote.observe(viewLifecycleOwner) { appRemote: SpotifyAppRemote? ->
            mSpotifyAppRemote = appRemote
            mPlayerApi = mSpotifyAppRemote!!.playerApi
            mPlayerApi!!.subscribeToPlayerState().setEventCallback { data: PlayerState ->
                if (data.track.uri == mTrack!!.track.uri) {
                    if (data.isPaused) {
                        mPlayButton.icon = mPlayDrawable
                    } else {
                        mPlayButton.icon = mPauseDrawable
                    }
                }
            }
        }
    }

    private fun nextTrack(wrappedTrack: TrackWrapper) {
        val bitmap = wrappedTrack.thumbnail
        val colorSet = create(bitmap)
        val primaryAnimator =
            ValueAnimator.ofObject(ArgbEvaluator(), mColorSet.primary, colorSet.primary)
        primaryAnimator.duration =
            resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
        primaryAnimator.addUpdateListener { animation: ValueAnimator ->
            mColorSet.primary = animation.animatedValue as Int
            mTrackNameView.setTextColor(mColorSet.primary)
            mPlayButton.iconTint = ColorStateList.valueOf(mColorSet.primary)
        }
        primaryAnimator.start()
        val secondaryAnimator =
            ValueAnimator.ofObject(ArgbEvaluator(), mColorSet.secondary, colorSet.secondary)
        secondaryAnimator.duration =
            resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
        secondaryAnimator.addUpdateListener { animation: ValueAnimator ->
            mColorSet.secondary = animation.animatedValue as Int
            mTrackArtistsView.setTextColor(mColorSet.secondary)
        }
        secondaryAnimator.start()
        val tertiaryAnimator =
            ValueAnimator.ofObject(ArgbEvaluator(), mColorSet.tertiary, colorSet.tertiary)
        tertiaryAnimator.duration =
            resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
        tertiaryAnimator.addUpdateListener { animation: ValueAnimator ->
            mColorSet.tertiary = animation.animatedValue as Int
        }
        tertiaryAnimator.start()
        val rippleAnimator =
            ValueAnimator.ofObject(ArgbEvaluator(), mColorSet.ripple, colorSet.ripple)
        rippleAnimator.duration =
            resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
        rippleAnimator.addUpdateListener { animation: ValueAnimator ->
            mColorSet.ripple = animation.animatedValue as Int
            mPlayButton!!.rippleColor = ColorStateList.valueOf(mColorSet.ripple)
        }
        rippleAnimator.start()
        mColorSet = colorSet
    }
}