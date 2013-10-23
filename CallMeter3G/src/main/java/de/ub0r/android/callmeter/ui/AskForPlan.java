/*
 * Copyright (C) 2009-2013 Felix Bechstein
 * 
 * This file is part of CallMeter 3G.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; If not, see <http://www.gnu.org/licenses/>.
 */
package de.ub0r.android.callmeter.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import de.ub0r.android.callmeter.CallMeter;
import de.ub0r.android.callmeter.R;
import de.ub0r.android.callmeter.data.DataProvider;
import de.ub0r.android.callmeter.data.RuleMatcher;
import de.ub0r.android.callmeter.ui.prefs.Preferences;
import de.ub0r.android.lib.Log;
import de.ub0r.android.lib.Utils;

/**
 * Display Ask for plan {@link Activity}.
 *
 * @author flx
 */
public final class AskForPlan extends Activity implements OnClickListener, OnDismissListener {

    /** Tag for output. */
    private static final String TAG = "afp";

    /** Extra providing id of call. */
    public static final String EXTRA_ID = "id";
    /** Extra providing date of call. */
    public static final String EXTRA_DATE = "date";
    /** Extra providing amount of call. */
    public static final String EXTRA_AMOUNT = "amount";

    /** Ids of plans' {@link Button}s. */
    private static final int[] PLAN_BTNS = new int[]{R.id.btn00, R.id.btn01, R.id.btn02,
            R.id.btn03, R.id.btn04, R.id.btn05, R.id.btn06, R.id.btn07, R.id.btn08, R.id.btn09,
            R.id.btn10, R.id.btn11, R.id.btn12, R.id.btn13, R.id.btn14, R.id.btn15, R.id.btn16,
            R.id.btn17, R.id.btn18, R.id.btn19};
    /** Maximal number of plans. */
    private static final int MAX_PLANS = PLAN_BTNS.length;
    /** Ids of plans. */
    private final int[] planIds = new int[MAX_PLANS];

    /** Task updating timeout field. */
    private AsyncTask<Void, Void, Void> timeoutTask = null;

    /** {@link TextView} holding timeout. */
    private TextView tvTimeout = null;
    /** {@link CheckBox} holding set default. */
    private CheckBox cbSetDefault = null;

    /** Default plan id. */
    private int defaultPlanId = -1;

    /** Data of call. */
    private long id;

    /** Inner {@link Dialog}. */
    private Dialog d = null;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.setLocale(this);
        d = new Dialog(this);
        d.setTitle(R.string.select_plan_);
        d.setContentView(R.layout.ask_for_plan);
        d.setCancelable(true);
        d.setOnDismissListener(this);

        final Intent intent = getIntent();
        id = intent.getLongExtra(EXTRA_ID, -1L);
        if (id < 0L) {
            Log.e(TAG, "no id:" + id);
            finish();
            return;
        }

        tvTimeout = (TextView) d.findViewById(R.id.autohide);
        cbSetDefault = (CheckBox) d.findViewById(R.id.set_default);
        d.findViewById(R.id.cancel).setOnClickListener(this);

        final Cursor c = getContentResolver().query(DataProvider.Plans.CONTENT_URI,
                DataProvider.Plans.PROJECTION,
                DataProvider.Plans.TYPE + " = " + DataProvider.TYPE_CALL, null, null);
        if (c == null || !c.moveToFirst()) {
            Log.e(TAG, "no plans: " + c);
            if (c != null && !c.isClosed()) {
                c.close();
            }
            finish();
            return;
        }

        int i = 0;
        do {
            planIds[i] = c.getInt(DataProvider.Plans.INDEX_ID);

            final Button v = (Button) d.findViewById(PLAN_BTNS[i]);
            v.setVisibility(View.VISIBLE);
            v.setOnClickListener(this);
            v.setText(c.getString(DataProvider.Plans.INDEX_NAME));
            ++i;
        } while (i < MAX_PLANS && c.moveToNext());

        if (!c.isClosed()) {
            c.close();
        }

        final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(this);
        try {
            defaultPlanId = p.getInt(Preferences.PREFS_ASK_FOR_PLAN_DEFAULT, -1);
        } catch (ClassCastException e) {
            // TODO: delete me in some later version
            Log.e(TAG, "legacy error hanling", e);
            defaultPlanId = (int) p.getLong(Preferences.PREFS_ASK_FOR_PLAN_DEFAULT, -1);
            p.edit().putInt(Preferences.PREFS_ASK_FOR_PLAN_DEFAULT, defaultPlanId).commit();
        }
        for (i = 0; i < MAX_PLANS; i++) {
            if (planIds[i] == defaultPlanId) {
                final int bid = PLAN_BTNS[i];
                Log.d(TAG, "request focus: " + bid);
                final Button v = (Button) d.findViewById(bid);
                v.requestFocus();
                v.setTextAppearance(this, android.R.style.TextAppearance_Large);
                break;
            }
        }
        String s = p.getString(Preferences.PREFS_ASK_FOR_PLAN_AUTOHIDE, "");
        final int timeout = Utils.parseInt(s, 0);
        if (timeout > 0) {
            timeoutTask = new AsyncTask<Void, Void, Void>() {
                private int count = timeout;

                @Override
                protected Void doInBackground(final Void... params) {
                    while (count > 0) {
                        try {
                            Thread.sleep(CallMeter.MILLIS);
                        } catch (InterruptedException e) {
                            Log.e(TAG, "intr. count=" + count, e);
                        }
                        --this.count;
                        publishProgress((Void) null);
                    }
                    return null;
                }

                @Override
                protected void onProgressUpdate(final Void... values) {
                    String s = String.format(AskForPlan.this.getString(R.string.autohide_in_),
                            count);
                    AskForPlan.this.tvTimeout.setText(s);
                }

                @Override
                protected void onPostExecute(final Void result) {
                    if (!this.isCancelled() && !AskForPlan.this.isFinishing()) {
                        Log.i(TAG, "autohide dialog");
                        final int pid = AskForPlan.this.defaultPlanId;
                        if (pid >= 0L) {
                            Log.i(TAG, "setPlan(" + pid + ")");
                            RuleMatcher.matchLog(AskForPlan.this.getContentResolver(),
                                    AskForPlan.this.id, pid);
                        }
                        AskForPlan.this.d.cancel();
                        AskForPlan.this.finish();
                    }
                }
            };
            timeoutTask.execute((Void) null);
        } else {
            tvTimeout.setVisibility(View.GONE);
        }

        d.show();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (timeoutTask != null) {
            timeoutTask.cancel(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onClick(final View v) {
        final int vid = v.getId();
        switch (vid) {
            case R.id.cancel:
                d.cancel();
                finish();
                break;
            default:
                for (int i = 0; i < MAX_PLANS; i++) {
                    if (vid == PLAN_BTNS[i]) {
                        final int pid = planIds[i];
                        Log.d(TAG, "setPlan(" + pid + ")");
                        RuleMatcher.matchLog(getContentResolver(), id, pid);
                        if (cbSetDefault.isChecked()) {
                            final Editor e = PreferenceManager.getDefaultSharedPreferences(this)
                                    .edit();
                            e.putInt(Preferences.PREFS_ASK_FOR_PLAN_DEFAULT, pid);
                            e.commit();
                        }
                        d.cancel();
                        finish();
                        break;
                    }
                }
                break;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDismiss(final DialogInterface dialog) {
        finish();
    }
}
