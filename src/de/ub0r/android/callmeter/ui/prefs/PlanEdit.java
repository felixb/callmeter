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

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemSelectedListener;
import de.ub0r.android.callmeter.R;
import de.ub0r.android.callmeter.data.DataProvider;

/**
 * Edit a single Plan.
 * 
 * @author flx
 */
public class PlanEdit extends Activity implements OnClickListener,
		OnItemSelectedListener {

	/** Id of edited filed. */
	private long id = -1;

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
	/** {@link Spinner} holding billday. */
	private Spinner spBillday = null;
	/** {@link EditText} holding cost per item. */
	private EditText etCostPerItem = null;
	/** {@link EditText} holding cost per amount. */
	private EditText etCostPerAmount = null;
	/** {@link EditText} holding cost per item in limit. */
	private EditText etCostPerItemInLimit = null;
	/** {@link EditText} holding cost per plan. */
	private EditText etCostPerPlan = null;

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
		this.etLimit = (EditText) this.findViewById(R.id.limit_et);
		this.spBillmode = (Spinner) this.findViewById(R.id.billmode_sp);
		this.spBillmode.setOnItemSelectedListener(this);
		this.etBillmodeCust1 = (EditText) this
				.findViewById(R.id.billmode_cust_1_et);
		this.etBillmodeCust2 = (EditText) this
				.findViewById(R.id.billmode_cust_2_et);
		this.spBillperiod = (Spinner) this.findViewById(R.id.billperiod_sp);
		this.spBillday = (Spinner) this.findViewById(R.id.billday_sp);
		this.etCostPerItem = (EditText) this
				.findViewById(R.id.cost_per_item_et);
		this.etCostPerAmount = (EditText) this
				.findViewById(R.id.cost_per_amount_et);
		this.etCostPerItemInLimit = (EditText) this
				.findViewById(R.id.cost_per_item_in_limit_et);
		this.etCostPerPlan = (EditText) this
				.findViewById(R.id.cost_per_plan_et);

		this.findViewById(R.id.ok).setOnClickListener(this);
		this.findViewById(R.id.cancel).setOnClickListener(this);
		this.findViewById(R.id.name_help).setOnClickListener(this);
		this.findViewById(R.id.type_help).setOnClickListener(this);
		this.findViewById(R.id.shortname_help).setOnClickListener(this);
		this.findViewById(R.id.limit_type_help).setOnClickListener(this);
		this.findViewById(R.id.limit_help).setOnClickListener(this);
		this.findViewById(R.id.billmode_help).setOnClickListener(this);
		this.findViewById(R.id.billperiod_help).setOnClickListener(this);
		this.findViewById(R.id.billday_help).setOnClickListener(this);
		this.findViewById(R.id.cost_per_item_help).setOnClickListener(this);
		this.findViewById(R.id.cost_per_amount_help).setOnClickListener(this);
		this.findViewById(R.id.cost_per_item_in_limit_help).setOnClickListener(
				this);
		this.findViewById(R.id.cost_per_plan_help).setOnClickListener(this);

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
			this.id = -1;
		}
		if (cursor != null) {
			final int nid = cursor.getInt(DataProvider.Plans.INDEX_ID);
			if (nid != this.id) {
				this.id = nid;
			} else {
				cursor.close();
				return;
			}
		}
		this.etName.setText(cursor.getString(DataProvider.Plans.INDEX_NAME));
		this.etShortname.setText(cursor
				.getString(DataProvider.Plans.INDEX_SHORTNAME));
		this.spType.setSelection(cursor.getInt(DataProvider.Plans.INDEX_TYPE));
		this.spLimitType.setSelection(cursor
				.getInt(DataProvider.Plans.INDEX_LIMIT_TYPE));
		this.etLimit.setText(cursor.getString(DataProvider.Plans.INDEX_LIMIT));
		this.spBillmode = (Spinner) this.findViewById(R.id.billmode_sp);
		final String billmode = cursor
				.getString(DataProvider.Plans.INDEX_BILLMODE);
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
		this.spBillperiod.setSelection(cursor
				.getInt(DataProvider.Plans.INDEX_BILLPERIOD));
		this.spBillday.setSelection(cursor
				.getInt(DataProvider.Plans.INDEX_BILLDAY));
		this.etCostPerItem.setText(cursor
				.getString(DataProvider.Plans.INDEX_COST_PER_ITEM));
		this.etCostPerAmount.setText(cursor
				.getString(DataProvider.Plans.INDEX_COST_PER_AMOUNT));
		this.etCostPerItemInLimit.setText(cursor
				.getString(DataProvider.Plans.INDEX_COST_PER_ITEM_IN_LIMIT));
		this.etCostPerPlan.setText(cursor
				.getString(DataProvider.Plans.INDEX_COST_PER_PLAN));

		cursor.close();
	}

	/**
	 * Show or hide fields based on data in there.
	 */
	private void showHideFileds() {
		final int t = this.spType.getSelectedItemPosition();

		int v;
		if (t == DataProvider.TYPE_SPACING) {
			v = View.GONE;
		} else {
			v = View.VISIBLE;
		}
		this.findViewById(R.id.shortname_layout).setVisibility(v);

		if (t == DataProvider.TYPE_SPACING || // .
				t == DataProvider.TYPE_TITLE) {
			v = View.GONE;
		} else {
			v = View.VISIBLE;
		}
		this.findViewById(R.id.limit_type_layout).setVisibility(v);
		// TODO: hide limit if limit_type == unlimited
		this.findViewById(R.id.limit_layout).setVisibility(v);
		this.findViewById(R.id.billperiod_layout).setVisibility(v);
		// TODO: hide billday if billperiod == infinite
		this.findViewById(R.id.billday_layout).setVisibility(v);
		this.findViewById(R.id.cost_per_plan_layout).setVisibility(v);
		this.findViewById(R.id.cost_per_item_layout).setVisibility(v);
		if (t == DataProvider.TYPE_CALL || // .
				t == DataProvider.TYPE_DATA) {
			v = View.VISIBLE;
		} else {
			v = View.GONE;
		}
		this.findViewById(R.id.cost_per_amount_layout).setVisibility(v);

		if (t == DataProvider.TYPE_CALL || t == DataProvider.TYPE_MIXED) {
			v = View.VISIBLE;
		} else {
			v = View.GONE;
		}
		this.findViewById(R.id.billmode_layout).setVisibility(v);
		this.findViewById(R.id.cost_per_item_in_limit_layout).setVisibility(v);

		final int bml = this.spBillmode.getCount();
		final int bmp = this.spBillmode.getSelectedItemPosition();
		if (bml == bmp + 1) {
			this.etBillmodeCust1.setVisibility(View.VISIBLE);
			this.etBillmodeCust2.setVisibility(View.VISIBLE);
		} else {
			this.etBillmodeCust1.setVisibility(View.GONE);
			this.etBillmodeCust2.setVisibility(View.GONE);
		}
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
	 * {@inheritDoc}
	 */
	@Override
	public final void onClick(final View v) {
		switch (v.getId()) {
		case R.id.ok:
			final ContentValues cv = new ContentValues();
			cv.put(DataProvider.Plans.NAME, this.etName.getText().toString());
			cv.put(DataProvider.Plans.SHORTNAME, this.etShortname.getText()
					.toString());
			cv.put(DataProvider.Plans.TYPE, this.spType
					.getSelectedItemPosition());
			cv.put(DataProvider.Plans.LIMIT_TYPE, this.spLimitType
					.getSelectedItemPosition());
			cv.put(DataProvider.Plans.LIMIT, this.etLimit.getText().toString());
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
			cv.put(DataProvider.Plans.BILLPERIOD, this.spBillperiod
					.getSelectedItemPosition());
			cv.put(DataProvider.Plans.BILLDAY, this.spBillday
					.getSelectedItemPosition());
			cv.put(DataProvider.Plans.COST_PER_ITEM, this.etCostPerItem
					.getText().toString());
			cv.put(DataProvider.Plans.COST_PER_AMOUNT, this.etCostPerAmount
					.getText().toString());
			cv.put(DataProvider.Plans.COST_PER_ITEM_IN_LIMIT,
					this.etCostPerItemInLimit.getText().toString());
			cv.put(DataProvider.Plans.COST_PER_PLAN, this.etCostPerPlan
					.getText().toString());

			final Uri uri = this.getIntent().getData();
			if (uri == null) {
				this.getContentResolver().insert(
						DataProvider.Plans.CONTENT_URI, cv);
			} else {
				this.getContentResolver().update(uri, cv, null, null);
			}
		case R.id.cancel:
			this.id = -1;
			this.finish();
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
			this.showHelp(R.string.cost_per_amount_help);
			break;
		case R.id.cost_per_item_in_limit_help:
			this.showHelp(R.string.cost_per_item_in_limit_help);
			break;
		case R.id.cost_per_plan_help:
			this.showHelp(R.string.cost_per_plan_help);
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
