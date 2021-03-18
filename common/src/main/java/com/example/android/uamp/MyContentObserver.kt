package com.example.android.uamp

import android.database.ContentObserver
import android.net.Uri
import android.os.Handler


class MyContentObserver(handler: Handler?, val callback:() -> Unit) : ContentObserver(handler) {

    override fun onChange(selfChange: Boolean) {
        this.onChange(selfChange, null)
    }

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        callback()
    }
}