package org.grub4android.grubmanager.activities;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import org.grub4android.grubmanager.R;
import org.grub4android.grubmanager.adapter.TwoLineAdapter;

import java.util.ArrayList;

public class UpdateActivity extends ActionBarActivity {
    private RecyclerView mRecyclerSysinfo;
    private TwoLineAdapter mAdapterSysinfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update);

        // toolbar
        android.support.v7.widget.Toolbar toolbar = (android.support.v7.widget.Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // sysinfo
        mRecyclerSysinfo = (RecyclerView) findViewById(R.id.my_recycler_view);
        mRecyclerSysinfo.setHasFixedSize(true);
        mRecyclerSysinfo.setLayoutManager(new LinearLayoutManager(this));

        ArrayList<TwoLineAdapter.Dataset> dataset = new ArrayList<>();
        dataset.add(new TwoLineAdapter.Dataset("Install/Update", null, TwoLineAdapter.ViewType.TYPE_SUBHEADER, 0));
        dataset.add(new TwoLineAdapter.Dataset("Update", "New version: abcdef", TwoLineAdapter.ViewType.TYPE_ITEM, android.R.id.button1));
        dataset.add(new TwoLineAdapter.Dataset("Changelog", "See what's new in this version", TwoLineAdapter.ViewType.TYPE_ITEM, android.R.id.button2));
        dataset.add(new TwoLineAdapter.Dataset("Uninstall", "Revert to original bootloader", TwoLineAdapter.ViewType.TYPE_ITEM, android.R.id.button3));

        dataset.add(new TwoLineAdapter.Dataset("System info", null, TwoLineAdapter.ViewType.TYPE_SUBHEADER, 0));
        dataset.add(new TwoLineAdapter.Dataset("GRUB", "2.02~beta2-8d4abea", TwoLineAdapter.ViewType.TYPE_ITEM, 0));
        dataset.add(new TwoLineAdapter.Dataset("LK", "0.5-a76337b", TwoLineAdapter.ViewType.TYPE_ITEM, 0));
        dataset.add(new TwoLineAdapter.Dataset("Multiboot", "0.1-6791079", TwoLineAdapter.ViewType.TYPE_ITEM, 0));

        mAdapterSysinfo = new TwoLineAdapter(dataset);
        mAdapterSysinfo.setOnDatasetItemClickListener(new TwoLineAdapter.OnDatasetItemClickListener() {
            @Override
            public void onClick(TwoLineAdapter.Dataset dataset) {
                Toast.makeText(UpdateActivity.this, dataset.mTitle, Toast.LENGTH_SHORT).show();
            }
        });
        mRecyclerSysinfo.setAdapter(mAdapterSysinfo);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_update, menu);
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

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
