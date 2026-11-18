package com.kimipro.launcher.script

import android.os.Handler
import android.os.Looper

object HookBus {
    private val handler = Handler(Looper.getMainLooper())
    @Volatile private var rate = 20

    private val loop = object : Runnable {
        override fun run() {
            Engine.tick()
            handler.postDelayed(this, (1000L / rate).coerceAtLeast(5))
        }
    }

    fun start(ticksPerSecond: Int = 20) {
        rate = ticksPerSecond
        stop()
        handler.post(loop)
    }

    fun stop() { handler.removeCallbacks(loop) }

    fun setRate(r: Int) { if (r > 0) rate = r }
}
