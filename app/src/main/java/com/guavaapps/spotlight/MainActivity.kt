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
import com.guavaapps.components.color.Argb;
import com.guavaapps.components.color.Hct;
import com.pixel.spotifyapi.Objects.UserPrivate;
import com.pixel.spotifyapi.SpotifyApi;
import com.pixel.spotifyapi.SpotifyService;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.sdk.android.auth.AuthorizationClient;
import com.spotify.sdk.android.auth.AuthorizationRequest;
import com.spotify.sdk.android.auth.AuthorizationResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
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

    private void getC (int c) {
        Log.e (TAG, "approx");
        getApproxInc (c);
        Log.e (TAG, "inc");
        getInc (c);

        Hct tf = Hct.Companion.fromInt (c);//Argb.Companion.from (r, g, b, 0).toInt ());
        float tone = tf.getTone ();

        Log.e (TAG, String.format ("tone=%f", tone));
    }

    private void getInc (int c) {
        Hct tf = Hct.Companion.fromInt (c);

        int approxC = c - (c % 10);
        int startT = -approxC + 10;
        int endT = 100 - approxC;

        float tone = tf.getTone ();

        for (int i = startT; i < endT; i += 10) {
            tf.setTone (tone + i);
            Log.e (TAG, i + ": " + getHex (
                    tf.toInt ()
            ));
        }
    }

    private void getApproxInc (int c) {
        Hct tf = Hct.Companion.fromInt (c);

        for (int i = 10; i < 100; i += 10) {
            tf.setTone (i);
            Log.e (TAG, i + ": " + getHex (
                    tf.toInt ()
            ));
        }
    }

    private void getColorForTone (int color, float tone) {
        Hct hct = Hct.Companion.fromInt (color);
        hct.setTone (tone);
        Log.e (TAG, "tf" + tone + " " + getHex (hct.toInt ()));
    }

    @SuppressLint ("ClickableViewAccessibility")
    @Override
    protected void onCreate (Bundle savedInstanceState) {
        super.onCreate (savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows (getWindow (), false);

        setContentView (R.layout.activity_main);

        try {
            FileInputStream fileInputStream = getAssets ().openFd ("model2.tflite").createInputStream ();
            FileChannel fileChannel = fileInputStream.getChannel ();
            File f = File.createTempFile ("model2", ".tflite");
            FileOutputStream out = new FileOutputStream (f);
            byte[] bytes = new byte[fileInputStream.available ()];
            fileInputStream.read (bytes);
            out.write (bytes);
            out.flush ();
            out.close ();
            try {
                DLSTMPModel model = new DLSTMPModel (f);

                //0.01544402]
                // [0.02702703]
                // [0.05405406]
                // [0.04826255]]

                //[[0.08406813 0.62631917]] testOn5 ()

//                model.getNextTest ();
                model.testOn5 ();
            } catch (Exception e) {
                e.printStackTrace ();
            }
        } catch (IOException e) {
            e.printStackTrace ();
        }


        int tf = Argb.Companion.from (255, 255, 111, 0).toInt ();
        int sp = Argb.Companion.from (255, 30, 215, 96).toInt ();
        int spl = Argb.Companion.from (255, 102, 0, 115).toInt ();
        int mng = Argb.Companion.from (255, 102, 0, 115).toInt (); // 19, 170, 82

        tf = sp;

        Hct hct = Hct.Companion.fromInt (tf);
        Log.e (TAG, "tone=" + hct.getTone ());

        getColorForTone (tf, 10);
        getColorForTone (tf, 40);
        getColorForTone (tf, 50);
        getColorForTone (tf, 60);
        getColorForTone (tf, 70);
        getColorForTone (tf, 80);
        getColorForTone (tf, 90);

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
                                                Type listType = new TypeToken <List <String>> () {
                                                }.getType ();
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