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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.ImageView
import androidx.core.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.NavHostFragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback

class ContentFragment : Fragment() {
    private var mViewModel: ContentViewModel? = null

    private var mPager: ViewPager2? = null
    private var track: FragmentContainerView? = null

    private var mUserFragment: UserFragment? = null
    private var mTrackFragment: TrackFragment? = null
    private var mExtraFragment: ExtraFragment? = null

    private var mSurfaceView: ImageView? = null
    private var mTempSurfaceView: ImageView? = null
    private var mSurfaceViewOverlay: View? = null

    private var mUser: ImageView? = null

    private var mColorSet = ColorSet()

    private var mSurfaceBitmap: Bitmap? = null
    private val mParent: FragmentContainerView? = null

    private var mNavController: NavController? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_content, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mViewModel = ViewModelProvider(requireActivity()).get(ContentViewModel::class.java)

        mNavController = NavHostFragment.findNavController(this)

        mPager = view.findViewById(R.id.pager)
        mPager!!.offscreenPageLimit = 1

        track = view.findViewById(R.id.track)

        val navHostFragment = requireActivity().supportFragmentManager
            .findFragmentById(R.id.track) as NavHostFragment?

        mUserFragment = UserFragment()

        mExtraFragment = ExtraFragment()

        mTrackFragment = track!!.getFragment<TrackFragment>()

        mSurfaceView = view.findViewById(R.id.surface_view)
        mTempSurfaceView = view.findViewById(R.id.temp_surface_view)

        mSurfaceViewOverlay = view.findViewById(R.id.surface_view_overlay)

        mUser = view.findViewById(R.id.user)

        mSurfaceView!!.setRenderEffect(
            RenderEffect.createBlurEffect(
                100f,
                100f,
                Shader.TileMode.CLAMP
            )
        )
        mTempSurfaceView!!.setRenderEffect(
            RenderEffect.createBlurEffect(
                100f,
                100f,
                Shader.TileMode.CLAMP
            )
        )
        mSurfaceView!!.scaleType = ImageView.ScaleType.CENTER_CROP
        mTempSurfaceView!!.scaleType = ImageView.ScaleType.CENTER_CROP

        mViewModel!!.album.observe(viewLifecycleOwner) { albumWrapper: AlbumWrapper? ->
            if (albumWrapper != null) {
                nextAlbum(albumWrapper)
            } else {
            }
        }

        mViewModel!!.user.observe(viewLifecycleOwner) { userWrapper: UserWrapper? ->
            if (userWrapper != null) {
                mUser!!.setImageBitmap(userWrapper.thumbnail)
            }
        }

        mUser!!.setOnClickListener { v: View? ->
            val trans = "trans"

            val extras = FragmentNavigatorExtras(v!! to trans)
            val directions = ContentFragmentDirections.actionContentFragmentToUserFragment()

            mUser!!.animate().apply {
                duration = resources.getInteger(com.google.android.material.R.integer.material_motion_duration_short_1).toLong()
                alpha( 0f)
            }.start()

            mNavController!!.navigate(directions, extras)
        }

        view.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val viewWidth = requireView().width
                val viewHeight = requireView().height
                ViewCompat.requestApplyInsets(view)

//                ViewCompat.setOnApplyWindowInsetsListener (view, (v, windowInsetsCompat) -> {
                val insets = ViewCompat.getRootWindowInsets(view)!!
                    .getInsets(WindowInsetsCompat.Type.systemBars()) //windowInsetsCompat.getInsets (WindowInsetsCompat.Type.systemBars ());

//                    return WindowInsetsCompat.CONSUMED;
//                });

                val params = mUser!!.layoutParams as ViewGroup.MarginLayoutParams
                params.apply {
                    topMargin = insets.top;
                }

                mPager!!.adapter = Adapter(requireActivity())
                mPager!!.registerOnPageChangeCallback(object : OnPageChangeCallback() {
                    private val pOffset = 0f
                    override fun onPageScrolled(
                        position: Int,
                        positionOffset: Float,
                        positionOffsetPixels: Int
                    ) {
                        super.onPageScrolled(position, positionOffset, positionOffsetPixels)

                        if (position == 0) {
                            try {
                                val rawScale =
                                    if (positionOffset < 0.5f) positionOffset / 0.5f else 1f
                                val scale = 1 - rawScale

                                mTrackFragment!!.resize(positionOffset)
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
                                mSurfaceViewOverlay!!.alpha =
                                    if (positionOffset > 0.16f) positionOffset else 0.16f
                            } catch (e: Exception) {
                            }
                        }
                    }
                })

                ViewCompat.setOnApplyWindowInsetsListener(view, null)

//                    return windowInsetsCompat;
//                }));
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
    }

    private fun nextAlbum(wrappedAlbum: AlbumWrapper) {
        nextTrack(TrackWrapper(null, wrappedAlbum.bitmap))
    }

    private fun nextTrack(wrappedTrack: TrackWrapper) {
        val bitmap = wrappedTrack.thumbnail
        val colorSet = ColorSet.create(bitmap)

        mSurfaceView!!.scaleType = ImageView.ScaleType.CENTER_CROP

        val temp = mSurfaceView!!.drawable as BitmapDrawable?
        val b = temp?.bitmap

        mTempSurfaceView!!.setImageBitmap(b)
        mTempSurfaceView!!.visibility = View.VISIBLE
        mTempSurfaceView!!.alpha = 1f

        mSurfaceView!!.setImageBitmap(bitmap)

        mTempSurfaceView!!.animate()
            .alpha(0f)
            .setDuration(resources.getInteger(android.R.integer.config_shortAnimTime).toLong())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)

                    mTempSurfaceView!!.visibility = View.GONE
                }
            })
            .start()

        mSurfaceBitmap = bitmap

        val drawable = GradientDrawable()

        val argbAnimator1 = ValueAnimator.ofObject(
            ArgbEvaluator(),
            mColorSet.surface[0],
            colorSet.surface[0]
        )
        argbAnimator1.duration =
            resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
        argbAnimator1.addUpdateListener { animation: ValueAnimator ->
            mColorSet.surface[0] = animation.animatedValue as Int
            drawable.colors = mColorSet.surface
            mSurfaceViewOverlay!!.background = drawable
        }
        val argbAnimator2 = ValueAnimator.ofObject(
            ArgbEvaluator(),
            mColorSet.surface[1],
            colorSet.surface[1]
        )
        argbAnimator2.duration =
            resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
        argbAnimator2.addUpdateListener { animation: ValueAnimator ->
            mColorSet.surface[1] = animation.animatedValue as Int
            drawable.colors = mColorSet.surface
            mSurfaceViewOverlay!!.background = drawable
        }
        argbAnimator1.start()
        argbAnimator2.start()
        mColorSet = colorSet
    }

    private inner class Adapter(fragmentActivity: FragmentActivity) :
        FragmentStateAdapter(fragmentActivity) {
        override fun createFragment(position: Int): Fragment {
            when (position) {
//                0 -> return Fragment()
                1 -> return mExtraFragment!!
            }
            return Fragment() /// fkjdkaaaa aaaaaaaaaaahhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh
        }

        override fun getItemCount(): Int {
            return 2
        }
    }

    companion object {
        private const val TAG = "ContentFragment"
    }
}