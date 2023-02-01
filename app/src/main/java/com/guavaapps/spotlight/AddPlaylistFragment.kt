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
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.graphics.Insets
import androidx.core.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.NavHostFragment
import androidx.transition.TransitionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.transition.*
import com.guavaapps.components.Components.getPx
import com.guavaapps.components.Components.getPxF
import com.guavaapps.components.color.Argb
import com.guavaapps.components.color.Hct
import com.guavaapps.components.listview.ListView
import com.guavaapps.spotlight.ColorSet.Companion.create
import org.w3c.dom.Text

class AddPlaylistFragment : Fragment(R.layout.fragment_add) {
    private val viewModel: ContentViewModel by activityViewModels { ContentViewModel.Factory }
    private var insets: Insets? = null

    private lateinit var content: ViewGroup

    private var colorSet = ColorSet()

    // user
    private lateinit var userView: ImageView
    private val titleView: TextView by lazy { requireView().findViewById(R.id.title) }
    private val nameView: TextInputLayout by lazy { requireView().findViewById(R.id.name) }
    private val addButton: MaterialButton by lazy { requireView().findViewById(R.id.add) }
    private val cancelButton: MaterialButton by lazy { requireView().findViewById(R.id.cancel) }

    private var positiveBlock: (String) -> Unit = {}
    private var negativeBlock: () -> Unit = {}

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

        ////////
//        sharedElementEnterTransition = transform
//
//        enterTransition = MaterialFade().apply {
//            mode = MaterialFade.MODE_IN
//            secondaryAnimatorProvider = null
//            duration = animDuration - 100
//        }
//
//        exitTransition = MaterialFade().apply {
//            mode = MaterialFade.MODE_OUT
//            secondaryAnimatorProvider = null
//            duration = animDuration - 100
//        }
//
//        returnTransition = exitTransition
    }

    @SuppressLint("ClickableViewAccessibility")
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

        // this fragment always uses a light theme
        // make status bar contrast with the background
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
                val navController = NavHostFragment.findNavController(this@AddPlaylistFragment)
                navController.navigateUp()
            }
        }
        requireActivity().onBackPressedDispatcher
            .addCallback(viewLifecycleOwner, callback)

        viewModel.user.observe(viewLifecycleOwner) {
            if (it.bitmap != null) applyColorSet(it.bitmap!!)
        }

        addButton.setOnClickListener { positiveBlock(nameView.editText?.text?.toString()!!) }
        cancelButton.setOnClickListener { negativeBlock() }

    }

    private fun applyColorSet(bitmap: Bitmap) {
        val colorSet = create(bitmap)

        requireView().setBackgroundColor(colorSet.primary)

        titleView.setTextColor(colorSet.text)
        nameView.editText?.setTextColor(colorSet.text)
        nameView.editText?.setHintTextColor(colorSet.text)
        nameView.boxStrokeColor = colorSet.text

        val buttonTextColor = with(Hct.fromInt(colorSet.primary)) {
            tone = 100f
            toInt()
        }

        this.colorSet = colorSet
    }

    fun onPositive(block: (name: String) -> Unit) {
        positiveBlock = block
        if (isInLayout) {
            addButton.setOnClickListener { positiveBlock(nameView.editText?.text?.toString()!!) }
        }
    }

    fun onNegative(block: () -> Unit) {
        negativeBlock = block
        if (isInLayout) {
            cancelButton.setOnClickListener { negativeBlock() }
        }
    }

    companion object {
        private const val TAG = "UserFragment"
    }
}