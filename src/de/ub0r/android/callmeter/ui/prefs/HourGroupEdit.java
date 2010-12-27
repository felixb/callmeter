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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ResourceCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import de.ub0r.android.callmeter.R;
import de.ub0r.android.callmeter.data.DataProvider;
import de.ub0r.android.callmeter.data.RuleMatcher;
import de.ub0r.android.lib.apis.ContactsWrapper;

/**
 * Edit a single Plan.
 * 
 * @author flx
 */
public class HourGroupEdit extends ListActivity implements OnClickListener,
		OnItemClickListener {
	/** {@link ContactsWrapper}. */
	public static final ContactsWrapper CWRAPPER = ContactsWrapper
			.getInstance();

	/** Item menu: edit. */
	private static final int WHICH_EDIT = 0;
	/** Item menu: delete. */
	private static final int WHICH_DELETE = 1;

	/** {@link EditText} holding name. */
	private EditText etName = null;

	/** Id of edited filed. */
	private long gid = -1;

	/**
	 * Adapter binding plans to View.
	 * 
	 * @author flx
	 */
	private class HourAdapter extends ResourceCursorAdapter {
		/** List of weekdays. */
		private final String[] resDays;
		/** List of hours. */
		private final String[] resHours;

		/**
		 * Default Constructor.
		 * 
		 * @param context
		 *            {@link Context}
		 * @param id
		 *            id of Hour group
		 */
		public HourAdapter(final Context context, final long id) {
			super(context, android.R.layout.simple_list_item_1, context
					.getContentResolver().query(
							ContentUris.withAppendedId(
									DataProvider.Hours.GROUP_URI, id),
							DataProvider.Hours.PROJECTION,
							null,
							null,
							DataProvider.Hours.DAY + ", "
									+ DataProvider.Hours.HOUR), true);
			this.resDays = context.getResources().getStringArray(
					R.array.weekdays_all);
			this.resHours = context.getResources().getStringArray(
					R.array.hours_all);
			this.registerDataSetObserver(new DataSetObserver() {
				@Override
				public void onChanged() {
					super.onChanged();
					HourGroupEdit.this.showEmptyHint();
				}
			});
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public final void bindView(final View view, final Context ctxt,
				final Cursor cursor) {
			final TextView twTitle = ((TextView) view
					.findViewById(android.R.id.text1));
			final int day = cursor.getInt(DataProvider.Hours.INDEX_DAY);
			final int hour = cursor.getInt(DataProvider.Hours.INDEX_HOUR);
			twTitle.setText(this.resDays[day] + ": " + this.resHours[hour]);
		}

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setTitle(this.getString(R.string.settings) + " > "
				+ this.getString(R.string.rules) + " > "
				+ this.getString(R.string.hours) + " > "
				+ this.getString(R.string.edit_));
		this.setContentView(R.layout.list_name_ok_help_add);

		final Uri u = this.getIntent().getData();
		if (u != null) {
			this.gid = ContentUris.parseId(u);
		}

		this.setListAdapter(new HourAdapter(this, this.gid));
		this.getListView().setOnItemClickListener(this);

		this.findViewById(R.id.ok).setOnClickListener(this);
		this.findViewById(R.id.add).setOnClickListener(this);
		this.findViewById(R.id.name_help).setOnClickListener(this);
		this.findViewById(R.id.help).setOnClickListener(this);

		this.etName = (EditText) this.findViewById(R.id.name_et);
		this.etName.setText(DataProvider.HoursGroup.getName(this
				.getContentResolver(), this.gid));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void onResume() {
		super.onResume();
		this.showEmptyHint();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void onPause() {
		super.onPause();
		if (this.getListAdapter().getCount() == 0) {
			Toast.makeText(this, R.string.empty_group, Toast.LENGTH_LONG)
					.show();
		}
	}

	/** Set the visibility for the empty groups hint. */
	private void showEmptyHint() {
		int v = View.GONE;
		if (this.getListAdapter().getCount() == 0) {
			v = View.VISIBLE;
		}
		TextView tv = (TextView) this.findViewById(R.id.empty_list);
		tv.setVisibility(v);
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
		Intent intent = null;
		switch (v.getId()) {
		case R.id.ok:
			final String n = this.etName.getText().toString();
			final ContentValues cv = new ContentValues();
			cv.put(DataProvider.HoursGroup.NAME, n);

			Uri uri = this.getIntent().getData();
			if (uri == null) {
				uri = this.getContentResolver().insert(
						DataProvider.HoursGroup.CONTENT_URI, cv);
			} else {
				this.getContentResolver().update(uri, cv, null, null);
			}
			intent = new Intent(this, HourGroupEdit.class);
			intent.setData(uri);
			this.setResult(RESULT_OK, new Intent(intent));
			RuleMatcher.unmatch(this);
			this.finish();
			break;
		case R.id.add:
			this.showHourDialog(-1);
			break;
		case R.id.name_help:
			this.showHelp(R.string.name_help);
			break;
		case R.id.help:
			this.showHelp(R.string.hourgroup_help);
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
		final Builder builder = new Builder(this);
		builder.setItems(R.array.dialog_edit_delete,
				new android.content.DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog,
							final int which) {
						switch (which) {
						case WHICH_EDIT:
							HourGroupEdit.this.showHourDialog(id);
							break;
						case WHICH_DELETE:
							HourGroupEdit.this.getContentResolver().delete(
									ContentUris.withAppendedId(DataProvider.// .
											Hours.CONTENT_URI, id), null, null);
							HourGroupEdit.this.showEmptyHint();
							break;
						default:
							break;
						}
					}
				});
		builder.setNegativeButton(android.R.string.cancel, null);
		builder.show();
	}

	/**
	 * Set a Hour.
	 * 
	 * @param nid
	 *            id of entry
	 * @param day
	 *            day
	 * @param hour
	 *            hour
	 */
	private void setHour(final long nid, final int day, final int hour) {
		final ContentValues cv = new ContentValues();
		cv.put(DataProvider.Hours.GID, this.gid);
		cv.put(DataProvider.Hours.DAY, day);
		cv.put(DataProvider.Hours.HOUR, hour);
		if (nid < 0) {
			this.getContentResolver()
					.insert(DataProvider.Hours.CONTENT_URI, cv);
		} else {
			this.getContentResolver().update(
					ContentUris.withAppendedId(DataProvider.Hours.CONTENT_URI,
							nid), cv, null, null);
		}
	}

	/**
	 * Get a Hour.
	 * 
	 * @param nid
	 *            id of entry
	 * @return Hour
	 */
	private int[] getHour(final long nid) {
		int[] ret = new int[] { 0, 0 };
		final Cursor cursor = this.getContentResolver()
				.query(
						ContentUris.withAppendedId(
								DataProvider.Hours.CONTENT_URI, nid),
						new String[] { DataProvider.Hours.DAY,
								DataProvider.Hours.HOUR }, null, null, null);
		if (cursor != null && cursor.moveToFirst()) {
			ret[0] = cursor.getInt(0);
			ret[1] = cursor.getInt(1);
		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
		return ret;
	}

	/**
	 * Show an add/delete dialog.
	 * 
	 * @param nid
	 *            id of entry
	 */
	private void showHourDialog(final long nid) {
		final Builder builder = new Builder(this);
		final View v = View
				.inflate(this, R.layout.prefs_hourgroup_dialog, null);
		final Spinner spDays = (Spinner) v.findViewById(R.id.days);
		final Spinner spHours = (Spinner) v.findViewById(R.id.hours);
		if (nid >= 0) {
			final int[] h = this.getHour(nid);
			spDays.setSelection(h[0]);
			spHours.setSelection(h[1]);
		}
		builder.setView(v);
		builder.setTitle(R.string.add_hour);
		builder.setCancelable(true);
		builder.setPositiveButton(android.R.string.ok,
				new DialogInterface.OnClickListener() {
					public void onClick(final DialogInterface dialog,
							final int id) {
						HourGroupEdit.this.setHour(nid, spDays
								.getSelectedItemPosition(), spHours
								.getSelectedItemPosition());
						HourGroupEdit.this.showEmptyHint();
					}
				});
		builder.setNegativeButton(android.R.string.cancel,
				new DialogInterface.OnClickListener() {
					public void onClick(final DialogInterface dialog,
							final int id) {
						HourGroupEdit.this.showEmptyHint();
					}
				});
		builder.show();
	}
}
