package com.guavaapps.spotlight

import android.animation.ArgbEvaluator
import com.guavaapps.components.Components.getPx
import com.guavaapps.spotlight.ColorSet.Companion.create
import com.google.android.material.tabs.TabLayout
import androidx.viewpager2.widget.ViewPager2
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import retrofit.RetrofitError
import android.view.View.OnLayoutChangeListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.view.ViewGroup.MarginLayoutParams
import android.os.Looper
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.tabs.TabLayoutMediator.TabConfigurationStrategy
import android.text.SpannableString
import android.animation.ValueAnimator
import android.text.style.ForegroundColorSpan
import android.text.Spanned
import android.content.res.ColorStateList
import android.os.Handler
import android.view.View
import androidx.core.graphics.Insets
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.pixel.spotifyapi.Objects.*
import retrofit.Callback
import retrofit.client.Response
import java.util.ArrayList
import java.util.HashMap

class ExtraFragment : Fragment() {
    private val viewModel: ContentViewModel by viewModels { ContentViewModel.Factory }

    private var mInsets: Insets? = null

    private var mColorSet = ColorSet()

    private var mTrack: TrackWrapper? = null
    private var mAlbum: AlbumWrapper? = null

    private val mArtists: MutableList<ArtistWrapper> = ArrayList()

    private var mAdapter: Adapter? = null

    private lateinit var mTabLayout: TabLayout

    private lateinit var mViewPager: ViewPager2

    private var mAlbumFragment: AlbumFragment? = null

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
        mTabLayout = view.findViewById(R.id.tab_layout)
        mViewPager = view.findViewById(R.id.view_pager)
        mTabLayout.setTabMode(TabLayout.MODE_SCROLLABLE)
        mTabLayout.setTabGravity(TabLayout.GRAVITY_CENTER)
        mAlbumFragment = AlbumFragment()
        mAlbumFragment!!.setListener { track: TrackSimple ->
            viewModel!!.spotifyService.value!!
                .getTrack(track.id, object : Callback<Track> {
                    override fun success(track: Track, response: Response) {
                        val wrappedTrack = TrackWrapper(track, viewModel!!.album.value!!.bitmap)
                        viewModel!!.track.setValue(wrappedTrack)
                        viewModel!!.spotifyAppRemote.value!!
                            .getPlayerApi()
                            .play(track.uri)
                    }

                    override fun failure(error: RetrofitError) {}
                })
        }
        mAdapter = Adapter(requireActivity())
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
        viewModel!!.track.observe(viewLifecycleOwner, Observer { trackWrapper: TrackWrapper? ->
            if (trackWrapper != null) {
                mTrack = trackWrapper
                if (mAlbumFragment!!.album == null || mAlbumFragment!!.album.album.id != mTrack!!.track.album.id) {
                    AppRepo.getInstance() // TODO use view model
                        .getAlbum(viewModel!!.spotifyService.value, context,
                            trackWrapper.track.album.id,
                            object : AppRepo.ResultListener() {
                                override fun onAlbum(albumWrapper: AlbumWrapper) {
                                    super.onAlbum(albumWrapper)
                                    Handler.createAsync(Looper.getMainLooper())
                                        .post {
                                            mViewPager.setAdapter(mAdapter)
                                            TabLayoutMediator(mTabLayout,
                                                mViewPager,
                                                TabConfigurationStrategy { tab: TabLayout.Tab, position: Int ->
                                                    if (position == 0) {
                                                        tab.text =
                                                            "From " + mTrack!!.track.album.name
                                                    } else tab.text =
                                                        mArtists[position - 1].artist.name
                                                }).attach()
                                        }
                                }
                            })
                }
            }
        })
        viewModel!!.album.observe(viewLifecycleOwner, Observer { albumWrapper: AlbumWrapper? ->
            if (albumWrapper != null) {
                mAlbum = albumWrapper
                nextAlbum(albumWrapper)
                val tabs: MutableList<Fragment> = ArrayList()
                tabs.add(mAlbumFragment!!)
                mArtists.clear()
                for (artistSimple in getAllArtists(mAlbum!!.album)) { //mAlbum.album.artists) {
                    val artist = Artist()
                    artist.id = artistSimple.id
                    artist.name = artistSimple.name
                    mArtists.add(ArtistWrapper(artist, null))
                    tabs.add(ArtistFragment())
                }
                mAdapter!!.setItems(tabs)
            }
        })
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
        val albumTabTitle = SpannableString("From " + album.name)
        val primaryAnimator =
            ValueAnimator.ofObject(ArgbEvaluator(), mColorSet.primary, colorSet.primary)
        primaryAnimator.duration =
            resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
        primaryAnimator.addUpdateListener { animation: ValueAnimator ->
            mColorSet.primary = animation.animatedValue as Int
            mTabLayout!!.setTabTextColors(mColorSet.primary, animation.animatedValue as Int)
            mTabLayout!!.setSelectedTabIndicatorColor(mColorSet.primary)
            albumTabTitle.setSpan(ForegroundColorSpan(mColorSet.primary),
                5,
                albumTabTitle.length,
                Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        }
        primaryAnimator.start()
        val secondaryAnimator =
            ValueAnimator.ofObject(ArgbEvaluator(), mColorSet.secondary, colorSet.secondary)
        secondaryAnimator.duration =
            resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
        secondaryAnimator.addUpdateListener { animation: ValueAnimator ->
            mColorSet.primary = animation.animatedValue as Int
            mTabLayout!!.setTabTextColors(mColorSet.secondary, mColorSet.primary)
            albumTabTitle.setSpan(ForegroundColorSpan(mColorSet.secondary),
                0,
                4,
                Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        }
        secondaryAnimator.start()
        val tertiaryAnimator =
            ValueAnimator.ofObject(ArgbEvaluator(), mColorSet.tertiary, colorSet.tertiary)
        tertiaryAnimator.duration =
            resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
        tertiaryAnimator.addUpdateListener { animation: ValueAnimator ->
            mColorSet.tertiary = animation.animatedValue as Int
        }
        tertiaryAnimator.start()
        val rippleAnimator =
            ValueAnimator.ofObject(ArgbEvaluator(), mColorSet.ripple, colorSet.ripple)
        rippleAnimator.duration =
            resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
        rippleAnimator.addUpdateListener { animation: ValueAnimator ->
            mColorSet.ripple = animation.animatedValue as Int
            mTabLayout!!.tabRippleColor =
                ColorStateList(arrayOf(intArrayOf()), intArrayOf(mColorSet.ripple))
        }
        rippleAnimator.start()
        mColorSet = colorSet
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