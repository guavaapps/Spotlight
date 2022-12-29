package com.guavaapps.spotlight;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.guavaapps.components.Components;

public class PlaylistView2 extends View {
    private static final String TAG = "AlbumFragment";

    private static final int SIZE_DP = 128;

    public PlaylistView2(Context context) {
        this (context, null);
    }

    public PlaylistView2(Context context, @Nullable AttributeSet attrs) {
        super (context, attrs);

        setLayoutParams (new ViewGroup.LayoutParams (Components.INSTANCE.getPx (context, SIZE_DP), Components.INSTANCE.getPx (context, SIZE_DP)));
    }

    @Override
    protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure (widthMeasureSpec, heightMeasureSpec);

        int minw = Components.INSTANCE.getPx (getContext (), SIZE_DP);
        int w = resolveSizeAndState(minw, widthMeasureSpec, 0);

        int minh = Components.INSTANCE.getPx (getContext (), SIZE_DP);
        int h = resolveSizeAndState(minh, heightMeasureSpec, 0);

        setMeasuredDimension(w, h);
    }

    public void setPlaylist (Bitmap bitmap) {
        setBackground (new BitmapDrawable (getResources (), bitmap));
    }
}
