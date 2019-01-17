package com.orsteg.residingtab

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.support.v4.view.ViewPager
import android.util.AttributeSet
import android.view.View
import android.view.WindowManager
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.schedule

/**
 * Copyright 2019 Orsteg Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

class RevealViewPager : ViewPager {

    private val activity: Activity? = run {
        if (context is Activity) context as Activity? else null
    }
    private val window = activity?.window

    // Touch event variables
    private val mTouchable: ArrayList<View> = ArrayList()
    private var mTouchOverrideView: View? = null
    //private var mTouchOverrideTime: Long = -1

    // Reveal transformation variables
    private var mRevealPosition: Int = -1
    private var appBar: View? = null
    private var resideForeground: View? = null
    private val mForegrounds: ArrayList<View> = ArrayList()
    private var actionBtn: View? = null
    private var mResideTab: View? = null
    private var visibilityChangeListener: OnResideTabVisibilityChangeListener? = null

    // State variables holding current states of full screen and translucence
    private var mState: Int = ViewPager.SCROLL_STATE_IDLE
    private var isFullScreen: Boolean? = null
    private var isTranslucentNav: Boolean? = null
    private var mCurrent = -1
    private var shouldTransform = false
    private var isStateSaved: Boolean? = null

    // Init the reveal transformer here
    private var mTransformer =  RevealTransformer()

    init {
        // bind the reveal transformer to the ViewPager
        addOnPageChangeListener(mTransformer)

        // Intercept touch events and deliver to other view layers beneath it when in reveal state
        setOnTouchListener { _, event ->

            //val time = event.downTime
            //check if this is a new touch session
            //if (time <= mTouchOverrideTime) clearTouchOverride()
            //mTouchOverrideTime = time

            // deliver touch events only when in IDLE state and on reveal position
            if (currentItem == mRevealPosition && mState == ViewPager.SCROLL_STATE_IDLE) {

                // Offset the vertical position of the touch event
                // due to a displacement of the ViewPager caused by the AppBar collapsing.
                event.offsetLocation(0f, appBar?.top?.toFloat()?:0f)

                // check if any view has a hold on touch events
                if (mTouchOverrideView != null) {
                    mTouchOverrideView?.dispatchTouchEvent(event)
                } else {
                    // Keep sending the touch event layer by layer until a view consumes it
                    for (touchable in mTouchable) {
                        if (touchable.dispatchTouchEvent(event)) break
                    }
                }
                //mTouchOverrideView != null
            }
            false
        }
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    /**
     * This method sets the residing tab view and its reveal position.
     * Its sets the reside view's touch response index to 1
     *
     * @param view The residing view
     * @param revealPosition the index of the residing view's reveal tab
     */
    fun setResidingView(view: View, revealPosition: Int) {
        touchables.remove(mResideTab)
        addTouchableViewLayer(view, 1)
        mResideTab = view
        setRevealPosition(revealPosition)
    }

    /**
     * Sets the position of the revealing item fragment.
     */
    fun setRevealPosition(position: Int) {
        mRevealPosition = position
    }

    /**
     * A view that has been declared as a touchable layer can call this method to retain all touch
     * events during a touch action, preventing the ViewPager from intercepting it.
     * When the view is done it should call [clearTouchOverride] to return control to the ViewPager

    fun overrideTouch(view: View) {
        if (mTouchable.contains(view)) mTouchOverrideView = view
    }

    fun clearTouchOverride() {
        mTouchOverrideView = null
    }
    */

    /**
     *  Used to set views that can receive touch events behind the view pager
     *  @param view The view to receive the touch event
     *  @param layerIndex Touch events are delivered in ascending order from index 0 till a view
     *  completely consumes the event
     */
    fun addTouchableViewLayer(view: View, layerIndex: Int = mTouchable.size) {
        var index = resolveIndex(layerIndex)
        if (!mTouchable.contains(view)) {
            mTouchable.add(index, view)
        } else if(mTouchable.indexOf(view) != index){
            mTouchable.remove(view)
            index = resolveIndex(index)
            mTouchable.add(index, view)
        }
    }

    fun removeTouchableViewLayer(view: View): Boolean {
        return mTouchable.remove(view)
    }

    fun removeTouchableViewLayerIndex(index: Int) {
        if (index >= 0 && index < mTouchable.size) mTouchable.removeAt(index)
    }

    /**
     * Attaches a lister for the reside view state changes
     */
    fun setOnResideTabVisibilityChangeListener(listener: OnResideTabVisibilityChangeListener?) {
        visibilityChangeListener = listener
    }

    private fun resolveIndex(index: Int) :Int {
        if (mTouchable.size == 0 || index < 0) return 0
        if (index > mTouchable.size) return mTouchable.size
        return index
    }

    /**
     * This method binds Views that will be transformed during page transitions. Call this method to set
     * all the transformation views at once, else call the individual binding methods for each view.
     *
     * @param appBar this should be the AppBarLayout in the layout.
     * This is the view that would be translated upwards during the residing tab reveal
     *
     * @param resideTabForeground This view is always placed above the residing tab,
     * but is drawn behind the ViewPager. It slides into position as the reveal begins
     *
     * @param actBtn This is the material design FloatingActionButton or any view serving the same purpose.
     */
    fun bindTransformedViews(appBar: View?, resideTabForeground: View?, actBtn: View?) {
        if (appBar != null) bindAppBar(appBar)
        if (resideTabForeground != null) bindForeground(resideTabForeground)
        if (actBtn != null) bindActionButton(actBtn)
    }

    // Individual methods for binding views that would be transformed during state changes
    fun bindAppBar(appBar: View?) {
        this.appBar = appBar
    }

    fun bindForeground(fore: View?) {
        touchables.remove(this.resideForeground)
        this.resideForeground = fore
        if (fore != null) addTouchableViewLayer(fore, 0)
    }

    fun bindActionButton(actBtn: View?) {
        this.actionBtn = actBtn
    }

    /**
     * A foreground is taken as any view that slides into position together with the reveal tab
     *
     * @param touchIndex determines the order they receive touch events.
     */
    fun addForeground(foreground: View, touchIndex: Int? = null) {
        if (!mForegrounds.contains(foreground)) mForegrounds.add(foreground)
        if(touchIndex != null)addTouchableViewLayer(foreground, touchIndex)
    }

    /**This method should not be called before any call to [ViewPager.setCurrentItem] is made during
     * layout initialization.
     * A call to [ViewPager.setCurrentItem] after calling this method during app Initialization,
     * introduces a subtle transformation error.
     *
     * @param inState . Pass the savedInstanceState bundle from [Activity.onCreate] to this method
     * @param firstInit . Set to false if the index of the residing tab, [mRevealPosition], = 0
     * and you do not want to initialise layout at that position. It disables initial transformations.
     * Default value is set to false. Set true if initial transformation is required
     */
    fun initTransformer(inState: Bundle?, firstInit: Boolean = false) {
        var current = inState?.getInt("RevealTransformer.mCurrent", -2)?:0

        isStateSaved = current != -2

        if (mCurrent != -1 && inState == null) current = mCurrent

        if ((inState == null && current == mRevealPosition && firstInit) ||
                (inState != null && current == mRevealPosition)) {
            mCurrent = current
           mTransformer.reside()
        }

    }


    /**
     * Method should be called in the [Activity.onSaveInstanceState] method to save the current state
     * of the UI
     */
    fun saveState(outState: Bundle?){
        outState?.apply {
            putInt("RevealTransformer.mCurrent", mCurrent)
        }
    }

    /**
     * Call this method in the Activity class either in [Activity.onResume] or [Activity.onWindowFocusChanged]
     * in order to make sure windowUIVisibility is always in the correct state.
     */
    fun updateUIVisibility() {
        mTransformer.setUIVisibility()
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        super.onRestoreInstanceState(state)

        // check if user saved view state in activity or not then initialise transformer
        if (isStateSaved == null || isStateSaved == false) initTransformer(null, true)

    }

    /**
     * The page transform class that handles transformation of the appbar, resideForeground,
     * action button and state of translucence and full screen.
     **/
    inner class RevealTransformer : ViewPager.OnPageChangeListener {

        init {
            // Set a ui visibility listener to reset the status bar when in full screen
            // mode, after a 2s delay.
            window?.decorView?.setOnSystemUiVisibilityChangeListener {
                Timer().schedule(2000){
                    activity?.runOnUiThread {
                        setUIVisibility()
                    }
                }
            }
        }

        // Initialises first transformation for the reside view
        fun reside() {
            setFullScreen()
            setTranslucentNav()

            Thread {
                // get the width of the screen
                val w = width

                // get an instance of the bottom value of the appBar
                var h = (appBar?.bottom ?: 0)

                // Loop till view is drawn or transformation is not necessary
                while ((h == 0 && appBar != null && appBar?.visibility == VISIBLE) ||
                        (w == 0 && visibility == VISIBLE)) h = (appBar?.bottom ?: 0)

                activity?.runOnUiThread {

                    // Translate the appBar up as the resideReveal slides into view
                    appBar?.translationY = -h - h / 8f

                    // Translate the resideForeground together with the resideReveal
                    resideForeground?.translationX = 0f

                    for (foreground in mForegrounds) {
                        foreground.translationX = 0f
                    }
                    // Translate the actionButton away as the resideReveal slides into view
                    actionBtn?.translationX = w.toFloat()
                }

            }.start()
            mCurrent = currentItem
        }


        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            // going right offset changes from zero to one

            // get an instance of the width of the screen
            val w = width

            // get an instance of the bottom value of the appBar
            val h = (appBar?.bottom ?: 0)

            if (position == mRevealPosition && shouldTransform) {

                // Translate the appBar up as the resideReveal slides into view
                var y = (positionOffset * h) - h
                if (y == -h.toFloat()) y = -h - h / 8f
                appBar?.translationY = y

                // Translate the resideForeground together with the resideReveal
                resideForeground?.translationX = -w * positionOffset

                for (foreground in mForegrounds) {
                    foreground.translationX = -w * positionOffset
                }
                // Translate the actionButton away as the resideReveal slides into view
                actionBtn?.translationX = w * (1 - positionOffset)

                // Set translucence based on the position of the resideReveal
                if (positionOffset < 1) setTranslucentNav()
                else exitTranslucentNav()

                // Set full screen based on the position of resideReveal
                if (positionOffset > 0) exitFullScreen()
                else setFullScreen()
            } else if (position == mRevealPosition - 1 && shouldTransform) {
                // going right offset changes from zero to one
                // Translate the appBar up as the resideReveal slides into view
                appBar?.translationY = -(positionOffset * h)

                // Translate the resideForeground together with the resideReveal
                resideForeground?.translationX = w * (1 - positionOffset)

                for (foreground in mForegrounds) {
                    foreground.translationX = w * (1 - positionOffset)
                }

                // Translate the actionButton away as the resideReveal slides into view
                actionBtn?.translationX = -w * positionOffset

                // Set translucence based on the position of the resideReveal
                if (positionOffset > 0) setTranslucentNav()
                else exitTranslucentNav()

                // Set full screen based on the position of resideReveal
                if (positionOffset < 1) exitFullScreen()
                else setFullScreen()
            } else {
                if (appBar?.translationY != 0f || resideForeground?.translationX == 0f) {
                    if (appBar?.translationY != 0f)exitTranslucentNav()
                    // Reset translation of the appBar
                    appBar?.translationY = 0f

                    // Reset translation of the actionButton
                    actionBtn?.translationX = 0f

                    // hide the resideForeground to the left or the right of the screen
                    if (position > mRevealPosition) {
                        resideForeground?.translationX = (-w).toFloat()

                        for (foreground in mForegrounds) {
                            foreground.translationX = (-w).toFloat()
                        }
                    } else {
                        resideForeground?.translationX = (w).toFloat()

                        for (foreground in mForegrounds) {
                            foreground.translationX = (w).toFloat()
                        }
                    }
                }
            }
        }

        override fun onPageSelected(position: Int) {
            // check if page scroll state is settling from/to the resideReveal position
            shouldTransform = (mCurrent == mRevealPosition || position == mRevealPosition)

            when (position) {
                mRevealPosition -> {
                    actionBtn?.visibility = View.INVISIBLE
                }
                else -> {
                    actionBtn?.visibility = View.VISIBLE
                }
            }
            mCurrent = position
        }

        override fun onPageScrollStateChanged(state: Int) {

            if (state == ViewPager.SCROLL_STATE_DRAGGING || state == ViewPager.SCROLL_STATE_IDLE) {
                shouldTransform = true
            }

            mState = state
        }

        // Methods for setting the UI Visibility
        private fun exitTranslucentNav() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                if (isTranslucentNav == null || isTranslucentNav == true) {
                    window?.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
                    isTranslucentNav = false
                    visibilityChangeListener?.onChanged(false)
                }
            }
        }

        private fun setTranslucentNav() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                if (isTranslucentNav == null || isTranslucentNav == false) {
                    window?.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
                            WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
                    isTranslucentNav = true
                    visibilityChangeListener?.onChanged(true)
                }
            }
        }

        private fun setFullScreen() {
            if (isFullScreen == null || isFullScreen == false && !isFullScreen()) {
                window?.decorView?.systemUiVisibility = FULLSCREEN
                isFullScreen = true
            }
        }

        private fun exitFullScreen() {
            if (isFullScreen == null || isFullScreen == true) {
                window?.decorView?.systemUiVisibility = NOT_FULLSCREEN
                isFullScreen = false
            }
        }

        private fun isFullScreen() = window?.decorView?.systemUiVisibility == FULLSCREEN

        fun setUIVisibility() {
            if (isFullScreen == true && !isFullScreen()) {
                window?.decorView?.systemUiVisibility = FULLSCREEN
            }
        }
    }

    companion object {
        val FULLSCREEN = View.SYSTEM_UI_FLAG_LAYOUT_STABLE +
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN + View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION +
                View.SYSTEM_UI_FLAG_FULLSCREEN

        val NOT_FULLSCREEN = View.SYSTEM_UI_FLAG_LAYOUT_STABLE +
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN + View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
    }

    /**
     * Interface for listening to when the residing view becomes visible and when it looses visibility
     */
    interface OnResideTabVisibilityChangeListener {
        fun onChanged(isVisible: Boolean)
    }

}