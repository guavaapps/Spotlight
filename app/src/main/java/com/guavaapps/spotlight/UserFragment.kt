package com.guavaapps.spotlight

import com.guavaapps.components.Components.getPx
import com.guavaapps.components.bitmap.BitmapTools.from
import com.guavaapps.spotlight.ColorSet.Companion.create
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import android.view.LayoutInflater
import android.view.ViewGroup
import android.os.Bundle
import android.view.animation.AnticipateOvershootInterpolator
import com.google.android.material.shape.ShapeAppearanceModel
import androidx.activity.OnBackPressedCallback
import androidx.navigation.fragment.NavHostFragment
import androidx.lifecycle.ViewModelProvider
import android.view.ViewOutlineProvider
import android.graphics.Outline
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.view.ViewGroup.MarginLayoutParams
import android.content.Intent
import com.pixel.spotifyapi.Objects.Pager
import com.pixel.spotifyapi.Objects.PlaylistSimple
import android.graphics.Bitmap
import android.util.TypedValue
import android.os.Looper
import retrofit.RetrofitError
import com.pixel.spotifyapi.Objects.Playlist
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.core.graphics.Insets
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.transition.MaterialContainerTransform
import com.guavaapps.components.listview.ListView
import retrofit.Callback
import retrofit.client.Response
import java.util.ArrayList
import java.util.LinkedHashMap
import java.util.concurrent.Executors

class UserFragment : Fragment() {
    private val viewModel: ContentViewModel by viewModels()
    private var mInsets: Insets? = null
    private val mPlaylists = LinkedHashMap<PlaylistWrapper, View>()
    private lateinit var mUserView: ImageView
    private lateinit var mUserNameView: TextView
    private lateinit var mSpotifyButton: MaterialButton
    private lateinit var mPlaylistListView: ListView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_user, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val transform = MaterialContainerTransform()
        transform.interpolator = AnticipateOvershootInterpolator(0.5f)
        transform.drawingViewId = R.id.fragment_container_view
        transform.duration =
            resources.getInteger(com.google.android.material.R.integer.material_motion_duration_long_2)
                .toLong()
        transform.fadeMode =
            MaterialContainerTransform.FADE_MODE_THROUGH
        transform.scrimColor = Color.TRANSPARENT
        transform.setAllContainerColors(Color.TRANSPARENT)
        val model = ShapeAppearanceModel.builder()
            .setAllCornerSizes(getPx(requireContext(), 32).toFloat())
            .build()
        transform.startShapeAppearanceModel = model
        //        transform.setEndShapeAppearanceModel (model);
        sharedElementEnterTransition = transform
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val callback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val navController = NavHostFragment.findNavController(this@UserFragment)
                navController.navigateUp()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
        mUserView = view.findViewById(R.id.user)
        mUserNameView = view.findViewById(R.id.user_name)
        mSpotifyButton = view.findViewById(R.id.spotify)
        mPlaylistListView = view.findViewById(R.id.list_view)
        mUserView!!.setOutlineProvider(object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                view.clipToOutline = true
                outline.setOval(0, 0, view.width, view.height)
            }
        })
        view.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val viewWidth = view.width
                val viewHeight = view.height
                mInsets = ViewCompat.getRootWindowInsets(view)!!
                    .getInsets(WindowInsetsCompat.Type.systemBars())
                val params = mUserView!!.getLayoutParams() as MarginLayoutParams
                params.topMargin = mInsets!!.top + getPx(this@UserFragment.requireContext(), 24)
                mUserView!!.setLayoutParams(params)
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
        viewModel.user.observe(viewLifecycleOwner) { userWrapper ->
            mUserView.setImageBitmap(userWrapper.thumbnail)
            mUserNameView.setText(userWrapper.user.display_name)
            mSpotifyButton.setOnClickListener(View.OnClickListener { v: View? ->
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(userWrapper.user.uri)
                intent.putExtra(Intent.EXTRA_REFERRER, "android-app://" + requireContext().packageName)
                startActivity(intent)
            })
            viewModel.spotifyService.value!!
                .getCurrentUserPlaylists(object : Callback<Pager<PlaylistSimple>> {
                    override fun success(
                        playlistSimplePager: Pager<PlaylistSimple>,
                        response: Response,
                    ) {
                        Executors.newSingleThreadExecutor().execute {
                            val items: MutableList<View> = ArrayList()
                            for (playlistSimple in playlistSimplePager.items) {
                                if (playlistSimple.owner.id != userWrapper.user.id) continue
                                val bitmap = from(playlistSimple.images[0].url)
                                val playlistWrapper = PlaylistWrapper(playlistSimple, bitmap)
                                val v = LayoutInflater.from(context)
                                    .inflate(R.layout.playlist_item, null, false)
                                v.layoutParams = ViewGroup.LayoutParams(-1, -2)
                                val bitmapView = v.findViewById<ImageView>(R.id.bitmap)
                                val nameView = v.findViewById<TextView>(R.id.name)
                                bitmapView.setImageBitmap(bitmap)
                                nameView.text = playlistSimple.name
                                val d = TypedValue()
                                context!!.theme.resolveAttribute(android.R.attr.selectableItemBackground,
                                    d,
                                    true)
                                v.foreground = resources.getDrawable(d.resourceId, context!!.theme)
                                v.setOnClickListener { v1: View? -> }
                                mPlaylists[playlistWrapper] = v
                                items.add(v)
                            }
                            val views = arrayOfNulls<View>(mPlaylists.size)
                            mPlaylists.values.toTypedArray()
                            Handler.createAsync(Looper.getMainLooper())
                                .post { mPlaylistListView.add(items) }
                        }
                    }

                    override fun failure(error: RetrofitError) {}
                })
            applyColorSet(userWrapper.thumbnail)
        }
    }

    private fun logDumpPlaylist(playlist: Playlist) {
        val id = playlist.id
        val name = playlist.name
        val owner = playlist.owner.display_name
        val isCollab = playlist.collaborative
        val isPublic = playlist.is_public
        val p = Playlist()
        Log.e(TAG, "-------- playlist log dump --------")
        Log.e(TAG, "name: $name")
        Log.e(TAG, "id: $id")
        Log.e(TAG, "owner: $owner")
        Log.e(TAG, "isCollab: $isCollab")
        Log.e(TAG, "isPublic: $isPublic")
        Log.e(TAG, "-------- --------")
    }

    private fun logDumpPlaylistSimple(playlistSimple: PlaylistSimple) {
        val id = playlistSimple.id
        val name = playlistSimple.name
        val owner = playlistSimple.owner.display_name
        val isCollab = playlistSimple.collaborative
        val isPublic = playlistSimple.is_public
        val p = Playlist()
        Log.e(TAG, "-------- playlist log dump --------")
        Log.e(TAG, "name: $name")
        Log.e(TAG, "id: $id")
        Log.e(TAG, "owner: $owner")
        Log.e(TAG, "isCollab: $isCollab")
        Log.e(TAG, "isPublic: $isPublic")
        Log.e(TAG, "-------- --------")
    }

    private fun applyColorSet(bitmap: Bitmap) {
        val colorSet = create(bitmap)
        mUserNameView!!.setTextColor(colorSet.primary)
        mSpotifyButton!!.setBackgroundColor(colorSet.primary)
        mSpotifyButton!!.setTextColor(colorSet.text)
        mSpotifyButton!!.rippleColor = ColorStateList.valueOf(colorSet.ripple)
    }

    companion object {
        private const val TAG = "UserFragment"
    }
}