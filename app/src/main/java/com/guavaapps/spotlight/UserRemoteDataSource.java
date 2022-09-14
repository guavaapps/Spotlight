package com.guavaapps.spotlight;

import android.graphics.Bitmap;

import com.guavaapps.components.bitmap.BitmapTools;
import com.pixel.spotifyapi.Objects.UserPrivate;
import com.pixel.spotifyapi.SpotifyService;

public class UserRemoteDataSource {
    private SpotifyService mSpotifyService;

    public UserRemoteDataSource (SpotifyService spotifyService) {
        mSpotifyService = spotifyService;
    }

    public UserWrapper getCurrentUser () {
        UserPrivate user = mSpotifyService.getCurrentUser ();
        Bitmap bitmap = BitmapTools.INSTANCE.from (user.images.get (0).url);

        return new UserWrapper (user, bitmap);
    }

    public void getCurrentUser (Callback callback) {
        callback.onGetUser (getCurrentUser ());
    }

    public interface Callback {
        void onGetUser (UserWrapper userWrapper);
    }
}
