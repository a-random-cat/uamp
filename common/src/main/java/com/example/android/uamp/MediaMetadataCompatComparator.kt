package com.example.android.uamp

import android.support.v4.media.MediaMetadataCompat
import com.example.android.uamp.media.extensions.displayTitle

class MediaMetadataCompatComparator() : Comparator<MediaMetadataCompat> {
    private val naturalOrderComparator = NaturalOrderComparator()
    override fun compare(o1: MediaMetadataCompat?, o2: MediaMetadataCompat?): Int {
        return naturalOrderComparator.compare(o1!!.displayTitle, o2!!.displayTitle)
    }

}