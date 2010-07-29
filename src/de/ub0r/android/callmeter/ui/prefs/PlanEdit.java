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

import java.util.Calendar;

import android.app.DatePickerDialog;
import android.app.ListActivity;
import android.app.AlertDialog.Builder;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnDismissListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.format.DateFormat;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import de.ub0r.android.callmeter.R;
import de.ub0r.android.callmeter.data.DataProvider;
import de.ub0r.android.callmeter.data.RuleMatcher;
import de.ub0r.android.lib.Log;

/**
 * Edit a single Plan.
 * 
 * @author flx
 */
public class PlanEdit extends ListActivity implements OnClickListener,
		OnItemSelectedListener, OnItemClickListener, OnDismissListener {
	/** Tag for debug out. */
	private static final String TAG = "pe";

	/** {@link PreferenceAdapter}. */
	private PreferenceAdapter adapter = null;

	/** Id of edited filed. */
	private long pid = -1;

	/** {@link EditText} holding name. */
	private EditText etName = null;
	/** {@link EditText} holding shortname. */
	private EditText etShortname = null;
	/** {@link Spinner} holding type. */
	private Spinner spType = null;
	/** {@link Spinner} holding type of limit. */
	private Spinner spLimitType = null;
	/** {@link EditText} holding limit. */
	private EditText etLimit = null;
	/** {@link Spinner} holding billmode. */
	private Spinner spBillmode = null;
	/** {@link EditText} holding billmode - custom 1. */
	private EditText etBillmodeCust1 = null;
	/** {@link EditText} holding billmode - custom 2. */
	private EditText etBillmodeCust2 = null;
	/** {@link Spinner} holding billperiod. */
	private Spinner spBillperiod = null;
	/** {@link Button} holding billperiod link. */
	private Button btnBillperiodId = null;
	/** {@link Button} holding billday. */
	private Button btnBillday = null;
	/** {@link EditText} holding cost per item. */
	private EditText etCostPerItem = null;
	/** {@link EditText} holding cost per amount. */
	private EditText etCostPerAmount, etCostPerAmount1, etCostPerAmount2;
	/** {@link EditText} holding cost per item in limit. */
	private EditText etCostPerItemInLimit = null;
	/** {@link EditText} holding cost per amount in limit. */
	private EditText etCostPerAmountInLimit, etCostPerAmountInLimit1,
			etCostPerAmountInLimit2;
	/** {@link EditText} holding cost per plan. */
	private EditText etCostPerPlan = null;
	/** {@link EditText} holding units for sms, mms, call in mixed plans. */
	private EditText etMixedUnitsSMS, etMixedUnitsMMS, etMixedUnitsCall;

	/** {@link View}s holding layout. */
	private View llShortname, llLimitType, llLimit, llBillmode, llBillperiod,
			llBillperiodId, llBillday, llCostPerItem, llCostPerAmount,
			llCostPerItemInLimit, llCostPerAmountInLimit, llCostPerPlan,
			llMixedUnits;

	/** First day of this bill period. */
	private long billday = 0;

	/** Linked billperiod. */
	private long billperiod = -1;

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
		this.fillBillday();
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
				this.adapter.add(new Preference.TextPreference(this,
						DataProvider.Plans.NAME, "", R.string.name_,
						R.string.name_help, InputType.TYPE_NULL));
				this.adapter.add(new Preference.TextPreference(this,
						DataProvider.Plans.SHORTNAME, "", R.string.shortname_,
						R.string.shortname_help, InputType.TYPE_NULL));
				this.adapter
						.add(new Preference.ListPreference(this,
								DataProvider.Plans.TYPE,
								DataProvider.TYPE_CALL, R.string.type_,
								R.string.type_help, R.array.plans_type));

				// TODO: billperiod
				// TODO: billperiodday
				// TODO: billperiodid

				this.adapter.add(new Preference.ListPreference(this,
						DataProvider.Plans.LIMIT_TYPE,
						DataProvider.LIMIT_TYPE_NONE, R.string.limit_type_,
						R.string.limit_type_help, R.array.limit_type));
				this.adapter.add(new Preference.TextPreference(this,
						DataProvider.Plans.LIMIT, "0", R.string.limit_,
						R.string.limit_help, InputType.TYPE_CLASS_NUMBER));
				this.adapter.add(new Preference.ListPreference(this,
						DataProvider.Plans.BILLMODE, 0, R.string.billmode_,
						R.string.billmode_help, R.array.billmodes));
				this.adapter.add(new Preference.TextPreference(this,
						DataProvider.Plans.COST_PER_PLAN, "0",
						R.string.cost_per_plan_, R.string.cost_per_plan_help,
						InputType.TYPE_CLASS_NUMBER
								| InputType.TYPE_NUMBER_FLAG_DECIMAL));
				this.adapter.add(new Preference.TextPreference(this,
						DataProvider.Plans.COST_PER_ITEM_IN_LIMIT, "0",
						R.string.cost_per_item_in_limit_,
						R.string.cost_per_item_in_limit_help,
						InputType.TYPE_CLASS_NUMBER
								| InputType.TYPE_NUMBER_FLAG_DECIMAL));
				this.adapter.add(new Preference.TextPreference(this,
						DataProvider.Plans.COST_PER_ITEM, "0",
						R.string.cost_per_item_, R.string.cost_per_item_help,
						InputType.TYPE_CLASS_NUMBER
								| InputType.TYPE_NUMBER_FLAG_DECIMAL));
				// TODO: costperamount in limit
				// TODO: costperamount
				this.adapter.load(cursor);
				this.setListAdapter(this.adapter);
				this.getListView().setOnItemClickListener(this);
			} else {
				cursor.close();
				return;
			}
		} else {
			return;
		}

		if (1 == 1) { // FIXME
			cursor.close();
			return;
		}
		this.etName.setText(cursor.getString(DataProvider.Plans.INDEX_NAME));
		this.etShortname.setText(cursor
				.getString(DataProvider.Plans.INDEX_SHORTNAME));
		this.spType.setSelection(cursor.getInt(DataProvider.Plans.INDEX_TYPE));
		try {
			final int r = cursor.getInt(DataProvider.Plans.INDEX_LIMIT_TYPE);
			if (this.spLimitType.getCount() > r) {
				this.spLimitType.setSelection(r);
			}
		} catch (IndexOutOfBoundsException e) {
			Log.e(TAG, "error loading limit type", e);
		}
		this.etLimit.setText(cursor.getString(DataProvider.Plans.INDEX_LIMIT));
		this.spBillmode = (Spinner) this.findViewById(R.id.billmode_sp);
		final String billmode = cursor
				.getString(DataProvider.Plans.INDEX_BILLMODE);
		if (billmode == null) {
			this.spBillmode.setSelection(0);
		} else {
			final String[] billmodeParts = billmode.split("/");
			final String[] billmodes = this.getResources().getStringArray(
					R.array.billmodes);
			final int l = billmodes.length;
			int bm = l - 1;
			for (int i = 0; i < l; i++) {
				if (billmode.equals(billmodes[i])) {
					bm = i;
					break;
				}
			}
			this.spBillmode.setSelection(bm);
			this.etBillmodeCust1.setText(billmodeParts[0]);
			this.etBillmodeCust2.setText(billmodeParts[1]);
		}
		this.billperiod = cursor.getLong(DataProvider.Plans.INDEX_BILLPERIOD);
		if (this.spBillperiod.getCount() > this.billperiod) {
			this.spBillperiod.setSelection((int) this.billperiod);
		} else {
			this.spBillperiod.setSelection(DataProvider.BILLPERIOD_INFINITE);
		}
		this.billday = cursor.getLong(DataProvider.Plans.INDEX_BILLDAY);
		Calendar calBD = Calendar.getInstance();
		calBD.setTimeInMillis(this.billday);
		calBD = DataProvider.Plans.getBillDay((int) this.billperiod, calBD,
				null, false);
		if (calBD != null) {
			this.billday = calBD.getTimeInMillis();
		}
		this.etCostPerItem.setText(cursor
				.getString(DataProvider.Plans.INDEX_COST_PER_ITEM));
		this.etCostPerItemInLimit.setText(cursor
				.getString(DataProvider.Plans.INDEX_COST_PER_ITEM_IN_LIMIT));
		this.etCostPerPlan.setText(cursor
				.getString(DataProvider.Plans.INDEX_COST_PER_PLAN));
		this.etMixedUnitsCall.setText(cursor
				.getString(DataProvider.Plans.INDEX_MIXED_UNITS_CALL));
		this.etMixedUnitsSMS.setText(cursor
				.getString(DataProvider.Plans.INDEX_MIXED_UNITS_SMS));
		this.etMixedUnitsMMS.setText(cursor
				.getString(DataProvider.Plans.INDEX_MIXED_UNITS_MMS));

		String s1 = cursor.getString(DataProvider.Plans.INDEX_COST_PER_AMOUNT1);
		String s2 = cursor.getString(DataProvider.Plans.INDEX_COST_PER_AMOUNT2);
		this.etCostPerAmount.setText(s1);
		this.etCostPerAmount1.setText(s1);
		this.etCostPerAmount2.setText(s2);

		s1 = cursor
				.getString(DataProvider.Plans.INDEX_COST_PER_AMOUNT_IN_LIMIT1);
		s2 = cursor
				.getString(DataProvider.Plans.INDEX_COST_PER_AMOUNT_IN_LIMIT2);
		this.etCostPerAmountInLimit.setText(s1);
		this.etCostPerAmountInLimit1.setText(s1);
		this.etCostPerAmountInLimit2.setText(s2);

		cursor.close();
	}

	/** Set text of billday {@link Button}. */
	private void fillBillday() {
		if (1 == 1) {
			return; // FIXME
		}
		this.btnBillday.setText(DateFormat.getDateFormat(this).format(
				this.billday));
		this.btnBillperiodId.setText(DataProvider.Plans.getName(this
				.getContentResolver(), this.billperiod));
	}

	/**
	 * Show or hide fields based on data in there.
	 */
	private void showHideFileds() {
		final int t = ((Preference.ListPreference) this.adapter
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
			// FIXME this.llCostPerAmount.setVisibility(View.GONE);
			// FIXME this.llCostPerAmountInLimit.setVisibility(View.GONE);
			// FIXME this.llMixedUnits.setVisibility(View.GONE);

			// FIXME: this.llBillperiodId.setVisibility(View.VISIBLE);
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
			// this.llMixedUnits.setVisibility(View.GONE);

			// FIXME: this.llBillperiodId.setVisibility(View.VISIBLE);
			// FIXME: this.llCostPerAmount.setVisibility(View.VISIBLE);
			// FIXME: this.llCostPerAmountInLimit.setVisibility(View.VISIBLE);
			this.adapter.hide(DataProvider.Plans.COST_PER_ITEM, false);
			this.adapter.hide(DataProvider.Plans.COST_PER_ITEM_IN_LIMIT, false);
			this.adapter.hide(DataProvider.Plans.COST_PER_PLAN, false);
			this.adapter.hide(DataProvider.Plans.LIMIT_TYPE, false);
			this.adapter.hide(DataProvider.Plans.SHORTNAME, false);
			break;
		case DataProvider.TYPE_CALL:
			this.adapter.hide(DataProvider.Plans.BILLDAY, true);
			this.adapter.hide(DataProvider.Plans.BILLPERIOD, true);
			// FIXME: this.llMixedUnits.setVisibility(View.GONE);

			this.adapter.hide(DataProvider.Plans.BILLMODE, false);
			// FIXME: this.llBillperiodId.setVisibility(View.VISIBLE);
			// FIXME: this.llCostPerAmount.setVisibility(View.VISIBLE);
			// FIXME: this.llCostPerAmountInLimit.setVisibility(View.VISIBLE);
			this.adapter.hide(DataProvider.Plans.COST_PER_ITEM, false);
			this.adapter.hide(DataProvider.Plans.COST_PER_ITEM_IN_LIMIT, false);
			this.adapter.hide(DataProvider.Plans.COST_PER_PLAN, false);
			this.adapter.hide(DataProvider.Plans.LIMIT_TYPE, false);
			this.adapter.hide(DataProvider.Plans.SHORTNAME, false);
			break;
		case DataProvider.TYPE_MIXED:
			this.adapter.hide(DataProvider.Plans.BILLDAY, true);
			this.adapter.hide(DataProvider.Plans.BILLPERIOD, true);
			// FIXME: this.llCostPerAmount.setVisibility(View.GONE);
			// FIXME: this.llCostPerAmountInLimit.setVisibility(View.GONE);

			this.adapter.hide(DataProvider.Plans.BILLMODE, false);
			// FIXME: this.llBillperiodId.setVisibility(View.VISIBLE);
			this.adapter.hide(DataProvider.Plans.COST_PER_ITEM, false);
			this.adapter.hide(DataProvider.Plans.COST_PER_ITEM_IN_LIMIT, false);
			this.adapter.hide(DataProvider.Plans.COST_PER_PLAN, false);
			this.adapter.hide(DataProvider.Plans.LIMIT_TYPE, false);
			// FIXME: this.llMixedUnits.setVisibility(View.VISIBLE);
			this.adapter.hide(DataProvider.Plans.SHORTNAME, false);
			break;
		case DataProvider.TYPE_SPACING:
			this.adapter.hide(DataProvider.Plans.BILLDAY, true);
			this.adapter.hide(DataProvider.Plans.BILLMODE, true);
			this.adapter.hide(DataProvider.Plans.BILLPERIOD, true);
			// FIXME: this.llBillperiodId.setVisibility(View.GONE);
			// FIXME: this.llCostPerAmount.setVisibility(View.GONE);
			// FIXME: this.llCostPerAmountInLimit.setVisibility(View.GONE);
			this.adapter.hide(DataProvider.Plans.COST_PER_ITEM, true);
			this.adapter.hide(DataProvider.Plans.COST_PER_ITEM_IN_LIMIT, true);
			this.adapter.hide(DataProvider.Plans.COST_PER_PLAN, true);
			this.adapter.hide(DataProvider.Plans.LIMIT_TYPE, true);
			// FIXME: this.llMixedUnits.setVisibility(View.GONE);
			this.adapter.hide(DataProvider.Plans.SHORTNAME, true);
			this.adapter.hide(DataProvider.Plans.LIMIT, true);
			break;
		case DataProvider.TYPE_TITLE:
			this.adapter.hide(DataProvider.Plans.SHORTNAME, false);

			this.adapter.hide(DataProvider.Plans.BILLDAY, true);
			this.adapter.hide(DataProvider.Plans.BILLMODE, true);
			this.adapter.hide(DataProvider.Plans.BILLPERIOD, true);
			// FIXME: this.llBillperiodId.setVisibility(View.GONE);
			// FIXME: this.llCostPerAmount.setVisibility(View.GONE);
			// FIXME: this.llCostPerAmountInLimit.setVisibility(View.GONE);
			this.adapter.hide(DataProvider.Plans.COST_PER_ITEM, true);
			this.adapter.hide(DataProvider.Plans.COST_PER_ITEM_IN_LIMIT, true);
			this.adapter.hide(DataProvider.Plans.COST_PER_PLAN, true);
			this.adapter.hide(DataProvider.Plans.LIMIT_TYPE, true);
			// FIXME: this.llMixedUnits.setVisibility(View.GONE);
			break;
		case DataProvider.TYPE_BILLPERIOD:
			// if (this.spBillperiod.getCount() <= this.billperiod) {
			// this.spBillperiod
			// .setSelection(DataProvider.BILLPERIOD_INFINITE);
			// } FIXME
			this.adapter.hide(DataProvider.Plans.SHORTNAME, false);
			this.adapter.hide(DataProvider.Plans.BILLPERIOD, false);

			this.adapter.hide(DataProvider.Plans.BILLMODE, true);
			// FIXME: this.llBillperiodId.setVisibility(View.GONE);
			// FIXME: this.llCostPerAmount.setVisibility(View.GONE);
			// FIXME: this.llCostPerAmountInLimit.setVisibility(View.GONE);
			this.adapter.hide(DataProvider.Plans.COST_PER_ITEM, true);
			this.adapter.hide(DataProvider.Plans.COST_PER_ITEM_IN_LIMIT, true);
			this.adapter.hide(DataProvider.Plans.COST_PER_PLAN, true);
			this.adapter.hide(DataProvider.Plans.LIMIT_TYPE, true);
			this.adapter.hide(DataProvider.Plans.LIMIT, true);
			// FIXME: this.llMixedUnits.setVisibility(View.GONE);
			break;
		default:
			break;
		}

		if (1 == 1) {
			return; // FIXME
		}

		if (t == DataProvider.TYPE_BILLPERIOD) {
			final int i = this.spBillperiod.getSelectedItemPosition();
			if (i == DataProvider.BILLPERIOD_INFINITE
					|| i == DataProvider.BILLPERIOD_DAY) {
				this.llBillday.setVisibility(View.GONE);
			} else {
				this.llBillday.setVisibility(View.VISIBLE);
			}
		} else if (t != DataProvider.TYPE_SPACING
				&& t != DataProvider.TYPE_TITLE) {
			final int lt = this.spLimitType.getSelectedItemPosition();
			if (lt == DataProvider.LIMIT_TYPE_NONE) {
				this.llLimit.setVisibility(View.GONE);
			} else {
				this.llLimit.setVisibility(View.VISIBLE);
			}

			final int bml = this.spBillmode.getCount();
			final int bmp = this.spBillmode.getSelectedItemPosition();
			if (bml == bmp + 1) {
				this.etBillmodeCust1.setVisibility(View.VISIBLE);
				this.etBillmodeCust2.setVisibility(View.VISIBLE);
			} else {
				this.etBillmodeCust1.setVisibility(View.GONE);
				this.etBillmodeCust2.setVisibility(View.GONE);
			}

			if (t == DataProvider.TYPE_CALL) {
				this.etCostPerAmount.setVisibility(View.INVISIBLE);
				this.etCostPerAmountInLimit.setVisibility(View.INVISIBLE);
				this.etCostPerAmount1.setVisibility(View.VISIBLE);
				this.etCostPerAmountInLimit1.setVisibility(View.VISIBLE);
				this.etCostPerAmount2.setVisibility(View.VISIBLE);
				this.etCostPerAmountInLimit2.setVisibility(View.VISIBLE);
			} else {
				this.etCostPerAmount.setVisibility(View.VISIBLE);
				this.etCostPerAmountInLimit.setVisibility(View.VISIBLE);
				this.etCostPerAmount1.setVisibility(View.GONE);
				this.etCostPerAmountInLimit1.setVisibility(View.GONE);
				this.etCostPerAmount2.setVisibility(View.GONE);
				this.etCostPerAmountInLimit2.setVisibility(View.GONE);
			}
		}
		this.spType.setOnItemSelectedListener(this);
		this.spLimitType.setOnItemSelectedListener(this);
		this.spBillperiod.setOnItemSelectedListener(this);
	}

	/**
	 * Save a plan to database.
	 * 
	 * @param t
	 *            type of plan
	 */
	private void savePlan(final int t) {
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
		// final int t = this.spType.getSelectedItemPosition();
		final int t = -1; // FIXME
		switch (v.getId()) {
		case R.id.ok:
			this.savePlan(t);
		case R.id.cancel:
			this.pid = -1;
			this.finish();
			break;
		case R.id.billperiodid_btn:
			final Cursor cursor = this.getContentResolver().query(
					DataProvider.Plans.CONTENT_URI,
					new String[] { DataProvider.Plans.ID,
							DataProvider.Plans.NAME },
					DataProvider.Plans.TYPE + " = "
							+ DataProvider.TYPE_BILLPERIOD, null,
					DataProvider.Plans.NAME);
			if (cursor == null || !cursor.moveToFirst()) {
				break;
			}
			final int l = cursor.getCount();
			final String[] items = new String[l];
			final long[] itemIds = new long[l];
			for (int i = 0; i < l; i++) {
				items[i] = cursor.getString(1);
				itemIds[i] = cursor.getLong(0);
				cursor.moveToNext();
			}
			cursor.close();
			final Builder builder = new Builder(this);
			builder.setCancelable(true);
			builder.setNegativeButton(android.R.string.cancel, null);
			builder.setItems(items,
					new android.content.DialogInterface.OnClickListener() {
						@Override
						public void onClick(final DialogInterface dialog,
								final int which) {
							PlanEdit.this.billperiod = itemIds[which];
							PlanEdit.this.fillBillday();
						}
					});
			builder.show();
			break;
		case R.id.billday_btn:
			final Calendar d = Calendar.getInstance();
			if (this.billday > 0) {
				d.setTimeInMillis(this.billday);
			}
			new DatePickerDialog(this, new OnDateSetListener() {
				@Override
				public void onDateSet(final DatePicker view, final int year,
						final int monthOfYear, final int dayOfMonth) {
					final Calendar dd = Calendar.getInstance();
					dd.set(year, monthOfYear, dayOfMonth, 0, 0, 0);
					PlanEdit.this.billday = dd.getTimeInMillis();
					PlanEdit.this.fillBillday();
					PlanEdit.this.showHideFileds();
				}
			}, d.get(Calendar.YEAR), d.get(Calendar.MONTH), d
					.get(Calendar.DAY_OF_MONTH)).show();
			break;
		default:
			break;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onItemSelected(final AdapterView<?> parent,
			final View view, final int position, final long id) {
		this.showHideFileds();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onNothingSelected(final AdapterView<?> parent) {
		// nothing to do
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
