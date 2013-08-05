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
import android.provider.CallLog.Calls;
import android.telephony.TelephonyManager;
import android.text.InputType;
import android.text.TextUtils;

import com.actionbarsherlock.app.SherlockPreferenceActivity;

import de.ub0r.android.callmeter.R;
import de.ub0r.android.callmeter.data.DataProvider;
import de.ub0r.android.callmeter.data.RuleMatcher;
import de.ub0r.android.lib.DbUtils;
import de.ub0r.android.lib.Log;
import de.ub0r.android.lib.Utils;

/**
 * Edit a single Plan.
 * 
 * @author flx
 */
public final class RuleEdit extends SherlockPreferenceActivity implements UpdateListener {
	/** Tag for debug out. */
	private static final String TAG = "re";

	/** This rule's {@link Uri}. */
	private Uri uri = null;
	/** {@link ContentValues} holding preferences. */
	private ContentValues values = new ContentValues();

	/** Array holding {@link String}s. */
	private String[] inOutNomatterCalls = null;
	/** Array holding {@link String}s. */
	private String[] inOutNomatterSms = null;
	/** Array holding {@link String}s. */
	private String[] inOutNomatterMms = null;
	/** Array holding {@link String}s. */
	private String[] inOutNomatterData = null;
	/** Array holding {@link String}s. */
	private String[] yesNoNomatter = null;

	/**
	 * Get a string array for directions.
	 * 
	 * @param type
	 *            type of array
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
	 * @param base
	 *            base array without no_matter_
	 * @return array with no_matter_
	 */
	private String[] getStrings(final int base) {
		switch (base) {
		case R.array.direction_calls:
			if (this.inOutNomatterCalls == null) {
				final String[] tmp1 = new String[3];
				final String[] tmp2 = this.getResources().getStringArray(base);
				tmp1[0] = tmp2[0];
				tmp1[1] = tmp2[1];
				tmp1[2] = this.getString(R.string.no_matter_);
				this.inOutNomatterCalls = tmp1;
			}
			return this.inOutNomatterCalls;
		case R.array.direction_sms:
			if (this.inOutNomatterSms == null) {
				final String[] tmp1 = new String[3];
				final String[] tmp2 = this.getResources().getStringArray(base);
				tmp1[0] = tmp2[0];
				tmp1[1] = tmp2[1];
				tmp1[2] = this.getString(R.string.no_matter_);
				this.inOutNomatterSms = tmp1;
			}
			return this.inOutNomatterSms;
		case R.array.direction_mms:
			if (this.inOutNomatterMms == null) {
				final String[] tmp1 = new String[3];
				final String[] tmp2 = this.getResources().getStringArray(base);
				tmp1[0] = tmp2[0];
				tmp1[1] = tmp2[1];
				tmp1[2] = this.getString(R.string.no_matter_);
				this.inOutNomatterMms = tmp1;
			}
			return this.inOutNomatterMms;
		case R.array.direction_data:
			if (this.inOutNomatterData == null) {
				final String[] tmp1 = new String[3];
				final String[] tmp2 = this.getResources().getStringArray(base);
				tmp1[0] = tmp2[0];
				tmp1[1] = tmp2[1];
				tmp1[2] = this.getString(R.string.no_matter_);
				this.inOutNomatterData = tmp1;
			}
			return this.inOutNomatterData;
		default:
			if (this.yesNoNomatter == null) {
				final String[] tmp1 = new String[3];
				tmp1[0] = this.getString(R.string.yes);
				tmp1[1] = this.getString(R.string.no);
				tmp1[2] = this.getString(R.string.no_matter_);
				this.yesNoNomatter = tmp1;
			}
			return this.yesNoNomatter;
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Utils.setLocale(this);

		this.addPreferencesFromResource(R.xml.group_prefs);
		this.uri = this.getIntent().getData();
	}

	@Override
	protected void onResume() {
		super.onResume();

		this.values.clear();
		this.reload();
	}

	/** Check, if there is some kind of multi sim. */
	private boolean hasSimId() {
		boolean b = false;
		for (String s : new String[] { "sim_id", "simid" }) {
			try {
				Cursor c = this.getContentResolver().query(Calls.CONTENT_URI, new String[] { s },
						s + " != 0", null, null);
				b = c.getCount() > 0;
			} catch (IllegalArgumentException e) {
				// ignore any error
			}
			if (b) {
				Log.i(TAG, "multi sim phone detected: " + s);
				break;
			}
		}
		return b;
	}

	/**
	 * Reload plans from ContentProvider.
	 */
	@SuppressWarnings("deprecation")
	private void reload() {
		PreferenceScreen ps = (PreferenceScreen) this.findPreference("container");
		ps.removeAll();

		boolean hasSimId = this.hasSimId();
		Cursor c = this.getContentResolver().query(this.uri, DataProvider.Rules.PROJECTION, null,
				null, null);
		if (c.moveToFirst()) {
			// name
			CVEditTextPreference ep = new CVEditTextPreference(this, this.values,
					DataProvider.Rules.NAME, R.string.rules_new);
			ep.setTitle(R.string.name_);
			ep.setSummary(R.string.name_help);
			ep.setText(c.getString(DataProvider.Rules.INDEX_NAME));
			ep.setInputType(InputType.TYPE_CLASS_TEXT);
			ps.addPreference(ep);
			this.getSupportActionBar().setSubtitle(ep.getText());
			// active
			CVCheckBoxPreference cp = new CVCheckBoxPreference(this, this.values,
					DataProvider.Rules.ACTIVE);
			cp.setTitle(R.string.active_);
			cp.setSummary(R.string.active_help);
			cp.setChecked(c.isNull(DataProvider.Rules.INDEX_ACTIVE)
					|| c.getInt(DataProvider.Rules.INDEX_ACTIVE) == 1);
			ps.addPreference(cp);
			// what
			CVListPreference lp = new CVListPreference(this, this.values, DataProvider.Rules.WHAT);
			lp.setTitle(R.string.what_);
			lp.setSummary(R.string.what_help);
			lp.setStatic(R.array.rules_type_id, R.array.rules_type);
			int w;
			if (c.isNull(DataProvider.Rules.INDEX_WHAT)) {
				w = DataProvider.TYPE_CALL;
				this.values.put(DataProvider.Rules.WHAT, w);
			} else {
				w = c.getInt(DataProvider.Rules.INDEX_WHAT);
			}
			lp.setValue(String.valueOf(w));
			ps.addPreference(lp);
			int t = DataProvider.what2type(w);
			// plan
			lp = new CVListPreference(this, this.values, DataProvider.Rules.PLAN_ID);
			lp.setTitle(R.string.plan_);
			lp.setSummary(R.string.plan_help);
			lp.setCursor(
					this.getContentResolver().query(DataProvider.Plans.CONTENT_URI,
							DataProvider.Plans.PROJECTION_BASIC, this.getPlanWhere(w), null, null),
					DataProvider.Plans.INDEX_ID, DataProvider.Plans.INDEX_NAME);
			lp.setValue(c.getString(DataProvider.Rules.INDEX_PLAN_ID));
			ps.addPreference(lp);
			// limit reached
			cp = new CVCheckBoxPreference(this, this.values, DataProvider.Rules.LIMIT_NOT_REACHED);
			cp.setTitle(R.string.limitnotreached_);
			cp.setSummary(R.string.limitnotreached_help);
			cp.setChecked(!c.isNull(DataProvider.Rules.INDEX_LIMIT_NOT_REACHED)
					&& c.getInt(DataProvider.Rules.INDEX_LIMIT_NOT_REACHED) == 1);
			ps.addPreference(cp);
			// direction
			lp = new CVListPreference(this, this.values, DataProvider.Rules.DIRECTION);
			lp.setTitle(R.string.direction_);
			lp.setSummary(R.string.direction_help);
			lp.setStatic(
					new String[] { String.valueOf(DataProvider.DIRECTION_IN),
							String.valueOf(DataProvider.DIRECTION_OUT), "-1" },
					this.getStrings(this.getStringArray(t)));
			int i;
			if (c.isNull(DataProvider.Rules.INDEX_DIRECTION)) {
				i = -1;
				this.values.put(DataProvider.Rules.DIRECTION, i);
			} else {
				i = c.getInt(DataProvider.Rules.INDEX_DIRECTION);
			}
			lp.setValue(String.valueOf(i));
			ps.addPreference(lp);
			// my number
			TelephonyManager tm = (TelephonyManager) this.getSystemService(TELEPHONY_SERVICE);
			final String mynumber = tm.getLine1Number();
			if (hasSimId && w == DataProvider.TYPE_CALL) {
				ep = new CVEditTextPreference(this, this.values, DataProvider.Rules.MYNUMBER, null);
				ep.setTitle(R.string.my_sim_id_);
				ep.setSummary(R.string.my_sim_id_help);
				ep.setText(c.getString(DataProvider.Rules.INDEX_MYNUMBER));
				ep.setInputType(InputType.TYPE_CLASS_NUMBER);
				ps.addPreference(ep);
			} else if (!TextUtils.isEmpty(mynumber)) {
				ep = new CVEditTextPreference(this, this.values, DataProvider.Rules.MYNUMBER, null) {
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
			lp = new CVListPreference(this, this.values, DataProvider.Rules.ROAMED);
			lp.setTitle(R.string.roamed_);
			lp.setSummary(R.string.roamed_help);
			lp.setStatic(new String[] { "0", "1", String.valueOf(DataProvider.Rules.NO_MATTER) },
					this.getStrings(-1));
			if (c.isNull(DataProvider.Rules.INDEX_ROAMED)) {
				i = DataProvider.Rules.NO_MATTER;
				this.values.put(DataProvider.Rules.ROAMED, i);
			} else {
				i = c.getInt(DataProvider.Rules.INDEX_ROAMED);
			}
			lp.setValue(String.valueOf(i));
			ps.addPreference(lp);
			if (w == DataProvider.Rules.WHAT_SMS) {
				// is websms
				lp = new CVListPreference(this, this.values, DataProvider.Rules.IS_WEBSMS);
				lp.setTitle(R.string.iswebsms_);
				lp.setSummary(R.string.iswebsms_help);
				lp.setStatic(new String[] { "0", "1", "-1" }, this.getStrings(-1));
				if (c.isNull(DataProvider.Rules.INDEX_IS_WEBSMS)) {
					i = -1;
					this.values.put(DataProvider.Rules.IS_WEBSMS, i);
				} else {
					i = c.getInt(DataProvider.Rules.INDEX_IS_WEBSMS);
				}
				lp.setValue(String.valueOf(i));
				ps.addPreference(lp);
				if (i == 1) {
					// websms connector
					ep = new CVEditTextPreference(this, this.values,
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
				lp = new CVListPreference(this, this.values, DataProvider.Rules.IS_SIPCALL);
				lp.setTitle(R.string.issipcall_);
				lp.setSummary(R.string.issipcall_help);
				lp.setStatic(new String[] { "0", "1", "-1" }, this.getStrings(-1));
				if (c.isNull(DataProvider.Rules.INDEX_IS_SIPCALL)) {
					i = -1;
					this.values.put(DataProvider.Rules.IS_SIPCALL, i);
				} else {
					i = c.getInt(DataProvider.Rules.INDEX_IS_SIPCALL);
				}
				lp.setValue(String.valueOf(i));
				ps.addPreference(lp);
			}
			// include hours
			lp = new CVListPreference(this, this.values, DataProvider.Rules.INHOURS_ID, true);
			lp.setTitle(R.string.hourgroup_);
			lp.setSummary(R.string.hourgroup_help);
			lp.setCursor(
					this.getContentResolver().query(DataProvider.HoursGroup.CONTENT_URI,
							DataProvider.HoursGroup.PROJECTION, null, null, null),
					DataProvider.HoursGroup.INDEX_ID, DataProvider.HoursGroup.INDEX_NAME);
			lp.setValue(c.getString(DataProvider.Rules.INDEX_INHOURS_ID));
			ps.addPreference(lp);
			// exclude hours
			lp = new CVListPreference(this, this.values, DataProvider.Rules.EXHOURS_ID, true);
			lp.setTitle(R.string.exhourgroup_);
			lp.setSummary(R.string.exhourgroup_help);
			lp.setCursor(
					this.getContentResolver().query(DataProvider.HoursGroup.CONTENT_URI,
							DataProvider.HoursGroup.PROJECTION, null, null, null),
					DataProvider.HoursGroup.INDEX_ID, DataProvider.HoursGroup.INDEX_NAME);
			lp.setValue(c.getString(DataProvider.Rules.INDEX_EXHOURS_ID));
			ps.addPreference(lp);
			if (w != DataProvider.Rules.WHAT_DATA) {
				// include numbers
				lp = new CVListPreference(this, this.values, DataProvider.Rules.INNUMBERS_ID, true);
				lp.setTitle(R.string.numbergroup_);
				lp.setSummary(R.string.numbergroup_help);
				lp.setCursor(
						this.getContentResolver().query(DataProvider.NumbersGroup.CONTENT_URI,
								DataProvider.NumbersGroup.PROJECTION, null, null, null),
						DataProvider.NumbersGroup.INDEX_ID, DataProvider.NumbersGroup.INDEX_NAME);
				lp.setValue(c.getString(DataProvider.Rules.INDEX_INNUMBERS_ID));
				ps.addPreference(lp);
				// exclude numbers
				lp = new CVListPreference(this, this.values, DataProvider.Rules.EXNUMBERS_ID, true);
				lp.setTitle(R.string.exnumbergroup_);
				lp.setSummary(R.string.exnumbergroup_help);
				lp.setCursor(
						this.getContentResolver().query(DataProvider.NumbersGroup.CONTENT_URI,
								DataProvider.NumbersGroup.PROJECTION, null, null, null),
						DataProvider.NumbersGroup.INDEX_ID, DataProvider.NumbersGroup.INDEX_NAME);
				lp.setValue(c.getString(DataProvider.Rules.INDEX_EXNUMBERS_ID));
				ps.addPreference(lp);
			}
		}
		c.close();
		if (this.values.size() > 0) {
			this.getContentResolver().update(this.uri, this.values, null, null);
			this.values.clear();
		}
	}

	/**
	 * Set plan's and what0 value.
	 * 
	 * @param w
	 *            type
	 * @return where clause
	 */
	private String getPlanWhere(final int w) {
		String where = null;
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
		Log.d(TAG, "plans.where: " + where);
		return where;
	}

	@Override
	public void onUpdateValue(final android.preference.Preference p) {
		if (this.uri != null && this.values.size() > 0) {
			this.getContentResolver().update(this.uri, this.values, null, null);
			this.values.clear();
			Preferences.setDefaultPlan(this, false);
			RuleMatcher.unmatch(this);
			this.reload();
		}
	}

	@Override
	public void onSetDefaultValue(final Preference p, final Object value) {
		if (value instanceof String) {
			this.values.put(p.getKey(), (String) value);
		} else if (value instanceof Integer) {
			this.values.put(p.getKey(), (Integer) value);
		} else if (value instanceof Long) {
			this.values.put(p.getKey(), (Long) value);
		} else if (value instanceof Boolean) {
			this.values.put(p.getKey(), (Boolean) value);
		} else {
			throw new IllegalArgumentException("unknown type " + value);
		}
	}
}
