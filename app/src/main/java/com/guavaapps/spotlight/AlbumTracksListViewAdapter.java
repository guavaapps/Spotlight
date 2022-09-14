//package com.guavaapps.spotlight;
//
//import android.graphics.Bitmap;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.ImageView;
//import android.widget.TextView;
//
//import androidx.annotation.NonNull;
//import androidx.palette.graphics.Palette;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.pixel.spotifyapi.Objects.ArtistSimple;
//import com.pixel.spotifyapi.Objects.TrackSimple;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class AlbumTracksListViewAdapter extends RecyclerView.Adapter {
//    private static final int LAYOUT = R.layout.album_player_item;
//
//    private AlbumWrapper mAlbum;
//    private int mPrimaryColor;
//    private int mSecondaryColor;
//
//    private int mSize;
//    private float mX;
//    private float mY;
//
//    @NonNull
//    @Override
//    public RecyclerView.ViewHolder onCreateViewHolder (@NonNull ViewGroup parent, int viewType) {
//        View view = LayoutInflater.from (parent.getContext ()).inflate (LAYOUT, parent, false);
//        return new ViewHolder (view);
//    }
//
//    @Override
//    public void onBindViewHolder (@NonNull RecyclerView.ViewHolder holder, int position) {
//        TrackSimple track = mAlbum.album.tracks.items.get (position);
//        Bitmap bitmap = mAlbum.thumbnail;
//
//        View view = holder.itemView;
//        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams ();
//        params.leftMargin = (int) mX;
//
//        if (position == 0) params.topMargin = (int) mY;
//        if (position == getItemCount () - 1) params.bottomMargin = (int) mY;
//
//        ImageView bitmapView = view.findViewById (R.id.bitmap_view);
//        TextView trackNameView = view.findViewById (R.id.track_name_view);
//        TextView trackArtistsView = view.findViewById (R.id.track_artists_view);
//
//        bitmapView.getLayoutParams ().width = mSize;
//        bitmapView.getLayoutParams ().height = mSize;
//        bitmapView.requestLayout ();
//
//        bitmapView.setImageBitmap (bitmap);
//        trackNameView.setText (track.name);
//
//        List <String> artists = new ArrayList <> ();
//
//        for (ArtistSimple artist : track.artists) {
//            artists.add (artist.name);
//        }
//
//        trackArtistsView.setText (String.join (", ", artists));
//
//        trackNameView.setTextColor (mPrimaryColor);
//        trackArtistsView.setTextColor (mSecondaryColor);
//    }
//
//    @Override
//    public int getItemCount () {
//        return mAlbum.album.tracks.items.size ();
//    }
//
//    public void setAlbum (AlbumWrapper album) {
//        mAlbum = album;
//
//        Bitmap bitmap = album.thumbnail;
//
//        int color = Palette.from (bitmap).generate ()
//                .getDominantSwatch ()
//                .getRgb ();
//
//        Hct primaryColor = Hct.fromInt (color);
//        primaryColor.setTone (90);
//        mPrimaryColor = primaryColor.toInt ();
//
//        Argb secondaryColor = Argb.from (primaryColor.toInt ());
//        secondaryColor.setAlpha (0.6f * 255);
//        mSecondaryColor = secondaryColor.toInt ();
//    }
//
//    public void setParams (int size, float x, float y) {
//        mSize = size;
//        mX = x;
//        mY = y;
//    }
//
//    private class ViewHolder extends RecyclerView.ViewHolder {
//        public ViewHolder (@NonNull View itemView) {
//            super (itemView);
//        }
//    }
//}
