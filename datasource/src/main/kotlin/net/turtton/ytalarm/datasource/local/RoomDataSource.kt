package net.turtton.ytalarm.datasource.local

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import net.turtton.ytalarm.kernel.di.DataSource

/**
 * Room データベースを生成する [DataSource] 実装。
 *
 * [AppDatabase] のシングルトンインスタンスを lazy で保持し、
 * [createExecutor] で返す。
 */
class RoomDataSource(private val context: Context) : DataSource<AppDatabase> {
    private val database by lazy {
        AppDatabase.getDataBase(context, CoroutineScope(Dispatchers.IO))
    }

    override fun createExecutor(): AppDatabase = database
}