package com.tools.library.banner.viewpager

import androidx.viewpager2.widget.ViewPager2
import com.tools.library.banner.viewpager.BannerViewPager
import com.tools.library.banner.viewpager.IBannerPagerIndicator

object BannerIndicatorHelper {

    fun bindViewPager(pagerIndicator: IBannerPagerIndicator, viewPager: BannerViewPager) {
        viewPager.removePageChangeCallbacks()
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                pagerIndicator.onPageScrolled(position, positionOffset, positionOffsetPixels)
            }

            override fun onPageSelected(position: Int) {
                pagerIndicator.onPageSelected(position)
            }

            override fun onPageScrollStateChanged(state: Int) {
                pagerIndicator.onPageScrollStateChanged(state)
            }
        })
    }
}