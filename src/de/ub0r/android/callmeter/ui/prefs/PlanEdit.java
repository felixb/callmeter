/*
 * Copyright (C) 2010 Felix Bechstein, The Android Open Source Project
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
import android.database.Cursor;
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

	/** {@link Cursor} representing the Uri reached via Intent. */
	private Cursor cursor = null;
	/** Id of edited filed. */
	private long id = -1;

	/** {@link EditText} holding name. */
	private EditText etName = null;
	/** {@link EditText} holding shortname. */
	private EditText etShortname = null;
	/** {@link Spinner} holding type. */
	private Spinner spType = null;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.prefs_planedit);

		this.etName = (EditText) this.findViewById(R.id.name_et);
		this.etShortname = (EditText) this.findViewById(R.id.shortname_et);
		this.spType = (Spinner) this.findViewById(R.id.type_sp);
		this.spType.setOnItemSelectedListener(this);

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
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void onResume() {
		super.onResume();
		this.cursor = this.getContentResolver().query(
				this.getIntent().getData(), DataProvider.Plans.PROJECTION,
				null, null, null);
		if (this.cursor == null || !this.cursor.moveToFirst()) {
			this.cursor = null;
			this.id = -1;
		}
		if (this.cursor != null) {
			final int nid = this.cursor.getInt(DataProvider.Plans.INDEX_ID);
			if (nid != this.id) {
				this.fillFields();
				this.id = nid;
			}
		}
		this.showHideFileds();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void onPause() {
		super.onPause();
		if (this.cursor != null && !this.cursor.isClosed()) {
			this.cursor.close();
		}
	}

	/**
	 * Fill the fields with data from the cursor.
	 */
	private void fillFields() {
		if (this.cursor == null) {
			return;
		}
		this.etName.setText(this.cursor
				.getString(DataProvider.Plans.INDEX_NAME));
		this.etShortname.setText(this.cursor
				.getString(DataProvider.Plans.INDEX_SHORTNAME));
		this.spType.setSelection(this.cursor
				.getInt(DataProvider.Plans.INDEX_TYPE));
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
			// TODO get data from fields
			// this.getContentResolver().update(
			// DataProvider.Plans.CONTENT_URI.buildUpon().appendPath(
			// String.valueOf(this.edit)).build(), this.editData,
			// null, null);
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
		if (parent.getId() == R.id.type_sp) {
			this.showHideFileds();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onNothingSelected(final AdapterView<?> parent) {
		// nothing to do
	}
}
