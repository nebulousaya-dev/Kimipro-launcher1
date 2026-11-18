package com.kimipro.launcher.script

import android.util.Log
import android.widget.Toast
import com.kimipro.launcher.App
import com.kimipro.launcher.game.GameBridge
import com.kimipro.launcher.module.ButtonSetting
import com.kimipro.launcher.module.Category
import com.kimipro.launcher.module.ModeSetting
import com.kimipro.launcher.module.Module
import com.kimipro.launcher.module.ModuleManager
import com.kimipro.launcher.module.Setting
import com.kimipro.launcher.module.SliderSetting
import com.kimipro.launcher.module.StateSetting
import com.kimipro.launcher.module.TextSetting
import com.kimipro.launcher.prefs.Prefs
import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.NativeArray
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.Undefined

/**
 * KP API v1 — новый API KimiPro Launcher для скриптов.
 *
 * Глобальный объект `kp` доступен в каждом скрипте:
 *   kp.version, kp.log(...), kp.toast(text)
 *   kp.module(name, category)            -> модуль
 *   kp.setting.slider/mode/state/text/button(...)
 *   kp.data.get/set/remove/all(...)
 *   kp.on("tick"|"enable"|"disable", fn)
 *   kp.tickRate(20)
 *   kp.game.*                            -> мост к клиенту игры
 */
object KPApi {

    fun register(cx: Context, scope: ScriptableObject, hooks: MutableMap<String, MutableList<Function>>) {
        val kp = cx.newObject(scope)

        ScriptableObject.putProperty(kp, "version", "1.0.0")

        putFn(kp, "log") { args ->
            Log.i("KimiPro", args.joinToString(" ") { it?.toString() ?: "null" })
            Undefined.instance
        }
        putFn(kp, "toast") { args ->
            Toast.makeText(App.app, args.firstOrNull()?.toString() ?: "", Toast.LENGTH_SHORT).show()
            Undefined.instance
        }
        putFn(kp, "on") { args ->
            val ev = args.getOrNull(0)?.toString() ?: return@putFn Undefined.instance
            (args.getOrNull(1) as? Function)?.let { hooks.getOrPut(ev) { mutableListOf() }.add(it) }
            Undefined.instance
        }
        putFn(kp, "tickRate") { args ->
            (args.getOrNull(0) as? Number)?.let { HookBus.setRate(it.toInt()) }
            Undefined.instance
        }

        // ---- kp.setting.* ----
        val setting = cx.newObject(scope)
        putFn(setting, "slider") { args ->
            val s = SliderSetting(
                args.getOrNull(0)?.toString() ?: "slider",
                num(args, 1, 1.0), num(args, 2, 0.0), num(args, 3, 10.0), num(args, 4, 0.1)
            )
            settingJs(cx, scope, s)
        }
        putFn(setting, "mode") { args ->
            val modes = (args.getOrNull(1) as? NativeArray)
                ?.map { it.toString() }?.toTypedArray() ?: arrayOf("default")
            settingJs(cx, scope, ModeSetting(args.getOrNull(0)?.toString() ?: "mode", modes))
        }
        putFn(setting, "state") { args ->
            val def = args.getOrNull(1) == true || args.getOrNull(1)?.toString() == "true"
            settingJs(cx, scope, StateSetting(args.getOrNull(0)?.toString() ?: "state", def))
        }
        putFn(setting, "text") { args ->
            settingJs(cx, scope, TextSetting(
                args.getOrNull(0)?.toString() ?: "text",
                args.getOrNull(1)?.toString() ?: "",
                args.getOrNull(2)?.toString() ?: ""
            ))
        }
        putFn(setting, "button") { args ->
            settingJs(cx, scope, ButtonSetting(args.getOrNull(0)?.toString() ?: "button", args.getOrNull(1)))
        }
        ScriptableObject.putProperty(kp, "setting", setting)

        // ---- kp.module(name, category) ----
        putFn(kp, "module") { args ->
            val name = args.getOrNull(0)?.toString() ?: "module"
            val cat = runCatching { Category.valueOf(args.getOrNull(1)?.toString()?.uppercase() ?: "OTHER") }
                .getOrDefault(Category.OTHER)
            val m = Module(name, cat)
            ModuleManager.add(m)
            moduleJs(cx, scope, m)
        }

        // ---- kp.data.* ----
        val data = cx.newObject(scope)
        putFn(data, "get") { args ->
            Prefs.get(args.getOrNull(0)?.toString() ?: "", args.getOrNull(1)) ?: Undefined.instance
        }
        putFn(data, "set") { args ->
            Prefs.set(args.getOrNull(0)?.toString() ?: "", args.getOrNull(1))
            Undefined.instance
        }
        putFn(data, "remove") { args ->
            Prefs.remove(args.getOrNull(0)?.toString() ?: "")
            Undefined.instance
        }
        putFn(data, "all") { Prefs.all() }
        ScriptableObject.putProperty(kp, "data", data)

        // ---- kp.game.* (мост к клиенту игры) ----
        val game = cx.newObject(scope)
        putFn(game, "playerX") { GameBridge.get("x") ?: Undefined.instance }
        putFn(game, "playerY") { GameBridge.get("y") ?: Undefined.instance }
        putFn(game, "playerZ") { GameBridge.get("z") ?: Undefined.instance }
        putFn(game, "yaw") { GameBridge.get("yaw") ?: Undefined.instance }
        putFn(game, "pitch") { GameBridge.get("pitch") ?: Undefined.instance }
        putFn(game, "health") { GameBridge.get("health") ?: Undefined.instance }
        putFn(game, "isInGame") { GameBridge.isInGame() }
        putFn(game, "setPosition") { args ->
            GameBridge.setPosition(num(args, 0), num(args, 1), num(args, 2)); Undefined.instance
        }
        putFn(game, "setRotation") { args ->
            GameBridge.setRotation(num(args, 0), num(args, 1)); Undefined.instance
        }
        putFn(game, "sendChat") { args ->
            GameBridge.sendChat(args.getOrNull(0)?.toString() ?: ""); Undefined.instance
        }
        putFn(game, "chatMsg") { args ->
            GameBridge.display(args.getOrNull(0)?.toString() ?: ""); Undefined.instance
        }
        ScriptableObject.putProperty(kp, "game", game)

        ScriptableObject.putProperty(scope, "kp", kp)
    }

    private fun num(args: Array<out Any?>, i: Int, def: Double = 0.0) =
        (args.getOrNull(i) as? Number)?.toDouble() ?: def

    private fun putFn(obj: Scriptable, name: String, block: (Array<out Any?>) -> Any?) {
        ScriptableObject.putProperty(obj, name, JsFn.wrap(block))
    }

    /** JS-прокси для настройки; реальный объект лежит в свойстве __setting. */
    private fun settingJs(cx: Context, scope: Scriptable, s: Setting): ScriptableObject {
        val o = cx.newObject(scope)
        ScriptableObject.putProperty(o, "__setting", s)
        ScriptableObject.putProperty(o, "type", s.type)

        putFn(o, "get") {
            when (s) {
                is SliderSetting -> s.value
                is ModeSetting -> s.current
                is StateSetting -> s.state
                is TextSetting -> s.text
                else -> Undefined.instance
            } ?: Undefined.instance
        }
        putFn(o, "set") { args ->
            when (s) {
                is SliderSetting -> s.set(num(args, 0))
                is ModeSetting -> s.select(args.getOrNull(0)?.toString() ?: "")
                is StateSetting -> s.set(args.getOrNull(0) == true || args.getOrNull(0)?.toString() == "true")
                is TextSetting -> s.setText(args.getOrNull(0)?.toString() ?: "")
                else -> {}
            }
            Undefined.instance
        }
        putFn(o, "onChange") { args ->
            val f = args.getOrNull(0) as? Function
            if (f != null) when (s) {
                is SliderSetting -> s.onChange = { v -> JsFn.call(f, v) }
                is ModeSetting -> s.onSelect = { m -> JsFn.call(f, m) }
                is StateSetting -> s.onToggle = { b -> JsFn.call(f, b) }
                is TextSetting -> s.onChange = { t -> JsFn.call(f, t) }
                else -> {}
            }
            Undefined.instance
        }
        putFn(o, "show") { args ->
            s.visible = args.getOrNull(0)?.toString() != "false"
            Undefined.instance
        }
        return o
    }

    /** JS-прокси для модуля. */
    private fun moduleJs(cx: Context, scope: Scriptable, m: Module): ScriptableObject {
        val o = cx.newObject(scope)
        putFn(o, "getName") { m.name }
        putFn(o, "getCategory") { m.category.name }
        putFn(o, "isActive") { m.active }
        putFn(o, "toggle") { m.toggle(); Undefined.instance }
        putFn(o, "setActive") { args ->
            m.setActive(args.getOrNull(0) == true || args.getOrNull(0)?.toString() == "true")
            Undefined.instance
        }
        putFn(o, "bind") { args ->
            m.bindKey = args.getOrNull(0)?.toString(); Undefined.instance
        }
        putFn(o, "getBind") { m.bindKey ?: Undefined.instance }
        putFn(o, "add") { args ->
            val proxy = args.getOrNull(0) as? Scriptable
            val s = proxy?.get("__setting", proxy) as? Setting
            if (s != null) m.addSetting(s)
            Undefined.instance
        }
        putFn(o, "get") { args ->
            m.settings[args.getOrNull(0)?.toString()] ?: throw RuntimeException(
                "Настройка '${args.getOrNull(0)}' не найдена в модуле '${m.name}'"
            )
        }
        putFn(o, "settings") { m.settings.values.toTypedArray() }
        putFn(o, "onEnable") { args -> m.onEnable = { JsFn.call(args.getOrNull(0)) }; Undefined.instance }
        putFn(o, "onDisable") { args -> m.onDisable = { JsFn.call(args.getOrNull(0)) }; Undefined.instance }
        putFn(o, "onTick") { args -> m.onTickListener = { JsFn.call(args.getOrNull(0)) }; Undefined.instance }
        return o
    }
}
