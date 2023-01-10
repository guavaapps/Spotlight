package com.guavaapps.spotlight

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.guavaapps.components.bitmap.BitmapTools.from
import com.guavaapps.spotlight.realm.AppUser
import com.guavaapps.spotlight.realm.RealmPlaylist
import com.guavaapps.spotlight.realm.TrackModel
import com.pixel.spotifyapi.Objects.*
import com.pixel.spotifyapi.SpotifyService
import com.spotify.android.appremote.api.AppRemote
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.protocol.types.PlayerState
import com.spotify.protocol.types.Repeat
import io.realm.Realm
import io.realm.RealmList
import io.realm.mongodb.Credentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bson.Document
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.collections.set

private const val TAG = "ViewModel"

class ContentViewModel(
    private var matcha: Matcha,
    private var userRepository: UserRepository,
    private var modelRepository: ModelRepository,
    private var localRealm: Realm,
) : ViewModel() {
    private var isWaiting = false
    private var needsTrack = true
    private var needsNextTrack = true

    // spotify
    private lateinit var spotifyService: SpotifyService
    private lateinit var spotifyAppRemote: AppRemote
    private var playerStateListener: ScheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor()

    // internal
    private val tracks: Queue<TrackWrapper?> = LinkedList()
    private var albums = arrayOf<AlbumWrapper>()
    private var allArtists = mutableListOf<ArtistWrapper>()
    private var userPlaylists = mutableListOf<PlaylistSimple>()

    // local copy of the user's listening history
    private val localTimeline = mutableListOf<TrackModel>()

    private val no = mutableListOf<String>()
    private val batch = mutableListOf<TrackModel>()

    private val playlistsListener = Executors.newSingleThreadScheduledExecutor()

    // observables
    val user = MutableLiveData<UserWrapper>()
    val track = MutableLiveData<TrackWrapper?>()
    val nextTrack = MutableLiveData<TrackWrapper?>()
    val album = MutableLiveData<AlbumWrapper?>()
    val artists = MutableLiveData<List<ArtistWrapper>?>()
    val artistTracks = MutableLiveData<MutableMap<String, List<TrackWrapper>>>()
    val playlists = MutableLiveData<MutableList<PlaylistWrapper>>()
    val playlistTracks = MutableLiveData<MutableMap<String, List<PlaylistTrackWrapper>>>()
    val playlist = MutableLiveData<PlaylistWrapper?>()
    val progress = MutableLiveData<Long>()

    // features map TODO do something about it idk i dont like it tho
    private var featuresMap = mapOf(
        "acousticness" to 0,
        "analysis_url" to 1,
        "danceability" to 2,
        "duration_ms" to 3,
        "energy" to 4,
        "instrumentalness" to 5,
        "key" to 6,
        "liveness" to 7,
        "loudness" to 8,
        "mode" to 9,
        "speechiness" to 10,
        "tempo" to 11,
        "time_signature" to 12,
        "valence" to 13
    )

    fun initForUser(
        spotifyService: SpotifyService,
        spotifyAppRemote: SpotifyAppRemote,
        user: UserPrivate? = null,
    ) {
        this.spotifyService = spotifyService
        this.spotifyAppRemote = spotifyAppRemote

        viewModelScope.launch {
            val user = user ?: spotifyService.getCurrentUser()

            login(user)

            updateUser(user)

            loadPlaylists()

            getNext()
        }
    }

    private suspend fun login(user: UserPrivate) {
        withContext(Dispatchers.IO) {
            val userId = user.id

            val credentials = Credentials.customFunction(
                Document(
                    mapOf(
                        "spotify_id" to userId,
                    )
                )
            )

//            modelRepository.close()
//            matcha.logout()

            matcha.login(credentials)
            modelRepository.init()
        }


        val model = modelRepository.model
        val timeline = arrayOf(
            floatArrayOf(0f, 0.5f, 0.2f, 0.74f, 0.24f)
        )

        val o = model.getNext(timeline)

        Log.e(TAG, "output - ${o.joinToString()}")
    }

    private suspend fun logout() {
        matcha.logout()
        reset()
    }

    private fun reset() {
        tracks.clear()
        track.value = null
        nextTrack.value = null
    }

    private suspend fun updateUser(user: UserPrivate) {
        val wrappedUser = UserWrapper(user)

        withContext(Dispatchers.IO) {
            wrappedUser.bitmap = from(user.images[0].url)
        }

        withContext(Dispatchers.Main) {
            this@ContentViewModel.user.value = wrappedUser
        }

        val appUser = AppUser().apply {
            spotify_id = user.id
            last_login = Date(System.currentTimeMillis())
            locale = user.country
        }

        withContext(Dispatchers.IO) {
            matcha.where(AppUser::class.java)
                .equalTo("_id", user.id).let {
                    val user = it.findFirst()
                    appUser.created = user?.created
                    appUser.playlist = user?.playlist

                    it.update(appUser)
                }

        }
    }

    private suspend fun pullBatch(): Array<TrackModel> {
        // pull local

        return emptyArray()
    }

    private fun getNextTrackFeatures(): FloatArray {
        val model = modelRepository.model

        val timeline = localTimeline.map { it.features.toFloatArray() }.toTypedArray()

        return model.getNext(timeline)
    }

    private fun createParamsObject(features: FloatArray): Map<String, Any> {
        var map = mutableMapOf<String, Any>(
            "genre_seed" to "hip_hop"
        )
        map.putAll(
            features.mapIndexed { i, feature -> "target_" + featuresMap.filterValues { it == i }.keys.first() to feature }
                .toMap()
        )

        Log.e(TAG, "map - $map")

        return mutableMapOf<String, Any>(
            "limit" to 5,
            "seed_genres" to "hip-hop"
        ).toMap()
    }

    private fun optimiseModel() {
        modelRepository.optimiseModel()
    }

    //i love you so fucking much and  when i looked at you just now i thought my heart was gonna explode
    fun logTrack(track: Track) {
        batch.add(TrackModel(
            track.id,
            track.uri,
            System.currentTimeMillis()
        ))
    }

    private suspend fun pushBatch() {
        batch.injectFeatures()

        withContext(Dispatchers.IO) {
            with(userRepository.get()) {
                timeline.addAll(batch)
                localTimeline.clear()
                localTimeline.addAll(batch)
                batch.clear()
            }

            playlist.value!!.playlist?.snapshot_id = spotifyService.addTracksToPlaylist(
                user.value!!.user.id, playlist.value!!.playlist?.id,
                mapOf("uris" to batch.map { it.uri }.joinToString(",")), null
            ).snapshot_id
        }
    }

    // ui
    private var first = true

    fun add() {
        val track = this.track.value!!.track

        batch.add(TrackModel(
            track.id,
            track.uri,
            System.currentTimeMillis()
        ))

        getNext()
    }

    fun no() {
        no.add(track.value!!.track.id)

        getNext()
    }

    fun getNext() {
        if (
            track.value != null &&
            (!batch.any { it.id == track.value?.track?.id } ||
                    !no.contains(track.value?.track?.id))
        ) return

        viewModelScope.launch {
            // The next batch has already been requested from the Spotify Api, listen for changes to ContentViewModel.track
            if (isWaiting) return@launch

            val next = tracks.poll()

            if (next?.track?.album?.id != album.value?.album?.id) {
                album.value?.album = null
            }

            track.value = next

            if (next != null) {
                //setAlbumBitmap(withContext(Dispatchers.IO) { from(next.track.album.images[0].url)!! })
                applyAlbum(next.track)
            }

            if (tracks.peek() == null) {
                Log.e(TAG, "doing GET")

                nextTrack.value = null

                if (first) {
                    first = false
                    get()
                }
            } else {
                Log.e(TAG, "doing SET")
                nextTrack.value = tracks.peek()
            }
        }
    }

    private fun applyArtists(track: Track) {
        Log.e(TAG, "applyArtists() - track=${track.name}{${track.id}}")

        val album = albums.find { it.album?.id == track.album.id }

        val ids = arrayOf(
            *track.artists.map { it.id }.toTypedArray(),
            *album?.album?.tracks?.items?.flatMap { it.artists.map { it.id } }!!
                .toTypedArray()).distinct()

        Log.e(TAG, "ids - $ids")

        artists.value = allArtists.filter {
            ids.contains(it.artist?.id ?: "")
        }
    }

    fun getArtistTracks(artist: ArtistSimple) = viewModelScope.launch {
        val needsTracks = !(artistTracks.value?.containsKey(artist.id) ?: false)

        if (needsTracks) {
            val tracks = loadArtistTracks(artist)

            if (artistTracks.value != null) {
                val copy = artistTracks.value!!
                copy[artist.id] = tracks
                artistTracks.value = copy
            } else artistTracks.value = mutableMapOf(artist.id to tracks)
        }
    }

    private fun applyAlbum(track: Track? = this.track.value?.track) = viewModelScope.launch {
        applyArtists(track!!)

        album.value = with(albums.find { it.album?.id == track?.album?.id }) {
            if (this != null) this
            else {
                loadAlbums(tracks.map { it!!.track }.toTypedArray())
                albums.first()
            }
        }
    }

    // ui loaders
    private suspend fun get() {
        isWaiting = true
        Log.e(TAG, "get()")

        val nextTrackFeatures = floatArrayOf()//getNextTrackFeatures()
        val requestObject = createParamsObject(nextTrackFeatures)

        val t =
            withContext(Dispatchers.IO) {
                spotifyService.getRecommendations(requestObject)
            }

        loadAlbums(t.tracks.toTypedArray())

        if (track.value == null) {
            track.value = t!!.tracks.removeFirst().let {
                TrackWrapper(it,
                    albums.find { w -> w.album?.id == it.album.id }?.bitmap
                )
            }

            applyAlbum()
        }

        if (nextTrack.value == null) {
            nextTrack.value = t!!.tracks.first().let {
                TrackWrapper(it,
                    albums.find { w -> w.album?.id == it.album.id }?.bitmap
                )
            }
        }

        tracks.addAll(t!!.tracks
            .map {
                TrackWrapper(
                    it,
                    albums.find { w -> w.album?.id == it.album.id }?.bitmap
                )
            })

        isWaiting = false
    }

    private suspend fun loadArtistTracks(artist: ArtistSimple): List<TrackWrapper> {
        val tracks = withContext(Dispatchers.IO) {
            spotifyService.getArtistTopTrack(artist.id,
                user.value?.user?.country ?: "")
                .tracks.map {
                    TrackWrapper(
                        it,
                        from(it.album.images.firstOrNull()?.url)
                    )
                }
        }

        return tracks
    }

    private suspend fun loadArtists(albums: List<Album>, reloadAll: Boolean = true) {
        val artists =
            albums.flatMap { it.tracks.items.flatMap { it.artists } }.distinctBy { it.id }.take(27)
        val ids = artists.joinToString(",") { it.id }

        allArtists = if (artists.size > 10) splitLoad(ids.split(","))
        else withContext(Dispatchers.IO) {
            spotifyService.getArtists(ids).artists.map {
                ArtistWrapper(
                    it,
                    from(it.images.firstOrNull()?.url)
                )
            }.toMutableList()
        }
    }

    private suspend fun loadAlbums(tracks: Array<Track>) {
        val ids = tracks.joinToString(",") { it.album.id }

        albums = withContext(Dispatchers.IO) {
            spotifyService.getAlbums(ids).albums.map {
                AlbumWrapper(it,
                    from(it.images.first().url)
                )
            }
        }.toTypedArray()

        loadArtists(albums.map { it.album!! })
    }

    // playlists

    /**
     * load all
     * get main id
     * update ui
     */
    private fun getMainPlaylistId(): String? {
        localRealm.executeTransaction {
            it.where(RealmPlaylist::class.java)
                .findAll()
                .deleteAllFromRealm()
        }

        return null
    }

    private suspend fun loadUserPlaylists() {
        withContext(Dispatchers.IO) {
            userPlaylists = spotifyService.getCurrentUserPlaylists().items
        }
    }

    private suspend fun prepareMainPlaylist() {
        val id = getMainPlaylistId()
            ?: userPlaylists.firstOrNull { it.owner.id == user.value?.user?.id }?.id

        playlist.value = userPlaylists.find { it.id == id }.let {
            Playlist().apply {
                this.id = it?.id
                this.name = it?.name
                this.uri = it?.uri
            }.wrap()
        }
    }

    private suspend fun loadPlaylists() {
        loadUserPlaylists()

        playlists.value = userPlaylists
            .filter {
                (it.owner.id == user.value?.user?.id).also { b ->
                    Log.e(TAG,
                        "${it.name} - accepted=$b name=${it.owner.display_name} user=${user.value?.user?.id}")
                }
            }
            .map {
                Playlist().apply {
                    id = it.id
                    name = it.name
                    images = it.images
                }.wrap()
            }.toMutableList()

        prepareMainPlaylist()
    }

    // the spotify web api can handle a maximum request size of 50 ids
    private suspend fun splitLoad(
        ids: List<String>,
        batchSize: Int = 50,
    ): MutableList<ArtistWrapper> {
        val artists = mutableListOf<ArtistWrapper>()

        ids.chunked(batchSize).forEach {
            val ids = it.joinToString(",")

            artists.addAll(
                withContext(Dispatchers.IO) {
                    spotifyService.getArtists(ids).artists.map {
                        ArtistWrapper(
                            it,
                            from(it.images.firstOrNull()?.url)
                        )
                    }
                }
            )
        }

        return artists
    }

    //player state
    fun play() {
        val uri = track.value!!.track.uri

        play(uri)
    }

    fun play(uri: String) {
        val playerApi = spotifyAppRemote.playerApi

        startPlayerStateListener()

        playerApi.playerState.setResultCallback {
            if (it.track.uri == uri) playerApi.resume() else playerApi.play(uri)
            if (it.playbackOptions.repeatMode != Repeat.ONE) playerApi.setRepeat(Repeat.ONE)
        }

        playerApi.play(uri)
    }

    fun pause() {
        val playerApi = spotifyAppRemote.playerApi
        stopPlayerStateListener()
        playerApi.pause()
    }

    fun seekTo(position: Long) {
        progress.setValue(position)
    }

    private fun startPlayerStateListener() {
        playerStateListener.scheduleAtFixedRate({
            spotifyAppRemote
                .playerApi
                .playerState
                .setResultCallback { data: PlayerState -> progress.setValue(data.playbackPosition) }
        }, 0, 500, TimeUnit.MILLISECONDS)
    }

    private fun stopPlayerStateListener() {
        playerStateListener.shutdown()
    }

// extension utils

    // set track features of each track of a mutable list of track models
    private suspend fun MutableList<TrackModel>.injectFeatures() {
        val ids = this.map { it.id }.joinToString(",")

        return withContext(Dispatchers.IO) {
            spotifyService.getTracksAudioFeatures(ids)
                .audio_features
                .forEach {
                    this@injectFeatures.find { track -> track.id == it.id }!!
                        .features = RealmList(*it.extractFeatures().toTypedArray())
                }
        }
    }

    // get the features of a track as a float array indexed based on featuresMap using reflection
    private fun AudioFeaturesTrack.extractFeatures(): FloatArray {
        val features = Array(featuresMap.size) { i ->
            val f = featuresMap.filterValues { it == i }.keys.first()
            AudioFeaturesTrack::class.java
                .getField(f)
                .get(this) as Float
        }

        return features.toFloatArray()
    }

    // view model
    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras,
            ): T {
                val app = extras[APPLICATION_KEY] as Ky

                return ContentViewModel(
                    app.matcha,
                    app.userRepository,
                    app.modelRepository,
                    app.localRealm
                ) as T
            }
        }
    }
}