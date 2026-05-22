package com.notion.todowidget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.widget.SwitchCompat
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.notion.todowidget.databinding.ActivityMainBinding
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.etToken.setText(Prefs.getToken(this))
        binding.etDbId.setText(Prefs.getDbId(this))

        // 투명도 슬라이더 초기값
        val widgetAlpha = (Prefs.getWidgetAlpha(this) * 100).toInt()
        val quadrantAlpha = (Prefs.getQuadrantAlpha(this) * 100).toInt()
        binding.seekWidgetAlpha.progress = widgetAlpha
        binding.seekQuadrantAlpha.progress = quadrantAlpha
        binding.tvWidgetAlphaVal.text = "$widgetAlpha%"
        binding.tvQuadrantAlphaVal.text = "$quadrantAlpha%"

        // 취소선 모드 스위치
        binding.switchStrikethrough.isChecked = Prefs.isStrikethroughMode(this)
        binding.switchStrikethrough.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setStrikethroughMode(this, isChecked)
        }

        binding.seekWidgetAlpha.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                binding.tvWidgetAlphaVal.text = "$progress%"
                Prefs.setWidgetAlpha(this@MainActivity, progress / 100f)
                refreshWidget()
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

        binding.seekQuadrantAlpha.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                binding.tvQuadrantAlphaVal.text = "$progress%"
                Prefs.setQuadrantAlpha(this@MainActivity, progress / 100f)
                refreshWidget()
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

        binding.btnSave.setOnClickListener {
            val token = binding.etToken.text.toString().trim()
            val dbId = binding.etDbId.text.toString().trim()

            if (token.isEmpty() || dbId.isEmpty()) {
                Toast.makeText(this, "API 토큰과 데이터베이스 ID를 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Prefs.setToken(this, token)
            Prefs.setDbId(this, dbId)

            binding.btnSave.isEnabled = false
            binding.tvStatus.visibility = View.VISIBLE
            binding.tvStatus.text = "노션에 연결 중..."

            thread {
                val result = NotionService.fetchTodosWithError(this)
                runOnUiThread {
                    binding.btnSave.isEnabled = true
                    if (result.first != null) {
                        val items = result.first!!
                        Prefs.setCache(this, TodoItem.listToJson(items))
                        Prefs.setLastSync(this, System.currentTimeMillis())
                        SyncWorker.schedulePeriodic(this)
                        refreshWidget()
                        binding.tvStatus.text = "✅ 연결 성공! 투두 ${items.size}개 불러왔어요.\n\n이제 홈화면으로 나가서 위젯을 추가하세요."
                    } else {
                        binding.tvStatus.text = "❌ 연결 실패\n\n${result.second}"
                    }
                }
            }
        }

        binding.btnSync.setOnClickListener {
            binding.btnSync.isEnabled = false
            binding.tvStatus.visibility = View.VISIBLE
            binding.tvStatus.text = "동기화 중..."

            thread {
                val result = NotionService.fetchTodosWithError(this)
                runOnUiThread {
                    binding.btnSync.isEnabled = true
                    if (result.first != null) {
                        val items = result.first!!
                        Prefs.setCache(this, TodoItem.listToJson(items))
                        Prefs.setLastSync(this, System.currentTimeMillis())
                        refreshWidget()
                        binding.tvStatus.text = "✅ 동기화 완료! 투두 ${items.size}개 불러왔어요."
                    } else {
                        binding.tvStatus.text = "❌ 동기화 실패\n\n${result.second}"
                    }
                }
            }
        }
    }

    private fun refreshWidget() {
        val mgr = AppWidgetManager.getInstance(this)
        val ids = mgr.getAppWidgetIds(ComponentName(this, TodoWidgetProvider::class.java))
        TodoWidgetProvider.updateWidgets(this, mgr, ids)
    }
}
