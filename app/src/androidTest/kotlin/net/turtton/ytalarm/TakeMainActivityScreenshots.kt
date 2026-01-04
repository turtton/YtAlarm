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
import coil.Coil
import kotlinx.coroutines.runBlocking
import net.turtton.ytalarm.activity.MainActivity
import net.turtton.ytalarm.idling.CoilIdlingResource
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
    private var coilIdlingResource: CoilIdlingResource? = null

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

        // CoilIdlingResourceを登録（画像読み込み完了待機用）
        composeTestRule.activityRule.scenario.onActivity { activity ->
            val app = activity.application as YtApplication
            coilIdlingResource = app.coilIdlingResourceController.registerCoilIdlingResource()
            IdlingRegistry.getInstance().register(coilIdlingResource)
            // ImageLoaderを再作成してEventListenerを有効化
            Coil.setImageLoader(app.newImageLoader())
        }

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
        coilIdlingResource?.also {
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
        waitForImagesAndIdle()
        Screengrab.screenshot("01-alarms")

        // 06-alarmSettings: アラーム設定画面
        // 最初のアラームをクリック
        composeTestRule.onAllNodesWithContentDescription("Alarm thumbnail")
            .onFirst()
            .performClick()
        waitForImagesAndIdle()
        Screengrab.screenshot("06-alarmSettings")

        // 戻る
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        waitForImagesAndIdle()

        // 07-drawer: ナビゲーションドロワー
        // TopAppBarのMenuボタンをクリック（複数あるので最初のものを選択）
        composeTestRule.onAllNodesWithContentDescription("Menu").onFirst().performClick()
        waitForImagesAndIdle()
        Screengrab.screenshot("07-drawer")

        // 02-playlist: プレイリスト一覧画面
        composeTestRule.onNodeWithText(
            context.getString(R.string.menu_title_playlist)
        ).performClick()
        waitForImagesAndIdle()
        Screengrab.screenshot("02-playlist")

        // 03-videos-origin: 動画一覧（最初のプレイリスト = 初期データ）
        composeTestRule.onAllNodesWithContentDescription("Playlist thumbnail")
            .onFirst()
            .performClick()
        waitForImagesAndIdle()
        Screengrab.screenshot("03-videos-origin")

        // 戻ってから別のプレイリストを選択
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        waitForImagesAndIdle()

        // 04-videos-playlist: 動画一覧（2番目のプレイリスト = Bandcampテストデータ）
        val playlistNodes = composeTestRule.onAllNodesWithContentDescription("Playlist thumbnail")
        val playlistCount = playlistNodes.fetchSemanticsNodes().size
        if (playlistCount > 1) {
            playlistNodes[1].performClick()
            waitForImagesAndIdle()
            Screengrab.screenshot("04-videos-playlist")
            composeTestRule.onNodeWithContentDescription("Back").performClick()
            waitForImagesAndIdle()
        } else {
            Log.w(LOG_TAG, "Skipping 04-videos-playlist: only $playlistCount playlist(s) found")
        }

        // 05-allvideos: 全動画一覧画面
        composeTestRule.onAllNodesWithContentDescription("Menu").onFirst().performClick()
        waitForImagesAndIdle()
        composeTestRule.onNodeWithText(
            context.getString(R.string.menu_title_video_list)
        ).performClick()
        waitForImagesAndIdle()
        Screengrab.screenshot("05-allvideos")

        // 08-videoplayer: 動画プレーヤー画面
        composeTestRule.onAllNodesWithContentDescription("Video thumbnail")
            .onFirst()
            .performClick()
        // IdlingResourceで動画読み込み完了を待機
        waitForImagesAndIdle()
        Thread.sleep(PLAYER_LOAD_WAIT_MS)
        Screengrab.screenshot("08-videoplayer")

        // 戻る（VideoPlayerScreenにはBackボタンがないのでシステムバックを使用）
        Espresso.pressBack()
        waitForImagesAndIdle()

        // 09-aboutpage: About画面
        composeTestRule.onAllNodesWithContentDescription("Menu").onFirst().performClick()
        waitForImagesAndIdle()
        composeTestRule.onNodeWithText(
            context.getString(R.string.menu_title_aboutpage)
        ).performClick()
        waitForImagesAndIdle()
        Screengrab.screenshot("09-aboutpage")
    }

    /**
     * Compose UIとCoil画像読み込みの両方が完了するまで待機
     */
    private fun waitForImagesAndIdle() {
        composeTestRule.waitForIdle()
        // Espresso IdlingResourcesの待機（CoilIdlingResource含む）
        Espresso.onIdle()
    }

    companion object {
        private const val LOG_TAG = "TakeMainActivityScreenshots"

        // 動画プレーヤーの読み込み待機時間（ミリ秒）
        private const val PLAYER_LOAD_WAIT_MS = 5000L
    }
}