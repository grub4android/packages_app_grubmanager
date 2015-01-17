package org.grub4android.grubmanager.activities;

import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import org.grub4android.grubmanager.R;
import org.grub4android.grubmanager.RootUtils;
import org.grub4android.grubmanager.fragments.BaseFragment;
import org.grub4android.grubmanager.fragments.NavigationDrawerFragment;
import org.grub4android.grubmanager.fragments.navigation.BootloaderSetupFragment;
import org.grub4android.grubmanager.fragments.navigation.OperatingSystemsFragment;
import org.grub4android.grubmanager.fragments.navigation.RecoveryToolsFragment;
import org.grub4android.grubmanager.fragments.navigation.SettingsFragment;


public class MainActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    private Toolbar mToolbar;
    private BaseFragment mNextFragment = null;
    private BaseFragment mActiveFragment = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // init busybox
        RootUtils.initBusybox(this);

        // set toolbar
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        // get navigation drawer fragment
        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);

        // fix padding for transparent statusbar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            findViewById(R.id.navigation_drawer).setPadding(0, getStatusBarHeight(), 0, 0);
        }

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.scrimInsetsFrameLayout,
                (DrawerLayout) findViewById(R.id.drawer_layout), mToolbar);
    }

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
        getSupportActionBar().setTitle(title);
    }

    @Override
    public void setTitle(int titleId) {
        super.setTitle(titleId);
        getSupportActionBar().setTitle(titleId);
    }

    private int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public Toolbar getToolbar() {
        return mToolbar;
    }

    public Fragment getActiveFragment() {
        return mActiveFragment;
    }

    public void startFragmentTransition() {
        if (mNextFragment != null) {
            // move next to active fragment
            mActiveFragment = mNextFragment;
            mNextFragment = null;

            // init
            mActiveFragment.init(this);
        }
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // create fragment
        BaseFragment f;
        if (position == 0)
            f = new OperatingSystemsFragment();
        else if (position == 1)
            f = new RecoveryToolsFragment();
        else if (position == 2)
            f = new BootloaderSetupFragment();
        else if (position == 3)
            f = new SettingsFragment();
        else return;

        // store next fragment
        mNextFragment = f;

        if (mActiveFragment == null) {
            startFragmentTransition();
        }

        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, f)
                .commit();
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        if (mActiveFragment != null)
            actionBar.setTitle(mActiveFragment.getTitle());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.menu_main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
