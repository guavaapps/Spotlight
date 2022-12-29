package com.guavaapps.spotlight

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.ViewOutlineProvider
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.transition.*
import com.guavaapps.components.Components.getPx
import com.guavaapps.components.Components.getPxF
import com.guavaapps.components.color.Hct
import com.guavaapps.components.listview.ListView
import com.guavaapps.spotlight.ColorSet.Companion.create
import java.net.CookieHandler

class UserFragment : Fragment(R.layout.fragment_user) {
    private val viewModel: ContentViewModel by activityViewModels { ContentViewModel.Factory }
    private var insets: Insets? = null

    private lateinit var content: ViewGroup

    private lateinit var userView: ImageView
    private lateinit var userNameView: TextView
    private lateinit var userIdView: TextView
    private lateinit var spotifyButton: MaterialButton

    private lateinit var listView: ListView
    private val views: MutableList<View> = mutableListOf()
    private val selectedView: View? = null

    private val addButton: MaterialButton by lazy { PlaylistView.createAddButton(requireContext()) {} }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val animDuration =
            resources.getInteger(com.google.android.material.R.integer.material_motion_duration_long_2)
                .toLong()

        val transform = MaterialContainerTransform().apply {
//            interpolator = AnticipateOvershootInterpolator(0.5f)
            drawingViewId = R.id.fragment_container_view
            duration = animDuration
//            fadeMode = MaterialContainerTransform.FADE_MODE_CROSS
            scrimColor = Color.TRANSPARENT
            containerColor = Color.TRANSPARENT
            startContainerColor = Color.TRANSPARENT
            endContainerColor = Color.TRANSPARENT
        }

//        transform.interpolator = AnticipateOvershootInterpolator(0.5f)
//        transform.drawingViewId = R.id.fragment_container_view
//        transform.duration = 5000
//        resources.getInteger(com.google.android.material.R.integer.material_motion_duration_long_2)
//                .toLong()
//
//        transform.fadeMode = MaterialContainerTransform.FADE_MODE_THROUGH
//
//        transform.scrimColor = Color.TRANSPARENT
//        transform.setAllContainerColors(Color.TRANSPARENT)
//        val model = ShapeAppearanceModel.builder()
//            .setAllCornerSizes(getPx(requireContext(), 32).toFloat())
//            .build()
//        transform.startShapeAppearanceModel = model
//        transform.endShapeAppearanceModel = model
        sharedElementEnterTransition = transform

        enterTransition = MaterialFade().apply {
            mode = MaterialFade.MODE_IN
            secondaryAnimatorProvider = null
            duration = animDuration - 100
        }

        exitTransition = MaterialFade().apply {
            mode = MaterialFade.MODE_OUT
            secondaryAnimatorProvider = null
            duration = animDuration - 100
        }

        returnTransition = exitTransition
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }

//        sharedElementEnterTransition = MaterialContainerTransform().apply {
//            // Manually add the Views to be shared since this is not a standard Fragment to
//            // Fragment shared element transition.
//            startView = parentFragment?.requireView()?.findViewById(R.id.user)
//            endView = view//.findViewById(R.id.user)
//            duration = 5000//resources.getInteger(R.integer.reply_motion_duration_large).toLong()
//            scrimColor = Color.TRANSPARENT
//            containerColor = Color.TRANSPARENT
//            fadeMode = MaterialContainerTransform.FADE_MODE_THROUGH
//            startContainerColor = Color.TRANSPARENT
//            endContainerColor = Color.TRANSPARENT
//        }
//
//        returnTransition = MaterialContainerTransform().apply {
//            // Manually add the Views to be shared since this is not a standard Fragment to
//            // Fragment shared element transition.
//            startView = view//view.findViewById(R.id.user)
//            endView = parentFragment?.requireView()?.findViewById(R.id.user)
//            duration = 5000//resources.getInteger(R.integer.reply_motion_duration_large).toLong()
//            scrimColor = Color.TRANSPARENT
//            containerColor = Color.TRANSPARENT
//            fadeMode = MaterialContainerTransform.FADE_MODE_THROUGH
//            startContainerColor = Color.TRANSPARENT
//            endContainerColor = Color.TRANSPARENT
//            addTarget(view)
//        }

        val callback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val navController = NavHostFragment.findNavController(this@UserFragment)
                navController.navigateUp()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)

        content = view.findViewById(R.id.content)

        userView = view.findViewById(R.id.user)
        userNameView = view.findViewById(R.id.user_name)
        userIdView = view.findViewById(R.id.user_id)
        spotifyButton = view.findViewById(R.id.spotify)

        listView = view.findViewById(R.id.list_view)

        listView.setOnScrollChangeListener { view, _, _, _, _ ->
            val scrollY = listView.computeVerticalScrollOffset()

            val MAX = getPxF(requireContext(), 4)
            val e = if (scrollY >= MAX) 1f
            else scrollY / MAX

            val start = Hct.fromInt(Color.RED)
            val (sHue, sChroma, sTone) = with(start) {
                Triple(hue, chroma, tone)
            }

            var end = Hct.fromInt(Color.RED)
            val (eHue, eChroma, eTone) = with(end) {
                Triple(hue, chroma, tone)
            }

            val hue = sHue + (eHue - sHue) * e
            val chroma = sChroma + (eChroma - sChroma) * e
            val tone = sTone + (eTone - sTone) * e

            val c = Hct.fromInt(0)
            c.hue = hue
            c.chroma = chroma
            c.tone = tone

            content.setBackgroundColor(c.toInt())

            Log.e(TAG, "scrollY=$scrollY e=$e elevation=${(4 * e).toInt()}")
        }

        view.viewTreeObserver.addOnGlobalLayoutListener(object :
            OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)

                insets = ViewCompat.getRootWindowInsets(view)!!
                    .getInsets(WindowInsetsCompat.Type.systemBars())

                with(userView.layoutParams as MarginLayoutParams) {
                    topMargin = insets!!.top + getPx(this@UserFragment.requireContext(), 24)

                    userView.layoutParams = this
                }
            }
        })

        viewModel.playlists.observe(viewLifecycleOwner) {
            views.clear()
            listView.clear()

            views.addAll(PlaylistView.createAll(requireContext(), it) { _, _ ->

            })

            views.addAll(PlaylistView.createAll(requireContext(), it) { _, _ ->

            })

            views.addAll(PlaylistView.createAll(requireContext(), it) { _, _ ->

            })

            listView.add(listOf(
                *views.toTypedArray(),
                //PlaylistView.createAddButton(requireContext()) {}.also { addButton = it }
                addButton
            ))

            addButton.insetBottom = ViewCompat.getRootWindowInsets(view)
                ?.getInsets(WindowInsetsCompat.Type.systemBars())
                ?.bottom ?: 0

            applyColorsToPlaylistListView(viewModel.user.value?.bitmap!!)
        }

        viewModel.user.observe(viewLifecycleOwner) { userWrapper ->
            userView.setImageBitmap(userWrapper.bitmap)
            userNameView.text = userWrapper.user.display_name

            userIdView.text = userWrapper.user?.id


            spotifyButton.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(userWrapper.user.uri)
                intent.putExtra(Intent.EXTRA_REFERRER,
                    "android-app://" + requireContext().packageName)
                startActivity(intent)
            }
            applyColorSet(userWrapper.bitmap!!)
        }

        viewModel.getPlaylists()
    }

    private fun applyColorSet(bitmap: Bitmap) {
        val colorSet = create(bitmap)

        requireView().setBackgroundColor(colorSet.primary)

        userNameView.setTextColor(colorSet.text)
        userIdView.setTextColor(colorSet.text)
        spotifyButton.setBackgroundColor(colorSet.color40)

        val buttonTextColor = with(Hct.fromInt(colorSet.primary)) {
            tone = 100f
            toInt()
        }

        spotifyButton.setTextColor(buttonTextColor)

        spotifyButton.rippleColor = ColorStateList.valueOf(colorSet.ripple)
    }

    private fun applyColorsToPlaylistListView(bitmap: Bitmap) {
        val colorSet = create(bitmap)

        views.forEach {
            it.setBackgroundColor(colorSet.primary)
            it.findViewById<TextView>(R.id.name).setTextColor(colorSet.text)
        }

        val t = with(Hct.fromInt(colorSet.primary)) {
            tone = 100f
            toInt()
        }

        views.firstOrNull()?.setBackgroundColor(colorSet.color40)
        views.firstOrNull()?.elevation = getPxF(requireContext(), 2)
        views.firstOrNull()?.findViewById<TextView>(R.id.name)
            ?.setTextColor(t)
    }

    companion object {
        private const val TAG = "UserFragment"
    }
}