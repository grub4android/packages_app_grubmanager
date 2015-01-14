package org.grub4android.grubmanager.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.melnykov.fab.FloatingActionButton;

import org.grub4android.grubmanager.R;
import org.grub4android.grubmanager.RootUtils;
import org.grub4android.grubmanager.Utils;
import org.grub4android.grubmanager.adapter.BootentryAdapter;
import org.grub4android.grubmanager.adapter.NavigationDrawerAdapter;
import org.grub4android.grubmanager.models.Bootentry;
import org.grub4android.grubmanager.models.JSONDeviceInfo;
import org.grub4android.grubmanager.updater.UpdaterClient;
import org.grub4android.grubmanager.widget.ScrimInsetsFrameLayout;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends ActionBarActivity {
    private DrawerLayout mDrawerLayout;

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private View mNotificationBar;
    private Button mNotificationButton;
    private ScrimInsetsFrameLayout mNavigationDrawer;
    private RecyclerView mNavRecyclerView;

    private JSONDeviceInfo mDeviceInfo = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // init busybox
        RootUtils.initBusybox(this);

        // toolbar
        setupToolbar();

        // list
        mRecyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        UpdaterClient.getDeviceInfo(this, new UpdaterClient.DeviceInfoReceivedCallback() {
            @Override
            public void onDeviceInfoReceived(JSONDeviceInfo deviceInfo, Exception eUC) {
                mDeviceInfo = deviceInfo;

                if (deviceInfo == null) {
                    Toast.makeText(MainActivity.this, getString(R.string.error_occurred), Toast.LENGTH_LONG).show();
                    return;
                }
            }
        });

        // data
        ArrayList<Bootentry> bootentries = new ArrayList<>();
        bootentries.add(new Bootentry("MIUI v5", "Dad's Installation"));
        bootentries.add(new Bootentry("MIUI v6", "Mom's Installation"));
        bootentries.add(new Bootentry("AOSP 4.1", "Testing"));
        bootentries.add(new Bootentry("AOSP 4.2", "Testing"));
        bootentries.add(new Bootentry("AOSP 4.3", "Testing"));
        bootentries.add(new Bootentry("AOSP 4.4", "Testing"));
        bootentries.add(new Bootentry("AOSP 5.0", "Testing"));
        bootentries.add(new Bootentry("CM10.1", "Testing"));
        bootentries.add(new Bootentry("CM10.2", "Testing"));
        bootentries.add(new Bootentry("CM11", "Testing"));
        bootentries.add(new Bootentry("CM12", "Development"));
        bootentries.add(new Bootentry("Mokee", "Testing"));
        bootentries.add(new Bootentry("PAC 4.4", "Testing"));
        bootentries.add(new Bootentry("PAC 5.0", "Testing"));
        bootentries.add(new Bootentry("AOKP", "Testing"));
        bootentries.add(new Bootentry("Gummy", "Testing"));
        bootentries.add(new Bootentry("OMNI 4.4", "Testing"));
        bootentries.add(new Bootentry("OMNI 5.0", "Testing"));

        // adapter
        mAdapter = new BootentryAdapter(bootentries);
        mRecyclerView.setAdapter(mAdapter);

        // FAB
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.attachToRecyclerView(mRecyclerView);

        // inapp_notification button
        mNotificationBar = findViewById(R.id.inapp_notification);
        mNotificationButton = (Button) findViewById(R.id.inapp_notification_button);
        mNotificationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, UpdateActivity.class);
                startActivityForResult(intent, 0);
            }
        });

        // navbar adapter
        ArrayList<NavigationDrawerAdapter.Item> al = new ArrayList<>();
        al.add(new NavigationDrawerAdapter.Item(R.string.navbar_bootloader, 0));
        al.add(new NavigationDrawerAdapter.Item(R.string.action_settings, 1));
        NavigationDrawerAdapter adapter = new NavigationDrawerAdapter(al);
        adapter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // close drawers
                mDrawerLayout.closeDrawers();

                switch (v.getId()) {
                    case 0:
                        // HACK: launch activity with delay for smooth close animation for drawer
                        Utils.runOnUiThread(MainActivity.this, 350, new Runnable() {
                            @Override
                            public void run() {
                                Intent intent = new Intent(MainActivity.this, UpdateActivity.class);
                                startActivityForResult(intent, 0);
                            }
                        });

                        break;
                }

            }
        });

        // navigation drawer
        mNavigationDrawer = (ScrimInsetsFrameLayout) findViewById(R.id.left_drawer);
        mNavigationDrawer.setPadding(0, getStatusBarHeight(), 0, 0);
        mNavRecyclerView = (RecyclerView) findViewById(R.id.navbar_recycler_view);
        mNavRecyclerView.setHasFixedSize(true);
        mNavRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mNavRecyclerView.setAdapter(adapter);


        // update UI
        updateUI();
    }

    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        updateUI();
    }

    private void updateUI() {
        UpdaterClient.getDeviceList(this, new UpdaterClient.DeviceListReceivedCallback() {
            @Override
            public void onDeviceListReceived(List<String> devices, Exception eUC) {
                // check for exception
                if (eUC != null) {
                    Toast.makeText(MainActivity.this, getString(R.string.error_occurred) + ": " + eUC.getMessage(), Toast.LENGTH_LONG).show();
                    return;
                }

                // check device info
                if (devices == null || !devices.contains(Build.DEVICE)) {
                    Toast.makeText(MainActivity.this, R.string.device_not_supported, Toast.LENGTH_LONG).show();
                    mRecyclerView.setVisibility(View.GONE);
                    return;
                }
                mRecyclerView.setVisibility(View.VISIBLE);

                // load deviceinfo
                updateUI_loadDeviceInfo();
            }
        });
    }

    private void updateUI_loadDeviceInfo() {
        UpdaterClient.getDeviceInfo(this, new UpdaterClient.DeviceInfoReceivedCallback() {
            @Override
            public void onDeviceInfoReceived(JSONDeviceInfo deviceInfo, Exception eUC) {
                // check for exception
                if (eUC != null) {
                    Toast.makeText(MainActivity.this, getString(R.string.error_occurred) + ": " + eUC.getMessage(), Toast.LENGTH_LONG).show();
                    return;
                }

                // check device info
                mDeviceInfo = deviceInfo;
                if (deviceInfo == null) {
                    Toast.makeText(MainActivity.this, getString(R.string.error_occurred), Toast.LENGTH_LONG).show();
                    return;
                }

                try {
                    String bootPath = mDeviceInfo.getAbsoluteBootPath(false);
                    String manifestPath = bootPath + "/manifest.json";

                    if (RootUtils.exists(manifestPath)) {
                        mNotificationBar.setVisibility(View.GONE);
                    } else {
                        mNotificationBar.setVisibility(View.VISIBLE);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, getString(R.string.error_occurred) + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void setupToolbar() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer);
        android.support.v7.widget.Toolbar toolbar = (android.support.v7.widget.Toolbar) findViewById(R.id.toolbar);
        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(
                this,
                mDrawerLayout,
                toolbar,
                R.string.hello_world, // open
                R.string.app_name // close
        ) {
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                invalidateOptionsMenu();
                syncState();
            }

            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu();
                syncState();
            }
        };
        mDrawerLayout.setDrawerListener(actionBarDrawerToggle);
        mDrawerLayout.setStatusBarBackgroundColor(getResources().getColor(R.color.primary_dark));

        //Set the custom toolbar
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mDrawerLayout.openDrawer(Gravity.LEFT);
                }
            });
        }

        actionBarDrawerToggle.syncState();
    }
}
