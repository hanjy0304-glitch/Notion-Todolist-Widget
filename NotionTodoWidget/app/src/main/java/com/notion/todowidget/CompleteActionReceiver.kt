package com.notion.todowidget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import kotlin.concurrent.thread

class CompleteActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_COMPLETE -> {
                val pageId = intent.getStringExtra(EXTRA_PAGE_ID) ?: return
                handleComplete(context, pageId)
            }
            ACTION_REFRESH -> SyncWorker.runNow(context)
        }
    }

    private fun handleComplete(context: Context, pageId: String) {
        if (Prefs.isStrikethroughMode(context)) {
            val alreadyChecked = Prefs.getCheckedIds(context).contains(pageId)
            if (alreadyChecked) {
                // 취소선 → 다시 탭: 취소선 제거 + 시작전으로 복원
                Prefs.removeCheckedId(context, pageId)
                thread { NotionService.resetItem(context, pageId) }
            } else {
                // 미체크 → 체크: 취소선 표시 + 완료 처리
                Prefs.addCheckedId(context, pageId)
                thread { NotionService.completeItem(context, pageId) }
            }
        } else {
            // 제거 모드: 캐시에서 즉시 제거 + 완료 처리
            val updated = TodoItem.listFromJson(Prefs.getCache(context)).filter { it.id != pageId }
            Prefs.setCache(context, TodoItem.listToJson(updated))
            thread { NotionService.completeItem(context, pageId) }
        }

        val mgr = AppWidgetManager.getInstance(context)
        val ids = mgr.getAppWidgetIds(ComponentName(context, TodoWidgetProvider::class.java))
        TodoWidgetProvider.updateWidgets(context, mgr, ids)
    }

    companion object {
        const val ACTION_COMPLETE = "com.notion.todowidget.ACTION_COMPLETE"
        const val ACTION_REFRESH = "com.notion.todowidget.ACTION_REFRESH"
        const val EXTRA_PAGE_ID = "page_id"
    }
}
