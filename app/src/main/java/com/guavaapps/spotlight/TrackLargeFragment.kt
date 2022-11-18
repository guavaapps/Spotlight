package com.guavaapps.spotlight

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.google.android.material.button.MaterialButton
import com.guavaapps.spotlight.ColorSet.Companion.create
import com.spotify.android.appremote.api.PlayerApi
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.protocol.types.PlayerState
import java.util.concurrent.ScheduledExecutorService

class TrackLargeFragment : Fragment() {
    private val viewModel: ContentViewModel by viewModels()

    private var spotifyAppRemote: SpotifyAppRemote? = null
    private var playerApi: PlayerApi? = null
    private var mTrack: TrackWrapper? = null
    private var mColorSet = ColorSet()
    private val mPlayerStateListener: ScheduledExecutorService? = null
    private var isPlaying = false
    private val mProgress: Long = 0
    private lateinit var mPlayDrawable: Drawable
    private lateinit var mPauseDrawable: Drawable
    private lateinit var mTrackNameView: TextView
    private lateinit var mPlayButton: MaterialButton
    private lateinit var mSeekBar: AppCompatSeekBar
    private lateinit var mProgressView: TextView
    private lateinit var mDurationView: TextView
    private lateinit var mSpotifyButton: MaterialButton
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_track_large, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mPlayDrawable = resources.getDrawable(R.drawable.ic_play_24, requireContext().theme)
        mPauseDrawable = resources.getDrawable(R.drawable.ic_pause_24, requireContext().theme)
        mTrackNameView = view.findViewById(R.id.track_name_view)
        mPlayButton = view.findViewById(R.id.play_button)
        mSeekBar = view.findViewById(R.id.seek_bar)
        mProgressView = view.findViewById(R.id.progress_view)
        mDurationView = view.findViewById(R.id.duration_view)
        mSpotifyButton = view.findViewById(R.id.spotify_button)

        with(mPlayButton) {
            setOnClickListener {
                if (!isPlaying) {
                    isPlaying = true
                    icon = mPauseDrawable
                    viewModel.play()
                } else {
                    isPlaying = false
                    icon = mPlayDrawable
                    viewModel.pause()
                }
            }
        }

        with (mSeekBar) {
            setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    if (fromUser) viewModel.progress.value = progress.toLong()
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {}

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    viewModel.spotifyAppRemote.value!!
                        .playerApi
                        .seekTo(mProgress)
                }
            })
        }

//        mSeekBar!!.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
//            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
//                if (fromUser) viewModel!!.progress.setValue(progress.toLong())
//            }
//
//            override fun onStartTrackingTouch(seekBar: SeekBar) {}
//            override fun onStopTrackingTouch(seekBar: SeekBar) {
//                viewModel!!.spotifyAppRemote.value!!
//                    .getPlayerApi()
//                    .seekTo(mProgress)
//            }
//        })

        //
        viewModel.spotifyAppRemote.observe(viewLifecycleOwner) { appRemote: SpotifyAppRemote ->
            spotifyAppRemote = appRemote
            playerApi = appRemote.playerApi
            playerApi!!.subscribeToPlayerState().setEventCallback { data: PlayerState ->
                if (data.track.uri == mTrack!!.track.uri) {
                    if (data.isPaused) {
                        mPlayButton!!.setIcon(mPlayDrawable)
                    } else {
                        mPlayButton!!.setIcon(mPauseDrawable)
                    }
                }
            }
        }

        viewModel.track.observe(viewLifecycleOwner) { trackWrapper: TrackWrapper? ->
            if (trackWrapper != null) {
                mTrack = trackWrapper
                nextTrack(trackWrapper)
                mSeekBar.max = mTrack!!.track.duration_ms.toInt()
            }
        }
        viewModel.nextTrack.observe(viewLifecycleOwner) { trackWrapper: TrackWrapper? -> }

        viewModel.progress.observe(viewLifecycleOwner) { progress: Long ->
            setProgress(progress)
            setSeekBar(progress)
        }
    }

    private fun nextTrack(wrappedTrack: TrackWrapper) {
        val track = wrappedTrack.track
        val bitmap = wrappedTrack.thumbnail

        val colorSet = create(bitmap)

        val trackName = SpannableString(track.name + " " + track.artists[0].name)
        val albumTabTitle = SpannableString("From " + track.album.name)

        val primaryAnimator = ValueAnimator.ofObject(ArgbEvaluator(), mColorSet.primary, colorSet.primary)
        primaryAnimator.duration = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
        primaryAnimator.addUpdateListener { animation: ValueAnimator ->
            mColorSet.primary = animation.animatedValue as Int

            trackName.setSpan(ForegroundColorSpan(mColorSet.primary),
                0,
                track.name.length,
                Spanned.SPAN_INCLUSIVE_INCLUSIVE)

            mTrackNameView!!.text = trackName

            mPlayButton!!.iconTint = ColorStateList.valueOf(mColorSet.primary)

            mSpotifyButton!!.setBackgroundColor(mColorSet.primary)

            mSeekBar!!.progressTintList = ColorStateList.valueOf(mColorSet.primary)
            mSeekBar!!.thumbTintList = ColorStateList.valueOf(mColorSet.primary)
        }

        primaryAnimator.start()

        val onPrimaryAnimator =
            ValueAnimator.ofObject(ArgbEvaluator(), mColorSet.text, colorSet.text)
        onPrimaryAnimator.duration =
            resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
        onPrimaryAnimator.addUpdateListener { animation: ValueAnimator ->
            mColorSet.text = animation.animatedValue as Int
            mSpotifyButton!!.setTextColor(mColorSet.text)
        }
        onPrimaryAnimator.start()
        val secondaryAnimator =
            ValueAnimator.ofObject(ArgbEvaluator(), mColorSet.secondary, colorSet.secondary)
        secondaryAnimator.duration =
            resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
        secondaryAnimator.addUpdateListener { animation: ValueAnimator ->
            mColorSet.secondary = animation.animatedValue as Int
            trackName.setSpan(ForegroundColorSpan(mColorSet.secondary),
                track.name.length + 1,
                trackName.length,
                Spanned.SPAN_INCLUSIVE_INCLUSIVE)
            mTrackNameView!!.text = trackName
        }
        secondaryAnimator.start()
        val tertiaryAnimator =
            ValueAnimator.ofObject(ArgbEvaluator(), mColorSet.tertiary, colorSet.tertiary)
        tertiaryAnimator.duration =
            resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
        tertiaryAnimator.addUpdateListener { animation: ValueAnimator ->
            mColorSet.tertiary = animation.animatedValue as Int
            mSeekBar!!.progressBackgroundTintList = ColorStateList.valueOf(mColorSet.tertiary)
            mProgressView!!.setTextColor(mColorSet.tertiary)
            mDurationView!!.setTextColor(mColorSet.tertiary)
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
        val progressString = TimeString.Builder(0)
            .minutes()
            .separator(":")
            .seconds("%02d")
            .build()
            .toString()
        val durationString = TimeString.Builder(track.duration_ms)
            .minutes()
            .separator(":")
            .seconds("%02d")
            .build()
            .toString()
        mProgressView!!.text = progressString
        mDurationView!!.text = durationString
        mColorSet = colorSet
    }

    fun setProgress(progress: Long) {
        val progressString = TimeString.Builder(progress)
            .minutes()
            .separator(":")
            .seconds("%02d")
            .build()
            .toString()
        mProgressView!!.text = progressString
    }

    fun setSeekBar(progress: Long) {
        mSeekBar!!.setProgress(progress.toInt(), true)
    }
}