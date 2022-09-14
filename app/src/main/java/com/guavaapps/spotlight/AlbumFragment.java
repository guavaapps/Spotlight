package com.guavaapps.spotlight;

import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.guavaapps.components.listview.ListView;
import com.pixel.spotifyapi.Objects.ArtistSimple;
import com.pixel.spotifyapi.Objects.TrackSimple;

import java.util.ArrayList;
import java.util.List;

public class AlbumFragment extends Fragment {
    private static final String TAG = "AlbumFragment";

    private ContentViewModel mViewModel;

    private AlbumWrapper mAlbum;
    private List <View> mItems = new ArrayList <> ();
    private List <String> mIds = new ArrayList <> ();

    private NestedScrollableHost mNestedScrollableHost;

    private ListView mListView;

    private String mId;
    private Listener mListener;
    private ColorSet mColorSet;

    public AlbumFragment () {
    }

    @Nullable
    @Override
    public View onCreateView (@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate (R.layout.fragment_album, container, false);
    }

    @Override
    public void onViewCreated (@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated (view, savedInstanceState);

        mViewModel = new ViewModelProvider (requireActivity ()).get (ContentViewModel.class);

        mNestedScrollableHost = view.findViewById (R.id.host);
        mNestedScrollableHost.post (() -> mNestedScrollableHost.init (getContext (), R.id.pager));//pager2));
        mListView = view.findViewById (R.id.list_view);

        mViewModel.getAlbum ().observe (getViewLifecycleOwner (), albumWrapper -> {
            String track = mViewModel.getTrack ().getValue ().track.id;

            setAlbum (albumWrapper, track);
        });
    }

    private void setCurrentTrack (String id) {
        int i = mIds.indexOf (mId);
        int j = mIds.indexOf (id);

        mId = id;

        if (i != -1) {
            View v1 = mItems.get (i);
            TextView titleView1 = v1.findViewById (R.id.title_view);
            TextView titleView2 = v1.findViewById (R.id.duration_view);
            titleView1.setTextColor (mColorSet.getSecondary ());
            titleView2.setTextColor (mColorSet.getSecondary ());
        }

        View v2 = mItems.get (j);
        TextView titleView2 = v2.findViewById (R.id.title_view);
        TextView titleView3 = v2.findViewById (R.id.duration_view);
        titleView2.setTextColor (mColorSet.getPrimary ());
        titleView3.setTextColor (mColorSet.getPrimary ());

        mListView.scrollToPosition (j);
    }

    public void setAlbum (AlbumWrapper album, String id) {
        mAlbum = album;

        mColorSet = ColorSet.Companion.create (album.bitmap);

        mIds.clear ();
        mItems.clear ();
        mListView.clear ();

        for (TrackSimple track : mAlbum.album.tracks.items) {
            mIds.add (track.id);

            View item = LayoutInflater.from (getContext ()).inflate (R.layout.album_view_item, null, false);
            item.setLayoutParams (new ViewGroup.LayoutParams (-1, -2));
            TypedArray typedArray = getContext ().getTheme ().obtainStyledAttributes (android.R.style.Theme_Material_NoActionBar, new int[] {android.R.attr.selectableItemBackground});
            Drawable ripple = getResources ().getDrawable (typedArray.getResourceId (0, 0), getContext ().getTheme ()).mutate ();
            ripple.setTint (mColorSet.getRipple ());
            item.setBackground (ripple);
            item.setClickable (true);
            item.setOnClickListener ((v -> {
                if (mListener != null) {
                    mListener.onClick (track);
                    setCurrentTrack (track.id);
                }
            }));
            TextView titleView = item.findViewById (R.id.title_view);
            TextView artistsView = item.findViewById (R.id.artists_view);
            TextView durationView = item.findViewById (R.id.duration_view);

            titleView.setText (track.name);

            String duration = new TimeString.Builder (track.duration_ms)
                    .minutes ()
                    .separator (":")
                    .seconds ("%02d")
                    .build ()
                    .toString ();

            durationView.setText (duration);

            List <String> artists = new ArrayList <> ();

            for (ArtistSimple artist : track.artists) {
                artists.add (artist.name);
            }

            artistsView.setText (String.join (", ", artists));

            titleView.setTextColor (mColorSet.getSecondary ());
            artistsView.setTextColor (mColorSet.getSecondary ());
            durationView.setTextColor (mColorSet.getSecondary ());

            mItems.add (item);
        }

        mListView.add (mItems);

        setCurrentTrack (id);
    }

    public void setListener (Listener l) {
        mListener = l;
    }

    public AlbumWrapper getAlbum () {
        return mAlbum;
    }

    public interface Listener {
        void onClick (TrackSimple track);
    }
}
