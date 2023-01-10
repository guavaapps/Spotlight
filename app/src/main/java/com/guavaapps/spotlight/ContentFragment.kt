package com.guavaapps.spotlight

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.transition.Fade
import android.transition.Visibility.MODE_OUT
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import androidx.core.view.*
import androidx.fragment.app.*
import androidx.navigation.NavController
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.NavHostFragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.google.android.material.button.MaterialButton
import com.google.android.material.transition.*
import com.guavaapps.components.Components.getPx
import com.guavaapps.components.color.Hct
import com.pixel.spotifyapi.Objects.Track
import kotlinx.coroutines.awaitAll
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import android.widget.ImageView as ImageView
import com.guavaapps.spotlight.UserFragment as UserFragment

private const val TAG = "ContentFragment"

// TODO content fragment resize trackFragment on page changed to 1
// TODO trackFragment.resize() - trackView.post -> apply layout params

class ContentFragment : Fragment(R.layout.fragment_content) {
    private val viewModel: ContentViewModel by activityViewModels { ContentViewModel.Factory }

    private lateinit var pager: ViewPager2
    private lateinit var track: FragmentContainerView

    private lateinit var userFragment: UserFragment
    private lateinit var trackFragment: TrackFragment
    private lateinit var extraFragment: ExtraFragment

    private lateinit var surfaceView: ImageView
    private lateinit var tempSurfaceView: ImageView
    private lateinit var surfaceViewOverlay: View

    private lateinit var userView: ImageView

    private var colorSet = ColorSet()

    private var surfaceBitmap: Bitmap? = null
    private var navController: NavController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }

        navController = NavHostFragment.findNavController(this)

        pager = view.findViewById(R.id.pager)
        pager.offscreenPageLimit = 1

        track = view.findViewById(R.id.track)

        userFragment = UserFragment()
        extraFragment = ExtraFragment()
        trackFragment = track.getFragment()

        surfaceView = view.findViewById(R.id.surface_view)
        tempSurfaceView = view.findViewById(R.id.temp_surface_view)

        surfaceViewOverlay = view.findViewById(R.id.surface_view_overlay)

        userView = view.findViewById(R.id.user)

        surfaceView.doOnPreDraw {
            val radius = surfaceView.width.toFloat()

            surfaceView.setRenderEffect(
                RenderEffect.createBlurEffect(
                    radius,
                    radius,
                    Shader.TileMode.CLAMP
                )
            )
            tempSurfaceView.setRenderEffect(
                RenderEffect.createBlurEffect(
                    radius,
                    radius,
                    Shader.TileMode.CLAMP
                )
            )
        }
        surfaceView.scaleType = ImageView.ScaleType.CENTER_CROP
        tempSurfaceView.scaleType = ImageView.ScaleType.CENTER_CROP

        pager.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                if (position == 1) {
                }//viewModel.loadAlbum()
            }
        })

        viewModel.album.observe(viewLifecycleOwner) { albumWrapper: AlbumWrapper? ->
            if (albumWrapper?.bitmap != null) {
                applyAlbum(albumWrapper)
            } else {
            }
        }

        viewModel.user.observe(viewLifecycleOwner) { userWrapper: UserWrapper? ->
            if (userWrapper != null) {
                userView.setImageBitmap(userWrapper.bitmap)
            }
        }

        userView.setOnClickListener { v: View? ->
            val trans = "trans"

            val animDuration =
                resources.getInteger(com.google.android.material.R.integer.material_motion_duration_long_2)
                    .toLong()

            exitTransition = MaterialFade().apply {
                mode = MaterialFade.MODE_OUT
                duration = animDuration - 100
            }

            reenterTransition = MaterialFade().apply {
                secondaryAnimatorProvider = null // remove scaler
                mode = MaterialFade.MODE_IN
                duration = animDuration - 100
            }

            val extras = FragmentNavigatorExtras(v!! to trans)
            val directions = ContentFragmentDirections.actionContentFragmentToUserFragment()

//            userView.animate().apply {
//                duration =
//                    resources.getInteger(com.google.android.material.R.integer.material_motion_duration_short_1)
//                        .toLong()
//                alpha(0f)
//            }.start()

            navController!!.navigate(directions, extras)
        }

        view.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                ViewCompat.requestApplyInsets(view)

//                ViewCompat.setOnApplyWindowInsetsListener (view, (v, windowInsetsCompat) -> {
                val insets = ViewCompat.getRootWindowInsets(view)!!
                    .getInsets(WindowInsetsCompat.Type.systemBars()) //windowInsetsCompat.getInsets (WindowInsetsCompat.Type.systemBars ());

//                    return WindowInsetsCompat.CONSUMED;
//                });

                val params = userView.layoutParams as ViewGroup.MarginLayoutParams
                params.apply {
                    topMargin = insets.top + getPx(requireContext(), 24)
                    rightMargin = insets.right + getPx(requireContext(), 24)
                }

                pager.adapter = Adapter(requireActivity())
                pager.registerOnPageChangeCallback(object : OnPageChangeCallback() {
                    private val pOffset = 0f
                    override fun onPageScrolled(
                        position: Int,
                        positionOffset: Float,
                        positionOffsetPixels: Int,
                    ) {
                        super.onPageScrolled(position, positionOffset, positionOffsetPixels)

                        if (position == 0) {
                            try {
                                val rawScale =
                                    if (positionOffset < 0.5f) positionOffset / 0.5f else 1f
                                val scale = 1 - rawScale

                                trackFragment.resize(positionOffset)
                                //mUserView.setAlpha (scale);

//                                ArgbEvaluator argbEvaluator = new ArgbEvaluator ();
//                                Argb c = Argb.from (mColorSet.surface);
//                                c.setAlpha (255);
//                                int argb = (int) argbEvaluator.evaluate (positionOffset, mColorSet.surfaceGradient [0], c.toInt ());
//                                int argb2 = (int) argbEvaluator.evaluate (positionOffset, mColorSet.surfaceGradient [1], c.toInt ());
////                                mSurfaceViewOverlay.setBackgroundColor (argb);
//                                GradientDrawable drawable = new GradientDrawable ();
//                                drawable.setOrientation (GradientDrawable.Orientation.TOP_BOTTOM);
//                                drawable.setColors (new int[] {argb, argb2});
//                                //mSurfaceViewOverlay.setBackground (drawable);
                                surfaceViewOverlay.alpha =
                                    if (positionOffset > 0.16f) positionOffset else 0.16f
                            } catch (e: Exception) {
                            }
                        }
                    }
                })

                ViewCompat.setOnApplyWindowInsetsListener(view, null)
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun applyAlbum(wrappedAlbum: AlbumWrapper) {
        val bitmap = wrappedAlbum.bitmap
        val colorSet = ColorSet.create(bitmap)

        surfaceView.scaleType = ImageView.ScaleType.CENTER_CROP

        val temp = surfaceView.drawable as BitmapDrawable?
        val b = temp?.bitmap

        tempSurfaceView.setImageBitmap(b)
        tempSurfaceView.visibility = View.VISIBLE
        tempSurfaceView.alpha = 1f

        surfaceView.setImageBitmap(bitmap)

        tempSurfaceView.animate()
            .alpha(0f)
            .setDuration(resources.getInteger(android.R.integer.config_shortAnimTime).toLong())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)

                    tempSurfaceView.visibility = View.GONE
                }
            })
            .start()

        surfaceBitmap = bitmap

        val drawable = GradientDrawable()

        val argbAnimator1 = ValueAnimator.ofObject(
            ArgbEvaluator(),
            this.colorSet.surface[0],
            colorSet.surface[0]
        )
        argbAnimator1.duration =
            resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
        argbAnimator1.addUpdateListener { animation: ValueAnimator ->
            this.colorSet.surface[0] = animation.animatedValue as Int
            drawable.colors = this.colorSet.surface
            surfaceViewOverlay.background = drawable
        }
        val argbAnimator2 = ValueAnimator.ofObject(
            ArgbEvaluator(),
            this.colorSet.surface[1],
            colorSet.surface[1]
        )
        argbAnimator2.duration =
            resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
        argbAnimator2.addUpdateListener { animation: ValueAnimator ->
            this.colorSet.surface[1] = animation.animatedValue as Int
            drawable.colors = this.colorSet.surface
            surfaceViewOverlay.background = drawable
        }
        argbAnimator1.start()
        argbAnimator2.start()
        this.colorSet = colorSet
    }

    private inner class Adapter(fragmentActivity: FragmentActivity) :
        FragmentStateAdapter(fragmentActivity) {
        override fun createFragment(position: Int): Fragment {
            if (position == 1) return extraFragment
            return Fragment() /// fkjdkaaaa aaaaaaaaaaahhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh
        }

        override fun getItemCount(): Int {
            return 2
        }
    }
}