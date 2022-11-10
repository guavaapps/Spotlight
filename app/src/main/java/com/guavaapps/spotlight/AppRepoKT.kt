package com.guavaapps.spotlight

import android.content.Context
import com.guavaapps.components.bitmap.BitmapTools.from
import com.pixel.spotifyapi.SpotifyService
import android.graphics.Bitmap
import com.guavaapps.components.bitmap.BitmapTools
import android.os.Looper
import retrofit.RetrofitError
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.guavaapps.spotlight.AppRepoKT
import com.guavaapps.spotlight.UserWrapper
import com.pixel.spotifyapi.Objects.UserPrivate
import android.graphics.BitmapFactory
import android.os.Handler
import android.util.Log
import com.guavaapps.spotlight.PlaylistWrapper
import com.pixel.spotifyapi.Objects.Album
import com.pixel.spotifyapi.Objects.Artist
import com.pixel.spotifyapi.Objects.Track
import retrofit.Callback
import retrofit.client.Response
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.Exception
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.Executors

class AppRepoKT private constructor() {
    private val trackQueue: Queue<String> = LinkedList()
    private val mPlaylists: List<String> = ArrayList()
    private val mPinnedPlaylist: String? = null
    private val mListeners: MutableList<Listener> = ArrayList()
    private val mLock: Any? = null
    private val mIsWaiting = false
    var r: String? = null
    var queue: Queue<String> = LinkedList()
    var tracks: List<String> = ArrayList()
    fun getNextTrack(spotifyService: SpotifyService, listener: NextTrackListener) {
//        if (mIsWaiting) return;
//        mIsWaiting = true;
        val t1 = "37lsV513gD04gFvKIPCw4N"
        val t2 = "5rurZZeggozpAZIHbI55cm"
        val t3 = "0rI56S1biB0efYypn7eNpP"
        val t4 = "7aL34pRMvHtAy4OUdynAad"
        val t5 = "1XVV4kghwYVW33D8JrnnYy"
        if (queue.isEmpty()) queue.addAll(Arrays.asList(t1, t2, t3, t4, t5))
        r = queue.poll()
        spotifyService.getTrack(r, object : Callback<Track> {
            override fun success(track: Track, response: Response) {
                Executors.newSingleThreadExecutor()
                    .execute {
                        val bitmap = from(track.album.images[0].url)
                        Handler.createAsync(Looper.getMainLooper())
                            .post { listener.onGetTrack(TrackWrapper(track, bitmap)) }
                    }
            }

            override fun failure(error: RetrofitError) {}
        })
    }

    fun isAlbumCached(context: Context, id: String): Boolean {
        val c = File(context.cacheDir, "$id.album")
        return if (c.exists()) true else false
    }

    fun getAlbum(spotifyService: SpotifyService, context: Context, id: String): AlbumWrapper {
        var wrappedAlbum: AlbumWrapper
        if (buildAlbumObject(context, id).also { wrappedAlbum = it!! } != null) return wrappedAlbum
        val album = spotifyService
            .getAlbum(id)
        val bitmap = from(album.images[0].url)
        wrappedAlbum = AlbumWrapper(album, bitmap)
        cacheAlbumObject(context, wrappedAlbum)
        return wrappedAlbum
    }

    fun getAlbum(
        spotifyService: SpotifyService,
        context: Context,
        id: String,
        listener: ResultListener
    ) {
        var wrappedAlbum: AlbumWrapper?
        if (buildAlbumObject(context, id).also { wrappedAlbum = it } != null) listener.onAlbum(
            wrappedAlbum
        )
        Executors.newSingleThreadExecutor()
            .execute {
                val album = spotifyService
                    .getAlbum(id)
                val bitmap = from(album.images[0].url)
                val wa = AlbumWrapper(album, bitmap)
                cacheAlbumObject(context, wa)
                listener.onAlbum(wa)
            }
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
    fun cacheAlbumObject(context: Context, wrappedAlbum: AlbumWrapper) {
        val album = wrappedAlbum.album
        val bitmap = wrappedAlbum.bitmap
        wrappedAlbum.album.available_markets = Arrays.asList("")
        for (trackSimple in wrappedAlbum.album.tracks.items) {
            trackSimple.available_markets = Arrays.asList("")
        }
        try {
            val albumOutputStream =
                FileOutputStream(context.cacheDir.toString() + "/" + album.id + ".album")
            val outputStream =
                FileOutputStream(context.cacheDir.toString() + "/" + album.id + ".jpeg")
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            val gson = GsonBuilder()
                .setPrettyPrinting()
                .setLenient()
                .create()
            val s = gson.toJson(album)
            albumOutputStream.write(s.toByteArray(StandardCharsets.UTF_8))
            albumOutputStream.flush()
            albumOutputStream.close()
        } catch (e: Exception) {
            Log.e(TAG, "album cache failed")
        }
    }

    fun cacheUserObject(context: Context, wrappedUser: UserWrapper) {
        val user = wrappedUser.user
        val bitmap = wrappedUser.thumbnail
        try {
            val userOutputStream =
                FileOutputStream(context.cacheDir.toString() + "/" + user.id + ".user")
            val outputStream =
                FileOutputStream(context.cacheDir.toString() + "/" + user.id + ".jpeg")
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            val gson = GsonBuilder()
                .setPrettyPrinting()
                .setLenient()
                .create()
            val s = gson.toJson(user)
            userOutputStream.write(s.toByteArray(StandardCharsets.UTF_8))
            userOutputStream.flush()
            userOutputStream.close()
        } catch (e: Exception) {
            Log.e(TAG, "user cache failed")
        }
    }

    fun cacheTrackObject(context: Context, wrappedTrack: TrackWrapper) {
        val track = wrappedTrack.track
        val bitmap = wrappedTrack.thumbnail
        wrappedTrack.track.available_markets = Arrays.asList("")
        try {
            val albumOutputStream =
                FileOutputStream(context.cacheDir.toString() + "/" + track.id + ".album")
            val outputStream =
                FileOutputStream(context.cacheDir.toString() + "/" + track.id + ".jpeg")
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            val gson = GsonBuilder()
                .setPrettyPrinting()
                .setLenient()
                .create()
            val s = gson.toJson(track)
            albumOutputStream.write(s.toByteArray(StandardCharsets.UTF_8))
            albumOutputStream.flush()
            albumOutputStream.close()
        } catch (e: Exception) {
            Log.e(TAG, "track cache failed")
        }
    }

    fun buildArtistObject(context: Context, id: String): ArtistWrapper? {
        val wrappedArtist: ArtistWrapper
        val artist: Artist
        val bitmap: Bitmap
        try {
            val artistInputStream =
                FileInputStream(context.cacheDir.toString() + "/" + id + ".artist")
            val inputStream = FileInputStream(context.cacheDir.toString() + "/" + id + ".jpeg")
            bitmap = BitmapFactory.decodeStream(inputStream)
            val gson = GsonBuilder()
                .setPrettyPrinting()
                .setLenient()
                .create()
            val bytes = ByteArray(artistInputStream.available())
            artistInputStream.read(bytes)
            artist = gson.fromJson(String(bytes), Artist::class.java)
        } catch (e: Exception) {
            return null
        }
        wrappedArtist = ArtistWrapper(artist, bitmap)
        return wrappedArtist
    }

    fun buildTrackObject(context: Context, id: String): TrackWrapper? {
        val wrappedTrack: TrackWrapper
        val track: Track
        val bitmap: Bitmap
        try {
            val trackInputStream =
                FileInputStream(context.cacheDir.toString() + "/" + id + ".track")
            val inputStream = FileInputStream(context.cacheDir.toString() + "/" + id + ".jpeg")
            bitmap = BitmapFactory.decodeStream(inputStream)
            val gson = GsonBuilder()
                .setPrettyPrinting()
                .setLenient()
                .create()
            val bytes = ByteArray(trackInputStream.available())
            trackInputStream.read(bytes)
            track = gson.fromJson(String(bytes), Track::class.java)
        } catch (e: Exception) {
            return null
        }
        wrappedTrack = TrackWrapper(track, bitmap)
        return wrappedTrack
    }

    fun buildAlbumObject(context: Context, id: String): AlbumWrapper? {
        var wrappedAlbum: AlbumWrapper? = null
        var album: Album? = null
        var bitmap: Bitmap? = null
        try {
            val albumInputStream =
                FileInputStream(context.cacheDir.toString() + "/" + id + ".album")
            val inputStream = FileInputStream(context.cacheDir.toString() + "/" + id + ".jpeg")
            bitmap = BitmapFactory.decodeStream(inputStream)
            val gson = GsonBuilder()
                .setPrettyPrinting()
                .setLenient()
                .create()
            val bytes = ByteArray(albumInputStream.available())
            albumInputStream.read(bytes)
            album = gson.fromJson(String(bytes), Album::class.java)
        } catch (e: Exception) {
            return null
        }
        wrappedAlbum = AlbumWrapper(album, bitmap)
        return wrappedAlbum
    }

    fun buildUserObject(context: Context, id: String): UserWrapper? {
        var wrappedUser: UserWrapper? = null
        var user: UserPrivate? = null
        var bitmap: Bitmap? = null
        try {
            val albumInputStream = FileInputStream(context.cacheDir.toString() + "/" + id + ".user")
            val inputStream = FileInputStream(context.cacheDir.toString() + "/" + id + ".jpeg")
            bitmap = BitmapFactory.decodeStream(inputStream)
            val gson = GsonBuilder()
                .setPrettyPrinting()
                .setLenient()
                .create()
            val bytes = ByteArray(albumInputStream.available())
            albumInputStream.read(bytes)
            user = gson.fromJson(String(bytes), UserPrivate::class.java)
        } catch (e: Exception) {
            return null
        }
        wrappedUser = UserWrapper(user, bitmap)
        return wrappedUser
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
    fun getCurrentUser(
        spotifyService: SpotifyService,
        context: Context?,
        listener: ResultListener
    ) {
        Executors.newSingleThreadExecutor()
            .execute {
                val user = spotifyService
                    .currentUser
                val bitmap: Bitmap?
                bitmap = if (user.images.size > 0) {
                    from(user.images[0].url)
                } else {
                    Bitmap.createBitmap(1080, 1080, Bitmap.Config.ARGB_8888)
                }
                val wu = UserWrapper(user, bitmap)
                listener.onUser(wu)
            }
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
    @Deprecated("")
    fun getUser(
        spotifyService: SpotifyService,
        context: Context,
        id: String,
        listener: ResultListener
    ) {
        var wrappedUser: UserWrapper?
        if (buildUserObject(context, id).also { wrappedUser = it } != null) listener.onUser(
            wrappedUser
        )
        Executors.newSingleThreadExecutor()
            .execute {
                val user = spotifyService
                    .currentUser
                val bitmap = from(user.images[0].url)
                val wu = UserWrapper(user, bitmap)
                cacheUserObject(context, wu)
                listener.onUser(wu)
            }
    }

    fun registerListener(l: Listener) {
        if (!mListeners.contains(l)) mListeners.add(l)
    }

    interface NextTrackListener {
        fun onGetTrack(trackWrapper: TrackWrapper?)
    }

    abstract class ResultListener {
        fun onUser(userWrapper: UserWrapper?) {}
        fun onAlbum(albumWrapper: AlbumWrapper?) {}
        fun onUserPlaylists(playlists: List<PlaylistWrapper?>?) {}
    }

    interface Listener {
        fun onReturnNextTrack(track: TrackWrapper?)
        fun onPlaylistsChanged(playlists: List<String?>?)
        fun onPinnedPlaylistChanged(pinnedPlaylist: String?)
    }

    companion object {
        private const val TAG = "AppRepo"
        private var sInstance: AppRepoKT? = null
        val instance: AppRepoKT?
            get() {
                if (sInstance == null) sInstance = AppRepoKT()
                return sInstance
            }
    }
}