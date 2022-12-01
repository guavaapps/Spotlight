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
import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.core.graphics.Insets
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.guavaapps.components.Components
import java.lang.Exception
import java.util.*
import androidx.fragment.app.FragmentContainerView as FragmentContainerView

class TrackFragment : Fragment() {
    private val viewModel: ContentViewModel by activityViewModels { ContentViewModel.Factory }

    private var insets: Insets? = null

    private lateinit var trackLargeContainer: FragmentContainerView
    private lateinit var trackSmallContainer: FragmentContainerView

    private var trackViewSize = 0

    private lateinit var trackView: ImageView
    private lateinit var nextTrackView: ImageView
    private lateinit var playlistView: ImageView

    private var hasTrack = true
    private var onTouchListener: OnTouchListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_track, container, false)
    }

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

    @SuppressLint("ClickableViewAccessibility")
    fun initViews(viewWidth: Int, viewHeight: Int) {
        val rawSize = 100000

        onTouchListener = OnTouchListener(viewWidth, viewHeight, nextTrackView)

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
        trackView.setOnTouchListener(onTouchListener)

        nextTrackView.x = (viewWidth / 2 - trackViewSize / 2).toFloat()
        nextTrackView.y = (viewHeight / 2 - trackViewSize / 2).toFloat()
        nextTrackView.scaleX = 0.5f
        nextTrackView.scaleY = 0.5f
        nextTrackView.alpha = 0.5f

        viewModel.user.observe(viewLifecycleOwner) { userWrapper -> }

        viewModel.getTrack().observe(viewLifecycleOwner) { trackWrapper: TrackWrapper? ->
            if (trackWrapper != null) {
                trackView.setImageBitmap(trackWrapper.thumbnail)
                hasTrack = true
            } else {
                val drawable = ColorDrawable()
                drawable.color = Color.MAGENTA
                trackView.setImageDrawable(drawable)
                hasTrack = false
            }
        }

        viewModel.nextTrack.observe(viewLifecycleOwner) { trackWrapper: TrackWrapper? ->
            if (trackWrapper != null) {
                Log.e(TAG, "nextTrack - ${trackWrapper.track.name} bitmap=${trackWrapper.thumbnail}")
                nextTrackView.setImageBitmap(trackWrapper.thumbnail)
            } else {
                Log.e(TAG, "nextTrack - null")
                val drawable = ColorDrawable()
                drawable.color = Color.MAGENTA
                nextTrackView.setImageDrawable(drawable)
            }
        }
    }

    fun resizeX(offset: Float) {
        val startX = (requireView().width / 2 - trackView!!.width / 2).toFloat()
        val endX = getPx(requireContext(), 24).toFloat()
        val d = endX - startX
        val p = offset * d
        val newX = startX + p
        trackView!!.x = newX
    }

    fun resizeY(offset: Float) {
        val startY = (requireView().height / 2 - trackView!!.height / 2).toFloat()
        val endY = (getPx(requireContext(), 24) + insets!!.top).toFloat()
        val d = endY - startY
        val p = offset * d
        val newY = startY + p
        trackView!!.y = newY
    }

    fun resizeScale(offset: Float) {
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

    fun alphaSmall(offset: Float) {
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

    fun alphaLarge(offset: Float) {
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
        if (offset == 0f) nextTrackView!!.visibility =
            View.VISIBLE else nextTrackView!!.visibility = View.GONE
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
                mNextTrackView.scaleX = 0.5f + s
                mNextTrackView.scaleY = 0.5f + s
                mNextTrackView.alpha = 0.5f + s
                playlistView!!.x = mPx
            }
            if (event.action == MotionEvent.ACTION_UP) {
//                mParent.setUserInputEnabled (true);
                v.parent.requestDisallowInterceptTouchEvent(false)
                reset(v)
                resize(v, 1f)
                if (hasTrack) {
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
                    val newX = view!!.width / 2 - trackView!!.width / 2
                    val newY = view!!.height / 2 - trackView!!.height / 2
                    viewModel.getNext()
                    trackView!!.x = newX.toFloat()
                    trackView!!.y = newY.toFloat()
                    trackView!!.scaleX = 1f
                    trackView!!.scaleY = 1f
                    trackView!!.alpha = 1f
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
            pulseOut(trackView)
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

        private const val TAG = "TrackFragment"
    }
}