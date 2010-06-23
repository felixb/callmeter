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

import android.app.ListActivity;
import android.app.AlertDialog.Builder;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import de.ub0r.android.callmeter.R;
import de.ub0r.android.callmeter.data.DataProvider;

/**
 * {@link ListActivity} for setting plans.
 * 
 * @author flx
 */
public class Plans extends ListActivity implements OnClickListener,
		OnItemClickListener {
	/** Tag for output. */
	public static final String TAG = "prefs.plans";

	/** Plans. */
	private PlanAdapter adapter = null;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.prefs_plans);
		this.adapter = new PlanAdapter(this);
		this.setListAdapter(this.adapter);
		this.getListView().setOnItemClickListener(this);
	}

	/**
	 * Swap two plans.
	 * 
	 * @param position
	 *            Position of selected plan
	 * @param direction
	 *            Direction to swap the plans. up: -1, down: +1
	 */
	private void swap(final int position, final int direction) {
		final ContentResolver cr = this.getContentResolver();
		Cursor cursor = null;
		// get plans
		final String idCurrent = String.valueOf(this.adapter
				.getItemId(position));
		final String idOther = String.valueOf(this.adapter.getItemId(position
				+ direction));
		cursor = cr.query(DataProvider.Plans.CONTENT_URI.buildUpon()
				.appendPath(idCurrent).build(), DataProvider.Plans.PROJECTION,
				null, null, null);
		if (cursor == null || !cursor.moveToFirst()) {
			return;
		}
		final int orderCurrent = cursor.getInt(DataProvider.Plans.INDEX_ORDER);
		cursor.close();
		cursor = cr.query(DataProvider.Plans.CONTENT_URI.buildUpon()
				.appendPath(idOther).build(), DataProvider.Plans.PROJECTION,
				null, null, null);
		if (cursor == null || !cursor.moveToFirst()) {
			return;
		}
		final int orderOther = cursor.getInt(DataProvider.Plans.INDEX_ORDER);
		cursor.close();

		// set new order
		final ContentValues cvCurrent = new ContentValues();
		ContentValues cvOther = null;
		if (orderCurrent == orderOther) {
			cvCurrent.put(DataProvider.Plans.ORDER, orderCurrent + direction);
		} else {
			cvOther = new ContentValues();
			cvCurrent.put(DataProvider.Plans.ORDER, orderOther);
			cvOther.put(DataProvider.Plans.ORDER, orderCurrent);
		}

		// push changes
		cr.update(DataProvider.Plans.CONTENT_URI.buildUpon().appendPath(
				String.valueOf(idCurrent)).build(), cvCurrent, null, null);
		if (cvOther != null) {
			cr.update(DataProvider.Plans.CONTENT_URI.buildUpon().appendPath(
					String.valueOf(idOther)).build(), cvOther, null, null);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onItemClick(final AdapterView<?> parent, final View view,
			final int position, final long id) {
		final Builder builder = new Builder(this);
		builder.setItems(R.array.prefs_plans_dialog,
				new android.content.DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog,
							final int which) {
						switch (which) {
						case 0: // set
							final Intent intent = new Intent(// .
									Plans.this, PlanEdit.class);
							intent.setData(DataProvider.Plans.CONTENT_URI
									.buildUpon().appendPath(String.valueOf(id))
									.build());
							Plans.this.startActivity(intent);
							break;
						case 1: // up
							Plans.this.swap(position, -1);
							break;
						case 2: // down
							Plans.this.swap(position, 1);
							break;
						default:
							break;
						}
					}
				});
		builder.show();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onClick(final View v) {
		switch (v.getId()) {
		case R.id.ok:
			final Intent intent = new Intent(this, PlanEdit.class);
			this.startActivity(intent);
			break;
		case R.id.add:
			this.finish();
			break;
		default:
			break;
		}
	}
}
