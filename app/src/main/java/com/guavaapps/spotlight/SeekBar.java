package com.guavaapps.spotlight;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.FloatRange;
import androidx.annotation.Nullable;

public class SeekBar extends androidx.appcompat.widget.AppCompatSeekBar {
    private float mProgress;

    private OnSeekBarProgressChangedListener mOnSeekBarProgressChangedListener;

    public SeekBar (Context context) {
        super (context);
    }

    public SeekBar (Context context, @Nullable AttributeSet rawAttrs) {
        super (context, rawAttrs);

    }

    @Override
    protected void onSizeChanged (int w, int h, int oldw, int oldh) {
        super.onSizeChanged (w, h, oldw, oldh);


    }

    public void seekTo (@FloatRange (from = 0, to = 1) float position) {
        mProgress = position;
    }

//    public void setOnProgressChangedListener (OnSeekBarProgressChangedListener l) {
//        mOnSeekBarProgressChangedListener = l;
//    }
}
