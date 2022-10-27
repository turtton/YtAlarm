package net.turtton.ytalarm.ui.fragment

import android.content.Context
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.turtton.ytalarm.R
import net.turtton.ytalarm.YtApplication.Companion.repository
import net.turtton.ytalarm.activity.MainActivity
import net.turtton.ytalarm.database.structure.Alarm
import net.turtton.ytalarm.database.structure.Video
import net.turtton.ytalarm.databinding.FragmentVideoPlayerBinding
import net.turtton.ytalarm.util.extensions.hourOfDay
import net.turtton.ytalarm.util.extensions.minute
import net.turtton.ytalarm.util.extensions.plusAssign
import net.turtton.ytalarm.util.observeAlarm
import net.turtton.ytalarm.util.updateAlarmSchedule
import net.turtton.ytalarm.viewmodel.AlarmViewModel
import net.turtton.ytalarm.viewmodel.AlarmViewModelFactory
import net.turtton.ytalarm.viewmodel.PlaylistViewModel
import net.turtton.ytalarm.viewmodel.PlaylistViewModelFactory
import net.turtton.ytalarm.viewmodel.VideoViewModel
import net.turtton.ytalarm.viewmodel.VideoViewModelFactory
import net.turtton.ytalarm.worker.UpdateSnoozeNotifyWorker
import java.util.*
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.minutes
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
        enableFullScreenMode(view)

        val activity = requireActivity()
        if (activity is MainActivity) {
            hideFab(activity)
        }

        val videoView = binding.videoView
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

        val dismissButton = binding.fragmentVideoPlayerButtonDismiss

        val isAlarm = args.isAlarm
        if (!isAlarm) {
            val timeView = binding.fragmentVideoPlayerTextTime
            val snoozeButton = binding.fragmentVideoPlayerButtonSnooze

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
            startAsAlarmMode(view, id.toLong())
        } else {
            lifecycleScope.launch {
                val video = videoViewModel.getFromVideoIdAsync(id).await()
                if (video == null) {
                    val message = R.string.snackbar_error_failed_to_get_video
                    Snackbar.make(view, message, Snackbar.LENGTH_LONG).show()
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

    private fun startAsAlarmMode(view: View, alarmId: Long) {
        val activity = requireActivity()
        val snoozeButton = binding.fragmentVideoPlayerButtonSnooze
        val videoView = binding.videoView

        lifecycleScope.launch {
            val alarmList = alarmViewModel.getAllAlarmsAsync().await().filter { it.isEnable }
            updateAlarmSchedule(view.context, alarmList)
        }
        if (alarmId == -1L) {
            val message = R.string.snackbar_error_failed_to_get_alarm
            Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
            Log.e(LOG_TAG, "Alarm id is -1")
        }
        val asyncAlarm = alarmViewModel.getFromIdAsync(alarmId)
        lifecycleScope.launch {
            val alarm = asyncAlarm.await() ?: onFailedToGetAlarm(alarmId)
            launch(Dispatchers.Main) {
                setUpSnoozeButton(view.context, snoozeButton, alarm)
            }
            updateAlarm(alarm)

            if (alarm.shouldVibrate) {
                launch(Dispatchers.Main) {
                    startVibration(view)
                }
            }

            val playlist = playlistViewModel.getFromIdsAsync(alarm.playListId).await()
            val videos = playlist.flatMap { it.videos }
                .distinct()
                .let { videoViewModel.getFromIdsAsync(it).await() }
            if (videos.isEmpty()) {
                val message = R.string.snackbar_error_empty_video
                Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
                Log.e(LOG_TAG, "Could not start alarm due to empty videos")
                return@launch
            }
            launch(Dispatchers.Main) {
                currentVolume = audioManager.getStreamVolume(musicStream)
                val volumeRate = alarm.volume.volume / Alarm.Volume.MAX_VOLUME.toFloat()
                val maxVolume = audioManager.getStreamMaxVolume(musicStream)
                val volume = (maxVolume * volumeRate).roundToInt()
                audioManager.setStreamVolume(musicStream, volume, AudioManager.FLAG_PLAY_SOUND)
            }
            var queue = if (alarm.shouldShuffle) {
                videos.shuffled().iterator()
            } else {
                videos.listIterator()
            }
            playVideo(view, queue.next())
            videoView.setOnCompletionListener {
                if (!queue.hasNext()) {
                    if (alarm.shouldLoop) {
                        queue = videos.iterator()
                    } else {
                        activity.finish()
                    }
                }
                playVideo(view, queue.next())
            }
        }
    }

    private fun enableFullScreenMode(view: View) {
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
    }

    private fun hideFab(activity: MainActivity) {
        val fab = activity.binding.fab
        fab.clearAnimation()
        fab.visibility = View.GONE

        activity.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        activity.binding.toolbar.visibility = View.GONE
    }

    private fun updateAlarm(alarm: Alarm) {
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
    }

    private fun setUpSnoozeButton(context: Context, snoozeButton: Button, alarm: Alarm) {
        snoozeButton.setOnClickListener {
            val now = Calendar.getInstance()
            now += alarm.snoozeMinute.minutes
            val snoozeAlarm = alarm.copy(
                id = 0,
                hour = now.hourOfDay,
                minute = now.minute,
                repeatType = Alarm.RepeatType.Snooze
            )
            val job = alarmViewModel.insert(snoozeAlarm)
            lifecycleScope.launch {
                UpdateSnoozeNotifyWorker.registerWorker(context)
                job.join()
                lifecycleScope.launch(Dispatchers.Main) {
                    if (!findNavController().navigateUp()) {
                        activity?.finish()
                    }
                }
            }
        }
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
                    val message = R.string.snackbar_error_failed_to_import_video
                    Snackbar.make(view, message, Snackbar.LENGTH_LONG).show()
                    Log.e(LOG_TAG, "failed to get stream url")
                    return@launch
                } else {
                    binding.progressBar.visibility = View.GONE
                    videoView.setVideoURI(videoUrl.toUri())
                    videoView.start()
                }
            }.onFailure {
                val message = R.string.snackbar_error_failed_to_import_video
                Snackbar.make(view, message, Snackbar.LENGTH_LONG).show()
                Log.e(LOG_TAG, "failed to get stream info", it)
            }
        }
    }

    private fun startVibration(view: View) {
        val context = view.context
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager?
            manager?.defaultVibrator
        } else {
            @Suppress("Deprecation")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
        } ?: kotlin.run {
            val message = R.string.snackbar_error_failed_to_prepare_vibration
            Snackbar.make(view, message, Snackbar.LENGTH_LONG).show()
            Log.e(LOG_TAG, "Failed to get vibrator service. Null")
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val wave = VibrationEffect.createWaveform(
                VIBRATION_TIMINGS,
                VIBRATION_AMPLITUDES,
                VIBRATION_REPEAT_POS
            )
            vibrator?.vibrate(wave)
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(VIBRATION_TIMINGS, VIBRATION_REPEAT_POS)
        }
    }

    private suspend fun onFailedToGetAlarm(invalidId: Long): Alarm {
        binding.fragmentVideoPlayerTextError.visibility = View.VISIBLE
        Log.e(LOG_TAG, "Failed to get alarm. TargetId: $invalidId")
        val allPlaylist = playlistViewModel.allPlaylistsAsync.await().map { it.id }
        return Alarm(
            repeatType = Alarm.RepeatType.Snooze,
            shouldLoop = true,
            isEnable = true,
            playListId = allPlaylist,
            snoozeMinute = 10
        )
    }

    companion object {
        private val VIBRATION_MILLIS = 1.5.seconds.toLong(DurationUnit.MILLISECONDS)
        private const val VIBRATION_STRENGTH = 255
        private val VIBRATION_TIMINGS = longArrayOf(VIBRATION_MILLIS, VIBRATION_MILLIS)
        private val VIBRATION_AMPLITUDES = intArrayOf(0, VIBRATION_STRENGTH)
        private const val VIBRATION_REPEAT_POS = 0
        private const val LOG_TAG = "FragmentVideoPlayer"
    }
}