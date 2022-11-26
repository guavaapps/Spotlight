package com.guavaapps.spotlight

import android.animation.Animator
import com.guavaapps.components.Components.getPx
import androidx.fragment.app.FragmentContainerView
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View.OnLayoutChangeListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModelProvider
import android.graphics.drawable.ColorDrawable
import android.animation.ValueAnimator
import android.view.MotionEvent
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.animation.AnimatorListenerAdapter
import android.graphics.Color
import android.view.View
import android.widget.ImageView
import androidx.core.graphics.Insets
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import java.lang.Exception
import java.util.*

class TrackFragment : Fragment() {
    private val viewModel: ContentViewModel by viewModels { ContentViewModel.Factory }
    private var mInsets: Insets? = null
    private var mTrackLargeContainer: FragmentContainerView? = null
    private var mTrackSmallContainer: FragmentContainerView? = null
    private var mTrackViewSize = 0
    private val mTracks: Queue<TrackWrapper> = LinkedList()

    //    private ImageView mUserView;
    private var mTrackView: ImageView? = null
    private var mNextTrackView: ImageView? = null
    private var mPlaylistView: ImageView? = null
    private var mHasTrack = true
    private var mOnTouchListener: OnTouchListener? = null
    private val mTrackLargeFragment = TrackLargeFragment()
    private val mTrackSmallFragment = TrackSmallFragment()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_track, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mTrackLargeContainer = view.findViewById(R.id.track_large_container)
        mTrackSmallContainer = view.findViewById(R.id.track_small_container)
        mTrackView = view.findViewById(R.id.track_view)
        mNextTrackView = view.findViewById(R.id.next_track_view)
        mPlaylistView = view.findViewById(R.id.playlist_view)

        view.addOnLayoutChangeListener(object : OnLayoutChangeListener {
            override fun onLayoutChange(
                view: View,
                left: Int,
                top: Int,
                right: Int,
                bottom: Int,
                oldLeft: Int,
                oldTop: Int,
                oldRight: Int,
                oldBottom: Int,
            ) {
                val viewWidth = view.width
                val viewHeight = view.height
                ViewCompat.setOnApplyWindowInsetsListener(view) { v: View?, windowInsetsCompat: WindowInsetsCompat ->
                    mInsets = windowInsetsCompat.getInsets(WindowInsetsCompat.Type.systemBars())
                    initViews(viewWidth, viewHeight)
                    ViewCompat.setOnApplyWindowInsetsListener(v!!, null)
                    WindowInsetsCompat.CONSUMED
                }
                view.removeOnLayoutChangeListener(this)
            }
        })
    }

    override fun onResume() {
        super.onResume()
    }

    fun initViews(viewWidth: Int, viewHeight: Int) {
//        mPeekViewWidth = mPeekView.getWidth ();
//        mPeekViewHeight = mPeekView.getHeight ();

//        int rawSize = 2 * (viewHeight / 2 - mPeekViewHeight
//                - Components.INSTANCE.getPx (getContext (), 48) // padding
//                - mInsets.bottom);
        val rawSize = 100000
        mTrackViewSize =
            if (rawSize < getPx(requireContext(), MAX_TRACK_VIEW_SIZE_DP)) rawSize else getPx(
                requireContext(), MAX_TRACK_VIEW_SIZE_DP)

//        mPeekViewSmallWidth = viewWidth - Components.INSTANCE.getPx (getContext (), 128) - Components.INSTANCE.getPx (getContext (), 48);

//        mUserView.setX (viewWidth - mUserView.getWidth () - Components.INSTANCE.getPx (getContext (), 24));
//        mUserView.setY (Components.INSTANCE.getPx (getContext (), 24) + mInsets.top);
//        mUserView.setOnClickListener (v -> {
//
//        });
//        mUserView.setOutlineProvider (new ViewOutlineProvider () {
//            @Override
//            public void getOutline (View view, Outline outline) {
//                mUserView.setClipToOutline (true);
//                outline.setOval (0,
//                        0,
//                        view.getWidth (),
//                        view.getHeight ()
//                );
//            }
//        });
        mTrackView!!.layoutParams = ConstraintLayout.LayoutParams(mTrackViewSize, mTrackViewSize)
        mNextTrackView!!.layoutParams =
            ConstraintLayout.LayoutParams(mTrackViewSize, mTrackViewSize)
        mTrackSmallContainer!!.layoutParams = ConstraintLayout.LayoutParams(
            viewWidth - getPx(requireContext(), 128 + 48 + 24),
            -2
        )
        try { // TODO init in layout
            val fragments = requireActivity().supportFragmentManager.fragments
            var s = false
            var l = false
            for (f in fragments) {
                if (f.javaClass == TrackSmallFragment::class.java) s = true
                if (f.javaClass == TrackLargeFragment::class.java) l = true
            }
            if (s || l) throw Exception()

//            requireActivity ().getSupportFragmentManager ().beginTransaction ()
//                    .add (mTrackLargeContainer.getId (), mTrackLargeFragment)
//                    .commit ();
//
//            requireActivity ().getSupportFragmentManager ().beginTransaction ()
//                    .add (mTrackSmallContainer.getId (), mTrackSmallFragment)
//                    .commit ();
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mTrackSmallContainer!!.x = getPx(requireContext(), 128 + 48).toFloat()
        val y = (getPx(requireContext(), 128) - mTrackSmallContainer!!.height) / 2 + mInsets!!.top
        mTrackSmallContainer!!.y = y.toFloat()
        mTrackView!!.x = (viewWidth / 2 - mTrackViewSize / 2).toFloat()
        mTrackView!!.y = (viewHeight / 2 - mTrackViewSize / 2).toFloat()
        mOnTouchListener = OnTouchListener(viewWidth, viewHeight, mNextTrackView!!)
        mTrackView!!.setOnTouchListener(mOnTouchListener)
        mNextTrackView!!.x = (viewWidth / 2 - mTrackViewSize / 2).toFloat()
        mNextTrackView!!.y = (viewHeight / 2 - mTrackViewSize / 2).toFloat()
        mNextTrackView!!.scaleX = 0.5f
        mNextTrackView!!.scaleY = 0.5f
        mNextTrackView!!.alpha = 0.5f
        viewModel.user.observe(viewLifecycleOwner) { userWrapper -> }
        viewModel.track.observe(viewLifecycleOwner) { trackWrapper: TrackWrapper? ->
            if (trackWrapper != null) {
                mTrackView!!.setImageBitmap(trackWrapper.thumbnail)
                //nextTrack (trackWrapper);
                mHasTrack = true
            } else {
                val drawable = ColorDrawable()
                drawable.color = Color.MAGENTA
                mTrackView!!.setImageDrawable(drawable)
                mHasTrack = false
            }
        }
        viewModel!!.nextTrack.observe(viewLifecycleOwner, Observer { trackWrapper: TrackWrapper? ->
            if (trackWrapper != null) {
                mNextTrackView!!.setImageBitmap(trackWrapper.thumbnail)
            } else {
                val drawable = ColorDrawable()
                drawable.color = Color.MAGENTA
                mTrackView!!.setImageDrawable(drawable)
            }
        })
    }

    fun resizeX(offset: Float) {
        val startX = (requireView().width / 2 - mTrackView!!.width / 2).toFloat()
        val endX = getPx(requireContext(), 24).toFloat()
        val d = endX - startX
        val p = offset * d
        val newX = startX + p
        mTrackView!!.x = newX
    }

    fun resizeY(offset: Float) {
        val startY = (requireView().height / 2 - mTrackView!!.height / 2).toFloat()
        val endY = (getPx(requireContext(), 24) + mInsets!!.top).toFloat()
        val d = endY - startY
        val p = offset * d
        val newY = startY + p
        mTrackView!!.y = newY
    }

    fun resizeScale(offset: Float) {
        val start = mTrackViewSize.toFloat()
        val end = getPx(requireContext(), 128).toFloat()
        val d = end - start
        val p = offset * d
        val newSize = (start + p).toInt()
        val params = mTrackView!!.layoutParams
        params.width = newSize
        params.height = newSize
        mTrackView!!.layoutParams = params
    }

    fun alphaSmall(offset: Float) {
        if (offset >= 0.5f) mTrackSmallContainer!!.visibility =
            View.VISIBLE else mTrackSmallContainer!!.visibility = View.GONE
        val start = 0f
        val end = 1f
        val d = end - start
        val o: Float = if (offset >= 0.5f) (offset - 0.5f) / 0.5f else 0f
        val p = o * d
        val newSize = start + p
        mTrackSmallContainer!!.alpha = newSize
    }

    fun alphaLarge(offset: Float) {
        if (offset <= 0.5f) mTrackLargeContainer!!.visibility =
            View.VISIBLE else mTrackLargeContainer!!.visibility = View.GONE
        val start = 1f
        val end = 0f
        val d = end - start
        val o: Float = if (offset <= 0.5f) offset / 0.5f else 1f
        val p1 = offset * d
        val p2 = o * d
        val newSize1 = start + p1
        val newSize2 = start + p2
        mTrackLargeContainer!!.alpha = newSize2
    }

    fun resize(offset: Float) {
        if (offset == 0f) mNextTrackView!!.visibility =
            View.VISIBLE else mNextTrackView!!.visibility = View.GONE
        resizeX(offset)
        resizeY(offset)
        resizeScale(offset)
        alphaSmall(offset)
        alphaLarge(offset)
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private val isWaiting = false

    // media
    fun setTrack(wrappedTrack: TrackWrapper?) {}

    //    private ColorSet createColorSet (Bitmap bitmap) {
    //        ColorSet colorSet = new ColorSet ();
    //
    //        int color = Palette.from (bitmap).generate ()
    //                .getDominantSwatch ()
    //                .getRgb ();
    //
    //        Hct surfaceColor = Hct.fromInt (color);
    //        surfaceColor.setTone (10);
    //        colorSet.surface = surfaceColor.toInt ();
    //
    //        Hct primaryColor = Hct.fromInt (color);
    //        primaryColor.setTone (90);
    //        colorSet.primary = primaryColor.toInt ();
    //
    //        Argb c = Argb.from (primaryColor.toInt ());
    //        c.setAlpha (0.6f * 255);
    //        colorSet.secondary = c.toInt ();
    //
    //        c.setAlpha (0.24f * 255);
    //        colorSet.tertiary = c.toInt ();
    //
    //        c.setAlpha (0.16f * 255);
    //        colorSet.ripple = c.toInt ();
    //
    //        return colorSet;
    //    }
    private inner class OnTouchListener(
        private val mViewWidth: Int,
        private val mViewHeight: Int,
        private val mNextTrackView: ImageView,
    ) : View.OnTouchListener {
        private val mPeekBound: Int
        private val mDismissBound: Int
        private val mMaxSx: Int
        private val mMaxSy: Int
        private val mMaxI: Int
        private var mX = 0f
        private var mY = 0f
        private var mTx = 0f
        private var mTy = 0f
        private var mDx = 0f
        private var mDy = 0f
        private var mSx = 0f
        private var mSy = 0f
        private var mPx = 0f
        private var mIx = 0f
        private val mIy = 0f
        private var mIdleLock: Any? = null
        private var mPeekLock: Any? = Any()
        private var mDismissLock: Any? = Any()
        private var mRxAnimator: ValueAnimator? = null
        private var mIxAnimator: ValueAnimator? = null
        private var mNextRxAnimator: ValueAnimator? = null
        private var mXAnimator: ValueAnimator? = null
        private var mYAnimator: ValueAnimator? = null
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            if (event.action == MotionEvent.ACTION_DOWN) {
//                mParent.setUserInputEnabled (false);
                view!!.parent.requestDisallowInterceptTouchEvent(true)
                if (mXAnimator != null && mXAnimator!!.isRunning) mXAnimator!!.cancel()
                if (mYAnimator != null && mYAnimator!!.isRunning) mYAnimator!!.cancel()
                mX = v.x
                mY = v.y
                mTx = event.rawX
                mTy = event.rawY
                mDx = v.x - event.rawX
                mDy = v.y - event.rawY
            }
            if (event.action == MotionEvent.ACTION_MOVE) {
                mSy = (event.rawY - mTy) / mViewHeight * mMaxSy
                mSx = (event.rawX - mTx) / mViewHeight * mMaxSy
                if (mHasTrack) {
                    mPx =
                        mViewWidth - (v.x + v.width / 2 - mViewWidth / 2) / mViewWidth / 2 * getPx(
                            requireContext(), 256)
                    if (event.rawX + mDx + v.width / 2 > mDismissBound && event.rawX + mDx + v.width / 2 < mPeekBound) {
                        if (mIdleLock == null) {
                            // onIdle
                            mIdleLock = Any() // lock peek
                            mPeekLock = null // unlock peek
                            mDismissLock = null
                            inflateX(0f)
                            resize(v, 1f)
                        }
                        mX = event.rawX + mDx + mIx
                    }
                    if (event.rawX + mDx + v.width / 2 >= mPeekBound) {
                        mSx = (event.rawX + mDx + v.width / 2 - mPeekBound) / mViewWidth * mMaxSx
                        if (mPeekLock == null) {
                            // onPeek
                            mPeekLock = Any() // lock peek
                            mIdleLock = null
                            mDismissLock = null
                            inflateX(mMaxI.toFloat())
                            resize(v, 0.5f)
                        }
                        mX = mPeekBound - v.width / 2 + mIx + mSx
                    }
                    if (event.rawX + mDx + v.width / 2 <= mDismissBound) {
                        mSx = (event.rawX + mDx + v.width / 2 - mDismissBound) / mViewWidth * mMaxSx
                        if (mDismissLock == null) {
                            // onDismiss
                            mDismissLock = Any()
                            mIdleLock = null
                            mPeekLock = null
                            inflateX(-mMaxI.toFloat())
                            resize(v, 0.5f)
                        }
                        mX = mDismissBound - v.width / 2 + mIx
                    }
                }
                mX = if (mHasTrack) mX else mViewWidth / 2 - v.width / 2 + mSx
                v.x = mX
                mY = mViewHeight / 2 - v.height / 2 + mSy
                v.y = mY
                val s = Math.abs(mViewWidth / 2 - mX - v.width / 2) / (mViewWidth / 2) * 0.25f
                mNextTrackView.scaleX = 0.5f + s
                mNextTrackView.scaleY = 0.5f + s
                mNextTrackView.alpha = 0.5f + s
                mPlaylistView!!.x = mPx
            }
            if (event.action == MotionEvent.ACTION_UP) {
//                mParent.setUserInputEnabled (true);
                v.parent.requestDisallowInterceptTouchEvent(false)
                reset(v)
                resize(v, 1f)
                if (mHasTrack) {
                    if (event.rawX + mDx + v.width / 2 >= mPeekBound) {
                        deflateTrack(v)
                    }
                    if (event.rawX + mDx + v.width / 2 <= mDismissBound) {
                        deflateTrack(v)
                    }
                }
            }
            return true
        }

        private fun reset(v: View) {
            val newX = mViewWidth / 2 - v.width / 2
            val newY = mViewHeight / 2 - v.height / 2
            if (mXAnimator != null && mXAnimator!!.isRunning) mXAnimator!!.cancel()
            mXAnimator = ValueAnimator.ofFloat(mX, newX.toFloat())
            mXAnimator!!.setDuration(resources.getInteger(android.R.integer.config_shortAnimTime)
                .toLong())
            mXAnimator!!.addUpdateListener(AnimatorUpdateListener { animation: ValueAnimator ->
                mX = animation.animatedValue as Float
                mPx = mViewWidth - (mX + v.width / 2 - mViewWidth / 2) / mViewWidth / 2 * getPx(
                    requireContext(), 256)
                v.x = mX
                mPlaylistView!!.x = mPx
            })
            if (mYAnimator != null && mYAnimator!!.isRunning) mYAnimator!!.cancel()
            mYAnimator = ValueAnimator.ofFloat(mY, newY.toFloat())
            mYAnimator!!.setDuration(resources.getInteger(android.R.integer.config_shortAnimTime)
                .toLong())
            mYAnimator!!.addUpdateListener(AnimatorUpdateListener { animation: ValueAnimator ->
                mY = animation.animatedValue as Float
                v.y = mY
            })
            mXAnimator!!.start()
            mYAnimator!!.start()
        }

        private fun resize(v: View, newR: Float) {
            if (mRxAnimator != null && mRxAnimator!!.isRunning) mRxAnimator!!.cancel()
            mRxAnimator = ValueAnimator.ofFloat(v.scaleX, newR)
            mRxAnimator!!.setDuration(resources.getInteger(android.R.integer.config_shortAnimTime)
                .toLong())
            mRxAnimator!!.addUpdateListener(AnimatorUpdateListener { animation: ValueAnimator ->
                val r = animation.animatedValue as Float
                v.scaleX = r
                v.scaleY = r
            })
            mRxAnimator!!.start()
        }

        private fun inflateX(newIx: Float) {
            if (mIxAnimator != null && mIxAnimator!!.isRunning) mIxAnimator!!.cancel()
            mIxAnimator = ValueAnimator.ofFloat(mIx, newIx)
            mIxAnimator!!.setDuration(resources.getInteger(android.R.integer.config_shortAnimTime)
                .toLong())
            mIxAnimator!!.addUpdateListener(AnimatorUpdateListener { animation ->
                mIx = animation.animatedValue as Float
            })
            mIxAnimator!!.start()
        }

        fun inflateNext() {
            mNextTrackView.animate().alpha(1f)
                .setDuration(resources.getInteger(android.R.integer.config_shortAnimTime).toLong())
                .start()
            if (mNextRxAnimator != null && mNextRxAnimator!!.isRunning) mNextRxAnimator!!.cancel()
            mNextRxAnimator = ValueAnimator.ofFloat(mNextTrackView.scaleX, 1f)
            mNextRxAnimator!!.setDuration(resources.getInteger(android.R.integer.config_shortAnimTime)
                .toLong())
            mNextRxAnimator!!.addUpdateListener(AnimatorUpdateListener { animation: ValueAnimator ->
                val r = animation.animatedValue as Float
                mNextTrackView.scaleX = r
                mNextTrackView.scaleY = r
            })
            mNextRxAnimator!!.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    val newX = view!!.width / 2 - mTrackView!!.width / 2
                    val newY = view!!.height / 2 - mTrackView!!.height / 2
                    viewModel.getNext()
                    mTrackView!!.x = newX.toFloat()
                    mTrackView!!.y = newY.toFloat()
                    mTrackView!!.scaleX = 1f
                    mTrackView!!.scaleY = 1f
                    mTrackView!!.alpha = 1f
                }
            })
            mNextRxAnimator!!.start()
        }

        fun deflateTrack(v: View) {
            mXAnimator!!.cancel()
            mYAnimator!!.cancel()
            mIxAnimator!!.cancel()
            v.animate()
                .alpha(0f)
                .setDuration(resources.getInteger(android.R.integer.config_shortAnimTime).toLong())
                .start()
            if (mRxAnimator != null && mRxAnimator!!.isRunning) mRxAnimator!!.cancel()
            mRxAnimator = ValueAnimator.ofFloat(v.scaleX, 0f)
            mRxAnimator!!.setDuration(resources.getInteger(android.R.integer.config_shortAnimTime)
                .toLong())
            mRxAnimator!!.addUpdateListener(AnimatorUpdateListener { animation: ValueAnimator ->
                val r = animation.animatedValue as Float
                v.scaleX = r
                v.scaleY = r
            })
            mRxAnimator!!.start()
            inflateNext()
        }

        private var hasCancellableImpendingEvents = true
        private var isLastPulse = false
        private val `in` =
            resources.getInteger(com.google.android.material.R.integer.material_motion_duration_medium_2)
        private val out =
            resources.getInteger(com.google.android.material.R.integer.material_motion_duration_medium_1)
        private val wait =
            4 * resources.getInteger(com.google.android.material.R.integer.material_motion_duration_long_2)
        private val d = (`in` + out + wait).toLong()
        private fun pulse() {
            pulseOut(mTrackView)
        }

        private fun cancelImpedingPulseEvents() {
            isLastPulse = true
            if (hasCancellableImpendingEvents) {
                if (mRxAnimator != null) mRxAnimator!!.cancel()
                //                hasCancellableImpendingEvents = false;
            }
        }

        private fun pulseOut(v: View?) {
//            mXAnimator.cancel ();
//            mYAnimator.cancel ();
//            mIxAnimator.cancel ();

//            if (mRxAnimator != null && mRxAnimator.isRunning ()) mRxAnimator.cancel ();
            mRxAnimator = ValueAnimator.ofFloat(v!!.scaleX, 1.1f)
            mRxAnimator!!.setDuration(out.toLong())
            mRxAnimator!!.addUpdateListener(AnimatorUpdateListener { animation: ValueAnimator ->
                val r = animation.animatedValue as Float
                v.scaleX = r
                v.scaleY = r
            })
            mRxAnimator!!.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    super.onAnimationStart(animation)
                    hasCancellableImpendingEvents = false
                }

                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    pulseIn(v)
                }

                override fun onAnimationCancel(animation: Animator) {
                    super.onAnimationCancel(animation)
                }
            })
            mRxAnimator!!.setStartDelay(wait.toLong())
            mRxAnimator!!.start()
        }

        private fun pulseIn(v: View?) {
//            mXAnimator.cancel ();
//            mYAnimator.cancel ();
//            mIxAnimator.cancel ();

//            if (mRxAnimator != null && mRxAnimator.isRunning ()) mRxAnimator.cancel ();
            mRxAnimator = ValueAnimator.ofFloat(v!!.scaleX, 1f)
            mRxAnimator!!.setDuration(`in`.toLong())
            mRxAnimator!!.addUpdateListener(AnimatorUpdateListener { animation: ValueAnimator ->
                val r = animation.animatedValue as Float
                v.scaleX = r
                v.scaleY = r
            })
            mRxAnimator!!.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationCancel(animation: Animator) {
                    super.onAnimationCancel(animation)
                }

                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    if (isLastPulse) {
                        isLastPulse = false
                        // update ui asRealmObject show new track
                    } else pulseOut(v)
                }
            })
            mRxAnimator!!.start()
        }

        private val TAG = "OTL"

        init {
            mPeekBound =
                mViewWidth - (mViewWidth - getPx(requireContext(),
                    256)) / 2 //(int) (0.75f * viewWidth);
            mDismissBound = (mViewWidth - getPx(requireContext(), 256)) / 2
            mMaxSx = getPx(requireContext(), 64)
            mMaxSy = getPx(requireContext(), 256)
            mMaxI = mViewWidth - getPx(requireContext(), 384)
        }
    }

    companion object {
        private const val MAX_TRACK_VIEW_SIZE_DP = 256

        //media
        private const val TAG = "TrackFragment"
    }
}