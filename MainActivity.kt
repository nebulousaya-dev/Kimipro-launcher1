package com.kimipro.launcher

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kimipro.launcher.module.ModuleManager
import com.kimipro.launcher.overlay.MenuService
import com.kimipro.launcher.script.HookBus
import com.kimipro.launcher.script.ScriptManager

class MainActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var scriptsAdapter: ScriptsAdapter
    private lateinit var modulesAdapter: ModulesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
            setBackgroundColor(0xFF0E1116.toInt())
        }
        val title = TextView(this).apply {
            text = "KimiPro Launcher"
            textSize = 24f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(0, 0, 0, pad / 2)
        }
        val tabs = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        fun tabBtn(text: String, onClick: (View) -> Unit): Button =
            Button(this).apply {
                this.text = text
                setOnClickListener(onClick)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
        val btnScripts = tabBtn("Скрипты") { recycler.adapter = scriptsAdapter }
        val btnModules = tabBtn("Модули") { recycler.adapter = modulesAdapter }
        val btnOverlay = tabBtn("Меню") { openOverlay() }
        tabs.addView(btnScripts)
        tabs.addView(btnModules)
        tabs.addView(btnOverlay)

        recycler = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        root.addView(title)
        root.addView(tabs)
        root.addView(recycler)
        setContentView(root)

        ScriptManager.loadAll()
        scriptsAdapter = ScriptsAdapter(ScriptManager.scripts) { msg ->
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
        }
        modulesAdapter = ModulesAdapter { msg ->
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
        }
        ModuleManager.onChange {
            Handler(Looper.getMainLooper()).post { if (::modulesAdapter.isInitialized) modulesAdapter.refresh() }
        }
        recycler.adapter = scriptsAdapter

        HookBus.start(20)
    }

    private fun openOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            Toast.makeText(this, "Выдайте разрешение на отображение поверх других приложений", Toast.LENGTH_LONG).show()
        } else {
            ContextCompat.startForegroundService(this, Intent(this, MenuService::class.java))
        }
    }
}
