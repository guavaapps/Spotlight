package com.guavaapps.spotlight

import com.pixel.spotifyapi.Objects.Artist
import com.pixel.spotifyapi.Objects.Track
import com.pixel.spotifyapi.SpotifyService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "Grapher"

// gets related artists and tracks to the ones previously listened
class Grapher(private val spotifyService: SpotifyService) {

    // get related artists and tracks based on the last batch
    // take tracks
    // get all artists and their genres
    // sort artists by most accepted genre
    // take artists from the top middle and bottom
    // for each artist
    suspend fun forBatch(tracks: List<Track>): GrapherObject {
        // to reduce the number of api calls, get all artists simultaneously
        // list of all ArtistSimple objects
        val allArtists = tracks.flatMap { it.artists }

        // get the full Artist objects (with genres)
        // api can handle a max of 50 artists ids
        val a = withContext(Dispatchers.IO) {
            allArtists.chunked(50) {
                spotifyService.getArtists(it.joinToString(",") { it.id }).artists
            }
        }
            .flatten()
            .distinctBy { it.id }

        // sort genres and artists
        val (sortedGenres, sortedArtists) = rank(a)

        val (hGenre, mGenre, lGenre) = selectGenres(sortedGenres)

        // get artists related to the selected artists
        val (hArtists, mArtists, lArtists) = getRelated(sortedArtists)


        val h = GrapherSeed(
            hGenre, hArtists
        )

        val m = GrapherSeed(
            mGenre, mArtists
        )

        val l = GrapherSeed(
            lGenre, lArtists
        )

        return GrapherObject(h, m, l)
    }

    suspend fun createFirst(): Pair<GrapherObject, GrapherObject> {
        return Pair(createFromHistory(), createFromRecentlyPlayed())
    }

    // create seeds based on the user's long term listening history
    private suspend fun createFromHistory() = withContext(Dispatchers.IO) {
        // query maps to get long term history
        val paramsShort = mapOf(
            // api limit <= 50
            "limit" to 50,
            "time_range" to "short_term"
        )

        val paramsMedium = mapOf(
            "limit" to 50,
            "time_range" to "medium_term"
        )

        val paramsLong = mapOf(
            "limit" to 50,
            "time_range" to "long_term"
        )

        // get top tracks, genres and artists
        // short term history ~last 4 weeks
        val shortArtists = spotifyService.getTopArtists(paramsShort).items.take(2)
        val shortGenre = shortArtists.flatMap { it.genres.distinct() }.first()

        // ~last 30 days
        val medArtists = spotifyService.getTopArtists(paramsMedium).items.take(2)
        val medGenre = medArtists.flatMap { it.genres.distinct() }.first()

        // ~last 6 months
        val longArtists = spotifyService.getTopArtists(paramsLong).items.take(2)
        val longGenre = longArtists.flatMap { it.genres.distinct() }.first()

        return@withContext GrapherObject(
            GrapherSeed(shortGenre, shortArtists),
            GrapherSeed(medGenre, medArtists),
            GrapherSeed(longGenre, longArtists)
        )
    }

    // create seeds from the user's recently played tracks
    private suspend fun createFromRecentlyPlayed(): GrapherObject {
        // get recently played tracks
        val recentlyPlayed = withContext(Dispatchers.IO) {
            spotifyService.getRecentlyPlayedTracks(mapOf("limit" to 50)).items
        }

        // get all artists
        val artists = withContext(Dispatchers.IO) {
            spotifyService.getArtists(recentlyPlayed.joinToString(",") { it.track.artists.first().id }).artists
        }

        // sort
        val (sortedGenres, sortedArtists) = rank(artists)

        val (hg, mg, lg) = selectGenres(sortedGenres)

        // get 4 artists from each section (top, middle and bottom)
        val (ha, ma, la) = selectArtists(sortedArtists, 4)

        return GrapherObject(
            GrapherSeed(hg, ha),
            GrapherSeed(mg, ma),
            GrapherSeed(lg, la),
        )
    }

    // get artists related to the ones provided
    private suspend fun getRelated(
        artists: List<Artist>,
    ): Triple<MutableList<Artist>, MutableList<Artist>, MutableList<Artist>> {
        // select the top, median and lowest ranked artists
        // returns one-item list
        val (high, med, low) = selectArtists(artists)

        // get related artists
        val related = withContext(Dispatchers.IO) {
            Triple(
                spotifyService.getRelatedArtists(high.first().id).artists,
                spotifyService.getRelatedArtists(med.first().id).artists,
                spotifyService.getRelatedArtists(low.first().id).artists,
            )
        }

        // make sure there is no repeating artists
        related.first.filterNot {
            artists.contains(it) || (related.second + related.third).contains(it)
        }
        related.second.filterNot {
            artists.contains(it) || (related.first + related.third).contains(it)
        }
        related.third.filterNot {
            artists.contains(it) || (related.first + related.second).contains(it)
        }

        return related
    }

    // select the top, median and lowest genres
    private fun selectGenres(genres: List<String>) = Triple(
        genres.first(),
        genres[genres.size / 2],
        genres.last()
    )

    // select the given number of artists from the top,
    // middle and bottom of the given list
    private fun selectArtists(
        artists: List<Artist>,
        count: Int = 1,
    ): Triple<List<Artist>, List<Artist>, List<Artist>> {
        val high = artists.take(count)

        val m = artists.size / 2 - (count - 1) / 2
        val med = artists.subList(m, m + count)

        val low = artists.takeLast(count)

        return Triple(high, med, low)
    }

    // sort artists based on the frequency of their genres in the given list
    private fun rank(artists: List<Artist>): Pair<List<String>, List<Artist>> {
        val map = mutableMapOf<String, MutableList<Artist>>()
        val countMap = mutableMapOf<String, Int>()

        // group artists by genre and determine the frequency of each genre
        artists.forEach { artist ->
            artist.genres.distinct().forEach {
                if (!countMap.containsKey(it)) {
                    countMap[it] = 1
                    map[it] = mutableListOf(artist)
                } else {
                    countMap[it] = countMap[it]!! + 1
                    if (!map[it]!!.contains(artist)) map[it]!!.add(artist)
                }
            }
        }

        // sort genres by frequency
        val genres = countMap.keys.sortedBy { countMap[it] }

        // sort artists by genre
        val artists = genres.map { map[it]!!.toList() }.flatten()
            .distinctBy { it.id }

        return Pair(genres, artists)
    }
}

// contains high, med and low rank seeds
data class GrapherObject(
    var high: GrapherSeed,
    var med: GrapherSeed,
    var low: GrapherSeed,
) {

    // convert GrapherSeed objects to maps
    // api call can contain a max of 5 seeds
    // were gonna be using 1 genre 4 artist seeds
    // since we want to be as specific as possible i.e genre seeds are too vague
    fun createParamsObjects(): List<Map<String, String>> {
        val h = mapOf(
            "seed_genres" to high.genre,
            "seed_artists" to high.artists.take(4).joinToString(",") { it.id },
        )

        val m = mapOf(
            "seed_genres" to med.genre,
            "seed_artists" to med.artists.take(4).joinToString(",") { it.id },
        )

        val l = mapOf(
            "seed_genres" to low.genre,
            "seed_artists" to low.artists.take(4).joinToString(",") { it.id },
        )

        return listOf(h, m, l)
    }

    // remove any seeds containing previously listened to artists
    fun filter (artists: List<String>) {
        high.artists = high.artists.filterNot { artists.contains(it.id) }
        med.artists = med.artists.filterNot { artists.contains(it.id) }
        low.artists = low.artists.filterNot { artists.contains(it.id) }
    }
}

// object containing the genre and all the possible artist seeds
data class GrapherSeed(
    var genre: String = "",
    var artists: List<Artist> = emptyList(),
)