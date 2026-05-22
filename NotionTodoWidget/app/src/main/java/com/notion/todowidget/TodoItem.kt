package com.notion.todowidget

import org.json.JSONArray
import org.json.JSONObject

data class TodoItem(
    val id: String,
    val title: String,
    val priority: String,
    val status: String,
    val deadline: String? = null  // "YYYY-MM-DD" or null
) {
    companion object {
        const val P_Q1 = "중요+긴급"
        const val P_Q2 = "중요+비긴급"
        const val P_Q3 = "비중요+긴급"
        const val P_Q4 = "비중요+비긴급"

        fun fromJson(obj: JSONObject) = TodoItem(
            id = obj.getString("id"),
            title = obj.getString("title"),
            priority = obj.getString("priority"),
            status = obj.getString("status"),
            deadline = if (obj.has("deadline") && !obj.isNull("deadline")) obj.getString("deadline") else null
        )

        fun listFromJson(json: String): List<TodoItem> {
            return try {
                val arr = JSONArray(json)
                (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }
            } catch (e: Exception) { emptyList() }
        }

        fun listToJson(items: List<TodoItem>): String {
            val arr = JSONArray()
            items.forEach { item ->
                arr.put(JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("priority", item.priority)
                    put("status", item.status)
                    if (item.deadline != null) put("deadline", item.deadline) else put("deadline", JSONObject.NULL)
                })
            }
            return arr.toString()
        }
    }
}
