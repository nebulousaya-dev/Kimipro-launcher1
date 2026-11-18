package com.kimipro.launcher

import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import com.kimipro.launcher.module.Module
import com.kimipro.launcher.module.ModuleManager

class ModulesAdapter(
    private val onError: (String) -> Unit
) : RecyclerView.Adapter<ModulesAdapter.VH>() {

    private var items: List<Module> = ModuleManager.modules.values.toList()

    fun refresh() {
        items = ModuleManager.modules.values.toList()
        notifyDataSetChanged()
    }

    class VH(val row: LinearLayout, val name: TextView, val sw: SwitchCompat) : RecyclerView.ViewHolder(row)

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val ctx = parent.context
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            val p = (10 * ctx.resources.displayMetrics.density).toInt()
            setPadding(0, p, 0, p)
        }
        val name = TextView(ctx).apply {
            setTextColor(0xFFDDDDDD.toInt())
            textSize = 15f
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val sw = SwitchCompat(ctx)
        row.addView(name)
        row.addView(sw)
        return VH(row, name, sw)
    }

    override fun onBindViewHolder(h: VH, position: Int) {
        val m = items[position]
        h.name.text = "${m.name}\n${m.category} · ${m.settings.size} настроек"
        h.sw.setOnCheckedChangeListener(null)
        h.sw.isChecked = m.active
        h.sw.isEnabled = m.toggleable
        h.sw.setOnCheckedChangeListener { _, checked ->
            runCatching { m.setActive(checked) }
                .onFailure { onError(it.message ?: "Ошибка"); h.sw.isChecked = m.active }
        }
        h.row.setOnLongClickListener {
            SettingsDialog.show(h.row.context, m, onError)
            true
        }
        h.row.setOnClickListener {
            SettingsDialog.show(h.row.context, m, onError)
        }
    }
}
