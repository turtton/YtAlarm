package net.turtton.ytalarm.kernel.di

/**
 * Executor（DB/リモートAPIなど）を生成するファクトリインターフェース。
 */
interface DataSource<Executor> {
    fun createExecutor(): Executor
}