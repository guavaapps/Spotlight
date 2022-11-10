package com.guavaapps.spotlight;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.guavaapps.components.bitmap.BitmapTools;
import com.pixel.spotifyapi.Objects.Album;
import com.pixel.spotifyapi.Objects.AudioFeaturesTrack;
import com.pixel.spotifyapi.Objects.AudioFeaturesTracks;
import com.pixel.spotifyapi.Objects.Recommendations;
import com.pixel.spotifyapi.Objects.SeedsGenres;
import com.pixel.spotifyapi.Objects.Track;
import com.pixel.spotifyapi.Objects.TrackSimple;
import com.pixel.spotifyapi.SpotifyService;
import com.spotify.android.appremote.api.PlayerApi;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.types.Repeat;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

// TODO deprecate app repo
public class ContentViewModel2 extends ViewModel {
    private static final String TAG = "ViewModel";
    private boolean mIsWaiting = false;
    private boolean mNeedsTrack = true;
    private boolean mNeedsNextTrack = true;
    private Queue <TrackWrapper> mTracks = new LinkedList <> ();

    private MutableLiveData <SpotifyService> mSpotifyService = new MutableLiveData <> ();
    private MutableLiveData <SpotifyAppRemote> mSpotifyAppRemote = new MutableLiveData <> ();
    private MutableLiveData <UserWrapper> mUser = new MutableLiveData <> ();
    private MutableLiveData <TrackWrapper> mTrack = new MutableLiveData <> ();
    private MutableLiveData <Long> mProgress = new MutableLiveData <> ();
    private MutableLiveData <TrackWrapper> mNextTrack = new MutableLiveData <> ();

    private MutableLiveData <AlbumWrapper> mAlbum = new MutableLiveData <> ();

    private ScheduledExecutorService mPlayerStateListener;

    public void getRecc () {
        Map <String, Object> params = new HashMap <> ();
        params.put ("seed_artists", "7dGJo4pcD2V6oG8kP0tJRR");
        params.put ("seed_genres", "hip-hop");

        mSpotifyService.getValue ()
                .getRecommendations (params, new Callback <Recommendations> () {
                    @Override
                    public void success (Recommendations recommendations, Response response) {
                        for (Track track : recommendations.tracks) {
//                            Log.e (TAG, "track: " + track.name + " by " + track.artists.get (0).name);
                        }

                        mSpotifyService.getValue ()
                                .getTrackAudioFeatures (recommendations.tracks.get (0).id, new Callback <AudioFeaturesTrack> () {
                                    @Override
                                    public void success (AudioFeaturesTrack audioFeaturesTrack, Response response) {
//                                        Log.e (TAG, "audio features for " + recommendations.tracks.get (0).name);
//                                        Log.e (TAG, "type: " + audioFeaturesTrack.type);
//                                        Log.e (TAG, "acousticness: " + audioFeaturesTrack.acousticness);
//                                        Log.e (TAG, "danceability: " + audioFeaturesTrack.danceability);
//                                        Log.e (TAG, "energy: " + audioFeaturesTrack.energy);
//                                        Log.e (TAG, "instrumentalness: " + audioFeaturesTrack.instrumentalness);
//                                        Log.e (TAG, "key: " + audioFeaturesTrack.key);
//                                        Log.e (TAG, "liveness: " + audioFeaturesTrack.liveness);
//                                        Log.e (TAG, "loudness: " + audioFeaturesTrack.loudness);
//                                        Log.e (TAG, "speechiness: " + audioFeaturesTrack.speechiness);
//                                        Log.e (TAG, "tempo: " + audioFeaturesTrack.tempo);
//                                        Log.e (TAG, "time_signature: " + audioFeaturesTrack.time_signature);
//                                        Log.e (TAG, "valence: " + audioFeaturesTrack.valence);
//                                        Log.e (TAG, "mode: " + audioFeaturesTrack.mode);
                                    }

                                    @Override
                                    public void failure (RetrofitError error) {

                                    }
                                });
                    }

                    @Override
                    public void failure (RetrofitError error) {
                        Log.e (TAG, error.getMessage ());
                    }
                });

        SpotifyService service = mSpotifyService.getValue ();

        service.getSeedsGenres (new Callback <SeedsGenres> () {
            @Override
            public void success (SeedsGenres seedsGenres, Response response) {
                seedsGenres.genres = seedsGenres.genres.subList (0, 1); // first 10

                Map <String, List <Float>> map = new HashMap <> ();

                for (String genre : seedsGenres.genres) {
                    Map <String, Object> p = new HashMap <> ();
                    p.put ("seed_genres", genre);
                    p.put ("limit", 100);

                    service.getRecommendations (params, new Callback <Recommendations> () {
                        @Override
                        public void success (Recommendations recommendations, Response response) {
                            List <String> ids = new ArrayList <> ();

                            for (Track t : recommendations.tracks) {
                                ids.add (t.id);
                            }

                            String tracks = String.join (",", ids);
                            service.getTracksAudioFeatures (tracks, new Callback <AudioFeaturesTracks> () {
                                @Override
                                public void success (AudioFeaturesTracks audioFeaturesTracks, Response response) {
                                    Handler.createAsync (Looper.getMainLooper ())
                                            .post (() -> {
                                                Log.e (TAG, genre + " {");

                                                for (Field f : AudioFeaturesTrack.class.getFields ()) {
                                                    try {
                                                        String field = f.getName ();
//                                                        Log.e (TAG, "field class - " + f.getType ());
                                                        if (f.getType ().equals (float.class) || f.getType ().equals (int.class)) {
                                                            Analysis analysis = getSigma (audioFeaturesTracks, field, map);

                                                            Log.e (TAG, "   " + field + " - " + analysis.m + " (" + (int) (100 * (analysis.sigma / analysis.m)) + "%)");
                                                        }
                                                    } catch (Exception e) {
                                                        // ignore
                                                    }
                                                }

                                                Log.e (TAG, "}");
                                            });
                                }

                                @Override
                                public void failure (RetrofitError error) {

                                }
                            });
                        }

                        @Override
                        public void failure (RetrofitError error) {

                        }
                    });
                }

                Handler.createAsync (Looper.getMainLooper ()).postDelayed (
                        () -> {
                            float minVSum = Float.MIN_VALUE;
                            float maxVSum = Float.MAX_VALUE;
                            String minParam = "";
                            String maxParam = "";

                            for (String param : map.keySet ()) {
                                Float[] v = new Float[map.get (param).size ()];
                                map.get (param).toArray (v);
                                float vSum = getSigma (v).sigma;

                                if (vSum < minVSum) {
                                    minVSum = vSum;
                                    minParam = param;
                                }

                                if (vSum > maxVSum) {
                                    maxVSum = vSum;
                                    maxParam = param;
                                }

                                Log.e (TAG, param + " - sigma=" + vSum);
                            }

                            Log.e (TAG, "param with lowest sigma - " + minParam);
                        },
                        5000
                );
            }

            @Override
            public void failure (RetrofitError error) {

            }
        });
    }

    class Analysis {
        public float m;
        public float sigma;

        public Analysis (float m, float sigma) {
            this.m = m;
            this.sigma = sigma;
        }
    }

    private Analysis getSigma (Float[] values) {
        float sum = 0;
        float sumDSq = 0;

        for (float v : values) {
            sum += v;
        }

        float m = sum / values.length;

        for (float v : values) {
            float d = v - m;
            sumDSq += (d * d);
        }

        float variance = sumDSq / (values.length - 1);

        return new Analysis (m, (float) Math.sqrt (variance));
    }

    private Analysis getSigma (AudioFeaturesTracks audioFeaturesTracks, String param, Map <String, List <Float>> map) {
        float sum = 0;
        float sumDSq = 0;

        for (AudioFeaturesTrack audioFeatures : audioFeaturesTracks.audio_features) {
            try {
                sum += (float) audioFeatures.getClass ().getField (param).get (audioFeatures);
            } catch (IllegalAccessException e) {
//                e.printStackTrace ();
            } catch (NoSuchFieldException e) {
//                e.printStackTrace ();
            }
        }

        float m = sum / audioFeaturesTracks.audio_features.size ();

        for (AudioFeaturesTrack audioFeatures : audioFeaturesTracks.audio_features) {
            try {
                float d = (float) audioFeatures.getClass ().getField (param).get (audioFeatures) - m;
                sumDSq += (d * d);

                map.putIfAbsent (param, new ArrayList <> ());
                float value = (float) audioFeatures.getClass ().getField (param).get (audioFeatures);
                List <Float> v = map.get (param);
                v.add (value);
            } catch (IllegalAccessException e) {
//                e.printStackTrace ();
            } catch (NoSuchFieldException e) {
//                e.printStackTrace ();
            }
        }

        float variance = sumDSq / (audioFeaturesTracks.audio_features.size () - 1);

        return new Analysis (m, (float) Math.sqrt (variance));
    }

    public void nextTrack (Context context) {
        TrackWrapper nextTrack = mTracks.poll ();
        if (nextTrack != null) {
            mTrack.setValue (nextTrack);

            boolean needsAlbum = mAlbum == null || !mAlbum.getValue ().album.id.equals (mTrack.getValue ().track.album.id);

            if (needsAlbum && AppRepo.getInstance ().isAlbumCached (context, nextTrack.track.album.id)) {
                album (context, nextTrack.track.album.id);
            }

            if (mTracks.peek () != null) {
                mNextTrack.setValue (mTracks.peek ());
            } else {
                mNeedsNextTrack = true;
                mNextTrack.setValue (null);
            }
        } else {
            mNeedsTrack = true;
            mTrack.setValue (null);
        }

        if (!mIsWaiting) {
            mIsWaiting = true;
            AppRepo.getInstance ()
                    .getNextTrack (mSpotifyService.getValue (), new AppRepo.NextTrackListener () {
                        @Override
                        public void onGetTrack (TrackWrapper trackWrapper) {
                            if (mNeedsTrack) {
                                mNeedsTrack = false;
                                mTrack.setValue (trackWrapper);
                                if (AppRepo.getInstance ().isAlbumCached (context, trackWrapper.track.album.id)) {
                                    album (context, trackWrapper.track.album.id);
                                }
                            } else {
                                mTracks.add (trackWrapper);

                                if (mNeedsNextTrack) {
                                    mNeedsNextTrack = false;
                                    mNextTrack.setValue (trackWrapper);
                                }
                            }

                            if (mTracks.size () <= 1) {
                                AppRepo.getInstance ()
                                        .getNextTrack (mSpotifyService.getValue (), this);
                            } else {
                                mIsWaiting = false;
                            }
                        }
                    });
        }
    }

    public void album2 (Context context, String id) {
        AppRepo.getInstance ()
                .getAlbum (getSpotifyService ().getValue (), context, id, new AppRepo.ResultListener () {
                    @Override
                    public void onAlbum (AlbumWrapper albumWrapper) {
                        super.onAlbum (albumWrapper);
                        Handler.createAsync (Looper.getMainLooper ())
                                .post (() -> mAlbum.setValue (albumWrapper));
                    }
                });
    }

    private static void cacheAlbumObject (Context context, AlbumWrapper wrappedAlbum) {
        Album album = wrappedAlbum.album;
        Bitmap bitmap = wrappedAlbum.bitmap;

        wrappedAlbum.album.available_markets = Arrays.asList ("");

        for (TrackSimple trackSimple : wrappedAlbum.album.tracks.items) {
            trackSimple.available_markets = Arrays.asList ("");
        }

        try {
            FileOutputStream albumOutputStream = new FileOutputStream (context.getCacheDir () + "/" + album.id + ".album");
            FileOutputStream outputStream = new FileOutputStream (context.getCacheDir () + "/" + album.id + ".jpeg");
            bitmap.compress (Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.flush ();
            outputStream.close ();

            Gson gson = new GsonBuilder ()
                    .setPrettyPrinting ()
                    .setLenient ()
                    .create ();

            String s = gson.toJson (album);
            albumOutputStream.write (s.getBytes (StandardCharsets.UTF_8));
            albumOutputStream.flush ();
            albumOutputStream.close ();
        } catch (Exception e) {
        }
    }

    private static AlbumWrapper buildAlbumObject (Context context, String id) {
        AlbumWrapper wrappedAlbum = null;

        Album album = null;
        Bitmap bitmap = null;

        try {
            FileInputStream albumInputStream = new FileInputStream (context.getCacheDir () + "/" + id + ".album");
            FileInputStream inputStream = new FileInputStream (context.getCacheDir () + "/" + id + ".jpeg");
            bitmap = BitmapFactory.decodeStream (inputStream);

            Gson gson = new GsonBuilder ()
                    .setPrettyPrinting ()
                    .setLenient ()
                    .create ();

            byte[] bytes = new byte[albumInputStream.available ()];
            albumInputStream.read (bytes);

            album = gson.fromJson (new String (bytes), Album.class);
        } catch (Exception e) {
            return null;
        }

        wrappedAlbum = new AlbumWrapper (album, bitmap);

        return wrappedAlbum;
    }

    public void album (Context context, String id) {
        AlbumWrapper wrappedAlbum;

        if ((wrappedAlbum = buildAlbumObject (context, id)) != null)
            mAlbum.setValue (wrappedAlbum);//listener.onAlbum (wrappedAlbum);

        Executors.newSingleThreadExecutor ()
                .execute (() -> {
                    Album album = mSpotifyService.getValue ()
                            .getAlbum (id);

                    Bitmap bitmap = BitmapTools.INSTANCE.from (album.images.get (0).url);

                    AlbumWrapper wa = new AlbumWrapper (album, bitmap);

                    cacheAlbumObject (context, wa);
                });
    }

    public LiveData <AlbumWrapper> getAlbum () {
        return mAlbum;
    }

    public LiveData <TrackWrapper> getTrack () {
        return mTrack;
    }

    public void setTrack (TrackWrapper track) {
        mTrack.setValue (track);
    }

    public LiveData <TrackWrapper> getNextTrack () {
        return mNextTrack;
    }

    public void setUser (UserWrapper userWrapper) {
        mUser.setValue (userWrapper);
    }

    public LiveData <UserWrapper> getUser () {
        return mUser;
    }

    public void setSpotifyService (SpotifyService spotifyService) {
        mSpotifyService.setValue (spotifyService);
    }

    public LiveData <SpotifyService> getSpotifyService () {
        return mSpotifyService;
    }

    public void setSpotifyAppRemote (SpotifyAppRemote spotifyAppRemote) {
        mSpotifyAppRemote.setValue (spotifyAppRemote);
    }

    public LiveData <SpotifyAppRemote> getSpotifyAppRemote () {
        return mSpotifyAppRemote;
    }

    public void setProgress (long progress) {
        mProgress.setValue (progress);
    }

    public LiveData <Long> getProgress () {
        return mProgress;
    }

    public void play () {
        PlayerApi playerApi = mSpotifyAppRemote.getValue ().getPlayerApi ();
        Track track = mTrack.getValue ().track;
        if (playerApi == null || track == null) return;

        startPlayerStateListener ();

        playerApi.getPlayerState ()
                .setResultCallback (data -> {
                    if (data.track.uri.equals (track.uri)) playerApi.resume ();
                    else playerApi.play (track.uri);

                    playerApi.setRepeat (Repeat.ONE);
                });
    }

    public void play (TrackWrapper wrappedTrack) {
        PlayerApi playerApi = mSpotifyAppRemote.getValue ().getPlayerApi ();
        Track track = wrappedTrack.track;
        if (playerApi == null || track == null) return;

        startPlayerStateListener ();

        playerApi.play (track.uri);
    }

    public void pause () {
        PlayerApi playerApi = mSpotifyAppRemote.getValue ().getPlayerApi ();

        stopPlayerStateListener ();

        if (playerApi == null) return;

        playerApi.pause ();
    }

    private void startPlayerStateListener () {
        if (mPlayerStateListener == null)
            mPlayerStateListener = Executors.newSingleThreadScheduledExecutor ();

        mPlayerStateListener.scheduleAtFixedRate (() -> {
            mSpotifyAppRemote.getValue ()
                    .getPlayerApi ()
                    .getPlayerState ()
                    .setResultCallback (data -> {
                        mProgress.setValue (data.playbackPosition);
                    });
        }, 0, 500, TimeUnit.MILLISECONDS);
    }

    private void stopPlayerStateListener () {
        if (mPlayerStateListener == null) return;
        mPlayerStateListener.shutdown ();
    }
}
