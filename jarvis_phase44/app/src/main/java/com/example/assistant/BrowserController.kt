package com.example.assistant

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Owns the in-app browser WebView. All DOM actions happen locally in the page. */
object BrowserController {
    private var webView: WebView? = null
    private var context: Context? = null

    fun attach(context: Context, view: WebView) {
        this.context = context.applicationContext
        webView = view
        view.settings.javaScriptEnabled = true
        view.settings.domStorageEnabled = true
        view.settings.javaScriptCanOpenWindowsAutomatically = false
        view.webViewClient = WebViewClient()
    }

    fun open(url: String) {
        val safe = normalizeUrl(url) ?: return
        webView?.post { webView?.loadUrl(safe) }
    }

    suspend fun evaluate(script: String): String = suspendCancellableCoroutine { cont ->
        val view = webView
        if (view == null) { cont.resume(""); return@suspendCancellableCoroutine }
        view.post {
            view.evaluateJavascript(script) { value ->
                if (cont.isActive) cont.resume(value?.removeSurrounding("\"", "\"")?.replace("\\n", "\n")?.replace("\\\"", "\"") ?: "")
            }
        }
    }

    suspend fun inspect(): String = evaluate("""
        (() => {
          const clean = s => (s || '').replace(/\\s+/g,' ').trim();
          const els = [...document.querySelectorAll('a,button,input,textarea,select,[role=button]')];
          return JSON.stringify({title: document.title, url: location.href, elements: els.slice(0,120).map((e,i)=>({
            i, tag:e.tagName.toLowerCase(), type:e.getAttribute('type')||'', text:clean(e.innerText||e.value||e.getAttribute('aria-label')||e.getAttribute('placeholder')),
            name:e.getAttribute('name')||'', placeholder:e.getAttribute('placeholder')||'', disabled:!!e.disabled
          }))});
        })()
    """.trimIndent())

    suspend fun click(target: String): String = evaluate("""
        (() => {
          const q=${jsString(target)}; const clean=s=>(s||'').replace(/\\s+/g,' ').trim().toLowerCase();
          const els=[...document.querySelectorAll('a,button,[role=button],input[type=submit],input[type=button]')];
          const e=els.find(x=>clean(x.innerText||x.value||x.getAttribute('aria-label'))===clean(q)) || els.find(x=>clean(x.innerText||x.value||x.getAttribute('aria-label')).includes(clean(q)));
          if(!e) return 'NOT_FOUND'; e.click(); return 'CLICKED';
        })()
    """.trimIndent())

    suspend fun type(target: String, text: String): String = evaluate("""
        (() => {
          const q=${jsString(target)}, v=${jsString(text)}; const clean=s=>(s||'').toLowerCase();
          const els=[...document.querySelectorAll('input,textarea,[contenteditable=true]')];
          const e=els.find(x=>clean(x.name)===clean(q)||clean(x.id)===clean(q)||clean(x.placeholder)===clean(q)||clean(x.getAttribute('aria-label'))===clean(q)) || els.find(x=>clean(x.placeholder).includes(clean(q)));
          if(!e) return 'NOT_FOUND'; e.focus(); if('value' in e) { const setter=Object.getOwnPropertyDescriptor(e.constructor.prototype,'value')?.set; setter?.call(e,v); } else e.textContent=v; e.dispatchEvent(new Event('input',{bubbles:true})); e.dispatchEvent(new Event('change',{bubbles:true})); return 'TYPED';
        })()
    """.trimIndent())

    fun back(): Boolean = webView?.goBack().let { true }

    private fun normalizeUrl(raw: String): String? {
        val t = raw.trim()
        val u = if (t.startsWith("http://") || t.startsWith("https://")) t else "https://$t"
        return try { Uri.parse(u).takeIf { it.scheme == "http" || it.scheme == "https" }?.toString() } catch (_: Exception) { null }
    }

    private fun jsString(s: String): String = "'" + s.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n") + "'"
}
