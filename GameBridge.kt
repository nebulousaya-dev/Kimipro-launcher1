package com.kimipro.launcher.game

/**
 * Мост к клиенту игры. Здесь лаунчер (или ваша модифицированная
 * клиентская часть) подключает реальные игровые функции — аналог
 * LocalPlayer/Level/Player из Variable Launcher.
 *
 * Без подключенного impl методы kp.game.* кидают IllegalStateException.
 */
object GameBridge {
    interface Impl {
        fun get(key: String): Any?          // "x", "y", "z", "yaw", "pitch", "health", ...
        fun setPosition(x: Double, y: Double, z: Double)
        fun setRotation(yaw: Double, pitch: Double)
        fun sendChat(text: String)
        fun display(text: String)           // сообщение в чат, видное только тебе
        fun isInGame(): Boolean
    }

    var impl: Impl? = null

    private fun requireImpl() = impl
        ?: throw IllegalStateException("GameBridge не подключён: реализуйте GameBridge.Impl в клиентской части")

    fun get(key: String): Any? = requireImpl().get(key)
    fun setPosition(x: Double, y: Double, z: Double) = requireImpl().setPosition(x, y, z)
    fun setRotation(yaw: Double, pitch: Double) = requireImpl().setRotation(yaw, pitch)
    fun sendChat(text: String) = requireImpl().sendChat(text)
    fun display(text: String) = requireImpl().display(text)
    fun isInGame(): Boolean = impl?.isInGame() ?: false
}
