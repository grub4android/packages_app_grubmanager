package org.grub4android.grubmanager;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;

import com.melnykov.fab.FloatingActionButton;

import org.grub4android.grubmanager.models.Bootentry;

import java.util.ArrayList;


public class MainActivity extends ActionBarActivity {
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
