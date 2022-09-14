package com.guavaapps.spotlight;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ObjectStore {
    private static final String TAG = "ObjectStore";
    private static final String l = "JSON/";

    private Context mContext;

    public void put (String name, Object o) {
        Gson gson = new GsonBuilder ()
                .setPrettyPrinting ()
                .setLenient ()
                .create ();

        String json = gson.toJson (o);

        Log.e (TAG, json);
    }

    public void get (String name, Class c) {
        Gson gson = new GsonBuilder ()
                .setPrettyPrinting ()
                .setLenient ()
                .create ();

        String json = "";

        Object o = gson.fromJson (json, c);
    }
}
