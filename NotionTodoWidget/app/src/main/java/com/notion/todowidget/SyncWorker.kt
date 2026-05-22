package com.notion.todowidget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

class SyncWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val ctx = applicationContext

        if (!Prefs.isConfigured(ctx)) return Result.success()

        val items = NotionService.fetchTodos(ctx)
        if (items != null) {
            Prefs.setCache(ctx, TodoItem.listToJson(items))
            Prefs.setLastSync(ctx, System.currentTimeMillis())
            // 노션에서 시작전/진행중으로 돌아온 항목은 체크 ID 제거 (노션이 정답)
            items.forEach { item -> Prefs.removeCheckedId(ctx, item.id) }
        }

        // 위젯 갱신 트리거
        val manager = AppWidgetManager.getInstance(ctx)
        val ids = manager.getAppWidgetIds(ComponentName(ctx, TodoWidgetProvider::class.java))
        if (ids.isNotEmpty()) {
            TodoWidgetProvider.updateWidgets(ctx, manager, ids)
        }

        return Result.success()
    }

    companion object {
        private const val WORK_NAME_PERIODIC = "notion_sync_periodic"
        private const val WORK_NAME_IMMEDIATE = "notion_sync_immediate"

        /** 15분마다 반복 동기화 등록 */
        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        /** 즉시 1회 동기화 실행 */
        fun runNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME_IMMEDIATE,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
