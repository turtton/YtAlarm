package net.turtton.ytalarm.ui.compose.screens

import net.turtton.ytalarm.database.structure.Alarm

/**
 * アラーム編集状態を表すsealed class
 *
 * 状態の不整合を防ぐため、編集状態を単一の状態として管理する。
 */
sealed class AlarmEditState {
    /** ボトムシート非表示 */
    data object Hidden : AlarmEditState()

    /** 新規アラーム作成中 */
    data class CreatingNew(val alarm: Alarm) : AlarmEditState()

    /** 既存アラーム編集中 */
    data class Editing(val alarmId: Long, val alarm: Alarm?) : AlarmEditState()
}