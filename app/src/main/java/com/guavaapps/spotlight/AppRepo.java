package com.guavaapps.spotlight;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.guavaapps.components.bitmap.BitmapTools;
import com.pixel.spotifyapi.Objects.Album;
import com.pixel.spotifyapi.Objects.Artist;
import com.pixel.spotifyapi.Objects.Track;
import com.pixel.spotifyapi.Objects.TrackSimple;
import com.pixel.spotifyapi.Objects.UserPrivate;
import com.pixel.spotifyapi.SpotifyService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Executors;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class AppRepo {
    private static final String TAG = "AppRepo";

    private Queue <String> trackQueue = new LinkedList <> ();
    private List <String> mPlaylists = new ArrayList <> ();
    private String mPinnedPlaylist;

    private List <Listener> mListeners = new ArrayList <> ();

    private static AppRepo sInstance;
    private Object mLock;
    private boolean mIsWaiting;

    private AppRepo () {

    }

    public static AppRepo getInstance () {
        if (sInstance == null) sInstance = new AppRepo ();

        return sInstance;
    }

    String r;

    Queue <String> queue = new LinkedList <> ();
    List <String> tracks = new ArrayList <> ();

    public void getNextTrack (SpotifyService spotifyService, NextTrackListener listener) {
//        if (mIsWaiting) return;
//        mIsWaiting = true;

        String t1 = "37lsV513gD04gFvKIPCw4N";

        String t2 = "5rurZZeggozpAZIHbI55cm";
        String t3 = "0rI56S1biB0efYypn7eNpP";
        String t4 = "7aL34pRMvHtAy4OUdynAad";
        String t5 = "1XVV4kghwYVW33D8JrnnYy";

        if (queue.isEmpty ()) queue.addAll (Arrays.asList (t1, t2, t3, t4, t5));

        r = queue.poll ();

        spotifyService.getTrack (r, new Callback <Track> () {
                    @Override
                    public void success (Track track, Response response) {
                        Executors.newSingleThreadExecutor ()
                                .execute (() -> {
                                    Bitmap bitmap = BitmapTools.INSTANCE.from (track.album.images.get (0).url);

                                    Handler.createAsync (Looper.getMainLooper ())
                                            .post (() -> {
                                                listener.onGetTrack (new TrackWrapper (track, bitmap));
//                                                mIsWaiting = false;
                                            });
                                });
                    }

                    @Override
                    public void failure (RetrofitError error) {

                    }
                });
    }

    public boolean isAlbumCached (Context context, String id) {
        File c = new File (context.getCacheDir (), id + ".album");
        if (c.exists ()) return true;

        return false;
    }

    public AlbumWrapper getAlbum (SpotifyService spotifyService, Context context, String id) {
        AlbumWrapper wrappedAlbum;

        if ((wrappedAlbum = buildAlbumObject (context, id)) != null) return wrappedAlbum;

        Album album = spotifyService
                .getAlbum (id);

        Bitmap bitmap = BitmapTools.INSTANCE.from (album.images.get (0).url);

        wrappedAlbum = new AlbumWrapper (album, bitmap);

        cacheAlbumObject (context, wrappedAlbum);

        return wrappedAlbum;
    }

    public void getAlbum (SpotifyService spotifyService, Context context, String id, ResultListener listener) {
        AlbumWrapper wrappedAlbum;

        if ((wrappedAlbum = buildAlbumObject (context, id)) != null) listener.onAlbum (wrappedAlbum);

        Executors.newSingleThreadExecutor ()
                .execute (() -> {
                    Album album = spotifyService
                            .getAlbum (id);

                    Bitmap bitmap = BitmapTools.INSTANCE.from (album.images.get (0).url);

                    AlbumWrapper wa = new AlbumWrapper (album, bitmap);

                    cacheAlbumObject (context, wa);

                    listener.onAlbum (wa);
                });
    }

//    public ArtistWrapper getArtist (SpotifyService spotifyService, Context context, String id) {
//        ArtistWrapper wrappedArtist;
//
//        if ((wrappedArtist = buildArtistObject (context, id)) != null) return wrappedArtist;
//
//        Artist artist = spotifyService
//                .getArtist (id);
//
//        Bitmap bitmap = BitmapTools.INSTANCE.from (artist.images.get (0).url);
//
//        wrappedArtist = new ArtistWrapper (artist, bitmap);
//
//        cacheArtistObject (context, wrappedArtist);
//
//        return wrappedArtist;
//    }

//    public void cacheArtistObject (Context context, ArtistWrapper wrappedArtist) {
//        Array array = new Array ();
//
//        Artist artist = wrappedArtist.artist;
//        array.put ("id", artist.id);
//        array.put ("uri", artist.uri);
//        array.put ("name", artist.name);
//        array.put ("bitmap", wrappedArtist.thumbnail);
//
//        byte[] bytes = ArrayTools.toBytes (array);
//
//        try {
//            FileOutputStream outputStream = new FileOutputStream (context.getCacheDir () + "/" + artist.id + ".artist");
//            outputStream.write (bytes);
//            outputStream.flush ();
//            outputStream.close ();
//        } catch (Exception e) {
//            e.printStackTrace ();
//        }
//    }

    public void cacheAlbumObject (Context context, AlbumWrapper wrappedAlbum) {
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
            Log.e (TAG, "album cache failed");
        }
    }

    public void cacheUserObject (Context context, UserWrapper wrappedUser) {
        UserPrivate user = wrappedUser.user;
        Bitmap bitmap = wrappedUser.thumbnail;

        try {
            FileOutputStream userOutputStream = new FileOutputStream (context.getCacheDir () + "/" + user.id + ".user");
            FileOutputStream outputStream = new FileOutputStream (context.getCacheDir () + "/" + user.id + ".jpeg");
            bitmap.compress (Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.flush ();
            outputStream.close ();

            Gson gson = new GsonBuilder ()
                    .setPrettyPrinting ()
                    .setLenient ()
                    .create ();

            String s = gson.toJson (user);
            userOutputStream.write (s.getBytes (StandardCharsets.UTF_8));
            userOutputStream.flush ();
            userOutputStream.close ();
        } catch (Exception e) {
            Log.e (TAG, "user cache failed");
        }
    }

    public void cacheTrackObject (Context context, TrackWrapper wrappedTrack) {
        Track track = wrappedTrack.track;
        Bitmap bitmap = wrappedTrack.thumbnail;

        wrappedTrack.track.available_markets = Arrays.asList ("");

        try {
            FileOutputStream albumOutputStream = new FileOutputStream (context.getCacheDir () + "/" + track.id + ".album");
            FileOutputStream outputStream = new FileOutputStream (context.getCacheDir () + "/" + track.id + ".jpeg");
            bitmap.compress (Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.flush ();
            outputStream.close ();

            Gson gson = new GsonBuilder ()
                    .setPrettyPrinting ()
                    .setLenient ()
                    .create ();

            String s = gson.toJson (track);
            albumOutputStream.write (s.getBytes (StandardCharsets.UTF_8));
            albumOutputStream.flush ();
            albumOutputStream.close ();
        } catch (Exception e) {
            Log.e (TAG, "track cache failed");
        }
    }

    public ArtistWrapper buildArtistObject (Context context, String id) {
        ArtistWrapper wrappedArtist;

        Artist artist;
        Bitmap bitmap;

        try {
            FileInputStream artistInputStream = new FileInputStream (context.getCacheDir () + "/" + id + ".artist");
            FileInputStream inputStream = new FileInputStream (context.getCacheDir () + "/" + id + ".jpeg");
            bitmap = BitmapFactory.decodeStream (inputStream);

            Gson gson = new GsonBuilder ()
                    .setPrettyPrinting ()
                    .setLenient ()
                    .create ();

            byte[] bytes = new byte[artistInputStream.available ()];
            artistInputStream.read (bytes);

            artist = gson.fromJson (new String (bytes), Artist.class);
        } catch (Exception e) {
            return null;
        }

        wrappedArtist = new ArtistWrapper (artist, bitmap);

        return wrappedArtist;
    }

    public TrackWrapper buildTrackObject (Context context, String id) {
        TrackWrapper wrappedTrack;

        Track track;
        Bitmap bitmap;

        try {
            FileInputStream trackInputStream = new FileInputStream (context.getCacheDir () + "/" + id + ".track");
            FileInputStream inputStream = new FileInputStream (context.getCacheDir () + "/" + id + ".jpeg");
            bitmap = BitmapFactory.decodeStream (inputStream);

            Gson gson = new GsonBuilder ()
                    .setPrettyPrinting ()
                    .setLenient ()
                    .create ();

            byte[] bytes = new byte[trackInputStream.available ()];
            trackInputStream.read (bytes);

            track = gson.fromJson (new String (bytes), Track.class);
        } catch (Exception e) {
            return null;
        }

        wrappedTrack = new TrackWrapper (track, bitmap);

        return wrappedTrack;
    }

    public AlbumWrapper buildAlbumObject (Context context, String id) {
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

    public UserWrapper buildUserObject (Context context, String id) {
        UserWrapper wrappedUser = null;

        UserPrivate user = null;
        Bitmap bitmap = null;

        try {
            FileInputStream albumInputStream = new FileInputStream (context.getCacheDir () + "/" + id + ".user");
            FileInputStream inputStream = new FileInputStream (context.getCacheDir () + "/" + id + ".jpeg");
            bitmap = BitmapFactory.decodeStream (inputStream);

            Gson gson = new GsonBuilder ()
                    .setPrettyPrinting ()
                    .setLenient ()
                    .create ();

            byte[] bytes = new byte[albumInputStream.available ()];
            albumInputStream.read (bytes);

            user = gson.fromJson (new String (bytes), UserPrivate.class);
        } catch (Exception e) {
            return null;
        }

        wrappedUser = new UserWrapper (user, bitmap);

        return wrappedUser;
    }

//    public PlaylistWrapper buildPlaylistObject (Context context, String id) {
//        PlaylistWrapper wrappedPlaylist = null;
//
//        Playlist playlist = null;
//        Bitmap bitmap = null;
//
//        try {
//            FileInputStream albumInputStream = new FileInputStream (context.getCacheDir () + "/" + id + ".playlist");
//            FileInputStream inputStream = new FileInputStream (context.getCacheDir () + "/" + id + ".jpeg");
//            bitmap = BitmapFactory.decodeStream (inputStream);
//
//            Gson gson = new GsonBuilder ()
//                    .setPrettyPrinting ()
//                    .setLenient ()
//                    .create ();
//
//            byte[] bytes = new byte[albumInputStream.available ()];
//            albumInputStream.read (bytes);
//
//            playlist = gson.fromJson (new String (bytes), Playlist.class);
//        } catch (Exception e) {
//            return null;
//        }
//
//        wrappedPlaylist = new PlaylistWrapper (playlist, bitmap);
//
//        return wrappedPlaylist;
//    }

    public void getCurrentUser (SpotifyService spotifyService, Context context, ResultListener listener) {
        Executors.newSingleThreadExecutor ()
                .execute (() -> {
                    UserPrivate user = spotifyService
                            .getCurrentUser ();

                    Bitmap bitmap;
                    if (user.images.size () > 0) {
                        bitmap = BitmapTools.INSTANCE.from (user.images.get (0).url);
                    }
                    else {
                        bitmap = Bitmap.createBitmap (1080, 1080, Bitmap.Config.ARGB_8888);
                    }

                    UserWrapper wu = new UserWrapper (user, bitmap);

                    listener.onUser (wu);
                });
    }

//    public void getUserPlaylists (Context context, String userId, String id, ResultListener listener) {
//        List <PlaylistWrapper> playlists = new ArrayList <> ();
//
//        SpotifyServiceAdapter.getServiceInstance ()
//                .getCurrentUserPlaylists (new Callback <Pager <PlaylistSimple>> () {
//                    @Override
//                    public void success (Pager <PlaylistSimple> playlistSimplePager, Response response) {
//                        for (PlaylistSimple playlistSimple : playlistSimplePager.items) {
//                            PlaylistWrapper wrappedPlaylist;
//
//                            if ((wrappedPlaylist = buildPlaylistObject (context, id)) != null && wrappedPlaylist.playlist.snapshot_id.equals (playlistSimple.snapshot_id)) {
//                                playlists.add (wrappedPlaylist);
//
//                                if (playlists.size () == playlistSimplePager.total) listener.onUserPlaylists (playlists);
//                            }
//                            else {
//                                SpotifyServiceAdapter.getServiceInstance ()
//                                        .getPlaylist (userId, id, new Callback <Playlist> () {
//                                            @Override
//                                            public void success (Playlist playlist, Response response) {
//                                                Bitmap bitmap = BitmapTools.from (playlist.images.get (0).url);
//
//                                                PlaylistWrapper playlistWrapper = new PlaylistWrapper (playlist, bitmap);
//
//                                                playlists.add (playlistWrapper);
//
//                                                if (playlists.size () == playlistSimplePager.total) listener.onUserPlaylists (playlists);
//                                            }
//
//                                            @Override
//                                            public void failure (RetrofitError error) {
//
//                                            }
//                                        });
//                            }
//                        }
//                    }
//
//                    @Override
//                    public void failure (RetrofitError error) {
//
//                    }
//                });
//    }

    @Deprecated
    public void getUser (SpotifyService spotifyService, Context context, String id, ResultListener listener) {
        UserWrapper wrappedUser;

        if ((wrappedUser = buildUserObject (context, id)) != null) listener.onUser (wrappedUser);

        Executors.newSingleThreadExecutor ()
                .execute (() -> {
                    UserPrivate user = spotifyService
                            .getCurrentUser ();

                    Bitmap bitmap = BitmapTools.INSTANCE.from (user.images.get (0).url);

                    UserWrapper wu = new UserWrapper (user, bitmap);

                    cacheUserObject (context, wu);

                    listener.onUser (wu);
                });
    }

    public void registerListener (Listener l) {
        if (!mListeners.contains (l)) mListeners.add (l);
    }

    public interface NextTrackListener {
        void onGetTrack (TrackWrapper trackWrapper);
    }

    public abstract static class ResultListener {
        public void onUser (UserWrapper userWrapper) {}

        public void onAlbum (AlbumWrapper albumWrapper) {}

        public void onUserPlaylists (List<PlaylistWrapper> playlists) {}
    }

    public interface Listener {
        void onReturnNextTrack (TrackWrapper track);

        void onPlaylistsChanged (List <String> playlists);

        void onPinnedPlaylistChanged (String pinnedPlaylist);
    }
}
