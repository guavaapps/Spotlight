package com.guavaapps.spotlight;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentContainerView;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

public class ContentFragmentKT extends Fragment {
    private static final String TAG = "ContentFragment";

    private ContentViewModel mViewModel;

    private ViewPager2 mPager;
    private FragmentContainerView track;

    private UserFragment mUserFragment;
    private TrackFragment mTrackFragment;
    private ExtraFragment mExtraFragment;

    private ImageView mSurfaceView;
    private ImageView mTempSurfaceView;
    private View mSurfaceViewOverlay;

    private ImageView mUser;

    private ColorSet mColorSet = new ColorSet ();
    private Bitmap mSurfaceBitmap;
    private FragmentContainerView mParent;

    private NavController mNavController;

    public ContentFragmentKT () {

    }

    @Override
    public void onCreate (Bundle savedInstanceState) {
        super.onCreate (savedInstanceState);
    }

    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container,
                              Bundle savedInstanceState) {

        return inflater.inflate (R.layout.fragment_content, container, false);
    }

    @Override
    public void onViewCreated (@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated (view, savedInstanceState);

        mViewModel = new ViewModelProvider (requireActivity ()).get (ContentViewModel.class);
        mNavController = NavHostFragment.findNavController (this);

        Log.e (TAG, "nav null: " + mNavController);

        mPager = view.findViewById (R.id.pager);
        mPager.setOffscreenPageLimit (1);
        track = view.findViewById (R.id.track);

        NavHostFragment navHostFragment = (NavHostFragment) requireActivity ().getSupportFragmentManager ()
                .findFragmentById (R.id.track);

        mUserFragment = new UserFragment ();
        mExtraFragment = new ExtraFragment ();

        mTrackFragment = track.getFragment ();

        mSurfaceView = view.findViewById (R.id.surface_view);
        mTempSurfaceView = view.findViewById (R.id.temp_surface_view);
        mSurfaceViewOverlay = view.findViewById (R.id.surface_view_overlay);

        mUser = view.findViewById (R.id.user);

        mSurfaceView.setRenderEffect (RenderEffect.createBlurEffect (100, 100, Shader.TileMode.CLAMP));
        mTempSurfaceView.setRenderEffect (RenderEffect.createBlurEffect (100, 100, Shader.TileMode.CLAMP));

        mSurfaceView.setScaleType (ImageView.ScaleType.CENTER_CROP);
        mTempSurfaceView.setScaleType (ImageView.ScaleType.CENTER_CROP);

        mViewModel.getAlbum ().observe (getViewLifecycleOwner (), albumWrapper -> {
            if (albumWrapper != null) {
                nextAlbum (albumWrapper);
            } else {

            }
        });

        mViewModel.getUser ().observe (getViewLifecycleOwner (), userWrapper -> {
            if (userWrapper != null) {
                mUser.setImageBitmap (userWrapper.thumbnail);
            }
        });

        mUser.setOnClickListener (v -> {
            String trans = "trans";

//            Navigator.Extras extras = new FragmentNavigator.Extras (Collections.singletonMap (v, trans));
            NavDirections directions = ContentFragmentDirections.actionContentFragmentToUserFragment ();
            mNavController.navigate (directions);
        });

        view.getViewTreeObserver ().addOnGlobalLayoutListener (new ViewTreeObserver.OnGlobalLayoutListener () {
            @Override
            public void onGlobalLayout () {
                int viewWidth = getView ().getWidth ();
                int viewHeight = getView ().getHeight ();

                ViewCompat.requestApplyInsets (view);

//                ViewCompat.setOnApplyWindowInsetsListener (view, (v, windowInsetsCompat) -> {
                Insets insets = ViewCompat.getRootWindowInsets (view).getInsets (WindowInsetsCompat.Type.systemBars ());//windowInsetsCompat.getInsets (WindowInsetsCompat.Type.systemBars ());

//                    return WindowInsetsCompat.CONSUMED;
//                });
                mPager.setAdapter (new Adapter (requireActivity ()));
                mPager.registerOnPageChangeCallback (new ViewPager2.OnPageChangeCallback () {
                    private float pOffset;

                    @Override
                    public void onPageScrolled (int position, float positionOffset, int positionOffsetPixels) {
                        super.onPageScrolled (position, positionOffset, positionOffsetPixels);

                        if (position == 0) {
                            try {
                                float rawScale = positionOffset < 0.5f ? positionOffset / 0.5f : 1f;
                                float scale = 1 - rawScale;

                                mTrackFragment.resize (positionOffset);
                                //mUserView.setAlpha (scale);

//                                ArgbEvaluator argbEvaluator = new ArgbEvaluator ();
//                                Argb c = Argb.from (mColorSet.surface);
//                                c.setAlpha (255);
//                                int argb = (int) argbEvaluator.evaluate (positionOffset, mColorSet.surfaceGradient [0], c.toInt ());
//                                int argb2 = (int) argbEvaluator.evaluate (positionOffset, mColorSet.surfaceGradient [1], c.toInt ());
////                                mSurfaceViewOverlay.setBackgroundColor (argb);
//                                GradientDrawable drawable = new GradientDrawable ();
//                                drawable.setOrientation (GradientDrawable.Orientation.TOP_BOTTOM);
//                                drawable.setColors (new int[] {argb, argb2});
//                                //mSurfaceViewOverlay.setBackground (drawable);
                                mSurfaceViewOverlay.setAlpha (positionOffset > 0.16f ? positionOffset : 0.16f);
                            } catch (Exception e) {

                            }
                        }
                    }

                    @Override
                    public void onPageSelected (int position) {
                        super.onPageSelected (position);
                    }

                    @Override
                    public void onPageScrollStateChanged (int state) {
                        super.onPageScrollStateChanged (state);
                    }
                });

                ViewCompat.setOnApplyWindowInsetsListener (view, null);

//                    return windowInsetsCompat;
//                }));


                view.getViewTreeObserver ().removeOnGlobalLayoutListener (this);
            }
        });
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
        nextTrack (new TrackWrapper (null, wrappedAlbum.bitmap));
    }

    private void nextTrack (TrackWrapper wrappedTrack) {
        Bitmap bitmap = wrappedTrack.thumbnail;
        ColorSet colorSet = ColorSet.Companion.create (bitmap);

        mSurfaceView.setScaleType (ImageView.ScaleType.CENTER_CROP);

        BitmapDrawable temp = (BitmapDrawable) mSurfaceView.getDrawable ();
        Bitmap b = temp != null ? temp.getBitmap () : null;

        mTempSurfaceView.setImageBitmap (b);
        mTempSurfaceView.setVisibility (View.VISIBLE);
        mTempSurfaceView.setAlpha (1f);

        mSurfaceView.setImageBitmap (bitmap);

        mTempSurfaceView.animate ()
                .alpha (0f)
                .setDuration (getResources ().getInteger (android.R.integer.config_shortAnimTime))
                .setListener (new AnimatorListenerAdapter () {
                    @Override
                    public void onAnimationEnd (Animator animation) {
                        super.onAnimationEnd (animation);

                        mTempSurfaceView.setVisibility (View.GONE);
                    }
                })
                .start ();

        mSurfaceBitmap = bitmap;

        GradientDrawable drawable = new GradientDrawable ();

        ValueAnimator argbAnimator1 = ValueAnimator.ofObject (new ArgbEvaluator (),
                mColorSet.getSurface ()[0],
                colorSet.getSurface ()[0]);

        argbAnimator1.setDuration (getResources ().getInteger (android.R.integer.config_shortAnimTime));
        argbAnimator1.addUpdateListener (animation -> {
            mColorSet.getSurface ()[0] = (int) animation.getAnimatedValue ();

            drawable.setColors (mColorSet.getSurface ());
            mSurfaceViewOverlay.setBackground (drawable);
        });

        ValueAnimator argbAnimator2 = ValueAnimator.ofObject (new ArgbEvaluator (),
                mColorSet.getSurface ()[1],
                colorSet.getSurface ()[1]);

        argbAnimator2.setDuration (getResources ().getInteger (android.R.integer.config_shortAnimTime));
        argbAnimator2.addUpdateListener (animation -> {
            mColorSet.getSurface ()[1] = (int) animation.getAnimatedValue ();
            drawable.setColors (mColorSet.getSurface ());
            mSurfaceViewOverlay.setBackground (drawable);
        });

        argbAnimator1.start ();
        argbAnimator2.start ();

        mColorSet = colorSet;
    }

    private class Adapter extends FragmentStateAdapter {
        public Adapter (@NonNull FragmentActivity fragmentActivity) {
            super (fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment (int position) {
            switch (position) {
                case 0:
                    return new Fragment ();
                case 1:
                    return mExtraFragment;
            }

            return null; /// fkjdkaaaa aaaaaaaaaaahhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh
        }

        @Override
        public int getItemCount () {
            return 2;
        }
    }
}