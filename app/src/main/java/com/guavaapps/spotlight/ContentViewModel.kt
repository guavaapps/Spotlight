package com.guavaapps.spotlight

import android.graphics.Bitmap
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
import kotlin.contracts.ExperimentalContracts

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
    private val localTimeline = mutableListOf<TrackModel>()
    private val batch = mutableListOf<TrackModel>()

    // observables TODO set getters and setters
    val user = MutableLiveData<UserWrapper>()
    val track: MutableLiveData<TrackWrapper?> by lazy {
        MutableLiveData<TrackWrapper?>()
    }

    fun getTrack() = track as LiveData<TrackWrapper?>

    fun setTrack(wrappedTrack: TrackWrapper?) {
        track.value = wrappedTrack
    }

    val nextTrack = MutableLiveData<TrackWrapper?>()
    val album = MutableLiveData<AlbumWrapper?>()
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
                loadAlbum(next.track)
            }


            if (tracks.peek() == null) {
                Log.e(TAG, "doing GET")

                nextTrack.value = null

                if (first) {
                    first = false
                    withLock { get() }
                }
            } else {
                Log.e(TAG, "doing SET")
                nextTrack.value = tracks.peek()
            }
        }
    }

    private var first = true

    private fun setAlbumBitmap(bitmap: Bitmap) {
        if (album.value == null) {
            album.value = AlbumWrapper(null, bitmap)
        } else /*if (album.value?.bitmap == null)*/ {
            album.value = album.value.apply {
                this?.bitmap = bitmap
            }
        }
    }

    private inline fun withLock(block: () -> Unit) {
        isWaiting = true

        block()

        isWaiting = false
    }

    private suspend fun get() {
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
                    withContext(Dispatchers.IO) { from(it.album.images[0].url) }.also {
                        setAlbumBitmap(it!!)
                    })
            }

            loadAlbum(track.value?.track)
        }

        Log.e(TAG, "set for track - ${track.value?.track?.name}")

        if (nextTrack.value == null) {
            Log.e(TAG, "-----next is null-----")

            nextTrack.value = t!!.tracks.first().let {
                TrackWrapper(it,
                    withContext(Dispatchers.IO) { from(it.album.images[0].url) })
            }
        }

        Log.e(TAG, "set for next - ${nextTrack.value?.track?.name}")

        tracks.addAll(t!!.tracks
            .map {
                TrackWrapper(
                    it,
                    withContext(Dispatchers.IO) { from(it.album.images[0].url) }
                )
            }.also {
            })
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
    }

    fun loadAlbum() = loadAlbum(track.value?.track)

    @Deprecated("Use batch load with loadAlbums() and apply with loadAlbum(Track) or loadAlbum()")
    fun loadSingleAlbum() = viewModelScope.launch {
        if (album.value?.album?.id == track.value?.track?.album?.id && album.value?.bitmap != null) return@launch

        album.value = withContext(Dispatchers.IO) {
            AlbumWrapper().apply {
                album = spotifyService.getAlbum(track.value!!.track.album.id)
                bitmap = from(album!!.images[0].url)

                Log.e(TAG, "---------------album type - ${album?.album_type}-----------------")
            }
        }
    }

    private fun loadAlbum(track: Track?) = viewModelScope.launch {
        album.value = with(albums.find { it.album?.id == track?.album?.id }) {
            if (this != null) this
            else {
                loadAlbums(tracks.map { it!!.track }.toTypedArray())
                albums.first()
            }
        }
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

    fun d() = viewModelScope.launch {
        LocalRealm.d(localRealm)
    }

    @OptIn(ExperimentalContracts::class)
    private fun <R, T> from(receiver: R, block: R.() -> T): T {
//        contract {
//            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
//        }

        return receiver.block()
    }

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
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
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