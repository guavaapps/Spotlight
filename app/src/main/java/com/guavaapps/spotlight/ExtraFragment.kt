package com.guavaapps.spotlight

import android.animation.ArgbEvaluator
import com.guavaapps.components.Components.getPx
import com.guavaapps.spotlight.ColorSet.Companion.create
import com.google.android.material.tabs.TabLayout
import androidx.viewpager2.widget.ViewPager2
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View.OnLayoutChangeListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.view.ViewGroup.MarginLayoutParams
import android.text.SpannableString
import android.animation.ValueAnimator
import android.text.style.ForegroundColorSpan
import android.text.Spanned
import android.content.res.ColorStateList
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.util.ArraySet
import android.util.Log
import android.view.View
import androidx.core.graphics.Insets
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.tabs.TabLayoutMediator
import com.pixel.spotifyapi.Objects.*

private const val TAG = "ExtraFragment"

class ExtraFragment : Fragment() {
    private val viewModel: ContentViewModel by activityViewModels { ContentViewModel.Factory }

    private var insets: Insets? = null

    private var colorSet = ColorSet()

    private var adapter: Adapter? = null

    private lateinit var tabLayout: TabLayout

    private lateinit var viewPager: ViewPager2

    private var albumFragment: AlbumFragment? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_extra, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tabLayout = view.findViewById(R.id.tab_layout)
        viewPager = view.findViewById(R.id.view_pager)
        adapter = Adapter(requireActivity())

        tabLayout.tabMode = TabLayout.MODE_SCROLLABLE
        tabLayout.tabGravity = TabLayout.GRAVITY_CENTER
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
//                if (tabLayout.selectedTabPosition == 0) applyAlbumTabSelected(tab!!)
//                else applyAlbumTabUnselected(tabLayout.getTabAt(0)!!)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }

        })

        albumFragment = AlbumFragment()
        albumFragment?.onTrackSelected {
            Log.e(TAG, "track selected - ${it?.name}")
        }

        requireView().addOnLayoutChangeListener(object : OnLayoutChangeListener {
            override fun onLayoutChange(
                view: View,
                left: Int,
                top: Int,
                right: Int,
                bottom: Int,
                oldLeft: Int,
                oldTop: Int,
                oldRight: Int,
                oldBottom: Int,
            ) {
                ViewCompat.setOnApplyWindowInsetsListener(requireView()) { v: View?, windowInsetsCompat: WindowInsetsCompat ->
                    insets = windowInsetsCompat.getInsets(WindowInsetsCompat.Type.systemBars())
                    val params = view.layoutParams as MarginLayoutParams
                    params.topMargin = getPx(context!!, 128 + 48) + insets!!.top
                    params.bottomMargin = insets!!.bottom
                    view.layoutParams = params
                    WindowInsetsCompat.CONSUMED
                }
                view.removeOnLayoutChangeListener(this)
            }
        })

        viewModel.album.observe(viewLifecycleOwner) { albumWrapper: AlbumWrapper? ->
            if (albumWrapper?.album != null) {
                val tabs = mutableListOf<Fragment>()

                tabs.add(albumFragment!!)

                val artists = albumWrapper.album?.tracks?.items?.flatMap { it.artists }
                    ?.toTypedArray()?.distinctBy { it.id } ?: emptyList<ArtistSimple>()

                artists.forEach {
                    tabs.add(ArtistFragment(viewModel.artists.value?.find { w -> w.artist?.id == it.id }?.artist))
                }

                adapter!!.setItems(tabs)
                viewPager.adapter = adapter

                TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                    tab.text = if (position == 0) {
                        "From " + albumWrapper.album?.name
                    } else {
                        artists.map { it.name }[position - 1]
                    }
                }.attach()

                applyAlbum(albumWrapper)
            }
        }

        viewModel.track.observe(viewLifecycleOwner) {
            if (it?.track?.album?.id != viewModel.album.value?.album?.id) {
                tabLayout.removeAllTabs()
//                viewPager.removeAllViews()
            }
        }
    }

    private fun applyAlbumTabSelected(tab: TabLayout.Tab) {
        val tabText = tab.text ?: ""

        val themedText: Spannable = SpannableString(tabText)

        themedText.setSpan(
            ForegroundColorSpan(colorSet.primary),
            0,
            tabText.length,
            Spanned.SPAN_INCLUSIVE_EXCLUSIVE
        )

        tab.text = themedText
    }

    private fun applyAlbumTabUnselected(tab: TabLayout.Tab) {
        val tabText = tab.text ?: ""

        val themedText = SpannableString(tabText)

        themedText.setSpan(
            ForegroundColorSpan(colorSet.primary),
            0,
            "FROM".length,
            Spanned.SPAN_INCLUSIVE_EXCLUSIVE
        )
        themedText.setSpan(
            ForegroundColorSpan(colorSet.secondary),
            "FROM".length,
            tabText.length,
            Spanned.SPAN_INCLUSIVE_EXCLUSIVE
        )

        tab.text = themedText
    }

    private fun applyArtistTabColors() {
        tabLayout.setTabTextColors(colorSet.secondary, colorSet.primary)
    }

    private fun applyAlbumTabColors() {
        with(tabLayout.getTabAt(0)!!) {
            if (isSelected) applyAlbumTabSelected(this)
            else applyAlbumTabUnselected(this)
        }
    }

    private fun applyTabColors() {
        applyArtistTabColors()
        applyAlbumTabColors()

        tabLayout.setSelectedTabIndicatorColor(colorSet.primary)
    }

    private fun applyAlbum(wrappedAlbum: AlbumWrapper) {
        val bitmap = wrappedAlbum.bitmap
        val colorSet = create(bitmap)

        val primaryAnimator = ValueAnimator.ofObject(
            ArgbEvaluator(),
            this.colorSet.primary, colorSet.primary
        )

        primaryAnimator.duration =
            resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

        primaryAnimator.addUpdateListener { animation: ValueAnimator ->
            this.colorSet.primary = animation.animatedValue as Int

            applyTabColors()
        }
        primaryAnimator.start()

        val secondaryAnimator =
            ValueAnimator.ofObject(ArgbEvaluator(), this.colorSet.secondary, colorSet.secondary)

        secondaryAnimator.duration =
            resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
        secondaryAnimator.addUpdateListener { animation: ValueAnimator ->
            this.colorSet.secondary = animation.animatedValue as Int

            applyTabColors()
        }
        secondaryAnimator.start()
        val tertiaryAnimator =
            ValueAnimator.ofObject(ArgbEvaluator(), this.colorSet.tertiary, colorSet.tertiary)
        tertiaryAnimator.duration =
            resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
        tertiaryAnimator.addUpdateListener { animation: ValueAnimator ->
            this.colorSet.tertiary = animation.animatedValue as Int
        }
        tertiaryAnimator.start()
        val rippleAnimator =
            ValueAnimator.ofObject(ArgbEvaluator(), this.colorSet.ripple, colorSet.ripple)
        rippleAnimator.duration =
            resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
        rippleAnimator.addUpdateListener { animation: ValueAnimator ->
            this.colorSet.ripple = animation.animatedValue as Int

            tabLayout!!.tabRippleColor =
                ColorStateList(arrayOf(intArrayOf()), intArrayOf(this.colorSet.ripple))
        }
        rippleAnimator.start()
        this.colorSet = colorSet
    }
}

fun createOvalShapeAppearance(r: Float): ShapeAppearanceModel {
    val shape = ShapeAppearanceModel.builder()
        .setAllCornerSizes(r)
        .build()

    return shape
}