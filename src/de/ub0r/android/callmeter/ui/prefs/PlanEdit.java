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

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.AlertDialog.Builder;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemSelectedListener;
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
public class PlanEdit extends Activity implements OnClickListener,
		OnItemSelectedListener {
	/** Tag for debug out. */
	private static final String TAG = "pe";

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
		this.setContentView(R.layout.prefs_planedit);

		this.etName = (EditText) this.findViewById(R.id.name_et);
		this.etShortname = (EditText) this.findViewById(R.id.shortname_et);
		this.spType = (Spinner) this.findViewById(R.id.type_sp);
		this.spType.setOnItemSelectedListener(this);
		this.spLimitType = (Spinner) this.findViewById(R.id.limit_type_sp);
		this.spLimitType.setOnItemSelectedListener(this);
		this.etLimit = (EditText) this.findViewById(R.id.limit_et);
		this.spBillmode = (Spinner) this.findViewById(R.id.billmode_sp);
		this.spBillmode.setOnItemSelectedListener(this);
		this.etBillmodeCust1 = (EditText) this
				.findViewById(R.id.billmode_cust_1_et);
		this.etBillmodeCust2 = (EditText) this
				.findViewById(R.id.billmode_cust_2_et);
		this.spBillperiod = (Spinner) this.findViewById(R.id.billperiod_sp);
		this.spBillperiod.setOnItemSelectedListener(this);
		this.btnBillperiodId = (Button) this
				.findViewById(R.id.billperiodid_btn);
		this.btnBillday = (Button) this.findViewById(R.id.billday_btn);
		this.btnBillday.setOnClickListener(this);
		this.etCostPerItem = (EditText) this
				.findViewById(R.id.cost_per_item_et);
		this.etCostPerAmount = (EditText) this
				.findViewById(R.id.cost_per_amount_et);
		this.etCostPerAmount1 = (EditText) this
				.findViewById(R.id.cost_per_amount_et1);
		this.etCostPerAmount2 = (EditText) this
				.findViewById(R.id.cost_per_amount_et2);
		this.etCostPerItemInLimit = (EditText) this
				.findViewById(R.id.cost_per_item_in_limit_et);
		this.etCostPerAmountInLimit = (EditText) this
				.findViewById(R.id.cost_per_amount_in_limit_et);
		this.etCostPerAmountInLimit1 = (EditText) this
				.findViewById(R.id.cost_per_amount_in_limit_et1);
		this.etCostPerAmountInLimit2 = (EditText) this
				.findViewById(R.id.cost_per_amount_in_limit_et2);
		this.etCostPerPlan = (EditText) this
				.findViewById(R.id.cost_per_plan_et);
		this.etMixedUnitsCall = (EditText) this
				.findViewById(R.id.mixed_units_call_et);
		this.etMixedUnitsSMS = (EditText) this
				.findViewById(R.id.mixed_units_sms_et);
		this.etMixedUnitsMMS = (EditText) this
				.findViewById(R.id.mixed_units_mms_et);

		this.llShortname = this.findViewById(R.id.shortname_layout);
		this.llLimitType = this.findViewById(R.id.limit_type_layout);
		this.llLimit = this.findViewById(R.id.limit_layout);
		this.llBillmode = this.findViewById(R.id.billmode_layout);
		this.llBillperiod = this.findViewById(R.id.billperiod_layout);
		this.llBillperiodId = this.findViewById(R.id.billperiodid_layout);
		this.llBillday = this.findViewById(R.id.billday_layout);
		this.llCostPerAmount = this.findViewById(R.id.cost_per_amount_layout);
		this.llCostPerItem = this.findViewById(R.id.cost_per_item_layout);
		this.llCostPerItemInLimit = this
				.findViewById(R.id.cost_per_item_in_limit_layout);
		this.llCostPerAmountInLimit = this
				.findViewById(R.id.cost_per_amount_in_limit_layout);
		this.llCostPerPlan = this.findViewById(R.id.cost_per_plan_layout);
		this.llMixedUnits = this.findViewById(R.id.mixed_units_layout);

		this.findViewById(R.id.ok).setOnClickListener(this);
		this.findViewById(R.id.cancel).setOnClickListener(this);
		this.findViewById(R.id.billperiodid_btn).setOnClickListener(this);
		this.findViewById(R.id.name_help).setOnClickListener(this);
		this.findViewById(R.id.type_help).setOnClickListener(this);
		this.findViewById(R.id.shortname_help).setOnClickListener(this);
		this.findViewById(R.id.limit_type_help).setOnClickListener(this);
		this.findViewById(R.id.limit_help).setOnClickListener(this);
		this.findViewById(R.id.billmode_help).setOnClickListener(this);
		this.findViewById(R.id.billperiod_help).setOnClickListener(this);
		this.findViewById(R.id.billperiodid_help).setOnClickListener(this);
		this.findViewById(R.id.billday_help).setOnClickListener(this);
		this.findViewById(R.id.cost_per_item_help).setOnClickListener(this);
		this.findViewById(R.id.cost_per_amount_help).setOnClickListener(this);
		this.findViewById(R.id.cost_per_item_in_limit_help).setOnClickListener(
				this);
		this.findViewById(R.id.cost_per_amount_in_limit_help)
				.setOnClickListener(this);
		this.findViewById(R.id.cost_per_plan_help).setOnClickListener(this);
		this.findViewById(R.id.mixed_units_call_help).setOnClickListener(this);
		this.findViewById(R.id.mixed_units_sms_help).setOnClickListener(this);
		this.findViewById(R.id.mixed_units_mms_help).setOnClickListener(this);

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
			} else {
				cursor.close();
				return;
			}
		} else {
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
		this.btnBillday.setText(DateFormat.getDateFormat(this).format(
				this.billday));
		this.btnBillperiodId.setText(DataProvider.Plans.getName(this
				.getContentResolver(), this.billperiod));
	}

	/**
	 * Show or hide fields based on data in there.
	 */
	private void showHideFileds() {
		this.spType.setOnItemSelectedListener(null);
		this.spLimitType.setOnItemSelectedListener(null);
		this.spBillperiod.setOnItemSelectedListener(null);
		final int t = this.spType.getSelectedItemPosition();
		switch (t) {
		case DataProvider.TYPE_MMS:
		case DataProvider.TYPE_SMS:
			this.llBillday.setVisibility(View.GONE);
			this.llBillmode.setVisibility(View.GONE);
			this.llBillperiod.setVisibility(View.GONE);
			this.llCostPerAmount.setVisibility(View.GONE);
			this.llCostPerAmountInLimit.setVisibility(View.GONE);
			this.llMixedUnits.setVisibility(View.GONE);

			this.llBillperiodId.setVisibility(View.VISIBLE);
			this.llCostPerItem.setVisibility(View.VISIBLE);
			this.llCostPerItemInLimit.setVisibility(View.VISIBLE);
			this.llCostPerPlan.setVisibility(View.VISIBLE);
			this.llLimitType.setVisibility(View.VISIBLE);
			this.llShortname.setVisibility(View.VISIBLE);
			break;
		case DataProvider.TYPE_DATA:
			this.llBillday.setVisibility(View.GONE);
			this.llBillmode.setVisibility(View.GONE);
			this.llBillperiod.setVisibility(View.GONE);
			this.llMixedUnits.setVisibility(View.GONE);

			this.llBillperiodId.setVisibility(View.VISIBLE);
			this.llCostPerAmount.setVisibility(View.VISIBLE);
			this.llCostPerAmountInLimit.setVisibility(View.VISIBLE);
			this.llCostPerItem.setVisibility(View.VISIBLE);
			this.llCostPerItemInLimit.setVisibility(View.VISIBLE);
			this.llCostPerPlan.setVisibility(View.VISIBLE);
			this.llLimitType.setVisibility(View.VISIBLE);
			this.llShortname.setVisibility(View.VISIBLE);
			break;
		case DataProvider.TYPE_CALL:
			this.llBillday.setVisibility(View.GONE);
			this.llBillperiod.setVisibility(View.GONE);
			this.llMixedUnits.setVisibility(View.GONE);

			this.llBillmode.setVisibility(View.VISIBLE);
			this.llBillperiodId.setVisibility(View.VISIBLE);
			this.llCostPerAmount.setVisibility(View.VISIBLE);
			this.llCostPerAmountInLimit.setVisibility(View.VISIBLE);
			this.llCostPerItem.setVisibility(View.VISIBLE);
			this.llCostPerItemInLimit.setVisibility(View.VISIBLE);
			this.llCostPerPlan.setVisibility(View.VISIBLE);
			this.llLimitType.setVisibility(View.VISIBLE);
			this.llShortname.setVisibility(View.VISIBLE);
			break;
		case DataProvider.TYPE_MIXED:
			this.llBillday.setVisibility(View.GONE);
			this.llBillperiod.setVisibility(View.GONE);
			this.llCostPerAmount.setVisibility(View.GONE);
			this.llCostPerAmountInLimit.setVisibility(View.GONE);

			this.llBillmode.setVisibility(View.VISIBLE);
			this.llBillperiodId.setVisibility(View.VISIBLE);
			this.llCostPerItem.setVisibility(View.VISIBLE);
			this.llCostPerItemInLimit.setVisibility(View.VISIBLE);
			this.llCostPerPlan.setVisibility(View.VISIBLE);
			this.llLimitType.setVisibility(View.VISIBLE);
			this.llMixedUnits.setVisibility(View.VISIBLE);
			this.llShortname.setVisibility(View.VISIBLE);
			break;
		case DataProvider.TYPE_SPACING:
			this.llBillday.setVisibility(View.GONE);
			this.llBillmode.setVisibility(View.GONE);
			this.llBillperiod.setVisibility(View.GONE);
			this.llBillperiodId.setVisibility(View.GONE);
			this.llCostPerAmount.setVisibility(View.GONE);
			this.llCostPerAmountInLimit.setVisibility(View.GONE);
			this.llCostPerItem.setVisibility(View.GONE);
			this.llCostPerItemInLimit.setVisibility(View.GONE);
			this.llCostPerPlan.setVisibility(View.GONE);
			this.llLimitType.setVisibility(View.GONE);
			this.llMixedUnits.setVisibility(View.GONE);
			this.llShortname.setVisibility(View.GONE);
			this.llLimit.setVisibility(View.GONE);
			break;
		case DataProvider.TYPE_TITLE:
			this.llShortname.setVisibility(View.VISIBLE);

			this.llBillday.setVisibility(View.GONE);
			this.llBillmode.setVisibility(View.GONE);
			this.llBillperiod.setVisibility(View.GONE);
			this.llBillperiodId.setVisibility(View.GONE);
			this.llCostPerAmount.setVisibility(View.GONE);
			this.llCostPerAmountInLimit.setVisibility(View.GONE);
			this.llCostPerItem.setVisibility(View.GONE);
			this.llCostPerItemInLimit.setVisibility(View.GONE);
			this.llCostPerPlan.setVisibility(View.GONE);
			this.llLimitType.setVisibility(View.GONE);
			this.llLimit.setVisibility(View.GONE);
			this.llMixedUnits.setVisibility(View.GONE);
			break;
		case DataProvider.TYPE_BILLPERIOD:
			if (this.spBillperiod.getCount() <= this.billperiod) {
				this.spBillperiod
						.setSelection(DataProvider.BILLPERIOD_INFINITE);
			}
			this.llShortname.setVisibility(View.VISIBLE);
			this.llBillperiod.setVisibility(View.VISIBLE);

			this.llBillmode.setVisibility(View.GONE);
			this.llBillperiodId.setVisibility(View.GONE);
			this.llCostPerAmount.setVisibility(View.GONE);
			this.llCostPerAmountInLimit.setVisibility(View.GONE);
			this.llCostPerItem.setVisibility(View.GONE);
			this.llCostPerItemInLimit.setVisibility(View.GONE);
			this.llCostPerPlan.setVisibility(View.GONE);
			this.llLimitType.setVisibility(View.GONE);
			this.llLimit.setVisibility(View.GONE);
			this.llMixedUnits.setVisibility(View.GONE);
			break;
		default:
			break;
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
	 * Show help text.
	 * 
	 * @param res
	 *            resource
	 */
	private void showHelp(final int res) {
		final Builder b = new Builder(this);
		b.setMessage(res);
		b.setPositiveButton(android.R.string.ok, null);
		b.show();
	}

	/**
	 * Save a plan to database.
	 * 
	 * @param t
	 *            type of plan
	 */
	private void savePlan(final int t) {
		final ContentValues cv = new ContentValues();
		cv.put(DataProvider.Plans.NAME, this.etName.getText().toString());
		cv.put(DataProvider.Plans.SHORTNAME, this.etShortname.getText()
				.toString());
		cv.put(DataProvider.Plans.TYPE, t);
		cv.put(DataProvider.Plans.LIMIT_TYPE, this.spLimitType
				.getSelectedItemPosition());
		cv.put(DataProvider.Plans.LIMIT, Utils.parseLong(this.etLimit.getText()
				.toString(), 0));
		final int bm = this.spBillmode.getSelectedItemPosition();
		final String[] billmodes = this.getResources().getStringArray(
				R.array.billmodes);
		if (bm == billmodes.length - 1) {
			final String billmode = this.etBillmodeCust1.getText() + "/"
					+ this.etBillmodeCust2.getText();
			cv.put(DataProvider.Plans.BILLMODE, billmode);
		} else {
			cv.put(DataProvider.Plans.BILLMODE, billmodes[bm]);
		}
		if (t == DataProvider.TYPE_BILLPERIOD) {
			cv.put(DataProvider.Plans.BILLPERIOD, this.spBillperiod
					.getSelectedItemPosition());
		} else {
			cv.put(DataProvider.Plans.BILLPERIOD, this.billperiod);
		}
		cv.put(DataProvider.Plans.BILLDAY, this.billday);
		cv.put(DataProvider.Plans.COST_PER_ITEM, this.etCostPerItem.getText()
				.toString());
		if (this.etCostPerAmount.getVisibility() == View.VISIBLE) {
			cv.put(DataProvider.Plans.COST_PER_AMOUNT1, this.etCostPerAmount
					.getText().toString());
			cv.put(DataProvider.Plans.COST_PER_AMOUNT2, this.etCostPerAmount
					.getText().toString());
		} else {
			cv.put(DataProvider.Plans.COST_PER_AMOUNT1, this.etCostPerAmount1
					.getText().toString());
			cv.put(DataProvider.Plans.COST_PER_AMOUNT2, this.etCostPerAmount2
					.getText().toString());
		}
		cv.put(DataProvider.Plans.COST_PER_ITEM_IN_LIMIT,
				this.etCostPerItemInLimit.getText().toString());
		if (this.etCostPerAmountInLimit.getVisibility() == View.VISIBLE) {
			cv.put(DataProvider.Plans.COST_PER_AMOUNT_IN_LIMIT1,
					this.etCostPerAmountInLimit.getText().toString());
			cv.put(DataProvider.Plans.COST_PER_AMOUNT_IN_LIMIT2,
					this.etCostPerAmountInLimit.getText().toString());
		} else {
			cv.put(DataProvider.Plans.COST_PER_AMOUNT_IN_LIMIT1,
					this.etCostPerAmountInLimit1.getText().toString());
			cv.put(DataProvider.Plans.COST_PER_AMOUNT_IN_LIMIT2,
					this.etCostPerAmountInLimit2.getText().toString());
		}
		cv.put(DataProvider.Plans.COST_PER_PLAN, this.etCostPerPlan.getText()
				.toString());
		cv.put(DataProvider.Plans.MIXED_UNITS_CALL, this.etMixedUnitsCall
				.getText().toString());
		cv.put(DataProvider.Plans.MIXED_UNITS_SMS, this.etMixedUnitsSMS
				.getText().toString());
		cv.put(DataProvider.Plans.MIXED_UNITS_MMS, this.etMixedUnitsMMS
				.getText().toString());

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
		final int t = this.spType.getSelectedItemPosition();
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
		case R.id.name_help:
			this.showHelp(R.string.name_help);
			break;
		case R.id.shortname_help:
			this.showHelp(R.string.shortname_help);
			break;
		case R.id.type_help:
			this.showHelp(R.string.type_help);
			break;
		case R.id.limit_type_help:
			this.showHelp(R.string.limit_type_help);
			break;
		case R.id.limit_help:
			this.showHelp(R.string.limit_help);
			break;
		case R.id.billmode_help:
			this.showHelp(R.string.billmode_help);
			break;
		case R.id.billperiodid_help:
			this.showHelp(R.string.billperiodid_help);
			break;
		case R.id.billperiod_help:
			this.showHelp(R.string.billperiod_help);
			break;
		case R.id.billday_help:
			this.showHelp(R.string.billday_help);
			break;
		case R.id.cost_per_item_help:
			this.showHelp(R.string.cost_per_item_help);
			break;
		case R.id.cost_per_amount_help:
			if (this.etCostPerAmount.getVisibility() == View.VISIBLE) {
				this.showHelp(R.string.cost_per_amount_help1);
			} else {
				this.showHelp(R.string.cost_per_amount_help2);
			}
			break;
		case R.id.cost_per_item_in_limit_help:
			this.showHelp(R.string.cost_per_item_in_limit_help);
			break;
		case R.id.cost_per_amount_in_limit_help:
			if (this.etCostPerAmountInLimit.getVisibility() == View.VISIBLE) {
				this.showHelp(R.string.cost_per_amount_in_limit_help1);
			} else {
				this.showHelp(R.string.cost_per_amount_in_limit_help2);
			}
			break;
		case R.id.cost_per_plan_help:
			this.showHelp(R.string.cost_per_plan_help);
			break;
		case R.id.mixed_units_call_help:
			this.showHelp(R.string.mixed_units_call_help);
			break;
		case R.id.mixed_units_sms_help:
			this.showHelp(R.string.mixed_units_sms_help);
			break;
		case R.id.mixed_units_mms_help:
			this.showHelp(R.string.mixed_units_mms_help);
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
}
