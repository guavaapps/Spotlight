package com.guavaapps.spotlight;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.FlingAnimation;

import com.guavaapps.components.Components;
import com.pixel.spotifyapi.Objects.Track;

import java.util.Map;

public class TrackView extends View {
    private static final String TAG = "TrackViewDebug";

    private static final int SIZE_DP = 256;

    private float mAnchorX;
    private float mAnchorY;

    protected float rawScaleX;
    protected float rawScaleY;
    protected float scaleX;
    protected float scaleY;
    protected float stretchX;
    protected float stretchY;

    protected View mParent;

    private Track mTrack;

    GestureDetector trackViewGestureDetector;

    private InteractionListener mInteractionListener;

    @SuppressLint ("ClickableViewAccessibility")
    public TrackView (Context context, float anchorX, float anchorY) {
        super (context);
        this.mAnchorX = anchorX;
        this.mAnchorY = anchorY;

        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService (Context.LAYOUT_INFLATER_SERVICE);
//        layoutInflater.inflate (R.layout.layout_track_view, this, true);
        setLayoutParams (new ViewGroup.LayoutParams (Components.INSTANCE.getPx (context, SIZE_DP), Components.INSTANCE.getPx (context, SIZE_DP)));
        setElevation (Components.INSTANCE.getPx (getContext (), 8)); // FIXME: set elevation

        setOnTouchListener (new TrackViewOnTouchListener ());
        trackViewGestureDetector = new GestureDetector (getContext (), trackViewOnGestureListener);
    }

    @Override
    protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure (widthMeasureSpec, heightMeasureSpec);

        int minw = Components.INSTANCE.getPx (getContext (), SIZE_DP);
        int w = resolveSizeAndState (minw, widthMeasureSpec, 0);

        int minh = Components.INSTANCE.getPx (getContext (), SIZE_DP);
        int h = resolveSizeAndState (minh, heightMeasureSpec, 0);

        setMeasuredDimension (w, h);
    }

    @Override
    protected void onLayout (boolean changed, int l, int t, int r, int b) {
        super.onLayout (changed, l, t, r, b);

        mParent = (View) getParent ();

        mAnchorX = mAnchorX - getWidth () / 2;
        mAnchorY = mAnchorY - getHeight () / 2;

        setX (mAnchorX);
        setY (mAnchorY);

        Log.d (TAG, "anchorX: " + mAnchorX);
        Log.d (TAG, "anchorY: " + mAnchorY);

        Log.d (TAG, "parentW: " + mParent.getWidth ());
    }

    public void setTrack (Map <Track, Bitmap> track) {
        mTrack = track.keySet ().iterator ().next ();

        Bitmap thumbnail = track.values ().iterator ().next ();

        setBackground (new BitmapDrawable (getResources (), thumbnail));
    }

    private void snapToAnchor () {
        animate ()
                .x (mAnchorX)
                .setDuration (getResources ().getInteger (android.R.integer.config_shortAnimTime))
                .setUpdateListener (animation -> {
                    computeScale ();
                    computeStretch ();

                    if (mInteractionListener != null) mInteractionListener.onPositionChanged (getX (), getY (), false);
                })
                .start ();

        animate ()
                .y (mAnchorY)
                .setDuration (getResources ().getInteger (android.R.integer.config_shortAnimTime))
                .setUpdateListener (animation -> {
                    //setY ((Float) animation.getAnimatedValue ());

                    computeScale ();
                    computeStretch ();

                    if (mInteractionListener != null) mInteractionListener.onPositionChanged (getX (), getY (), false);

                })
                .start ();
    }

    public void flingOut (float velocityX, float velocityY) {
        FlingAnimation flingX = new FlingAnimation (this, DynamicAnimation.TRANSLATION_X);
        flingX.setStartVelocity (velocityX)
                .setFriction (0.1f)
                .start ();

        FlingAnimation flingY = new FlingAnimation (this, DynamicAnimation.TRANSLATION_Y);
        flingY.setStartVelocity (velocityY)
                .setFriction (0.1f)
                .start ();
    }

//    private void computeDx () {
//        dx = getX () - touchX;
//    }
//
//    private void computeDy () {
//        mDy = getY () - touchY;
//    }
//
//    private void deflateDx () {
//        if (dxAnimator == null || !dxAnimator.isRunning ()) {
//            dxAnimator = ValueAnimator.ofFloat (dx, -getWidth () / 2);
//            dxAnimator.setInterpolator (new LinearInterpolator ());
//            dxAnimator.setDuration (getResources ().getInteger (android.R.integer.config_shortAnimTime));
//            dxAnimator.addUpdateListener ((animation) -> {
//                dx = (float) animation.getAnimatedValue ();
//
//                computeScale ();
//                computeStretch ();
//
//                setX (touchX + dx);
//            });
//            dxAnimator.start ();
//        }
//    }
//
//    private void deflateDy () {
//        if (mDyAnimator == null || !mDyAnimator.isRunning ()) {
//            mDyAnimator = ValueAnimator.ofFloat (mDy, -getHeight () / 2);
//            mDyAnimator.setInterpolator (new LinearInterpolator ());
//            mDyAnimator.setDuration (getResources ().getInteger (android.R.integer.config_shortAnimTime));
//            mDyAnimator.addUpdateListener ((animation) -> {
//                mDy = (float) animation.getAnimatedValue ();
//
//                setY (touchY + mDy);
//
//                computeScale ();
//                computeStretch ();
//            });
//            mDyAnimator.start ();
//        }
//    }
//
//    private void stopDxDeflation () {
//        if (dxAnimator != null) {
//            dxAnimator.cancel ();
//            dxAnimator = null;
//        }
//    }
//
//    private void stopDyDeflation () {
//        if (mDyAnimator != null) {
//            mDyAnimator.cancel ();
//            mDyAnimator = null;
//        }
//    }

//    private OnTouchListener mOnTouchListener = new OnTouchListener () {
//        private float pX;
//        private float pY;
//
//        @Override
//        public boolean onTouch (View v, MotionEvent event) {
//            if (trackViewGestureDetector.onTouchEvent (event)) return true;
//
//            int action = event.getAction ();
//            touchX = event.getRawX () - mParent.getX ();
//            touchY = event.getRawY () - mParent.getY ();
//
//            if (action == MotionEvent.ACTION_DOWN) {
//                computeDx ();
//                computeDy ();
//
//                deflateDx ();
//                deflateDy ();
//            } else if (action == MotionEvent.ACTION_MOVE) {
//                float rawTouchDx = touchX - pX;
//                float rawTouchDy = touchY - pY;
//
//                float touchDx = Math.abs (rawTouchDx);
//                float touchDy = Math.abs (rawTouchDy);
//
//                float directionX = rawTouchDx != 0 ? rawTouchDx / touchDx : 0;
//                float directionY = rawTouchDy != 0 ? rawTouchDy / touchDy : 0;
//
//                if (touchDx < event.getXPrecision ()) directionX = 0;
//                if (touchDy < event.getYPrecision ()) directionY = 0;
//
//                if (directionX == -1) {
//                    if (dx + getWidth () / 2 > 0) {
//                        deflateDx ();
//                    } else if (dx + getWidth () / 2 < 0) {
//                        stopDxDeflation ();
//                        computeDx ();
//                    }
//                } else if (directionX == 1) {
//                    if (dx + getWidth () / 2 < 0) {
//                        deflateDx ();
//                    } else if (dx + getWidth () / 2 > 0) {
//                        stopDxDeflation ();
//                        computeDx ();
//                    }
//                }
//
//                if (directionY == -1) {
//                    if (mDy + getHeight () / 2 > 0) {
//                        deflateDy ();
//                    } else if (mDy + getHeight () / 2 < 0) {
//                        stopDyDeflation ();
//                        computeDy ();
//                    }
//                } else if (directionY == 1) {
//                    if (mDy + getHeight () / 2 < 0) {
//                        deflateDy ();
//                    } else if (mDy + getHeight () / 2 > 0) {
//                        stopDyDeflation ();
//                        computeDy ();
//                    }
//                }
//
//                setX (touchX + dx);
//                setY (touchY + mDy);
//
//                pX = touchX;
//                pY = touchY;
//
//                computeScale ();
//                computeStretch ();
//                dispatchOnPositionChanged (TrackView.this, getX (), getY (), scaleX, scaleY, stretchX, stretchY, true);
//            } else if (action == MotionEvent.ACTION_UP) {
//                stopDxDeflation ();
//                stopDyDeflation ();
//
//                snapToAnchor ();
//            }
//
//            return true;
//        }
//    };

    private class TrackViewOnTouchListener implements OnTouchListener {
        private float mX;
        private float mY;

        private float mPX;
        private float mPY;

        private float mDx;
        private float mDy;

        private ValueAnimator mDxAnimator;
        private ValueAnimator mDyAnimator;

        @Override
        public boolean onTouch (View v, MotionEvent event) {
            if (trackViewGestureDetector.onTouchEvent (event)) return true;

            int action = event.getAction ();
            mX = event.getRawX () - mParent.getX ();
            mY = event.getRawY () - mParent.getY ();

            if (action == MotionEvent.ACTION_DOWN) {
                computeDx ();
                computeDy ();

                deflateDx ();
                deflateDy ();
            } else if (action == MotionEvent.ACTION_MOVE) {
                float rawTouchDx = mX - mPX;
                float rawTouchDy = mY - mPY;

                float touchDx = Math.abs (rawTouchDx);
                float touchDy = Math.abs (rawTouchDy);

                float directionX = rawTouchDx != 0 ? rawTouchDx / touchDx : 0;
                float directionY = rawTouchDy != 0 ? rawTouchDy / touchDy : 0;

                if (touchDx < event.getXPrecision ()) directionX = 0;
                if (touchDy < event.getYPrecision ()) directionY = 0;

                if (directionX == -1) {
                    if (mDx + getWidth () / 2 > 0) {
                        deflateDx ();
                    } else if (mDx + getWidth () / 2 < 0) {
                        stopDxDeflation ();
                        computeDx ();
                    }
                } else if (directionX == 1) {
                    if (mDx + getWidth () / 2 < 0) {
                        deflateDx ();
                    } else if (mDx + getWidth () / 2 > 0) {
                        stopDxDeflation ();
                        computeDx ();
                    }
                }

                if (directionY == -1) {
                    if (mDy + getHeight () / 2 > 0) {
                        deflateDy ();
                    } else if (mDy + getHeight () / 2 < 0) {
                        stopDyDeflation ();
                        computeDy ();
                    }
                } else if (directionY == 1) {
                    if (mDy + getHeight () / 2 < 0) {
                        deflateDy ();
                    } else if (mDy + getHeight () / 2 > 0) {
                        stopDyDeflation ();
                        computeDy ();
                    }
                }

                setX (mX + mDx);
                setY (mY + mDy);

                mPX = mX;
                mPY = mY;

                computeScale ();
                computeStretch ();
                if (mInteractionListener != null) mInteractionListener.onPositionChanged (getX (), getY (), true);
            } else if (action == MotionEvent.ACTION_UP) {
                stopDxDeflation ();
                stopDyDeflation ();

                snapToAnchor ();
            }

            return true;
        }

        private void computeDx () {
            mDx = getX () - mX;
        }

        private void computeDy () {
            mDy = getY () - mY;
        }

        private void deflateDx () {
            if (mDxAnimator == null || !mDxAnimator.isRunning ()) {
                mDxAnimator = ValueAnimator.ofFloat (mDx, -getWidth () / 2);
                mDxAnimator.setInterpolator (new LinearInterpolator ());
                mDxAnimator.setDuration (getResources ().getInteger (android.R.integer.config_shortAnimTime));
                mDxAnimator.addUpdateListener ((animation) -> {
                    mDx = (float) animation.getAnimatedValue ();

                    computeScale ();
                    computeStretch ();

                    setX (mX + mDx);
                });
                mDxAnimator.start ();
            }
        }

        private void deflateDy () {
            if (mDyAnimator == null || !mDyAnimator.isRunning ()) {
                mDyAnimator = ValueAnimator.ofFloat (mDy, -getHeight () / 2);
                mDyAnimator.setInterpolator (new LinearInterpolator ());
                mDyAnimator.setDuration (getResources ().getInteger (android.R.integer.config_shortAnimTime));
                mDyAnimator.addUpdateListener ((animation) -> {
                    mDy = (float) animation.getAnimatedValue ();

                    setY (mY + mDy);

                    computeScale ();
                    computeStretch ();
                });
                mDyAnimator.start ();
            }
        }

        private void stopDxDeflation () {
            if (mDxAnimator != null) {
                mDxAnimator.cancel ();
                mDxAnimator = null;
            }
        }

        private void stopDyDeflation () {
            if (mDyAnimator != null) {
                mDyAnimator.cancel ();
                mDyAnimator = null;
            }
        }
    }

    private GestureDetector.OnGestureListener trackViewOnGestureListener = new GestureDetector.SimpleOnGestureListener () {
        @Override
        public boolean onFling (MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            float x = velocityX / ViewConfiguration.get (getContext ()).getScaledMaximumFlingVelocity ();
            float y = velocityY / ViewConfiguration.get (getContext ()).getScaledMaximumFlingVelocity ();

            float v = (float) Math.sqrt (x * x + y * y);
            float dx = Math.abs (e1.getRawX () - e2.getRawX ());
            float mDy = Math.abs (e1.getRawY () - e2.getRawY ());

            float d = (float) Math.sqrt (dx * dx + mDy * mDy) / getContext ().getResources ().getDisplayMetrics ().widthPixels;

            if (d >= 0.4 && (e1.getRawX () - e2.getRawX ()) > 0) {
                //flingOut (-ViewConfiguration.get (getContext ()).getScaledMaximumFlingVelocity (), velocityY);

                //return true;
            }

            return false;
        }
    };

    public void computeScale () {
        rawScaleX = (getX () + getWidth () / 2 - mParent.getWidth () / 2) / (0.5f * mParent.getWidth ());
        rawScaleY = (getY () + getHeight () / 2 - mParent.getHeight () / 2) / (0.5f * mParent.getHeight ());

        scaleX = Math.abs ((getX () + getWidth () / 2 - mParent.getWidth () / 2) / (0.5f * mParent.getWidth ()));
        scaleY = Math.abs ((getY () + getHeight () / 2 - mParent.getHeight () / 2) / (0.5f * mParent.getHeight ()));
    }

    public void computeStretch () {
        DecelerateInterpolator decelerateInterpolator = new DecelerateInterpolator ();

        float _x = rawScaleX / scaleX;
        float _y = rawScaleY / scaleY;

        stretchX = (rawScaleX != 0 ? _x : 0) * decelerateInterpolator.getInterpolation (scaleX);
        stretchY = (rawScaleY != 0 ? _y : 0) * decelerateInterpolator.getInterpolation (scaleY);
    }

    public void setInteractionListener (InteractionListener l) {
        mInteractionListener = l;
    }

    public interface InteractionListener {
        void onPositionChanged (float x, float y, boolean down);
    }
}
