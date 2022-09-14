package com.guavaapps.spotlight;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.guavaapps.components.Components;
import com.pixel.spotifyapi.Objects.Album;
import com.pixel.spotifyapi.Objects.AlbumSimple;
import com.pixel.spotifyapi.Objects.Artist;
import com.pixel.spotifyapi.Objects.ArtistSimple;
import com.pixel.spotifyapi.Objects.Track;
import com.pixel.spotifyapi.Objects.TrackSimple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class ExtraFragment extends Fragment {
    private static final String TAG = "ExtraFragment";

    private ContentViewModel mViewModel;

    private Insets mInsets;

    private ColorSet mColorSet = new ColorSet ();

    private TrackWrapper mTrack;
    private AlbumWrapper mAlbum;
    private List <ArtistWrapper> mArtists = new ArrayList <> ();
    private Adapter mAdapter;

    private TabLayout mTabLayout;
    private ViewPager2 mViewPager;

    private AlbumFragment mAlbumFragment;
    private List <ArtistFragment> mArtistFragments = new ArrayList <> ();

    public ExtraFragment () {

    }

    @Override
    public void onCreate (Bundle savedInstanceState) {
        super.onCreate (savedInstanceState);
    }

    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container,
                              Bundle savedInstanceState) {

        return inflater.inflate (R.layout.fragment_extra, container, false);
    }

    @Override
    public void onViewCreated (@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated (view, savedInstanceState);

        mViewModel = new ViewModelProvider (requireActivity ()).get (ContentViewModel.class);

        mTabLayout = view.findViewById (R.id.tab_layout);
        mViewPager = view.findViewById (R.id.view_pager);

        mTabLayout.setTabMode (TabLayout.MODE_SCROLLABLE);
        mTabLayout.setTabGravity (TabLayout.GRAVITY_CENTER);

        mAlbumFragment = new AlbumFragment ();
        mAlbumFragment.setListener ((track -> {
            mViewModel.getSpotifyService ().getValue ()
                    .getTrack (track.id, new Callback <Track> () {
                        @Override
                        public void success (Track track, Response response) {
                            TrackWrapper wrappedTrack = new TrackWrapper (track, mViewModel.getAlbum ().getValue ().bitmap);
                            mViewModel.setTrack (wrappedTrack);

                            mViewModel.getSpotifyAppRemote ().getValue ()
                                    .getPlayerApi ()
                                    .play (track.uri);
                        }

                        @Override
                        public void failure (RetrofitError error) {

                        }
                    });
        }));

        mAdapter = new Adapter (requireActivity ());

        getView ().addOnLayoutChangeListener ((new View.OnLayoutChangeListener () {
            @Override
            public void onLayoutChange (View view, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                ViewCompat.setOnApplyWindowInsetsListener (getView (), (v, windowInsetsCompat) -> {
                    mInsets = windowInsetsCompat.getInsets (WindowInsetsCompat.Type.systemBars ());

                    ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams ();
                    params.topMargin = Components.INSTANCE.getPx (getContext (), 128 + 48) + mInsets.top;
                    params.bottomMargin = mInsets.bottom;
                    view.setLayoutParams (params);

                    return WindowInsetsCompat.CONSUMED;
                });

                view.removeOnLayoutChangeListener (this);
            }
        }));

        mViewModel.getTrack ().observe (getViewLifecycleOwner (), trackWrapper -> {
            if (trackWrapper != null) {
                mTrack = trackWrapper;

                if (mAlbumFragment.getAlbum () == null || !mAlbumFragment.getAlbum ().album.id.equals (mTrack.track.album.id)) {
                    AppRepo.getInstance () // TODO use view model
                            .getAlbum (mViewModel.getSpotifyService ().getValue (), getContext (),
                                    trackWrapper.track.album.id,
                                    new AppRepo.ResultListener () {
                                        @Override
                                        public void onAlbum (AlbumWrapper albumWrapper) {
                                            super.onAlbum (albumWrapper);

                                            Handler.createAsync (Looper.getMainLooper ())
                                                    .post (() -> {
                                                        mViewPager.setAdapter (mAdapter);
                                                        new TabLayoutMediator (mTabLayout, mViewPager, (tab, position) -> {
                                                            if (position == 0) {
                                                                tab.setText ("From " + mTrack.track.album.name);
                                                            } else
                                                                tab.setText (mArtists.get (position - 1).artist.name);
                                                        }).attach ();
                                                    });
                                        }
                                    });
                }
            }
        });

        mViewModel.getAlbum ().observe (getViewLifecycleOwner (), albumWrapper -> {
            if (albumWrapper != null) {
                mAlbum = albumWrapper;
                nextAlbum (albumWrapper);

                List <Fragment> tabs = new ArrayList <> ();
                tabs.add (mAlbumFragment);
                mArtists.clear ();

                for (ArtistSimple artistSimple : getAllArtists (mAlbum.album)) {//mAlbum.album.artists) {
                    Artist artist = new Artist ();
                    artist.id = artistSimple.id;
                    artist.name = artistSimple.name;
                    mArtists.add (new ArtistWrapper (artist, null));
                    tabs.add (new ArtistFragment ());
                }

                mAdapter.setItems (tabs);
            }
        });
    }

    private static List <ArtistSimple> getAllArtists (Album album) {
        List <ArtistSimple> artists = new ArrayList <> ();
        Map <String, Integer> quantitizedMap = new HashMap <> ();

        for (TrackSimple trackSimple : album.tracks.items) {
            for (ArtistSimple artistSimple : trackSimple.artists) {
                if (quantitizedMap.keySet ().contains (artistSimple.id)) {
//                    int v = quantitizedMap.get (artistSimple);
//
//                    quantitizedMap.put (artistSimple, v++);
                } else {
                    artists.add (artistSimple);
                    quantitizedMap.put (artistSimple.id, 1);
                }
            }
        }

        return artists;
    }

    @Override
    public void onResume () {
        super.onResume ();
    }

    @Override
    public void onPause () {
        super.onPause ();
    }

    @Override
    public void onDestroy () {
        super.onDestroy ();
    }

    private void nextAlbum (AlbumWrapper wrappedAlbum) {
        Album album = wrappedAlbum.album;
        Bitmap bitmap = wrappedAlbum.bitmap;
        ColorSet colorSet = ColorSet.Companion.create (bitmap);

        SpannableString albumTabTitle = new SpannableString ("From " + album.name);

        ValueAnimator primaryAnimator = ValueAnimator.ofObject (new ArgbEvaluator (), mColorSet.getPrimary (), colorSet.getPrimary ());
        primaryAnimator.setDuration (getResources ().getInteger (android.R.integer.config_shortAnimTime));
        primaryAnimator.addUpdateListener (animation -> {
            mColorSet.setPrimary ((int) animation.getAnimatedValue ());

            mTabLayout.setTabTextColors (mColorSet.getPrimary (), (int) animation.getAnimatedValue ());

            mTabLayout.setSelectedTabIndicatorColor (mColorSet.getPrimary ());

            albumTabTitle.setSpan (new ForegroundColorSpan (mColorSet.getPrimary ()), 5, albumTabTitle.length (), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        });
        primaryAnimator.start ();

        ValueAnimator secondaryAnimator = ValueAnimator.ofObject (new ArgbEvaluator (), mColorSet.getSecondary (), colorSet.getSecondary ());
        secondaryAnimator.setDuration (getResources ().getInteger (android.R.integer.config_shortAnimTime));
        secondaryAnimator.addUpdateListener (animation -> {
            mColorSet.setPrimary ((int) animation.getAnimatedValue ());

            mTabLayout.setTabTextColors (mColorSet.getSecondary (), mColorSet.getPrimary ());

            albumTabTitle.setSpan (new ForegroundColorSpan (mColorSet.getSecondary ()), 0, 4, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
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

            mTabLayout.setTabRippleColor (new ColorStateList (new int[][] {{}}, new int[] {mColorSet.getRipple ()}));
        });
        rippleAnimator.start ();

        mColorSet = colorSet;
    }
}