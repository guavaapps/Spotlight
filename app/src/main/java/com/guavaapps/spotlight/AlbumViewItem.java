package com.guavaapps.spotlight;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.pixel.spotifyapi.Objects.ArtistSimple;
import com.pixel.spotifyapi.Objects.TrackSimple;

import java.util.ArrayList;
import java.util.List;

public class AlbumViewItem extends View {
    private static final int LAYOUT = R.layout.album_view_item;

    TextView mTitleView;
    TextView mArtistsView;

    public AlbumViewItem (@NonNull Context context, TrackSimple track) {
        super (context);

//        LayoutInflater.from (context).inflate (LAYOUT, this,true);

        mTitleView = findViewById (R.id.title_view);
        mArtistsView = findViewById (R.id.artists_view);

        mTitleView.setText (track.name);

        List <String> artists = new ArrayList <> ();

        for (ArtistSimple artist : track.artists) {
            artists.add (artist.name);
        }

        mArtistsView.setText (String.join (", ", artists));
    }
}
