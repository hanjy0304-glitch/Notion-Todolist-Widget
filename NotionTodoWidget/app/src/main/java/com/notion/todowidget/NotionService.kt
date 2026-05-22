package com.notion.todowidget

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object NotionService {

    private const val NOTION_VERSION = "2022-06-28"

    fun fetchTodosWithError(context: Context): Pair<List<TodoItem>?, String> {
        val token = Prefs.getToken(context)
        val dbId = Prefs.getDbId(context).replace("-", "").trim()
        if (token.isEmpty()) return Pair(null, "API 토큰이 비어있습니다")
        if (dbId.isEmpty()) return Pair(null, "데이터베이스 ID가 비어있습니다")

        return try {
            val url = URL("https://api.notion.com/v1/databases/$dbId/query")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Notion-Version", NOTION_VERSION)
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 15000
                readTimeout = 15000
                doOutput = true
            }

            val body = """{"page_size": 100}"""
            conn.outputStream.bufferedWriter().use { it.write(body) }

            val code = conn.responseCode
            if (code != 200) {
                val errMsg = try {
                    conn.errorStream?.bufferedReader()?.readText() ?: "응답 없음"
                } catch (e: Exception) { "에러 읽기 실패" }
                return Pair(null, "HTTP $code\n$errMsg")
            }

            val response = conn.inputStream.bufferedReader().readText()
            val items = parseResults(response).filter { it.status != "완료" }
            Pair(items, "")
        } catch (e: Exception) {
            Pair(null, "네트워크 오류: ${e.message}")
        }
    }

    fun fetchTodos(context: Context): List<TodoItem>? = fetchTodosWithError(context).first

    fun resetItem(context: Context, pageId: String): Boolean {
        val token = Prefs.getToken(context)
        if (token.isEmpty()) return false
        return try {
            val url = java.net.URL("https://api.notion.com/v1/pages/$pageId")
            val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
                requestMethod = "PATCH"
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Notion-Version", NOTION_VERSION)
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 15000; readTimeout = 15000; doOutput = true
            }
            val body = """{"properties":{"상태":{"status":{"name":"시작 전"}}}}"""
            conn.outputStream.bufferedWriter().use { it.write(body) }
            conn.responseCode == 200
        } catch (e: Exception) { false }
    }

    fun completeItem(context: Context, pageId: String): Boolean {
        val token = Prefs.getToken(context)
        if (token.isEmpty()) return false

        return try {
            val url = URL("https://api.notion.com/v1/pages/$pageId")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "PATCH"
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Notion-Version", NOTION_VERSION)
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 15000
                readTimeout = 15000
                doOutput = true
            }

            val body = """{"properties":{"상태":{"status":{"name":"완료"}}}}"""
            conn.outputStream.bufferedWriter().use { it.write(body) }
            conn.responseCode == 200
        } catch (e: Exception) {
            false
        }
    }

    private fun parseResults(response: String): List<TodoItem> {
        val items = mutableListOf<TodoItem>()
        return try {
            val json = JSONObject(response)
            val results = json.getJSONArray("results")

            for (i in 0 until results.length()) {
                val page = results.getJSONObject(i)
                val id = page.getString("id")
                val props = page.getJSONObject("properties")

                val title = getTitleValue(props, "이름")
                    ?: getTitleValue(props, "Name")
                    ?: getTitleValue(props, "제목")
                    ?: continue

                if (title.isBlank()) continue

                val status = try {
                    props.getJSONObject("상태").getJSONObject("status").getString("name")
                } catch (e: Exception) { "시작 전" }

                val priority = try {
                    props.getJSONObject("우선순위 평가").getJSONObject("select").getString("name")
                } catch (e: Exception) { TodoItem.P_Q4 }

                val deadline = try {
                    props.getJSONObject("마감일").optJSONObject("date")?.getString("start")
                } catch (e: Exception) { null }

                items.add(TodoItem(id, title, priority, status, deadline))
            }
            items
        } catch (e: Exception) {
            items
        }
    }

    private fun getTitleValue(props: JSONObject, key: String): String? {
        return try {
            val arr = props.getJSONObject(key).getJSONArray("title")
            if (arr.length() > 0) arr.getJSONObject(0).getString("plain_text").trim() else null
        } catch (e: Exception) { null }
    }
}
