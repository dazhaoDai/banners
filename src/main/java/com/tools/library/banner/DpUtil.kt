package com.tools.library.banner


/**
 *
 * Created by dazhao.dai 2022/7/1
 */

inline val Int.dp: Int get() = (this * ToolsApp.context.resources.displayMetrics.density  + 0.5f).toInt()
