package com.guavaapps.spotlight

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.button.MaterialButton
import com.google.android.material.shape.ShapeAppearanceModel
import com.guavaapps.components.Components.getPx
import com.guavaapps.components.Components.getPxF
import com.guavaapps.components.listview.ListView
import com.guavaapps.spotlight.realm.d
import io.realm.annotations.Beta

private const val TAG = "PlaylistsFragment"

object PlaylistView {
    fun create(
        context: Context,
        playlist: PlaylistWrapper,
        onClick: (View, PlaylistWrapper) -> Unit,
    ): View {
        val view = LayoutInflater.from(context).inflate(R.layout.track_view_item, null)
        view.tag = playlist.playlist?.id
        view.layoutParams = ViewGroup.MarginLayoutParams(-1, -2).apply {
            topMargin = getPx(context, 6)
            leftMargin = getPx(context, 12)
            rightMargin = getPx(context, 12)
            bottomMargin = getPx(context, 6)
        }

        val bitmap = view.findViewById<ImageView>(R.id.bitmap)
        val name = view.findViewById<TextView>(R.id.name)

        bitmap.setImageBitmap(playlist.thumbnail)
        name.text = playlist.playlist?.name

        val typedArray =
            context.theme.obtainStyledAttributes(android.R.style.Theme_Material_NoActionBar,
                intArrayOf(android.R.attr.selectableItemBackground))

        val ripple =
            context.resources.getDrawable(typedArray.getResourceId(0, 0), context.theme)
                .mutate()

        view.background = ripple
//        view.setBackgroundColor(Color.RED)

        view.clipToOutline = true
        view.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, getPxF(context, 24))
            }
        }

        view.setOnClickListener { onClick(view, playlist) }

        //view.elevation = getPxF(context, 2)

        return view
    }

    fun createAll(
        context: Context,
        playlists: List<PlaylistWrapper>,
        onClick: (View, PlaylistWrapper) -> Unit,
    ): List<View> {
        return playlists.mapIndexed { i, it -> PlaylistView.create(context, it, onClick) }
    }

    fun createAddButton(context: Context, onClick: () -> Unit): MaterialButton {
        val button = MaterialButton(context)

        button.text = "Add"
        button.setBackgroundColor(Color.MAGENTA)
        button.icon = context.resources.getDrawable(R.drawable.ic_add_24, context.theme)
        button.iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START

        button.shapeAppearanceModel = ShapeAppearanceModel.builder()
            .setAllCornerSizes(getPxF(context, 24))
            .build()

        button.insetTop = getPx(context, 6)
        button.insetBottom = getPx(context, 6)

        button.layoutParams = ViewGroup.MarginLayoutParams(-1, getPx(context, 76 + 12)).apply {
            leftMargin = getPx(context, 12)
            rightMargin = getPx(context, 12)
        }

        return button
    }

    fun wrapButton(button: MaterialButton) {
        val wrapper = FrameLayout(button.context)
        wrapper.addView(button)
    }
}