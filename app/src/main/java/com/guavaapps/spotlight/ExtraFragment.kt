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
import android.util.Log
import android.view.View
import androidx.core.graphics.Insets
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.tabs.TabLayoutMediator
import com.pixel.spotifyapi.Objects.*
import java.util.ArrayList
import java.util.HashMap

class ExtraFragment : Fragment() {
    private val viewModel: ContentViewModel by activityViewModels { ContentViewModel.Factory }

    private var mInsets: Insets? = null

    private var colorSet = ColorSet()

    private var mTrack: TrackWrapper? = null
    private var mAlbum: AlbumWrapper? = null

    private val mArtists: MutableList<ArtistWrapper> = ArrayList()

    private var adapter: Adapter? = null

    private lateinit var tabLayout: TabLayout

    private lateinit var viewPager: ViewPager2

    private var albumFragment: AlbumFragment? = null

    private val mArtistFragments: List<ArtistFragment> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

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
        tabLayout.tabMode = TabLayout.MODE_SCROLLABLE
        tabLayout.tabGravity = TabLayout.GRAVITY_CENTER
        albumFragment = AlbumFragment()
//        mAlbumFragment!!.setListener { track: TrackSimple ->
//            viewModel.spotifyService
//                .getTrack(track.id, object : Callback<Track> {
//                    override fun success(track: Track, response: Response) {
//                        val wrappedTrack = TrackWrapper(track, viewModel!!.album.value!!.bitmap)
//                        viewModel!!.track.setValue(wrappedTrack)
//                        viewModel!!.spotifyAppRemote.value!!
//                            .getPlayerApi()
//                            .play(track.uri)
//                    }
//
//                    override fun failure(error: RetrofitError) {}
//                })
//        }
        adapter = Adapter(requireActivity())
        viewPager.adapter = adapter

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
                    mInsets = windowInsetsCompat.getInsets(WindowInsetsCompat.Type.systemBars())
                    val params = view.layoutParams as MarginLayoutParams
                    params.topMargin = getPx(context!!, 128 + 48) + mInsets!!.top
                    params.bottomMargin = mInsets!!.bottom
                    view.layoutParams = params
                    WindowInsetsCompat.CONSUMED
                }
                view.removeOnLayoutChangeListener(this)
            }
        })
        viewModel.track.observe(viewLifecycleOwner) { trackWrapper: TrackWrapper? ->
            if (trackWrapper != null) {
                mTrack = trackWrapper
                if (albumFragment!!.album == null || albumFragment!!.album!!.album!!.id != mTrack!!.track.album.id) {
//                    AppRepo.getInstance() // TODO use view model
//                        .getAlbum(viewModel!!.spotifyService.value, context,
//                            trackWrapper.track.album.id,
//                            object : AppRepo.ResultListener() {
//                                override fun onAlbum(albumWrapper: AlbumWrapper) {
//                                    super.onAlbum(albumWrapper)
//                                    Handler.createAsync(Looper.getMainLooper())
//                                        .post {
//                                            mViewPager.setAdapter(mAdapter)
//                                            TabLayoutMediator(mTabLayout,
//                                                mViewPager,
//                                                TabConfigurationStrategy { tab: TabLayout.Tab, position: Int ->
//                                                    if (position == 0) {
//                                                        tab.text =
//                                                            "From " + mTrack!!.track.album.name
//                                                    } else tab.text =
//                                                        mArtists[position - 1].artist.name
//                                                }).attach()
//                                        }
//                                }
//                            })
                }
            }
        }
        viewModel.album.observe(viewLifecycleOwner) { albumWrapper: AlbumWrapper? ->
            Log.e(TAG, "extra album")
            if (albumWrapper?.album != null) {
                Log.e(TAG, "album not null")

                mAlbum = albumWrapper
                nextAlbum(albumWrapper)
                val tabs = mutableListOf<Fragment>()
                tabs.add(albumFragment!!)
                mArtists.clear()
                for (artistSimple in getAllArtists(mAlbum!!.album!!)) { //mAlbum.album.artists) {
                    Log.e(TAG, "adding artist - ${artistSimple.name}")
                    val artist = Artist()
                    artist.id = artistSimple.id
                    artist.name = artistSimple.name
                    mArtists.add(ArtistWrapper(artist, null))
                    tabs.add(ArtistFragment())
                }
                adapter!!.setItems(tabs)

                TabLayoutMediator (tabLayout, viewPager) { tab, position ->
                    if (position == 0) tab.text = mAlbum?.album?.name
                    else tab.text = mArtists [position - 1].artist?.name
                }.attach()
            }
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun nextAlbum(wrappedAlbum: AlbumWrapper) {
        val album = wrappedAlbum.album
        val bitmap = wrappedAlbum.bitmap
        val colorSet = create(bitmap)
        val albumTabTitle = SpannableString("From " + album!!.name)
        val primaryAnimator =
            ValueAnimator.ofObject(ArgbEvaluator(), this.colorSet.primary, colorSet.primary)
        primaryAnimator.duration =
            resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
        primaryAnimator.addUpdateListener { animation: ValueAnimator ->
            this.colorSet.primary = animation.animatedValue as Int
            tabLayout!!.setTabTextColors(this.colorSet.primary, animation.animatedValue as Int)
            tabLayout!!.setSelectedTabIndicatorColor(this.colorSet.primary)
            albumTabTitle.setSpan(ForegroundColorSpan(this.colorSet.primary),
                5,
                albumTabTitle.length,
                Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        }
        primaryAnimator.start()
        val secondaryAnimator =
            ValueAnimator.ofObject(ArgbEvaluator(), this.colorSet.secondary, colorSet.secondary)
        secondaryAnimator.duration =
            resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
        secondaryAnimator.addUpdateListener { animation: ValueAnimator ->
            this.colorSet.primary = animation.animatedValue as Int
            tabLayout!!.setTabTextColors(this.colorSet.secondary, this.colorSet.primary)
            albumTabTitle.setSpan(ForegroundColorSpan(this.colorSet.secondary),
                0,
                4,
                Spanned.SPAN_INCLUSIVE_INCLUSIVE)
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

    companion object {
        private const val TAG = "ExtraFragment"
        private fun getAllArtists(album: Album): List<ArtistSimple> {
            val artists: MutableList<ArtistSimple> = ArrayList()
            val quantitizedMap: MutableMap<String, Int> = HashMap()
            for (trackSimple in album.tracks.items) {
                for (artistSimple in trackSimple.artists) {
                    if (quantitizedMap.keys.contains(artistSimple.id)) {
//                    int v = quantitizedMap.get (artistSimple);
//
//                    quantitizedMap.put (artistSimple, v++);
                    } else {
                        artists.add(artistSimple)
                        quantitizedMap[artistSimple.id] = 1
                    }
                }
            }
            return artists
        }
    }
}