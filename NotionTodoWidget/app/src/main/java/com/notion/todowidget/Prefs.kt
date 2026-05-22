package com.notion.todowidget

import android.content.Context

object Prefs {
    private const val PREF_NAME = "notion_todo_prefs"
    private const val KEY_TOKEN = "api_token"
    private const val KEY_DB_ID = "database_id"
    private const val KEY_CACHE = "todo_cache"
    private const val KEY_LAST_SYNC = "last_sync"
    private const val KEY_WIDGET_ALPHA = "widget_alpha"
    private const val KEY_QUADRANT_ALPHA = "quadrant_alpha"

    fun getToken(context: Context): String =
        prefs(context).getString(KEY_TOKEN, "") ?: ""
    fun setToken(context: Context, token: String) =
        prefs(context).edit().putString(KEY_TOKEN, token).apply()

    fun getDbId(context: Context): String =
        prefs(context).getString(KEY_DB_ID, "") ?: ""
    fun setDbId(context: Context, dbId: String) =
        prefs(context).edit().putString(KEY_DB_ID, dbId.trim()).apply()

    fun getCache(context: Context): String =
        prefs(context).getString(KEY_CACHE, "[]") ?: "[]"
    fun setCache(context: Context, json: String) =
        prefs(context).edit().putString(KEY_CACHE, json).apply()

    fun getLastSync(context: Context): Long =
        prefs(context).getLong(KEY_LAST_SYNC, 0L)
    fun setLastSync(context: Context, time: Long) =
        prefs(context).edit().putLong(KEY_LAST_SYNC, time).apply()

    fun getWidgetAlpha(context: Context): Float =
        prefs(context).getFloat(KEY_WIDGET_ALPHA, 1.0f)
    fun setWidgetAlpha(context: Context, alpha: Float) =
        prefs(context).edit().putFloat(KEY_WIDGET_ALPHA, alpha).apply()

    fun getQuadrantAlpha(context: Context): Float =
        prefs(context).getFloat(KEY_QUADRANT_ALPHA, 1.0f)
    fun setQuadrantAlpha(context: Context, alpha: Float) =
        prefs(context).edit().putFloat(KEY_QUADRANT_ALPHA, alpha).apply()

    fun isConfigured(context: Context): Boolean =
        getToken(context).isNotEmpty() && getDbId(context).isNotEmpty()

    private const val KEY_STRIKETHROUGH = "strikethrough_mode"
    private const val KEY_CHECKED_IDS = "checked_ids"

    fun isStrikethroughMode(context: Context): Boolean =
        prefs(context).getBoolean(KEY_STRIKETHROUGH, false)
    fun setStrikethroughMode(context: Context, enabled: Boolean) =
        prefs(context).edit().putBoolean(KEY_STRIKETHROUGH, enabled).apply()

    fun getCheckedIds(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_CHECKED_IDS, emptySet()) ?: emptySet()
    fun addCheckedId(context: Context, id: String) {
        val set = getCheckedIds(context).toMutableSet()
        set.add(id)
        prefs(context).edit().putStringSet(KEY_CHECKED_IDS, set).apply()
    }
    fun removeCheckedId(context: Context, id: String) {
        val set = getCheckedIds(context).toMutableSet()
        set.remove(id)
        prefs(context).edit().putStringSet(KEY_CHECKED_IDS, set).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
}
