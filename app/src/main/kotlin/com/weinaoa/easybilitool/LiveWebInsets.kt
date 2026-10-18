/*
Copyright (c) 2026 weinaoa
EasyBiliTool is licensed under Mulan PubL v2.
You can use this software according to the terms and conditions of the Mulan PubL v2.
You may obtain a copy of Mulan PubL v2 at:
    http://license.coscl.org.cn/MulanPubL-2.0
THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
See the Mulan PubL v2 for more details.
*/
package com.weinaoa.easybilitool

import android.app.Activity
import android.graphics.Color
import android.net.Uri
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import java.lang.reflect.Proxy
import org.json.JSONObject

/** Navigation-only adapter for the host's live following list. No painted footer. */
internal class LiveWebInsets(private val activity: Activity) {
    private var current: View? = null
    private val savedPadding = mutableMapOf<View, Int>()
    private val savedMargin = mutableMapOf<View, Int>()
    private var epoch = 0
    private var nextEvaluation = 0L
    private var lastReport = ""
    var background: Int? = null
        private set

    fun find(): View? {
        if (activity.javaClass.name != "com.bililive.bililive.infra.hybrid.ui.WebViewActivity") return null
        return runCatching {
            val view = Reflector.callMethod(activity, "getLiveWeb") as? View ?: return null
            val address = Uri.parse(Reflector.callMethod(view, "getUrl") as? String ?: return null)
            view.takeIf { address.scheme == "https" && address.host == "live.bilibili.com" &&
                address.path == "/p/html/live-app-my-focus/index.html" }
        }.getOrNull()
    }

    fun apply(view: View, content: View, navigationHeight: Int): Boolean {
        if (current !== view) {
            restore()
            current = view
        }
        var changed = false
        var node: View? = view
        while (node != null) {
            val item = node
            if (item.paddingBottom == navigationHeight) {
                savedPadding.putIfAbsent(item, item.paddingBottom)
                item.setPadding(item.paddingLeft, item.paddingTop, item.paddingRight, 0)
                changed = true
            }
            val params = item.layoutParams as? ViewGroup.MarginLayoutParams
            if (params?.bottomMargin == navigationHeight) {
                savedMargin.putIfAbsent(item, params.bottomMargin)
                params.bottomMargin = 0
                item.layoutParams = params
                changed = true
            }
            if (item === content) break
            node = item.parent as? View
        }
        if (SystemClock.uptimeMillis() >= nextEvaluation) {
            nextEvaluation = SystemClock.uptimeMillis() + 1000
            val requestEpoch = epoch
            val dp = navigationHeight / activity.resources.displayMetrics.density
            evaluate(view, pageScript(dp)) { response ->
                if (requestEpoch != epoch || current !== view) return@evaluate
                runCatching {
                    val data = JSONObject(response)
                    val rgb = Regex("[\\d.]+").findAll(data.optString("background")).map { it.value.toFloat() }.toList()
                    if (rgb.size >= 3 && (rgb.size == 3 || rgb[3] == 1f)) {
                        background = Color.rgb(rgb[0].toInt(), rgb[1].toInt(), rgb[2].toInt())
                    }
                    val report = "viewport=${data.optInt("height")} safe=${data.optInt("safe")}"
                    if (lastReport != report) {
                        lastReport = report
                        HookRuntime.log("[EasyBiliTool] LIVE_WEB_INSETS $report")
                    }
                }
            }
        }
        return changed
    }

    fun restore() {
        epoch++
        current?.let { evaluate(it, "window.__easyBiliNav&&window.__easyBiliNav.undo()") {} }
        current = null
        background = null
        nextEvaluation = 0
        savedPadding.forEach { (view, value) ->
            if (view.paddingBottom == 0) view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, value)
        }
        savedMargin.forEach { (view, value) ->
            val params = view.layoutParams as? ViewGroup.MarginLayoutParams
            if (params?.bottomMargin == 0) { params.bottomMargin = value; view.layoutParams = params }
        }
        savedPadding.clear()
        savedMargin.clear()
    }

    private fun evaluate(view: View, script: String, done: (String) -> Unit) {
        try {
            val method = view.javaClass.methods.singleOrNull {
                it.name == "evaluateJavascript" && it.parameterTypes.size == 2 &&
                    it.parameterTypes[1].name == "com.bilibili.app.comm.bhwebview.api.interfaces.ValueCallback"
            } ?: return
            val type = method.parameterTypes[1]
            val callback = Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { proxy, called, args ->
                when (called.name) {
                    "onReceiveValue" -> { (args?.getOrNull(0) as? String)?.let(done); null }
                    "hashCode" -> System.identityHashCode(proxy)
                    "equals" -> proxy === args?.getOrNull(0)
                    "toString" -> "EasyBiliNavigationCallback"
                    else -> null
                }
            }
            method.invoke(view, script, callback)
        } catch (error: Exception) {
            HookRuntime.log("[EasyBiliTool] LIVE_WEB_INSETS_BYPASS ${error.javaClass.simpleName}")
        }
    }

    private fun pageScript(dp: Float) = """
        (function(){
          if(!document.body)return null;
          var owner=window.__easyBiliNav;
          if(!owner){
            var tag=document.querySelector('meta[name=viewport]'),newTag=!tag;
            if(newTag){tag=document.createElement('meta');tag.name='viewport';document.head.appendChild(tag);}
            var before=tag.getAttribute('content');
            tag.setAttribute('content',(before||'width=device-width,initial-scale=1').replace(/viewport-fit\s*=\s*[^,]+,?/g,'')+',viewport-fit=cover');
            owner={element:null,release:function(){if(this.element){this.element.style.setProperty('padding-bottom',this.value,this.priority);this.element=null;}},
              undo:function(){this.release();if(newTag)tag.remove();else if(before===null)tag.removeAttribute('content');else tag.setAttribute('content',before);delete window.__easyBiliNav;}};
            window.__easyBiliNav=owner;
          }
          var chosen=document.scrollingElement,best=0;
          document.querySelectorAll('body *').forEach(function(element){
            var rect=element.getBoundingClientRect();
            if(rect.width<innerWidth*.6||rect.height<innerHeight*.4||rect.bottom<=innerHeight*.5||rect.top>=innerHeight)return;
            if(!/auto|scroll/.test(getComputedStyle(element).overflowY))return;
            var area=rect.width*rect.height;if(area>best){chosen=element;best=area;}
          });
          if(!chosen)return null;
          if(owner.element!==chosen){owner.release();owner.element=chosen;owner.value=chosen.style.getPropertyValue('padding-bottom');owner.priority=chosen.style.getPropertyPriority('padding-bottom');owner.base=parseFloat(getComputedStyle(chosen).paddingBottom)||0;}
          var inset=Math.max(owner.base,$dp/(window.visualViewport?visualViewport.scale:1));
          if(chosen.style.getPropertyValue('padding-bottom')!==inset+'px')chosen.style.setProperty('padding-bottom',inset+'px','important');
          var background=getComputedStyle(document.body).backgroundColor;
          if(background==='rgba(0, 0, 0, 0)')background=getComputedStyle(document.documentElement).backgroundColor;
          return {background:background,height:innerHeight,safe:Math.round(inset)};
        })()
    """.trimIndent()
}
