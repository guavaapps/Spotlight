package com.guavaapps.spotlight

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.*
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.ThemeUtils
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentContainerView
import androidx.navigation.fragment.NavHostFragment
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.guavaapps.spotlight.Mapper.map
import com.pixel.spotifyapi.Objects.*
import com.pixel.spotifyapi.SpotifyApi
import com.pixel.spotifyapi.SpotifyService
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import retrofit.Callback
import retrofit.RetrofitError
import retrofit.client.Response
import retrofit.converter.GsonConverter
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

private const val TAG = "MainActivity"
private const val CLIENT_ID = "431b4c7e106c4470a0b145cdfe7962bd"
private const val SPOTIFY_PREMIUM = "premium"
private const val SPOTIFY = "com.spotify.music"
private const val REQUEST_AUTH = 0
private val SCOPES = arrayOf(
    "streaming",
    "user-follow-read",
    "playlist-read-private",
    "user-read-private",
    "user-library-modify",
    "playlist-modify-private",
    "playlist-modify-public",
    "user-read-playback-position",
    "user-top-read",
    "user-read-recently-played"
)

class MainActivity : AppCompatActivity() {
    private lateinit var app: Ky

    private val viewModel: ContentViewModel by viewModels { ContentViewModel.Factory }

    private var lock: Any? = Any()

    private lateinit var fragmentContainerView: FragmentContainerView
    private lateinit var contentFragment: ContentFragment

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.i(TAG,
            "i love you sooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo much ")

        app = application as Ky

        val cacheFolder = filesDir
        val tmpFolder = File(cacheFolder, "tmp")
        val exists = tmpFolder.exists()

        Log.e(TAG, "exists=$exists")
        if (exists) {
            val tmpFiles = tmpFolder.listFiles()
            tmpFiles.forEach { Log.e(TAG, "tmp=${it.name}") }
        }

        Ducky.quack(this)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContentView(R.layout.activity_main)

        fragmentContainerView = findViewById(R.id.fragment_container_view)
        contentFragment = ContentFragment()

        val content = findViewById<View>(android.R.id.content)

        val fragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container_view) as NavHostFragment

        val navController = fragment.navController

        content.viewTreeObserver
            .addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    if (lock == null) {
                        content.viewTreeObserver
                            .removeOnPreDrawListener(this)

//                            getSupportFragmentManager ().beginTransaction ()
//                                    .add (mFragmentContainerView.getId (), mContentFragment)
//                                    .commit ();
                    }
                    return false
                }
            })

        if (isSpotifyInstalled) {// && app.matcha.currentUser == null) {
            auth()
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    fun getRecent(token: String) {
        Executors.newSingleThreadScheduledExecutor()
            .execute {
                val conn = URL("https://api.spotify.com/v1/me/player/recently-played/?limit=1")
                    .openConnection() as HttpURLConnection

                conn.requestMethod = "GET"
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.setRequestProperty("Content-type", "application/json")

                val response = conn.inputStream.readBytes()
                val json = String(response)

                val track = json.substringAfter("\"track\" : {")
                    .substringBeforeLast("} ],")

                /**
                 * album
                 * artists
                 * disc_number
                 * duration_ms
                 * explicit
                 * external_ids
                 *
                 */

                try {
                    val type = object : TypeToken<Pager<RecentlyPlayedTrack>>() {}.type

                    val jsonObject = GsonBuilder()
                        .setPrettyPrinting()
                        .serializeNulls()
                        .create()
                        .fromJson<Pager<RecentlyPlayedTrack>>(json, type)

                    Log.e(TAG, "recent - $track")

                    Log.e(TAG,
                        "jsonObject - [${jsonObject.items.size}] ${jsonObject.items.firstOrNull()?.track?.name} ${jsonObject.items.first().played_at}")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
    }

    fun getTrack(token: String) {
        Executors.newSingleThreadScheduledExecutor()
            .execute {

                // 3 bless 74Te5k4g7mVRm9hjjA5U8O
                val track = "74Te5k4g7mVRm9hjjA5U8O"
                val conn = URL("https://api.spotify.com/v1/tracks/$track")
                    .openConnection() as HttpURLConnection

                conn.requestMethod = "GET"
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.setRequestProperty("Content-type", "application/json")

                val response = conn.inputStream.readBytes()
                val json = String(response)

                val jsonTrack = GsonBuilder()
                    .create()
                    .fromJson(json, Track::class.java)

                Log.e(TAG, "track - ${jsonTrack.name}")
            }
    }

    fun getRecentSpotify(spotifyService: SpotifyService) {
        Executors.newSingleThreadScheduledExecutor()
            .execute {
                try {
                    val tracks =
                        spotifyService.getRecentlyPlayedTracks(mapOf(SpotifyService.LIMIT to 1)).items.forEach {
                            Log.e(TAG, "[track] ${it.track.name} {${it.played_at}}")
                        }

                    Log.e(TAG, "tracks - ${tracks}")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)

        if (requestCode == REQUEST_AUTH) {
            val response = AuthorizationClient.getResponse(resultCode, intent)

            Log.e(TAG, response.type.toString())

            when (response.type) {
                AuthorizationResponse.Type.TOKEN -> {
                    val accessToken = response.accessToken

                    val spotifyApi = SpotifyApi()
                    spotifyApi.setAccessToken(accessToken)
                    val spotifyService = spotifyApi.service

                    spotifyService.getCurrentUser(object : Callback<UserPrivate> {
                        override fun success(t: UserPrivate?, response: Response?) {
                            if (t!!.product == SPOTIFY_PREMIUM) {
                                getAppRemote {
                                    viewModel.initForUser(spotifyService, it!!, t)

                                    lock = null
                                }
                            }
                        }

                        override fun failure(error: RetrofitError?) {
                            // not authorised
                            Log.d(TAG, "error verifying user")
                            if (error?.response?.status == 401) {
                                Log.e(TAG, "not authorized")
                            }
                        }

                    })
                }
                AuthorizationResponse.Type.ERROR -> {
                    val e = response.error
                    Log.e(TAG, e)
                }
                else -> {}
            }
        }
    }

    fun auth() {
        val builder = AuthorizationRequest.Builder(
            CLIENT_ID,
            AuthorizationResponse.Type.TOKEN,
            "http://localhost/"
        )

        builder.setScopes(SCOPES)

        val request = builder.build()

        AuthorizationClient.openLoginActivity(this, REQUEST_AUTH, request)
    }

    private fun getAppRemote(onResult: (appRemote: SpotifyAppRemote?) -> Unit) {
        val connectionParams = ConnectionParams.Builder(CLIENT_ID)
            .setRedirectUri("http://localhost/")
            .showAuthView(true)
            .build()

        SpotifyAppRemote.connect(
            this@MainActivity,
            connectionParams,
            object : Connector.ConnectionListener {
                override fun onConnected(spotifyAppRemote: SpotifyAppRemote) {
                    Log.d(TAG, "SpotifyAppRemote connected")
                    onResult(spotifyAppRemote)
                }

                override fun onFailure(error: Throwable) {
                    Log.d(
                        TAG,
                        "SpotifyAppRemote failed to connect - " + error.message + " cause=" + error.cause
                    )

                    onResult(null)
                }
            })
    }

    private val isSpotifyInstalled: Boolean
        private get() {
            try {
                packageManager.getPackageInfo(SPOTIFY, 0)
                return true
            } catch (e: PackageManager.NameNotFoundException) {
            }
            return false
        }

    private fun isPremium(currentUser: UserPrivate): Boolean {
        return currentUser.product == SPOTIFY_PREMIUM
    }
}