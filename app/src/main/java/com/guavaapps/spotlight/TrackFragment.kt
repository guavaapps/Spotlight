package com.guavaapps.spotlight;

import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_MOVE;
import static android.view.MotionEvent.ACTION_UP;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;
import androidx.lifecycle.ViewModelProvider;

import com.guavaapps.components.Components;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class TrackFragment extends Fragment {
    private static final int MAX_TRACK_VIEW_SIZE_DP = 256;

    private ContentViewModel mViewModel;

    private Insets mInsets;
    private FragmentContainerView mTrackLargeContainer;
    private FragmentContainerView mTrackSmallContainer;
    private int mTrackViewSize;

    private Queue <TrackWrapper> mTracks = new LinkedList <> ();

//    private ImageView mUserView;

    private ImageView mTrackView;
    private ImageView mNextTrackView;
    private ImageView mPlaylistView;

    private boolean mHasTrack = true;

    //media

    private static final String TAG = "TrackFragment";

    private OnTouchListener mOnTouchListener;
    private TrackLargeFragment mTrackLargeFragment = new TrackLargeFragment ();
    private TrackSmallFragment mTrackSmallFragment = new TrackSmallFragment ();

    public TrackFragment () {

    }

    @Override
    public void onCreate (Bundle savedInstanceState) {
        super.onCreate (savedInstanceState);
    }

    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container,
                              Bundle savedInstanceState) {

        return inflater.inflate (R.layout.fragment_track, container, false);
    }

    @Override
    public void onViewCreated (@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated (view, savedInstanceState);

//        mAlbumTracksListView = view.findViewById (R.id.album_tracks_list_view);
//        mAlbumTracksListView.setLayoutManager (new LinearLayoutManager (getContext ()));
//        PagerSnapHelper snapHelper = new PagerSnapHelper ();
//        snapHelper.attachToRecyclerView (mAlbumTracksListView);


        mTrackLargeContainer = view.findViewById (R.id.track_large_container);
        mTrackSmallContainer = view.findViewById (R.id.track_small_container);

        mTrackView = view.findViewById (R.id.track_view);
        mNextTrackView = view.findViewById (R.id.next_track_view);
        mPlaylistView = view.findViewById (R.id.playlist_view);

        getView ().addOnLayoutChangeListener ((new View.OnLayoutChangeListener () {
            @Override
            public void onLayoutChange (View view, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                int viewWidth = getView ().getWidth ();
                int viewHeight = getView ().getHeight ();

                ViewCompat.setOnApplyWindowInsetsListener (getView (), (v, windowInsetsCompat) -> {
                    mInsets = windowInsetsCompat.getInsets (WindowInsetsCompat.Type.systemBars ());

                    initViews (viewWidth, viewHeight);

                    ViewCompat.setOnApplyWindowInsetsListener (v, null);

                    return WindowInsetsCompat.CONSUMED;
                });

                view.removeOnLayoutChangeListener (this);
            }
        }));
    }

    @Override
    public void onResume () {
        super.onResume ();
    }

    public void initViews (int viewWidth, int viewHeight) {
//        mPeekViewWidth = mPeekView.getWidth ();
//        mPeekViewHeight = mPeekView.getHeight ();

//        int rawSize = 2 * (viewHeight / 2 - mPeekViewHeight
//                - Components.INSTANCE.getPx (getContext (), 48) // padding
//                - mInsets.bottom);
        int rawSize = 100000;

        mTrackViewSize = rawSize < Components.INSTANCE.getPx (getContext (), MAX_TRACK_VIEW_SIZE_DP) ? rawSize : Components.INSTANCE.getPx (getContext (), MAX_TRACK_VIEW_SIZE_DP);

//        mPeekViewSmallWidth = viewWidth - Components.INSTANCE.getPx (getContext (), 128) - Components.INSTANCE.getPx (getContext (), 48);

//        mUserView.setX (viewWidth - mUserView.getWidth () - Components.INSTANCE.getPx (getContext (), 24));
//        mUserView.setY (Components.INSTANCE.getPx (getContext (), 24) + mInsets.top);
//        mUserView.setOnClickListener (v -> {
//
//        });
//        mUserView.setOutlineProvider (new ViewOutlineProvider () {
//            @Override
//            public void getOutline (View view, Outline outline) {
//                mUserView.setClipToOutline (true);
//                outline.setOval (0,
//                        0,
//                        view.getWidth (),
//                        view.getHeight ()
//                );
//            }
//        });

        mTrackView.setLayoutParams (new ConstraintLayout.LayoutParams (mTrackViewSize, mTrackViewSize));
        mNextTrackView.setLayoutParams (new ConstraintLayout.LayoutParams (mTrackViewSize, mTrackViewSize));

        mTrackSmallContainer.setLayoutParams (new ConstraintLayout.LayoutParams (
                viewWidth - Components.INSTANCE.getPx (getContext (), 128 + 48 + 24),
                -2
        ));

        try { // TODO init in layout
            List <Fragment> fragments = requireActivity ().getSupportFragmentManager ().getFragments ();

            boolean s = false;
            boolean l = false;

            for (Fragment f : fragments) {
                if (f.getClass ().equals (TrackSmallFragment.class)) s = true;
                if (f.getClass ().equals (TrackLargeFragment.class)) l = true;
            }

            if (s || l) throw new Exception ();

//            requireActivity ().getSupportFragmentManager ().beginTransaction ()
//                    .add (mTrackLargeContainer.getId (), mTrackLargeFragment)
//                    .commit ();
//
//            requireActivity ().getSupportFragmentManager ().beginTransaction ()
//                    .add (mTrackSmallContainer.getId (), mTrackSmallFragment)
//                    .commit ();
        }
        catch (Exception e) {
            e.printStackTrace ();
        }

        mTrackSmallContainer.setX (Components.INSTANCE.getPx (getContext (), 128 + 48));

        int y = (Components.INSTANCE.getPx (getContext (), 128) - mTrackSmallContainer.getHeight ()) / 2 + mInsets.top;

        mTrackSmallContainer.setY (y);

        mTrackView.setX (viewWidth / 2 - mTrackViewSize / 2);
        mTrackView.setY (viewHeight / 2 - mTrackViewSize / 2);
        mOnTouchListener = new OnTouchListener (viewWidth, viewHeight, mNextTrackView);
        mTrackView.setOnTouchListener (mOnTouchListener);

        mNextTrackView.setX (viewWidth / 2 - mTrackViewSize / 2);
        mNextTrackView.setY (viewHeight / 2 - mTrackViewSize / 2);
        mNextTrackView.setScaleX (0.5f);
        mNextTrackView.setScaleY (0.5f);
        mNextTrackView.setAlpha (0.5f);

        mViewModel = new ViewModelProvider (requireActivity ()).get (ContentViewModel.class);

        mViewModel.getUser ().observe (getViewLifecycleOwner (), userWrapper -> {
//            mUserView.setImageBitmap (userWrapper.thumbnail);
        });

        mViewModel.getTrack ().observe (getViewLifecycleOwner (), trackWrapper -> {
            if (trackWrapper != null) {
                mTrackView.setImageBitmap (trackWrapper.thumbnail);
                //nextTrack (trackWrapper);
                mHasTrack = true;
            } else {
                ColorDrawable drawable = new ColorDrawable ();
                drawable.setColor (Color.MAGENTA);
                mTrackView.setImageDrawable (drawable);
                mHasTrack = false;
            }
        });

        mViewModel.getNextTrack ().observe (getViewLifecycleOwner (), trackWrapper -> {
            if (trackWrapper != null) {
                mNextTrackView.setImageBitmap (trackWrapper.thumbnail);
            } else {
                ColorDrawable drawable = new ColorDrawable ();
                drawable.setColor (Color.MAGENTA);
                mTrackView.setImageDrawable (drawable);
            }
        });
    }

    public void resizeX (float offset) {
        float startX = getView ().getWidth () / 2 - mTrackView.getWidth () / 2;
        float endX = Components.INSTANCE.getPx (getContext (), 24);

        float d = endX - startX;

        float p = offset * d;

        float newX = startX + p;

        mTrackView.setX (newX);
    }

    public void resizeY (float offset) {
        float startY = getView ().getHeight () / 2 - mTrackView.getHeight () / 2;
        float endY = Components.INSTANCE.getPx (getContext (), 24) + mInsets.top;

        float d = endY - startY;

        float p = offset * d;

        float newY = startY + p;

        mTrackView.setY (newY);
    }

    public void resizeScale (float offset) {
        float start = mTrackViewSize;
        float end = Components.INSTANCE.getPx (getContext (), 128);

        float d = end - start;

        float p = offset * d;

        int newSize = (int) (start + p);

        ViewGroup.LayoutParams params = mTrackView.getLayoutParams ();
        params.width = newSize;
        params.height = newSize;
        mTrackView.setLayoutParams (params);
    }

    public void alphaSmall (float offset) {
        if (offset >= 0.5f) mTrackSmallContainer.setVisibility (View.VISIBLE);
        else mTrackSmallContainer.setVisibility (View.GONE);

        float start = 0;
        float end = 1;

        float d = end - start;

        float o = offset >= 0.5f ? (offset - 0.5f) / 0.5f : 0;

        float p = o * d;

        float newSize = start + p;

        mTrackSmallContainer.setAlpha (newSize);
    }

    public void alphaLarge (float offset) {
        if (offset <= 0.5f) mTrackLargeContainer.setVisibility (View.VISIBLE);
        else mTrackLargeContainer.setVisibility (View.GONE);

        float start = 1;
        float end = 0;

        float d = end - start;

        float o = offset <= 0.5f ? offset / 0.5f : 1;

        float p1 = offset * d;
        float p2 = o * d;

        float newSize1 = start + p1;
        float newSize2 = start + p2;

        mTrackLargeContainer.setAlpha (newSize2);
    }

    public void resize (float offset) {
        if (offset == 0) mNextTrackView.setVisibility (View.VISIBLE);
        else mNextTrackView.setVisibility (View.GONE);

        resizeX (offset);
        resizeY (offset);
        resizeScale (offset);
        alphaSmall (offset);
        alphaLarge (offset);
    }

    @Override
    public void onPause () {
        super.onPause ();
    }

    @Override
    public void onDestroy () {
        super.onDestroy ();
    }


    private boolean isWaiting = false;

    // media

    public void setTrack (TrackWrapper wrappedTrack) {

    }

//    private ColorSet createColorSet (Bitmap bitmap) {
//        ColorSet colorSet = new ColorSet ();
//
//        int color = Palette.from (bitmap).generate ()
//                .getDominantSwatch ()
//                .getRgb ();
//
//        Hct surfaceColor = Hct.fromInt (color);
//        surfaceColor.setTone (10);
//        colorSet.surface = surfaceColor.toInt ();
//
//        Hct primaryColor = Hct.fromInt (color);
//        primaryColor.setTone (90);
//        colorSet.primary = primaryColor.toInt ();
//
//        Argb c = Argb.from (primaryColor.toInt ());
//        c.setAlpha (0.6f * 255);
//        colorSet.secondary = c.toInt ();
//
//        c.setAlpha (0.24f * 255);
//        colorSet.tertiary = c.toInt ();
//
//        c.setAlpha (0.16f * 255);
//        colorSet.ripple = c.toInt ();
//
//        return colorSet;
//    }

    private class OnTouchListener implements View.OnTouchListener {
        private static final String TAG = "OTL";

        private final int mViewWidth;
        private final int mViewHeight;
        private final int mPeekBound;
        private final int mDismissBound;

        private ImageView mNextTrackView;

        private final int mMaxSx;
        private final int mMaxSy;
        private final int mMaxI;

        private float mX;
        private float mY;

        private float mTx;
        private float mTy;

        private float mDx;
        private float mDy;

        private float mSx;
        private float mSy;

        private float mPx;

        private float mIx;
        private float mIy;
        private Object mIdleLock;
        private Object mPeekLock = new Object ();
        private Object mDismissLock = new Object ();

        private ValueAnimator mRxAnimator;
        private ValueAnimator mIxAnimator;

        private ValueAnimator mNextRxAnimator;

        private ValueAnimator mXAnimator;
        private ValueAnimator mYAnimator;

        public OnTouchListener (int viewWidth, int viewHeight, ImageView nextTrackView) {
            mViewWidth = viewWidth;
            mViewHeight = viewHeight;
            mNextTrackView = nextTrackView;

            mPeekBound = mViewWidth - (viewWidth - Components.INSTANCE.getPx (getContext (), 256)) / 2;//(int) (0.75f * viewWidth);
            mDismissBound = (viewWidth - Components.INSTANCE.getPx (getContext (), 256)) / 2;

            mMaxSx = Components.INSTANCE.getPx (getContext (), 64);
            mMaxSy = Components.INSTANCE.getPx (getContext (), 256);
            mMaxI = mViewWidth - Components.INSTANCE.getPx (getContext (), 384);
        }

        @Override
        public boolean onTouch (View v, MotionEvent event) {
            if (event.getAction () == ACTION_DOWN) {
//                mParent.setUserInputEnabled (false);
                getView ().getParent ().requestDisallowInterceptTouchEvent (true);

                if (mXAnimator != null && mXAnimator.isRunning ()) mXAnimator.cancel ();
                if (mYAnimator != null && mYAnimator.isRunning ()) mYAnimator.cancel ();

                mX = v.getX ();
                mY = v.getY ();

                mTx = event.getRawX ();
                mTy = event.getRawY ();

                mDx = v.getX () - event.getRawX ();
                mDy = v.getY () - event.getRawY ();
            }

            if (event.getAction () == ACTION_MOVE) {
                mSy = (event.getRawY () - mTy) / mViewHeight * mMaxSy;
                mSx = (event.getRawX () - mTx) / mViewHeight * mMaxSy;

                if (mHasTrack) {
                    mPx = mViewWidth - (v.getX () + v.getWidth () / 2 - mViewWidth / 2) / mViewWidth / 2 * Components.INSTANCE.getPx (getContext (), 256);

                    if (event.getRawX () + mDx + v.getWidth () / 2 > mDismissBound && event.getRawX () + mDx + v.getWidth () / 2 < mPeekBound) {
                        if (mIdleLock == null) {
                            // onIdle
                            mIdleLock = new Object (); // lock peek
                            mPeekLock = null; // unlock peek
                            mDismissLock = null;

                            inflateX (0);
                            resize (v, 1f);
                        }

                        mX = event.getRawX () + mDx + mIx;
                    }

                    if (event.getRawX () + mDx + v.getWidth () / 2 >= mPeekBound) {
                        mSx = (event.getRawX () + mDx + v.getWidth () / 2 - mPeekBound) / mViewWidth * mMaxSx;

                        if (mPeekLock == null) {
                            // onPeek
                            mPeekLock = new Object (); // lock peek
                            mIdleLock = null;
                            mDismissLock = null;

                            inflateX (mMaxI);
                            resize (v, 0.5f);
                        }

                        mX = mPeekBound - v.getWidth () / 2 + mIx + mSx;
                    }

                    if (event.getRawX () + mDx + v.getWidth () / 2 <= mDismissBound) {
                        mSx = (event.getRawX () + mDx + v.getWidth () / 2 - mDismissBound) / mViewWidth * mMaxSx;

                        if (mDismissLock == null) {
                            // onDismiss

                            mDismissLock = new Object ();
                            mIdleLock = null;
                            mPeekLock = null;

                            inflateX (-mMaxI);
                            resize (v, 0.5f);
                        }
                        mX = mDismissBound - v.getWidth () / 2 + mIx;
                    }
                }

                mX = mHasTrack ? mX : mViewWidth / 2 - v.getWidth () / 2 + mSx;
                v.setX (mX);
                mY = mViewHeight / 2 - v.getHeight () / 2 + mSy;
                v.setY (mY);

                float s = Math.abs (mViewWidth / 2 - mX - v.getWidth () / 2) / (mViewWidth / 2) * 0.25f;

                mNextTrackView.setScaleX (0.5f + s);
                mNextTrackView.setScaleY (0.5f + s);
                mNextTrackView.setAlpha (0.5f + s);

                mPlaylistView.setX (mPx);
            }
            if (event.getAction () == ACTION_UP) {
//                mParent.setUserInputEnabled (true);
                v.getParent ().requestDisallowInterceptTouchEvent (false);

                reset (v);
                resize (v, 1f);

                if (mHasTrack) {
                    if (event.getRawX () + mDx + v.getWidth () / 2 >= mPeekBound) {
                        deflateTrack (v);
                    }

                    if (event.getRawX () + mDx + v.getWidth () / 2 <= mDismissBound) {
                        deflateTrack (v);
                    }
                }
            }

            return true;
        }

        private void reset (View v) {
            int newX = mViewWidth / 2 - v.getWidth () / 2;
            int newY = mViewHeight / 2 - v.getHeight () / 2;

            if (mXAnimator != null && mXAnimator.isRunning ()) mXAnimator.cancel ();
            mXAnimator = ValueAnimator.ofFloat (mX, newX);
            mXAnimator.setDuration (getResources ().getInteger (android.R.integer.config_shortAnimTime));
            mXAnimator.addUpdateListener (animation -> {
                mX = (float) animation.getAnimatedValue ();

                mPx = mViewWidth - (mX + v.getWidth () / 2 - mViewWidth / 2) / mViewWidth / 2 * Components.INSTANCE.getPx (getContext (), 256);

                v.setX (mX);
                mPlaylistView.setX (mPx);
            });

            if (mYAnimator != null && mYAnimator.isRunning ()) mYAnimator.cancel ();
            mYAnimator = ValueAnimator.ofFloat (mY, newY);
            mYAnimator.setDuration (getResources ().getInteger (android.R.integer.config_shortAnimTime));
            mYAnimator.addUpdateListener (animation -> {
                mY = (float) animation.getAnimatedValue ();
                v.setY (mY);
            });

            mXAnimator.start ();
            mYAnimator.start ();
        }

        private void resize (View v, float newR) {
            if (mRxAnimator != null && mRxAnimator.isRunning ()) mRxAnimator.cancel ();
            mRxAnimator = ValueAnimator.ofFloat (v.getScaleX (), newR);
            mRxAnimator.setDuration (getResources ().getInteger (android.R.integer.config_shortAnimTime));
            mRxAnimator.addUpdateListener (animation -> {
                float r = (float) animation.getAnimatedValue ();
                v.setScaleX (r);
                v.setScaleY (r);
            });
            mRxAnimator.start ();
        }

        private void inflateX (float newIx) {
            if (mIxAnimator != null && mIxAnimator.isRunning ()) mIxAnimator.cancel ();
            mIxAnimator = ValueAnimator.ofFloat (mIx, newIx);
            mIxAnimator.setDuration (getResources ().getInteger (android.R.integer.config_shortAnimTime));
            mIxAnimator.addUpdateListener (new ValueAnimator.AnimatorUpdateListener () {
                @Override
                public void onAnimationUpdate (ValueAnimator animation) {
                    mIx = (float) animation.getAnimatedValue ();
                }
            });
            mIxAnimator.start ();
        }

        public void inflateNext () {
            mNextTrackView.animate ().alpha (1f)
                    .setDuration (getResources ().getInteger (android.R.integer.config_shortAnimTime))
                    .start ();

            if (mNextRxAnimator != null && mNextRxAnimator.isRunning ()) mNextRxAnimator.cancel ();
            mNextRxAnimator = ValueAnimator.ofFloat (mNextTrackView.getScaleX (), 1f);
            mNextRxAnimator.setDuration (getResources ().getInteger (android.R.integer.config_shortAnimTime));
            mNextRxAnimator.addUpdateListener (animation -> {
                float r = (float) animation.getAnimatedValue ();
                mNextTrackView.setScaleX (r);
                mNextTrackView.setScaleY (r);
            });
            mNextRxAnimator.addListener (new AnimatorListenerAdapter () {
                @Override
                public void onAnimationEnd (Animator animation) {
                    super.onAnimationEnd (animation);

                    int newX = getView ().getWidth () / 2 - mTrackView.getWidth () / 2;
                    int newY = getView ().getHeight () / 2 - mTrackView.getHeight () / 2;

                    mViewModel.nextTrack (requireActivity ());

                    mTrackView.setX (newX);
                    mTrackView.setY (newY);
                    mTrackView.setScaleX (1f);
                    mTrackView.setScaleY (1f);
                    mTrackView.setAlpha (1f);
                }
            });
            mNextRxAnimator.start ();
        }

        public void deflateTrack (View v) {
            mXAnimator.cancel ();
            mYAnimator.cancel ();
            mIxAnimator.cancel ();

            v.animate ()
                    .alpha (0f)
                    .setDuration (getResources ().getInteger (android.R.integer.config_shortAnimTime))
                    .start ();

            if (mRxAnimator != null && mRxAnimator.isRunning ()) mRxAnimator.cancel ();
            mRxAnimator = ValueAnimator.ofFloat (v.getScaleX (), 0f);
            mRxAnimator.setDuration (getResources ().getInteger (android.R.integer.config_shortAnimTime));
            mRxAnimator.addUpdateListener (animation -> {
                float r = (float) animation.getAnimatedValue ();
                v.setScaleX (r);
                v.setScaleY (r);
            });
            mRxAnimator.start ();

            inflateNext ();
        }

        private boolean hasCancellableImpendingEvents = true;
        private boolean isLastPulse = false;

        private int in = getResources ().getInteger (com.google.android.material.R.integer.material_motion_duration_medium_2);
        private int out = getResources ().getInteger (com.google.android.material.R.integer.material_motion_duration_medium_1);
        private int wait = 4 * getResources ().getInteger (com.google.android.material.R.integer.material_motion_duration_long_2);
        private long d = in + out + wait;

        private void pulse () {
            pulseOut (mTrackView);
        }

        private void cancelImpedingPulseEvents () {
            isLastPulse = true;
            if (hasCancellableImpendingEvents) {
                if (mRxAnimator != null) mRxAnimator.cancel ();
//                hasCancellableImpendingEvents = false;
            }
        }

        private void pulseOut (View v) {
//            mXAnimator.cancel ();
//            mYAnimator.cancel ();
//            mIxAnimator.cancel ();

//            if (mRxAnimator != null && mRxAnimator.isRunning ()) mRxAnimator.cancel ();

            mRxAnimator = ValueAnimator.ofFloat (v.getScaleX (), 1.1f);
            mRxAnimator.setDuration (out);
            mRxAnimator.addUpdateListener (animation -> {
                float r = (float) animation.getAnimatedValue ();
                v.setScaleX (r);
                v.setScaleY (r);
            });
            mRxAnimator.addListener (new AnimatorListenerAdapter () {
                @Override
                public void onAnimationStart (Animator animation) {
                    super.onAnimationStart (animation);
                    hasCancellableImpendingEvents = false;
                }

                @Override
                public void onAnimationEnd (Animator animation) {
                    super.onAnimationEnd (animation);

                    pulseIn (v);
                }

                @Override
                public void onAnimationCancel (Animator animation) {
                    super.onAnimationCancel (animation);
                }
            });
            mRxAnimator.setStartDelay (wait);
            mRxAnimator.start ();
        }

        private void pulseIn (View v) {
//            mXAnimator.cancel ();
//            mYAnimator.cancel ();
//            mIxAnimator.cancel ();

//            if (mRxAnimator != null && mRxAnimator.isRunning ()) mRxAnimator.cancel ();
            mRxAnimator = ValueAnimator.ofFloat (v.getScaleX (), 1f);
            mRxAnimator.setDuration (in);
            mRxAnimator.addUpdateListener (animation -> {
                float r = (float) animation.getAnimatedValue ();
                v.setScaleX (r);
                v.setScaleY (r);
            });
            mRxAnimator.addListener (new AnimatorListenerAdapter () {
                @Override
                public void onAnimationCancel (Animator animation) {
                    super.onAnimationCancel (animation);
                }

                @Override
                public void onAnimationEnd (Animator animation) {
                    super.onAnimationEnd (animation);
                    if (isLastPulse) {
                        isLastPulse = false;
                        // update ui asRealmObject show new track
                    } else pulseOut (v);
                }
            });
            mRxAnimator.start ();
        }
    }
}