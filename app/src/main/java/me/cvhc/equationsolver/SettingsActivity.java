package me.cvhc.equationsolver;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;


public class SettingsActivity extends AppCompatPreferenceActivity {
    private SettingsFragment fragment;

    @Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        fragment = new SettingsFragment();
        getFragmentManager().beginTransaction().replace(android.R.id.content, fragment).commit();
        setupActionBar();
    }

    public static class SettingsFragment extends PreferenceFragment
            implements SharedPreferences.OnSharedPreferenceChangeListener
    {
        private SharedPreferences sharedPreferences;

        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

            Preference prefResetSettings = findPreference("pref_reset_settings");
            prefResetSettings.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    final Context context = SettingsFragment.this.getActivity();
                    new AlertDialog.Builder(context)
                            .setTitle(R.string.pref_reset_settings)
                            .setMessage("Please confirm!")
                            .setIconAttribute(android.R.attr.alertDialogIcon)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener(){
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    sharedPreferences.edit().clear().commit();
                                    initPrefSummary();
                                }
                            })
                            .setNegativeButton(android.R.string.cancel, null)
                            .show();
                    return true;
                }
            });

            initPrefSummary();
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        }

        @Override
        public void onPause() {
            getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
            super.onPause();
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            updatePrefSummary(findPreference(key));
        }

        private void initPrefSummary() {
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getActivity());

            for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); ++i) {
                Preference preference = getPreferenceScreen().getPreference(i);

                if (preference instanceof PreferenceCategory) {
                    PreferenceCategory pGrp = (PreferenceCategory) preference;
                    for (int j = 0; j < pGrp.getPreferenceCount(); j++) {
                        updatePrefSummary(pGrp.getPreference(j));
                    }
                } else {
                    updatePrefSummary(preference);
                }
            }
        }

        private void updatePrefSummary(Preference pref) {
            String key = pref.getKey();

            if (key == null) { return; };

            switch (key) {
                case "pref_plot_samples":
                    pref.setSummary(String.format(getString(R.string.pref_summary_plot_sampling_points),
                            sharedPreferences.getInt(key, 0)));
                    break;
                case "pref_default_lower_bound":
                case "pref_default_upper_bound":
                    pref.setSummary(String.format(getString(R.string.format_bound),
                            sharedPreferences.getFloat(key, Float.NaN)));
                    break;
            }
        }
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            if (!super.onMenuItemSelected(featureId, item)) {
                NavUtils.navigateUpFromSameTask(this);
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }
}