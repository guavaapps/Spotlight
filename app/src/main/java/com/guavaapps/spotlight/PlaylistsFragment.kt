package com.guavaapps.spotlight

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.button.MaterialButton
import com.google.android.material.shape.ShapeAppearanceModel
import com.guavaapps.components.Components.getPx
import com.guavaapps.components.Components.getPxF
import com.guavaapps.components.listview.ListView
import com.guavaapps.spotlight.realm.d

private const val TAG = "PlaylistsFragment"

private typealias AListView = android.widget.ListView

class PlaylistsFragment : Fragment(R.layout.fragment_playlists) {
    private val paddingX: Int by lazy { getPx(requireContext(), 24) }
    private val paddingY: Int by lazy { getPx(requireContext(), 24) }

    private val viewModel: ContentViewModel by activityViewModels { ContentViewModel.Factory }

    private var selected: String? = null
    private var views: List<View>? = null
    private var addButton: MaterialButton? = null

    private lateinit var listView: ListView

    private var colorSet = ColorSet()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listView = view.findViewById(R.id.list_view)
        listView.clipToOutline = true

        val b = Bitmap.createBitmap(
            IntArray(500 * 500) { Color.RED },
            500, 500,
            Bitmap.Config.ARGB_8888
        )

        viewModel.playlists.observe(viewLifecycleOwner) {
            Log.e(TAG, "playlists - ${
                it.joinToString { it.playlist?.name ?: "null" }
            }")

            with(listView) {
                clear()
                add(
                    listOf(
                        *PlaylistView.createAll(
                            requireContext(),
                            it
                        ) { _, _ -> }.also { views = it }
                            .toTypedArray(),
                        PlaylistView.createAddButton(requireContext()) { Log.e(TAG, "add") }
                            .also { addButton = it }
                    )
                )
            }

            applyShapeAppearance()
            applyColors(b)//viewModel.user.value?.bitmap)

            val selectedView = views?.find { it.tag == selected } ?: return@observe

            selectView(null, selectedView)
        }

        viewModel.playlist.observe(viewLifecycleOwner) {
            selected = it?.playlist?.id

            val selectedView =
                if (views != null) views?.find { it.tag == selected } ?: return@observe
                else return@observe

            selectView(null, selectedView)
        }

        view.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)

                val insets = ViewCompat.getRootWindowInsets(view)!!
                    .getInsets(WindowInsetsCompat.Type.systemBars())



                with(insets) {
                    Log.e(TAG, "insets - top=$top")
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()

        viewModel.getPlaylists()
    }

    private fun applyColors(bitmap: Bitmap?) {
        val view = requireView()

        val colorSet = ColorSet.create(bitmap)

        val titleView = view.findViewById<TextView>(R.id.title)
        val helpView = view.findViewById<TextView>(R.id.help)

        titleView.setTextColor(colorSet.text)
        helpView.setTextColor(colorSet.text)

        view.setBackgroundColor(colorSet.primary)

//        listView.setBackgroundColor(colorSet.primary)

        views?.forEach {
            val typedArray =
                requireContext().theme.obtainStyledAttributes(android.R.style.Theme_Material_NoActionBar,
                    intArrayOf(android.R.attr.selectableItemBackground))

            val ripple =
                resources.getDrawable(typedArray.getResourceId(0, 0), requireContext().theme)
                    .mutate()

            ripple.setTint(colorSet.ripple)

            it.background = ripple

            val nameView = it.findViewById<TextView>(R.id.name)
            nameView.setTextColor(colorSet.color10)
        }

        addButton?.backgroundTintList = ColorStateList.valueOf(colorSet.color40)

        this.colorSet = colorSet
    }

    private var viewAnimator: ValueAnimator? = null
    private var prevAnimator: ValueAnimator? = null

    private fun selectView(prev: View?, view: View) {
        if (viewAnimator != null) viewAnimator?.cancel()

        if (prev != null) {
            val prev = prev.findViewById<View>(R.id.content)

            val prevStart = (prev.background as ColorDrawable?)?.color ?: Color.TRANSPARENT
            val prevEnd = Color.TRANSPARENT

            // vincent
            prevAnimator = ValueAnimator.ofObject(ArgbEvaluator(), prevStart, prevEnd)
            with(prevAnimator!!) {
                duration = 200
                addUpdateListener {
                    prev.setBackgroundColor(it.animatedValue as Int)
                }

                start()
            }
        }

        val view = view.findViewById<View>(R.id.content)
        val start = (view.background as ColorDrawable?)?.color ?: Color.TRANSPARENT
        val end = colorSet.color40

        viewAnimator = ValueAnimator.ofObject(ArgbEvaluator(), start, end)
        with(viewAnimator!!) {
            duration = 200
            addUpdateListener {
                view.setBackgroundColor(it.animatedValue as Int)
            }

            start()
        }
    }

    private val View.viewContent: View?
        get() = findViewById(R.id.content)

    private fun applyShapeAppearance() {
        views?.forEach { it.viewContent?.setPadding(paddingX, paddingX, paddingX, paddingX) }
        views?.first()?.viewContent?.setPadding(paddingX, paddingX, paddingX, paddingX)
    }
}

object PlaylistView {
    fun create(
        context: Context,
        playlist: PlaylistWrapper,
        onClick: (View, PlaylistWrapper) -> Unit,
    ): View {
        val view = LayoutInflater.from(context).inflate(R.layout.track_view_item, null)
        view.tag = playlist.playlist?.id
        view.layoutParams = ViewGroup.MarginLayoutParams(-1, -2).apply {
            topMargin = getPx(context, 6)
            leftMargin = getPx(context, 12)
            rightMargin = getPx(context, 12)
            bottomMargin = getPx(context, 6)
        }

        val bitmap = view.findViewById<ImageView>(R.id.bitmap)
        val name = view.findViewById<TextView>(R.id.name)

        bitmap.setImageBitmap(playlist.thumbnail)
        name.text = playlist.playlist?.name

        val typedArray =
            context.theme.obtainStyledAttributes(android.R.style.Theme_Material_NoActionBar,
                intArrayOf(android.R.attr.selectableItemBackground))

        val ripple =
            context.resources.getDrawable(typedArray.getResourceId(0, 0), context.theme)
                .mutate()

        view.background = ripple
        view.setBackgroundColor(Color.RED)

        view.clipToOutline = true
        view.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, getPxF(context, 24))
            }
        }

        view.setOnClickListener { onClick(view, playlist) }

        return view
    }

    fun createAll(
        context: Context,
        playlists: List<PlaylistWrapper>,
        onClick: (View, PlaylistWrapper) -> Unit,
    ): List<View> {
        return playlists.mapIndexed { i, it -> PlaylistView.create(context, it, onClick) }
    }

    fun createAddButton(context: Context, onClick: () -> Unit): MaterialButton {
        val button = MaterialButton(context)

        button.text = "Add"
        button.setBackgroundColor(Color.MAGENTA)
        button.icon = context.resources.getDrawable(R.drawable.ic_add_24, context.theme)
        button.iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START

        button.shapeAppearanceModel = ShapeAppearanceModel.builder()
            .setAllCornerSizes(getPxF(context, 24))
            .build()

        button.insetTop = getPx(context, 6)
        button.insetBottom = getPx(context, 6)

        button.layoutParams = ViewGroup.MarginLayoutParams(-1, getPx(context, 76 + 12)).apply {
            leftMargin = getPx(context, 12)
            rightMargin = getPx(context, 12)
        }

        return button
    }

    fun wrapButton(button: MaterialButton) {
        val wrapper = FrameLayout(button.context)
        wrapper.addView(button)
    }
}