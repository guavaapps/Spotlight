package com.guavaapps.spotlight

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import androidx.annotation.ColorInt
import com.guavaapps.spotlight.ColorSet
import androidx.palette.graphics.Palette
import androidx.palette.graphics.Palette.Swatch
import com.google.android.material.color.ColorRoles
import com.google.android.material.color.MaterialColors
import com.guavaapps.components.color.Argb
import com.guavaapps.components.color.Hct

class ColorSet {
    // TODO rename colors
    // TODO find a way to deal with low delta
    // TODO use Pair<K, V> instead of IntArray(2)

    // tones:
    // primary -> 90
    // secondary -> primary alpha 0.6
    // tertiary -> primary alpha 0.24
    // text -> primary 10
    // surface ->

    val MAX_DELTA_E = Math.sqrt(195075.0).toFloat()
    var primary = 0
    var secondary = 0
    var tertiary = 0
    var text = 0
    var ripple = 0
    var surface = IntArray(2)
    var surfaceGradient = IntArray(2)
    var surfaceOverlay = 0

    var color40 = 0
    var color10 = 0

    companion object {
        private const val TAG = "ColorSet"
        fun create(bitmap: Bitmap?): ColorSet {
            val colorSet = ColorSet()

            var swatch: Swatch? = null
            var builder: Palette.Builder? = null

            if (bitmap != null) {
                builder = Palette.Builder(bitmap)

                if (builder.generate().dominantSwatch.also { swatch = it } == null) {
                    // palette builder adds a lightness filter by default
                    // if bitmap too dark or light asRealmObject have a dominant swatch clear the filter
                    swatch = builder.clearFilters()
                        .generate()
                        .dominantSwatch
                }
            }

            val color = swatch?.rgb ?: Color.BLACK

            val color40 = Hct.fromInt(color)
            color40.tone = 40f
            colorSet.color40 = color40.toInt()

            val color10 = Hct.fromInt(color)
            color10.tone = 10f
            colorSet.color10 = color10.toInt()

            val primaryColor = Hct.fromInt(color)
            primaryColor.tone = 90f
            colorSet.primary = primaryColor.toInt()

            val textColor: Hct = Hct.fromInt(color)
            textColor.tone = 10f
            colorSet.text = textColor.toInt()

            val c = Argb.from(primaryColor.toInt())
            c.alpha = 0.6f * 255
            colorSet.secondary = c.toInt()

            c.alpha = 0.24f * 255
            colorSet.tertiary = c.toInt()

            c.alpha = 0.16f * 255
            colorSet.ripple = c.toInt()

            val surfaceColor1: Hct = Hct.fromInt(color)
            surfaceColor1.tone = 10f

            colorSet.surface[0] = surfaceColor1.toInt().toArgb().apply { alpha = 0.16f * 255 }.toInt()

            val nextDominant = findNextDominant(builder?.generate())?.rgb ?: Color.BLACK
            val surfaceColor2: Hct = Hct.fromInt(nextDominant)
            surfaceColor2.chroma = surfaceColor1.chroma
            surfaceColor2.tone = 10f

            colorSet.surface[1] = surfaceColor2.toInt()

            return colorSet
        }

        private fun findNextDominant(palette: Palette?): Swatch? {
            palette ?: return null

            val swatches = palette.swatches
            val dominant = palette.dominantSwatch!!.rgb
            var next = palette.dominantSwatch
            var hPop = Int.MIN_VALUE
            for (swatch in swatches) {
                val color = swatch.rgb
                if (color != dominant && swatch.population > hPop) {
                    hPop = swatch.population
                    next = swatch
                }
            }
            return next
        }

        private fun deltaH(color1: Int, color2: Int): Float {
            val hct1: Hct = Hct.fromInt(color1)
            val hue1 = hct1.hue
            val hct2: Hct = Hct.fromInt(color2)
            val hue2 = hct2.hue
            return hue1 - hue2
        }

        private fun delta(color1: Int, color2: Int): Float {
            val hct1: Hct = Hct.fromInt(color1)
            val hue1 = hct1.hue
            val chroma1 = hct1.chroma
            val tone1 = hct1.tone
            val hct2: Hct = Hct.fromInt(color2)
            val hue2 = hct2.hue
            val chroma2 = hct2.chroma
            val tone2 = hct2.tone
            val dH = hue1 - hue2
            val dC = chroma1 - chroma2
            val dT = tone1 - tone2
            return Math.sqrt((dH * dH + dC * dC + dT * dT).toDouble()).toFloat()
        }
    }
}


fun Int.toArgb () = Argb.from(this)
fun Int.toHct () = Hct.fromInt(this)