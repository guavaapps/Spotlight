package com.guavaapps.spotlight

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.guavaapps.components.bitmap.BitmapTools.from
import com.guavaapps.spotlight.realm.AppUser
import com.guavaapps.spotlight.realm.TrackModel
import com.pixel.spotifyapi.Objects.*
import com.pixel.spotifyapi.SpotifyService
import com.spotify.android.appremote.api.AppRemote
import com.spotify.android.appremote.api.SpotifyAppRemote
import io.realm.Realm
import io.realm.RealmList
import io.realm.mongodb.Credentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bson.Document
import retrofit.RetrofitError
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.collections.set

private const val TAG = "ViewModel"

// spotify track features
private val FEATURES = listOf(
    "acousticness",
    "danceability",
    "energy",
    "instrumentalness",
    "liveness",
    "loudness",
    "speechiness",
    "tempo",
    "valence",
)

// the spotify web api supports up to 100 tracks being added
// to a playlist
private const val MAX_BATCH_SIZE = 100

class ContentViewModel(
    private var matcha: Matcha,
    private var modelRepository: ModelRepository,
    private var realm: Realm,
) : ViewModel() {
    private var isWaiting = false

    private lateinit var appUser: AppUser

    // spotify
    private lateinit var spotifyService: SpotifyService
    private lateinit var spotifyAppRemote: AppRemote
    private var playerStateListener: ScheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor()

    // internal
    private val tracks: Queue<TrackWrapper?> = LinkedList()
    private var albumInternal: AlbumWrapper? = null
    private var allArtists = mutableListOf<ArtistWrapper>()
    private var userPlaylists = mutableListOf<PlaylistSimple>()

    // local copy of the user's listening history
    private val localTimeline = mutableListOf<TrackModel>()

    private val rejected = mutableListOf<String>()
    private val batch = mutableListOf<TrackModel>()

    // observables
    val user = MutableLiveData<UserWrapper>()
    val track = MutableLiveData<TrackWrapper?>()
    val nextTrack = MutableLiveData<TrackWrapper?>()
    val album = MutableLiveData<AlbumWrapper?>()
    val artists = MutableLiveData<List<ArtistWrapper>>()
    val artistTracks = MutableLiveData<MutableMap<String, List<TrackWrapper>>>()
    val playlists = MutableLiveData<MutableList<PlaylistWrapper>>()
    val playlistTracks = MutableLiveData<MutableMap<String, List<PlaylistTrackWrapper>>>()
    val playlist = MutableLiveData<PlaylistWrapper?>()
    val progress = MutableLiveData<Long>()

    fun initForUser(
        spotifyService: SpotifyService,
        spotifyAppRemote: SpotifyAppRemote,
        user: UserPrivate,
    ) {
        this.spotifyService = spotifyService
        this.spotifyAppRemote = spotifyAppRemote

        viewModelScope.launch {
            // get user data
            login(user)

            // update user ui
            updateUser(user)

            //local timeline available after updateUser()
            modelRepository.init(localTimeline.map {
                it.features.toFloatArray()
            }.filterNot { it.isEmpty() }
                .toTypedArray())

            // load user playlists and update ui
            loadPlaylists()

            // get next track
            getNext()
        }
    }

    private suspend fun login(user: UserPrivate) = withContext(Dispatchers.IO) {
        val userId = user.id

        val credentials = Credentials.customFunction(
            Document(
                mapOf(
                    "spotify_id" to userId,
                )
            )
        )

        // authenticate with mongo db using the user's spotify id
        matcha.login(credentials)
    }

    private suspend fun updateUser(user: UserPrivate) {
        val wrappedUser = UserWrapper(user)

        // TODO UserPrivate.wrap()
        withContext(Dispatchers.IO) {
            wrappedUser.bitmap = from(user.images[0].url)
        }

        // update ui
        withContext(Dispatchers.Main) {
            this@ContentViewModel.user.value = wrappedUser
        }

        withContext(Dispatchers.IO) {
            matcha.where(AppUser::class.java)
                .equalTo("_id", user.id).let {
                    val user = it.findFirst()

                    // update last login date in the user's document
                    user?.last_login = Date(System.currentTimeMillis())

                    // copy the user's listening history
                    localTimeline.addAll(
                        user?.timeline?.let {
                            it.ifEmpty { getRecentlyPlayedTracks() }
                        }!!
                    )

                    // update the local and remote user documents
                    user.let { u ->
                        it.update(u)
                        realm.executeTransaction {
                            it.insertOrUpdate(u)
                        }
                    }

                    this@ContentViewModel.appUser = user
                }
        }
    }

    // dlstmp model
    private suspend fun getRecentlyPlayedTracks() = withContext(Dispatchers.IO) {
        val recentlyPlayedTracks = spotifyService.getRecentlyPlayedTracks(mapOf("limit" to 50))

        val tracks = recentlyPlayedTracks.items.map {
            TrackModel(
                it.track.id,
                it.track.uri,
                timestamp = 0L
            )
        }.toMutableList()

        tracks.injectFeatures()

        return@withContext tracks
    }

    // get artist, track and genre seeds based on the user's listening history
    // more recent seeds have a higher weight
    // since the spotify api supports a maximum of 5 seeds per recommendation request
    // 3 short term (most recent), 2 medium term and 1 long term seed is used
    private suspend fun getSeeds() = withContext(Dispatchers.IO) {
        val paramsShort = mapOf(
            // api limit <= 50
            "limit" to 50,
            // get a broad picture of what the user listens to
            "time_range" to "short_term"
        )

        val paramsMedium = mapOf(
            "limit" to 50,
            "time_range" to "medium_term"
        )

        val paramsLong = mapOf(
            "limit" to 50,
            "time_range" to "long_term"
        )

        val topGenres = mutableListOf<String>()

        val topArtists = listOf(
            *spotifyService.getTopArtists(paramsShort).items.let {
                topGenres.addAll(it.flatMap { it.genres.distinct() }.distinct().take(3))
                it.take(3).toTypedArray()
            },
            *spotifyService.getTopArtists(paramsMedium).items.let {
                topGenres.addAll(it.flatMap { it.genres.distinct() }.distinct().take(2))
                it.take(2).toTypedArray()
            },
            *spotifyService.getTopArtists(paramsLong).items.let {
                topGenres.addAll(it.flatMap { it.genres.distinct() }.distinct().take(1))
                it.take(1).toTypedArray()
            },
        )

        val topTracks = listOf(
            *spotifyService.getTopTracks(paramsShort).items.take(3).toTypedArray(),
            *spotifyService.getTopTracks(paramsMedium).items.take(2).toTypedArray(),
            *spotifyService.getTopTracks(paramsLong).items.take(1).toTypedArray(),
        )

        return@withContext Triple(
            topArtists.joinToString(",") { it.id },
            topTracks.joinToString(",") { it.id },
            topGenres.joinToString(",") { it.replace(" ", "-") }
        )
    }

    private fun getNextTrackFeatures(): FloatArray {
        val model = modelRepository.model

        val timeline = localTimeline.map { it.features.toFloatArray() }.toTypedArray()
            .filterNot { it.isEmpty() } // exclude empty results
            .toTypedArray()

        return model.getNext(timeline)
    }

    private suspend fun createParamsObject(features: FloatArray): List<Map<String, Any>> {
        val iterator = features.iterator()

        // the recommendations provided will be based on these seeds
        val (artists, tracks, genres) = getSeeds()

        // the indices of the feature names and the feature values
        // always match and the order doesn't change
        // map values
        val features = FEATURES.map {
            "target_$it" to iterator.next()
        }.toMap()

        // create final objects
        return listOf(
            mapOf(
                "limit" to 100,
                "seed_tracks" to tracks,
                *features.toList().toTypedArray()
            ),
            mapOf(
                "limit" to 100,
                "seed_artists" to artists,
                *features.toList().toTypedArray()
            ),
            mapOf(
                "limit" to 100,
                "seed_genres" to genres,
                *features.toList().toTypedArray()
            )
        )
    }

    //i love you so fucking much and  when i looked at you just now i thought my heart was gonna explode

    // ui
    fun getAlbum() = viewModelScope.launch {
        if (album.value?.album?.tracks != null) return@launch

        loadAlbum(track.value?.track!!)
        loadArtists(albumInternal?.album!!)

        applyAlbum()
    }

    // tracks
    fun add() = viewModelScope.launch {
        val track = this@ContentViewModel.track.value!!.track

        // reset player
        seekTo(0)
        getNext()
        play()

        // queue track
        batch.add(TrackModel(
            track.id,
            track.uri,
            System.currentTimeMillis()
        ).also { localTimeline.add(it) })

        if (batch.size >= MAX_BATCH_SIZE) {
            // dispatch the added tracks queue
            pushBatch()

            // optimise the model based on the tracks listened in this session
            modelRepository.optimiseModel(localTimeline.map {
                it.features.toFloatArray()
            }.toTypedArray())
        }
    }

    fun reject() = viewModelScope.launch {
        rejected.add(track.value!!.track.id)

        seekTo(0)
        getNext()
        play()
    }

    private suspend fun pushBatch() {
        if (playlist.value == null) return

        batch.injectFeatures()

        withContext(Dispatchers.IO) {
            val appUser = matcha.where(AppUser::class.java)
                .equalTo("_id", user.value?.user?.id!!)
                .findFirst()

            with(appUser) {
                // update the user's remote and local listening history
                this?.timeline?.addAll(batch)
                localTimeline.addAll(batch)
            }

            matcha.where(AppUser::class.java)
                .equalTo("_id", user.value?.user?.id!!)
                .update(appUser!!)

            try {
                // POST to spotify playlist
                playlist.value!!.playlist?.snapshot_id = spotifyService.addTracksToPlaylist(
                    user.value!!.user.id, playlist.value!!.playlist?.id,
                    mapOf("uris" to batch.map { it.uri }.joinToString(",")), emptyMap()
                ).snapshot_id
            } catch (e: RetrofitError) {
                // notify the ui that the selected playlist failed
                withContext(Dispatchers.Main) { playlist.value = null }
            }
        }

        batch.clear()
    }

    private suspend fun createPlaceholderAlbum(wrappedTrack: TrackWrapper) = Album().apply {
        id = wrappedTrack.track.id
        name = wrappedTrack.track.name
    }.wrap(wrappedTrack.thumbnail)

    private suspend fun getNext() {
        // the next batch has already been requested from the Spotify Api
        // listen for changes to ContentViewModel.track
        if (isWaiting) return

        // get next track in the queue
        val next = tracks.poll()

        if (next?.track?.album?.id != album.value?.album?.id) {
            album.value?.album = null
        }

        track.value = next

        // if the new track is not null create a placeholder
        // album using the album name and bitmap provided by the track object
        // and notify the ui
        // this prevents a blank ui while the full album info (including tracks)
        // is being loaded
        if (next != null) {
            album.value = createPlaceholderAlbum(next)
        }

        if (tracks.peek() == null) {
            nextTrack.value = null

            // load next batch of tracks
            viewModelScope.launch(Dispatchers.Main) {
                loadNextTracks()
            }
        } else {
            nextTrack.value = tracks.peek()
        }
    }

    private suspend fun loadNextTracks() {
        isWaiting = true

        // get track features and create the corresponding requests
        val nextTrackFeatures = getNextTrackFeatures()
        val requestObject = createParamsObject(nextTrackFeatures)

        // perform the requests
        val t = withContext(Dispatchers.IO) {
            requestObject.flatMap {
                spotifyService.getRecommendations(it).tracks
            }
        }.distinctBy { it.id }
            .toMutableList()

        // update the ui
        if (track.value == null) {
            track.value = t.removeFirst().let { it.wrap() }

            album.value = createPlaceholderAlbum(track.value!!)
        }

        if (nextTrack.value == null) {
            nextTrack.value = t.first().wrap()
        }

        // queue the tracks
        t.forEach {
            tracks.add(it.wrap())

            isWaiting = false
        }
    }

    // album
    // these functions are related to the ExtraFragment ui

    // update the ui with the new album and artists
    private fun applyAlbum() = viewModelScope.launch {
        artists.value = allArtists
        album.value = albumInternal
    }

    // load album
    private suspend fun loadAlbum(track: Track) {
        val a = withContext(Dispatchers.IO) { spotifyService.getAlbum(track.album.id).wrap() }
        this@ContentViewModel.albumInternal = a
    }

    // artists
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

    private suspend fun loadArtists(album: Album) {
        // get all distinct artist ids from this album
        val artists = album.tracks.items.flatMap { it.artists }
            .distinctBy { it.id }

        val ids = artists.joinToString(",") { it.id }

        // GET from spotify
        allArtists = withContext(Dispatchers.IO) {
            spotifyService.getArtists(ids).artists.map { it.wrap() }.toMutableList()
        }
    }

    private suspend fun loadArtistTracks(artist: ArtistSimple): List<TrackWrapper> {
        // GET artist tracks from spotify
        val tracks = withContext(Dispatchers.IO) {
            spotifyService.getArtistTopTrack(artist.id,
                user.value?.user?.country ?: "")
                .tracks.map { it.wrap() }
        }

        return tracks
    }

    // playlists
    private suspend fun loadPlaylists() {
        loadUserPlaylists()

        playlists.value = userPlaylists
            // filter user's playlists
            .filter { it.owner.id == user.value?.user?.id }
            .map {
                Playlist().apply {
                    id = it.id
                    name = it.name
                    images = it.images
                }.wrap()
            }.toMutableList()

        prepareMainPlaylist()
    }

    private suspend fun getMainPlaylistId() = withContext(Dispatchers.IO) {
        // get the id of the playlist the user has selected to
        // add tracks to
        matcha.where(AppUser::class.java)
            .equalTo("_id", user.value?.user?.id!!)
            .findFirst()
            ?.playlist
    }

    fun setMainPlaylist(id: String?) = viewModelScope.launch {
        playlist.value = playlists.value?.find { it.playlist?.id == id }

        withContext(Dispatchers.IO) {
            val u = matcha.where(AppUser::class.java)
                .equalTo("_id", user.value?.user?.id!!)
                .findFirst()

            u?.playlist = id

            u?.let {
                matcha.where(AppUser::class.java)
                    .equalTo("_id", user.value?.user?.id!!)
                    .upsert(it)
            }

//            realm.executeTransaction { realm ->
//                u?.let { realm.insertOrUpdate(it) }
//            }
        }
    }

    private suspend fun prepareMainPlaylist() {
        // get the playlist object from the user's playlists
        // or the first playlist available if the user hasn't selected any
        val id = getMainPlaylistId() ?: userPlaylists.firstOrNull {
            it.owner.id == user.value?.user?.id
        }?.id

        playlist.value = userPlaylists.find { it.id == id }.let {
            Playlist().apply {
                this.id = it?.id
                this.name = it?.name
                this.uri = it?.uri
            }.wrap()
        }
    }

    private suspend fun loadUserPlaylists() = withContext(Dispatchers.IO) {
        userPlaylists = spotifyService.getCurrentUserPlaylists().items
    }

    //player
    fun play() {
        val uri = track.value!!.track.uri

        play(uri)
    }

    fun play(uri: String) {
        val playerApi = spotifyAppRemote.playerApi

        startPlayerStateListener()

        playerApi.play(uri)
    }

    fun resume() {
        val playerApi = spotifyAppRemote.playerApi

        startPlayerStateListener()

        playerApi.resume()
    }

    fun pause() {
        val playerApi = spotifyAppRemote.playerApi
        stopPlayerStateListener()
        playerApi.pause()
    }

    fun seekTo(position: Long) {
        val playerApi = spotifyAppRemote.playerApi
        progress.setValue(position)
        playerApi.seekTo(position)
    }

    var shouldPausePlayerStateListener = false

    fun startPlayerStateListener() {
        playerStateListener = Executors.newSingleThreadScheduledExecutor()

        playerStateListener.scheduleAtFixedRate({
            val playerApi = spotifyAppRemote.playerApi

            playerApi.playerState
                .setResultCallback {
                    if (it.playbackPosition == it.track.duration) {
                        playerApi.pause()
                        playerApi.seekTo(0)
                    }
                    if (!shouldPausePlayerStateListener) progress.value = it.playbackPosition
                }
        }, 0, 500, TimeUnit.MILLISECONDS)
    }

    fun stopPlayerStateListener() {
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

    // get the features of a track as a float array indexed based on FEATURES using reflection
    private fun AudioFeaturesTrack.extractFeatures(): FloatArray {
        val features = Array(FEATURES.size) { i ->
            val f = FEATURES.filterIndexed { it, _ -> it == i }.first()

            val value = AudioFeaturesTrack::class.java
                .getField(f)
                .get(this) as Number

            value.toFloat()
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
                    app.modelRepository,
                    app.realm
                ) as T
            }
        }
    }
}