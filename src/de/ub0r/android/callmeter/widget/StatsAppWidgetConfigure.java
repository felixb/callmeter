/*
 * Copyright (C) 2010 Felix Bechstein
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
package de.ub0r.android.callmeter.widget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import de.ub0r.android.callmeter.R;
import de.ub0r.android.callmeter.data.DataProvider;

/**
 * Configure a stats widget.
 * 
 * @author flx
 */
public final class StatsAppWidgetConfigure extends Activity implements
		OnClickListener {
	/** Widget id. */
	private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	/** {@link Spinner} holding the plan. */
	private Spinner spinner;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setTitle(this.getString(R.string.app_name) + " > "
				+ this.getString(R.string.widget_config_));
		this.setContentView(R.layout.stats_appwidget_config);
		this.spinner = (Spinner) this.findViewById(R.id.spinner);
		final Cursor c = this.getContentResolver()
				.query(
						DataProvider.Plans.CONTENT_URI,
						new String[] { DataProvider.Plans.ID,
								DataProvider.Plans.NAME },
						DataProvider.Plans.WHERE_REALPLANS, null,
						DataProvider.Plans.NAME);
		final SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
				android.R.layout.simple_spinner_item, c,
				new String[] { DataProvider.Plans.NAME },
				new int[] { android.R.id.text1 });
		this.spinner.setAdapter(adapter);
		this.findViewById(R.id.ok).setOnClickListener(this);
		this.findViewById(R.id.cancel).setOnClickListener(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onResume() {
		super.onResume();

		final Intent intent = this.getIntent();
		final Bundle extras = intent.getExtras();
		if (extras != null) {
			this.mAppWidgetId = extras.getInt(
					AppWidgetManager.EXTRA_APPWIDGET_ID,
					AppWidgetManager.INVALID_APPWIDGET_ID);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void onClick(final View v) {
		switch (v.getId()) {
		case R.id.ok:
			SharedPreferences.Editor editor = PreferenceManager
					.getDefaultSharedPreferences(this).edit();
			editor.putLong(StatsAppWidgetProvider.WIDGET_PLANID
					+ this.mAppWidgetId, this.spinner.getSelectedItemId());
			editor.commit();

			final AppWidgetManager appWidgetManager = AppWidgetManager
					.getInstance(this);
			StatsAppWidgetProvider.updateWidget(this, appWidgetManager,
					this.mAppWidgetId);

			final Intent resultValue = new Intent();
			resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
					this.mAppWidgetId);
			this.setResult(RESULT_OK, resultValue);
			this.finish();
			break;
		case R.id.cancel:
			this.finish();
			break;
		default:
			break;
		}
	}
}
