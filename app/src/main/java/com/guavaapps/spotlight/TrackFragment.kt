package com.guavaapps.spotlight

import android.animation.Animator
import com.guavaapps.components.Components.getPx
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View.OnLayoutChangeListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.constraintlayout.widget.ConstraintLayout
import android.graphics.drawable.ColorDrawable
import android.animation.ValueAnimator
import android.view.MotionEvent
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.graphics.Color
import android.util.Log
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import androidx.core.graphics.Insets
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import java.lang.Exception
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import androidx.fragment.app.FragmentContainerView as FragmentContainerView

class TrackFragment : Fragment(R.layout.fragment_track) {
    private val viewModel: ContentViewModel by activityViewModels { ContentViewModel.Factory }

    private var insets: Insets? = null

    private lateinit var trackLargeContainer: FragmentContainerView
    private lateinit var trackSmallContainer: FragmentContainerView

    private var trackViewSize = 0

    private lateinit var trackView: ImageView
    private lateinit var nextTrackView: ImageView
    private lateinit var playlistView: ImageView

    @Deprecated("Use OnTouchListener.lock() and unlock()")
    private var hasTrack = true
    private var trackViewOnTouchListener: OnTouchListener? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        trackLargeContainer = view.findViewById(R.id.track_large_container)
        trackSmallContainer = view.findViewById(R.id.track_small_container)
        trackView = view.findViewById(R.id.track_view)
        nextTrackView = view.findViewById(R.id.next_track_view)
        playlistView = view.findViewById(R.id.playlist_view)

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
                    insets = windowInsetsCompat.getInsets(WindowInsetsCompat.Type.systemBars())

                    initViews(viewWidth, viewHeight)

                    ViewCompat.setOnApplyWindowInsetsListener(v!!, null)
                    WindowInsetsCompat.CONSUMED
                }

                view.removeOnLayoutChangeListener(this)
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()

        Log.e(TAG, "onDestroyView")
    }

    override fun onDestroy() {
        super.onDestroy()

        Log.e(TAG, "onDestroy")
    }

    @SuppressLint("ClickableViewAccessibility")
    fun initViews(viewWidth: Int, viewHeight: Int) {
        val rawSize = 100000

        trackViewOnTouchListener = OnTouchListener(viewWidth, viewHeight)

        trackViewSize = if (rawSize < getPx(requireContext(), MAX_TRACK_VIEW_SIZE_DP)) rawSize
        else getPx(requireContext(), MAX_TRACK_VIEW_SIZE_DP)

        trackView.layoutParams = ConstraintLayout.LayoutParams(trackViewSize, trackViewSize)
        nextTrackView.layoutParams = ConstraintLayout.LayoutParams(trackViewSize, trackViewSize)

        trackSmallContainer.layoutParams = ConstraintLayout.LayoutParams(
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
        } catch (e: Exception) {
            e.printStackTrace()
        }

//        val y = (getPx(requireContext(), 128) - trackSmallContainer.height) / 2 + insets!!.top
        trackSmallContainer.x = getPx(requireContext(), 128 + 48).toFloat()
        trackSmallContainer.y = ((getPx(requireContext(),
            128) - trackSmallContainer.height) / 2 + insets!!.top).toFloat()

        trackView.x = (viewWidth / 2 - trackViewSize / 2).toFloat()
        trackView.y = (viewHeight / 2 - trackViewSize / 2).toFloat()
        trackView.setOnTouchListener(trackViewOnTouchListener)

        nextTrackView.x = (viewWidth / 2 - trackViewSize / 2).toFloat()
        nextTrackView.y = (viewHeight / 2 - trackViewSize / 2).toFloat()
        nextTrackView.scaleX = 0.5f
        nextTrackView.scaleY = 0.5f
        nextTrackView.alpha = 0.5f

        viewModel.user.observe(viewLifecycleOwner) { userWrapper -> }

        viewModel.track.observe(viewLifecycleOwner) { trackWrapper: TrackWrapper? ->
            if (trackWrapper != null) {
                trackView.setImageBitmap(trackWrapper.thumbnail)
                hasTrack = true
                trackViewOnTouchListener?.unlock()
            } else {
                val drawable = ColorDrawable()
                drawable.color = Color.GRAY
                trackView.setImageDrawable(drawable)
                hasTrack = false
                trackViewOnTouchListener?.lock()
                //trackViewOnTouchListener?.doLoadingAnim()
            }
        }

        viewModel.nextTrack.observe(viewLifecycleOwner) { trackWrapper: TrackWrapper? ->
            if (trackWrapper != null) {
                nextTrackView.setImageBitmap(trackWrapper.thumbnail)
            } else {
                val drawable = ColorDrawable()
                drawable.color = Color.TRANSPARENT
                nextTrackView.setImageDrawable(drawable)
            }
        }
    }

    private fun resizeX(offset: Float) {
        val startX = (requireView().width / 2 - trackView!!.width / 2).toFloat()
        val endX = getPx(requireContext(), 24).toFloat()
        val d = endX - startX
        val p = offset * d
        val newX = startX + p
        trackView!!.x = newX
    }

    private fun resizeY(offset: Float) {
        val startY = (requireView().height / 2 - trackView!!.height / 2).toFloat()
        val endY = (getPx(requireContext(), 24) + insets!!.top).toFloat()
        val d = endY - startY
        val p = offset * d
        val newY = startY + p
        trackView!!.y = newY
    }

    private fun resizeScale(offset: Float) {
        val start = trackViewSize.toFloat()
        val end = getPx(requireContext(), 128).toFloat()
        val d = end - start
        val p = offset * d
        val newSize = (start + p).toInt()
        val params = trackView!!.layoutParams

        params.width = newSize
        params.height = newSize
        trackView!!.layoutParams = params
    }

    private fun alphaSmall(offset: Float) {
        if (offset >= 0.5f) trackSmallContainer!!.visibility =
            View.VISIBLE else trackSmallContainer!!.visibility = View.GONE
        val start = 0f
        val end = 1f
        val d = end - start
        val o: Float = if (offset >= 0.5f) (offset - 0.5f) / 0.5f else 0f
        val p = o * d
        val newSize = start + p
        trackSmallContainer!!.alpha = newSize
    }

    private fun alphaLarge(offset: Float) {
        if (offset <= 0.5f) trackLargeContainer!!.visibility =
            View.VISIBLE else trackLargeContainer!!.visibility = View.GONE
        val start = 1f
        val end = 0f
        val d = end - start
        val o: Float = if (offset <= 0.5f) offset / 0.5f else 1f
        val p1 = offset * d
        val p2 = o * d
        val newSize1 = start + p1
        val newSize2 = start + p2
        trackLargeContainer!!.alpha = newSize2
    }

    fun resize(offset: Float) {
        if (offset == 0f) nextTrackView.visibility = View.VISIBLE
        else nextTrackView.visibility = View.GONE

        resizeX(offset)
        resizeY(offset)
        resizeScale(offset)
        alphaSmall(offset)
        alphaLarge(offset)
    }

    // TODO DONT TOUCH AT ALL COSTS THIS TOOK SOME MUCH TIME TO DO
    private inner class OnTouchListener(
        private val mViewWidth: Int,
        private val mViewHeight: Int,
    ) : View.OnTouchListener {
        private val mPeekBound: Int
        private val mDismissBound: Int
        private val mMaxSx: Int
        private val mMaxSy: Int
        private val mMaxI: Int

        private var hasTrack = false

        init {
            mPeekBound =
                mViewWidth - (mViewWidth - getPx(requireContext(),
                    256)) / 2 //(int) (0.75f * viewWidth);
            mDismissBound = (mViewWidth - getPx(requireContext(), 256)) / 2
            mMaxSx = getPx(requireContext(), 64)
            mMaxSy = getPx(requireContext(), 256)
            mMaxI = mViewWidth - getPx(requireContext(), 384)
        }

        private var mX = 0f // view
        private var mY = 0f

        private var mTx = 0f // touch
        private var mTy = 0f

        private var mDx = 0f // touch - view
        private var mDy = 0f

        private var mSx = 0f // stretch
        private var mSy = 0f

        private var mPx = 0f

        private var mIx = 0f // inflation
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

                // if !hastrack x = view.x + s // stretch only
                // else x = ~touch.x + s //
                if (hasTrack) {
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
                mX = if (hasTrack) mX else mViewWidth / 2 - v.width / 2 + mSx
                v.x = mX
                mY = mViewHeight / 2 - v.height / 2 + mSy
                v.y = mY
                val s = Math.abs(mViewWidth / 2 - mX - v.width / 2) / (mViewWidth / 2) * 0.25f
                nextTrackView.scaleX = 0.5f + s
                nextTrackView.scaleY = 0.5f + s
                nextTrackView.alpha = 0.5f + s
                playlistView!!.x = mPx
            }
            if (event.action == MotionEvent.ACTION_UP) {
//                mParent.setUserInputEnabled (true);
                v.parent.requestDisallowInterceptTouchEvent(false)
                reset(v)
                resize(v, 1f)
                if (hasTrack) {
                    if (event.rawX + mDx + v.width / 2 >= mPeekBound) {
                        deflateTrack()
                        inflateNext()
                    }
                    if (event.rawX + mDx + v.width / 2 <= mDismissBound) {
                        deflateTrack()
                        inflateNext()
                    }
                }
            }
            return true
        }

        fun lock() {
            hasTrack = false
        }

        fun unlock() {
            hasTrack = true
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
                playlistView!!.x = mPx
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
            mIxAnimator!!.addUpdateListener { animation ->
                mIx = animation.animatedValue as Float
            }
            mIxAnimator!!.start()
        }

        private fun inflateNext() {
            nextTrackView.animate().alpha(1f)
                .setDuration(resources.getInteger(android.R.integer.config_shortAnimTime).toLong())
                .start()
            if (mNextRxAnimator != null && mNextRxAnimator!!.isRunning) mNextRxAnimator!!.cancel()
            mNextRxAnimator = ValueAnimator.ofFloat(nextTrackView.scaleX, 1f)
            mNextRxAnimator!!.interpolator = OvershootInterpolator()
            mNextRxAnimator!!.setDuration(resources.getInteger(com.google.android.material.R.integer.material_motion_duration_long_2)
                .toLong())
            mNextRxAnimator!!.addUpdateListener { animation: ValueAnimator ->
                val r = animation.animatedValue as Float
                nextTrackView.scaleX = r
                nextTrackView.scaleY = r
            }
            mNextRxAnimator!!.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    val newX = view!!.width / 2 - trackView!!.width / 2
                    val newY = view!!.height / 2 - trackView!!.height / 2
                    viewModel.getNext()
                    trackView!!.x = newX.toFloat()
                    trackView!!.y = newY.toFloat()
                    if (viewModel.nextTrack.value != null) {
                        trackView!!.scaleX = 1f
                        trackView!!.scaleY = 1f
                        trackView!!.alpha = 1f
                    } else blankAnim()
                }
            })
            mNextRxAnimator!!.start()
        }

        private fun deflateTrack() {
            mXAnimator!!.cancel()
            mYAnimator!!.cancel()
            mIxAnimator!!.cancel()
            trackView.animate()
                .alpha(0f)
                .setDuration(resources.getInteger(android.R.integer.config_shortAnimTime).toLong())
                .start()
            if (mRxAnimator != null && mRxAnimator!!.isRunning) mRxAnimator!!.cancel()
            mRxAnimator = ValueAnimator.ofFloat(trackView.scaleX, 0f)
            mRxAnimator!!.setDuration(resources.getInteger(android.R.integer.config_shortAnimTime)
                .toLong())
            mRxAnimator!!.addUpdateListener(AnimatorUpdateListener { animation: ValueAnimator ->
                val r = animation.animatedValue as Float
                trackView.scaleX = r
                trackView.scaleY = r
            })
            mRxAnimator!!.start()
        }

        private fun blankAnim() {
            trackView.scaleX = 0.5f
            trackView.scaleY = 0.5f
            trackView.alpha = 0f
            trackView.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(resources.getInteger(com.google.android.material.R.integer.material_motion_duration_long_2)
                    .toLong())
                .setInterpolator(OvershootInterpolator())
                .start()
        }

        private fun doublePulseLoadingAnim() {
            trackView.animate()
                .scaleX(1.25f)
                .scaleY(1.25f)
                .setDuration(com.google.android.material.R.integer.material_motion_duration_long_2.toLong())
                .setInterpolator(OvershootInterpolator())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator?) {
                        trackView.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(com.google.android.material.R.integer.material_motion_duration_long_2.toLong())
                            .setInterpolator(OvershootInterpolator())
                            .start()
                    }
                })
                .start()
        }

        fun doLoadingAnim() {
            Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate({
                    doublePulseLoadingAnim()
                }, 1000, 1000, TimeUnit.MILLISECONDS)
        }

        fun cancelLoadingAnim() {
            trackView.clearAnimation()
        }
    }

    companion object {
        private const val MAX_TRACK_VIEW_SIZE_DP = 256

        private const val TAG = "TrackFragment"
    }
}