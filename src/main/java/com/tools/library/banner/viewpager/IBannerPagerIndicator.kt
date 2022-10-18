package com.tools.library.banner.viewpager

interface IBannerPagerIndicator {

    fun setCount(count: Int)

    fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int)

    fun onPageSelected(position: Int)

    fun onPageScrollStateChanged(state: Int)
}