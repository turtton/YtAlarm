package net.turtton.ytalarm.fragment

import android.content.Context
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatActivity
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
import net.turtton.ytalarm.structure.Video
import net.turtton.ytalarm.util.RepeatType
import net.turtton.ytalarm.util.UpdateSnoozeNotifyWorker
import net.turtton.ytalarm.util.observeAlarm
import net.turtton.ytalarm.viewmodel.AlarmViewModel
import net.turtton.ytalarm.viewmodel.AlarmViewModelFactory
import net.turtton.ytalarm.viewmodel.PlaylistViewModel
import net.turtton.ytalarm.viewmodel.PlaylistViewModelFactory
import net.turtton.ytalarm.viewmodel.VideoViewModel
import net.turtton.ytalarm.viewmodel.VideoViewModelFactory
import java.util.Calendar
import kotlin.math.roundToInt

class FragmentVideoPlayer : Fragment() {
    private val args: FragmentVideoPlayerArgs by navArgs()
    private var _binding: FragmentVideoPlayerBinding? = null
    private val videoViewModel: VideoViewModel by viewModels {
        VideoViewModelFactory(requireActivity().application.repository)
    }
    private val playlistViewModel: PlaylistViewModel by viewModels {
        PlaylistViewModelFactory(requireActivity().application.repository)
    }

    private val alarmViewModel: AlarmViewModel by viewModels {
        AlarmViewModelFactory(requireActivity().application.repository)
    }

    private val binding get() = _binding!!

    private val musicStream = AudioManager.STREAM_MUSIC
    private lateinit var audioManager: AudioManager
    private var currentVolume: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVideoPlayerBinding.inflate(inflater, container, false)

        audioManager = requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        alarmViewModel.allAlarms.observeAlarm(this, view.context)

        // FullScreen
        val activity = requireActivity()
        if (Build.VERSION.SDK_INT >= 30) {
            val windowInsetsController = view.windowInsetsController!!
            windowInsetsController.systemBarsBehavior =
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            windowInsetsController.hide(WindowInsets.Type.systemBars())
        } else {
            val window = (activity as AppCompatActivity).window
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        }

        // hide fab
        if (activity is MainActivity) {
            activity.binding.fab.visibility = View.GONE
        }

        val snoozeButton = binding.fragmentVideoPlayerButtonSnooze
        val dismissButton = binding.fragmentVideoPlayerButtonDismiss
        val videoView = binding.videoView
        val timeView = binding.fragmentVideoPlayerTextTime

        videoView.setOnPreparedListener {
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

        val isAlarm = args.isAlarm
        if (!isAlarm) {
            timeView.visibility = View.GONE
            snoozeButton.visibility = View.GONE
            dismissButton.text = view.context.getText(R.string.fragment_video_player_button_stop)
        }
        dismissButton.setOnClickListener {
            if (!findNavController().navigateUp()) {
                activity.finish()
            }
        }

        val id = args.id
        if (isAlarm) {
            val alarmId = id.toLong()
            if (alarmId == -1L) {
                Snackbar.make(view, "Failed to get target alarm data", 900).show()
                Log.e("VideoPlayerFragment", "Alarm id is -1")
                activity.finish()
            }
            val asyncAlarm = alarmViewModel.getFromIdAsync(alarmId)
            lifecycleScope.launch {
                val alarm = asyncAlarm.await()
                // set snooze button
                launch(Dispatchers.Main) {
                    snoozeButton.setOnClickListener {
                        val now = Calendar.getInstance()
                        var hour = now[Calendar.HOUR_OF_DAY]
                        var minute = now[Calendar.MINUTE]
                        // TODO edit snooze time(add calc test)
                        minute += 1
                        if (minute > 59) {
                            minute %= 60
                            hour += 1
                        }
                        if (hour > 23) {
                            hour %= 24
                        }
                        val snoozeAlarm = alarm.copy(
                            id = null,
                            time = "$hour:$minute",
                            repeatType = RepeatType.Snooze
                        )
                        val job = alarmViewModel.insert(snoozeAlarm)
                        lifecycleScope.launch {
                            UpdateSnoozeNotifyWorker.registerWorker(view.context)
                            job.join()
                            lifecycleScope.launch(Dispatchers.Main) {
                                if (!findNavController().navigateUp()) {
                                    activity.finish()
                                }
                            }
                        }
                    }
                }
                // update alarm
                var repeatType = alarm.repeatType
                if (repeatType is RepeatType.Date) {
                    repeatType = RepeatType.Once
                }
                when (repeatType) {
                    is RepeatType.Once -> {
                        alarmViewModel.update(alarm.copy(repeatType = repeatType, enable = false))
                    }
                    is RepeatType.Everyday, is RepeatType.Days -> {
                        alarmViewModel.update(alarm)
                    }
                    is RepeatType.Snooze -> {
                        alarmViewModel.delete(alarm)
                    }
                    else -> {}
                }

                val playlist = playlistViewModel.getFromIdAsync(alarm.playListId!!).await()
                val videos = playlist?.let { videoViewModel.getFromIdsAsync(it.videos).await() }
                if (videos.isNullOrEmpty()) {
                    launch(Dispatchers.Main) {
                        Snackbar.make(view, "Video is Empty", 900).show()
                    }
                }
                launch(Dispatchers.Main) {
                    currentVolume = audioManager.getStreamVolume(musicStream)
                    val maxVolume =
                        audioManager.getStreamMaxVolume(musicStream) * (alarm.volume / 100f)
                    audioManager.setStreamVolume(
                        musicStream,
                        maxVolume.roundToInt(),
                        AudioManager.FLAG_PLAY_SOUND
                    )
                }
                var queue = 0
                playVideo(view, videos!!.first())
                videoView.setOnCompletionListener {
                    if (++queue >= videos.size) {
                        if (alarm.loop) {
                            queue = 0
                        } else {
                            return@setOnCompletionListener
                        }
                    }
                    playVideo(view, videos[queue])
                }
            }
        } else {
            lifecycleScope.launch {
                val video = videoViewModel.getFromIdAsync(id).await()
                if (video == null) {
                    Snackbar.make(view, "Error! Cannot get video data.", Snackbar.LENGTH_LONG)
                        .setAction("Action", null)
                        .show()
                    return@launch
                }
                playVideo(view, video)
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        val activity = requireActivity()
        if (activity is MainActivity) {
            activity.binding.fab.visibility = View.VISIBLE
        }

        currentVolume?.let {
            audioManager.setStreamVolume(musicStream, it, AudioManager.FLAG_PLAY_SOUND)
        }

        val window = activity.window
        val insetsController = WindowCompat.getInsetsController(window, requireView())
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.show(WindowInsetsCompat.Type.systemBars())

        super.onDestroyView()
    }

    private fun playVideo(view: View, video: Video) = lifecycleScope.launch rootLaunch@{
        launch(Dispatchers.Main) {
            binding.progressBar.visibility = View.VISIBLE
        }

        val videoView = binding.videoView
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