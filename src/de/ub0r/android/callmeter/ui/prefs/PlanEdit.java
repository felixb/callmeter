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

import android.app.ListActivity;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import de.ub0r.android.callmeter.R;
import de.ub0r.android.callmeter.data.DataProvider;
import de.ub0r.android.callmeter.data.RuleMatcher;
import de.ub0r.android.callmeter.ui.prefs.Preference.BillmodePreference;
import de.ub0r.android.callmeter.ui.prefs.Preference.CursorPreference;
import de.ub0r.android.callmeter.ui.prefs.Preference.DatePreference;
import de.ub0r.android.callmeter.ui.prefs.Preference.ListPreference;
import de.ub0r.android.callmeter.ui.prefs.Preference.Text2Preference;
import de.ub0r.android.callmeter.ui.prefs.Preference.TextPreference;
import de.ub0r.android.lib.Log;
import de.ub0r.android.lib.Utils;

/**
 * Edit a single Plan.
 * 
 * @author flx
 */
public class PlanEdit extends ListActivity implements OnClickListener, OnItemClickListener,
		OnDismissListener {
	/** Tag for debug out. */
	private static final String TAG = "pe";

	/** {@link PreferenceAdapter}. */
	private PreferenceAdapter adapter = null;

	/** Id of edited filed. */
	private long pid = -1L;
	/** Plan's type. */
	private int ptype = -1;

	/** Show advanced settings. */
	private boolean showAdvanced = false;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Utils.setLocale(this);
		this.setTitle(this.getString(R.string.settings) + " > " + this.getString(R.string.plans)
				+ " > " + this.getString(R.string.edit_));
		this.setContentView(R.layout.list_ok_cancel);

		this.getListView().setOnItemClickListener(this);
		this.findViewById(R.id.ok).setOnClickListener(this);
		this.findViewById(R.id.cancel).setOnClickListener(this);

		this.fillFields();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void onNewIntent(final Intent intent) {
		super.onNewIntent(intent);
		this.fillFields();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void onResume() {
		super.onResume();
		Utils.setLocale(this);
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		this.showAdvanced = prefs.getBoolean(Preferences.PREFS_ADVANCED, false);
		this.showHideFileds();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void onPause() {
		super.onPause();
	}

	/**
	 * Get a new {@link PreferenceAdapter} for this {@link ListActivity}.
	 * 
	 * @return {@link PreferenceAdapter}
	 */
	private PreferenceAdapter getAdapter() {
		final PreferenceAdapter ret = new PreferenceAdapter(this);
		ret.add(new TextPreference(this, DataProvider.Plans.NAME, this
				.getString(R.string.plans_new), R.string.name_, R.string.name_help,
				InputType.TYPE_CLASS_TEXT));
		ret.add(new TextPreference(this, DataProvider.Plans.SHORTNAME, this.getString(
				R.string.plans_new).replaceAll(" ", ""), R.string.shortname_,
				R.string.shortname_help, InputType.TYPE_CLASS_TEXT));
		ret.add(new ListPreference(this, DataProvider.Plans.TYPE, DataProvider.TYPE_CALL,
				R.string.type_, R.string.type_help, R.array.plans_type));
		ret.add(new ListPreference(this, DataProvider.Plans.BILLPERIOD,
				DataProvider.BILLPERIOD_1MONTH, R.string.billperiod_, R.string.billperiod_help,
				R.array.billperiod));
		ret.add(new DatePreference(this, DataProvider.Plans.BILLDAY, R.string.billday_,
				R.string.billday_help, true));
		ret.add(new CursorPreference(this, DataProvider.Plans.BILLPERIOD_ID,
				R.string.billperiodid_, R.string.billperiodid_help, -1, -1, -1,
				DataProvider.Plans.CONTENT_URI, DataProvider.Plans.ID, DataProvider.Plans.NAME,
				DataProvider.Plans.TYPE + " == " + DataProvider.TYPE_BILLPERIOD, false, null, null,
				null));
		ret.add(new CursorPreference(this, DataProvider.Plans.MERGED_PLANS, R.string.merge_plans_,
				R.string.merge_plans_help, -1, -1, -1, DataProvider.Plans.CONTENT_URI,
				DataProvider.Plans.ID, DataProvider.Plans.NAME, null, true, null, null, null));
		ret.add(new ListPreference(this, DataProvider.Plans.LIMIT_TYPE,
				DataProvider.LIMIT_TYPE_NONE, R.string.limit_type_, R.string.limit_type_help,
				R.array.limit_type));
		ret.add(new TextPreference(this, DataProvider.Plans.LIMIT, "0", R.string.limit_,
				R.string.limit_help, InputType.TYPE_CLASS_NUMBER
						| InputType.TYPE_NUMBER_FLAG_DECIMAL));
		ret.add(new BillmodePreference(this, DataProvider.Plans.BILLMODE, R.string.billmode_,
				R.string.billmode_help));
		ret.add(new TextPreference(this, DataProvider.Plans.STRIP_SECONDS, "0",
				R.string.strip_seconds_, R.string.strip_seconds_help, InputType.TYPE_CLASS_NUMBER));
		int t, th;
		if (this.ptype == DataProvider.TYPE_BILLPERIOD
				&& PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
						Preferences.PREFS_PREPAID, false)) {
			t = R.string.balance_;
			th = R.string.balance_help;
		} else {
			t = R.string.cost_per_plan_;
			th = R.string.cost_per_plan_help;
		}
		ret.add(new TextPreference(this, DataProvider.Plans.COST_PER_PLAN, "0", t, th,
				InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL));
		ret.add(new TextPreference(this, DataProvider.Plans.COST_PER_ITEM_IN_LIMIT, "0",
				R.string.cost_per_item_in_limit_, R.string.cost_per_item_in_limit_help,
				InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL));
		ret.add(new TextPreference(this, DataProvider.Plans.COST_PER_ITEM, "0",
				R.string.cost_per_item_, R.string.cost_per_item_help, InputType.TYPE_CLASS_NUMBER
						| InputType.TYPE_NUMBER_FLAG_DECIMAL));
		ret.add(new TextPreference(this, DataProvider.Plans.MIXED_UNITS_CALL, "0",
				R.string.mixed_units_call_, R.string.mixed_units_call_help,
				InputType.TYPE_CLASS_NUMBER));
		ret.add(new TextPreference(this, DataProvider.Plans.MIXED_UNITS_SMS, "0",
				R.string.mixed_units_sms_, R.string.mixed_units_sms_help,
				InputType.TYPE_CLASS_NUMBER));
		ret.add(new TextPreference(this, DataProvider.Plans.MIXED_UNITS_MMS, "0",
				R.string.mixed_units_mms_, R.string.mixed_units_mms_help,
				InputType.TYPE_CLASS_NUMBER));
		ret.add(new Text2Preference(this, DataProvider.Plans.COST_PER_AMOUNT_IN_LIMIT1,
				DataProvider.Plans.COST_PER_AMOUNT_IN_LIMIT2, "0", "0",
				R.string.cost_per_amount_in_limit_, R.string.cost_per_amount_in_limit_help1,
				R.string.cost_per_amount_in_limit_help2, InputType.TYPE_CLASS_NUMBER
						| InputType.TYPE_NUMBER_FLAG_DECIMAL));
		ret.add(new Text2Preference(this, DataProvider.Plans.COST_PER_AMOUNT1,
				DataProvider.Plans.COST_PER_AMOUNT2, "0", "0", R.string.cost_per_amount_,
				R.string.cost_per_amount_help1, R.string.cost_per_amount_help2,
				InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL));

		((TextPreference) ret.getPreference(DataProvider.Plans.COST_PER_PLAN))
				.setHint(R.string.units_cost);
		((TextPreference) ret.getPreference(DataProvider.Plans.MIXED_UNITS_CALL))
				.setHint(R.string.units_units_per_minute);
		((TextPreference) ret.getPreference(DataProvider.Plans.MIXED_UNITS_MMS))
				.setHint(R.string.units_units_per_message);
		((TextPreference) ret.getPreference(DataProvider.Plans.MIXED_UNITS_SMS))
				.setHint(R.string.units_units_per_message);
		return ret;
	}

	/**
	 * Set selection for merged plans field.
	 */
	private void setMergePlansSelection() {
		final PreferenceAdapter a = this.adapter;
		if (a == null) {
			return;
		}

		final int t = ((ListPreference) a.getPreference(DataProvider.Plans.TYPE)).getValue();
		String sel;
		if (t == DataProvider.TYPE_MIXED) {
			sel = "(" + DataProvider.Plans.TYPE + " = " + DataProvider.TYPE_CALL + " OR "
					+ DataProvider.Plans.TYPE + " = " + DataProvider.TYPE_SMS + " OR "
					+ DataProvider.Plans.TYPE + " = " + DataProvider.TYPE_MMS + ")";
		} else {
			sel = DataProvider.Plans.TYPE + " = " + t;
		}
		sel += " AND " + DataProvider.Plans.ID + " != " + this.pid + " AND "
				+ DataProvider.Plans.MERGED_PLANS + " IS NULL";
		Log.d(TAG, "selection: " + sel);
		((CursorPreference) a.getPreference(DataProvider.Plans.MERGED_PLANS)).setCursor(sel);
	}

	/**
	 * Fill the fields with data from the cursor.
	 */
	private void fillFields() {
		final Uri uri = this.getIntent().getData();
		int nid = -1;
		Cursor cursor = null;
		if (uri != null) {
			cursor = this.getContentResolver().query(uri, DataProvider.Plans.PROJECTION, null,
					null, null);
			if (cursor == null || !cursor.moveToFirst()) {
				cursor = null;
				this.pid = -1;
			}
		}

		if (cursor != null) {
			nid = cursor.getInt(DataProvider.Plans.INDEX_ID);
		}
		if (this.pid == -1 || nid != this.pid) {
			this.pid = nid;
			if (cursor != null) {
				cursor.getInt(DataProvider.Plans.INDEX_TYPE);
			} else {
				this.ptype = -1;
			}
			this.adapter = this.getAdapter();
			this.setListAdapter(this.adapter);
		}
		if (cursor != null && !cursor.isClosed()) {
			this.adapter.load(cursor);
			cursor.close();
		}
		final PreferenceAdapter a = this.adapter;
		if (this.ptype == DataProvider.TYPE_BILLPERIOD) {
			final int bp = ((ListPreference) a.getPreference(DataProvider.Plans.BILLPERIOD))
					.getValue();
			if (bp != DataProvider.BILLPERIOD_INFINITE) {
				final DatePreference dp = (DatePreference) a
						.getPreference(DataProvider.Plans.BILLDAY);
				Calendar bd = dp.getValue();
				bd = DataProvider.Plans.getBillDay(bp, bd, null, false);
				if (bd != null) {
					dp.setValue(bd);
				}
			}
		}
	}

	/**
	 * Show or hide fields based on data in there.
	 * 
	 * @param t
	 *            type of plan
	 */
	private void showHideExtraFileds(final int t) {
		final PreferenceAdapter a = this.adapter;
		if (a == null) {
			return;
		}

		if (t == DataProvider.TYPE_BILLPERIOD) {
			final ListPreference p = (ListPreference) a
					.getPreference(DataProvider.Plans.BILLPERIOD);
			final int i = p.getValue();
			a.hide(DataProvider.Plans.BILLDAY, i == DataProvider.BILLPERIOD_DAY);
		} else if (t != DataProvider.TYPE_SPACING && t != DataProvider.TYPE_TITLE) {
			final long ppid = DataProvider.Plans.getParent(this.getContentResolver(), this.pid);
			final ListPreference p = (ListPreference) a
					.getPreference(DataProvider.Plans.LIMIT_TYPE);
			final int lt = p.getValue();
			final boolean nolimit = lt == DataProvider.LIMIT_TYPE_NONE;
			a.hide(DataProvider.Plans.LIMIT, nolimit);
			if (nolimit && ppid < 0L) {
				a.hide(DataProvider.Plans.COST_PER_AMOUNT_IN_LIMIT1, true);
				a.hide(DataProvider.Plans.COST_PER_ITEM_IN_LIMIT, true);
			}
			final String mergedPlans = ((CursorPreference) a
					.getPreference(DataProvider.Plans.MERGED_PLANS)).getMultiValue();
			if (mergedPlans != null && mergedPlans.length() > 0) {
				a.hide(DataProvider.Plans.COST_PER_AMOUNT_IN_LIMIT1, true);
				a.hide(DataProvider.Plans.COST_PER_ITEM_IN_LIMIT, true);
				a.hide(DataProvider.Plans.COST_PER_ITEM, true);
				a.hide(DataProvider.Plans.COST_PER_AMOUNT1, true);
				a.hide(DataProvider.Plans.BILLMODE, true);
				a.hide(DataProvider.Plans.STRIP_SECONDS, true);
			}
			if (ppid >= 0) {
				a.hide(DataProvider.Plans.MERGED_PLANS, true);
			}

			final Text2Preference pil = (Text2Preference) a
					.getPreference(DataProvider.Plans.COST_PER_AMOUNT_IN_LIMIT1);
			final Text2Preference pol = (Text2Preference) a
					.getPreference(DataProvider.Plans.COST_PER_AMOUNT1);
			pil.setSingleMode(t != DataProvider.TYPE_CALL);
			pol.setSingleMode(t != DataProvider.TYPE_CALL);
		}
	}

	/**
	 * Show or hide fields based on data in there.
	 */
	private void showHideFileds() {
		final PreferenceAdapter a = this.adapter;
		a.hide(DataProvider.Plans.TYPE, !this.showAdvanced);
		final int t = ((ListPreference) a.getPreference(DataProvider.Plans.TYPE)).getValue();
		switch (t) {
		case DataProvider.TYPE_SPACING:
			a.hide(DataProvider.Plans.SHORTNAME, true);
			a.hide(DataProvider.Plans.BILLDAY, true);
			a.hide(DataProvider.Plans.BILLMODE, true);
			a.hide(DataProvider.Plans.STRIP_SECONDS, true);
			a.hide(DataProvider.Plans.BILLPERIOD, true);
			a.hide(DataProvider.Plans.BILLPERIOD_ID, true);
			a.hide(DataProvider.Plans.COST_PER_AMOUNT1, true);
			a.hide(DataProvider.Plans.COST_PER_AMOUNT_IN_LIMIT1, true);
			a.hide(DataProvider.Plans.COST_PER_ITEM, true);
			a.hide(DataProvider.Plans.COST_PER_ITEM_IN_LIMIT, true);
			a.hide(DataProvider.Plans.COST_PER_PLAN, true);
			a.hide(DataProvider.Plans.LIMIT_TYPE, true);
			a.hide(DataProvider.Plans.MIXED_UNITS_CALL, true);
			a.hide(DataProvider.Plans.MIXED_UNITS_SMS, true);
			a.hide(DataProvider.Plans.MIXED_UNITS_MMS, true);
			a.hide(DataProvider.Plans.LIMIT, true);
			a.hide(DataProvider.Plans.MERGED_PLANS, true);
			break;
		case DataProvider.TYPE_BILLPERIOD:
			final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(this);
			final boolean prepaid = p.getBoolean(Preferences.PREFS_PREPAID, false);
			a.hide(DataProvider.Plans.BILLPERIOD, prepaid);
			if (prepaid) {
				((ListPreference) a.getPreference(DataProvider.Plans.BILLPERIOD))
						.setValue(DataProvider.BILLPERIOD_INFINITE);
			}
			// TODO: set billperiod on enabling prepaid plans?
			a.hide(DataProvider.Plans.COST_PER_PLAN, false);
			a.hide(DataProvider.Plans.BILLMODE, true);
			a.hide(DataProvider.Plans.STRIP_SECONDS, true);
			a.hide(DataProvider.Plans.BILLPERIOD_ID, true);
			a.hide(DataProvider.Plans.COST_PER_AMOUNT1, true);
			a.hide(DataProvider.Plans.COST_PER_AMOUNT_IN_LIMIT1, true);
			a.hide(DataProvider.Plans.COST_PER_ITEM, true);
			a.hide(DataProvider.Plans.COST_PER_ITEM_IN_LIMIT, true);
			a.hide(DataProvider.Plans.LIMIT_TYPE, true);
			a.hide(DataProvider.Plans.LIMIT, true);
			a.hide(DataProvider.Plans.MIXED_UNITS_CALL, true);
			a.hide(DataProvider.Plans.MIXED_UNITS_SMS, true);
			a.hide(DataProvider.Plans.MIXED_UNITS_MMS, true);
			a.hide(DataProvider.Plans.MERGED_PLANS, true);
			a.hide(DataProvider.Plans.SHORTNAME, false);
			break;
		default:
			a.hide(DataProvider.Plans.SHORTNAME, false);
			a.hide(DataProvider.Plans.BILLPERIOD_ID, false);
			a.hide(DataProvider.Plans.MERGED_PLANS, false);

			a.hide(DataProvider.Plans.BILLDAY, true);
			a.hide(DataProvider.Plans.BILLPERIOD, true);
			break;
		}

		switch (t) {
		case DataProvider.TYPE_MMS:
		case DataProvider.TYPE_SMS:
			a.hide(DataProvider.Plans.BILLMODE, true);
			a.hide(DataProvider.Plans.STRIP_SECONDS, true);
			a.hide(DataProvider.Plans.COST_PER_AMOUNT1, true);
			a.hide(DataProvider.Plans.COST_PER_AMOUNT_IN_LIMIT1, true);
			a.hide(DataProvider.Plans.MIXED_UNITS_CALL, true);
			a.hide(DataProvider.Plans.MIXED_UNITS_SMS, true);
			a.hide(DataProvider.Plans.MIXED_UNITS_MMS, true);

			a.hide(DataProvider.Plans.COST_PER_ITEM, false);
			a.hide(DataProvider.Plans.COST_PER_ITEM_IN_LIMIT, false);
			a.hide(DataProvider.Plans.COST_PER_PLAN, false);
			a.hide(DataProvider.Plans.LIMIT, false);
			a.hide(DataProvider.Plans.LIMIT_TYPE, false);
			break;
		case DataProvider.TYPE_DATA:
			a.hide(DataProvider.Plans.MIXED_UNITS_CALL, true);
			a.hide(DataProvider.Plans.MIXED_UNITS_SMS, true);
			a.hide(DataProvider.Plans.MIXED_UNITS_MMS, true);
			a.hide(DataProvider.Plans.COST_PER_ITEM, true);
			a.hide(DataProvider.Plans.COST_PER_ITEM_IN_LIMIT, true);
			a.hide(DataProvider.Plans.BILLMODE, true);
			a.hide(DataProvider.Plans.STRIP_SECONDS, true);

			a.hide(DataProvider.Plans.COST_PER_AMOUNT1, false);
			a.hide(DataProvider.Plans.COST_PER_AMOUNT_IN_LIMIT1, false);
			a.hide(DataProvider.Plans.COST_PER_PLAN, false);
			a.hide(DataProvider.Plans.LIMIT, false);
			a.hide(DataProvider.Plans.LIMIT_TYPE, false);
			break;
		case DataProvider.TYPE_CALL:
			a.hide(DataProvider.Plans.MIXED_UNITS_CALL, true);
			a.hide(DataProvider.Plans.MIXED_UNITS_SMS, true);
			a.hide(DataProvider.Plans.MIXED_UNITS_MMS, true);

			a.hide(DataProvider.Plans.BILLMODE, false);
			a.hide(DataProvider.Plans.STRIP_SECONDS, false);
			a.hide(DataProvider.Plans.BILLPERIOD_ID, false);
			a.hide(DataProvider.Plans.COST_PER_AMOUNT1, false);
			a.hide(DataProvider.Plans.COST_PER_AMOUNT_IN_LIMIT1, false);
			a.hide(DataProvider.Plans.COST_PER_ITEM, false);
			a.hide(DataProvider.Plans.COST_PER_ITEM_IN_LIMIT, false);
			a.hide(DataProvider.Plans.COST_PER_PLAN, false);
			a.hide(DataProvider.Plans.LIMIT, false);
			a.hide(DataProvider.Plans.LIMIT_TYPE, false);
			break;
		case DataProvider.TYPE_MIXED:
			a.hide(DataProvider.Plans.COST_PER_AMOUNT1, true);
			a.hide(DataProvider.Plans.COST_PER_AMOUNT_IN_LIMIT1, true);

			a.hide(DataProvider.Plans.BILLMODE, false);
			a.hide(DataProvider.Plans.STRIP_SECONDS, false);
			a.hide(DataProvider.Plans.COST_PER_ITEM, false);
			a.hide(DataProvider.Plans.COST_PER_ITEM_IN_LIMIT, false);
			a.hide(DataProvider.Plans.COST_PER_PLAN, false);
			a.hide(DataProvider.Plans.LIMIT, false);
			a.hide(DataProvider.Plans.LIMIT_TYPE, false);
			a.hide(DataProvider.Plans.MIXED_UNITS_CALL, false);
			a.hide(DataProvider.Plans.MIXED_UNITS_SMS, false);
			a.hide(DataProvider.Plans.MIXED_UNITS_MMS, false);
			break;
		case DataProvider.TYPE_TITLE:
			a.hide(DataProvider.Plans.SHORTNAME, true);
			a.hide(DataProvider.Plans.BILLMODE, true);
			a.hide(DataProvider.Plans.STRIP_SECONDS, true);
			a.hide(DataProvider.Plans.BILLPERIOD_ID, true);
			a.hide(DataProvider.Plans.COST_PER_AMOUNT1, true);
			a.hide(DataProvider.Plans.COST_PER_AMOUNT_IN_LIMIT1, true);
			a.hide(DataProvider.Plans.COST_PER_ITEM, true);
			a.hide(DataProvider.Plans.COST_PER_ITEM_IN_LIMIT, true);
			a.hide(DataProvider.Plans.COST_PER_PLAN, true);
			a.hide(DataProvider.Plans.LIMIT, true);
			a.hide(DataProvider.Plans.LIMIT_TYPE, true);
			a.hide(DataProvider.Plans.MIXED_UNITS_CALL, true);
			a.hide(DataProvider.Plans.MIXED_UNITS_SMS, true);
			a.hide(DataProvider.Plans.MIXED_UNITS_MMS, true);
			a.hide(DataProvider.Plans.MERGED_PLANS, true);
			break;
		default:
			break;
		}
		this.showHideExtraFileds(t);
		this.setMergePlansSelection();
		this.setHint(t);
	}

	/**
	 * Set hint for EditTexts.
	 * 
	 * @param t
	 *            type of plan
	 */
	private void setHint(final int t) {
		final PreferenceAdapter a = this.adapter;
		if (a == null) {
			return;
		}
		final ListPreference p = (ListPreference) a.getPreference(DataProvider.Plans.LIMIT_TYPE);
		final int lt = p.getValue();
		TextPreference tp0 = (TextPreference) a.getPreference(DataProvider.Plans.LIMIT);
		switch (lt) {
		case DataProvider.LIMIT_TYPE_COST:
			tp0.setHint(R.string.units_cost);
			break;
		case DataProvider.LIMIT_TYPE_UNITS:
			switch (t) {
			case DataProvider.TYPE_CALL:
				tp0.setHint(R.string.units_minutes);
				break;
			case DataProvider.TYPE_DATA:
				tp0.setHint(R.string.units_mbyte);
				break;
			case DataProvider.TYPE_MIXED:
				tp0.setHint(R.string.units_units);
				break;
			case DataProvider.TYPE_MMS:
			case DataProvider.TYPE_SMS:
				tp0.setHint(R.string.units_num_msg);
				break;
			default:
				tp0.setHint(-1);
				break;
			}
			break;
		default:
			tp0.setHint(-1);
			break;
		}

		tp0 = (TextPreference) a.getPreference(DataProvider.Plans.COST_PER_ITEM);
		TextPreference tp1 = (TextPreference) a
				.getPreference(DataProvider.Plans.COST_PER_ITEM_IN_LIMIT);
		Text2Preference tp2 = (Text2Preference) a
				.getPreference(DataProvider.Plans.COST_PER_AMOUNT1);
		Text2Preference tp3 = (Text2Preference) a
				.getPreference(DataProvider.Plans.COST_PER_AMOUNT_IN_LIMIT1);

		switch (t) {
		case DataProvider.TYPE_CALL:
			tp0.setHint(R.string.units_cost_per_call);
			tp1.setHint(R.string.units_cost_per_call);
			tp2.setHint(R.string.units_cost_per_minute);
			tp3.setHint(R.string.units_cost_per_minute);
			break;
		case DataProvider.TYPE_DATA:
			tp2.setHint(R.string.units_cost_per_mbyte);
			tp3.setHint(R.string.units_cost_per_mbyte);
			break;
		case DataProvider.TYPE_MIXED:
			tp0.setHint(R.string.units_cost_per_unit);
			tp1.setHint(R.string.units_cost_per_unit);
			break;
		case DataProvider.TYPE_MMS:
		case DataProvider.TYPE_SMS:
			tp0.setHint(R.string.units_cost_per_message);
			tp1.setHint(R.string.units_cost_per_message);
			break;
		default:
			tp0.setHint(-1);
			tp1.setHint(-1);
			tp2.setHint(-1);
			tp3.setHint(-1);
			break;
		}
	}

	/**
	 * Save a plan to database.
	 */
	private void savePlan() {
		final ContentValues cv = this.adapter.save();
		final Uri uri = this.getIntent().getData();
		if (uri == null) {
			this.getContentResolver().insert(DataProvider.Plans.CONTENT_URI, cv);
		} else {
			this.getContentResolver().update(uri, cv, null, null);
		}
		RuleMatcher.unmatch(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onClick(final View v) {
		switch (v.getId()) {
		case R.id.ok:
			this.savePlan();
			Preferences.setDefaultPlan(this, false);
		case R.id.cancel:
			this.pid = -1;
			this.finish();
			break;
		default:
			break;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onItemClick(final AdapterView<?> parent, final View view, final int position,
			final long id) {
		final Preference p = this.adapter.getItem(position);
		p.showDialog(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onDismiss(final DialogInterface dialog) {
		this.showHideFileds();
		this.adapter.notifyDataSetInvalidated();
	}
}
