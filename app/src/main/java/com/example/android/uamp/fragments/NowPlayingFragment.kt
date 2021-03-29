/*
 * Copyright 2019 Google Inc. All rights reserved.
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

package com.example.android.uamp.fragments

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.example.android.uamp.R
import com.example.android.uamp.databinding.FragmentNowplayingBinding
import com.example.android.uamp.utils.InjectorUtils
import com.example.android.uamp.viewmodels.MainActivityViewModel
import com.example.android.uamp.viewmodels.NowPlayingFragmentViewModel
import com.example.android.uamp.viewmodels.NowPlayingFragmentViewModel.NowPlayingMetadata

/**
 * A fragment representing the current media item being played.
 */


class NowPlayingFragment : Fragment() {

    private val mainActivityViewModel by activityViewModels<MainActivityViewModel> {
        InjectorUtils.provideMainActivityViewModel(requireContext())
    }
    private val nowPlayingViewModel by viewModels<NowPlayingFragmentViewModel> {
        InjectorUtils.provideNowPlayingFragmentViewModel(requireContext())
    }

    lateinit var binding: FragmentNowplayingBinding
    var touchingSeekBar = false

    companion object {
        fun newInstance() = NowPlayingFragment()
        val speeds = arrayOf(0.9F, 1.0F, 1.1F, 1.2F, 1.3F, 1.4F)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentNowplayingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Always true, but lets lint know that as well.
        val context = activity ?: return

        // Attach observers to the LiveData coming from this ViewModel
        nowPlayingViewModel.mediaMetadata.observe(viewLifecycleOwner,
            Observer { mediaItem -> updateUI(view, mediaItem) })
        nowPlayingViewModel.mediaButtonRes.observe(viewLifecycleOwner,
            Observer { res ->
                binding.mediaButton.setImageResource(res)
            })
        nowPlayingViewModel.mediaPosition.observe(viewLifecycleOwner,
            Observer { pos ->
                binding.position.text = NowPlayingMetadata.timestampToMSS(context, pos)
                if (!touchingSeekBar) {
                    binding.seekBar.progress = pos.toInt()
                }
            })

        mainActivityViewModel.musicServiceConnection.shuffleState.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                //    @IntDef({SHUFFLE_MODE_INVALID, SHUFFLE_MODE_NONE, SHUFFLE_MODE_ALL, SHUFFLE_MODE_GROUP})
                0 -> binding.shuffleButton.setImageResource(R.drawable.exo_controls_shuffle_off)
                1 -> binding.shuffleButton.setImageResource(R.drawable.exo_controls_shuffle_on)
            }
        })

        mainActivityViewModel.musicServiceConnection.repeatState.observe(viewLifecycleOwner, Observer { state ->
            //  @IntDef({REPEAT_MODE_INVALID, REPEAT_MODE_NONE, REPEAT_MODE_ONE, REPEAT_MODE_ALL,  REPEAT_MODE_GROUP})
            when (state) {
                0 -> binding.repeatButton.setImageResource(R.drawable.exo_controls_repeat_off)
                1 -> binding.repeatButton.setImageResource(R.drawable.exo_controls_repeat_one)
                2 -> binding.repeatButton.setImageResource(R.drawable.exo_controls_repeat_all)
            }
        })


        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                if (b) {
                    mainActivityViewModel.userPressSeekTo(i)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                touchingSeekBar = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                touchingSeekBar = false
            }
        })

        // Setup UI handlers for buttons
        binding.mediaButton.setOnClickListener {
            nowPlayingViewModel.mediaMetadata.value?.let { mainActivityViewModel.playMediaId(it.id) }
        }
        binding.repeatButton.setOnClickListener {
            mainActivityViewModel.setRepeat((mainActivityViewModel.musicServiceConnection.repeatState.value!! + 1) % 3)
        }
        binding.shuffleButton.setOnClickListener {
            mainActivityViewModel.setShuffle((mainActivityViewModel.musicServiceConnection.shuffleState.value!! + 1) % 2)
        }

        binding.rewindButton.setOnClickListener {
            nowPlayingViewModel.rewind()
        }
        binding.fastforwardButton.setOnClickListener {
            nowPlayingViewModel.fastForward()
        }

        binding.nextButton.setOnClickListener {
            nowPlayingViewModel.skipNext()
        }
        binding.prevButton.setOnClickListener {
            nowPlayingViewModel.skipPrev()
        }

        binding.speedButton.setOnClickListener {
            if (nowPlayingViewModel.isPlaying()) {
                nowPlayingViewModel.setPlaybackSpeed(speeds[(speeds.indexOf(speedFromPlayer) + 1) % speeds.size])
            }
        }

        // Initialize playback duration and position to zero
        binding.duration.text = NowPlayingMetadata.timestampToMSS(context, 0L)
        binding.position.text = NowPlayingMetadata.timestampToMSS(context, 0L)
    }

    var speedFromPlayer = 1.0F

    /**
     * Internal function used to update all UI elements except for the current item playback
     */
    private fun updateUI(view: View, metadata: NowPlayingMetadata) = with(binding) {
        if (metadata.albumArtUri == Uri.EMPTY) {
            albumArt.setImageResource(R.drawable.ic_album_black_24dp)
        } else {
            Glide.with(view)
                .load(metadata.albumArtUri)
                .into(albumArt)
        }
        title.text = metadata.title
        subtitle.text = metadata.subtitle
        duration.text = metadata.duration
        if (metadata.durationVal > 0) {
            seekBar.max = metadata.durationVal
        }
        speedFromPlayer = metadata.playbackSpeed
        if (metadata.playbackSpeed > 0) {
            speedButton.text = "${metadata.playbackSpeed}x"
            speedButton.isEnabled = true
        } else {
            speedButton.isEnabled = false
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainActivityViewModel.isShowing.value = true
    }
    override fun onDetach() {
        super.onDetach()
        mainActivityViewModel.isShowing.value = false
    }
}
