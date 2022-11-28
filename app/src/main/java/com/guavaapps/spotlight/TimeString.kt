package com.guavaapps.spotlight

import java.util.concurrent.TimeUnit

class TimeString(private val millis: Long) {
    private val components = mutableListOf<String>()

    override fun toString(): String {
        return components.joinToString("")
    }

    fun separator(s: String) {
        components.add(s)
    }

    fun milliseconds(f: String? = "%d") {
        val c = (millis
                - TimeUnit.SECONDS.toMillis(millis))
        components.add(String.format(f!!, c))
    }

    fun seconds(f: String? = "%d") {
        val c = (TimeUnit.MILLISECONDS.toSeconds(millis)
                - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)))
        components.add(String.format(f!!, c))
    }

    fun minutes(f: String? = "%d") {
        val c = (TimeUnit.MILLISECONDS.toMinutes(millis)
                - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)))
        components.add(String.format(f!!, c))
    }

    class Builder(milliseconds: Long) {
        private val mTimeString: TimeString
        fun separator(s: String): Builder {
            mTimeString.components.add(s)
            return this
        }

        fun milliseconds(f: String? = "%d"): Builder {
            val c = (mTimeString.millis
                    - TimeUnit.SECONDS.toMillis(mTimeString.millis))
            mTimeString.components.add(String.format(f!!, c))
            return this
        }

        fun seconds(f: String? = "%d"): Builder {
            val c = (TimeUnit.MILLISECONDS.toSeconds(mTimeString.millis)
                    - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(mTimeString.millis)))
            mTimeString.components.add(String.format(f!!, c))
            return this
        }

        fun minutes(f: String? = "%d"): Builder {
            val c = (TimeUnit.MILLISECONDS.toMinutes(mTimeString.millis)
                    - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(mTimeString.millis)))
            mTimeString.components.add(String.format(f!!, c))
            return this
        }

        fun build(): TimeString {
            return mTimeString
        }

        init {
            mTimeString = TimeString(milliseconds)
        }
    }
}