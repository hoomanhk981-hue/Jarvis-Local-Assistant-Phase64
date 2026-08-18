package com.example.assistant

import android.app.Activity
import android.os.Bundle
import android.webkit.WebView
import android.widget.FrameLayout

class BrowserActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = WebView(this)
        view.layoutParams = FrameLayout.LayoutParams(-1, -1)
        setContentView(view)
        BrowserController.attach(this, view)
        intent.getStringExtra("url")?.let(BrowserController::open)
    }
}
