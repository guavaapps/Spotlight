//package com.guavaapps.spotlight;
//
//import android.content.Intent;
//import android.content.res.ColorStateList;
//import android.graphics.Bitmap;
//import android.graphics.Color;
//import android.graphics.Outline;
//import android.net.Uri;
//import android.os.Bundle;
//import android.os.Handler;
//import android.os.Looper;
//import android.util.Log;
//import android.util.TypedValue;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.view.ViewOutlineProvider;
//import android.view.ViewTreeObserver;
//import android.view.animation.AnticipateOvershootInterpolator;
//import android.widget.ImageView;
//import android.widget.TextView;
//
//import androidx.activity.OnBackPressedCallback;
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.core.graphics.Insets;
//import androidx.core.view.ViewCompat;
//import androidx.core.view.WindowInsetsCompat;
//import androidx.fragment.app.Fragment;
//import androidx.lifecycle.ViewModelProvider;
//import androidx.navigation.NavController;
//import androidx.navigation.fragment.NavHostFragment;
//
//import com.google.android.material.button.MaterialButton;
//import com.google.android.material.shape.ShapeAppearanceModel;
//import com.google.android.material.transition.MaterialContainerTransform;
//import com.guavaapps.components.Components;
//import com.guavaapps.components.bitmap.BitmapTools;
//import com.guavaapps.components.listview.ListView;
//import com.pixel.spotifyapi.Objects.Pager;
//import com.pixel.spotifyapi.Objects.Playlist;
//import com.pixel.spotifyapi.Objects.PlaylistSimple;
//
//import java.util.ArrayList;
//import java.util.LinkedHashMap;
//import java.util.List;
//import java.util.concurrent.Executors;
//
//import retrofit.Callback;
//import retrofit.RetrofitError;
//import retrofit.client.Response;
//
//public class UserFragmentKT extends Fragment {
//    private static final String TAG = "UserFragment";
//    private ContentViewModel mViewModel;
//    private Insets mInsets;
//
//    private LinkedHashMap <PlaylistWrapper, View> mPlaylists = new LinkedHashMap <> ();
//
//    private ImageView mUserView;
//    private TextView mUserNameView;
//    private MaterialButton mSpotifyButton;
//    private ListView mPlaylistListView;
//
//    public UserFragmentKT() {
//    }
//
//    @Nullable
//    @Override
//    public View onCreateView (@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
//        return inflater.inflate (R.layout.fragment_user, container, false);
//    }
//
//    @Override
//    public void onCreate (@Nullable Bundle savedInstanceState) {
//        super.onCreate (savedInstanceState);
//
//        MaterialContainerTransform transform = new MaterialContainerTransform ();
//        transform.setInterpolator (new AnticipateOvershootInterpolator (0.5f));
//        transform.setDrawingViewId (R.id.fragment_container_view);
//        transform.setDuration (getResources ().getInteger (com.google.android.material.R.integer.material_motion_duration_long_2));
//        transform.setFadeMode (MaterialContainerTransform.FADE_MODE_THROUGH);
//        transform.setScrimColor (Color.TRANSPARENT);
//        transform.setAllContainerColors (Color.TRANSPARENT);
//
//        ShapeAppearanceModel model = ShapeAppearanceModel.builder()
//                .setAllCornerSizes (Components.INSTANCE.getPx (getContext (), 32))
//                .build();
//
//        transform.setStartShapeAppearanceModel (model);
////        transform.setEndShapeAppearanceModel (model);
//
//        setSharedElementEnterTransition (transform);
//    }
//
//    @Override
//    public void onViewCreated (@NonNull View view, @Nullable Bundle savedInstanceState) {
//        super.onViewCreated (view, savedInstanceState);
//
//        OnBackPressedCallback callback = new OnBackPressedCallback (true) {
//            @Override
//            public void handleOnBackPressed () {
//                NavController navController = NavHostFragment.findNavController (UserFragmentKT.this);
//                navController.navigateUp ();
//            }
//        };
//
//        requireActivity ().getOnBackPressedDispatcher ().addCallback (getViewLifecycleOwner (), callback);
//
//        mViewModel = new ViewModelProvider (requireActivity ()).get (ContentViewModel.class);
//
//        mUserView = view.findViewById (R.id.user);
//        mUserNameView = view.findViewById (R.id.user_name);
//        mSpotifyButton = view.findViewById (R.id.spotify);
//        mPlaylistListView = view.findViewById (R.id.list_view);
//
//        mUserView.setOutlineProvider (new ViewOutlineProvider () {
//            @Override
//            public void getOutline (View view, Outline outline) {
//                view.setClipToOutline (true);
//                outline.setOval (0, 0, view.getWidth (), view.getHeight ());
//            }
//        });
//
//        view.getViewTreeObserver ().addOnGlobalLayoutListener (new ViewTreeObserver.OnGlobalLayoutListener () {
//            @Override
//            public void onGlobalLayout () {
//                int viewWidth = view.getWidth ();
//                int viewHeight = view.getHeight ();
//
//                mInsets = ViewCompat.getRootWindowInsets (view).getInsets (WindowInsetsCompat.Type.systemBars ());
//
//                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) mUserView.getLayoutParams ();
//                params.topMargin = mInsets.top + Components.INSTANCE.getPx (UserFragmentKT.this.getContext (), 24);
//                mUserView.setLayoutParams (params);
//
//                view.getViewTreeObserver ().removeOnGlobalLayoutListener (this);
//            }
//        });
//
//        mViewModel.getUser().observe (getViewLifecycleOwner (), userWrapper -> {
//            mUserView.setImageBitmap (userWrapper.thumbnail);
//            mUserNameView.setText (userWrapper.user.display_name);
//
//            mSpotifyButton.setOnClickListener (v -> {
//                Intent intent = new Intent (Intent.ACTION_VIEW);
//                intent.setData (Uri.parse (userWrapper.user.uri));
//                intent.putExtra (Intent.EXTRA_REFERRER, "android-app://" + getContext ().getPackageName ());
//
//                startActivity (intent);
//            });
//
//            mViewModel.getSpotifyService ().getValue ()
//                    .getCurrentUserPlaylists (new Callback <Pager <PlaylistSimple>> () {
//                        @Override
//                        public void success (Pager <PlaylistSimple> playlistSimplePager, Response response) {
//                            Executors.newSingleThreadExecutor ().execute (() -> {
//                                List <View> items = new ArrayList <> ();
//
//                                for (PlaylistSimple playlistSimple : playlistSimplePager.items) {
//                                    if (!playlistSimple.owner.id.equals (userWrapper.user.id))
//                                        continue;
//                                    Bitmap bitmap = BitmapTools.INSTANCE.from (playlistSimple.images.get (0).url);
//
//                                    PlaylistWrapper playlistWrapper = new PlaylistWrapper (playlistSimple, bitmap);
//                                    View v = LayoutInflater.from (getContext ()).inflate (R.layout.playlist_item, null, false);
//                                    v.setLayoutParams (new ViewGroup.LayoutParams (-1, -2));
//                                    ImageView bitmapView = v.findViewById (R.id.bitmap);
//                                    TextView nameView = v.findViewById (R.id.name);
//
//                                    bitmapView.setImageBitmap (bitmap);
//                                    nameView.setText (playlistSimple.name);
//
//                                    TypedValue d = new TypedValue ();
//                                    getContext ().getTheme ().resolveAttribute (android.R.attr.selectableItemBackground, d, true);
//                                    v.setForeground (getResources ().getDrawable (d.resourceId, getContext ().getTheme ()));
//
//                                    v.setOnClickListener (v1 -> {
//
//                                    });
//
//                                    mPlaylists.put (playlistWrapper, v);
//                                    items.add (v);
//                                }
//
//                                View[] views = new View[mPlaylists.size ()];
//                                mPlaylists.values ().toArray (views);
//
//                                Handler.createAsync (Looper.getMainLooper ())
//                                        .post (() -> mPlaylistListView.add (items));
//                            });
//                        }
//
//                        @Override
//                        public void failure (RetrofitError error) {
//
//                        }
//                    });
//
//            applyColorSet (userWrapper.thumbnail);
//        });
//    }
//
//    private void logDumpPlaylist (Playlist playlist) {
//        String id = playlist.id;
//        String name = playlist.name;
//        String owner = playlist.owner.display_name;
//        boolean isCollab = playlist.collaborative;
//        boolean isPublic = playlist.is_public;
//
//        Playlist p = new Playlist ();
//
//        Log.e (TAG, "-------- playlist log dump --------");
//        Log.e (TAG, "name: " + name);
//        Log.e (TAG, "id: " + id);
//        Log.e (TAG, "owner: " + owner);
//        Log.e (TAG, "isCollab: " + isCollab);
//        Log.e (TAG, "isPublic: " + isPublic);
//
//        Log.e (TAG, "-------- --------");
//    }
//
//    private void logDumpPlaylistSimple (PlaylistSimple playlistSimple) {
//        String id = playlistSimple.id;
//        String name = playlistSimple.name;
//        String owner = playlistSimple.owner.display_name;
//        boolean isCollab = playlistSimple.collaborative;
//        boolean isPublic = playlistSimple.is_public;
//
//        Playlist p = new Playlist ();
//
//        Log.e (TAG, "-------- playlist log dump --------");
//        Log.e (TAG, "name: " + name);
//        Log.e (TAG, "id: " + id);
//        Log.e (TAG, "owner: " + owner);
//        Log.e (TAG, "isCollab: " + isCollab);
//        Log.e (TAG, "isPublic: " + isPublic);
//
//        Log.e (TAG, "-------- --------");
//    }
//
//    private void applyColorSet (Bitmap bitmap) {
//        ColorSet colorSet = ColorSet.Companion.create (bitmap);
//
//        mUserNameView.setTextColor (colorSet.getPrimary ());
//        mSpotifyButton.setBackgroundColor (colorSet.getPrimary ());
//        mSpotifyButton.setTextColor (colorSet.getText ());
//        mSpotifyButton.setRippleColor (ColorStateList.valueOf (colorSet.getRipple ()));
//    }
//}
