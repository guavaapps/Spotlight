package com.guavaapps.spotlight;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.pixel.spotifyapi.Objects.ArtistSimple;
import com.spotify.android.appremote.api.PlayerApi;
import com.spotify.android.appremote.api.SpotifyAppRemote;

import java.util.ArrayList;
import java.util.List;

public class TrackSmallFragment extends Fragment {
    private boolean mIsPlaying = false;

    private ContentViewModel mViewModel;
    private SpotifyAppRemote mSpotifyAppRemote;
    private PlayerApi mPlayerApi;

    private TrackWrapper mTrack;

    private ColorSet mColorSet = new ColorSet ();

    private Drawable mPlayDrawable;
    private Drawable mPauseDrawable;

    private MaterialButton mPlayButton;
    private TextView mTrackNameView;
    private TextView mTrackArtistsView;

    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container,
                              Bundle savedInstanceState) {
        return inflater.inflate (R.layout.fragment_track_small, container, false);
    }

    @Override
    public void onViewCreated (@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated (view, savedInstanceState);

        mViewModel = new ViewModelProvider (requireActivity ()).get (ContentViewModel.class);

        mPlayDrawable = getResources ().getDrawable (R.drawable.ic_play_24, getContext ().getTheme ());
        mPauseDrawable = getResources ().getDrawable (R.drawable.ic_pause_24, getContext ().getTheme ());

        mPlayButton = view.findViewById (R.id.play);
        mTrackNameView = view.findViewById (R.id.track_name);
        mTrackArtistsView = view.findViewById (R.id.artists);

        mPlayButton.setOnClickListener (v -> {
            if (mIsPlaying) {
                mIsPlaying = false;
                mPlayButton.setIcon (mPlayDrawable);
                mViewModel.pause ();
            } else {
                mIsPlaying = true;
                mPlayButton.setIcon (mPauseDrawable);
                mViewModel.play ();
            }
        });

        mViewModel.getTrack ().observe (getViewLifecycleOwner (), trackWrapper -> {
            if (trackWrapper != null) {
                mTrack = trackWrapper;

                mTrackNameView.setText (trackWrapper.track.name);

                List <String> artists = new ArrayList <> ();

                for (ArtistSimple artistSimple : trackWrapper.track.artists) {
                    artists.add (artistSimple.name);
                }

                mTrackArtistsView.setText (String.join (", ", artists));
                mTrackNameView.setSelected (true);
                nextTrack (trackWrapper);
            }
        });

        mViewModel.getSpotifyAppRemote ().observe (getViewLifecycleOwner (), appRemote -> {
            mSpotifyAppRemote = appRemote;
            mPlayerApi = mSpotifyAppRemote.getPlayerApi ();
            mPlayerApi.subscribeToPlayerState ().setEventCallback (data -> {
                if (data.track.uri.equals (mTrack.track.uri)) {
                    if (data.isPaused) {
                        mPlayButton.setIcon (mPlayDrawable);
                    } else {
                        mPlayButton.setIcon (mPauseDrawable);
                    }
                }
            });
        });
    }

    private void nextTrack (TrackWrapper wrappedTrack) {
        Bitmap bitmap = wrappedTrack.thumbnail;
        ColorSet colorSet = ColorSet.Companion.create (bitmap);

        ValueAnimator primaryAnimator = ValueAnimator.ofObject (new ArgbEvaluator (), mColorSet.getPrimary (), colorSet.getPrimary ());
        primaryAnimator.setDuration (getResources ().getInteger (android.R.integer.config_shortAnimTime));
        primaryAnimator.addUpdateListener (animation -> {
            mColorSet.setPrimary ((int) animation.getAnimatedValue ());

            mTrackNameView.setTextColor (mColorSet.getPrimary ());
            mPlayButton.setIconTint (ColorStateList.valueOf (mColorSet.getPrimary ()));
        });
        primaryAnimator.start ();

        ValueAnimator secondaryAnimator = ValueAnimator.ofObject (new ArgbEvaluator (), mColorSet.getSecondary (), colorSet.getSecondary ());
        secondaryAnimator.setDuration (getResources ().getInteger (android.R.integer.config_shortAnimTime));
        secondaryAnimator.addUpdateListener (animation -> {
            mColorSet.setSecondary ((int) animation.getAnimatedValue ());

            mTrackArtistsView.setTextColor (mColorSet.getSecondary ());
        });
        secondaryAnimator.start ();

        ValueAnimator tertiaryAnimator = ValueAnimator.ofObject (new ArgbEvaluator (), mColorSet.getTertiary (), colorSet.getTertiary ());
        tertiaryAnimator.setDuration (getResources ().getInteger (android.R.integer.config_shortAnimTime));
        tertiaryAnimator.addUpdateListener (animation -> {
            mColorSet.setTertiary ((int) animation.getAnimatedValue ());
        });
        tertiaryAnimator.start ();

        ValueAnimator rippleAnimator = ValueAnimator.ofObject (new ArgbEvaluator (), mColorSet.getRipple (), colorSet.getRipple ());
        rippleAnimator.setDuration (getResources ().getInteger (android.R.integer.config_shortAnimTime));
        rippleAnimator.addUpdateListener (animation -> {
            mColorSet.setRipple ((int) animation.getAnimatedValue ());

            mPlayButton.setRippleColor (ColorStateList.valueOf (mColorSet.getRipple ()));
        });
        rippleAnimator.start ();

        mColorSet = colorSet;
    }
}