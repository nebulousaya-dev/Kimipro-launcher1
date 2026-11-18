package com.kimipro.launcher.script

import android.os.Environment
import com.kimipro.launcher.App
import java.io.File

object ScriptManager {
    data class Script(val file: File, var enabled: Boolean = false, var error: String? = null)

    private val _scripts = mutableListOf<Script>()
    val scripts: List<Script> get() = _scripts

    val dir: File
        get() {
            val legacy = File(Environment.getExternalStorageDirectory(), "KimiPro/scripts")
            val scoped = File(App.app.getExternalFilesDir(null), "scripts")
            return when {
                legacy.exists() -> legacy
                legacy.mkdirs() -> legacy
                else -> scoped.apply { mkdirs() }
            }
        }

    fun loadAll() {
        _scripts.clear()
        dir.listFiles { f -> f.isFile && f.extension.equals("js", true) }
            ?.sortedBy { it.name }
            ?.forEach { _scripts += Script(it) }
    }

    fun setEnabled(s: Script, enabled: Boolean) {
        if (enabled == s.enabled) return
        s.error = null
        try {
            if (enabled) Engine.enableScript(s.file.name, s.file.readText())
            else Engine.disableScript(s.file.name)
            s.enabled = enabled
        } catch (t: Throwable) {
            s.error = t.message ?: t.toString()
            if (enabled) runCatching { Engine.disableScript(s.file.name) }
            s.enabled = false
        }
    }
}
