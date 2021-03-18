package com.example.android.uamp

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import com.example.android.uamp.media.extensions.flag
import com.example.android.uamp.media.extensions.title

class MediaMetadataCompatComparator() : Comparator<MediaMetadataCompat> {
    private val naturalOrderComparator = NaturalOrderComparator()
    override fun compare(o1: MediaMetadataCompat?, o2: MediaMetadataCompat?): Int {
        if (o1!!.flag == o2!!.flag) {
            return naturalOrderComparator.compare(o1!!.title, o2!!.title)
        }

        if (o1!!.flag != MediaBrowserCompat.MediaItem.FLAG_PLAYABLE) {
            return -1
        }


        return 1
    }

}