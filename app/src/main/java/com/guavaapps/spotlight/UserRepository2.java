package com.guavaapps.spotlight;

import androidx.lifecycle.MutableLiveData;

public class UserRepository2 {
    private UserLocalDataSource mLocalSource;
    private UserRemoteDataSource mRemoteSource;

    private MutableLiveData <UserWrapper> mCurrent = new MutableLiveData <> ();

    public UserRepository2(UserLocalDataSource localSource, UserRemoteDataSource remoteSource) {
        mLocalSource = localSource;
        mRemoteSource = remoteSource;
    }

    public void getCurrentUser () {
        boolean hasCurrent = mCurrent.getValue () != null;

        if (! hasCurrent) {
            mRemoteSource.getCurrentUser (userWrapper -> {
                mCurrent.setValue (userWrapper);
            });
        }
    }
}