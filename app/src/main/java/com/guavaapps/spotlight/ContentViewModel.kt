package com.guavaapps.spotlight

import android.os.Looper
import android.util.Log
import androidx.core.os.HandlerCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.guavaapps.components.bitmap.BitmapTools.from
import com.guavaapps.spotlight.realm.RealmAlbumWrapper
import com.guavaapps.spotlight.realm.TrackModel
import com.pixel.spotifyapi.Objects.*
import com.pixel.spotifyapi.SpotifyService
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.protocol.types.PlayerState
import com.spotify.protocol.types.Repeat
import io.realm.RealmList
import io.realm.mongodb.Credentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bson.Document
import retrofit.Callback
import retrofit.RetrofitError
import retrofit.client.Response
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
    private val executor = Executors.newSingleThreadExecutor()
    private val uiHandler = HandlerCompat.createAsync(Looper.getMainLooper())

    private var isWaiting = false
    private var needsTrack = true
    private var needsNextTrack = true

    private val tracks: Queue<TrackWrapper?> = LinkedList()
    private val localTimeline = mutableListOf<TrackModel>()
    private val tracksBatch = mutableListOf<Track>()
    private val batch = mutableListOf<TrackModel>()
    private val currentSession = 0

    val spotifyService = MutableLiveData<SpotifyService>()
    val spotifyAppRemote = MutableLiveData<SpotifyAppRemote>()

    val user = MutableLiveData<UserWrapper>()

    val track = MutableLiveData<TrackWrapper?>()
    val progress = MutableLiveData<Long>()
    val nextTrack = MutableLiveData<TrackWrapper?>()

    val album: MutableLiveData<AlbumWrapper?> = MutableLiveData()

    private var mPlayerStateListener: ScheduledExecutorService? = null

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

    fun initForUser(spotifyService: SpotifyService, user: UserPrivate? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = user ?: spotifyService.getCurrentUser()

            login(spotifyService, user)

            updateUi(user)
        }
    }

    private suspend fun login(spotifyService: SpotifyService, user: UserPrivate) {
        withContext(Dispatchers.IO) {
            val userId = user.id

            val credentials = Credentials.customFunction(
                Document(
                    mapOf(
                        "spotify_id" to userId
                    )
                )
            )

            modelRepository.close()
            matcha.logout()

            matcha.login(credentials)
            modelRepository.init()
        }
    }

    private suspend fun updateUi(user: UserPrivate) {
        val wrappedUser = UserWrapper()
        wrappedUser.user = user

        withContext(Dispatchers.IO) {
            wrappedUser.thumbnail = from(user.images[0].url)
        }

        withContext(Dispatchers.Main) {
            this@ContentViewModel.user.value = wrappedUser
        }
    }

    private suspend fun pullBatch(): Array<TrackModel> {
        // pull local

        return emptyArray()
    }

    fun logOutUser() {
//        val user = userRepository.get()
//        user.timeline.addAll(localTimeline)

//        userRepository.update(user)
    }

    private fun getNextTrackFeatures(): FloatArray {
        val model = modelRepository.model

        val timeline = localTimeline.map { it.features.toFloatArray() }.toTypedArray()

        scaleMinMax(timeline).also {
            return invertMinMax(
                it.apply {
                    result = arrayOf(model.getNext(it.result))
                }
            ).first()
        }
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

//        return map.toMap()
        return mutableMapOf<String, Any>(
//            "seed_genres" to "hip-hop"
        ).toMap()
    }

    fun getNext() {
        // The next batch has already been requested from the Spotify Api, listen for changes to ContentViewModel.track
        if (isWaiting) return

        val next = tracks.poll()
        if (next != null) {
            track.value = next
            val needsAlbum =
                album == null || album.value!!.album.id != track.value!!.track.album.id

            val album = localRealm.get(
                RealmAlbumWrapper::class.java,
                track.value!!.track.album.id
            )

            if (needsAlbum && album != null) {
                this.album.value = album as AlbumWrapper
            }
            if (tracks.peek() != null) {
                nextTrack.value = tracks.peek()
            } else {
                nextTrack.value = null

                isWaiting = true

                spotifyService.value!!.getSeedsGenres(object : Callback<SeedsGenres> {
                    override fun success(t: SeedsGenres?, response: Response?) {
                        Log.e(TAG, "genres - ${t!!.genres.joinToString(", ")}")
                    }

                    override fun failure(error: RetrofitError?) {
                        Log.e(TAG, "error - ${error!!.message}")
                    }

                })

                spotifyService.value!!.getRecommendations(
                    createParamsObject(
                        floatArrayOf()//getNextTrackFeatures()
                    ), object : Callback<Recommendations> {
                        override fun success(t: Recommendations?, response: Response?) {
                            t!!.tracks.removeFirst().also {
                                track.value = TrackWrapper(
                                    it,
                                    from(it.album.images[0].url)
                                )
                            }

                            t!!.tracks.removeFirst().also {
                                nextTrack.value = TrackWrapper(
                                    it,
                                    from(it.album.images[0].url)
                                )
                            }

                            tracks.addAll(t!!.tracks
                                .slice(2..tracks.indices.last - 1)
                                .map {
                                    TrackWrapper(
                                        it,
                                        from(it.album.images[0].url)
                                    )
                                })

                            isWaiting = false
                        }

                        override fun failure(error: RetrofitError?) {
                            // unlock the spotify api
                            isWaiting = false

                            if (error!!.response.status == 401) {
                                // reauth the user
                            }


                            // retry once TODO retry many?
                            if (!hasRetried) {
                                hasRetried = true
                                getNext()
                            } else {
                                // were fucked
                            }
                        }
                    })

            }
        }
    }

    private fun createModel() {
        modelRepository.createModel()
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
                val session = this.timeline.last()!!

                if (session.session_id != currentSession) return@with

                session.tracks.addAll(batch)
            }
        }

        spotifyService.value!!.addTracksToPlaylist(
            user.value!!.user.id, "playlist_id",
            mapOf("uris" to batch.map { it.uri }.joinToString(",")), null
        )
    }

    private fun isBreakaway(): Boolean {
        return false
    }

    private suspend fun MutableList<TrackModel>.injectFeatures() {
        val ids = this.map { it.id }.joinToString(",")

        return withContext(Dispatchers.IO) {
            spotifyService.value!!.getTracksAudioFeatures(ids)
                .audio_features
                .forEach {
                    this@injectFeatures.find { track -> track.id == it.id }!!
                        .features = RealmList(*it.extractFeatures().toTypedArray())
                }
        }
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

    private fun scaleMinMax(data: Array<FloatArray>): Scaler {
        var min = Float.MIN_VALUE
        var max = Float.MAX_VALUE

        data.forEach { x ->
            val mn = x.min()
            val mx = x.max()

            if (mn < min) min = mn
            if (mx < max) max = mx
        }

        var scaled = Array(data.size) { i ->
            val d = max - min

            FloatArray(data[i].size) { j ->
                (data[i][j] - min) / d
            }
        }


        return Scaler(
            scaled,
            min,
            max
        )
    }

    private fun invertMinMax(data: Scaler): Array<FloatArray> {
        val delta = data.max - data.min

        var inverted = Array(data.result.size) { i ->

            FloatArray(data.result[i].size) { j ->
                data.result[i][j] * delta + data.min
            }
        }

        return inverted
    }

    private data class Scaler(
        var result: Array<FloatArray>,
        var min: Float,
        var max: Float,
    )

    private var hasRetried = false

    fun loadAlbum() {
        spotifyService.value!!.getAlbum(track.value!!.track.album.id,
            object : Callback<Album> {
                override fun success(t: Album?, response: Response?) {
                    album.value = AlbumWrapper(
                        t,
                        from(t!!.images[0].url)
                    )
                }

                override fun failure(error: RetrofitError?) {

                }

            })
    }

    fun play() {
        val uri = track.value!!.track.uri

        play(uri)
    }

    fun play(uri: String) {
        val playerApi = spotifyAppRemote.value!!.playerApi

        startPlayerStateListener()

        playerApi.playerState.setResultCallback {
            if (it.track.uri == uri) playerApi.resume() else playerApi.play(uri)
            if (it.playbackOptions.repeatMode != Repeat.ONE) playerApi.setRepeat(Repeat.ONE)
        }

        playerApi.play(uri)
    }

    fun pause() {
        val playerApi = spotifyAppRemote.value!!.playerApi
        stopPlayerStateListener()
        playerApi.pause()
    }

    private fun startPlayerStateListener() {
        if (mPlayerStateListener == null) mPlayerStateListener =
            Executors.newSingleThreadScheduledExecutor()
        mPlayerStateListener!!.scheduleAtFixedRate({
            spotifyAppRemote.value!!
                .getPlayerApi()
                .playerState
                .setResultCallback { data: PlayerState -> progress.setValue(data.playbackPosition) }
        }, 0, 500, TimeUnit.MILLISECONDS)
    }

    private fun stopPlayerStateListener() {
        if (mPlayerStateListener == null) return
        mPlayerStateListener!!.shutdown()
    }

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