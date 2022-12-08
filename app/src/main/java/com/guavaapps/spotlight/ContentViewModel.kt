package com.guavaapps.spotlight

import android.util.Log
import androidx.lifecycle.*
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.guavaapps.components.bitmap.BitmapTools.from
import com.guavaapps.spotlight.realm.TrackModel
import com.pixel.spotifyapi.Objects.*
import com.pixel.spotifyapi.SpotifyService
import com.spotify.android.appremote.api.AppRemote
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.protocol.types.Empty
import com.spotify.protocol.types.PlayerState
import com.spotify.protocol.types.Repeat
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

private const val TAG = "ViewModel"

class ContentViewModel(
    private var matcha: Matcha,
    private var userRepository: UserRepository,
    private var modelRepository: ModelRepository,
    private var localRealm: LocalRealm,
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
    private val localTimeline = mutableListOf<TrackModel>()
    private val batch = mutableListOf<TrackModel>()

    // observables TODO set getters and setters
    val user = MutableLiveData<UserWrapper>()
    val track = MutableLiveData<TrackWrapper?>()
    val nextTrack = MutableLiveData<TrackWrapper?>()
    val album = MutableLiveData<AlbumWrapper?>()
    val artists = MutableLiveData<List<ArtistWrapper>?>()
    val artistTracks = MutableLiveData<MutableMap<String, List<TrackWrapper>>>()
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

        viewModelScope.launch(Dispatchers.IO) {
            val user = user ?: spotifyService.getCurrentUser()

            login(user)

            updateUser(user)

            getNext()
        }
    }

    fun f() {

    }

    private fun reset() {
        tracks.clear()
        track.value = null
        nextTrack.value = null
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
//            modelRepository.init()
        }
    }

    private suspend fun updateUser(user: UserPrivate) {
        val wrappedUser = UserWrapper(user)

        withContext(Dispatchers.IO) {
            wrappedUser.bitmap = from(user.images[0].url)
        }

        withContext(Dispatchers.Main) {
            this@ContentViewModel.user.value = wrappedUser
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

    fun getNext() {
        Log.e(TAG,
            "getNext () - tracks=${tracks.size} next=[${tracks.peek()?.track?.name} ${if (tracks.size > 1) tracks.toTypedArray()[1]?.track?.name else null}]")

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
                .also { c -> Log.e(TAG, "[pulled from all artists] ${it.artist?.name} apply?=$c") }
        }
    }

    fun getArtistTracks(artist: ArtistSimple) =
        viewModelScope.launch {
            val areAnyNull = allArtists.any { it.artist == null }

            Log.e(TAG, "decided to check if bby any chance any of them were null cos weird shit happens - ${
                if (areAnyNull) "THEY FUCKING ARE"
                else "nop"
            }")

            val needsExplicitReload =
                allArtists.find { it.artist?.id == artist.id }?.thumbnail == null

//            val needsTracks = !(artistTracks.value?.any { it.key == artist.id } ?: false)
            val needsTracks = ! (artistTracks.value?.containsKey(artist.id) ?: false)

            Log.e(TAG, "needsExplicitReload? $needsExplicitReload")

            if (needsExplicitReload) {
                allArtists.indices.find { allArtists[it].artist?.id == artist.id }
                    .takeIf { it != null }!!
                    .let {
                        Log.e (TAG, "i have accepted that the spotify api is a bitch")
                        Log.e (TAG, "therefore i shall not proceed in my attempts to somehow load the images of tracks that FOR WHATEVER REASON dont FUCKING RETURN THE IMAGE URLS")
                        //allArtists[it] = loadArtistExplicitly(artist)
                    }
            }

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

    private suspend fun loadArtistExplicitly(artist: ArtistSimple) = withContext(Dispatchers.IO) {
        spotifyService.getArtist(artist.id).let {
            ArtistWrapper(
                it,
                from(it.images.firstOrNull()?.url)
            )
        }
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

    // the spotify web api can handle a maximum request size of 50 ids
    // TODO impl in SpotifyApi
    private suspend fun splitLoad(ids: List<String>): MutableList<ArtistWrapper> {
        val batchSize = 50

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