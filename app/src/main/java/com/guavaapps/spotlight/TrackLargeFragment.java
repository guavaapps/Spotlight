package com.guavaapps.spotlight;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatSeekBar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.pixel.spotifyapi.Objects.Track;
import com.spotify.android.appremote.api.PlayerApi;
import com.spotify.android.appremote.api.SpotifyAppRemote;

import java.util.concurrent.ScheduledExecutorService;

public class TrackLargeFragment extends Fragment {

    private ContentViewModel mViewModel;

    private SpotifyAppRemote mSpotifyAppRemote;
    private PlayerApi mPlayerApi;
    private TrackWrapper mTrack;

    private ColorSet mColorSet = new ColorSet ();

    private ScheduledExecutorService mPlayerStateListener;
    private boolean mIsPlaying = false;
    private long mProgress = 0;

    private Drawable mPlayDrawable;
    private Drawable mPauseDrawable;

    private TextView mTrackNameView;
    private MaterialButton mPlayButton;
    private AppCompatSeekBar mSeekBar;
    private TextView mProgressView;
    private TextView mDurationView;
    private MaterialButton mSpotifyButton;

    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container,
                              Bundle savedInstanceState) {
        return inflater.inflate (R.layout.fragment_track_large, container, false);
    }

    @Override
    public void onViewCreated (@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated (view, savedInstanceState);

        mPlayDrawable = getResources ().getDrawable (R.drawable.ic_play_24, getContext ().getTheme ());
        mPauseDrawable = getResources ().getDrawable (R.drawable.ic_pause_24, getContext ().getTheme ());

        mTrackNameView = view.findViewById (R.id.track_name_view);
        mPlayButton = view.findViewById (R.id.play_button);
        mSeekBar = view.findViewById (R.id.seek_bar);
        mProgressView = view.findViewById (R.id.progress_view);
        mDurationView = view.findViewById (R.id.duration_view);

        mSpotifyButton = view.findViewById (R.id.spotify_button);

        mPlayButton.setOnClickListener (v -> {
            if (!mIsPlaying) {
                mIsPlaying = true;
                mPlayButton.setIcon (mPauseDrawable);

                mViewModel.play ();
            } else {
                mIsPlaying = false;
                mPlayButton.setIcon (mPlayDrawable);

                mViewModel.pause ();
            }
        });

        mSeekBar.setOnSeekBarChangeListener (new SeekBar.OnSeekBarChangeListener () {
            @Override
            public void onProgressChanged (SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) mViewModel.setProgress (progress);
            }

            @Override
            public void onStartTrackingTouch (SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch (SeekBar seekBar) {
                mViewModel.getSpotifyAppRemote ().getValue ()
                        .getPlayerApi ()
                        .seekTo (mProgress);
            }
        });

        //

        mViewModel = new ViewModelProvider (requireActivity ()).get (ContentViewModel.class);

        mViewModel.getSpotifyAppRemote ().observe (getViewLifecycleOwner (), appRemote -> {
            mSpotifyAppRemote = appRemote;
            mPlayerApi = appRemote.getPlayerApi ();
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

        mViewModel.getTrack ().observe (getViewLifecycleOwner (), trackWrapper -> {
            if (trackWrapper != null) {
                mTrack = trackWrapper;
                nextTrack (trackWrapper);
                mSeekBar.setMax ((int) mTrack.track.duration_ms);
            }
        });

        mViewModel.getNextTrack ().observe (getViewLifecycleOwner (), trackWrapper -> {

        });

        mViewModel.getProgress ().observe (getViewLifecycleOwner (), progress -> {
            setProgress (progress);
            setSeekBar (progress);
        });
    }

    private void nextTrack (TrackWrapper wrappedTrack) {
        Track track = wrappedTrack.track;
        Bitmap bitmap = wrappedTrack.thumbnail;
        ColorSet colorSet = ColorSet.Companion.create (bitmap);

        SpannableString trackName = new SpannableString (track.name + " " + track.artists.get (0).name);
        SpannableString albumTabTitle = new SpannableString ("From " + track.album.name);

        ValueAnimator primaryAnimator = ValueAnimator.ofObject (new ArgbEvaluator (), mColorSet.getPrimary (), colorSet.getPrimary ());
        primaryAnimator.setDuration (getResources ().getInteger (android.R.integer.config_shortAnimTime));
        primaryAnimator.addUpdateListener (animation -> {
            mColorSet.setPrimary ((int) animation.getAnimatedValue ());

            trackName.setSpan (new ForegroundColorSpan (mColorSet.getPrimary ()), 0, track.name.length (), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            mTrackNameView.setText (trackName);

            mPlayButton.setIconTint (ColorStateList.valueOf (mColorSet.getPrimary ()));
            mSpotifyButton.setBackgroundColor (mColorSet.getPrimary ());

            mSeekBar.setProgressTintList (ColorStateList.valueOf (mColorSet.getPrimary ()));
            mSeekBar.setThumbTintList (ColorStateList.valueOf (mColorSet.getPrimary ()));

//            mTabLayout.setTabTextColors (mSecondaryColor, (int) animation.getAnimatedValue ());
//
//            mTabLayout.setSelectedTabIndicatorColor (mPrimaryColor);

//            albumTabTitle.setSpan (new ForegroundColorSpan (mPrimaryColor), 5, albumTabTitle.length (), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        });
        primaryAnimator.start ();

        ValueAnimator onPrimaryAnimator = ValueAnimator.ofObject (new ArgbEvaluator (), mColorSet.getText (), colorSet.getText ());
        onPrimaryAnimator.setDuration (getResources ().getInteger (android.R.integer.config_shortAnimTime));
        onPrimaryAnimator.addUpdateListener (animation -> {
            mColorSet.setText ((int) animation.getAnimatedValue ());

            mSpotifyButton.setTextColor (mColorSet.getText ());
        });
        onPrimaryAnimator.start ();

        ValueAnimator secondaryAnimator = ValueAnimator.ofObject (new ArgbEvaluator (), mColorSet.getSecondary (), colorSet.getSecondary ());
        secondaryAnimator.setDuration (getResources ().getInteger (android.R.integer.config_shortAnimTime));
        secondaryAnimator.addUpdateListener (animation -> {
            mColorSet.setSecondary ((int) animation.getAnimatedValue ());

            trackName.setSpan (new ForegroundColorSpan (mColorSet.getSecondary ()), track.name.length () + 1, trackName.length (), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            mTrackNameView.setText (trackName);
//            mTrackNameView.setAutoSizeTextTypeUniformWithConfiguration (mMaxH / 2, mMaxH, 1, TypedValue.COMPLEX_UNIT_PX);

//            mExtraArtistsView.setText ("Extra Artists");
//            mExtraArtistsView.setTextColor (mSecondaryColor);

//            mTabLayout.setTabTextColors (mSecondaryColor, mPrimaryColor);

//            albumTabTitle.setSpan (new ForegroundColorSpan (mSecondaryColor), 0, 4, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        });
        secondaryAnimator.start ();

        ValueAnimator tertiaryAnimator = ValueAnimator.ofObject (new ArgbEvaluator (), mColorSet.getTertiary (), colorSet.getTertiary ());
        tertiaryAnimator.setDuration (getResources ().getInteger (android.R.integer.config_shortAnimTime));
        tertiaryAnimator.addUpdateListener (animation -> {
            mColorSet.setTertiary ((int) animation.getAnimatedValue ());

            mSeekBar.setProgressBackgroundTintList (ColorStateList.valueOf (mColorSet.getTertiary ()));

            mProgressView.setTextColor (mColorSet.getTertiary ());
            mDurationView.setTextColor (mColorSet.getTertiary ());
        });
        tertiaryAnimator.start ();

        ValueAnimator rippleAnimator = ValueAnimator.ofObject (new ArgbEvaluator (), mColorSet.getRipple (), colorSet.getRipple ());
        rippleAnimator.setDuration (getResources ().getInteger (android.R.integer.config_shortAnimTime));
        rippleAnimator.addUpdateListener (animation -> {
            mColorSet.setRipple ( (int) animation.getAnimatedValue ());

            mPlayButton.setRippleColor (ColorStateList.valueOf (mColorSet.getRipple ()));
//            mTabLayout.setTabRippleColor (new ColorStateList (new int[][] {{}}, new int[] {mRippleColor}));
        });
        rippleAnimator.start ();

        String progressString = new TimeString.Builder (0)
                .minutes ()
                .separator (":")
                .seconds ("%02d")
                .build ()
                .toString ();

        String durationString = new TimeString.Builder (track.duration_ms)
                .minutes ()
                .separator (":")
                .seconds ("%02d")
                .build ()
                .toString ();

        mProgressView.setText (progressString);
        mDurationView.setText (durationString);

        mColorSet = colorSet;
    }

    public void setProgress (long progress) {
        String progressString = new TimeString.Builder (progress)
                .minutes ()
                .separator (":")
                .seconds ("%02d")
                .build ()
                .toString ();

        mProgressView.setText (progressString);
    }

    public void setSeekBar (long progress) {
        mSeekBar.setProgress ((int) progress, true);
    }
}