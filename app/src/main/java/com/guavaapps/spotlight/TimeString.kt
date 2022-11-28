package com.guavaapps.spotlight;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TimeString {
    private long mMilliseconds;
    private List <String> mComponents = new ArrayList <> ();

    public TimeString (long milliseconds) {
        mMilliseconds = milliseconds;
    }

    @Deprecated
    public static String getTimeString (long ms) {
        return TimeUnit.MILLISECONDS.toMinutes (ms)
                + ":"
                + (TimeUnit.MILLISECONDS.toSeconds (ms) - TimeUnit.MINUTES.toSeconds (TimeUnit.MILLISECONDS.toMinutes (ms)));
    }

    @NonNull
    @Override
    public String toString () {
        return String.join ("", mComponents);
    }

    public static final class Builder {
        private TimeString mTimeString;

        public Builder (long milliseconds) {
            mTimeString = new TimeString (milliseconds);
        }

        public Builder separator (String s) {
            mTimeString.mComponents.add (s);

            return this;
        }

        public Builder milliseconds () {
            return milliseconds ("%d");
        }

        public Builder milliseconds (String f) {
            long c = mTimeString.mMilliseconds
                    - TimeUnit.SECONDS.toMillis (mTimeString.mMilliseconds);

            mTimeString.mComponents.add (String.format (f, c));

            return this;
        }

        public Builder seconds () {
            return seconds ("%d");
        }

        public Builder seconds (String f) {
            long c = (TimeUnit.MILLISECONDS.toSeconds (mTimeString.mMilliseconds)
                    - TimeUnit.MINUTES.toSeconds (TimeUnit.MILLISECONDS.toMinutes (mTimeString.mMilliseconds)));

            mTimeString.mComponents.add (String.format (f, c));

            return this;
        }

        public Builder minutes () {
            return minutes ("%d");
        }

        public Builder minutes (String f) {
            long c = TimeUnit.MILLISECONDS.toMinutes (mTimeString.mMilliseconds)
                    - TimeUnit.HOURS.toMinutes (TimeUnit.MILLISECONDS.toHours (mTimeString.mMilliseconds));

            mTimeString.mComponents.add (String.format (f, c));

            return this;
        }

        public TimeString build () {
            return mTimeString;
        }
    }
}
