package com.guavaapps.spotlight;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.ArrayList;
import java.util.List;

public class Adapter extends FragmentStateAdapter {
    @LayoutRes
    private static final int LAYOUT = R.layout.adapter_layout;
    private List <Fragment> mItems = new ArrayList <> ();

    public Adapter (@NonNull FragmentActivity fragmentActivity) {
        super (fragmentActivity);
    }

    public void setItems (List <Fragment> items) {
        mItems = items;
    }

    @NonNull
    @Override
    public Fragment createFragment (int position) {
        return mItems.get (position);
    }

    @Override
    public int getItemCount () {
        return mItems.size ();
    }
}
