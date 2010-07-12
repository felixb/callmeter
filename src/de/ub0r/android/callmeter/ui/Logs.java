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
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
public final class Logs extends ListActivity implements OnClickListener {
	/** Tag for output. */
	public static final String TAG = "logs";

	/** Prefs: {@link ToggleButton} state for calls. */
	private static final String PREF_CALL = "_logs_call";
	/** Prefs: {@link ToggleButton} state for sms. */
	private static final String PREF_SMS = "_logs_sms";
	/** Prefs: {@link ToggleButton} state for mms. */
	private static final String PREF_MMS = "_logs_mms";
	/** Prefs: {@link ToggleButton} state for data. */
	private static final String PREF_DATA = "_logs_data";

	/** {@link ToggleButton}s. */
	private ToggleButton tbCall, tbSMS, tbMMS, tbData;

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
			long amount = cursor.getLong(DataProvider.Logs.INDEX_AMOUNT);
			buf.append(Plans.formatAmount(t, amount));

			((TextView) view.findViewById(android.R.id.text1)).setText(buf
					.toString());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setTheme(Preferences.getTheme(this));
		this.setContentView(R.layout.logs);
		this.setTitle(R.string.logs);
		this.tbCall = (ToggleButton) this.findViewById(R.id.calls);
		this.tbCall.setOnClickListener(this);
		this.tbSMS = (ToggleButton) this.findViewById(R.id.sms);
		this.tbSMS.setOnClickListener(this);
		this.tbMMS = (ToggleButton) this.findViewById(R.id.mms);
		this.tbMMS.setOnClickListener(this);
		this.tbData = (ToggleButton) this.findViewById(R.id.data);
		this.tbData.setOnClickListener(this);
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(this);
		this.tbCall.setChecked(p.getBoolean(PREF_CALL, true));
		this.tbSMS.setChecked(p.getBoolean(PREF_SMS, true));
		this.tbMMS.setChecked(p.getBoolean(PREF_MMS, true));
		this.tbData.setChecked(p.getBoolean(PREF_DATA, true));

		this.setAdapter();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onStop() {
		super.onStop();
		final Editor e = PreferenceManager.getDefaultSharedPreferences(this)
				.edit();
		e.putBoolean(PREF_CALL, this.tbCall.isChecked());
		e.putBoolean(PREF_SMS, this.tbSMS.isChecked());
		e.putBoolean(PREF_MMS, this.tbMMS.isChecked());
		e.putBoolean(PREF_DATA, this.tbData.isChecked());
		e.commit();
	}

	/** Set Adapter. */
	private void setAdapter() {
		String where = DataProvider.Logs.TYPE + " < 0 ";
		if (this.tbCall.isChecked()) {
			where += " OR " + DataProvider.Logs.TYPE + " = "
					+ DataProvider.TYPE_CALL;
		}
		if (this.tbSMS.isChecked()) {
			where += " OR " + DataProvider.Logs.TYPE + " = "
					+ DataProvider.TYPE_SMS;
		}
		if (this.tbMMS.isChecked()) {
			where += " OR " + DataProvider.Logs.TYPE + " = "
					+ DataProvider.TYPE_MMS;
		}
		if (this.tbData.isChecked()) {
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
