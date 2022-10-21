package net.turtton.ytalarm.ui.fragment

import android.content.Context
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.drawerlayout.widget.DrawerLayout
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.turtton.ytalarm.R
import net.turtton.ytalarm.YtApplication.Companion.repository
import net.turtton.ytalarm.activity.MainActivity
import net.turtton.ytalarm.database.structure.Alarm
import net.turtton.ytalarm.database.structure.Video
import net.turtton.ytalarm.databinding.FragmentVideoPlayerBinding
import net.turtton.ytalarm.util.observeAlarm
import net.turtton.ytalarm.util.updateAlarmSchedule
import net.turtton.ytalarm.viewmodel.AlarmViewModel
import net.turtton.ytalarm.viewmodel.AlarmViewModelFactory
import net.turtton.ytalarm.viewmodel.PlaylistViewModel
import net.turtton.ytalarm.viewmodel.PlaylistViewModelFactory
import net.turtton.ytalarm.viewmodel.VideoViewModel
import net.turtton.ytalarm.viewmodel.VideoViewModelFactory
import net.turtton.ytalarm.worker.UpdateSnoozeNotifyWorker
import java.util.Calendar
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

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

    private var vibrator: Vibrator? = null

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
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
            val fab = activity.binding.fab
            fab.clearAnimation()
            fab.visibility = View.GONE

            activity.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            activity.binding.toolbar.visibility = View.GONE
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
            lifecycleScope.launch {
                val alarmList = alarmViewModel.getAllAlarmsAsync().await()
                updateAlarmSchedule(view.context, alarmList)
            }
            startVibration()
            val alarmId = id.toLong()
            if (alarmId == -1L) {
                Snackbar.make(
                    view,
                    "Failed to get target alarm data",
                    Snackbar.LENGTH_SHORT
                ).show()
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
                        now.add(Calendar.MINUTE, alarm.snoozeMinute)
                        val hour = now[Calendar.HOUR_OF_DAY]
                        val minute = now[Calendar.MINUTE]
                        val snoozeAlarm = alarm.copy(
                            id = 0,
                            hour = hour,
                            minute = minute,
                            repeatType = Alarm.RepeatType.Snooze
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
                if (repeatType is Alarm.RepeatType.Date) {
                    repeatType = Alarm.RepeatType.Once
                }
                when (repeatType) {
                    is Alarm.RepeatType.Once -> {
                        alarmViewModel.update(alarm.copy(repeatType = repeatType, isEnable = false))
                    }
                    is Alarm.RepeatType.Everyday, is Alarm.RepeatType.Days -> {
                        alarmViewModel.update(alarm)
                    }
                    is Alarm.RepeatType.Snooze -> {
                        alarmViewModel.delete(alarm)
                    }
                    else -> {}
                }

                val playlist = playlistViewModel.getFromIdsAsync(alarm.playListId).await()
                val videos = playlist.flatMap { it.videos }
                    .distinct()
                    .let { videoViewModel.getFromIdsAsync(it).await() }
                if (videos.isEmpty()) {
                    launch(Dispatchers.Main) {
                        Snackbar.make(view, "Video is Empty", Snackbar.LENGTH_SHORT).show()
                    }
                }
                launch(Dispatchers.Main) {
                    currentVolume = audioManager.getStreamVolume(musicStream)
                    val volumeRate = alarm.volume.volume / Alarm.Volume.MAX_VOLUME.toFloat()
                    val maxVolume = audioManager.getStreamMaxVolume(musicStream) * volumeRate
                    audioManager.setStreamVolume(
                        musicStream,
                        maxVolume.roundToInt(),
                        AudioManager.FLAG_PLAY_SOUND
                    )
                }
                var queue = 0
                playVideo(view, videos.first())
                videoView.setOnCompletionListener {
                    if (++queue >= videos.size) {
                        if (alarm.shouldLoop) {
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
                val video = videoViewModel.getFromVideoIdAsync(id).await()
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
            activity.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            activity.binding.toolbar.visibility = View.VISIBLE
        }

        currentVolume?.let {
            audioManager.setStreamVolume(musicStream, it, AudioManager.FLAG_PLAY_SOUND)
        }

        val window = activity.window
        val insetsController = WindowCompat.getInsetsController(window, requireView())
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.show(WindowInsetsCompat.Type.systemBars())

        vibrator?.cancel()
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

    private fun startVibration() {
        vibrator = requireContext().getSystemService()
        lifecycleScope.launch {
            while (true) {
                delay(VIBRATION_LENGTH_MILLIS * 2)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val oneShot = VibrationEffect.createOneShot(
                        VIBRATION_LENGTH_MILLIS,
                        VIBRATION_STRENGTH
                    )
                    vibrator?.vibrate(oneShot)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(VIBRATION_LENGTH_MILLIS)
                }
            }
        }
    }

    companion object {
        private val VIBRATION_LENGTH_MILLIS = 1.5.seconds.toLong(DurationUnit.MILLISECONDS)
        private const val VIBRATION_STRENGTH = 255
    }
}