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
import android.app.AlertDialog.Builder;
import android.content.ContentUris;
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
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemClickListener;
import de.ub0r.android.callmeter.R;
import de.ub0r.android.callmeter.data.DataProvider;
import de.ub0r.android.callmeter.ui.prefs.Preference.BoolPreference;
import de.ub0r.android.callmeter.ui.prefs.Preference.CursorPreference;
import de.ub0r.android.callmeter.ui.prefs.Preference.ListPreference;
import de.ub0r.android.callmeter.ui.prefs.Preference.TextPreference;
import de.ub0r.android.lib.DbUtils;

/**
 * Edit a single Plan.
 * 
 * @author flx
 */
public class RuleEdit extends ListActivity implements OnClickListener,
		OnItemClickListener, OnDismissListener {
	/** {@link PreferenceAdapter}. */
	private PreferenceAdapter adapter = null;

	/** Activity result request id: rule. */
	private static final int REQUEST_RULE = 0;
	/** Activity result request id: numbers. */
	private static final int REQUEST_NUMBERS = 1;
	/** Activity result request id: numbers. */
	private static final int REQUEST_HOURS = 2;

	/** Item menu: edit. */
	private static final int WHICH_SELECT = 0;
	/** Item menu: edit. */
	private static final int WHICH_EDIT = 1;
	/** Item menu: delete. */
	private static final int WHICH_DELETE = 2;

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
		this.setContentView(R.layout.list_ok_cancel);

		this.findViewById(R.id.ok).setOnClickListener(this);
		this.findViewById(R.id.cancel).setOnClickListener(this);

		this.fillFields();
		this.fillWhat1();
		this.fillWhat0();
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
			switch (requestCode) {
			case REQUEST_RULE:
				if (uri == null) {
					this.what1 = -1;
				} else {
					this.what1 = ContentUris.parseId(uri);
				}
				this.fillWhat1();
				break;
			case REQUEST_HOURS:
			case REQUEST_NUMBERS:
				if (uri == null) {
					this.what0 = -1;
				} else {
					this.what0 = ContentUris.parseId(uri);
				}
				this.fillWhat0();
				break;
			default:
				break;
			}
		}
	}

	/**
	 * Set text of the what0 {@link Button}.
	 */
	private void fillWhat0() {
		if (1 == 1) {
			return; // FIXME
		}
		if (this.what0 < 0) {
			this.btnWhat0.setText(R.string.none);
		} else {
			switch (this.spWhat.getSelectedItemPosition()) {
			case DataProvider.Rules.WHAT_HOURS:
				this.btnWhat0.setText(DataProvider.HoursGroup.getName(this
						.getContentResolver(), this.what0));
				break;
			case DataProvider.Rules.WHAT_NUMBERS:
				this.btnWhat0.setText(DataProvider.NumbersGroup.getName(this
						.getContentResolver(), this.what0));
				break;
			default:
				this.btnWhat0.setText(R.string.none);
				break;
			}
		}
	}

	/**
	 * Set text of the what1 {@link Button}.
	 */
	private void fillWhat1() {
		if (1 == 1) {
			return; // FIXME
		}
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
		if (1 == 1) {
			return; // FIXME
		}
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
					+ DataProvider.TYPE_BILLPERIOD + " AND "
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
		this.isChild = this.getIntent().getBooleanExtra(EXTRA_ISCHILD, false);
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

				this.adapter = new PreferenceAdapter(this);
				this.adapter.add(new TextPreference(this,
						DataProvider.Rules.NAME, "", R.string.name_,
						R.string.name_help, InputType.TYPE_NULL));
				this.adapter
						.add(new ListPreference(this, DataProvider.Rules.WHAT,
								DataProvider.Rules.WHAT_CALL, R.string.what_,
								R.string.what_help, R.array.rules_type));
				this.adapter.add(new CursorPreference(this,
						DataProvider.Rules.WHAT1, R.string.what0_,
						R.string.what0_help, R.string.edit_selected_,
						R.string.new_, -1, DataProvider.HoursGroup.CONTENT_URI,
						DataProvider.HoursGroup.ID,
						DataProvider.HoursGroup.NAME, null, null, null, null)); // FIXME
				this.adapter.add(new BoolPreference(this,
						DataProvider.Rules.NOT, R.string.negate_,
						R.string.negate_help, this));
				this.adapter.add(new CursorPreference(this,
						DataProvider.Rules.PLAN_ID, R.string.plan_,
						R.string.plan_help, -1, -1, -1,
						DataProvider.Plans.CONTENT_URI, DataProvider.Plans.ID,
						DataProvider.Plans.NAME, null, null, null, null));
				this.adapter.add(new CursorPreference(this,
						DataProvider.Rules.WHAT1, R.string.what1_,
						R.string.what1_help, R.string.edit_selected_,
						R.string.new_, R.string.clear_,
						DataProvider.Rules.CONTENT_URI, DataProvider.Rules.ID,
						DataProvider.Rules.NAME, DbUtils.sqlAnd(
								DataProvider.Rules.ISCHILD + " = 1",
								DataProvider.Rules.ID + " != "
										+ DataProvider.Rules.WHAT1),
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(final DialogInterface dialog,
									final int which) {
								final long sel = ((CursorPreference) // .
								RuleEdit.this.adapter
										.getPreference(DataProvider.// .
										Rules.WHAT1)).getValue();
								if (sel < 0) {
									return;
								}
								final Intent fi = new Intent(
										Intent.ACTION_VIEW,
										ContentUris.withAppendedId(
												DataProvider.Rules.CONTENT_URI,
												sel), RuleEdit.this,
										RuleEdit.class);
								fi.putExtra(EXTRA_ISCHILD, true);
								RuleEdit.this.startActivityForResult(fi,
										REQUEST_RULE);
							}
						}, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(final DialogInterface dialog,
									final int which) {
								final Intent fi = new Intent(RuleEdit.this,
										RuleEdit.class);
								fi.putExtra(EXTRA_ISCHILD, true);
								RuleEdit.this.startActivityForResult(fi,
										REQUEST_RULE);
							}
						}, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(final DialogInterface dialog,
									final int which) {
								((CursorPreference) RuleEdit.this.adapter
										.getPreference(DataProvider.// .
										Rules.WHAT1)).clearValue();
								// FIXME
							}
						}));
				this.adapter.load(cursor);
				this.setListAdapter(this.adapter);
				this.getListView().setOnItemClickListener(this);
			} else {
				cursor.close();
				return;
			}
		}
		cursor.close();
	}

	/**
	 * Show or hide fields based on data in there.
	 */
	private void showHideFileds() {
		if (1 == 1) {
			return; // FIXME
		}

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
		// this.findViewById(R.id.what0_layout).setVisibility(v);

		switch (t) {
		case DataProvider.Rules.WHAT_INCOMMING:
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
		// this.findViewById(R.id.negate_layout).setVisibility(v);

		if (this.isChild) {
			v = View.GONE;
		} else {
			v = View.VISIBLE;
		}
		// this.findViewById(R.id.plan_layout).setVisibility(v);
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
		case -2: // R.id.what0_btn:
			int rt = -1;
			Intent ie = null;
			if (t == DataProvider.Rules.WHAT_NUMBERS) {
				intent = new Intent(this, NumberGroups.class);
				ie = new Intent(this, NumberGroupEdit.class);
				ie.setData(ContentUris.withAppendedId(
						DataProvider.NumbersGroup.CONTENT_URI, this.what0));
				rt = REQUEST_NUMBERS;
			} else if (t == DataProvider.Rules.WHAT_HOURS) {
				intent = new Intent(this, HourGroups.class);
				ie = new Intent(this, HourGroupEdit.class);
				ie.setData(ContentUris.withAppendedId(
						DataProvider.HoursGroup.CONTENT_URI, this.what0));
				rt = REQUEST_HOURS;
			}
			if (this.what0 < 0) {
				this.startActivityForResult(intent, rt);
			} else {
				final Intent fi = intent;
				final Intent fie = ie;
				final int r = rt;
				final Builder builder = new Builder(this);
				builder.setItems(R.array.dialog_select_edit_delete,
						new android.content.DialogInterface.OnClickListener() {
							@Override
							public void onClick(final DialogInterface dialog,
									final int which) {
								switch (which) {
								case WHICH_SELECT:
									RuleEdit.this.startActivityForResult(// .
											fi, r);
									break;
								case WHICH_EDIT:
									RuleEdit.this.startActivity(fie);
									break;
								case WHICH_DELETE:
									// TODO: delete old child from DB
									RuleEdit.this.what0 = -1;
									RuleEdit.this.fillWhat0();
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
		case -1: // R.id.what1_btn:
			final Intent fi = new Intent(this, RuleEdit.class);
			fi.putExtra(EXTRA_ISCHILD, true);
			if (this.what1 >= 0) {
				fi.setData(ContentUris.withAppendedId(
						DataProvider.Rules.CONTENT_URI, this.what1));

				final Builder builder = new Builder(this);
				builder.setItems(R.array.dialog_edit_delete,
						new android.content.DialogInterface.OnClickListener() {
							@Override
							public void onClick(final DialogInterface dialog,
									final int which) {
								switch (which) {
								case 0:
									RuleEdit.this.startActivityForResult(// .
											fi, REQUEST_RULE);
									break;
								case 1:
									// TODO: delete old child from DB
									RuleEdit.this.what1 = -1;
									RuleEdit.this.fillWhat1();
									break;
								default:
									break;
								}
							}
						});
				builder.setNegativeButton(android.R.string.cancel, null);
				builder.show();
			} else {
				this.startActivityForResult(fi, REQUEST_RULE);
			}
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
