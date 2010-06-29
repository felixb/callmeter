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
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SimpleCursorAdapter;
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

	/** Activity result request id: rule. */
	private static final int REQUEST_RULE = 0;
	/** Activity result request id: numbers. */
	private static final int REQUEST_NUMBERS = 1;
	/** Activity result request id: numbers. */
	private static final int REQUEST_HOURS = 2;

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
	private long plan = -1;
	/** Data for what0. */
	private long what0 = -1;
	/** Data for what1. */
	private long what1 = -1;
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
		this.fillChild();
		this.fillPlan();
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
	protected final void onActivityResult(final int requestCode,
			final int resultCode, final Intent data) {
		if (resultCode == RESULT_OK) {
			final Uri uri = data.getData();
			if (uri == null) {
				this.what1 = -1;
			} else {
				this.what1 = Long.parseLong(uri.getPathSegments().get(1));
			}
		}
		this.fillChild();
	}

	/**
	 * Set text of the child {@link Button}.
	 */
	private void fillChild() {
		Cursor cursor = null;
		if (this.what1 >= 0) {
			cursor = this.getContentResolver().query(
					ContentUris.withAppendedId(DataProvider.Rules.CONTENT_URI,
							this.what1),
					new String[] { DataProvider.Rules.NAME }, null, null, null);
		}
		if (cursor != null && cursor.moveToFirst()) {
			this.btnWhat1.setText(cursor.getString(0));
		} else {
			this.btnWhat1.setText(R.string.none);
		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
	}

	/**
	 * Set plan {@link Spinner}'s adapter.
	 */
	private void fillPlan() {
		final int t = this.spWhat.getSelectedItemPosition();
		String where = null;
		switch (t) {
		case DataProvider.Rules.WHAT_CALL:
			where = DataProvider.Plans.TYPE + " = " + DataProvider.TYPE_CALL
					+ " OR " + DataProvider.Plans.TYPE + " = "
					+ DataProvider.TYPE_MIXED;
			break;
		case DataProvider.Rules.WHAT_DATA:
			where = DataProvider.Plans.TYPE + " = " + DataProvider.TYPE_DATA;
			break;
		case DataProvider.Rules.WHAT_SMS:
		case DataProvider.Rules.WHAT_MMS:
			where = DataProvider.Plans.TYPE + " = " + DataProvider.TYPE_SMS
					+ " OR " + DataProvider.Plans.TYPE + " = "
					+ DataProvider.TYPE_MMS + " OR " + DataProvider.Plans.TYPE
					+ " = " + DataProvider.TYPE_MIXED;
			break;
		default:
			where = DataProvider.Plans.TYPE + " != "
					+ DataProvider.TYPE_BILLMODE + " AND "
					+ DataProvider.Plans.TYPE + " != "
					+ DataProvider.TYPE_SPACING + " AND "
					+ DataProvider.Plans.TYPE + " != "
					+ DataProvider.TYPE_TITLE;
			break;
		}

		final Cursor cursor = this.getContentResolver()
				.query(
						DataProvider.Plans.CONTENT_URI,
						new String[] { DataProvider.Plans.ID,
								DataProvider.Plans.NAME }, where, null,
						DataProvider.Plans.NAME);
		this.spPlan.setAdapter(new SimpleCursorAdapter(this,
				android.R.layout.simple_spinner_item, cursor,
				new String[] { DataProvider.Plans.NAME },
				new int[] { android.R.id.text1 }));

		if (this.plan >= 0) {
			final Adapter adapter = this.spPlan.getAdapter();
			final int l = adapter.getCount();
			for (int i = 0; i < l; i++) {
				if (adapter.getItemId(i) == this.plan) {
					this.spPlan.setSelection(i);
					break;
				}
			}
		}
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
		this.isChild = this.getIntent().getBooleanExtra(EXTRA_ISCHILD, false);
		// TODO: what0

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
		final int t = this.spWhat.getSelectedItemPosition();
		Intent intent = null;
		switch (v.getId()) {
		case R.id.ok:
			final String n = this.etName.getText().toString();
			final ContentValues cv = new ContentValues();
			cv.put(DataProvider.Rules.NAME, n);
			cv.put(DataProvider.Rules.NOT, this.cbNegate.isChecked());
			cv.put(DataProvider.Rules.WHAT, t);
			cv.put(DataProvider.Rules.WHAT0, this.what0);
			cv.put(DataProvider.Rules.WHAT1, this.what1);
			cv.put(DataProvider.Rules.PLAN_ID, this.plan);
			cv.put(DataProvider.Rules.ISCHILD, this.isChild);

			Uri uri = this.getIntent().getData();
			if (uri == null) {
				uri = this.getContentResolver().insert(
						DataProvider.Rules.CONTENT_URI, cv);
			} else {
				this.getContentResolver().update(uri, cv, null, null);
			}
			this.id = -1;
			intent = new Intent(this, RuleEdit.class);
			intent.setData(uri);
			this.setResult(RESULT_OK, new Intent(intent));
			this.finish();
			break;
		case R.id.cancel:
			this.id = -1;
			this.setResult(RESULT_CANCELED);
			this.finish();
			break;
		case R.id.what0_btn:
			int rt = -1;
			if (t == DataProvider.Rules.WHAT_NUMBERS) {
				intent = new Intent(this, NumberGroups.class);
				rt = REQUEST_NUMBERS;
			} else if (t == DataProvider.Rules.WHAT_HOURS) {
				// intent = new Intent(this, HourGroups.class);
				rt = REQUEST_HOURS;
			}
			if (this.what0 < 0) {
				this.startActivityForResult(intent, rt);
			} else {
				final Intent i = intent;
				final int r = rt;
				final Builder builder = new Builder(this);
				builder.setItems(R.array.prefs_edit_delete,
						new android.content.DialogInterface.OnClickListener() {
							@Override
							public void onClick(final DialogInterface dialog,
									final int which) {
								switch (which) {
								case 0:
									RuleEdit.this.startActivityForResult(// .
											i, r);
									break;
								case 1:
									// TODO: delete old child from DB
									RuleEdit.this.what0 = -1;
									break;
								default:
									break;
								}
							}
						});
				builder.setNegativeButton(android.R.string.cancel, null);
				builder.show();
			}
			break;
		case R.id.what1_btn:
			final Intent i = new Intent(this, RuleEdit.class);
			i.putExtra(EXTRA_ISCHILD, true);
			if (this.what1 >= 0) {
				i.setData(ContentUris.withAppendedId(
						DataProvider.Rules.CONTENT_URI, this.what1));

				final Builder builder = new Builder(this);
				builder.setItems(R.array.prefs_edit_delete,
						new android.content.DialogInterface.OnClickListener() {
							@Override
							public void onClick(final DialogInterface dialog,
									final int which) {
								switch (which) {
								case 0:
									RuleEdit.this.startActivityForResult(// .
											i, REQUEST_RULE);
									break;
								case 1:
									// TODO: delete old child from DB
									RuleEdit.this.what1 = -1;
									RuleEdit.this.fillChild();
									break;
								default:
									break;
								}
							}
						});
				builder.setNegativeButton(android.R.string.cancel, null);
				builder.show();
			} else {
				this.startActivityForResult(i, REQUEST_RULE);
			}
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
		switch (parent.getId()) {
		case R.id.plan_sp:
			this.plan = id;
			break;
		case R.id.type_sp:
			this.showHideFileds();
			this.fillPlan();
		default:
			break;
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
