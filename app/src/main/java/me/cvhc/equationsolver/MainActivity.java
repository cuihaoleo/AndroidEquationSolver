package me.cvhc.equationsolver;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends IMEDetectActivity {
    MainFragment mCurrentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        assert toolbar != null;
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayShowTitleEnabled(true);

        mCurrentFragment = MainFragment.newInstance();
        mCurrentFragment.setArguments(savedInstanceState);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, mCurrentFragment)
                .commit();
    }

    /**
     * Call onBackPressed method of MainFragment. If fragment's returns false,
     * superclass's onBackPressed is called to terminate the app.
     *
     * This allows fragment to "override" this method.
     */
    @Override
    public void onBackPressed() {
        if (!mCurrentFragment.onBackPressed()) {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.action_help:
                startActivity(new Intent(this, HelpActivity.class));
                return true;
            case R.id.action_exit:
                new AlertDialog.Builder(this)
                        .setTitle(R.string.action_exit)
                        .setMessage(R.string.confirm_exit)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                MainActivity.this.finish();
                            }
                        })
                        .setNegativeButton(android.R.string.no, null)
                        .setIconAttribute(android.R.attr.alertDialogIcon)
                        .show();
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
