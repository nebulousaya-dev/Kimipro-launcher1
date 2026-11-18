package com.kimipro.launcher.module

enum class Category { COMBAT, MOVEMENT, PLAYER, MISC, OTHER }

sealed class Setting(val name: String) {
    var visible = true
    abstract val type: String
}

class SliderSetting(
    name: String,
    val def: Double,
    val min: Double,
    val max: Double,
    val step: Double
) : Setting(name) {
    override val type = "slider"
    var value = def
    var onChange: ((Double) -> Unit)? = null
    fun set(v: Double) {
        value = v.coerceIn(min, max)
        onChange?.invoke(value)
    }
}

class ModeSetting(name: String, val modes: Array<String>) : Setting(name) {
    override val type = "mode"
    var current: String = modes.firstOrNull() ?: ""
    var onSelect: ((String) -> Unit)? = null
    fun select(m: String) {
        if (m in modes) {
            current = m
            onSelect?.invoke(m)
        }
    }
}

class StateSetting(name: String, var state: Boolean) : Setting(name) {
    override val type = "state"
    var onToggle: ((Boolean) -> Unit)? = null
    fun set(s: Boolean) {
        state = s
        onToggle?.invoke(s)
    }
}

class TextSetting(name: String, val hint: String, var text: String) : Setting(name) {
    override val type = "text"
    var onChange: ((String) -> Unit)? = null
    fun setText(t: String) {
        text = t
        onChange?.invoke(t)
    }
}

class ButtonSetting(name: String, var action: Any? = null) : Setting(name) {
    override val type = "button"
}

class Module(
    val name: String,
    val category: Category = Category.OTHER,
    val toggleable: Boolean = true,
    val bindable: Boolean = true
) {
    var active = false
        private set
    var bindKey: String? = null
    val settings = LinkedHashMap<String, Setting>()
    var onEnable: (() -> Unit)? = null
    var onDisable: (() -> Unit)? = null
    var onTickListener: (() -> Unit)? = null

    fun addSetting(s: Setting) { settings[s.name] = s }

    fun getSetting(name: String) =
        settings[name] ?: throw RuntimeException("Настройка '$name' не найдена в модуле '${this.name}'")

    fun toggle() { if (toggleable) setActive(!active) }

    fun setActive(a: Boolean) {
        if (!toggleable && a) return
        if (active == a) return
        active = a
        if (a) onEnable?.invoke() else onDisable?.invoke()
    }

    fun tick() {
        if (active) runCatching { onTickListener?.invoke() }
            .onFailure { android.util.Log.e("KimiPro", "tick error in $name", it) }
    }
}
