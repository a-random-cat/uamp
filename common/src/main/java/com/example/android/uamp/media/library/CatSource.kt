package com.example.android.uamp.media.library

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.util.Log
import com.example.android.uamp.MediaMetadataCompatComparator
import com.example.android.uamp.MusicApplication
import com.example.android.uamp.media.R
import com.example.android.uamp.media.extensions.album
import com.example.android.uamp.media.extensions.albumArtUri
import com.example.android.uamp.media.extensions.artist
import com.example.android.uamp.media.extensions.displayDescription
import com.example.android.uamp.media.extensions.displaySubtitle
import com.example.android.uamp.media.extensions.displayTitle
import com.example.android.uamp.media.extensions.duration
import com.example.android.uamp.media.extensions.flag
import com.example.android.uamp.media.extensions.id
import com.example.android.uamp.media.extensions.mediaUri
import com.example.android.uamp.media.extensions.title
import com.example.android.uamp.media.extensions.trackNumber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.StringBuilder

class CatSource(val context: Context) : AbstractMusicSource() {

    var allAudio: MutableList<MediaMetadataCompat> = mutableListOf()

   // var catalog: List<Pair<String, List<MediaMetadataCompat>>> = emptyList()
    val mapping = HashMap<String, MutableList<MediaMetadataCompat>>()

    operator fun get(mediaId: String) = mapping[mediaId]
    val searchableByUnknownCaller = true

    init {
        state = STATE_INITIALIZING
    }

    // Not using this
    override fun iterator(): Iterator<MediaMetadataCompat> = allAudio.iterator()

    override suspend fun load() {
        updateCatalog()?.let { updatedCatalog ->
          //  catalog = updatedCatalog
            state = STATE_INITIALIZED
        } ?: run {
           // catalog = emptyList()
            state = STATE_ERROR
        }
    }

    /**
     * Function to connect to a remote URI and download/process the JSON file that corresponds to
     * [MediaMetadataCompat] objects.
     */
    private suspend fun updateCatalog() {
        mapping[UAMP_BROWSABLE_ROOT] = mutableListOf()
        withContext(Dispatchers.IO) {
            val pathCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.RELATIVE_PATH
            } else {
                MediaStore.Audio.Media.DATA
            }

            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.TRACK,
                MediaStore.Audio.Media.ALBUM,
                pathCode
            )


// Show only videos that are at least 5 minutes in duration.
            val selection = "${pathCode} LIKE ? OR ${pathCode} LIKE ?"
            val selectionArgs = arrayOf(
                "Music/%",
                "%/Music/%"
            )

// Display videos in alphabetical order based on their display name.
            val sortOrder = "${pathCode} ASC"

            val query = MusicApplication.getInstance().contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )
            query?.use { cursor ->
                // Cache column indices.
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val nameColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
               val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.ArtistColumns.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
             //   val genreColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.GenresColumns.NAME)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val trackNumberColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
              //  val trackCountColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.ArtistColumns.NUMBER_OF_TRACKS)
               // val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val pathColumn = cursor.getColumnIndexOrThrow(pathCode)

                val headPath = "/Music/"
                val lenHeadPath = headPath.length

                while (cursor.moveToNext()) {
                    // Get values of columns for a given video.
                    val idVal = cursor.getLong(idColumn)
                    val titleVal = cursor.getString(nameColumn)
                    val artistVal = cursor.getString(artistColumn)
                    val albumVal = cursor.getString(albumColumn)

                    var pathVal = cursor.getString(pathColumn)


                    pathVal = pathVal.substring(pathVal.indexOf(headPath)+lenHeadPath)
                    buildPathTo(pathVal)

                    val parentIndex = pathVal.lastIndexOf("/")
                    val parentPath = if (parentIndex == -1) "/" else pathVal.substring(0, parentIndex)

                    val newItem = MediaMetadataCompat.Builder().apply {
                        id = idVal.toString()
                        title = titleVal
                        artist = artistVal
                        album = albumVal
                        duration = cursor.getLong(durationColumn)

                        // genre = cursor.getString(genreColumn)
                        mediaUri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            idVal
                        ).toString()

                        albumArtUri = RESOURCE_ROOT_URI + MusicApplication.getInstance().resources.getResourceEntryName(R.drawable.ic_album)
                        trackNumber = cursor.getLong(trackNumberColumn)
                        //    trackCount = cursor.getLong(trackCountColumn)
                        flag = MediaBrowserCompat.MediaItem.FLAG_PLAYABLE

                        // To make things easier for *displaying* these, set the display properties as well.
                        displayTitle = titleVal
                        displaySubtitle = artistVal
                        displayDescription = albumVal
                        //displayIconUri = jsonMusic.image
                    }.build()


                    mapping[parentPath]!!.add(newItem)
                    allAudio.add(newItem)


                }
            }

            mapping.values.forEach {
                it.sortWith(MediaMetadataCompatComparator())
            }


            // Add description keys to be used by the ExoPlayer MediaSession extension when
            // announcing metadata changes.
           // mediaMetadataCompats.forEach { it.description.extras?.putAll(it.bundle) }

          //  mapping.keys.map { Pair (it, mapping[it]!!)}
        }
    }

    private fun buildPathTo(pathVal:String) {
        val parts = pathVal.split("/")
        val lastIndex = parts.size-1

        var builder = StringBuilder()
        for ((index, part) in parts.withIndex()) {
            if (index < lastIndex) {
                var parentPath = builder.toString()
                if (index > 0) {
                    builder = builder.append("/")
                }
                if (parentPath == "") {
                    parentPath = UAMP_BROWSABLE_ROOT
                }
                builder = builder.append(part)
                val path = builder.toString()
                if (!mapping.containsKey(path)) {
                    val albumMetadata = MediaMetadataCompat.Builder().apply {
                        id = path
                        title = part
                        albumArtUri = RESOURCE_ROOT_URI + context.resources.getResourceEntryName(R.drawable.ic_album)
                        flag = MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
                    }.build()

                    mapping[parentPath]!!.add(albumMetadata)
                    mapping[path] = mutableListOf()
                }
            }
        }
    }
}


const val UAMP_BROWSABLE_ROOT = "/"
const val UAMP_EMPTY_ROOT = "@empty@"
const val UAMP_RECOMMENDED_ROOT = "__RECOMMENDED__"
const val UAMP_ALBUMS_ROOT = "__ALBUMS__"
const val UAMP_RECENT_ROOT = "__RECENT__"

const val MEDIA_SEARCH_SUPPORTED = "android.media.browse.SEARCH_SUPPORTED"

const val RESOURCE_ROOT_URI = "android.resource://com.example.android.uamp.next/drawable/"