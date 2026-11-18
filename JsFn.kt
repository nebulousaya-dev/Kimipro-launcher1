package com.kimipro.launcher.script

import org.mozilla.javascript.BaseFunction
import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.Undefined

object JsFn {
    fun wrap(block: (Array<out Any?>) -> Any?): BaseFunction = object : BaseFunction() {
        override fun call(cx: Context, scope: Scriptable, thisObj: Scriptable, args: Array<out Any?>): Any? {
            val r = block(args)
            return if (r == null || r == Undefined.instance) Undefined.instance
            else Context.javaToJS(r, scope)
        }
    }

    fun call(f: Any?, vararg args: Any?) {
        if (f !is Function) return
        val cx = Context.enter()
        cx.optimizationLevel = -1
        try {
            val scope = f.parentScope ?: cx.initStandardObjects()
            val js = args.map { Context.javaToJS(it, scope) }.toTypedArray()
            f.call(cx, scope, scope, js)
        } finally {
            Context.exit()
        }
    }
}
