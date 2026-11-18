package com.kimipro.launcher.script

import android.util.Log
import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.ScriptableObject

object Engine {
    class Handle(
        val name: String,
        val cx: Context,
        val scope: ScriptableObject,
        val hooks: MutableMap<String, MutableList<Function>>
    )

    private val running = LinkedHashMap<String, Handle>()
    val handles: Collection<Handle> get() = running.values

    fun enableScript(name: String, code: String) {
        check(!running.containsKey(name)) { "Скрипт уже запущен: $name" }
        val cx = Context.enter()
        cx.optimizationLevel = -1
        cx.languageVersion = Context.VERSION_ES6
        try {
            val scope = cx.initStandardObjects()
            val hooks = mutableMapOf<String, MutableList<Function>>()
            KPApi.register(cx, scope, hooks)
            cx.evaluateString(scope, code, name, 1, null)
            val h = Handle(name, cx, scope, hooks)
            running[name] = h
            callHook(h, "onEnable")
        } catch (t: Throwable) {
            running.remove(name)
            Context.exit()
            throw t
        }
    }

    fun disableScript(name: String) {
        val h = running.remove(name) ?: return
        runCatching { callHook(h, "onDisable") }
        Context.exit()
    }

    fun isRunning(name: String) = running.containsKey(name)

    private fun callHook(h: Handle, name: String) {
        val f = h.scope.get(name, h.scope)
        if (f is Function) JsFn.call(f)
        h.hooks[name]?.toList()?.forEach { JsFn.call(it) }
    }

    fun tick() {
        running.values.toList().forEach { h ->
            runCatching { callHook(h, "onTick") }
                .onFailure { Log.e("KimiPro", "onTick error in ${h.name}", it) }
        }
        ModuleManagerHolder.tickAll()
    }

    // чтобы избежать циклического импорта на уровне инициализации
    private object ModuleManagerHolder {
        fun tickAll() = com.kimipro.launcher.module.ModuleManager.tickAll()
    }
}
