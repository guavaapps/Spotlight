package com.guavaapps.spotlight

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentContainerView
import androidx.navigation.fragment.NavHostFragment
import com.google.gson.Gson
import com.guavaapps.spotlight.realm.RealmTrack
import com.pixel.spotifyapi.Objects.*
import com.pixel.spotifyapi.SpotifyApi
import com.pixel.spotifyapi.SpotifyService
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import io.realm.Realm
import io.realm.mongodb.User
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private var spotifyService: SpotifyService? = null
    private val spotifyAppRemote: SpotifyAppRemote? = null

    private val viewModel: ContentViewModel by viewModels { ContentViewModel.Factory }
    private var lock: Any? = Any()
    private lateinit var fragmentContainerView: FragmentContainerView
    private lateinit var contentFragment: ContentFragment

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContentView(R.layout.activity_main)

        fragmentContainerView = findViewById(R.id.fragment_container_view)
        contentFragment = ContentFragment()

        val content = findViewById<View>(android.R.id.content)

        val fragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container_view) as NavHostFragment?

        val navController = fragment!!.navController

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
        if (isSpotifyInstalled) {
            auth()
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)

        if (requestCode == REQUEST_AUTH) {
            val response = AuthorizationClient.getResponse(resultCode, intent)

            Log.e(TAG, response.type.toString())

            when (response.type) {
                AuthorizationResponse.Type.TOKEN -> {
                    val accessToken = response.accessToken

                    Log.e(TAG, "accessToken - $accessToken")

                    val spotifyApi = SpotifyApi()
                    spotifyApi.setAccessToken(accessToken)

                    spotifyService = spotifyApi.service

                    viewModel.spotifyService.value = spotifyService!!

                    viewModel.initForUser(
                        this,
                        spotifyService!!
                    )

//                    mViewModel.getRecc ();
//                    AppRepo.getInstance()
//                        .getCurrentUser(
//                            spotifyService,
//                            this@MainActivity,
//                            object : ResultListener() {
//                                override fun onUser(userWrapper: UserWrapper) {
//                                    Handler.createAsync(Looper.getMainLooper())
//                                        .post {
//                                            val jsonArray = "[\"0\", \"1\"]"
//                                            val listType =
//                                                object : TypeToken<List<String?>?>() {}.type
//                                            val list =
//                                                Gson().fromJson<List<String>>(jsonArray, listType)
//                                            for (s in list) {
//                                                Log.e(TAG, "list: $s")
//                                            }
//                                        }
//                                }
//                            })
//                    viewModel!!.getNext()
                    lock = null
                }
                AuthorizationResponse.Type.ERROR -> {
                    val e = response.error
                    Log.e(TAG, e)
                }
                else -> {}
            }
        }
    }

    class TrackObject {
        val artists: List<String>? = null
        val available_markets: List<String>? = null
        val is_playable: Boolean? = null
        val linked_from: String? = null
        val disc_number = 0
        val duration_ms: Long = 0
        val explicit: Boolean? = null
        val external_urls: Map<String, String>? = null
        val href: String? = null
        val id: String? = null
        val name: String? = null
        val preview_url: String? = null
        val track_number = 0
        val type: String? = null
        val uri: String? = null

        var album: AlbumSimple? = null
        var external_ids: Map<String, String>? = null
        var popularity: Int? = null
    }

    fun auth() {
        val builder = AuthorizationRequest.Builder(
            CLIENT_ID,
            AuthorizationResponse.Type.TOKEN,
            "http://localhost/"
        )

        builder.setScopes(
            arrayOf(
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
        )

        val request = builder.build()

        AuthorizationClient.openLoginActivity(this, REQUEST_AUTH, request)

        val connectionParams = ConnectionParams.Builder(CLIENT_ID)
            .setRedirectUri("http://localhost/")
            .showAuthView(true)
            .build()

        Handler.createAsync(Looper.getMainLooper()).postDelayed(
            {
                SpotifyAppRemote.connect(
                    this@MainActivity,
                    connectionParams,
                    object : Connector.ConnectionListener {
                        override fun onConnected(spotifyAppRemote: SpotifyAppRemote) {
                            Log.d(TAG, "SpotifyAppRemote connected")
                            viewModel!!.spotifyAppRemote.value = spotifyAppRemote
                        }

                        override fun onFailure(error: Throwable) {
                            Log.d(
                                TAG,
                                "SpotifyAppRemote failed to connect - " + error.message + " cause=" + error.cause
                            )
                        }
                    })
            },
            5000
        )
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

    companion object {
        private const val TAG = "MainActivity"
        private const val DEBUG = "MainActivity:DEBUG"
        private const val CLIENT_ID =
            "431b4c7e106c4470a0b145cdfe7962bd" //"86ef10fd39344e10a060ade09f7c7a78";
        private const val SPOTIFY_PREMIUM = "premium"
        private const val SPOTIFY = "com.spotify.music"
        private const val REQUEST_AUTH = 0
    }
}