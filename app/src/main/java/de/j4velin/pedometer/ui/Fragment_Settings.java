/*
 * Copyright 2014 Thomas Hoffmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.j4velin.pedometer.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.RadioGroup;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;

import de.j4velin.pedometer.Database;
import de.j4velin.pedometer.R;

public class Fragment_Settings extends PreferenceFragment implements OnPreferenceClickListener {

    final static int DEFAULT_GOAL = 10000;
    final static float DEFAULT_STEP_SIZE = Locale.getDefault() == Locale.US ? 2.5f : 75f;
    final static String DEFAULT_STEP_UNIT = Locale.getDefault() == Locale.US ? "ft" : "cm";

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings);

        final SharedPreferences prefs =
                getActivity().getSharedPreferences("pedometer", Context.MODE_PRIVATE);

        findPreference("import").setOnPreferenceClickListener(this);
        findPreference("export").setOnPreferenceClickListener(this);

        Preference goal = findPreference("goal");
        goal.setOnPreferenceClickListener(this);
        goal.setSummary(getString(R.string.goal_summary, prefs.getInt("goal", DEFAULT_GOAL)));

        Preference stepsize = findPreference("stepsize");
        stepsize.setOnPreferenceClickListener(this);
        stepsize.setSummary(getString(R.string.step_size_summary,
                prefs.getFloat("stepsize_value", DEFAULT_STEP_SIZE),
                prefs.getString("stepsize_unit", DEFAULT_STEP_UNIT)));

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        inflater.inflate(R.menu.main, menu);
    }

    @Override
    public void onPrepareOptionsMenu(final Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.action_settings).setVisible(false);
        menu.findItem(R.id.action_split_count).setVisible(false);
    }

    @Override
    public boolean onPreferenceClick(final Preference preference) {
        AlertDialog.Builder builder;
        View v;
        final SharedPreferences prefs =
                getActivity().getSharedPreferences("pedometer", Context.MODE_PRIVATE);
        switch (preference.getTitleRes()) {
            case R.string.goal:
                builder = new AlertDialog.Builder(getActivity());
                final NumberPicker np = new NumberPicker(getActivity());
                np.setMinValue(1);
                np.setMaxValue(100000);
                np.setValue(prefs.getInt("goal", 10000));
                builder.setView(np);
                builder.setTitle(R.string.set_goal);
                builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    np.clearFocus();
                    prefs.edit().putInt("goal", np.getValue()).commit();
                    preference.setSummary(getString(R.string.goal_summary, np.getValue()));
                    dialog.dismiss();
                });
                builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss());
                Dialog dialog = builder.create();
                dialog.getWindow().setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                dialog.show();
                break;
            case R.string.step_size:
                builder = new AlertDialog.Builder(getActivity());
                v = getActivity().getLayoutInflater().inflate(R.layout.stepsize, null);
                final RadioGroup unit = (RadioGroup) v.findViewById(R.id.unit);
                final EditText value = (EditText) v.findViewById(R.id.value);
                unit.check(
                        prefs.getString("stepsize_unit", DEFAULT_STEP_UNIT).equals("cm") ? R.id.cm :
                                R.id.ft);
                value.setText(String.valueOf(prefs.getFloat("stepsize_value", DEFAULT_STEP_SIZE)));
                builder.setView(v);
                builder.setTitle(R.string.set_step_size);
                builder.setPositiveButton(android.R.string.ok, (dialog1, which) -> {
                    try {
                        prefs.edit().putFloat("stepsize_value",
                                        Float.valueOf(value.getText().toString()))
                                .putString("stepsize_unit",
                                        unit.getCheckedRadioButtonId() == R.id.cm ? "cm" : "ft")
                                .apply();
                        preference.setSummary(getString(R.string.step_size_summary,
                                Float.valueOf(value.getText().toString()),
                                unit.getCheckedRadioButtonId() == R.id.cm ? "cm" : "ft"));
                    } catch (NumberFormatException nfe) {
                        nfe.printStackTrace();
                    }
                    dialog1.dismiss();
                });
                builder.setNegativeButton(android.R.string.cancel, (dialog12, which) -> dialog12.dismiss());
                builder.create().show();
                break;
            case R.string.import_title:
            case R.string.export_title:
                if (preference.getTitleRes() == R.string.import_title) {
                    importCsv();
                } else {
                    exportCsv();
                }
                break;
        }
        return false;
    }

    /**
     * Creates the CSV file containing data about past days and the steps taken on them
     * <p/>
     * Requires external storage to be writeable
     */
    private void exportCsv() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            final File f = new File(getContext().getApplicationContext().getFilesDir().getAbsolutePath(), "Pedometer.csv");
            if (f.exists()) {
                new AlertDialog.Builder(getActivity()).setMessage(R.string.file_already_exists)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            dialog.dismiss();
                            writeToFile(f);
                        }).setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss()).create().show();
            } else {
                writeToFile(f);
            }
        } else {
            new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.error_external_storage_not_available)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss()).create().show();
        }
    }

    /**
     * Imports previously exported data from a csv file
     * <p/>
     * Requires external storage to be readable. Overwrites days for which there is already an entry in the database
     */
    private void importCsv() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File f = new File(getContext().getApplicationContext().getFilesDir().getAbsolutePath(), "Pedometer.csv");
            if (!f.exists() || !f.canRead()) {
                new AlertDialog.Builder(getActivity())
                        .setMessage(getString(R.string.file_cant_read, f.getAbsolutePath()))
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss()).create().show();
                return;
            }
            Database db = Database.getInstance(getActivity());
            String line;
            String[] data;
            int ignored = 0, inserted = 0, overwritten = 0;
            BufferedReader in;
            try {
                in = new BufferedReader(new FileReader(f));
                while ((line = in.readLine()) != null) {
                    data = line.split(";");
                    try {
                        if (db.insertDayFromBackup(Long.valueOf(data[0]),
                                Integer.valueOf(data[1]))) {
                            inserted++;
                        } else {
                            overwritten++;
                        }
                    } catch (Exception nfe) {
                        ignored++;
                    }
                }
                in.close();
            } catch (IOException e) {
                new AlertDialog.Builder(getActivity())
                        .setMessage(getString(R.string.error_file, e.getMessage()))
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss()).create().show();
                e.printStackTrace();
                return;
            } finally {
                db.close();
            }
            String message = getString(R.string.entries_imported, inserted + overwritten);
            if (overwritten > 0)
                message += "\n\n" + getString(R.string.entries_overwritten, overwritten);
            if (ignored > 0) message += "\n\n" + getString(R.string.entries_ignored, ignored);
            new AlertDialog.Builder(getActivity()).setMessage(message)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss()).create().show();
        } else {
            new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.error_external_storage_not_available)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss()).create().show();
        }
    }

    private void writeToFile(final File f) {
        BufferedWriter out;
        try {
            f.createNewFile();
            out = new BufferedWriter(new FileWriter(f));
        } catch (IOException e) {
            new AlertDialog.Builder(getActivity())
                    .setMessage(getString(R.string.error_file, e.getMessage()))
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss()).create().show();
            e.printStackTrace();
            return;
        }
        Database db = Database.getInstance(getActivity());
        Cursor c =
                db.query(new String[]{"date", "steps"}, "date > 0", null, null, null, "date", null);
        try {
            if (c != null && c.moveToFirst()) {
                while (!c.isAfterLast()) {
                    out.append(c.getString(0)).append(";")
                            .append(String.valueOf(Math.max(0, c.getInt(1)))).append("\n");
                    c.moveToNext();
                }
            }
            out.flush();
            out.close();
        } catch (IOException e) {
            new AlertDialog.Builder(getActivity())
                    .setMessage(getString(R.string.error_file, e.getMessage()))
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss()).create().show();
            e.printStackTrace();
            return;
        } finally {
            if (c != null) c.close();
            db.close();
        }
        new AlertDialog.Builder(getActivity())
                .setMessage(getString(R.string.data_saved, f.getAbsolutePath()))
                .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss()).create().show();
    }
}
