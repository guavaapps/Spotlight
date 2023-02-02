package com.guavaapps.spotlight

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentContainerView
import com.guavaapps.components.color.Argb
import com.guavaapps.components.color.Hct
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
    private lateinit var app: App

    private val viewModel: ContentViewModel by viewModels { ContentViewModel.Factory }

    private val isSpotifyInstalled: Boolean
        get() {
            try {
                packageManager.getPackageInfo(SPOTIFY, 0)
                return true
            } catch (e: PackageManager.NameNotFoundException) {
            }
            return false
        }

    private var lock: Any? = Any()

    private lateinit var fragmentContainerView: FragmentContainerView
    private lateinit var contentFragment: ContentFragment

    fun createThemeColors() {
        val spotify = 0x1db954
        val tensorflow = 0xff6f00
        val app = 0x660073

        val hex = tensorflow

        val hct = Hct.fromInt(hex)

        hct.toInt().let {
            it.logHct()
            it.logArgb()
        }

        val colors = mutableListOf<String>()

        (0..100 step 10).forEach {
            Log.e(TAG, "tone=$it")
            val hct = Hct.fromInt(hex).apply { tone = it.toFloat() }
            hct.toInt().let {
                it.logHct()
                it.logArgb()

                val hexColor = "#${Integer.toHexString(it)}"

                colors.add(hexColor)
            }
        }

        Log.e(TAG, colors.mapIndexed {i, it -> "${i * 10}: $it"}.joinToString("\n"))
    }

    fun Int.logHct() = with(Hct.fromInt(this)) {
        Log.e(TAG, "    [hct] ${this@logHct} - h=$hue c=$chroma t=$tone")
    }

    fun Int.logArgb() = with(Argb.from(this)) {
        Log.e(TAG, "    [hct] ${this@logArgb} - a=$alpha h=$red c=$green t=$blue")
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createThemeColors()

        Log.i(TAG,
            "i love you sooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo much ")

        app = application as App

        Ducky.quack(this)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContentView(R.layout.activity_main)

        fragmentContainerView = findViewById(R.id.fragment_container_view)
        contentFragment = ContentFragment()

        val content = findViewById<View>(android.R.id.content)

        content.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (lock == null) {
                    content.viewTreeObserver.removeOnPreDrawListener(this)
                }
                return false
            }
        })

        if (isSpotifyInstalled) {
            auth()
        } else {
            // show install spotify fragment
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)

        if (requestCode == REQUEST_AUTH) {
            val response = AuthorizationClient.getResponse(resultCode, intent)

            when (response.type) {
                AuthorizationResponse.Type.TOKEN -> {
                    val accessToken = response.accessToken

                    val spotifyApi = SpotifyApi()
                    spotifyApi.setAccessToken(accessToken)
                    val spotifyService = spotifyApi.service

                    spotifyService.getCurrentUser(object : Callback<UserPrivate> {
                        override fun success(t: UserPrivate, response: Response) {
                            if (t.product == SPOTIFY_PREMIUM) {
                                getAppRemote {
                                    viewModel.initForUser(spotifyService, it!!, t)

                                    lock = null
                                }
                            }
                        }

                        override fun failure(error: RetrofitError) {
                            // not authorised
                            Log.d(TAG, "error verifying user")
                            if (error.response.status == 401) {
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
                    Log.d(TAG,
                        "SpotifyAppRemote failed to connect - ${error.message} cause=${error.cause}")

                    error.printStackTrace()
                    onResult(null)
                }
            })
    }
}

val spotifyContainer = 0x72fe8f
val spotifyText = 0x002107
val appContainer = 0xffd5fe
val appText = 0x36003f
val tfContainer = 0xffdbc9
val tfText = 0x341000

val `ViewModelkt` = 0