package com.tools.library.banner.viewpager

import androidx.collection.ArraySet
import androidx.collection.arrayMapOf
import androidx.collection.arraySetOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

/**
 *
 * Created by dazhao.dai 2022/1/28
 */
object BannerPlayController: LifecycleEventObserver {

    private val viewPagersMap = arrayMapOf<LifecycleOwner, ArraySet<BannerViewPager>>()

    fun add(lifecycleOwner: LifecycleOwner, bannerViewPager: BannerViewPager) {
        lifecycleOwner.lifecycle.addObserver(this)
         var viewPagers = viewPagersMap[lifecycleOwner]
        if (viewPagers.isNullOrEmpty()) {
            viewPagers = arraySetOf<BannerViewPager>().apply {
                add(bannerViewPager)
            }
            viewPagersMap[lifecycleOwner] = viewPagers
        } else {
            viewPagers.add(bannerViewPager)
        }
    }

    fun remove(lifecycleOwner: LifecycleOwner) {
        viewPagersMap.remove(lifecycleOwner)?.forEach {
            it.pausePlay()
        }
    }

    fun release() {
        viewPagersMap.values.forEach { set ->
            set.forEach {
                it.pausePlay()
            }
        }
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when(event) {
            Lifecycle.Event.ON_PAUSE -> pauseBanner(source)
            Lifecycle.Event.ON_RESUME -> {
                resumeBanner(source)
            }
            Lifecycle.Event.ON_DESTROY -> {
                pauseBanner(source)
                viewPagersMap.remove(source)
            }
            else -> {}
        }
    }

    private fun resumeBanner(source: LifecycleOwner) {
        viewPagersMap[source]?.onEach {
            it.resumePlay()
        }
    }

    private fun pauseBanner(source: LifecycleOwner) {
        viewPagersMap[source]?.apply {
            forEach {
                it.pausePlay()
            }
        }
    }
}