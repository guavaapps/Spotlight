package com.guavaapps.spotlight

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.ColorDrawable
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
import androidx.annotation.CallSuper
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.transition.*
import com.guavaapps.components.Components.getPx
import com.guavaapps.components.Components.getPxF
import com.guavaapps.components.color.Argb
import com.guavaapps.components.color.Hct
import com.guavaapps.components.listview.ListView
import com.guavaapps.spotlight.ColorSet.Companion.create
import java.net.CookieHandler

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

    // playlists
    private lateinit var listView: ListView
    private val views: MutableList<View> = mutableListOf()
    private var selectedView: View? = null

    private val addButton: MaterialButton by lazy { PlaylistView.createAddButton(requireContext()) {} }

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

            content.setBackgroundColor(c)
            content.elevation = e * getPx(requireContext(), 4)

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

            views.addAll(PlaylistView.createAll(requireContext(), it) { view, playlist ->
                selectView(selectedView, view)
                selectedView = view
            })

            views.addAll(PlaylistView.createAll(requireContext(), it) { _, _ ->

            })

            views.addAll(PlaylistView.createAll(requireContext(), it) { _, _ ->

            })

            val placeholderView = View(requireContext()).apply {
                layoutParams = ViewGroup.LayoutParams(-1, content.height)
                setBackgroundColor(Color.TRANSPARENT)
            }

            listView.add(listOf(
                placeholderView,
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

        this.colorSet = colorSet
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

//        views.firstOrNull()?.setBackgroundColor(colorSet.color40)
//        views.firstOrNull()?.elevation = getPxF(requireContext(), 2)
//        views.firstOrNull()?.findViewById<TextView>(R.id.name)
//            ?.setTextColor(t)
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

//            prev.setBackgroundColor(prevEnd)

            // vincent
            prevAnimator = ValueAnimator.ofObject(ArgbEvaluator(), prevStart, prevEnd)
            with(prevAnimator!!) {
                duration = 200
                addUpdateListener {
                    prev.setBackgroundColor(it.animatedValue as Int)
                }

                start()
            }

            prevTextAnimator = ValueAnimator.ofObject(ArgbEvaluator(), prevTextStart, prevTextEnd)
            with(prevTextAnimator!!) {
                duration = 200
                addUpdateListener {
                    prevNameView.setBackgroundColor(it.animatedValue as Int)
                }

                start()
            }
        }

        val view = view.findViewById<View>(R.id.content)
        val nameView = view.findViewById<TextView>(R.id.name)

        val start = (view.background as ColorDrawable?)?.color ?: Color.TRANSPARENT
        val end = colorSet.color40

        val textStart = nameView.textColors.defaultColor
        val textEnd = with(Hct.fromInt(colorSet.primary)) {
            tone = 100f
            toInt()
        }

//        view.setBackgroundColor(end)

        viewAnimator = ValueAnimator.ofObject(ArgbEvaluator(), start, end)
        with(viewAnimator!!) {
            duration = 200
            addUpdateListener {
                view.setBackgroundColor(it.animatedValue as Int)
            }

            start()
        }

        viewTextAnimator = ValueAnimator.ofObject(ArgbEvaluator(), textStart, textEnd)
        with(viewTextAnimator!!) {
            duration = 200
            addUpdateListener {
                nameView.setBackgroundColor(it.animatedValue as Int)
            }

            start()
        }
    }

    companion object {
        private const val TAG = "UserFragment"
    }
}