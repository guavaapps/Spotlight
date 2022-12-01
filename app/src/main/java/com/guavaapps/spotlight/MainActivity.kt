package com.guavaapps.spotlight

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentContainerView
import androidx.navigation.fragment.NavHostFragment
import com.pixel.spotifyapi.Objects.UserPrivate
import com.pixel.spotifyapi.SpotifyApi
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import retrofit.Callback
import retrofit.RetrofitError
import retrofit.client.Response

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
    private val spotifyAppRemote: SpotifyAppRemote? = null

    private lateinit var app: Ky

    private val viewModel: ContentViewModel by viewModels { ContentViewModel.Factory }

    private var lock: Any? = Any()

    private lateinit var fragmentContainerView: FragmentContainerView
    private lateinit var contentFragment: ContentFragment

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        app = application as Ky

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

                    val spotifyApi = SpotifyApi()
                    spotifyApi.setAccessToken(accessToken)
                    val spotifyService = spotifyApi.service

                    spotifyService.getCurrentUser(object : Callback<UserPrivate> {
                        override fun success(t: UserPrivate?, response: Response?) {
                            if (t!!.product == SPOTIFY_PREMIUM) {
                                // is premium
                                Log.d(TAG, "isPremium=true")

                                getAppRemote {
                                    viewModel.initForUser(spotifyService, it!!, t)

                                    lock = null
                                }
                            } else {
                                Log.d(TAG, "isPremium=false")
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