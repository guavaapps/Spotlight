package com.guavaapps.spotlight;

public class UserLocalDataSource {
    private ObjectStore mObjectStore;

    public UserLocalDataSource (ObjectStore objectStore) {
        mObjectStore = objectStore;
    }

    public boolean hasUser (String id) {
        return false;
    }

    public UserWrapper getUser (String id) {
        return null;
    }

    public void putUser (UserWrapper user) {

    }

    public void getAllUsers () {

    }

    public void deleteUser (String id) {

    }
}
