# Safe yt-dlp updates

Automatic updates default to on. Application startup reconciles one unique daily WorkManager
request (KEEP); disabling cancels only that periodic request, not a manual update. Automatic
work needs an unmetered network, charging, battery-not-low and storage-not-low. Both modes
use the saved STABLE/NIGHTLY channel and exponential retry (initial backoff 30 minutes).
WorkManager timing is inexact and constraints may postpone an update indefinitely.

## Playback first

At worker start, and again inside the update gate, postpone when AlarmActivity is alive
(including stopped/locked and configuration recreation), or an enabled alarm is within
30 minutes. Include the firing minute rather than moving an exactly-current daily alarm to
tomorrow. Also check AlarmManager's actual next alarm clock; this conservatively includes
other apps' alarm clocks. Past date alarms do not hide future alarms.

YtDlpExecutor owns a process-wide read/write gate: imports, information/stream requests and
downloads take read access; init, update, verification and recovery take exclusive access.
Readers can run concurrently. An update never waits for existing readers: it retries instead.
Readers wait at most **3 seconds** for exclusive work, then return an error through existing
repository error handling. This bounds gate waiting, not network execution time. The merged
manifest currently contains no android:process components; adding another process requires
cross-process locking. VideoPlayerScreen and its playback/fallback logic are unchanged.

The previous auto-update removal (fb0394b, PR #309) also fixed an init/getInfo race and loading
state cleanup on errors. Keep the application's init Deferred and the existing stream path's
await: scheduling an update is not a substitute for initialization.

## Installation transaction

The adapter is explicitly coupled to youtubedl-android **0.18.1** (tag SHA
864791ae478e5e32194722337286790829c0a689). Reviewed upstream sources:

- [YoutubeDL.kt](https://github.com/yausername/youtubedl-android/blob/0.18.1/library/src/main/java/com/yausername/youtubedl_android/YoutubeDL.kt)
- [YoutubeDLUpdater.kt](https://github.com/yausername/youtubedl-android/blob/0.18.1/library/src/main/java/com/yausername/youtubedl_android/YoutubeDLUpdater.kt)
- [SharedPrefsHelper.kt](https://github.com/yausername/youtubedl-android/blob/0.18.1/common/src/main/java/com/yausername/youtubedl_common/SharedPrefsHelper.kt)

1. Persist/fsync an update-pending marker in noBackupFilesDir before calling the updater.
2. Update, then execute a network-free --version probe, including ALREADY_UP_TO_DATE results.
   Require zero exit status and nonempty output. Probe timeout is 10 seconds; interrupt the
   blocking execute and join its thread before releasing exclusive access.
3. On any update/probe failure, stage/fsync the library's bundled raw ytdlp resource and rename
   it over noBackupFilesDir/youtubedl-android/yt-dlp/yt-dlp. Clear both dlpVersion and
   dlpVersionName in preferences youtubedl-android with synchronous commit; retain pythonLibVersion.
4. Verify the bundled installation before removing the marker. Report retry even after a
   successful rollback. If recovery fails, retain the marker and reject readers; a subsequent
   updater can retry recovery. On next process startup, repair marked/missing/empty installs
   before exposing the executor. Do not rely on init(), which becomes a same-process no-op.

The marker protects against process kill, not all filesystem/power-loss scenarios. --version
only proves startup, not that extraction works on YouTube. The bundled fallback can itself be
outdated. Library upgrades must recheck all internal paths/resources/preferences and interrupt
behavior. Running automatic updates is ordinary background work, not a background FGS launch.

## Device verification checklist (not covered by JVM tests)

- Fresh install, upgrade, cold alarm launch, screen lock and activity recreation.
- Toggle off/on and restart; inspect unique periodic work, constraints and saved channel.
- Manual/automatic update during import/download/ringing, and at 30 minutes before an alarm.
- Slow update: stream request fails within roughly 3 seconds and playback fallback takes over.
- Kill process during binary replacement/verification; restart restores bundled file and clears
  both version keys. Simulate invalid binary, probe timeout and full storage/failed recovery.
- Successful STABLE and NIGHTLY update, stream extraction and download on each supported ABI.
