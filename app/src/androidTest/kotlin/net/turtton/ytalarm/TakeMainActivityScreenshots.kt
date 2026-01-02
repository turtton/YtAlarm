package net.turtton.ytalarm

import android.util.Log
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import kotlinx.coroutines.runBlocking
import net.turtton.ytalarm.activity.MainActivity
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
 * MainActivityのスクリーンショットテスト（Compose版）
 *
 * Compose Testing APIを使用してUI操作を行い、
 * fastlane screengrabでスクリーンショットを取得する。
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class TakeMainActivityScreenshots {
    private var idlingResource: VideoPlayerLoadingResource? = null

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

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

        // テストデータを挿入
        composeTestRule.activityRule.scenario.onActivity { activity ->
            runBlocking {
                val database = (activity.application as YtApplication).database
                TestDataHelper.insertAllTestData(
                    database.videoDao(),
                    database.playlistDao(),
                    database.alarmDao()
                )
            }
        }

        // UIの安定を待機
        composeTestRule.waitForIdle()
    }

    @After
    fun afterAll() {
        CleanStatusBar.disable()
        idlingResource?.also {
            IdlingRegistry.getInstance().unregister(it)
        }

        // テストデータをクリーンアップ
        composeTestRule.activityRule.scenario.onActivity { activity ->
            runBlocking {
                val database = (activity.application as YtApplication).database
                database.alarmDao().deleteAll()
                database.playlistDao().deleteAll()
                database.videoDao().deleteAll()
            }
        }
    }

    @Test
    fun testTakeScreenshot() {
        val context = composeTestRule.activity

        // 01-alarms: アラーム一覧画面
        composeTestRule.waitForIdle()
        Screengrab.screenshot("01-alarms")

        // 06-alarmSettings: アラーム設定画面
        // 最初のアラームをクリック
        composeTestRule.onAllNodesWithContentDescription("Alarm thumbnail")
            .onFirst()
            .performClick()
        composeTestRule.waitForIdle()
        Screengrab.screenshot("06-alarmSettings")

        // 戻る
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()

        // 07-drawer: ナビゲーションドロワー
        // TopAppBarのMenuボタンをクリック（複数あるので最初のものを選択）
        composeTestRule.onAllNodesWithContentDescription("Menu").onFirst().performClick()
        composeTestRule.waitForIdle()
        Screengrab.screenshot("07-drawer")

        // 02-playlist: プレイリスト一覧画面
        composeTestRule.onNodeWithText(
            context.getString(R.string.menu_title_playlist)
        ).performClick()
        composeTestRule.waitForIdle()
        Screengrab.screenshot("02-playlist")

        // 03-videos-origin: 動画一覧（最初のプレイリスト = 初期データ）
        composeTestRule.onAllNodesWithContentDescription("Playlist thumbnail")
            .onFirst()
            .performClick()
        composeTestRule.waitForIdle()
        Screengrab.screenshot("03-videos-origin")

        // 戻ってから別のプレイリストを選択
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()

        // 04-videos-playlist: 動画一覧（2番目のプレイリスト = Bandcampテストデータ）
        val playlistNodes = composeTestRule.onAllNodesWithContentDescription("Playlist thumbnail")
        val playlistCount = playlistNodes.fetchSemanticsNodes().size
        if (playlistCount > 1) {
            playlistNodes[1].performClick()
            composeTestRule.waitForIdle()
            Screengrab.screenshot("04-videos-playlist")
            composeTestRule.onNodeWithContentDescription("Back").performClick()
            composeTestRule.waitForIdle()
        } else {
            Log.w(LOG_TAG, "Skipping 04-videos-playlist: only $playlistCount playlist(s) found")
        }

        // 05-allvideos: 全動画一覧画面
        composeTestRule.onAllNodesWithContentDescription("Menu").onFirst().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(
            context.getString(R.string.menu_title_video_list)
        ).performClick()
        composeTestRule.waitForIdle()
        Screengrab.screenshot("05-allvideos")

        // 08-videoplayer: 動画プレーヤー画面
        composeTestRule.onAllNodesWithContentDescription("Video thumbnail")
            .onFirst()
            .performClick()
        // IdlingResourceで動画読み込み完了を待機
        composeTestRule.waitForIdle()
        Thread.sleep(PLAYER_LOAD_WAIT_MS)
        Screengrab.screenshot("08-videoplayer")

        // 戻る（VideoPlayerScreenにはBackボタンがないのでシステムバックを使用）
        Espresso.pressBack()
        composeTestRule.waitForIdle()

        // 09-aboutpage: About画面
        composeTestRule.onAllNodesWithContentDescription("Menu").onFirst().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(
            context.getString(R.string.menu_title_aboutpage)
        ).performClick()
        composeTestRule.waitForIdle()
        Screengrab.screenshot("09-aboutpage")
    }

    companion object {
        private const val LOG_TAG = "TakeMainActivityScreenshots"

        // 動画プレーヤーの読み込み待機時間（ミリ秒）
        private const val PLAYER_LOAD_WAIT_MS = 5000L
    }
}