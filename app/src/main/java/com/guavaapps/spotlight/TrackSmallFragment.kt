package com.guavaapps.spotlight

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.button.MaterialButton
import com.guavaapps.spotlight.ColorSet.Companion.create

class TrackSmallFragment : Fragment(R.layout.fragment_track_small) {
    private val viewModel: ContentViewModel by activityViewModels { ContentViewModel.Factory }

    private var isPlaying = false

    private var colorSet = ColorSet()

    private lateinit var playDrawable: Drawable
    private lateinit var pauseDrawable: Drawable
    private lateinit var playButton: MaterialButton
    private lateinit var trackNameView: TextView
    private lateinit var trackArtistsView: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        playDrawable = resources.getDrawable(R.drawable.ic_play_24, requireContext().theme)
        pauseDrawable = resources.getDrawable(R.drawable.ic_pause_24, requireContext().theme)
        playButton = view.findViewById(R.id.play)
        trackNameView = view.findViewById(R.id.track_name)
        trackArtistsView = view.findViewById(R.id.artists)

        with(playButton) {
            setOnClickListener {
                if (isPlaying) {
                    isPlaying = false

                    icon = playDrawable

                    viewModel.pause()
                } else {
                    isPlaying = true

                    icon = pauseDrawable

                    viewModel.play()
                }
            }
        }

        viewModel.track.observe(viewLifecycleOwner) {
            if (it != null) {
                trackNameView.text = it.track.name
                trackArtistsView.text = it.track.artists.joinToString { it.name }

                trackNameView.isSelected = true
                trackArtistsView.isSelected = true

                applyColors(it.thumbnail!!)
            }
        }
    }

    private fun applyColors(bitmap: Bitmap) {
        val colorSet = create(bitmap)

        val primaryAnimator =
            ValueAnimator.ofObject(ArgbEvaluator(), this.colorSet.primary, colorSet.primary)

        primaryAnimator.duration =
            resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

        primaryAnimator.addUpdateListener {
            this.colorSet.primary = it.animatedValue as Int
            trackNameView.setTextColor(this.colorSet.primary)
            playButton.iconTint = ColorStateList.valueOf(this.colorSet.primary)
        }

        primaryAnimator.start()
        val secondaryAnimator =
            ValueAnimator.ofObject(ArgbEvaluator(), this.colorSet.secondary, colorSet.secondary)
        secondaryAnimator.duration =
            resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
        secondaryAnimator.addUpdateListener {
            this.colorSet.secondary = it.animatedValue as Int
            trackArtistsView.setTextColor(this.colorSet.secondary)
        }
        secondaryAnimator.start()
        val tertiaryAnimator =
            ValueAnimator.ofObject(ArgbEvaluator(), this.colorSet.tertiary, colorSet.tertiary)
        tertiaryAnimator.duration =
            resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
        tertiaryAnimator.addUpdateListener {
            this.colorSet.tertiary = it.animatedValue as Int
        }
        tertiaryAnimator.start()
        val rippleAnimator =
            ValueAnimator.ofObject(ArgbEvaluator(), this.colorSet.ripple, colorSet.ripple)
        rippleAnimator.duration =
            resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
        rippleAnimator.addUpdateListener {
            this.colorSet.ripple = it.animatedValue as Int
            playButton!!.rippleColor = ColorStateList.valueOf(this.colorSet.ripple)
        }
        rippleAnimator.start()
        this.colorSet = colorSet
    }
}