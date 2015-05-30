/*
 * Copyright (C) 2009-2013 Felix Bechstein
 * 
 * This file is part of Call Meter 3G.
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
package de.ub0r.android.callmeter.ui.prefs;

import android.app.AlertDialog.Builder;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.telephony.TelephonyManager;
import android.text.InputType;
import android.text.TextUtils;

import de.ub0r.android.callmeter.R;
import de.ub0r.android.callmeter.data.DataProvider;
import de.ub0r.android.callmeter.data.LogRunnerService;
import de.ub0r.android.callmeter.data.RuleMatcher;
import de.ub0r.android.callmeter.ui.TrackingSherlockPreferenceActivity;
import de.ub0r.android.lib.DbUtils;
import de.ub0r.android.lib.Utils;
import de.ub0r.android.logg0r.Log;

/**
 * Edit a single Plan.
 *
 * @author flx
 */
public final class RuleEdit extends TrackingSherlockPreferenceActivity implements UpdateListener {

    /**
     * Tag for debug out.
     */
    private static final String TAG = "RuleEdit";

    /**
     * This rule's {@link Uri}.
     */
    private Uri uri = null;

    /**
     * {@link ContentValues} holding preferences.
     */
    private ContentValues values = new ContentValues();

    /**
     * Array holding {@link String}s.
     */
    private String[] inOutNoMatterCalls = null;

    /**
     * Array holding {@link String}s.
     */
    private String[] inOutNoMatterSms = null;

    /**
     * Array holding {@link String}s.
     */
    private String[] inOutNoMatterMms = null;

    /**
     * Array holding {@link String}s.
     */
    private String[] inOutNoMatterData = null;

    /**
     * Array holding {@link String}s.
     */
    private String[] yesNoNoMatter = null;

    /**
     * Get a string array for directions.
     *
     * @param type type of array
     * @return string array
     */
    private int getStringArray(final int type) {
        switch (type) {
            case DataProvider.TYPE_SMS:
                return R.array.direction_sms;
            case DataProvider.TYPE_MMS:
                return R.array.direction_mms;
            case DataProvider.TYPE_DATA:
                return R.array.direction_data;
            default:
                return R.array.direction_calls;
        }
    }

    /**
     * Get a {@link String}-Array for ListView.
     *
     * @param base base array without no_matter_
     * @return array with no_matter_
     */
    private String[] getStrings(final int base) {
        switch (base) {
            case R.array.direction_calls:
                if (inOutNoMatterCalls == null) {
                    final String[] tmp1 = new String[3];
                    final String[] tmp2 = getResources().getStringArray(base);
                    tmp1[0] = tmp2[0];
                    tmp1[1] = tmp2[1];
                    tmp1[2] = getString(R.string.no_matter_);
                    inOutNoMatterCalls = tmp1;
                }
                return inOutNoMatterCalls;
            case R.array.direction_sms:
                if (inOutNoMatterSms == null) {
                    final String[] tmp1 = new String[3];
                    final String[] tmp2 = getResources().getStringArray(base);
                    tmp1[0] = tmp2[0];
                    tmp1[1] = tmp2[1];
                    tmp1[2] = getString(R.string.no_matter_);
                    inOutNoMatterSms = tmp1;
                }
                return inOutNoMatterSms;
            case R.array.direction_mms:
                if (inOutNoMatterMms == null) {
                    final String[] tmp1 = new String[3];
                    final String[] tmp2 = getResources().getStringArray(base);
                    tmp1[0] = tmp2[0];
                    tmp1[1] = tmp2[1];
                    tmp1[2] = getString(R.string.no_matter_);
                    inOutNoMatterMms = tmp1;
                }
                return inOutNoMatterMms;
            case R.array.direction_data:
                if (inOutNoMatterData == null) {
                    final String[] tmp1 = new String[3];
                    final String[] tmp2 = getResources().getStringArray(base);
                    tmp1[0] = tmp2[0];
                    tmp1[1] = tmp2[1];
                    tmp1[2] = getString(R.string.no_matter_);
                    inOutNoMatterData = tmp1;
                }
                return inOutNoMatterData;
            default:
                if (yesNoNoMatter == null) {
                    final String[] tmp1 = new String[3];
                    tmp1[0] = getString(R.string.yes);
                    tmp1[1] = getString(R.string.no);
                    tmp1[2] = getString(R.string.no_matter_);
                    yesNoNoMatter = tmp1;
                }
                return yesNoNoMatter;
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.setLocale(this);

        addPreferencesFromResource(R.xml.group_prefs);
        uri = getIntent().getData();
    }

    @Override
    protected void onResume() {
        super.onResume();

        values.clear();
        reload();
    }

    /**
     * Reload plans from ContentProvider.
     */
    @SuppressWarnings("deprecation")
    private void reload() {
        PreferenceScreen ps = (PreferenceScreen) findPreference("container");
        ps.removeAll();

        boolean hasCallsSimId = LogRunnerService.checkCallsSimIdColumn(getContentResolver());
        boolean hasSmsSimId = LogRunnerService.checkSmsSimIdColumn(getContentResolver());
        Cursor c = getContentResolver().query(uri, DataProvider.Rules.PROJECTION, null,
                null, null);
        if (c.moveToFirst()) {
            // name
            CVEditTextPreference ep = new CVEditTextPreference(this, values,
                    DataProvider.Rules.NAME, R.string.rules_new);
            ep.setTitle(R.string.name_);
            ep.setSummary(R.string.name_help);
            ep.setText(c.getString(DataProvider.Rules.INDEX_NAME));
            ep.setInputType(InputType.TYPE_CLASS_TEXT);
            ps.addPreference(ep);
            getSupportActionBar().setSubtitle(ep.getText());
            // active
            CVCheckBoxPreference cp = new CVCheckBoxPreference(this, values,
                    DataProvider.Rules.ACTIVE);
            cp.setTitle(R.string.active_);
            cp.setSummary(R.string.active_help);
            cp.setChecked(c.isNull(DataProvider.Rules.INDEX_ACTIVE)
                    || c.getInt(DataProvider.Rules.INDEX_ACTIVE) == 1);
            ps.addPreference(cp);
            // what
            CVListPreference lp = new CVListPreference(this, values, DataProvider.Rules.WHAT);
            lp.setTitle(R.string.what_);
            lp.setSummary(R.string.what_help);
            lp.setStatic(R.array.rules_type_id, R.array.rules_type);
            int w;
            if (c.isNull(DataProvider.Rules.INDEX_WHAT)) {
                Log.d(TAG, "what: null");
                w = DataProvider.Rules.WHAT_CALL;
                values.put(DataProvider.Rules.WHAT, w);
            } else {
                w = c.getInt(DataProvider.Rules.INDEX_WHAT);
            }
            Log.d(TAG, "what: ", w);
            lp.setValue(String.valueOf(w));
            ps.addPreference(lp);
            int t = DataProvider.what2type(w);
            // plan
            lp = new CVListPreference(this, values, DataProvider.Rules.PLAN_ID);
            lp.setTitle(R.string.plan_);
            lp.setSummary(R.string.plan_help);
            lp.setCursor(
                    getContentResolver().query(DataProvider.Plans.CONTENT_URI,
                            DataProvider.Plans.PROJECTION_BASIC, getPlanWhere(w), null, null),
                    DataProvider.Plans.INDEX_ID, DataProvider.Plans.INDEX_NAME).close();
            lp.setValue(c.getString(DataProvider.Rules.INDEX_PLAN_ID));
            ps.addPreference(lp);
            // limit reached
            cp = new CVCheckBoxPreference(this, values, DataProvider.Rules.LIMIT_NOT_REACHED);
            cp.setTitle(R.string.limitnotreached_);
            cp.setSummary(R.string.limitnotreached_help);
            cp.setChecked(!c.isNull(DataProvider.Rules.INDEX_LIMIT_NOT_REACHED)
                    && c.getInt(DataProvider.Rules.INDEX_LIMIT_NOT_REACHED) == 1);
            ps.addPreference(cp);
            // direction
            lp = new CVListPreference(this, values, DataProvider.Rules.DIRECTION);
            lp.setTitle(R.string.direction_);
            lp.setSummary(R.string.direction_help);
            lp.setStatic(
                    new String[]{String.valueOf(DataProvider.DIRECTION_IN),
                            String.valueOf(DataProvider.DIRECTION_OUT), "-1"},
                    getStrings(getStringArray(t)));
            int i;
            if (c.isNull(DataProvider.Rules.INDEX_DIRECTION)) {
                i = -1;
                values.put(DataProvider.Rules.DIRECTION, i);
            } else {
                i = c.getInt(DataProvider.Rules.INDEX_DIRECTION);
            }
            lp.setValue(String.valueOf(i));
            ps.addPreference(lp);
            // my number
            TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            final String mynumber = tm.getLine1Number();
            if ((hasCallsSimId && w == DataProvider.Rules.WHAT_CALL) || // .
                    (hasSmsSimId && w == DataProvider.Rules.WHAT_SMS)) {
                ep = new CVEditTextPreference(this, values, DataProvider.Rules.MYNUMBER, null);
                ep.setTitle(R.string.my_sim_id_);
                ep.setSummary(R.string.my_sim_id_help);
                ep.setText(c.getString(DataProvider.Rules.INDEX_MYNUMBER));
                ep.setInputType(InputType.TYPE_CLASS_NUMBER);
                ps.addPreference(ep);
            } else if (!TextUtils.isEmpty(mynumber)) {
                ep = new CVEditTextPreference(this, values, DataProvider.Rules.MYNUMBER, null) {
                    @Override
                    protected void onPrepareDialogBuilder(final Builder builder) {
                        super.onPrepareDialogBuilder(builder);
                        final CVEditTextPreference pref = this;
                        builder.setNeutralButton(R.string.set_current_number,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(final DialogInterface dialog,
                                            final int which) {
                                        // pref.setText(mynumber);
                                        RuleEdit.this.values.put(pref.getKey(), mynumber);
                                        RuleEdit.this.onUpdateValue(pref);
                                    }
                                });
                    }
                };
                ep.setTitle(R.string.my_number_);
                ep.setSummary(R.string.my_number_help);
                ep.setText(c.getString(DataProvider.Rules.INDEX_MYNUMBER));
                ep.setInputType(InputType.TYPE_CLASS_PHONE);
                ps.addPreference(ep);
            }
            // roamed
            lp = new CVListPreference(this, values, DataProvider.Rules.ROAMED);
            lp.setTitle(R.string.roamed_);
            lp.setSummary(R.string.roamed_help);
            lp.setStatic(new String[]{"0", "1", String.valueOf(DataProvider.Rules.NO_MATTER)},
                    getStrings(-1));
            if (c.isNull(DataProvider.Rules.INDEX_ROAMED)) {
                i = DataProvider.Rules.NO_MATTER;
                values.put(DataProvider.Rules.ROAMED, i);
            } else {
                i = c.getInt(DataProvider.Rules.INDEX_ROAMED);
            }
            lp.setValue(String.valueOf(i));
            ps.addPreference(lp);
            if (w == DataProvider.Rules.WHAT_SMS) {
                // is websms
                lp = new CVListPreference(this, values, DataProvider.Rules.IS_WEBSMS);
                lp.setTitle(R.string.iswebsms_);
                lp.setSummary(R.string.iswebsms_help);
                lp.setStatic(new String[]{"0", "1", "-1"}, getStrings(-1));
                if (c.isNull(DataProvider.Rules.INDEX_IS_WEBSMS)) {
                    i = -1;
                    values.put(DataProvider.Rules.IS_WEBSMS, i);
                } else {
                    i = c.getInt(DataProvider.Rules.INDEX_IS_WEBSMS);
                }
                lp.setValue(String.valueOf(i));
                ps.addPreference(lp);
                if (i == 0) {
                    // websms connector
                    ep = new CVEditTextPreference(this, values,
                            DataProvider.Rules.IS_WEBSMS_CONNETOR, null);
                    ep.setTitle(R.string.iswebsms_connector_);
                    ep.setSummary(R.string.iswebsms_connector_help);
                    ep.setText(c.getString(DataProvider.Rules.INDEX_IS_WEBSMS_CONNETOR));
                    ep.setInputType(InputType.TYPE_CLASS_TEXT);
                    ps.addPreference(ep);
                }
            }
            if (w == DataProvider.Rules.WHAT_CALL) {
                // is sip call
                lp = new CVListPreference(this, values, DataProvider.Rules.IS_SIPCALL);
                lp.setTitle(R.string.issipcall_);
                lp.setSummary(R.string.issipcall_help);
                lp.setStatic(new String[]{"0", "1", "-1"}, getStrings(-1));
                if (c.isNull(DataProvider.Rules.INDEX_IS_SIPCALL)) {
                    i = -1;
                    values.put(DataProvider.Rules.IS_SIPCALL, i);
                } else {
                    i = c.getInt(DataProvider.Rules.INDEX_IS_SIPCALL);
                }
                lp.setValue(String.valueOf(i));
                ps.addPreference(lp);
            }
            // in-/exclude hours
            Cursor query = getContentResolver().query(DataProvider.HoursGroup.CONTENT_URI,
                    DataProvider.HoursGroup.PROJECTION, null, null, null);
            if (query.getCount() > 0) {
                // include hours
                lp = new CVListPreference(this, values, DataProvider.Rules.INHOURS_ID, true);
                lp.setTitle(R.string.hourgroup_);
                lp.setSummary(R.string.hourgroup_help);
                lp.setCursor(query, DataProvider.HoursGroup.INDEX_ID,
                        DataProvider.HoursGroup.INDEX_NAME);
                lp.setValue(c.getString(DataProvider.Rules.INDEX_INHOURS_ID));
                ps.addPreference(lp);
                // exclude hours
                lp = new CVListPreference(this, values, DataProvider.Rules.EXHOURS_ID, true);
                lp.setTitle(R.string.exhourgroup_);
                lp.setSummary(R.string.exhourgroup_help);
                lp.setCursor(query, DataProvider.HoursGroup.INDEX_ID,
                        DataProvider.HoursGroup.INDEX_NAME);
                lp.setValue(c.getString(DataProvider.Rules.INDEX_EXHOURS_ID));
                ps.addPreference(lp);
            }
            query.close();
            if (w != DataProvider.Rules.WHAT_DATA) {
                // in-/exclude numbers
                query = getContentResolver().query(DataProvider.NumbersGroup.CONTENT_URI,
                        DataProvider.NumbersGroup.PROJECTION, null, null, null);
                if (query.getCount() > 0) {
                    // include numbers
                    lp = new CVListPreference(this, values, DataProvider.Rules.INNUMBERS_ID, true);
                    lp.setTitle(R.string.numbergroup_);
                    lp.setSummary(R.string.numbergroup_help);
                    lp.setCursor(query, DataProvider.NumbersGroup.INDEX_ID,
                            DataProvider.NumbersGroup.INDEX_NAME);
                    lp.setValue(c.getString(DataProvider.Rules.INDEX_INNUMBERS_ID));
                    ps.addPreference(lp);
                    // exclude numbers
                    lp = new CVListPreference(this, values, DataProvider.Rules.EXNUMBERS_ID, true);
                    lp.setTitle(R.string.exnumbergroup_);
                    lp.setSummary(R.string.exnumbergroup_help);
                    lp.setCursor(query, DataProvider.NumbersGroup.INDEX_ID,
                            DataProvider.NumbersGroup.INDEX_NAME);
                    lp.setValue(c.getString(DataProvider.Rules.INDEX_EXNUMBERS_ID));
                    ps.addPreference(lp);
                }
                query.close();
            }
        }
        c.close();
        if (values.size() > 0) {
            getContentResolver().update(uri, values, null, null);
            values.clear();
        }
    }

    /**
     * Set plan's and what0 value.
     *
     * @param w type
     * @return where clause
     */
    private String getPlanWhere(final int w) {
        String where;
        switch (w) {
            case DataProvider.Rules.WHAT_CALL:
                where = DataProvider.Plans.TYPE + " = " + DataProvider.TYPE_CALL + " OR "
                        + DataProvider.Plans.TYPE + " = " + DataProvider.TYPE_MIXED;
                break;
            case DataProvider.Rules.WHAT_DATA:
                where = DataProvider.Plans.TYPE + " = " + DataProvider.TYPE_DATA + " OR "
                        + DataProvider.Plans.TYPE + " = " + DataProvider.TYPE_MIXED;
                break;
            case DataProvider.Rules.WHAT_SMS:
            case DataProvider.Rules.WHAT_MMS:
                where = DataProvider.Plans.TYPE + " = " + DataProvider.TYPE_SMS + " OR "
                        + DataProvider.Plans.TYPE + " = " + DataProvider.TYPE_MMS + " OR "
                        + DataProvider.Plans.TYPE + " = " + DataProvider.TYPE_MIXED;
                break;
            default:
                where = DataProvider.Plans.WHERE_REALPLANS;
                break;
        }
        where = DbUtils.sqlAnd(where, DataProvider.Plans.MERGED_PLANS + " IS NULL");
        Log.d(TAG, "plans.where: ", where);
        return where;
    }

    @Override
    public void onUpdateValue(final android.preference.Preference p) {
        if (uri != null && values.size() > 0) {
            getContentResolver().update(uri, values, null, null);
            values.clear();
            Preferences.setDefaultPlan(this, false);
            RuleMatcher.unmatch(this);
            reload();
        }
    }

    @Override
    public void onSetDefaultValue(final Preference p, final Object value) {
        if (value instanceof String) {
            values.put(p.getKey(), (String) value);
        } else if (value instanceof Integer) {
            values.put(p.getKey(), (Integer) value);
        } else if (value instanceof Long) {
            values.put(p.getKey(), (Long) value);
        } else if (value instanceof Boolean) {
            values.put(p.getKey(), (Boolean) value);
        } else {
            throw new IllegalArgumentException("unknown type " + value);
        }
    }
}
