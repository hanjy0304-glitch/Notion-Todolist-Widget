package com.notion.todowidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.*

class TodoWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        updateWidgets(context, manager, ids)
        SyncWorker.schedulePeriodic(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context, manager: AppWidgetManager, id: Int, newOptions: Bundle
    ) {
        updateWidgets(context, manager, intArrayOf(id))
    }

    companion object {

        fun updateWidgets(context: Context, manager: AppWidgetManager, ids: IntArray) {
            ids.forEach { id ->
                val options = manager.getAppWidgetOptions(id)
                manager.updateAppWidget(id, buildRemoteViews(context, options, id))
                listOf(R.id.q1_list, R.id.q2_list, R.id.q3_list, R.id.q4_list).forEach { listId ->
                    manager.notifyAppWidgetViewDataChanged(id, listId)
                }
            }
        }

        fun buildRemoteViews(context: Context, options: Bundle = Bundle(), widgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_layout)
            val widgetAlpha = Prefs.getWidgetAlpha(context)
            val quadrantAlpha = Prefs.getQuadrantAlpha(context)

            // 위젯 배경 투명도
            val widgetAlphaInt = (widgetAlpha * 255).toInt()
            if (widgetAlphaInt >= 255) {
                views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.widget_bg)
            } else {
                views.setInt(R.id.widget_root, "setBackgroundColor", Color.argb(widgetAlphaInt, 255, 255, 255))
            }

            // 사분면 배경 투명도
            applyQuadrantAlpha(views, R.id.q1_container, Color.parseColor("#FFF3F3"), quadrantAlpha, R.drawable.quadrant_bg_q1)
            applyQuadrantAlpha(views, R.id.q2_container, Color.parseColor("#FFFDE7"), quadrantAlpha, R.drawable.quadrant_bg_q2)
            applyQuadrantAlpha(views, R.id.q3_container, Color.parseColor("#E8F4FD"), quadrantAlpha, R.drawable.quadrant_bg_q3)
            applyQuadrantAlpha(views, R.id.q4_container, Color.parseColor("#F1F8E9"), quadrantAlpha, R.drawable.quadrant_bg_q4)

            // 동기화 시간
            val lastSync = Prefs.getLastSync(context)
            if (lastSync > 0) {
                views.setTextViewText(R.id.tv_last_sync, SimpleDateFormat("HH:mm 동기화", Locale.KOREA).format(Date(lastSync)))
            }

            // 새로고침 버튼
            val refreshPi = PendingIntent.getBroadcast(
                context, 999,
                Intent(context, CompleteActionReceiver::class.java).apply { action = CompleteActionReceiver.ACTION_REFRESH },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_refresh, refreshPi)

            // 타이틀/배경 탭 → 앱 열기
            val appPi = PendingIntent.getActivity(
                context, 997,
                Intent(context, MainActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.tv_widget_title, appPi)
            views.setOnClickPendingIntent(R.id.widget_root, appPi)

            // 사분면 라벨 탭 → 노션 DB 열기
            val dbId = Prefs.getDbId(context)
            if (dbId.isNotEmpty()) {
                val notionPi = PendingIntent.getActivity(
                    context, 998,
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://www.notion.so/$dbId")).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                listOf(R.id.q1_label, R.id.q2_label, R.id.q3_label, R.id.q4_label).forEach {
                    views.setOnClickPendingIntent(it, notionPi)
                }
            }

            // 체크 버튼 PendingIntent 템플릿
            val checkTemplate = PendingIntent.getBroadcast(
                context, 0,
                Intent(context, CompleteActionReceiver::class.java).apply { action = CompleteActionReceiver.ACTION_COMPLETE },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )

            // ListView RemoteAdapter 설정
            setupList(context, views, R.id.q1_list, TodoItem.P_Q1, widgetId, checkTemplate)
            setupList(context, views, R.id.q2_list, TodoItem.P_Q2, widgetId, checkTemplate)
            setupList(context, views, R.id.q3_list, TodoItem.P_Q3, widgetId, checkTemplate)
            setupList(context, views, R.id.q4_list, TodoItem.P_Q4, widgetId, checkTemplate)

            return views
        }

        private fun setupList(
            context: Context, views: RemoteViews, listId: Int,
            priority: String, widgetId: Int, checkTemplate: PendingIntent
        ) {
            val intent = Intent(context, WidgetListService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                putExtra("quadrant", priority)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(listId, intent)
            views.setPendingIntentTemplate(listId, checkTemplate)
        }

        private fun applyQuadrantAlpha(
            views: RemoteViews, containerId: Int, bgColor: Int, alpha: Float, drawableRes: Int
        ) {
            val alphaInt = (alpha * 255).toInt()
            if (alphaInt >= 255) {
                views.setInt(containerId, "setBackgroundResource", drawableRes)
            } else {
                views.setInt(containerId, "setBackgroundColor",
                    Color.argb(alphaInt, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor)))
            }
        }
    }
}
