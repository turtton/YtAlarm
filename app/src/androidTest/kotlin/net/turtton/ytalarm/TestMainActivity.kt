package net.turtton.ytalarm

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.turtton.ytalarm.activity.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * MainActivityのComposeテスト
 *
 * 注意: このテストはCompose移行後のバージョンです。
 * 以前のFragment-basedテスト（basicFabTest）はCompose移行により削除されました。
 * 完全なUIテストは今後のPhase 14で実装予定です。
 */
@RunWith(AndroidJUnit4::class)
class TestMainActivity {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    /**
     * アプリが正常に起動し、初期画面（アラームリスト）が表示されることを確認
     */
    @Test
    fun appLaunchesSuccessfully() {
        // アラームリスト画面のタイトルが表示されていることを確認
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.nav_alarm_list)
        ).assertIsDisplayed()
    }
}