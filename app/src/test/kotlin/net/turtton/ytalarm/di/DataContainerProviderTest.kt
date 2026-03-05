package net.turtton.ytalarm.di

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import net.turtton.ytalarm.datasource.local.AppDatabase
import net.turtton.ytalarm.datasource.remote.YtDlpExecutor
import net.turtton.ytalarm.kernel.di.LocalDataSourceContainer
import net.turtton.ytalarm.kernel.di.RemoteDataSourceContainer
import net.turtton.ytalarm.usecase.UseCaseContainer
import org.mockito.kotlin.mock

@Suppress("UNUSED")
class DataContainerProviderTest :
    FunSpec({
        context("DataContainerProvider") {
            test("stub implementation returns non-null UseCaseContainer") {
                val stubContainer: UseCaseContainer<
                    AppDatabase,
                    YtDlpExecutor,
                    LocalDataSourceContainer<AppDatabase>,
                    RemoteDataSourceContainer<YtDlpExecutor>
                    > = mock()
                val provider = object : DataContainerProvider {
                    override fun getUseCaseContainer() = stubContainer
                }
                provider.getUseCaseContainer() shouldNotBe null
            }

            test("stub implementation satisfies DataContainerProvider interface") {
                val stubContainer: UseCaseContainer<
                    AppDatabase,
                    YtDlpExecutor,
                    LocalDataSourceContainer<AppDatabase>,
                    RemoteDataSourceContainer<YtDlpExecutor>
                    > = mock()
                val provider: DataContainerProvider = object : DataContainerProvider {
                    override fun getUseCaseContainer() = stubContainer
                }
                provider.shouldBeInstanceOf<DataContainerProvider>()
            }

            test("stub implementation returns UseCaseContainer that is UseCaseContainer instance") {
                val stubContainer: UseCaseContainer<
                    AppDatabase,
                    YtDlpExecutor,
                    LocalDataSourceContainer<AppDatabase>,
                    RemoteDataSourceContainer<YtDlpExecutor>
                    > = mock()
                val provider = object : DataContainerProvider {
                    override fun getUseCaseContainer() = stubContainer
                }
                val container = provider.getUseCaseContainer()
                container.shouldBeInstanceOf<UseCaseContainer<*, *, *, *>>()
            }
        }
    })