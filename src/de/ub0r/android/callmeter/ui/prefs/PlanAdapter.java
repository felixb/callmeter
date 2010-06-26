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

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import de.ub0r.android.callmeter.R;
import de.ub0r.android.callmeter.data.DataProvider;

/**
 * Adapter binding plans to View.
 * 
 * @author flx
 */
public class PlanAdapter extends ResourceCursorAdapter {
	/** Tag for output. */
	public static final String TAG = "prefs.pa";

	/** Type of plans. */
	private final String[] types;

	/**
	 * Default Constructor.
	 * 
	 * @param context
	 *            {@link Context}
	 */
	public PlanAdapter(final Context context) {
		super(context, R.layout.prefs_plans_item, context.getContentResolver()
				.query(DataProvider.Plans.CONTENT_URI,
						DataProvider.Plans.PROJECTION, null, null,
						DataProvider.Plans.ORDER), true);
		this.types = context.getResources().getStringArray(R.array.plans_type);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void bindView(final View view, final Context ctxt,
			final Cursor cursor) {
		final TextView twTitle = ((TextView) view.findViewById(R.id.normtitle));
		twTitle.setText(cursor.getString(DataProvider.Plans.INDEX_NAME));
		final TextView twType = ((TextView) view.findViewById(R.id.type));
		twType
				.setText(this.types[cursor
						.getInt(DataProvider.Plans.INDEX_TYPE)]);
	}

}
