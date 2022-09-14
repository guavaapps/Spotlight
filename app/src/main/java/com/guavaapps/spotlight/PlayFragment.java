package com.guavaapps.spotlight;

import android.animation.ValueAnimator;
import android.graphics.Outline;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.guavaapps.components.Components;

// TODO move userButton into ContentFragment and implement MaterialContainerTransform from userButton to UserFragment on userButton click
// TODO deprecate PlayFragment
// TODO integrate TimeString with main pink.components library
// TODO add playlist selector in UserFragment
// TODO add gradient to surfaceOverlay in ContentFragment
// TODO improve color sampling
// TODO create fragments to handle no spotify, no premium and sign in
// TODO create remote repo, convert AppRepo to a local database and interface to access and publish user nns and Spotify content caches

public class PlayFragment extends Fragment {
    private static final String TAG = "PlayFragment";
    private ContentViewModel mViewModel;

    private FragmentContainerView mFrag;

    private ImageView mUserView;
    private View.OnClickListener mUserButtonOnClickListener = v -> {
        NavHostFragment navHostFragment = (NavHostFragment) getChildFragmentManager ().findFragmentById (R.id.frag);
        NavController navController = navHostFragment.getNavController ();

        if (navController.getCurrentDestination ().getId () == R.id.contentFragment) {
            navController.navigate (R.id.action_contentFragment_to_userFragment);
            v.setVisibility (View.GONE);
            v.setOnClickListener (null);
        }
    };

    @Nullable
    @Override
    public View onCreateView (@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate (R.layout.fragment_play, container, false);
    }

    @Override
    public void onViewCreated (@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated (view, savedInstanceState);

        mViewModel = new ViewModelProvider (requireActivity ()).get (ContentViewModel.class);

        mFrag = view.findViewById (R.id.frag);
        mUserView = view.findViewById (R.id.user);

        view.getViewTreeObserver ().addOnGlobalLayoutListener (() -> {
            int viewWidth = view.getWidth ();
            int viewHeight = view.getHeight ();

            Insets insets = ViewCompat.getRootWindowInsets (view).getInsets (WindowInsetsCompat.Type.systemBars ());

            mUserView.setX (viewWidth - mUserView.getWidth () - Components.INSTANCE.getPx (getContext (), 24));
            mUserView.setY (Components.INSTANCE.getPx (getContext (), 24) + insets.top);
            mUserView.setOnClickListener (mUserButtonOnClickListener);
            mUserView.setOutlineProvider (new ViewOutlineProvider () {
                @Override
                public void getOutline (View view, Outline outline) {
                    mUserView.setClipToOutline (true);
                    outline.setOval (0,
                            0,
                            view.getWidth (),
                            view.getHeight ()
                    );
                }
            });

        });

        mViewModel.getUser ().observe (getViewLifecycleOwner (), userWrapper -> {
            Log.d (TAG, "set user bitmap");
            mUserView.setImageBitmap (userWrapper.thumbnail);
        });
    } // i wanna die

    // kill me please i wanna die

    private void layoutExpanded () {
        float x = mUserView.getX ();
        float y = mUserView.getY ();

        View parent = (View) mUserView.getParent ();
        float expandedX = parent.getWidth () / 2 - mUserView.getWidth () / 2;
        float expandedY = parent.getHeight () / 2 - mUserView.getHeight () / 2;

        ValueAnimator xAnimator = ValueAnimator.ofFloat (x, expandedX);
        ValueAnimator yAnimator = ValueAnimator.ofFloat (y, expandedY);
    }
}//hihihihihihihihiihiihihihihihihihihihihihihihihihihihihiihihihihiilpvehihihihihihihihihihihhuhuhuhihhihihihihihihihihihihihihihyouhihihihihihihihihihihihihihihihihihihihihihisohihihihiihihihihihihmuchihihihihihihihihihihihihihihbabyhihihihihihihiihhoihihihihihyouhihihihihihcouldhihihihihihihihihihihinihihihihneverhihihihihihihihihihhtophihihihihihihihithihihihihijihihinoigujyhtgfrdewsedfrgtyh6uj7ik8okij7uh6yg5tf4rd3esrftgyhujiko8ijynhbtgvrbhnjki8ol9p;lo8kumjynhtbnjymik7olkijuhtygrtybhnujmiunyhbtgrvfgbhnjmkujnygvcfgvtjunbhytjunyhbtgyhjunybhgvthygthybtgvrfgthybtgrfgthygtfrgthuj7iunybhgtt5y6huybgtvf5y6u7jnhybgthyngt56yujnhbgtyhujnhbgthyujikmnhbjuikmhngtyjunhygtfrtgyhjuhybgvfcrty6hb gvftgyhnbgfretgyhnbgvtfyhujbgyhgvfgbhgvvfcgvfgvvgtfgtgvtfvftcrcrvcfrtfcgtfvcbhjuiytrewsaszxcvgbhjnmkl,kmjnbhgvfcdxszdxfghjuklo,ikmujnyhbtgvrfedwfcrgrhes hedcje`ekedka`ew
