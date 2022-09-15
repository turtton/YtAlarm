package net.turtton.ytalarm.fragment

import android.content.Context
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.snackbar.Snackbar
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.turtton.ytalarm.MainActivity
import net.turtton.ytalarm.R
import net.turtton.ytalarm.YtApplication.Companion.repository
import net.turtton.ytalarm.databinding.FragmentVideoPlayerBinding
import net.turtton.ytalarm.viewmodel.PlaylistViewModel
import net.turtton.ytalarm.viewmodel.PlaylistViewModelFactory
import net.turtton.ytalarm.viewmodel.VideoViewModel
import net.turtton.ytalarm.viewmodel.VideoViewModelFactory

class FragmentVideoPlayer : Fragment() {
    private val args: FragmentVideoPlayerArgs by navArgs()
    private var _binding: FragmentVideoPlayerBinding? = null
    private val videoViewModel: VideoViewModel by viewModels {
        VideoViewModelFactory(requireActivity().application.repository)
    }
    private val playlistViewModel: PlaylistViewModel by viewModels {
        PlaylistViewModelFactory(requireActivity().application.repository)
    }

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVideoPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // FullScreen
        if (Build.VERSION.SDK_INT >= 30) {
            val windowInsetsController = view.windowInsetsController!!
            windowInsetsController.systemBarsBehavior =
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            windowInsetsController.hide(WindowInsets.Type.systemBars())
        } else {
            val window = (requireActivity() as MainActivity).window
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        }
        // set audio
        val audioManager = requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val musicStream = AudioManager.STREAM_MUSIC
        val maxVolume = audioManager.getStreamMaxVolume(musicStream)
        audioManager.setStreamVolume(musicStream, maxVolume, AudioManager.FLAG_PLAY_SOUND)

        // hide fab
        (requireActivity() as MainActivity).binding.fab.visibility = View.GONE

        val snoozeButton = binding.fragmentVideoPlayerButtonSnooze
        val dismissButton = binding.fragmentVideoPlayerButtonDismiss
        val videoView = binding.videoView
        val timeView = binding.fragmentVideoPlayerTextTime

        // speed fix
        videoView.setOnPreparedListener {
            it.playbackParams.speed = 1.0f
            it.setOnInfoListener { _, what, _ ->
                when (what) {
                    MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START -> {
                        videoView.background = null
                        true
                    }
                    else -> false
                }
            }
        }

        if (findNavController().backQueue.isNotEmpty()) {
            timeView.visibility = View.GONE
            snoozeButton.visibility = View.GONE
            dismissButton.text = view.context.getText(R.string.fragment_video_player_button_stop)
            dismissButton.setOnClickListener {
                findNavController().popBackStack()
            }
        } else {
            TODO()
        }

        val id = args.id
        if (args.isList) {
            TODO()
        } else {
            lifecycleScope.launch rootLaunch@{
                val video = videoViewModel.getFromIdAsync(id).await()
                if (video == null) {
                    Snackbar.make(view, "Error! Cannot get video data.", Snackbar.LENGTH_LONG)
                        .setAction("Action", null)
                        .show()
                    return@rootLaunch
                }
                val url = video.videoUrl
                Glide.with(view).load(video.thumbnailUrl.toUri())
                    .into(object : CustomViewTarget<View, Drawable>(view) {
                        override fun onResourceReady(
                            resource: Drawable,
                            transition: Transition<in Drawable>?
                        ) {
                            videoView.background = resource
                        }

                        override fun onLoadFailed(errorDrawable: Drawable?) {}
                        override fun onResourceCleared(placeholder: Drawable?) {}
                    })
                val infoResult = withContext(Dispatchers.IO) {
                    val request = YoutubeDLRequest(url)
                    request.addOption("-f", "best")
                    kotlin.runCatching {
                        YoutubeDL.getInstance().getInfo(request)
                    }
                }
                launch(Dispatchers.Main) {
                    infoResult.onSuccess {
                        val videoUrl = it.url
                        if (videoUrl.isNullOrEmpty()) {
                            Snackbar.make(
                                view,
                                "Error! Failed to get video stream url!!",
                                Snackbar.LENGTH_LONG
                            )
                                .setAction("Action", null)
                                .show()
                            return@launch
                        } else {
                            binding.progressBar.visibility = View.GONE
                            videoView.setVideoURI(videoUrl.toUri())
                            videoView.start()
                        }
                    }.onFailure {
                        Snackbar.make(view, "Error! Cannot get video data!!", Snackbar.LENGTH_LONG)
                            .setAction("Action", null)
                            .show()
                        Log.e("YtAram", "failed to get stream info", it)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        (requireActivity() as MainActivity).binding.fab.visibility = View.VISIBLE

        val audioManager = requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val musicStream = AudioManager.STREAM_MUSIC
        audioManager.setStreamVolume(musicStream, 0, AudioManager.FLAG_PLAY_SOUND)

        val window = (requireActivity() as MainActivity).window
        val insetsController = WindowCompat.getInsetsController(window, requireView())
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.show(WindowInsetsCompat.Type.systemBars())
    }
}