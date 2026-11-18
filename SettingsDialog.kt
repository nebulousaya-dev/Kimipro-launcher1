package com.kimipro.launcher

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.kimipro.launcher.module.ButtonSetting
import com.kimipro.launcher.module.ModeSetting
import com.kimipro.launcher.module.Module
import com.kimipro.launcher.module.Setting
import com.kimipro.launcher.module.SliderSetting
import com.kimipro.launcher.module.StateSetting
import com.kimipro.launcher.module.TextSetting
import com.kimipro.launcher.script.JsFn
import java.util.Locale

object SettingsDialog {

    fun show(context: Context, m: Module, onError: (String) -> Unit) {
        if (m.settings.isEmpty()) {
            Toast.makeText(context, "У модуля нет настроек", Toast.LENGTH_SHORT).show()
            return
        }
        val density = context.resources.displayMetrics.density
        val hPad = (20 * density).toInt()
        val box = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(hPad, hPad / 2, hPad, 0)
        }

        m.settings.values.filter { it.visible }.forEach { s ->
            when (s) {
                is SliderSetting -> box.addView(sliderView(context, s))
                is ModeSetting -> box.addView(modeView(context, s))
                is StateSetting -> box.addView(stateView(context, s, onError))
                is TextSetting -> box.addView(textView(context, s))
                is ButtonSetting -> box.addView(buttonView(context, s, onError))
            }
        }

        AlertDialog.Builder(context)
            .setTitle(m.name)
            .setView(ScrollView(context).apply { addView(box) })
            .setPositiveButton("Готово", null)
            .show()
    }

    private fun label(context: Context, text: String): TextView =
        TextView(context).apply {
            this.text = text
            setTextColor(0xFFBBBBBB.toInt())
            textSize = 13f
        }

    private fun sliderView(context: Context, s: SliderSetting): View {
        val box = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val valueText = label(context, "")
        fun apply(p: Int, fire: Boolean) {
            val v = s.min + p * s.step
            valueText.text = String.format(Locale.US, "%s: %.2f", s.name, v)
            if (fire) runCatching { s.set(v) }
        }
        val steps = ((s.max - s.min) / s.step).toInt().coerceAtLeast(1)
        val seek = SeekBar(context).apply {
            max = steps
            progress = (((s.value - s.min) / s.step).toInt()).coerceIn(0, steps)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) { if (fromUser) apply(p, true) }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }
        box.addView(valueText)
        box.addView(seek)
        apply(seek.progress, false)
        return box
    }

    private fun modeView(context: Context, s: ModeSetting): View {
        val b = Button(context).apply { text = "${s.name}: ${s.current}" }
        b.setOnClickListener {
            AlertDialog.Builder(context)
                .setTitle(s.name)
                .setItems(s.modes) { _, which ->
                    runCatching { s.select(s.modes[which]) }
                    b.text = "${s.name}: ${s.current}"
                }
                .show()
        }
        return b
    }

    private fun stateView(context: Context, s: StateSetting, onError: (String) -> Unit): View =
        CheckBox(context).apply {
            text = s.name
            isChecked = s.state
            setOnCheckedChangeListener { _, c ->
                runCatching { s.set(c) }.onFailure { onError(it.message ?: "Ошибка"); isChecked = s.state }
            }
        }

    private fun textView(context: Context, s: TextSetting): View {
        val box = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        box.addView(label(context, s.name))
        val et = EditText(context).apply {
            hint = s.hint
            setText(s.text)
            setTextColor(0xFFFFFFFF.toInt())
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                override fun afterTextChanged(t: Editable?) {
                    runCatching { s.setText(t?.toString() ?: "") }
                }
            })
        }
        box.addView(et)
        return box
    }

    private fun buttonView(context: Context, s: ButtonSetting, onError: (String) -> Unit): View =
        Button(context).apply {
            text = s.name
            setOnClickListener {
                runCatching { JsFn.call(s.action) }.onFailure { onError(it.message ?: "Ошибка") }
            }
        }
}
