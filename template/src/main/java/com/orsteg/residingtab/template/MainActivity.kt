package com.orsteg.residingtab.template

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v7.app.AppCompatActivity
import android.view.*
import android.widget.Toast
import com.orsteg.residingtab.RevealViewPager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.fragment_main.view.*

class MainActivity : AppCompatActivity() {

    /**
     * The [android.support.v4.view.PagerAdapter] that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * [android.support.v4.app.FragmentStatePagerAdapter].
     */
    private var mRevealAdapter: RevealAdapter? = null
    private val RESIDE_TAB_INDEX: Int = 0
    private val START_INDEX: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set the initial UI Visibility
        window.decorView.systemUiVisibility = RevealViewPager.NOT_FULLSCREEN

        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)

        // Create the adapter that will return a fragment for each tab
        mRevealAdapter = RevealAdapter(supportFragmentManager)

        container.apply {
            // Set up the ViewPager with the sections adapter.
            adapter = mRevealAdapter

            //set offscreen limit to the number of tabs
            offscreenPageLimit = 4

            // Setup view pager with tabs
            addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tabs))

            // Set the initial tab position
            currentItem = START_INDEX

            // Set residing view parameters
            setOnResideTabVisibilityChangeListener(object : RevealViewPager.OnResideTabVisibilityChangeListener {
                override fun onChanged(isVisible: Boolean) {
                    // Todo: Do something on reside view or foreground when visibility changes
                    if (isVisible) {
                        Toast.makeText(this@MainActivity, "visible", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@MainActivity, "invisible", Toast.LENGTH_SHORT).show()
                    }
                }
            })

            // Pass an instance of the residing view and its reveal position
            setResidingView(reside_content, RESIDE_TAB_INDEX)

            // Bind views that are going to be transformed during page transition
            bindTransformedViews(appbar, reside_view_foreground, fab)

            // Initialise the transformer
            initTransformer(savedInstanceState, false)
        }

        // Setup tabs with view pager
        tabs.addOnTabSelectedListener(TabLayout.ViewPagerOnTabSelectedListener(container))

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }


        // Test for click events on the residing view and foreground
        reside_content.setOnClickListener {
            Toast.makeText(this@MainActivity, "Residing view clicked", Toast.LENGTH_SHORT).show()
        }
        foreground_widget.setOnClickListener {
            Toast.makeText(this@MainActivity, "Foreground clicked", Toast.LENGTH_SHORT).show()
        }

    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)

        // save transformation state of the ViewPager
        container.saveState(outState)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        // Update UI Visibility when windows focus changes
        if (hasFocus) {
            container.updateUIVisibility()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId

        if (id == R.id.action_settings) {
            return true
        }

        return super.onOptionsItemSelected(item)
    }


    /**
     * A [FragmentPagerAdapter] that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    inner class RevealAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

        override fun getItem(position: Int): Fragment {
            // getItem is called to instantiate the fragment for the given page.
            // Return a ResideRevealFragment if position = 0 else
            // Return a PlaceHolderFragment (defined as a static inner class below).

            if (position == RESIDE_TAB_INDEX) return ResideRevealFragment()
            return PlaceholderFragment.newInstance(position + 1)
        }

        override fun getCount(): Int {
            // Show 4 total pages.
            return 4
        }
    }


    /**
     * The fragment that reveals the residing view.
     */
    class ResideRevealFragment : Fragment() {

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                                  savedInstanceState: Bundle?): View? {
            return null
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    class PlaceholderFragment : Fragment() {

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                                  savedInstanceState: Bundle?): View? {
            val rootView = inflater.inflate(R.layout.fragment_main, container, false)

            rootView.section_label.text = getString(R.string.section_format, arguments?.getInt(ARG_SECTION_NUMBER))

            return rootView
        }

        companion object {
            /**
             * The fragment argument representing the section number for this
             * fragment.
             */
            private val ARG_SECTION_NUMBER = "section_number"

            /**
             * Returns a new instance of this fragment for the given section
             * number.
             */
            fun newInstance(sectionNumber: Int): PlaceholderFragment {
                val fragment = PlaceholderFragment()
                val args = Bundle()
                args.putInt(ARG_SECTION_NUMBER, sectionNumber)
                fragment.arguments = args
                return fragment
            }
        }
    }
}