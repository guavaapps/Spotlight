package com.guavaapps.spotlight

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.button.MaterialButton
import com.guavaapps.spotlight.ColorSet.Companion.create
import com.pixel.spotifyapi.Objects.Track

class TrackLargeFragment : Fragment(R.layout.fragment_track_large) {
    private val viewModel: ContentViewModel by activityViewModels { ContentViewModel.Factory }

    private var colorSet = ColorSet()

    private var isPlaying = false

    private lateinit var playDrawable: Drawable
    private lateinit var pauseDrawable: Drawable

    private lateinit var trackNameView: TextView
    private lateinit var artistsView: TextView

    private lateinit var playButton: MaterialButton
    private lateinit var seekBar: AppCompatSeekBar

    private lateinit var progressView: TextView
    private lateinit var durationView: TextView

    private lateinit var spotifyButton: MaterialButton

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        playDrawable = resources.getDrawable(R.drawable.ic_play_24, requireContext().theme)
        pauseDrawable = resources.getDrawable(R.drawable.ic_pause_24, requireContext().theme)

        trackNameView = view.findViewById(R.id.track_name_view)
        artistsView = view.findViewById(R.id.artists_view)

        playButton = view.findViewById(R.id.play_button)
        seekBar = view.findViewById(R.id.seek_bar)

        progressView = view.findViewById(R.id.progress_view)
        durationView = view.findViewById(R.id.duration_view)

        spotifyButton = view.findViewById(R.id.spotify_button)

        with(playButton) {
            setOnClickListener {
                if (!isPlaying) {
                    isPlaying = true
                    icon = pauseDrawable
                    viewModel.play()
                } else {
                    isPlaying = false
                    icon = playDrawable
                    viewModel.pause()
                }
            }
        }

        with(seekBar) {
            setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        viewModel.shouldPausePlayerStateListener = true
                        viewModel.progress.value = progress.toLong()
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {}

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    viewModel.shouldPausePlayerStateListener = false
                    viewModel.seekTo(progress.toLong())
                }
            })
        }

        viewModel.track.observe(viewLifecycleOwner) {
            if (it != null) {
                trackNameView.text = it.track.name
                artistsView.text = it.track.artists.joinToString { it.name }
                applyTrackDuration(it.track)

                applyColors(it.thumbnail!!)
            }
        }

        viewModel.progress.observe(viewLifecycleOwner) {
            setProgress(it)
            setSeekBar(it)
        }
    }

    private fun applyColors(bitmap: Bitmap) {
        val colorSet = create(bitmap)

        val primaryAnimator = ValueAnimator.ofObject(
            ArgbEvaluator(),
            this.colorSet.primary, colorSet.primary
        )
        primaryAnimator.duration =
            resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

        primaryAnimator.addUpdateListener {
            this.colorSet.primary = it.animatedValue as Int

            trackNameView.setTextColor(this.colorSet.primary)

            playButton.iconTint = ColorStateList.valueOf(this.colorSet.primary)

            spotifyButton.setBackgroundColor(this.colorSet.primary)

            seekBar.progressTintList = ColorStateList.valueOf(this.colorSet.primary)
            seekBar.thumbTintList = ColorStateList.valueOf(this.colorSet.primary)
        }

        primaryAnimator.start()

        val onPrimaryAnimator =
            ValueAnimator.ofObject(ArgbEvaluator(), this.colorSet.text, colorSet.text)

        onPrimaryAnimator.duration =
            resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

        onPrimaryAnimator.addUpdateListener {
            this.colorSet.text = it.animatedValue as Int
            spotifyButton.setTextColor(this.colorSet.text)
        }

        onPrimaryAnimator.start()

        val secondaryAnimator =
            ValueAnimator.ofObject(ArgbEvaluator(), this.colorSet.secondary, colorSet.secondary)

        secondaryAnimator.duration =
            resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

        secondaryAnimator.addUpdateListener {
            this.colorSet.secondary = it.animatedValue as Int

            artistsView.setTextColor(this.colorSet.secondary)

        }

        secondaryAnimator.start()

        val tertiaryAnimator =
            ValueAnimator.ofObject(ArgbEvaluator(), this.colorSet.tertiary, colorSet.tertiary)

        tertiaryAnimator.duration =
            resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

        tertiaryAnimator.addUpdateListener {
            this.colorSet.tertiary = it.animatedValue as Int

            seekBar.progressBackgroundTintList = ColorStateList.valueOf(this.colorSet.tertiary)

            progressView.setTextColor(this.colorSet.tertiary)

            durationView.setTextColor(this.colorSet.tertiary)
        }

        tertiaryAnimator.start()

        val rippleAnimator =
            ValueAnimator.ofObject(ArgbEvaluator(), this.colorSet.ripple, colorSet.ripple)

        rippleAnimator.duration =
            resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

        rippleAnimator.addUpdateListener {
            this.colorSet.ripple = it.animatedValue as Int

            playButton.rippleColor = ColorStateList.valueOf(this.colorSet.ripple)
        }

        rippleAnimator.start()

        this.colorSet = colorSet
    }

    private fun applyTrackDuration(track: Track) {
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

        progressView.text = progressString

        durationView.text = durationString

        seekBar.max = track.duration_ms.toInt()
    }

    private fun setProgress(progress: Long) {
        val progressString = TimeString.Builder(progress)
            .minutes()
            .separator(":")
            .seconds("%02d")
            .build()
            .toString()
        progressView.text = progressString
    }

    private fun setSeekBar(progress: Long) {
        seekBar.setProgress(progress.toInt(), true)
    }
}