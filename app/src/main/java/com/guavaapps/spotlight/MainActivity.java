package com.guavaapps.spotlight;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.FragmentContainerView;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import com.pixel.spotifyapi.Objects.UserPrivate;
import com.pixel.spotifyapi.SpotifyApi;
import com.pixel.spotifyapi.SpotifyService;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.sdk.android.auth.AuthorizationClient;
import com.spotify.sdk.android.auth.AuthorizationRequest;
import com.spotify.sdk.android.auth.AuthorizationResponse;

import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final String DEBUG = "MainActivity:DEBUG";

    private static final String CLIENT_ID = "431b4c7e106c4470a0b145cdfe7962bd";//"86ef10fd39344e10a060ade09f7c7a78";

    private static final String SPOTIFY_PREMIUM = "premium";
    private static final String SPOTIFY = "com.spotify.music";

    private static final int REQUEST_AUTH = 0;
    private SpotifyService mSpotifyService;
    private SpotifyAppRemote mSpotifyAppRemote;

    private ContentViewModel mViewModel;

    private Object mLock = new Object ();

    private FragmentContainerView mFragmentContainerView;

    private ContentFragment mContentFragment;
    private PlayFragment mPlayFragment;

    private void trainIris () {
        NetworkDenseKt.batchTrain ();
    }

    private String getHex (int color) {
        return String.format ("#%06X", color & 0xffffff);
    }

    @SuppressLint ("ClickableViewAccessibility")
    @Override
    protected void onCreate (Bundle savedInstanceState) {
        super.onCreate (savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows (getWindow (), false);

        setContentView (R.layout.activity_main);

//        LSTMSHNode node = new LSTMSHNode ();
//        node.create (4);
//        node.feed (
//                new float[] {0.1f, 0.33f, 0.24f, 0.74f},
//                new float[] {0.24f, 0.74f, 0.33f, 0.1f}
//        );
//        node.feed (
//                new float[] {0.1f, 0.33f, 0.24f, 0.74f},
//                new float[] {0.24f, 0.74f, 0.33f, 0.1f}
//        );

        ObjectStore objectStore = new ObjectStore ();

        mViewModel = new ViewModelProvider (this).get (ContentViewModel.class);

        mFragmentContainerView = findViewById (R.id.fragment_container_view);
        mContentFragment = new ContentFragment ();
        mPlayFragment = new PlayFragment ();

        View content = findViewById (android.R.id.content);

        NavHostFragment fragment = (NavHostFragment) getSupportFragmentManager ().findFragmentById (R.id.fragment_container_view);
        NavController navController = fragment.getNavController ();

        content.getViewTreeObserver ()
                .addOnPreDrawListener (new ViewTreeObserver.OnPreDrawListener () {
                    @Override
                    public boolean onPreDraw () {
                        if (mLock == null) {
                            content.getViewTreeObserver ()
                                    .removeOnPreDrawListener (this);

//                            getSupportFragmentManager ().beginTransaction ()
//                                    .add (mFragmentContainerView.getId (), mContentFragment)
//                                    .commit ();
                        }

                        return false;
                    }
                });

        if (isSpotifyInstalled ()) {
            auth ();
        }
    }

    @Override
    protected void onResume () {
        super.onResume ();
    }

    @Override
    protected void onDestroy () {
        super.onDestroy ();
    }

    protected void onActivityResult (int requestCode, int resultCode, Intent intent) {
        super.onActivityResult (requestCode, resultCode, intent);

        if (requestCode == REQUEST_AUTH) {
            AuthorizationResponse response = AuthorizationClient.getResponse (resultCode, intent);

            Log.e (TAG, response.getType ().toString ());

            switch (response.getType ()) {
                case TOKEN:
                    String accessToken = response.getAccessToken ();
                    Log.e (TAG, "accessToken");

                    SpotifyApi spotifyApi = new SpotifyApi ();
                    spotifyApi.setAccessToken (accessToken);
                    mSpotifyService = spotifyApi.getService ();

                    mViewModel.setSpotifyService (mSpotifyService);

//                    mViewModel.getRecc ();

                    AppRepo.getInstance ()
                            .getCurrentUser (mSpotifyService, MainActivity.this, new AppRepo.ResultListener () {
                                @Override
                                public void onUser (UserWrapper userWrapper) {


                                    Handler.createAsync (Looper.getMainLooper ())
                                            .post (() -> {
                                                DB db = new DB (MainActivity.this);
                                                db.login (userWrapper.user.id);
                                                mViewModel.setUser (userWrapper);

                                                String jsonArray = "[\"0\", \"1\"]";
                                                Type listType = new TypeToken <List <String>> () {}.getType ();
                                                List <String> list = new Gson ().fromJson (jsonArray, listType);
                                                for (String s : list) {
                                                    Log.e (TAG, "list: " + s);
                                                }
                                            });
                                }
                            });

                    mViewModel.nextTrack (this);

                    mLock = null;

                    break;

                case ERROR:
                    String e = response.getError ();
                    Log.e (TAG, e);

                    break;

                default:
            }
        }
    }

    public void auth () {
        AuthorizationRequest.Builder builder = new AuthorizationRequest.Builder (CLIENT_ID, AuthorizationResponse.Type.TOKEN, "http://localhost/");

        builder.setScopes (new String[] {"streaming", "user-follow-read", "playlist-read-private", "user-read-private", "user-library-modify", "playlist-modify-private", "playlist-modify-public"});
        AuthorizationRequest request = builder.build ();

        AuthorizationClient.openLoginActivity (this, REQUEST_AUTH, request);

        ConnectionParams connectionParams = new ConnectionParams.Builder (CLIENT_ID)
                .setRedirectUri ("http://localhost/")
                .showAuthView (true)
                .build ();

        Handler.createAsync (Looper.getMainLooper ()).postDelayed (
                () -> {
                    SpotifyAppRemote.connect (MainActivity.this, connectionParams, new Connector.ConnectionListener () {
                        @Override
                        public void onConnected (SpotifyAppRemote spotifyAppRemote) {
                            Log.d (TAG, "SpotifyAppRemote connected");

                            mViewModel.setSpotifyAppRemote (spotifyAppRemote);
                        }

                        @Override
                        public void onFailure (Throwable error) {
                            Log.d (TAG, "SpotifyAppRemote failed to connect - " + error.getMessage () + " cause=" + error.getCause ());
                        }
                    });
                },
                5000
        );
    }

    private boolean isSpotifyInstalled () {
        try {
            getPackageManager ().getPackageInfo (SPOTIFY, 0);

            return true;
        } catch (PackageManager.NameNotFoundException e) {
        }
        return false;
    }

    private boolean isPremium (UserPrivate currentUser) {
        return currentUser.product.equals (SPOTIFY_PREMIUM);
    }
}