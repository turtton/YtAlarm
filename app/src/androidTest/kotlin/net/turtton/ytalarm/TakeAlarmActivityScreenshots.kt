package net.turtton.ytalarm

import android.content.Intent
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.IdlingRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.runBlocking
import net.turtton.ytalarm.activity.AlarmActivity
import net.turtton.ytalarm.idling.VideoPlayerLoadingResource
import net.turtton.ytalarm.util.TestDataHelper
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy
import tools.fastlane.screengrab.cleanstatusbar.CleanStatusBar
import tools.fastlane.screengrab.cleanstatusbar.MobileDataType
import tools.fastlane.screengrab.locale.LocaleTestRule

/**
 * AlarmActivityのスクリーンショットテスト（Compose版）
 *
 * アラーム発火時の画面をスクリーンショットで取得する。
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class TakeAlarmActivityScreenshots {
    private var idlingResource: VideoPlayerLoadingResource? = null
    private var scenario: ActivityScenario<AlarmActivity>? = null
    private var testAlarmId: Long = -1L

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Rule
    @JvmField
    val localeTestRule = LocaleTestRule()

    @Before
    fun beforeAll() {
        CleanStatusBar()
            .setClock("0000")
            .setMobileNetworkDataType(MobileDataType.FOURG)
            .enable()

        Screengrab.setDefaultScreenshotStrategy(UiAutomatorScreenshotStrategy())

        // テストデータを事前に挿入し、アラームIDを取得
        val application = ApplicationProvider.getApplicationContext<YtApplication>()
        runBlocking {
            // YoutubeDLを事前に初期化（最初のテスト実行時の遅延を回避）
            YoutubeDL.getInstance().init(application)

            val database = application.database
            testAlarmId = TestDataHelper.insertAllTestData(
                database.videoDao(),
                database.playlistDao(),
                database.alarmDao()
            )
        }
    }

    @After
    fun afterAll() {
        CleanStatusBar.disable()
        idlingResource?.also {
            IdlingRegistry.getInstance().unregister(it)
        }
        scenario?.close()
    }

    @Test
    fun testTakeScreenshot() {
        // AlarmActivityをIntentで起動（テストで挿入したアラームIDを設定）
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            AlarmActivity::class.java
        ).apply {
            putExtra(AlarmActivity.EXTRA_ALARM_ID, testAlarmId)
        }

        scenario = ActivityScenario.launch<AlarmActivity>(intent)

        scenario?.onActivity { activity ->
            // IdlingResourceを登録
            idlingResource = activity.videoPlayerLoadingResourceController
                .registerVideoPlayerLoadingResource()
            IdlingRegistry.getInstance().register(idlingResource)
        }

        // Composeの状態同期を待機
        composeTestRule.waitForIdle()

        // 動画プレーヤーの読み込みを待機
        // IdlingResourceだけでは不十分な場合があるので追加の待機
        Thread.sleep(PLAYER_LOAD_WAIT_MS)

        // 再度Composeの状態同期を確認
        composeTestRule.waitForIdle()

        Screengrab.screenshot("00-alarm")
    }

    companion object {
        // 動画プレーヤーの読み込み待機時間（ミリ秒）
        private const val PLAYER_LOAD_WAIT_MS = 15000L
    }
}