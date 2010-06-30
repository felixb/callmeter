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
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import de.ub0r.android.callmeter.R;
import de.ub0r.android.callmeter.data.DataProvider;

/**
 * Edit a single Plan.
 * 
 * @author flx
 */
public class NumberGroupEdit extends ListActivity implements OnClickListener,
		OnItemClickListener {

	/** Item menu: edit. */
	private static final int WHICH_EDIT = 0;
	/** Item menu: delete. */
	private static final int WHICH_DELETE = 1;

	/** {@link EditText} holding name. */
	private EditText etName = null;

	/** Id of edited filed. */
	private long id = -1;

	/**
	 * Adapter binding plans to View.
	 * 
	 * @author flx
	 */
	private static class NumberAdapter extends ResourceCursorAdapter {
		/**
		 * Default Constructor.
		 * 
		 * @param context
		 *            {@link Context}
		 * @param id
		 *            id of number group
		 */
		public NumberAdapter(final Context context, final long id) {
			super(context, android.R.layout.simple_list_item_1, context
					.getContentResolver().query(
							ContentUris.withAppendedId(
									DataProvider.Numbers.GROUP_URI, id),
							DataProvider.Numbers.PROJECTION, null, null, null),
					true);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public final void bindView(final View view, final Context ctxt,
				final Cursor cursor) {
			final TextView twTitle = ((TextView) view
					.findViewById(android.R.id.text1));
			twTitle
					.setText(cursor
							.getString(DataProvider.Numbers.INDEX_NUMBER));
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
				+ this.getString(R.string.numbers) + " > "
				+ this.getString(R.string.edit_));
		this.setContentView(R.layout.list_name_ok_add);

		final Uri u = this.getIntent().getData();
		if (u != null) {
			this.id = ContentUris.parseId(u);
		}

		this.setListAdapter(new NumberAdapter(this, this.id));
		this.getListView().setOnItemClickListener(this);

		this.findViewById(R.id.ok).setOnClickListener(this);
		this.findViewById(R.id.add).setOnClickListener(this);
		this.findViewById(R.id.name_help).setOnClickListener(this);

		this.etName = (EditText) this.findViewById(R.id.name_et);
		this.etName.setText(DataProvider.NumbersGroup.getName(this
				.getContentResolver(), this.id));
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
			cv.put(DataProvider.NumbersGroup.NAME, n);

			Uri uri = this.getIntent().getData();
			if (uri == null) {
				uri = this.getContentResolver().insert(
						DataProvider.NumbersGroup.CONTENT_URI, cv);
			} else {
				this.getContentResolver().update(uri, cv, null, null);
			}
			intent = new Intent(this, NumberGroupEdit.class);
			intent.setData(uri);
			this.setResult(RESULT_OK, new Intent(intent));
			this.finish();
			break;
		case R.id.add:
			// TODO: fill me
			break;
		case R.id.name_help:
			this.showHelp(R.string.name_help);
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
		builder.setItems(R.array.prefs_edit_delete,
				new android.content.DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog,
							final int which) {
						switch (which) {
						case WHICH_EDIT:
							// TODO: show dialog
							break;
						case WHICH_DELETE:
							NumberGroupEdit.this.getContentResolver().delete(
									ContentUris.withAppendedId(
											DataProvider.Numbers.CONTENT_URI,
											id), null, null);
							break;
						default:
							break;
						}
					}
				});
		builder.setNegativeButton(android.R.string.cancel, null);
		builder.show();
	}
}
