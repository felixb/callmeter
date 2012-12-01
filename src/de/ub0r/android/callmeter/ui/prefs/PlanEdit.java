/*
 * Copyright (C) 2009-2012 Felix Bechstein
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

import java.util.Calendar;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.InputType;

import com.actionbarsherlock.app.SherlockPreferenceActivity;

import de.ub0r.android.callmeter.R;
import de.ub0r.android.callmeter.data.DataProvider;
import de.ub0r.android.callmeter.data.RuleMatcher;
import de.ub0r.android.lib.Log;
import de.ub0r.android.lib.Utils;

/**
 * Edit a single Plan.
 * 
 * @author flx
 */
public final class PlanEdit extends SherlockPreferenceActivity implements UpdateListener {
	/** Tag for debug out. */
	private static final String TAG = "pe";

	/** This rule's {@link Uri}. */
	private Uri uri = null;
	/** Id of edited filed. */
	private int pid = -1;
	/** {@link ContentValues} holding preferences. */
	private ContentValues values = new ContentValues();

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Utils.setLocale(this);

		this.addPreferencesFromResource(R.xml.group_prefs);
		this.uri = this.getIntent().getData();
		this.pid = (int) ContentUris.parseId(this.uri);
	}

	@Override
	protected void onResume() {
		super.onResume();
		Utils.setLocale(this);

		this.reload();
	}

	/**
	 * Reload plans from ContentProvider.
	 */
	@SuppressWarnings("deprecation")
	private void reload() {
		PreferenceScreen ps = (PreferenceScreen) this.findPreference("container");
		ps.removeAll();

		Cursor c = this.getContentResolver().query(this.uri, DataProvider.Plans.PROJECTION, null,
				null, null);
		if (c.moveToFirst()) {
			int t;
			if (c.isNull(DataProvider.Plans.INDEX_TYPE)) {
				t = DataProvider.TYPE_CALL;
				this.values.put(DataProvider.Plans.TYPE, t);
			} else {
				t = c.getInt(DataProvider.Plans.INDEX_TYPE);
			}
			int lt;
			if (c.isNull(DataProvider.Plans.INDEX_LIMIT_TYPE)) {
				lt = DataProvider.LIMIT_TYPE_NONE;
				this.values.put(DataProvider.Plans.LIMIT_TYPE, lt);
			} else {
				lt = c.getInt(DataProvider.Plans.INDEX_LIMIT_TYPE);
			}
			int ppid = DataProvider.Plans.getParent(this.getContentResolver(), this.pid);
			String merged = c.getString(DataProvider.Plans.INDEX_MERGED_PLANS);
			if (merged != null && merged.replaceAll(",", "").length() == 0) {
				merged = null;
			}

			SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(this);
			boolean advanced = p.getBoolean(Preferences.PREFS_ADVANCED, false);
			boolean prepaid = p.getBoolean(Preferences.PREFS_PREPAID, false);
			// name
			CVEditTextPreference ep = new CVEditTextPreference(this, this.values,
					DataProvider.Plans.NAME, R.string.plans_new);
			ep.setTitle(R.string.name_);
			ep.setSummary(R.string.name_help);
			ep.setText(c.getString(DataProvider.Plans.INDEX_NAME));
			ep.setInputType(InputType.TYPE_CLASS_TEXT);
			ps.addPreference(ep);
			this.getSupportActionBar().setSubtitle(ep.getText());
			if (t != DataProvider.TYPE_SPACING && t != DataProvider.TYPE_TITLE) {
				// short name
				ep = new CVEditTextPreference(this, this.values, DataProvider.Plans.SHORTNAME, this
						.getString(R.string.plans_new).replaceAll(" ", ""));
				ep.setTitle(R.string.shortname_);
				ep.setSummary(R.string.shortname_help);
				ep.setText(c.getString(DataProvider.Plans.INDEX_SHORTNAME));
				ep.setInputType(InputType.TYPE_CLASS_TEXT);
				ps.addPreference(ep);
			}
			// type
			if (advanced) {
				CVListPreference lp = new CVListPreference(this, this.values,
						DataProvider.Plans.TYPE);
				lp.setTitle(R.string.type_);
				lp.setSummary(R.string.type_help);
				lp.setStatic(R.array.plans_type_id, R.array.plans_type);
				lp.setValue(String.valueOf(t));
				ps.addPreference(lp);
			}
			if (t == DataProvider.TYPE_BILLPERIOD) {
				// bill period
				int bpl;
				if (c.isNull(DataProvider.Plans.INDEX_BILLPERIOD)) {
					bpl = DataProvider.BILLPERIOD_1MONTH;
					this.values.put(DataProvider.Plans.BILLPERIOD, bpl);
				} else {
					bpl = c.getInt(DataProvider.Plans.INDEX_BILLPERIOD);
				}
				if (!prepaid) {
					CVListPreference lp = new CVListPreference(this, this.values,
							DataProvider.Plans.BILLPERIOD);
					lp.setTitle(R.string.billperiod_);
					lp.setSummary(R.string.billperiod_help);
					lp.setStatic(R.array.billperiod_id, R.array.billperiod);
					lp.setValue(String.valueOf(bpl));
					ps.addPreference(lp);
				} else {
					// set bill period to infinite
					this.values
							.put(DataProvider.Plans.BILLPERIOD, DataProvider.BILLPERIOD_INFINITE);
				}
				if (bpl != DataProvider.BILLPERIOD_DAY) {
					// bill day
					CVDatePreference dp = new CVDatePreference(this, this.values,
							DataProvider.Plans.BILLDAY, true);
					dp.setTitle(R.string.billday_);
					dp.setSummary(R.string.billday_help);
					Calendar cal = Calendar.getInstance();
					if (c.isNull(DataProvider.Plans.INDEX_BILLDAY)) {
						cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), 1, 0, 1, 0);
						this.values.put(DataProvider.Plans.BILLDAY, cal.getTimeInMillis());
					} else {
						cal.setTimeInMillis(c.getLong(DataProvider.Plans.INDEX_BILLDAY));
						if (bpl != DataProvider.BILLPERIOD_INFINITE) {
							cal = DataProvider.Plans.getBillDay(bpl, cal, null, false);
						}
					}
					dp.setValue(cal);
					ps.addPreference(dp);
				}
			} else if (t != DataProvider.TYPE_SPACING && t != DataProvider.TYPE_TITLE) {
				// bill period id
				CVListPreference lp = new CVListPreference(this, this.values,
						DataProvider.Plans.BILLPERIOD_ID);
				lp.setTitle(R.string.billperiodid_);
				lp.setSummary(R.string.billperiodid_help);
				lp.setCursor(
						this.getContentResolver().query(DataProvider.Plans.CONTENT_URI,
								DataProvider.Plans.PROJECTION_BASIC,
								DataProvider.Plans.WHERE_BILLPERIODS, null, null),
						DataProvider.Plans.INDEX_ID, DataProvider.Plans.INDEX_NAME);
				int i;
				if (c.isNull(DataProvider.Plans.INDEX_BILLPERIOD_ID)) {
					i = -1;
					this.values.put(DataProvider.Plans.BILLPERIOD_ID, i);
				} else {
					i = c.getInt(DataProvider.Plans.INDEX_BILLPERIOD_ID);
				}
				lp.setValue(String.valueOf(i));
				ps.addPreference(lp);
				if (ppid < 0L) {
					// merge plans
					lp = new CVListPreference(this, this.values, DataProvider.Plans.MERGED_PLANS,
							true);
					lp.setTitle(R.string.merge_plans_);
					lp.setSummary(R.string.merge_plans_help);
					lp.setCursor(
							this.getContentResolver().query(DataProvider.Plans.CONTENT_URI,
									DataProvider.Plans.PROJECTION_BASIC,
									this.getMergePlansWhere(t), null, null),
							DataProvider.Plans.INDEX_ID, DataProvider.Plans.INDEX_NAME);
					lp.setValue(merged);
					ps.addPreference(lp);
				}
				// limit type
				lp = new CVListPreference(this, this.values, DataProvider.Plans.LIMIT_TYPE);
				lp.setTitle(R.string.limit_type_);
				lp.setSummary(R.string.limit_type_help);
				lp.setStatic(R.array.limit_type_id, R.array.limit_type);
				lp.setValue(String.valueOf(lt));
				ps.addPreference(lp);
				if (lt != DataProvider.LIMIT_TYPE_NONE) {
					// limit
					ep = new CVEditTextPreference(this, this.values, DataProvider.Plans.LIMIT, "");
					ep.setTitle(R.string.limit_);
					ep.setSummary(R.string.limit_help);
					ep.setHint(this.getLimitHint(t, lt));
					ep.setText(c.getString(DataProvider.Plans.INDEX_LIMIT));
					ep.setInputType(InputType.TYPE_CLASS_NUMBER
							| InputType.TYPE_NUMBER_FLAG_DECIMAL);
					ps.addPreference(ep);
				}
				if (merged == null && (t == DataProvider.TYPE_CALL || t == DataProvider.TYPE_MIXED)) {
					// bill mode
					CVBillModePreference bp = new CVBillModePreference(this, this.values,
							DataProvider.Plans.BILLMODE);
					bp.setTitle(R.string.billmode_);
					bp.setSummary(R.string.billmode_help);
					bp.setValue(c.getString(DataProvider.Plans.INDEX_BILLMODE));
					ps.addPreference(bp);
					if (advanced) {
						// strip first seconds
						ep = new CVEditTextPreference(this, this.values,
								DataProvider.Plans.STRIP_SECONDS, "0");
						ep.setTitle(R.string.strip_seconds_);
						ep.setSummary(R.string.strip_seconds_help);
						ep.setText(c.getString(DataProvider.Plans.INDEX_STRIP_SECONDS));
						ep.setInputType(InputType.TYPE_CLASS_NUMBER);
						ps.addPreference(ep);
						// strip anything but first seconds
						ep = new CVEditTextPreference(this, this.values,
								DataProvider.Plans.STRIP_PAST, "0");
						ep.setTitle(R.string.strip_past_);
						ep.setSummary(R.string.strip_past_help);
						ep.setText(c.getString(DataProvider.Plans.INDEX_STRIP_PAST));
						ep.setInputType(InputType.TYPE_CLASS_NUMBER);
						ps.addPreference(ep);
					}
				}
			}
			if (t == DataProvider.TYPE_BILLPERIOD && prepaid) {
				// balance
				ep = new CVEditTextPreference(this, this.values, DataProvider.Plans.COST_PER_PLAN,
						"");
				ep.setTitle(R.string.balance_);
				ep.setSummary(R.string.balance_help);
				ep.setText(c.getString(DataProvider.Plans.INDEX_COST_PER_PLAN));
				ep.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
				ps.addPreference(ep);
			} else if (t != DataProvider.TYPE_SPACING && t != DataProvider.TYPE_TITLE) {
				// cost per plan
				ep = new CVEditTextPreference(this, this.values, DataProvider.Plans.COST_PER_PLAN,
						"");
				ep.setTitle(R.string.cost_per_plan_);
				ep.setSummary(R.string.cost_per_plan_help);
				ep.setText(c.getString(DataProvider.Plans.INDEX_COST_PER_PLAN));
				ep.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
				ps.addPreference(ep);
			}
			if (merged == null
					&& (t == DataProvider.TYPE_SMS || t == DataProvider.TYPE_MMS
							|| t == DataProvider.TYPE_CALL || t == DataProvider.TYPE_MIXED)) {
				if (lt != DataProvider.LIMIT_TYPE_NONE || ppid > 0L) {
					// cost per item in limit
					ep = new CVEditTextPreference(this, this.values,
							DataProvider.Plans.COST_PER_ITEM_IN_LIMIT, "");
					ep.setTitle(R.string.cost_per_item_in_limit_);
					ep.setSummary(R.string.cost_per_item_in_limit_help);
					ep.setText(c.getString(DataProvider.Plans.INDEX_COST_PER_ITEM_IN_LIMIT));
					ep.setHint(this.getCostPerItemHint(t));
					ep.setInputType(InputType.TYPE_CLASS_NUMBER
							| InputType.TYPE_NUMBER_FLAG_DECIMAL);
					ps.addPreference(ep);
				}
				// cost per item
				ep = new CVEditTextPreference(this, this.values, DataProvider.Plans.COST_PER_ITEM,
						"");
				ep.setTitle(R.string.cost_per_item_);
				if (lt == DataProvider.LIMIT_TYPE_NONE) {
					ep.setSummary(R.string.cost_per_item_no_limit_help);
				} else {
					ep.setSummary(R.string.cost_per_item_help);
				}
				ep.setText(c.getString(DataProvider.Plans.INDEX_COST_PER_ITEM));
				ep.setHint(this.getCostPerItemHint(t));
				ep.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
				ps.addPreference(ep);
			}
			if (t == DataProvider.TYPE_MIXED) {
				// mixed: units/minute
				ep = new CVEditTextPreference(this, this.values,
						DataProvider.Plans.MIXED_UNITS_CALL, "");
				ep.setTitle(R.string.mixed_units_call_);
				ep.setSummary(R.string.mixed_units_call_help);
				ep.setText(c.getString(DataProvider.Plans.INDEX_MIXED_UNITS_CALL));
				ep.setInputType(InputType.TYPE_CLASS_NUMBER);
				ps.addPreference(ep);
				// mixed: units/sms
				ep = new CVEditTextPreference(this, this.values,
						DataProvider.Plans.MIXED_UNITS_SMS, "");
				ep.setTitle(R.string.mixed_units_sms_);
				ep.setSummary(R.string.mixed_units_sms_help);
				ep.setText(c.getString(DataProvider.Plans.INDEX_MIXED_UNITS_SMS));
				ep.setInputType(InputType.TYPE_CLASS_NUMBER);
				ps.addPreference(ep);
				// mixed: units/mms
				ep = new CVEditTextPreference(this, this.values,
						DataProvider.Plans.MIXED_UNITS_MMS, "");
				ep.setTitle(R.string.mixed_units_mms_);
				ep.setSummary(R.string.mixed_units_mms_help);
				ep.setText(c.getString(DataProvider.Plans.INDEX_MIXED_UNITS_MMS));
				ep.setInputType(InputType.TYPE_CLASS_NUMBER);
				ps.addPreference(ep);
				// mixed: units/data
				ep = new CVEditTextPreference(this, this.values,
						DataProvider.Plans.MIXED_UNITS_DATA, "");
				ep.setTitle(R.string.mixed_units_data_);
				ep.setSummary(R.string.mixed_units_data_help);
				ep.setText(c.getString(DataProvider.Plans.INDEX_MIXED_UNITS_DATA));
				ep.setInputType(InputType.TYPE_CLASS_NUMBER);
				ps.addPreference(ep);
			}
			if (merged == null && (t == DataProvider.TYPE_CALL || t == DataProvider.TYPE_DATA)) {
				if (lt != DataProvider.LIMIT_TYPE_NONE || ppid > 0L) {
					// cost per amount in limit
					CV2EditTextPreference ep2 = new CV2EditTextPreference(this, this.values,
							DataProvider.Plans.COST_PER_AMOUNT_IN_LIMIT1,
							DataProvider.Plans.COST_PER_AMOUNT_IN_LIMIT2,
							t != DataProvider.TYPE_CALL || !advanced, "", "");
					ep2.setTitle(R.string.cost_per_amount_in_limit_);
					if (t == DataProvider.TYPE_CALL && advanced) {
						ep2.setSummary(R.string.cost_per_amount_in_limit_help2);
					} else {
						ep2.setSummary(R.string.cost_per_amount_in_limit_help1);
					}
					ep2.setHint(this.getCostPerAmountHint(t));
					ep2.setText(c.getString(DataProvider.Plans.INDEX_COST_PER_AMOUNT_IN_LIMIT1),
							c.getString(DataProvider.Plans.INDEX_COST_PER_AMOUNT_IN_LIMIT2));
					ep2.setInputType(InputType.TYPE_CLASS_NUMBER
							| InputType.TYPE_NUMBER_FLAG_DECIMAL);
					ps.addPreference(ep2);
				}
				// cost per amount
				CV2EditTextPreference ep2 = new CV2EditTextPreference(this, this.values,
						DataProvider.Plans.COST_PER_AMOUNT1, DataProvider.Plans.COST_PER_AMOUNT2,
						t != DataProvider.TYPE_CALL || !advanced, "", "");
				ep2.setTitle(R.string.cost_per_amount_);
				if (lt == DataProvider.LIMIT_TYPE_NONE) {
					if (t == DataProvider.TYPE_CALL && advanced) {
						ep2.setSummary(R.string.cost_per_amount_no_limit_help2);
					} else {
						ep2.setSummary(R.string.cost_per_amount_no_limit_help1);
					}
				} else {
					if (t == DataProvider.TYPE_CALL && advanced) {
						ep2.setSummary(R.string.cost_per_amount_help2);
					} else {
						ep2.setSummary(R.string.cost_per_amount_help1);
					}
				}
				ep2.setHint(this.getCostPerAmountHint(t));
				ep2.setText(c.getString(DataProvider.Plans.INDEX_COST_PER_AMOUNT1),
						c.getString(DataProvider.Plans.INDEX_COST_PER_AMOUNT2));
				ep2.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
				ps.addPreference(ep2);
			}
		}
		c.close();
		if (this.values.size() > 0) {
			this.getContentResolver().update(this.uri, this.values, null, null);
			this.values.clear();
		}
	}

	/**
	 * Get selection for merged plans field.
	 * 
	 * @param t
	 *            plan type
	 * @return where clause
	 */
	private String getMergePlansWhere(final int t) {
		String where;
		if (t == DataProvider.TYPE_MIXED) {
			where = "(" + DataProvider.Plans.TYPE + " in (" + DataProvider.TYPE_CALL + ","
					+ DataProvider.TYPE_SMS + "," + DataProvider.TYPE_MMS + ","
					+ DataProvider.TYPE_DATA + "))";
		} else {
			where = DataProvider.Plans.TYPE + " = " + t;
		}
		where += " AND " + DataProvider.Plans.ID + " != " + this.pid + " AND "
				+ DataProvider.Plans.MERGED_PLANS + " IS NULL";
		Log.d(TAG, "selection: " + where);
		return where;
	}

	/**
	 * Get hint for LIMIT preference.
	 * 
	 * @param t
	 *            type
	 * @param lt
	 *            limit type
	 * @return res id
	 */
	private int getLimitHint(final int t, final int lt) {
		switch (lt) {
		case DataProvider.LIMIT_TYPE_COST:
			return R.string.units_cost;
		case DataProvider.LIMIT_TYPE_UNITS:
			switch (t) {
			case DataProvider.TYPE_CALL:
				return R.string.units_minutes;
			case DataProvider.TYPE_DATA:
				return R.string.units_mbyte;
			case DataProvider.TYPE_MIXED:
				return R.string.units_units;
			case DataProvider.TYPE_MMS:
			case DataProvider.TYPE_SMS:
				return R.string.units_num_msg;
			default:
				return -1;
			}
		default:
			return -1;
		}
	}

	/**
	 * Get hint for COST_PER_ITEM preference.
	 * 
	 * @param t
	 *            type
	 * @return res id
	 */
	private int getCostPerItemHint(final int t) {
		switch (t) {
		case DataProvider.TYPE_CALL:
			return R.string.units_cost_per_call;
		case DataProvider.TYPE_MIXED:
			return R.string.units_cost_per_unit;
		case DataProvider.TYPE_MMS:
		case DataProvider.TYPE_SMS:
			return R.string.units_cost_per_message;
		default:
			return -1;
		}
	}

	/**
	 * Get hint for COST_PER_AMOUNT1 preference.
	 * 
	 * @param t
	 *            type
	 * @return res id
	 */
	private int getCostPerAmountHint(final int t) {
		switch (t) {
		case DataProvider.TYPE_CALL:
			return R.string.units_cost_per_minute;
		case DataProvider.TYPE_DATA:
			return R.string.units_cost_per_mbyte;
		default:
			return -1;
		}
	}

	@Override
	public void onUpdateValue(final android.preference.Preference p) {
		int l = this.values.size();
		if (this.uri != null && l > 0) {
			boolean badkey = !this.values.containsKey(DataProvider.Plans.NAME);
			badkey &= !this.values.containsKey(DataProvider.Plans.SHORTNAME);
			boolean needUnmatch = l > 1 || badkey;
			badkey &= !this.values.containsKey(DataProvider.Plans.LIMIT);
			badkey &= !this.values.containsKey(DataProvider.Plans.COST_PER_ITEM);
			badkey &= !this.values.containsKey(DataProvider.Plans.COST_PER_AMOUNT1);
			boolean nonDefault = l > 1 || badkey;
			this.getContentResolver().update(this.uri, this.values, null, null);
			this.values.clear();
			if (nonDefault) {
				Preferences.setDefaultPlan(this, false);
			}
			if (needUnmatch) {
				RuleMatcher.unmatch(this);
			}
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
