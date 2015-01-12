package org.grub4android.grubmanager.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.melnykov.fab.FloatingActionButton;

import org.grub4android.grubmanager.R;
import org.grub4android.grubmanager.adapter.BootentryAdapter;
import org.grub4android.grubmanager.models.Bootentry;

import java.util.ArrayList;


public class MainActivity extends ActionBarActivity {
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private Button mNotificationButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupToolbar();

        mRecyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // specify an adapter (see also next example)
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

        mAdapter = new BootentryAdapter(bootentries);
        mRecyclerView.setAdapter(mAdapter);

        // FAB
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.attachToRecyclerView(mRecyclerView);

        // inapp_notification button
        mNotificationButton = (Button) findViewById(R.id.inapp_notification_button);
        mNotificationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, UpdateActivity.class);
                startActivity(intent);
            }
        });
    }

    private void setupToolbar() {
        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer);
        android.support.v7.widget.Toolbar toolbar = (android.support.v7.widget.Toolbar) findViewById(R.id.toolbar);
        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.hello_world, // open
                R.string.app_name // close
        )

        {
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
        drawerLayout.setDrawerListener(actionBarDrawerToggle);
        drawerLayout.setStatusBarBackgroundColor(getResources().getColor(R.color.primary_dark));

        //Set the custom toolbar
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        actionBarDrawerToggle.syncState();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
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
