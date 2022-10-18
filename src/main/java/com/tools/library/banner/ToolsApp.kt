package com.tools.library.banner

import android.app.Application
import android.content.Context
import com.tools.library.banner.ToolsApp

object ToolsApp {
    private var app: Application? = null
    val context: Context
        get() = app!!.applicationContext

    fun init(app: Application?) {
        ToolsApp.app = app
    }
}