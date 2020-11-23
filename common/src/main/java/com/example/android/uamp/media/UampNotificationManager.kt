/*
 * Copyright 2020 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.uamp.media

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.net.Uri
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.android.uamp.MusicApplication
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.ui.PlayerNotificationManager.MediaDescriptionAdapter
import com.google.android.exoplayer2.util.NotificationUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import java.util.Arrays
import java.util.HashMap


const val NOW_PLAYING_CHANNEL_ID = "com.example.android.uamp.media.NOW_PLAYING"
const val NOW_PLAYING_NOTIFICATION_ID = 0xb339 // Arbitrary number used to identify our notification

/**
 * A wrapper class for ExoPlayer's PlayerNotificationManager. It sets up the notification shown to
 * the user during audio playback and provides track metadata, such as track title and icon image.
 */
class UampNotificationManager(
    private val context: Context,
    sessionToken: MediaSessionCompat.Token,
    notificationListener: PlayerNotificationManager.NotificationListener
) {

    private var player: Player? = null
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private val notificationManager: PlayerNotificationManager
    private val platformNotificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager


    init {
        val mediaController = MediaControllerCompat(context, sessionToken)



        notificationManager = createWithNotificationChannel(
            context,
            NOW_PLAYING_CHANNEL_ID,
            R.string.notification_channel,
            R.string.notification_channel_description,
            NOW_PLAYING_NOTIFICATION_ID,
            DescriptionAdapter(mediaController),
            notificationListener,
            CatActionReceiver(mediaController)
        ).apply {
            setMediaSessionToken(sessionToken)
            setSmallIcon(R.drawable.ic_notification)

            // Don't display the rewind or fast-forward buttons.
            setRewindIncrementMs(0)
            setFastForwardIncrementMs(0)
            setUseNavigationActionsInCompactView(true)
        }
    }

    class CatActionReceiver(val mediaController: MediaControllerCompat) : PlayerNotificationManager.CustomActionReceiver {
        override fun createCustomActions(
            context: Context,
            instanceId: Int
        ): Map<String, NotificationCompat.Action> {
            val ret: MutableMap<String, NotificationCompat.Action> = HashMap()

            val intent: Intent = Intent(STEP_FORWARD).setPackage(context.packageName)
            val pendingIntent = PendingIntent.getBroadcast(
                context, instanceId, intent, PendingIntent.FLAG_CANCEL_CURRENT
            )

            val intent2: Intent = Intent(STEP_BACK).setPackage(context.packageName)
            val pendingIntent2 = PendingIntent.getBroadcast(
                context, instanceId, intent2, PendingIntent.FLAG_CANCEL_CURRENT
            )

            ret[STEP_FORWARD] = NotificationCompat.Action.Builder(
                R.drawable.exo_icon_fastforward,
                "Only in wearable",
                pendingIntent
            )
                .build()
            ret[STEP_BACK] = NotificationCompat.Action.Builder(
                R.drawable.exo_icon_rewind,
                "Only in wearable",
                pendingIntent2
            )
                .build()
            return ret
        }

        override fun getCustomActions(player: Player): List<String> {
            return customActions
        }

        override fun onCustomAction(player: Player, action: String, intent: Intent) {
            Log.i("Meow", "Custom Action")
            when (action) {
                STEP_FORWARD -> {
                    Log.i("Meow", STEP_FORWARD)
                    mediaController.transportControls.fastForward()
                }
                STEP_BACK -> {
                    Log.i("Meow", STEP_BACK)
                    mediaController.transportControls.rewind()
                }
            }
        }

        companion object {
            const val STEP_BACK = "Back"
            const val STEP_FORWARD = "Forward"
            val customActions =
                Arrays.asList(
                    STEP_BACK, STEP_FORWARD
                )

        }
    }

    fun createWithNotificationChannel(
        context: Context?,
        channelId: String?,
        @StringRes channelName: Int,
        @StringRes channelDescription: Int,
        notificationId: Int,
        mediaDescriptionAdapter: MediaDescriptionAdapter?,
        notificationListener: PlayerNotificationManager.NotificationListener?,
        customActionReceiver: PlayerNotificationManager.CustomActionReceiver?
    ): PlayerNotificationManager {
        NotificationUtil.createNotificationChannel(
            context!!,
            channelId!!,
            channelName,
            channelDescription,
            NotificationUtil.IMPORTANCE_LOW
        )
        return PlayerNotificationManager(
            context!!, channelId!!, notificationId,
            mediaDescriptionAdapter!!, notificationListener,
            customActionReceiver
        )
    }

    fun hideNotification() {
        notificationManager.setPlayer(null)
    }

    fun showNotificationForPlayer(player: Player){
        notificationManager.setPlayer(player)
    }

    private inner class DescriptionAdapter(private val controller: MediaControllerCompat) :
        PlayerNotificationManager.MediaDescriptionAdapter {

        var currentIconUri: Uri? = null
        var currentBitmap: Bitmap? = null

        override fun createCurrentContentIntent(player: Player): PendingIntent? =
            controller.sessionActivity

        override fun getCurrentContentText(player: Player) =
            controller.metadata.description.subtitle.toString()

        override fun getCurrentContentTitle(player: Player) =
            controller.metadata.description.title.toString()

        override fun getCurrentLargeIcon(
            player: Player,
            callback: PlayerNotificationManager.BitmapCallback
        ): Bitmap? {
            return getBitmapFromVectorDrawable(MusicApplication.getInstance(), R.drawable.ic_album)

            /*val iconUri = controller.metadata.description.iconUri
            return if (currentIconUri != iconUri || currentBitmap == null) {

                // Cache the bitmap for the current song so that successive calls to
                // `getCurrentLargeIcon` don't cause the bitmap to be recreated.
                currentIconUri = iconUri
                serviceScope.launch {
                    currentBitmap = iconUri?.let {
                        resolveUriAsBitmap(it)
                    }
                    currentBitmap?.let { callback.onBitmap(it) }
                }
                null
            } else {
                currentBitmap
            }*/
        }

        fun getBitmapFromVectorDrawable(context: Context?, drawableId: Int): Bitmap {
            var drawable: Drawable? = context?.let { ContextCompat.getDrawable(it, drawableId) }
            val bitmap = Bitmap.createBitmap(
                drawable!!.intrinsicWidth,
                drawable!!.intrinsicHeight, Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight())
            drawable.draw(canvas)
            return bitmap
        }

        private suspend fun resolveUriAsBitmap(uri: Uri): Bitmap? {
            return withContext(Dispatchers.IO) {
                // Block on downloading artwork.
                Glide.with(context).applyDefaultRequestOptions(glideOptions)
                    .asBitmap()
                    .error(R.drawable.ic_album)
                    .load(uri)
                    .submit(NOTIFICATION_LARGE_ICON_SIZE, NOTIFICATION_LARGE_ICON_SIZE)
                    .get()
            }
        }
    }
}

const val NOTIFICATION_LARGE_ICON_SIZE = 144 // px

private val glideOptions = RequestOptions()
    .fallback(R.drawable.default_art)
    .diskCacheStrategy(DiskCacheStrategy.DATA)

private const val MODE_READ_ONLY = "r"
