/*
 * Copyright (C) 2009-2010 Felix Bechstein, The Android Open Source Project
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

import android.app.ListActivity;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnDismissListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
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

/**
 * Edit a single Plan.
 * 
 * @author flx
 */
public class PlanEdit extends ListActivity implements OnClickListener,
		OnItemClickListener, OnDismissListener {
	/** Tag for debug out. */
	// private static final String TAG = "pe";

	/** {@link PreferenceAdapter}. */
	private PreferenceAdapter adapter = null;

	/** Id of edited filed. */
	private long pid = -1;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setTitle(this.getString(R.string.settings) + " > "
				+ this.getString(R.string.plans) + " > "
				+ this.getString(R.string.edit_));
		this.setContentView(R.layout.list_ok_cancel);

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
	 * Fill the fields with data from the cursor.
	 */
	private void fillFields() {
		final Uri uri = this.getIntent().getData();
		if (uri == null) {
			return;
		}
		Cursor cursor = this.getContentResolver().query(uri,
				DataProvider.Plans.PROJECTION, null, null, null);
		if (cursor == null || !cursor.moveToFirst()) {
			cursor = null;
			this.pid = -1;
		}
		if (cursor != null) {
			final int nid = cursor.getInt(DataProvider.Plans.INDEX_ID);
			if (nid != this.pid) {
				this.pid = nid;

				this.adapter = new PreferenceAdapter(this);
				this.adapter.add(new TextPreference(this,
						DataProvider.Plans.NAME, "", R.string.name_,
						R.string.name_help, InputType.TYPE_NULL));
				this.adapter.add(new TextPreference(this,
						DataProvider.Plans.SHORTNAME, "", R.string.shortname_,
						R.string.shortname_help, InputType.TYPE_NULL));
				this.adapter
						.add(new ListPreference(this, DataProvider.Plans.TYPE,
								DataProvider.TYPE_CALL, R.string.type_,
								R.string.type_help, R.array.plans_type));
				this.adapter.add(new ListPreference(this,
						DataProvider.Plans.BILLPERIOD,
						DataProvider.BILLPERIOD_1MONTH, R.string.billperiod_,
						R.string.billperiod_help, R.array.billperiod));
				this.adapter.add(new DatePreference(this,
						DataProvider.Plans.BILLDAY, R.string.billday_,
						R.string.billday_help));
				this.adapter.add(new CursorPreference(this,
						DataProvider.Plans.BILLPERIOD_ID,
						R.string.billperiodid_, R.string.billperiodid_help, -1,
						-1, -1, DataProvider.Plans.CONTENT_URI,
						DataProvider.Plans.ID, DataProvider.Plans.NAME,
						DataProvider.Plans.TYPE + " == "
								+ DataProvider.TYPE_BILLPERIOD, null, null,
						null));
				this.adapter.add(new ListPreference(this,
						DataProvider.Plans.LIMIT_TYPE,
						DataProvider.LIMIT_TYPE_NONE, R.string.limit_type_,
						R.string.limit_type_help, R.array.limit_type));
				this.adapter.add(new TextPreference(this,
						DataProvider.Plans.LIMIT, "0", R.string.limit_,
						R.string.limit_help, InputType.TYPE_CLASS_NUMBER));
				this.adapter.add(new BillmodePreference(this,
						DataProvider.Plans.BILLMODE, R.string.billmode_,
						R.string.billmode_help));
				this.adapter.add(new TextPreference(this,
						DataProvider.Plans.COST_PER_PLAN, "0",
						R.string.cost_per_plan_, R.string.cost_per_plan_help,
						InputType.TYPE_CLASS_NUMBER
								| InputType.TYPE_NUMBER_FLAG_DECIMAL));
				this.adapter.add(new TextPreference(this,
						DataProvider.Plans.COST_PER_ITEM_IN_LIMIT, "0",
						R.string.cost_per_item_in_limit_,
						R.string.cost_per_item_in_limit_help,
						InputType.TYPE_CLASS_NUMBER
								| InputType.TYPE_NUMBER_FLAG_DECIMAL));
				this.adapter.add(new TextPreference(this,
						DataProvider.Plans.COST_PER_ITEM, "0",
						R.string.cost_per_item_, R.string.cost_per_item_help,
						InputType.TYPE_CLASS_NUMBER
								| InputType.TYPE_NUMBER_FLAG_DECIMAL));
				this.adapter.add(new TextPreference(this,
						DataProvider.Plans.MIXED_UNITS_CALL, "0",
						R.string.mixed_units_call_,
						R.string.mixed_units_call_help,
						InputType.TYPE_CLASS_NUMBER));
				this.adapter.add(new TextPreference(this,
						DataProvider.Plans.MIXED_UNITS_SMS, "0",
						R.string.mixed_units_sms_,
						R.string.mixed_units_sms_help,
						InputType.TYPE_CLASS_NUMBER));
				this.adapter.add(new TextPreference(this,
						DataProvider.Plans.MIXED_UNITS_MMS, "0",
						R.string.mixed_units_mms_,
						R.string.mixed_units_mms_help,
						InputType.TYPE_CLASS_NUMBER));
				this.adapter.add(new Text2Preference(this,
						DataProvider.Plans.COST_PER_AMOUNT_IN_LIMIT1,
						DataProvider.Plans.COST_PER_AMOUNT_IN_LIMIT2, "0", "0",
						R.string.cost_per_amount_in_limit_,
						R.string.cost_per_amount_in_limit_help1,
						R.string.cost_per_amount_in_limit_help2,
						InputType.TYPE_CLASS_NUMBER
								| InputType.TYPE_NUMBER_FLAG_DECIMAL));
				this.adapter.add(new Text2Preference(this,
						DataProvider.Plans.COST_PER_AMOUNT1,
						DataProvider.Plans.COST_PER_AMOUNT2, "0", "0",
						R.string.cost_per_amount_,
						R.string.cost_per_amount_help1,
						R.string.cost_per_amount_help2,
						InputType.TYPE_CLASS_NUMBER
								| InputType.TYPE_NUMBER_FLAG_DECIMAL));
				this.adapter.load(cursor);
				this.setListAdapter(this.adapter);
				this.getListView().setOnItemClickListener(this);
			}
		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
	}

	/**
	 * Show or hide fields based on data in there.
	 */
	private void showHideFileds() {
		final int t = ((ListPreference) this.adapter
				.getPreference(DataProvider.Plans.TYPE)).getValue();
		switch (t) {
		case DataProvider.TYPE_MMS:
		case DataProvider.TYPE_SMS:
			this.adapter.hide(DataProvider.Plans.SHORTNAME, false);
			break;
		case DataProvider.TYPE_SPACING:
			this.adapter.hide(DataProvider.Plans.SHORTNAME, true);
			break;
		default:
			this.adapter.hide(DataProvider.Plans.SHORTNAME, false);
			break;
		}

		switch (t) {
		case DataProvider.TYPE_MMS:
		case DataProvider.TYPE_SMS:
			this.adapter.hide(DataProvider.Plans.BILLDAY, true);
			this.adapter.hide(DataProvider.Plans.BILLMODE, true);
			this.adapter.hide(DataProvider.Plans.BILLPERIOD, true);
			this.adapter.hide(DataProvider.Plans.COST_PER_AMOUNT1, true);
			this.adapter.hide(DataProvider.Plans.COST_PER_AMOUNT_IN_LIMIT1,
					true);
			this.adapter.hide(DataProvider.Plans.MIXED_UNITS_CALL, true);
			this.adapter.hide(DataProvider.Plans.MIXED_UNITS_SMS, true);
			this.adapter.hide(DataProvider.Plans.MIXED_UNITS_MMS, true);

			this.adapter.hide(DataProvider.Plans.BILLPERIOD_ID, false);
			this.adapter.hide(DataProvider.Plans.COST_PER_ITEM, false);
			this.adapter.hide(DataProvider.Plans.COST_PER_ITEM_IN_LIMIT, false);
			this.adapter.hide(DataProvider.Plans.COST_PER_PLAN, false);
			this.adapter.hide(DataProvider.Plans.LIMIT_TYPE, false);
			this.adapter.hide(DataProvider.Plans.SHORTNAME, false);
			break;
		case DataProvider.TYPE_DATA:
			this.adapter.hide(DataProvider.Plans.BILLDAY, true);
			this.adapter.hide(DataProvider.Plans.BILLMODE, true);
			this.adapter.hide(DataProvider.Plans.BILLPERIOD, true);
			this.adapter.hide(DataProvider.Plans.MIXED_UNITS_CALL, true);
			this.adapter.hide(DataProvider.Plans.MIXED_UNITS_SMS, true);
			this.adapter.hide(DataProvider.Plans.MIXED_UNITS_MMS, true);

			this.adapter.hide(DataProvider.Plans.BILLPERIOD_ID, false);
			this.adapter.hide(DataProvider.Plans.COST_PER_AMOUNT1, false);
			this.adapter.hide(DataProvider.Plans.COST_PER_AMOUNT_IN_LIMIT1,
					false);
			this.adapter.hide(DataProvider.Plans.COST_PER_ITEM, false);
			this.adapter.hide(DataProvider.Plans.COST_PER_ITEM_IN_LIMIT, false);
			this.adapter.hide(DataProvider.Plans.COST_PER_PLAN, false);
			this.adapter.hide(DataProvider.Plans.LIMIT_TYPE, false);
			this.adapter.hide(DataProvider.Plans.SHORTNAME, false);
			break;
		case DataProvider.TYPE_CALL:
			this.adapter.hide(DataProvider.Plans.BILLDAY, true);
			this.adapter.hide(DataProvider.Plans.BILLPERIOD, true);
			this.adapter.hide(DataProvider.Plans.MIXED_UNITS_CALL, true);
			this.adapter.hide(DataProvider.Plans.MIXED_UNITS_SMS, true);
			this.adapter.hide(DataProvider.Plans.MIXED_UNITS_MMS, true);

			this.adapter.hide(DataProvider.Plans.BILLMODE, false);
			this.adapter.hide(DataProvider.Plans.BILLPERIOD_ID, false);
			this.adapter.hide(DataProvider.Plans.COST_PER_AMOUNT1, false);
			this.adapter.hide(DataProvider.Plans.COST_PER_AMOUNT_IN_LIMIT1,
					false);
			this.adapter.hide(DataProvider.Plans.COST_PER_ITEM, false);
			this.adapter.hide(DataProvider.Plans.COST_PER_ITEM_IN_LIMIT, false);
			this.adapter.hide(DataProvider.Plans.COST_PER_PLAN, false);
			this.adapter.hide(DataProvider.Plans.LIMIT_TYPE, false);
			this.adapter.hide(DataProvider.Plans.SHORTNAME, false);
			break;
		case DataProvider.TYPE_MIXED:
			this.adapter.hide(DataProvider.Plans.BILLDAY, true);
			this.adapter.hide(DataProvider.Plans.BILLPERIOD, true);
			this.adapter.hide(DataProvider.Plans.COST_PER_AMOUNT1, true);
			this.adapter.hide(DataProvider.Plans.COST_PER_AMOUNT_IN_LIMIT1,
					true);

			this.adapter.hide(DataProvider.Plans.BILLMODE, false);
			this.adapter.hide(DataProvider.Plans.BILLPERIOD_ID, false);
			this.adapter.hide(DataProvider.Plans.COST_PER_ITEM, false);
			this.adapter.hide(DataProvider.Plans.COST_PER_ITEM_IN_LIMIT, false);
			this.adapter.hide(DataProvider.Plans.COST_PER_PLAN, false);
			this.adapter.hide(DataProvider.Plans.LIMIT_TYPE, false);
			this.adapter.hide(DataProvider.Plans.MIXED_UNITS_CALL, false);
			this.adapter.hide(DataProvider.Plans.MIXED_UNITS_SMS, false);
			this.adapter.hide(DataProvider.Plans.MIXED_UNITS_MMS, false);
			this.adapter.hide(DataProvider.Plans.SHORTNAME, false);
			break;
		case DataProvider.TYPE_SPACING:
			this.adapter.hide(DataProvider.Plans.BILLDAY, true);
			this.adapter.hide(DataProvider.Plans.BILLMODE, true);
			this.adapter.hide(DataProvider.Plans.BILLPERIOD, true);
			this.adapter.hide(DataProvider.Plans.BILLPERIOD_ID, true);
			this.adapter.hide(DataProvider.Plans.COST_PER_AMOUNT1, true);
			this.adapter.hide(DataProvider.Plans.COST_PER_AMOUNT_IN_LIMIT1,
					true);
			this.adapter.hide(DataProvider.Plans.COST_PER_ITEM, true);
			this.adapter.hide(DataProvider.Plans.COST_PER_ITEM_IN_LIMIT, true);
			this.adapter.hide(DataProvider.Plans.COST_PER_PLAN, true);
			this.adapter.hide(DataProvider.Plans.LIMIT_TYPE, true);
			this.adapter.hide(DataProvider.Plans.MIXED_UNITS_CALL, true);
			this.adapter.hide(DataProvider.Plans.MIXED_UNITS_SMS, true);
			this.adapter.hide(DataProvider.Plans.MIXED_UNITS_MMS, true);
			this.adapter.hide(DataProvider.Plans.SHORTNAME, true);
			this.adapter.hide(DataProvider.Plans.LIMIT, true);
			break;
		case DataProvider.TYPE_TITLE:
			this.adapter.hide(DataProvider.Plans.SHORTNAME, false);

			this.adapter.hide(DataProvider.Plans.BILLDAY, true);
			this.adapter.hide(DataProvider.Plans.BILLMODE, true);
			this.adapter.hide(DataProvider.Plans.BILLPERIOD, true);
			this.adapter.hide(DataProvider.Plans.BILLPERIOD_ID, true);
			this.adapter.hide(DataProvider.Plans.COST_PER_AMOUNT1, true);
			this.adapter.hide(DataProvider.Plans.COST_PER_AMOUNT_IN_LIMIT1,
					true);
			this.adapter.hide(DataProvider.Plans.COST_PER_ITEM, true);
			this.adapter.hide(DataProvider.Plans.COST_PER_ITEM_IN_LIMIT, true);
			this.adapter.hide(DataProvider.Plans.COST_PER_PLAN, true);
			this.adapter.hide(DataProvider.Plans.LIMIT_TYPE, true);
			this.adapter.hide(DataProvider.Plans.MIXED_UNITS_CALL, true);
			this.adapter.hide(DataProvider.Plans.MIXED_UNITS_SMS, true);
			this.adapter.hide(DataProvider.Plans.MIXED_UNITS_MMS, true);
			break;
		case DataProvider.TYPE_BILLPERIOD:
			this.adapter.hide(DataProvider.Plans.SHORTNAME, false);
			this.adapter.hide(DataProvider.Plans.BILLPERIOD, false);

			this.adapter.hide(DataProvider.Plans.BILLMODE, true);
			this.adapter.hide(DataProvider.Plans.BILLPERIOD_ID, true);
			this.adapter.hide(DataProvider.Plans.COST_PER_AMOUNT1, true);
			this.adapter.hide(DataProvider.Plans.COST_PER_AMOUNT_IN_LIMIT1,
					true);
			this.adapter.hide(DataProvider.Plans.COST_PER_ITEM, true);
			this.adapter.hide(DataProvider.Plans.COST_PER_ITEM_IN_LIMIT, true);
			this.adapter.hide(DataProvider.Plans.COST_PER_PLAN, true);
			this.adapter.hide(DataProvider.Plans.LIMIT_TYPE, true);
			this.adapter.hide(DataProvider.Plans.LIMIT, true);
			this.adapter.hide(DataProvider.Plans.MIXED_UNITS_CALL, true);
			this.adapter.hide(DataProvider.Plans.MIXED_UNITS_SMS, true);
			this.adapter.hide(DataProvider.Plans.MIXED_UNITS_MMS, true);
			break;
		default:
			break;
		}

		if (t == DataProvider.TYPE_BILLPERIOD) {
			final ListPreference p = (ListPreference) this.adapter
					.getPreference(DataProvider.Plans.BILLPERIOD);
			final int i = p.getValue();
			this.adapter.hide(DataProvider.Plans.BILLDAY,
					i == DataProvider.BILLPERIOD_INFINITE
							|| i == DataProvider.BILLPERIOD_DAY);
		} else if (t != DataProvider.TYPE_SPACING
				&& t != DataProvider.TYPE_TITLE) {
			final ListPreference p = (ListPreference) this.adapter
					.getPreference(DataProvider.Plans.LIMIT_TYPE);
			final int lt = p.getValue();
			this.adapter.hide(DataProvider.Plans.LIMIT,
					lt == DataProvider.LIMIT_TYPE_NONE);

			final Text2Preference pil = (Text2Preference) this.adapter
					.getPreference(DataProvider.Plans.// .
					COST_PER_AMOUNT_IN_LIMIT1);
			final Text2Preference pol = (Text2Preference) this.adapter
					.getPreference(DataProvider.Plans.COST_PER_AMOUNT1);
			pil.setSingleMode(t != DataProvider.TYPE_CALL);
			pol.setSingleMode(t != DataProvider.TYPE_CALL);
		}
	}

	/**
	 * Save a plan to database.
	 */
	private void savePlan() {
		final ContentValues cv = this.adapter.save();
		final Uri uri = this.getIntent().getData();
		if (uri == null) {
			this.getContentResolver()
					.insert(DataProvider.Plans.CONTENT_URI, cv);
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
	public final void onItemClick(final AdapterView<?> parent, final View view,
			final int position, final long id) {
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
