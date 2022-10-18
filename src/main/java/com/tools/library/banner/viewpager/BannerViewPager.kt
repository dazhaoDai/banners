package com.tools.library.banner.viewpager

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import androidx.annotation.Px
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.get
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL
import androidx.viewpager2.widget.ViewPager2.OffscreenPageLimit
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import androidx.viewpager2.widget.ViewPager2.Orientation
import androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_DRAGGING
import com.tools.library.banner.R
import kotlin.math.abs

class BannerViewPager @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var pausePlay: Boolean = false
    private var loopPlay: Boolean = true
    private var autoPlay: Boolean = false

    @Orientation
    private var newOrientation: Int = ORIENTATION_HORIZONTAL
    private var playDuration: Long = 0L
    private var scrollSpeed: Float = MILLISECONDS_PER_INCH //default is 25f
    private var tempPosition = 0
    private var touchX: Float = 0f
    private var touchY: Float = 0f
    private var scaleTouchSlop: Int
    private var lastSelectedPosition = -1

    private val pageChangeCallbacks by lazy {
        CompositeOnPageChangeCallback(3)
    }
    private val autoPlayTask: Runnable by lazy {
        return@lazy Runnable {
            removeCallbacks(autoPlayTask)
            if (pausePlay) return@Runnable
            tempPosition++
            if (tempPosition >= lastItem()) {
                setCurrentItem(firstItem(), false)
                post(autoPlayTask)
            } else  {
                setCurrentItem(tempPosition, true)
            }
        }
    }

    private fun lastItem() = itemCount()
    private fun firstItem() = if (loopPlay) 1 else 0
    private var viewPager: ViewPager2

    init {
        initAttribute(attrs)
        scaleTouchSlop = ViewConfiguration.get(context).scaledTouchSlop
        viewPager = ViewPager2(context, attrs, defStyleAttr).apply {
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageScrolled(
                    position: Int,
                    positionOffset: Float,
                    positionOffsetPixels: Int
                ) {
                    super.onPageScrolled(position, positionOffset, positionOffsetPixels)
                    val realPosition = getRealPosition(position)
                    pageChangeCallbacks.onPageScrolled(realPosition, positionOffset, positionOffsetPixels)
                }

                override fun onPageScrollStateChanged(state: Int) {
                    super.onPageScrollStateChanged(state)
                    checkNextPosition(state == SCROLL_STATE_DRAGGING)
                    pageChangeCallbacks.onPageScrollStateChanged(state)
                }

                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    tempPosition = position
                    val realPosition = getRealPosition(position)
                    if (realPosition != lastSelectedPosition) {
                        lastSelectedPosition = realPosition
                        pageChangeCallbacks.onPageSelected(realPosition)
                    }
                }
            })
            orientation = newOrientation
        }
        (viewPager[0] as? RecyclerView)?.apply {
            setItemViewCacheSize(CACHE_SIZE)
            setHasFixedSize(true)
        }
        val layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        addView(viewPager, layoutParams)
        initViewPagerScrollProxy()
    }

    private fun initAttribute(attrs: AttributeSet?) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.BannerViewPager)
        autoPlay = typedArray.getBoolean(R.styleable.BannerViewPager_autoPlay, true)
        loopPlay = typedArray.getBoolean(R.styleable.BannerViewPager_loopPlay, true)
        playDuration = typedArray.getInt(R.styleable.BannerViewPager_loopDuration, DEFAULT_INTERVAL_DURATION).toLong()
        newOrientation = typedArray.getInt(R.styleable.BannerViewPager_android_orientation, ORIENTATION_HORIZONTAL)
        scrollSpeed = typedArray.getFloat(R.styleable.BannerViewPager_scrollSpeed, MILLISECONDS_PER_INCH).rem(201f)
        typedArray.recycle()
    }

    private fun checkNextPosition(dragging: Boolean) {
        if (!dragging || !loopPlay) return
        if (tempPosition == 0) {
            viewPager.setCurrentItem(getRealItemCount(), false)
        } else if (tempPosition == getRealItemCount() + 1) {
            viewPager.setCurrentItem(firstItem(), false)
        }
    }

    fun setAutoPlay(autoPlay: Boolean) {
        this.autoPlay = autoPlay
    }

    fun setLoopPlay(loopPlay: Boolean) {
        this.loopPlay = loopPlay
    }

    fun setPlayDuration(playDuration: Long) {
        this.playDuration = playDuration
    }

    fun setAdapter(adapter: Adapter<out RecyclerView.ViewHolder>) {
        lastSelectedPosition = -1
        loopPlay = adapter.itemCount > 1
        viewPager.adapter = BannerAdapterWrapper(loopPlay, adapter)
        checkLoopLegal()
        setCurrentItem(firstItem(), false)
    }

    fun getAdapter(): Adapter<RecyclerView.ViewHolder>? {
      return viewPager.adapter
    }

    fun notifyDataSetChanged() {
        if (getAdapter() == null) return
        getAdapter()?.notifyDataSetChanged()
        checkLoopLegal()
        setCurrentItem(firstItem(), false)
    }

    fun setOrientation(@ViewPager2.Orientation orientation: Int) {
        viewPager.orientation = orientation
    }

    @ViewPager2.Orientation
    fun getOrientation(): Int {
        return viewPager.orientation
    }

    fun setCurrentItem(item: Int) {
        setCurrentItem(item, true)
    }

    fun setCurrentItem(item: Int, smoothScroll: Boolean) {
        viewPager.setCurrentItem(item, smoothScroll)
        handleAutoplay(autoPlay)
    }

    fun getCurrentItem(): Int {
        return viewPager.currentItem
    }

    @ViewPager2.ScrollState
    fun getScrollState(): Int {
        return viewPager.scrollState
    }

    fun beginFakeDrag(): Boolean {
        return viewPager.beginFakeDrag()
    }

    fun fakeDragBy(@SuppressLint("SupportAnnotationUsage") @Px offsetPxFloat: Float): Boolean {
        return viewPager.fakeDragBy(offsetPxFloat)
    }

    fun endFakeDrag(): Boolean {
        return viewPager.endFakeDrag()
    }

    fun isFakeDragging(): Boolean {
        return viewPager.isFakeDragging
    }

    fun setUserInputEnabled(enabled: Boolean) {
        viewPager.isUserInputEnabled = enabled
    }

    fun isUserInputEnabled(): Boolean {
        return viewPager.isUserInputEnabled
    }

    fun setOffscreenPageLimit(@OffscreenPageLimit limit: Int) {
        viewPager.offscreenPageLimit = limit
    }

    @OffscreenPageLimit
    fun getOffscreenPageLimit(): Int {
        return viewPager.offscreenPageLimit
    }

    override fun canScrollHorizontally(direction: Int): Boolean {
        return viewPager.canScrollHorizontally(direction)
    }

    override fun canScrollVertically(direction: Int): Boolean {
        return viewPager.canScrollVertically(direction)
    }

    fun removePageChangeCallbacks() {
        pageChangeCallbacks.removeAllCallbacks()
    }

    fun registerOnPageChangeCallback(callback: ViewPager2.OnPageChangeCallback) {
        pageChangeCallbacks.addOnPageChangeCallback(callback)
    }

    fun unregisterOnPageChangeCallback(callback: ViewPager2.OnPageChangeCallback) {
        pageChangeCallbacks.removeOnPageChangeCallback(callback)
    }

    fun setPageTransformer(transformer: ViewPager2.PageTransformer?) {
        viewPager.setPageTransformer(transformer)
    }


    fun requestTransform() {
        viewPager.requestTransform()
    }

    fun addItemDecoration(decor: RecyclerView.ItemDecoration) {
        viewPager.addItemDecoration(decor)
    }


    fun addItemDecoration(decor: RecyclerView.ItemDecoration, index: Int) {
        viewPager.addItemDecoration(decor, index)
    }

    fun getItemDecorationAt(index: Int): RecyclerView.ItemDecoration {
        return viewPager.getItemDecorationAt(index)
    }


    fun getItemDecorationCount(): Int {
        return viewPager.itemDecorationCount
    }


    fun invalidateItemDecorations() {
        viewPager.invalidateItemDecorations()
    }

    fun removeItemDecorationAt(index: Int) {
        viewPager.removeItemDecorationAt(index)
    }

    fun removeItemDecoration(decor: RecyclerView.ItemDecoration) {
        viewPager.removeItemDecoration(decor)
    }

    private fun handleAutoplay(autoPlay: Boolean) {
        if (singleItem()) return
        if (!autoPlay || playDuration <= 0L) {
            removeCallbacks(autoPlayTask)
            return
        }
        postDelayed(autoPlayTask, playDuration)
    }

    private fun checkLoopLegal() {
        if (singleItem()) {
            loopPlay = false
            autoPlay = false
            pausePlay = true
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        if (singleItem()) {
            return super.dispatchTouchEvent(event)
        }
        event?.let {
            when (it.action) {
                MotionEvent.ACTION_DOWN -> {
                    parent.requestDisallowInterceptTouchEvent(true)
                    handleAutoplay(false)
                    touchX = it.x
                    touchY = it.y
                }
                 MotionEvent.ACTION_MOVE -> {
                    if (isUserInputEnabled()) {
                        val distanceX: Float = abs(it.x - touchX)
                        val distanceY: Float = abs(it.y - touchY)
                        if (getOrientation() == ORIENTATION_HORIZONTAL) {
                            if (distanceX > distanceY) {
                                parent?.requestDisallowInterceptTouchEvent(true)
                                if (distanceX > scaleTouchSlop) {
                                    return super.dispatchTouchEvent(event)
                                }
                            } else {
                                parent?.requestDisallowInterceptTouchEvent(false)
                            }
                        } else {
                            if (distanceY > distanceX) {
                                parent?.requestDisallowInterceptTouchEvent(true)
                                if (distanceY > scaleTouchSlop) {
                                    return super.dispatchTouchEvent(event)
                                }
                            } else {
                                parent?.requestDisallowInterceptTouchEvent(false)
                            }
                        }
                    }
                }
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL,
                MotionEvent.ACTION_OUTSIDE -> {
                    parent?.requestDisallowInterceptTouchEvent(false)
                    handleAutoplay(autoPlay)
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }


    private fun singleItem() = getRealItemCount() <= 1

    private fun getRealPosition(position: Int): Int {
        val adapter = getAdapter() ?: return position
        return if (adapter is BannerAdapterWrapper) {
            adapter.getRealPosition(position)
        } else {
            position
        }
    }

    private fun itemCount(): Int {
        val adapter = getAdapter() ?: return 0
        return adapter.itemCount
    }

    private fun getRealItemCount(): Int {
        val adapter = getAdapter() ?: return 0
        return if (adapter is BannerAdapterWrapper) {
            adapter.getRealCount()
        } else {
            adapter.itemCount
        }
    }

    fun pausePlay() {
        pausePlay = true
        handleAutoplay(false)
    }

    fun resumePlay() {
        pausePlay = false
        handleAutoplay(autoPlay)
    }

    inner class CompositeOnPageChangeCallback(initialCapacity: Int) : OnPageChangeCallback() {

        private val mCallbacks: MutableList<OnPageChangeCallback> = ArrayList(initialCapacity)

        fun removeAllCallbacks() {
            mCallbacks.clear()
        }

        /**
         * Adds the given callback to the list of subscribers
         */
        fun addOnPageChangeCallback(callback: OnPageChangeCallback) {
            mCallbacks.add(callback)
        }

        /**
         * Removes the given callback from the list of subscribers
         */
        fun removeOnPageChangeCallback(callback: OnPageChangeCallback) {
            mCallbacks.remove(callback)
        }

        /**
         * @see OnPageChangeCallback.onPageScrolled
         */
        override fun onPageScrolled(position: Int, positionOffset: Float, @Px positionOffsetPixels: Int) {
            try {
                for (callback in mCallbacks) {
                    callback.onPageScrolled(position, positionOffset, positionOffsetPixels)
                }
            } catch (ex: ConcurrentModificationException) {
                throwCallbackListModifiedWhileInUse(ex)
            }
        }

        /**
         * @see OnPageChangeCallback.onPageSelected
         */
        override fun onPageSelected(position: Int) {
            try {
                for (callback in mCallbacks) {
                    callback.onPageSelected(position)
                }
            } catch (ex: ConcurrentModificationException) {
                throwCallbackListModifiedWhileInUse(ex)
            }
        }

        /**
         * @see OnPageChangeCallback.onPageScrollStateChanged
         */
        override fun onPageScrollStateChanged(@ViewPager2.ScrollState state: Int) {
            try {
                for (callback in mCallbacks) {
                    callback.onPageScrollStateChanged(state)
                }
            } catch (ex: ConcurrentModificationException) {
                throwCallbackListModifiedWhileInUse(ex)
            }
        }

        private fun throwCallbackListModifiedWhileInUse(parent: ConcurrentModificationException) {
            throw IllegalStateException(
                "Adding and removing callbacks during dispatch to callbacks is not supported",
                parent
            )
        }
    }

    private fun initViewPagerScrollProxy() {
        try {
            val recyclerView = viewPager.getChildAt(0) as RecyclerView
            val layoutManager = (recyclerView.layoutManager as LinearLayoutManager?) ?: return
            val proxyLayoutManger = PageScrollLayoutManger(context, layoutManager)
            recyclerView.layoutManager = proxyLayoutManger

            val mRecyclerView =
                RecyclerView.LayoutManager::class.java.getDeclaredField("mRecyclerView")
            mRecyclerView.isAccessible = true
            mRecyclerView[layoutManager] = recyclerView

            val layoutMangerField = ViewPager2::class.java.getDeclaredField("mLayoutManager")
            layoutMangerField.isAccessible = true
            layoutMangerField[viewPager] = proxyLayoutManger

            val pageTransformerAdapterField =
                ViewPager2::class.java.getDeclaredField("mPageTransformerAdapter")
            pageTransformerAdapterField.isAccessible = true
            val mPageTransformerAdapter = pageTransformerAdapterField[viewPager]
            if (mPageTransformerAdapter != null) {
                val aClass: Class<*> = mPageTransformerAdapter.javaClass
                val layoutManager = aClass.getDeclaredField("mLayoutManager")
                layoutManager.isAccessible = true
                layoutManager[mPageTransformerAdapter] = proxyLayoutManger
            }

            val scrollEventAdapterField =
                ViewPager2::class.java.getDeclaredField("mScrollEventAdapter")
            scrollEventAdapterField.isAccessible = true
            val mScrollEventAdapter = scrollEventAdapterField[viewPager]
            if (mScrollEventAdapter != null) {
                val aClass: Class<*> = mScrollEventAdapter.javaClass
                val layoutManager = aClass.getDeclaredField("mLayoutManager")
                layoutManager.isAccessible = true
                layoutManager[mScrollEventAdapter] = proxyLayoutManger
            }
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }
    }

    inner class PageScrollLayoutManger constructor(
        context: Context?,
        val layoutManager: LinearLayoutManager
    ) :
        LinearLayoutManager(context, layoutManager.orientation, false) {
        override fun performAccessibilityAction(
            recycler: RecyclerView.Recycler,
            state: RecyclerView.State, action: Int, args: Bundle?
        ): Boolean {
            return layoutManager.performAccessibilityAction(recycler, state, action, args)
        }

        override fun onInitializeAccessibilityNodeInfo(
            recycler: RecyclerView.Recycler,
            state: RecyclerView.State, info: AccessibilityNodeInfoCompat
        ) {
            layoutManager.onInitializeAccessibilityNodeInfo(recycler, state, info)
        }

        override fun requestChildRectangleOnScreen(
            parent: RecyclerView,
            child: View, rect: Rect, immediate: Boolean,
            focusedChildVisible: Boolean
        ): Boolean {
            return layoutManager.requestChildRectangleOnScreen(
                parent,
                child,
                rect,
                immediate,
                focusedChildVisible
            )
        }
        
        override fun smoothScrollToPosition(
            recyclerView: RecyclerView,
            state: RecyclerView.State,
            position: Int
        ) {
            val linearSmoothScroller: LinearSmoothScroller =
                object : LinearSmoothScroller(recyclerView.context) {
                    override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
                        return scrollSpeed / displayMetrics.densityDpi
                    }
                }

            linearSmoothScroller.targetPosition = position
            startSmoothScroll(linearSmoothScroller)
        }
    }

    inner class BannerAdapterWrapper<VH :RecyclerView.ViewHolder>(
        private val loopPlay: Boolean,
        private val realAdapter: Adapter<VH>
    ) : RecyclerView.Adapter<VH>() {

        private val FAKE_COUNT = 2

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            return realAdapter.onCreateViewHolder(parent, viewType)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val realPosition = getRealPosition(position)
            realAdapter.onBindViewHolder(holder, realPosition)
        }

        override fun onBindViewHolder(
            holder: VH,
            position: Int,
            payloads: MutableList<Any>
        ) {
            val realPosition = getRealPosition(position)
            realAdapter.onBindViewHolder(holder, realPosition, payloads)
        }

        override fun getItemCount(): Int {
            return if (loopPlay) {
                realAdapter.itemCount + FAKE_COUNT
            } else {
                realAdapter.itemCount
            }
        }

        override fun registerAdapterDataObserver(observer: RecyclerView.AdapterDataObserver) {
            realAdapter.registerAdapterDataObserver(observer)
        }

        override fun unregisterAdapterDataObserver(observer: RecyclerView.AdapterDataObserver) {
            realAdapter.unregisterAdapterDataObserver(observer)
        }

        override fun findRelativeAdapterPositionIn(
            adapter: Adapter<out RecyclerView.ViewHolder>,
            viewHolder: RecyclerView.ViewHolder,
            localPosition: Int
        ): Int {
            return if (loopPlay) {
                localPosition
            } else {
                realAdapter.findRelativeAdapterPositionIn(adapter, viewHolder, localPosition)
            }
        }

        override fun getItemViewType(position: Int): Int {
            val realPosition = getRealPosition(position)
            return realAdapter.getItemViewType(realPosition)
        }

        override fun setHasStableIds(hasStableIds: Boolean) {
            realAdapter.setHasStableIds(hasStableIds)
        }

        override fun getItemId(position: Int): Long {
            val realPosition = getRealPosition(position)
            return realAdapter.getItemId(realPosition)
        }

        override fun onViewRecycled(holder: VH) {
            realAdapter.onViewRecycled(holder)
        }

        override fun onFailedToRecycleView(holder: VH): Boolean {
            return realAdapter.onFailedToRecycleView(holder)
        }

        override fun onViewAttachedToWindow(holder: VH) {
            realAdapter.onViewAttachedToWindow(holder)
        }

        override fun onViewDetachedFromWindow(holder: VH) {
            realAdapter.onViewDetachedFromWindow(holder)
        }

        override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
            realAdapter.onAttachedToRecyclerView(recyclerView)
        }

        override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
            realAdapter.onDetachedFromRecyclerView(recyclerView)
        }

        override fun setStateRestorationPolicy(strategy: StateRestorationPolicy) {
            realAdapter.stateRestorationPolicy = strategy
        }

        fun getRealPosition(position: Int): Int {
            val dataCount = getRealCount()
            if (dataCount == 0) return 0
            return if (loopPlay) {
                var realPosition = (position - FAKE_COUNT / 2) % dataCount
                if (realPosition < 0) {
                    realPosition += dataCount
                }
                realPosition
            } else {
                position
            }
        }

        fun getRealCount() = realAdapter.itemCount
    }

    companion object {
        private const val MILLISECONDS_PER_INCH: Float = 80f
        private const val DEFAULT_INTERVAL_DURATION = 2000
        private const val CACHE_SIZE = 8
    }
}