package com.notion.todowidget

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import java.text.SimpleDateFormat
import java.util.*

class WidgetListFactory(
    private val context: Context,
    intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    private val quadrant = intent.getStringExtra("quadrant") ?: TodoItem.P_Q4
    private var items: List<TodoItem> = emptyList()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        val all = TodoItem.listFromJson(Prefs.getCache(context))
        val filtered = all.filter { it.priority == quadrant }
        // 마감일 있는 항목 오름차순, 없는 항목은 하단
        items = filtered.sortedWith(Comparator { a, b ->
            when {
                a.deadline == null && b.deadline == null -> 0
                a.deadline == null -> 1
                b.deadline == null -> -1
                else -> a.deadline.compareTo(b.deadline)
            }
        })
    }

    override fun onDestroy() {}
    override fun getCount() = items.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position >= items.size) return RemoteViews(context.packageName, R.layout.widget_list_item)
        val item = items[position]
        val views = RemoteViews(context.packageName, R.layout.widget_list_item)

        views.setTextViewText(R.id.item_title, item.title)
        views.setTextColor(R.id.item_check, getCheckColor(quadrant))

        // 취소선 처리
        val isChecked = Prefs.getCheckedIds(context).contains(item.id)
        val flags = if (isChecked) Paint.STRIKE_THRU_TEXT_FLAG or Paint.ANTI_ALIAS_FLAG
                    else Paint.ANTI_ALIAS_FLAG
        views.setInt(R.id.item_title, "setPaintFlags", flags)
        views.setTextColor(R.id.item_title,
            if (isChecked) Color.parseColor("#AAAAAA") else Color.parseColor("#3D3D3D"))

        // D-day 표시
        val ddayInfo = getDdayInfo(item.deadline)
        if (ddayInfo != null) {
            views.setViewVisibility(R.id.item_dday, View.VISIBLE)
            views.setTextViewText(R.id.item_dday, ddayInfo.first)
            views.setTextColor(R.id.item_dday, Color.parseColor(ddayInfo.second))
        } else {
            views.setViewVisibility(R.id.item_dday, View.GONE)
        }

        // 체크 버튼 fill-in intent
        val fillIn = Intent().apply {
            putExtra(CompleteActionReceiver.EXTRA_PAGE_ID, item.id)
        }
        views.setOnClickFillInIntent(R.id.item_check, fillIn)

        return views
    }

    override fun getLoadingView() = null
    override fun getViewTypeCount() = 1
    override fun getItemId(position: Int) =
        if (position < items.size) items[position].id.hashCode().toLong() else position.toLong()
    override fun hasStableIds() = true

    /** D-day 계산. 2주(14일) 이내만 표시. null이면 미표시 */
    private fun getDdayInfo(deadline: String?): Pair<String, String>? {
        if (deadline == null) return null
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.time
            val deadlineDate = sdf.parse(deadline) ?: return null
            val diffDays = ((deadlineDate.time - today.time) / (1000 * 60 * 60 * 24)).toInt()

            if (diffDays > 14) return null

            val label = when {
                diffDays == 0 -> "D-Day"
                diffDays > 0  -> "D-$diffDays"
                else          -> "D+${-diffDays}"
            }
            val color = when {
                diffDays <= 3 -> "#FF5252"
                diffDays <= 7 -> "#FF9800"
                else          -> "#78909C"
            }
            Pair(label, color)
        } catch (e: Exception) { null }
    }

    private fun getCheckColor(q: String): Int = when (q) {
        TodoItem.P_Q1 -> Color.parseColor("#E57373")
        TodoItem.P_Q2 -> Color.parseColor("#F9A825")
        TodoItem.P_Q3 -> Color.parseColor("#66BB6A")
        else           -> Color.parseColor("#42A5F5")
    }
}
