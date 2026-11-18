package com.kimipro.launcher.overlay

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import android.content.pm.ServiceInfo
import com.kimipro.launcher.module.ModuleManager

class MenuService : Service() {

    private lateinit var wm: WindowManager
    private var view: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        show()
    }

    private fun startForeground() {
        val channel = NotificationChannelCompat.Builder("kimipro", NotificationManagerCompat.IMPORTANCE_LOW)
            .setName("KimiPro")
            .build()
        NotificationManagerCompat.from(this).createNotificationChannel(channel)
        val notification = NotificationCompat.Builder(this, "kimipro")
            .setSmallIcon(android.R.drawable.ic_menu_more)
            .setContentTitle("KimiPro Launcher")
            .setContentText("Плавающее меню активно")
            .build()
        val type = if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0
        runCatching { ServiceCompat.startForeground(this, 1, notification, type) }
    }

    private fun show() {
        val density = resources.displayMetrics.density
        val pad = (14 * density).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
            setBackgroundColor(0xD9101418.toInt())
        }
        val handle = TextView(this).apply {
            text = "\u2630 KimiPro"
            textSize = 16f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(0, 0, 0, pad / 2)
        }
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        fun render() {
            list.removeAllViews()
            ModuleManager.modules.values.forEach { m ->
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    val p = (8 * density).toInt()
                    setPadding(0, p, 0, p)
                }
                val name = TextView(this).apply {
                    text = m.name + "  \u00B7  " + m.category
                    setTextColor(0xFFEEEEEE.toInt())
                    textSize = 14f
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                }
                val sw = SwitchCompat(this).apply {
                    isChecked = m.active
                    isEnabled = m.toggleable
                    setOnCheckedChangeListener { _, c -> runCatching { m.setActive(c) } }
                }
                row.addView(name)
                row.addView(sw)
                list.addView(row)
            }
        }
        render()
        ModuleManager.onChange {
            Handler(Looper.getMainLooper()).post { if (view != null) render() }
        }

        root.addView(handle)
        root.addView(ScrollView(this).apply { addView(list) })
        val close = Button(this).apply { text = "\u2715 Закрыть" }
        root.addView(close)

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            WindowManager.LayoutParams.TYPE_PHONE
        val lp = WindowManager.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 120
        }

        var dx = 0f
        var dy = 0f
        handle.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    dx = e.rawX - lp.x
                    dy = e.rawY - lp.y
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    lp.x = (e.rawX - dx).toInt()
                    lp.y = (e.rawY - dy).toInt()
                    wm.updateViewLayout(root, lp)
                    true
                }
                else -> false
            }
        }
        close.setOnClickListener { stopSelf() }

        wm.addView(root, lp)
        view = root
    }

    override fun onDestroy() {
        view?.let { runCatching { wm.removeView(it) } }
        view = null
        super.onDestroy()
    }
}
