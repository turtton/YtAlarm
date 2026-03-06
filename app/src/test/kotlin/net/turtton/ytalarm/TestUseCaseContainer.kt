package net.turtton.ytalarm

import net.turtton.ytalarm.datasource.local.AppDatabase
import net.turtton.ytalarm.datasource.remote.YtDlpExecutor
import net.turtton.ytalarm.usecase.LocalDataSourceContainer
import net.turtton.ytalarm.usecase.RemoteDataSourceContainer
import net.turtton.ytalarm.usecase.UseCaseContainer

typealias TestUseCaseContainer = UseCaseContainer<
    AppDatabase,
    YtDlpExecutor,
    LocalDataSourceContainer<AppDatabase>,
    RemoteDataSourceContainer<YtDlpExecutor>
    >