package com.kimipro.launcher.module

object ModuleManager {
    val modules = LinkedHashMap<String, Module>()
    private val listeners = mutableListOf<(Module) -> Unit>()

    fun add(m: Module) {
        modules[m.name] = m
        listeners.toList().forEach { runCatching { it(m) } }
    }

    fun remove(name: String) { modules.remove(name) }
    fun get(name: String) = modules[name]
    fun names() = modules.keys.toTypedArray()
    fun tickAll() { modules.values.toList().forEach { it.tick() } }
    fun onChange(l: (Module) -> Unit) { listeners += l }
}
