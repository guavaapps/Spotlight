package com.guavaapps.spotlight

import android.content.Context
import com.guavaapps.components.bitmap.BitmapTools.from
import com.pixel.spotifyapi.SpotifyService
import com.spotify.android.appremote.api.SpotifyAppRemote
import android.os.Looper
import android.graphics.Bitmap
import com.spotify.protocol.types.PlayerState
import com.spotify.protocol.types.Repeat
import com.google.gson.GsonBuilder
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.os.HandlerCompat
import androidx.lifecycle.*
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.guavaapps.spotlight.realm.RealmAlbumWrapper
import com.guavaapps.spotlight.realm.RealmTrack
import com.guavaapps.spotlight.realm.TrackModel
import com.guavaapps.spotlight.realm.User
import com.pixel.spotifyapi.Objects.*
import io.realm.Realm
import io.realm.RealmList
import retrofit.Callback
import retrofit.RetrofitError
import retrofit.client.Response
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.Exception
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class ContentViewModel : ViewModel() {
    private val executor = Executors.newSingleThreadExecutor()
    private val uiHandler = HandlerCompat.createAsync(Looper.getMainLooper())

    lateinit var userRepository: UserRepository
    lateinit var mongoClient: MongoClient
    lateinit var modelRepository: ModelRepository
    lateinit var localRealm: LocalRealm
    lateinit var realm: Realm

    private var isWaiting = false
    private var needsTrack = true
    private var needsNextTrack = true
    private val tracks: Queue<TrackWrapper?> = LinkedList()
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

    private var localTimeline = mutableListOf<FloatArray>()

    fun initForUser(
        context: Context,
        service: SpotifyService,
    ) {
        spotifyService.value = service

        if (user.value != null) {
            logOutUser()
        }

        spotifyService.value!!.getCurrentUser(object : Callback<UserPrivate> {
            override fun success(t: UserPrivate?, response: Response?) {
                executor.execute {
                    val bitmap = from(t!!.images[0].url)

                    val userId = t.id

                    mongoClient = MongoClient(context)

                    val remoteModelDataSource = RemoteModelDataSource()

                    modelRepository = ModelRepository(
                        userId,
                        mongoClient,
                        remoteModelDataSource
                    )

                    userRepository = UserRepository(
                        userId,
                        mongoClient
                    )

                    localRealm = LocalRealm(context)

                    localTimeline = getTimeline()

                    uiHandler.post {
                        user.value = UserWrapper(
                            t,
                            bitmap
                        )

                        getNext()
                    }
                }
            }

            override fun failure(error: RetrofitError?) {

            }

        })
    }

    fun logOutUser() {
        val user = userRepository.get()
        user.timeline.addAll(
            localTimeline.map {
                TrackModel().apply {
                    features = RealmList(*it.toTypedArray())
                }
            }
        )

        userRepository.update(user)
    }

    fun getUserAsync() {
        spotifyService.value!!.getCurrentUser(object : Callback<UserPrivate> {
            override fun success(t: UserPrivate?, response: Response?) {
                user.value = UserWrapper(
                    t!!,
                    from(t.images[0].url)
                )
            }

            override fun failure(error: RetrofitError?) {
                // retry login
            }

        })
    }

    private fun getNextTrackFeatures(): FloatArray {
        val model = modelRepository.model

        scaleMinMax(localTimeline.toTypedArray()).also {
            return invertMinMax(
                it.apply {
                    result = arrayOf(model.getNext(it.result))
                }
            ).first()
        }
    }

    private fun getTimeline(): MutableList<FloatArray> {
        return userRepository.get()
            .timeline
            .map { it.features.toFloatArray() }
            .toMutableList()
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

        return map.toMap()
    }

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

                spotifyService.value!!.getRecommendations(
                    createParamsObject(
                        getNextTrackFeatures()
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

    fun logSuccess(tracks: Array<RealmTrack>) {
        val ids = tracks.map { it.id }

        spotifyService.value!!.getTracksAudioFeatures(
            ids.joinToString(","),
            object : Callback<AudioFeaturesTracks> {
                override fun success(t: AudioFeaturesTracks?, response: Response?) {
                    localTimeline.addAll(t!!.audio_features.map { it.extractFeatures() })
                }

                override fun failure(error: RetrofitError?) {

                }
            }
        )
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

    private fun AudioFeaturesTrack.extractFeatures(): FloatArray {
        val features = Array(featuresMap.size) { i ->
            val f = featuresMap.filterValues { it == i }.keys.first()
            AudioFeaturesTrack::class.java
                .getField(f)
                .get(this) as Float
        }

        return features.toFloatArray()
    }

    fun play() {
        val playerApi = spotifyAppRemote.value!!.playerApi
        val track = track.value!!.track
        if (playerApi == null || track == null) return
        startPlayerStateListener()
        playerApi.playerState
            .setResultCallback { data: PlayerState ->
                if (data.track.uri == track.uri) playerApi.resume() else playerApi.play(track.uri)
                playerApi.setRepeat(Repeat.ONE)
            }
    }

    fun play(wrappedTrack: TrackWrapper) {
        val playerApi = spotifyAppRemote.value!!.playerApi
        val track = wrappedTrack.track
        if (playerApi == null || track == null) return
        startPlayerStateListener()
        playerApi.play(track.uri)
    }

    fun pause() {
        val playerApi = spotifyAppRemote.value!!.playerApi
        stopPlayerStateListener()
        if (playerApi == null) return
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
        private const val TAG = "ViewModel"

        private fun cacheAlbumObject(context: Context, wrappedAlbum: AlbumWrapper) {
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
            }
        }

        private fun buildAlbumObject(context: Context, id: String): AlbumWrapper? {
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

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[APPLICATION_KEY]

                return ContentViewModel() as T
            }
        }
    }
}