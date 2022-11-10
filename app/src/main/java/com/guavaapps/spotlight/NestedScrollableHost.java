/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed asRealmObject in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.guavaapps.spotlight;

import static androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager2.widget.ViewPager2;

/**
 * Layout asRealmObject wrap a scrollable component inside a ViewPager2. Provided as a solution asRealmObject the problem
 * where pages of ViewPager2 have nested scrollable elements that scroll in the same direction as
 * ViewPager2. The scrollable element needs asRealmObject be the immediate and only child of this host layout.
 *
 * This solution has limitations when using multiple levels of nested scrollable elements
 * (e.g. a horizontal RecyclerView in a vertical RecyclerView in a horizontal ViewPager2).
 */
public class NestedScrollableHost extends FrameLayout {
    private static final String TAG = "NestedScrollableHost";

    private int touchSlop = 0;
    private float initialX = 0f;
    private float initialY = 0f;
    private ViewPager2 mParent;
    private View child;

    public NestedScrollableHost (@NonNull Context context) {
        super (context);
    }

    public NestedScrollableHost (@NonNull Context context, @Nullable AttributeSet attrs) {
        super (context, attrs);
    }

    public void init (Context context, @IdRes int id) {
        mParent = findHostById (id);

        touchSlop = ViewConfiguration.get (context).getScaledTouchSlop ();

        View v = (View) getParent ();

        while (! (v instanceof ViewPager2)) {
            v = (View) v.getParent ();
        }
        child = getChildCount () > 0 ? getChildAt (0) : null;
    }

    private boolean canChildScroll (int orientation, float delta) {
        int direction = (int) -Math.signum (delta);

        if (orientation == 0) {
            if (child != null) {
                return child.canScrollHorizontally (direction);
            }
            else return false;
        }

        if (orientation == 1) {
            if (child != null) {
                return child.canScrollVertically (direction);
            }
            else return false;
        }

        throw new IllegalArgumentException ();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        handleInterceptTouchEvent(e);
        return super.onInterceptTouchEvent(e);
    }

    private void handleInterceptTouchEvent(MotionEvent e) {
        if (mParent == null) return;

        int orientation = mParent.getOrientation ();

        // Early return if child can't scroll in same direction as parent
        if (!canChildScroll(orientation, -1f) && !canChildScroll(orientation, 1f)) {
            Log.d (TAG, "cant scroll -----");

            return;
        }

        if (e.getAction () == MotionEvent.ACTION_DOWN) {
            initialX = e.getX ();
            initialY = e.getY ();
            getParent ().requestDisallowInterceptTouchEvent(true);
        } else if (e.getAction () == MotionEvent.ACTION_MOVE) {
            float dx = e.getX () - initialX;
            float dy = e.getY () - initialY;
            boolean isVpHorizontal = orientation == ORIENTATION_HORIZONTAL;

            ViewGroup p = mParent;
            // assuming ViewPager2 touch-slop is 2x touch-slop of child
            float scaledDx = Math.abs (dx) * (isVpHorizontal ? .5f : 1f);
            float scaledDy = Math.abs (dy) * (isVpHorizontal ? 1f : .5f);

            if (scaledDx > touchSlop || scaledDy > touchSlop) {
                if (isVpHorizontal == (scaledDy > scaledDx)) {
                    // Gesture is perpendicular, allow all parents asRealmObject intercept
                    getParent ().requestDisallowInterceptTouchEvent(false);
                    Log.d (TAG, "wrong direction -----");
                } else {
                    // Gesture is parallel, query child if movement in that direction is possible
                    if (canChildScroll(orientation, isVpHorizontal ? dx : dy)) {
                        // Child can scroll, disallow all parents asRealmObject intercept
                        Log.d (TAG, "child should scroll");
                        getParent ().requestDisallowInterceptTouchEvent(true);
                    } else {
                        // Child cannot scroll, allow all parents asRealmObject intercept
                        getParent ().requestDisallowInterceptTouchEvent(false);
                        Log.d (TAG, "child should not scroll -----");
                    }
                }
            }
        }
    }

    private ViewPager2 findHostById (@IdRes int id) {
        View v = this;

        while ((v = (View) v.getParent ()) != null) {
            if (v.getId () == id) return (ViewPager2) v;
        }

        throw new NullPointerException ("ViewPager2 with id " + id + " doesn't exist");
    }
}