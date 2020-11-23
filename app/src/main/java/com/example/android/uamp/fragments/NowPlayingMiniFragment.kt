package com.example.android.uamp.fragments

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.example.android.uamp.R
import com.example.android.uamp.databinding.FragmentNowplayingBinding
import com.example.android.uamp.databinding.WidgetNowPlayingBinding
import com.example.android.uamp.utils.InjectorUtils
import com.example.android.uamp.viewmodels.MainActivityViewModel
import com.example.android.uamp.viewmodels.NowPlayingFragmentViewModel
import kotlinx.android.synthetic.main.widget_now_playing.*

class NowPlayingMiniFragment : Fragment() {

    private val mainActivityViewModel by activityViewModels<MainActivityViewModel> {
        InjectorUtils.provideMainActivityViewModel(requireContext())
    }
    private val nowPlayingViewModel by viewModels<NowPlayingFragmentViewModel> {
        InjectorUtils.provideNowPlayingFragmentViewModel(requireContext())
    }

    lateinit var binding: WidgetNowPlayingBinding



    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        binding = WidgetNowPlayingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.isVisible = false
        // Always true, but lets lint know that as well.
        val context = activity ?: return

        // Attach observers to the LiveData coming from this ViewModel
        nowPlayingViewModel.mediaMetadata.observe(viewLifecycleOwner,
                Observer { mediaItem ->
                    updateUI(view, mediaItem)
                })
        nowPlayingViewModel.mediaButtonRes.observe(viewLifecycleOwner,
                Observer { res ->
                    binding.playingPlaypause.setImageResource(res)
                })
        nowPlayingViewModel.mediaPosition.observe(viewLifecycleOwner,
                Observer { pos ->
                    binding.playingProgress.progress = pos.toInt()
                })


        // Setup UI handlers for buttons
        binding.playingPlaypause.setOnClickListener {
            nowPlayingViewModel.mediaMetadata.value?.let { mainActivityViewModel.playMediaId(it.id) }
        }


        binding.playingNext.setOnClickListener {
            nowPlayingViewModel.skipNext()
        }

        view.setOnClickListener {
            mainActivityViewModel.showFragment(NowPlayingFragment.newInstance())
            view.isVisible = false
        }

        mainActivityViewModel.isShowing.observe(viewLifecycleOwner, Observer { isShowing ->
            Log.i("Meow", "is showing changed to "+isShowing)
            if (isShowing) {
                view.isVisible = false
            } else {
                if (nowPlayingViewModel.mediaMetadata.value != null) {
                    view.isVisible = true
                    updateUI(view, nowPlayingViewModel.mediaMetadata.value!!)
                }
            }
        })

    }

    /**
     * Internal function used to update all UI elements except for the current item playback
     */
    private fun updateUI(view: View, metadata: NowPlayingFragmentViewModel.NowPlayingMetadata) = with(binding) {
        Log.i("Meow", "updateUI")
        if (!mainActivityViewModel.isShowing.value!!) {
            Log.i("Meow", "not isShowing")
            view.isVisible = true
            if (metadata.albumArtUri == Uri.EMPTY) {
                playingArt.setImageResource(R.drawable.ic_album_black_24dp)
            } else {
                Glide.with(view)
                        .load(metadata.albumArtUri)
                        .into(playing_art)
            }
            playingTitle.text = metadata.title
            playingSubtitle.text = metadata.subtitle

            playingTitle.isSelected = true

            if (metadata.durationVal > 0) {
                playing_progress.max = metadata.durationVal
            }
        } else {
            Log.i("Meow", "isShowing")
            view.isVisible = false
        }
    }
}
