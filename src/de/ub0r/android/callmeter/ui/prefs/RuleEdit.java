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
import android.widget.Button;
import android.widget.CheckBox;
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
public class RuleEdit extends Activity implements OnClickListener,
		OnItemSelectedListener {

	/** Extra for {@link Intent}: is child? */
	static final String EXTRA_ISCHILD = "is_child";

	/** Id of edited filed. */
	private long id = -1;

	/** {@link EditText} holding name. */
	private EditText etName = null;
	/** {@link Spinner} holding target plan. */
	private Spinner spPlan = null;
	/** {@link CheckBox} holding negate. */
	private CheckBox cbNegate = null;
	/** {@link Spinner} holding what. */
	private Spinner spWhat = null;
	/** {@link Button} holding what0. */
	private Button btnWhat0 = null;
	/** {@link Button} holding what1. */
	private Button btnWhat1 = null;

	/** Data for target plan. */
	private int plan = -1;
	/** Data for what0. */
	private int what0 = -1;
	/** Data for what1. */
	private int what1 = -1;
	/** Data for is child. */
	private boolean isChild = false;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setTitle(this.getString(R.string.settings) + " > "
				+ this.getString(R.string.rules) + " > "
				+ this.getString(R.string.edit_));
		this.setContentView(R.layout.prefs_ruleedit);

		this.etName = (EditText) this.findViewById(R.id.name_et);
		this.spPlan = (Spinner) this.findViewById(R.id.plan_sp);
		this.spPlan.setOnItemSelectedListener(this);
		this.cbNegate = (CheckBox) this.findViewById(R.id.negate_cb);
		this.spWhat = (Spinner) this.findViewById(R.id.type_sp);
		this.spWhat.setOnItemSelectedListener(this);
		this.btnWhat0 = (Button) this.findViewById(R.id.what0_btn);
		this.btnWhat0.setOnClickListener(this);
		this.btnWhat1 = (Button) this.findViewById(R.id.what1_btn);
		this.btnWhat1.setOnClickListener(this);

		this.findViewById(R.id.ok).setOnClickListener(this);
		this.findViewById(R.id.cancel).setOnClickListener(this);
		this.findViewById(R.id.name_help).setOnClickListener(this);
		this.findViewById(R.id.type_help).setOnClickListener(this);
		this.findViewById(R.id.what0_help).setOnClickListener(this);
		this.findViewById(R.id.what1_help).setOnClickListener(this);
		this.findViewById(R.id.plan_help).setOnClickListener(this);
		this.findViewById(R.id.negate_help).setOnClickListener(this);

		this.fillFields();
		this.showHideFileds();
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
		this.isChild = this.getIntent().getBooleanExtra(EXTRA_ISCHILD, false);
		Cursor cursor = this.getContentResolver().query(uri,
				DataProvider.Rules.PROJECTION, null, null, null);
		if (cursor == null || !cursor.moveToFirst()) {
			cursor = null;
			this.id = -1;
		}
		if (cursor != null) {
			final int nid = cursor.getInt(DataProvider.Rules.INDEX_ID);
			if (nid != this.id) {
				this.id = nid;
			} else {
				cursor.close();
				return;
			}
		}
		this.etName.setText(cursor.getString(DataProvider.Rules.INDEX_NAME));
		this.cbNegate
				.setChecked(cursor.getInt(DataProvider.Rules.INDEX_NOT) != 0);
		this.spWhat.setSelection(cursor.getInt(DataProvider.Rules.INDEX_WHAT));
		this.what0 = cursor.getInt(DataProvider.Rules.INDEX_WHAT0);
		this.what1 = cursor.getInt(DataProvider.Rules.INDEX_WHAT1);
		this.plan = cursor.getInt(DataProvider.Rules.INDEX_PLAN_ID);
		// TODO: fill plan, what0, what1

		cursor.close();
	}

	/**
	 * Show or hide fields based on data in there.
	 */
	private void showHideFileds() {
		final int t = this.spWhat.getSelectedItemPosition();

		int v;
		switch (t) {
		case DataProvider.Rules.WHAT_HOURS:
		case DataProvider.Rules.WHAT_NUMBER:
		case DataProvider.Rules.WHAT_NUMBERS:
			v = View.VISIBLE;
			break;
		default:
			v = View.GONE;
			break;
		}
		this.findViewById(R.id.what0_layout).setVisibility(v);

		switch (t) {
		case DataProvider.Rules.WHAT_HOURS:
		case DataProvider.Rules.WHAT_LIMIT_REACHED:
		case DataProvider.Rules.WHAT_NUMBER:
		case DataProvider.Rules.WHAT_NUMBERS:
		case DataProvider.Rules.WHAT_ROAMING:
			v = View.VISIBLE;
			break;
		default:
			v = View.GONE;
			break;
		}
		this.findViewById(R.id.negate_layout).setVisibility(v);

		if (this.isChild) {
			v = View.GONE;
		} else {
			v = View.VISIBLE;
		}
		this.findViewById(R.id.plan_layout).setVisibility(v);
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
			cv.put(DataProvider.Rules.NAME, this.etName.getText().toString());
			cv.put(DataProvider.Rules.NOT, this.cbNegate.isChecked());
			cv.put(DataProvider.Rules.WHAT, this.spWhat
					.getSelectedItemPosition());
			cv.put(DataProvider.Rules.WHAT0, this.what0);
			cv.put(DataProvider.Rules.WHAT1, this.what1);
			cv.put(DataProvider.Rules.PLAN_ID, this.plan);
			cv.put(DataProvider.Rules.ISCHILD, this.isChild);

			final Uri uri = this.getIntent().getData();
			if (uri == null) {
				this.getContentResolver().insert(
						DataProvider.Rules.CONTENT_URI, cv);
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
		case R.id.type_help:
			this.showHelp(R.string.what_help);
			break;
		case R.id.negate_help:
			this.showHelp(R.string.negate_help);
			break;
		case R.id.what0_help:
			this.showHelp(R.string.what0_help);
			break;
		case R.id.what1_help:
			this.showHelp(R.string.what1_help);
			break;
		case R.id.plan_help:
			this.showHelp(R.string.plan_help);
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
