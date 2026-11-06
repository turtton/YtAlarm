package net.turtton.ytalarm.worker

import android.content.SharedPreferences
import androidx.work.BackoffPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.WorkManager
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import net.turtton.ytalarm.util.extensions.ytDlpAutoUpdate
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import java.util.concurrent.TimeUnit

@Suppress("RestrictedApi")
class YtDlpUpdateSchedulerTest :
    FunSpec({
        test("periodic request requires unmetered charging battery and storage") {
            val request = YtDlpUpdateScheduler.periodicRequest()
            val spec = request.workSpec
            spec.intervalDuration shouldBe TimeUnit.DAYS.toMillis(1)
            spec.constraints.requiredNetworkType shouldBe NetworkType.UNMETERED
            spec.constraints.requiresCharging() shouldBe true
            spec.constraints.requiresBatteryNotLow() shouldBe true
            spec.constraints.requiresStorageNotLow() shouldBe true
            spec.backoffPolicy shouldBe BackoffPolicy.EXPONENTIAL
            spec.backoffDelayDuration shouldBe TimeUnit.MINUTES.toMillis(30)
            spec.input.getBoolean(YtDlpUpdateScheduler.AUTOMATIC, false) shouldBe true
        }
        test("repeated registration uses one unique work name with KEEP") {
            val manager = mock<WorkManager>()
            repeat(2) { YtDlpUpdateScheduler.reconcile(manager, enabled = true) }
            verify(manager, times(2)).enqueueUniquePeriodicWork(
                eq(YtDlpUpdateScheduler.WORK_NAME),
                eq(ExistingPeriodicWorkPolicy.KEEP),
                any()
            )
            verifyNoMoreInteractions(manager)
        }
        test("disabling cancels only automatic work") {
            val manager = mock<WorkManager>()
            YtDlpUpdateScheduler.reconcile(manager, enabled = false)
            verify(manager).cancelUniqueWork(YtDlpUpdateScheduler.WORK_NAME)
            verifyNoMoreInteractions(manager)
        }
        test("automatic updates are enabled by default and can be disabled") {
            val preferences = mock<SharedPreferences>()
            whenever(preferences.getBoolean("YtDlpAutoUpdate", true)).thenReturn(true)
            preferences.ytDlpAutoUpdate shouldBe true
            verify(preferences).getBoolean("YtDlpAutoUpdate", true)
            whenever(preferences.getBoolean("YtDlpAutoUpdate", true)).thenReturn(false)
            preferences.ytDlpAutoUpdate shouldBe false
        }
    })