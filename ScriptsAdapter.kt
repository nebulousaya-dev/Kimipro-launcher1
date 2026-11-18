package com.kimipro.launcher

import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import com.kimipro.launcher.script.ScriptManager

class ScriptsAdapter(
    private val scripts: List<ScriptManager.Script>,
    private val onError: (String) -> Unit
) : RecyclerView.Adapter<ScriptsAdapter.VH>() {

    class VH(val row: LinearLayout, val name: TextView, val sw: SwitchCompat) : RecyclerView.ViewHolder(row)

    override fun getItemCount() = scripts.size

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
        val s = scripts[position]
        h.name.text = s.file.name + (s.error?.let { "\n\u26A0 $it" } ?: "")
        h.sw.setOnCheckedChangeListener(null)
        h.sw.isChecked = s.enabled
        h.sw.setOnCheckedChangeListener { _, checked ->
            ScriptManager.setEnabled(s, checked)
            s.error?.let(onError)
            notifyItemChanged(position)
        }
    }
}
