package com.guavaapps.spotlight

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.*
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.Insets
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.*
import androidx.fragment.app.*
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.transition.TransitionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.transition.*
import com.guavaapps.components.Components.getPx
import com.guavaapps.components.Components.getPxF
import com.guavaapps.components.color.Argb
import com.guavaapps.components.color.Hct
import com.guavaapps.components.listview.ListView
import com.guavaapps.spotlight.ColorSet.Companion.create

class UserFragment : Fragment(R.layout.fragment_user) {
    private val viewModel: ContentViewModel by activityViewModels { ContentViewModel.Factory }
    private var insets: Insets? = null

    private lateinit var content: ViewGroup

    private var colorSet = ColorSet()

    // user
    private lateinit var userView: ImageView
    private lateinit var userNameView: TextView
    private lateinit var userIdView: TextView
    private lateinit var spotifyButton: MaterialButton
    private lateinit var manageAccountButton: MaterialButton
    private lateinit var signOutAccountButton: MaterialButton
    private lateinit var deleteAccountButton: MaterialButton
    private lateinit var cancelButton: MaterialButton

    private lateinit var addPlaylistFragment: MaterialCardView

    // playlists
    private lateinit var listView: ListView
    private val views: MutableList<View> = mutableListOf()
    private var selectedView: View? = null

    // animators
    private var viewAnimator: ValueAnimator? = null
    private var prevAnimator: ValueAnimator? = null

    private var viewTextAnimator: ValueAnimator? = null
    private var prevTextAnimator: ValueAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val animDuration =
            resources.getInteger(com.google.android.material.R.integer.material_motion_duration_long_2)
                .toLong()

        val transform = MaterialContainerTransform().apply {
            drawingViewId = R.id.fragment_container_view
            duration = animDuration
            scrimColor = Color.TRANSPARENT
            containerColor = Color.TRANSPARENT
            startContainerColor = Color.TRANSPARENT
            endContainerColor = Color.TRANSPARENT
        }

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

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }

        with(requireActivity().window) {
            WindowCompat.getInsetsController(this, decorView).let {
                // true for light theme i.e. dark status bar
                it.isAppearanceLightStatusBars = true

                // change back to light status bar on exit
                view.doOnDetach { _ ->
                    it.isAppearanceLightStatusBars = false
                }
            }
        }

        val callback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val navController = NavHostFragment.findNavController(this@UserFragment)
                navController.navigateUp()
            }
        }

        requireActivity().onBackPressedDispatcher
            .addCallback(viewLifecycleOwner, callback)

        content = view.findViewById(R.id.content)

        userView = view.findViewById(R.id.user)
        userNameView = view.findViewById(R.id.user_name)
        userIdView = view.findViewById(R.id.user_id)
        spotifyButton = view.findViewById(R.id.spotify)
        manageAccountButton = view.findViewById(R.id.manage_account)
        signOutAccountButton = view.findViewById(R.id.sign_out)
        deleteAccountButton = view.findViewById(R.id.delete_account)
        cancelButton = view.findViewById(R.id.cancel)

        listView = view.findViewById(R.id.list_view)
        listView.canScroll = false

        addPlaylistFragment = view.findViewById(R.id.add)

        spotifyButton.apply {
            doOnLayout {
                val radius = height - insetTop - insetBottom

//                shapeAppearanceModel = ShapeAppearanceModel.builder(
//                    requireContext(),
//                    R.style.ButtonGroupCenter,
//                    0)
//                    .setTopLeftCornerSize(radius / 2f)
//                    .setBottomLeftCornerSize(radius / 2f)
//                    .build()
            }
        }

        manageAccountButton.apply {
            doOnLayout {
                val radius = height - insetTop - insetBottom

                shapeAppearanceModel = ShapeAppearanceModel.builder(
                    requireContext(),
                    R.style.ButtonGroupCenter,
                    0)
                    .setTopRightCornerSize(radius / 2f)
                    .setBottomRightCornerSize(radius / 2f)
                    .build()
            }
        }

        signOutAccountButton.apply {
            doOnLayout {
                val radius = height - insetTop - insetBottom

                shapeAppearanceModel = ShapeAppearanceModel.builder(
                    requireContext(),
                    R.style.ButtonGroupCenter,
                    0)
                    .setTopLeftCornerSize(radius / 2f)
                    .setTopRightCornerSize(radius / 2f)
                    .build()
            }
        }

        deleteAccountButton.apply {
            doOnLayout {
                val radius = height - insetTop - insetBottom

                shapeAppearanceModel = ShapeAppearanceModel.builder(
                    requireContext(),
                    R.style.ButtonGroupCenter,
                    0)
//                    .setAllCornerSizes(getPxF(requireContext(), 12))
                    .build()
            }
        }

//        addPlaylistFragment.elevation = getPxF(requireContext(), 4)
        addPlaylistFragment.apply {
            doOnLayout {
                val button = signOutAccountButton

                val radius = button.height - button.insetTop - button.insetBottom

                Log.e(TAG, "radius ------ $radius")

                shapeAppearanceModel = ShapeAppearanceModel.builder(
                    requireContext(),
                    R.style.ButtonGroupCenter,
                    R.style.ButtonGroupCenter)
                    .setAllCornerSizes(radius / 2f)
                    .build()

                addPlaylistFragment.radius = radius / 2f
            }
        }

        cancelButton.apply {
            doOnLayout {
                val radius = height - insetTop - insetBottom

                shapeAppearanceModel = ShapeAppearanceModel.builder(
                    requireContext(),
                    R.style.ButtonGroupCenter,
                    0)
                    .setBottomLeftCornerSize(radius / 2f)
                    .setBottomRightCornerSize(radius / 2f)
                    .build()
            }
        }

        manageAccountButton.setOnClickListener {
            showAlertDialog(it)
        }

        signOutAccountButton.setOnClickListener {
            hideAlertDialog(manageAccountButton)
        }

        deleteAccountButton.setOnClickListener {
            hideAlertDialog(manageAccountButton)
        }

        cancelButton.setOnClickListener {
            hideAlertDialog(manageAccountButton)
        }

        listView.setOnScrollChangeListener { _, _, _, _, _ ->
            val scrollY = listView.computeVerticalScrollOffset()

            val MAX = getPxF(requireContext(), 24)
            val e = if (scrollY >= MAX) 1f
            else scrollY / MAX

            val start = Hct.fromInt(colorSet.primary).toInt()
            var end = Argb.from(colorSet.tertiary).apply {
                red = 100f
                alpha = 255f
            }.toInt()
            val a = ArgbEvaluator()
            val c = a.evaluate(e, start, end) as Int
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
            Log.e(TAG, "playlists - ${it.size}")

            views.clear()
            listView.clear()

            val drawable =
                ResourcesCompat.getDrawable(resources,
                    R.drawable.ic_playlist_48,
                    requireContext().theme)!!.mutate()
            drawable.setTint(colorSet.text)

            val it = it.map {
                if (it.thumbnail == null) {
                    Log.e(TAG, "--------- aaaaaaaaaa----------")
                    it.thumbnail = drawable.toBitmap(640, 640, Bitmap.Config.ARGB_8888)
                }

                it
            }

            views.addAll(PlaylistView.createAll(requireContext(), it) { view, playlist ->
                selectView(selectedView, view)
                selectedView = view

                viewModel.setMainPlaylist(playlist.playlist?.id)
            })

            listView.add(listOf(
                *views.toTypedArray(),
            ))

            applyColorsToPlaylistListView(viewModel.user.value?.bitmap!!)
        }

        viewModel.playlist.observe(viewLifecycleOwner) {
            Log.e(TAG, "id=${it?.playlist?.id} size=${views.size}")

            val view = views.find { v ->
                Log.e(TAG, "tag=${v.tag} id=${it?.playlist?.id}")

                v.tag == it?.playlist?.id
            } ?: return@observe

            Log.e(TAG, "isViewNull=false")

            view.doOnLayout {
                selectView(selectedView, view)
                selectedView = view
            }
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
    }

    private fun applyColorSet(bitmap: Bitmap) {
        val colorSet = create(bitmap)

        requireView().setBackgroundColor(colorSet.primary)

        addPlaylistFragment.setBackgroundColor(colorSet.primary)

        userNameView.setTextColor(colorSet.text)
        userIdView.setTextColor(colorSet.text)
        spotifyButton.setBackgroundColor(colorSet.color40)
        manageAccountButton.setBackgroundColor(colorSet.color40)
        signOutAccountButton.setBackgroundColor(colorSet.color40)
        deleteAccountButton.setBackgroundColor(colorSet.color40)
        cancelButton.setBackgroundColor(colorSet.color40)

        val buttonTextColor = with(Hct.fromInt(colorSet.primary)) {
            tone = 100f
            toInt()
        }

        spotifyButton.setTextColor(buttonTextColor)
        manageAccountButton.setTextColor(buttonTextColor)
        signOutAccountButton.setTextColor(buttonTextColor)
        deleteAccountButton.setTextColor(buttonTextColor)
        cancelButton.setTextColor(buttonTextColor)

        spotifyButton.rippleColor = ColorStateList.valueOf(colorSet.ripple)
        manageAccountButton.rippleColor = ColorStateList.valueOf(colorSet.ripple)
        signOutAccountButton.rippleColor = ColorStateList.valueOf(colorSet.ripple)
        deleteAccountButton.rippleColor = ColorStateList.valueOf(colorSet.ripple)
        cancelButton.rippleColor = ColorStateList.valueOf(colorSet.ripple)

        this.colorSet = colorSet
    }

    private fun applyColorsToPlaylistListView(bitmap: Bitmap) {
        val colorSet = create(bitmap)

        views.forEach {
            it.setBackgroundColor(colorSet.primary)
            it.findViewById<TextView>(R.id.name).setTextColor(colorSet.text)
        }

    }

    private fun selectView(prev: View?, view: View) {
        if (viewAnimator != null) viewAnimator?.cancel()

        if (prev != null) {
            val prev = prev.findViewById<View>(R.id.content)
            val prevNameView = prev.findViewById<TextView>(R.id.name)

            val prevStart = (prev.background as ColorDrawable?)?.color ?: Color.TRANSPARENT
            val prevEnd = Color.TRANSPARENT

            val prevTextStart = prevNameView.textColors.defaultColor
            val prevTextEnd = colorSet.text

            prev.setBackgroundColor(prevEnd)

            prevAnimator = ValueAnimator.ofObject(ArgbEvaluator(), prevStart, prevEnd)
            with(prevAnimator!!) {
                duration = 150
                addUpdateListener {
                    prev.setBackgroundColor(it.animatedValue as Int)
                }

                start()
            }

            prevTextAnimator =
                ValueAnimator.ofObject(ArgbEvaluator(), prevTextStart, prevTextEnd)
            with(prevTextAnimator!!) {
                duration = 150
                addUpdateListener {
                    prevNameView.setTextColor(it.animatedValue as Int)
                }

                start()
            }
        }

        val view = view.findViewById<View>(R.id.content)
        val nameView = view.findViewById<TextView>(R.id.name)

        val start = (view.background as ColorDrawable?)?.color ?: Color.TRANSPARENT
        val end = colorSet.color40

        val textStart = nameView.textColors.defaultColor
        val textEnd = colorSet.primary

        with(Hct.fromInt(colorSet.primary)) {
            tone = 100f
            toInt()
        }

        viewAnimator = ValueAnimator.ofObject(ArgbEvaluator(), start, end)
        with(viewAnimator!!) {
            duration = 150
            addUpdateListener {
                view.setBackgroundColor(it.animatedValue as Int)
            }

            start()
        }

        viewTextAnimator = ValueAnimator.ofObject(ArgbEvaluator(), textStart, textEnd)
        with(viewTextAnimator!!) {
            duration = 150
            addUpdateListener {
                nameView.setTextColor(it.animatedValue as Int)
            }

            start()
        }
    }

    private fun showAlertDialog(view: View) {
        val t = MaterialContainerTransform().apply { // TODO create exit trans
            startView = view
            endView = addPlaylistFragment
            duration = 750
            scrimColor = Color.TRANSPARENT
//            startShapeAppearanceModel = (view as Shapeable).shapeAppearanceModel
//            endShapeAppearanceModel = addPlaylistFragment.shapeAppearanceModel
            addTarget(addPlaylistFragment)
        }

        view.visibility = INVISIBLE
        addPlaylistFragment.visibility = VISIBLE

        TransitionManager.beginDelayedTransition(content, t)
    }

    private fun hideAlertDialog(view: View) {
        val t = MaterialContainerTransform().apply { // TODO create exit trans
            startView = addPlaylistFragment
            endView = view
            duration = 350
            scrimColor = Color.TRANSPARENT
//            startShapeAppearanceModel = addPlaylistFragment.shapeAppearanceModel
//            endShapeAppearanceModel = (view as Shapeable).shapeAppearanceModel

            addTarget(view)
        }

        view.visibility = VISIBLE
        addPlaylistFragment.visibility = INVISIBLE
        TransitionManager.beginDelayedTransition(content, t)
    }

    companion object {
        private const val TAG = "UserFragment"
    }
}