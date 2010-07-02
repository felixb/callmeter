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
package de.ub0r.android.callmeter.ui;

import java.util.Date;

import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import android.widget.ToggleButton;
import de.ub0r.android.callmeter.R;
import de.ub0r.android.callmeter.data.DataProvider;
import de.ub0r.android.callmeter.ui.prefs.Preferences;

/**
 * Callmeter's Log {@link ListActivity}.
 * 
 * @author flx
 */
public class Logs extends ListActivity implements OnClickListener {
	/** Tag for output. */
	public static final String TAG = "logs";

	/**
	 * Adapter binding logs to View.
	 * 
	 * @author flx
	 */
	public class LogAdapter extends ResourceCursorAdapter {

		/**
		 * Default Constructor.
		 * 
		 * @param where
		 *            slection
		 * @param context
		 *            {@link Context}
		 */
		public LogAdapter(final Context context, final String where) {
			super(context, R.layout.logs_item, context.getContentResolver()
					.query(DataProvider.Logs.CONTENT_URI,
							DataProvider.Logs.PROJECTION, where, null,
							DataProvider.Logs.DATE + " DESC"), true);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public final void bindView(final View view, final Context context,
				final Cursor cursor) {
			final ContentResolver cr = context.getContentResolver();
			final StringBuilder buf = new StringBuilder();
			final int t = cursor.getInt(DataProvider.Logs.INDEX_TYPE);
			String[] strs = context.getResources().getStringArray(
					R.array.plans_type);
			buf.append(strs[t]);
			final int dir = cursor.getInt(DataProvider.Logs.INDEX_DIRECTION);
			strs = context.getResources().getStringArray(
					R.array.direction_calls);
			buf.append(" (" + strs[dir] + "): ");
			final long date = cursor.getLong(DataProvider.Logs.INDEX_DATE);
			buf
					.append(DateFormat.getDateFormat(context).format(
							new Date(date)));
			buf.append(" ");
			buf
					.append(DateFormat.getTimeFormat(context).format(
							new Date(date)));
			buf.append("\n");
			buf.append(DataProvider.Plans.getName(cr, cursor
					.getLong(DataProvider.Logs.INDEX_PLAN_ID)));
			buf.append("\t");
			buf.append(DataProvider.Rules.getName(cr, cursor
					.getLong(DataProvider.Logs.INDEX_RULE_ID)));
			buf.append("\t");
			buf.append(cursor.getString(DataProvider.Logs.INDEX_REMOTE));
			buf.append("\t");
			buf.append(cursor.getString(DataProvider.Logs.INDEX_AMOUNT));

			((TextView) view.findViewById(android.R.id.text1)).setText(buf
					.toString());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setTheme(Preferences.getTheme(this));
		this.setContentView(R.layout.logs);
		this.setTitle(R.string.logs);
		this.findViewById(R.id.calls).setOnClickListener(this);
		this.findViewById(R.id.sms).setOnClickListener(this);
		this.findViewById(R.id.mms).setOnClickListener(this);
		this.findViewById(R.id.data).setOnClickListener(this);
		this.setAdapter();
	}

	/** Set Adapter. */
	private void setAdapter() {
		String where = DataProvider.Logs.TYPE + " < 0 ";
		if (((ToggleButton) this.findViewById(R.id.calls)).isChecked()) {
			where += " OR " + DataProvider.Logs.TYPE + " = "
					+ DataProvider.TYPE_CALL;
		}
		if (((ToggleButton) this.findViewById(R.id.sms)).isChecked()) {
			where += " OR " + DataProvider.Logs.TYPE + " = "
					+ DataProvider.TYPE_SMS;
		}
		if (((ToggleButton) this.findViewById(R.id.mms)).isChecked()) {
			where += " OR " + DataProvider.Logs.TYPE + " = "
					+ DataProvider.TYPE_MMS;
		}
		if (((ToggleButton) this.findViewById(R.id.data)).isChecked()) {
			where += " OR " + DataProvider.Logs.TYPE + " = "
					+ DataProvider.TYPE_DATA;
		}

		this.setListAdapter(new LogAdapter(this, where));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onClick(final View v) {
		this.setAdapter();
	}
}
