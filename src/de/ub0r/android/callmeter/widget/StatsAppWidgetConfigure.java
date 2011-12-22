/*
 * Copyright (C) 2010-2011 Felix Bechstein
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

import yuku.ambilwarna.AmbilWarnaDialog;
import yuku.ambilwarna.AmbilWarnaDialog.OnAmbilWarnaListener;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import de.ub0r.android.callmeter.R;
import de.ub0r.android.callmeter.data.DataProvider;
import de.ub0r.android.lib.Log;
import de.ub0r.android.lib.Utils;

/**
 * Configure a stats widget.
 * 
 * @author flx
 */
public final class StatsAppWidgetConfigure extends Activity implements
		OnClickListener, OnCheckedChangeListener, OnSeekBarChangeListener {
	/** Tag for logging. */
	private static final String TAG = "wdgtcfg";

	/** Widget id. */
	private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	/** {@link Spinner} holding the plan. */
	private Spinner spinner;

	/** {@link CheckBox}s. */
	private CheckBox cbHideName, cbShowShortname, cbShowCost, cbShowBillp,
			cbShowIcon, cbSmallWidget;
	/** {@link EditText}s. */
	private EditText etPlanTextSize, etStatsTextSize;
	/** {@link Button}s. */
	private Button btnTextColor, btnBgColor;
	/** {@link View}s. */
	private View vTextColor, vBgColor;
	/** {@link SeekBar}. */
	private SeekBar sbBgTransparency;

	/** Default text size. */
	static final float DEFAULT_TEXTSIZE = 10f;
	/** Default text color. */
	static final int DEFAULT_TEXTCOLOR = 0xffffffff;
	/** Default background color. */
	static final int DEFAULT_BGCOLOR = 0x80000000;
	/** Bit mask for colors. */
	private static final int BITMASK_COLOR = 0x00FFFFFF;
	/** Shift for transparency. */
	private static final int BITSHIFT_TRANSPARENCY = 24;

	/** Projection for {@link SimpleCursorAdapter} query. */
	private static final String[] PROJ_ADAPTER = new String[] {
			DataProvider.Plans.ID, DataProvider.Plans.NAME,
			DataProvider.Plans.SHORTNAME };

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		this.setTheme(R.style.Theme_SherlockUb0r);
		Utils.setLocale(this);
		super.onCreate(savedInstanceState);
		this.setTitle(this.getString(R.string.app_name) + " > "
				+ this.getString(R.string.widget_config_));
		this.setContentView(R.layout.stats_appwidget_config);
		this.spinner = (Spinner) this.findViewById(R.id.spinner);
		this.cbHideName = (CheckBox) this.findViewById(R.id.hide_name);
		this.cbHideName
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(
							final CompoundButton buttonView,
							final boolean isChecked) {
						StatsAppWidgetConfigure.this.cbShowShortname
								.setEnabled(!isChecked);
					}
				});
		this.cbShowShortname = (CheckBox) this.findViewById(R.id.shortname);
		this.cbShowShortname.setOnCheckedChangeListener(this);
		this.cbShowCost = (CheckBox) this.findViewById(R.id.cost);
		this.cbShowBillp = (CheckBox) this.findViewById(R.id.pbillp);
		this.cbShowIcon = (CheckBox) this.findViewById(R.id.show_icon);
		this.cbSmallWidget = (CheckBox) this.findViewById(R.id.small_widget);
		this.etPlanTextSize = (EditText) this
				.findViewById(R.id.widget_plan_textsize);
		this.etStatsTextSize = (EditText) this
				.findViewById(R.id.widget_stats_textsize);
		this.btnTextColor = (Button) this.findViewById(R.id.textcolor);
		this.btnBgColor = (Button) this.findViewById(R.id.bgcolor);
		this.vTextColor = this.findViewById(R.id.textcolorfield);
		this.vBgColor = this.findViewById(R.id.bgcolorfield);
		this.sbBgTransparency = (SeekBar) this
				.findViewById(R.id.bgtransparency);
		this.setAdapter();
		this.findViewById(R.id.ok).setOnClickListener(this);
		this.findViewById(R.id.cancel).setOnClickListener(this);
		this.btnTextColor.setOnClickListener(this);
		this.btnBgColor.setOnClickListener(this);
		this.sbBgTransparency.setOnSeekBarChangeListener(this);
		this.setTextColor(DEFAULT_TEXTCOLOR);
		this.setBgColor(DEFAULT_BGCOLOR, false);
	}

	/** Set {@link SimpleCursorAdapter} for {@link Spinner}. */
	private void setAdapter() {
		final Cursor c = this.getContentResolver().query(
				DataProvider.Plans.CONTENT_URI, PROJ_ADAPTER,
				DataProvider.Plans.WHERE_PLANS, null, DataProvider.Plans.NAME);
		String[] fieldName;
		if (this.cbShowShortname.isChecked()) {
			fieldName = new String[] { DataProvider.Plans.SHORTNAME };
		} else {
			fieldName = new String[] { DataProvider.Plans.NAME };
		}
		final SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
				android.R.layout.simple_spinner_item, c, fieldName,
				new int[] { android.R.id.text1 });
		final int pos = this.spinner.getSelectedItemPosition();
		this.spinner.setAdapter(adapter);
		if (pos >= 0 && pos < this.spinner.getCount()) {
			this.spinner.setSelection(pos);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onResume() {
		super.onResume();
		Utils.setLocale(this);

		final Intent intent = this.getIntent();
		if (intent != null) {
			this.mAppWidgetId = intent.getIntExtra(
					AppWidgetManager.EXTRA_APPWIDGET_ID,
					AppWidgetManager.INVALID_APPWIDGET_ID);
		}
		this.load();
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
			editor.putBoolean(StatsAppWidgetProvider.WIDGET_HIDETNAME
					+ this.mAppWidgetId, this.cbHideName.isChecked());
			editor.putBoolean(StatsAppWidgetProvider.WIDGET_SHORTNAME
					+ this.mAppWidgetId, this.cbShowShortname.isChecked());
			editor.putBoolean(StatsAppWidgetProvider.WIDGET_COST
					+ this.mAppWidgetId, this.cbShowCost.isChecked());
			editor.putBoolean(StatsAppWidgetProvider.WIDGET_BILLPERIOD
					+ this.mAppWidgetId, this.cbShowBillp.isChecked());
			editor.putBoolean(StatsAppWidgetProvider.WIDGET_ICON
					+ this.mAppWidgetId, this.cbShowIcon.isChecked());
			editor.putBoolean(StatsAppWidgetProvider.WIDGET_SMALL
					+ this.mAppWidgetId, this.cbSmallWidget.isChecked());
			editor.putFloat(StatsAppWidgetProvider.WIDGET_STATS_TEXTSIZE
					+ this.mAppWidgetId, Utils.parseFloat(this.etStatsTextSize
					.getText().toString(), DEFAULT_TEXTSIZE));
			editor.putFloat(StatsAppWidgetProvider.WIDGET_PLAN_TEXTSIZE
					+ this.mAppWidgetId, Utils.parseFloat(this.etPlanTextSize
					.getText().toString(), DEFAULT_TEXTSIZE));
			editor.putInt(StatsAppWidgetProvider.WIDGET_TEXTCOLOR
					+ this.mAppWidgetId, this.getTextColor());
			editor.putInt(StatsAppWidgetProvider.WIDGET_BGCOLOR
					+ this.mAppWidgetId, this.getBgColor());
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
		case R.id.textcolor:
			new AmbilWarnaDialog(this, this.getBgColor(),
					new OnAmbilWarnaListener() {
						@Override
						public void onOk(final AmbilWarnaDialog dialog, // .
								final int color) {
							StatsAppWidgetConfigure.this.setTextColor(color);
						}

						@Override
						public void onCancel(final AmbilWarnaDialog dialog) {
							// nothing to do
						}

						public void onReset(final AmbilWarnaDialog dialog) {
							StatsAppWidgetConfigure.this
									.setTextColor(DEFAULT_TEXTCOLOR);
						}
					}).show();
			break;
		case R.id.bgcolor:
			new AmbilWarnaDialog(this, this.getBgColor(),
					new OnAmbilWarnaListener() {
						@Override
						public void onOk(final AmbilWarnaDialog dialog, // .
								final int color) {
							StatsAppWidgetConfigure.this.setBgColor(color,
									false);
						}

						@Override
						public void onCancel(final AmbilWarnaDialog dialog) {
							// nothing to do
						}

						public void onReset(final AmbilWarnaDialog dialog) {
							StatsAppWidgetConfigure.this.setBgColor(
									DEFAULT_BGCOLOR, false);
						}
					}).show();
			break;
		default:
			break;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCheckedChanged(final CompoundButton buttonView,
			final boolean isChecked) {
		switch (buttonView.getId()) {
		case R.id.shortname:
			this.setAdapter();
			return;
		default:
			return;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onProgressChanged(final SeekBar seekBar, final int progress,
			final boolean fromUser) {
		Log.d(TAG, "onProgressChanged(" + progress + ")");
		final int tp = 255 - progress;
		int c = this.getBgColor();
		Log.d(TAG, "color: " + c);
		c = c & BITMASK_COLOR;
		Log.d(TAG, "color: " + c);
		Log.i(TAG, "transparency: " + Integer.toHexString(// .
				tp << BITSHIFT_TRANSPARENCY));
		c = c | tp << BITSHIFT_TRANSPARENCY;
		Log.d(TAG, "color: " + c);
		this.setBgColor(c, true);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onStartTrackingTouch(final SeekBar seekBar) {
		// nothing todo
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onStopTrackingTouch(final SeekBar seekBar) {
		// nothing todo
	}

	/**
	 * Load widget's configuration.
	 */
	private void load() {
		SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(this);
		long pid = p.getLong(StatsAppWidgetProvider.WIDGET_PLANID
				+ this.mAppWidgetId, -1);
		SpinnerAdapter adapter = this.spinner.getAdapter();
		int l = this.spinner.getCount();
		for (int i = 0; i < l; i++) {
			if (adapter.getItemId(i) == pid) {
				this.spinner.setSelection(i);
				break;
			}
		}
		this.cbHideName.setChecked(p.getBoolean(
				StatsAppWidgetProvider.WIDGET_HIDETNAME + this.mAppWidgetId,
				false));
		this.cbShowShortname.setChecked(p.getBoolean(
				StatsAppWidgetProvider.WIDGET_SHORTNAME + this.mAppWidgetId,
				false));
		this.cbShowCost.setChecked(p.getBoolean(
				StatsAppWidgetProvider.WIDGET_COST + this.mAppWidgetId, false));
		this.cbShowBillp.setChecked(p.getBoolean(
				StatsAppWidgetProvider.WIDGET_BILLPERIOD + this.mAppWidgetId,
				false));
		this.cbShowIcon.setChecked(p.getBoolean(
				StatsAppWidgetProvider.WIDGET_ICON + this.mAppWidgetId, false));
		this.cbSmallWidget
				.setChecked(p.getBoolean(StatsAppWidgetProvider.WIDGET_SMALL
						+ this.mAppWidgetId, false));
		float f = p.getFloat(StatsAppWidgetProvider.WIDGET_STATS_TEXTSIZE
				+ this.mAppWidgetId, -1);
		if (f > 0f && f != DEFAULT_TEXTSIZE) {
			this.etStatsTextSize.setText(String.valueOf(f));
		} else {
			this.etStatsTextSize.setText(null);
		}
		f = p.getFloat(StatsAppWidgetProvider.WIDGET_PLAN_TEXTSIZE
				+ this.mAppWidgetId, -1);
		if (f > 0f && f != DEFAULT_TEXTSIZE) {
			this.etPlanTextSize.setText(String.valueOf(f));
		} else {
			this.etPlanTextSize.setText(null);
		}
		this.setTextColor(p.getInt(StatsAppWidgetProvider.WIDGET_TEXTCOLOR
				+ this.mAppWidgetId, DEFAULT_TEXTCOLOR));
		this.setBgColor(
				p.getInt(StatsAppWidgetProvider.WIDGET_BGCOLOR
						+ this.mAppWidgetId, DEFAULT_BGCOLOR), false);
	}

	/**
	 * Get background color currently set.
	 * 
	 * @return color
	 */
	private int getBgColor() {
		return Long.decode(this.btnBgColor.getText().toString()).intValue();
	}

	/**
	 * Set the background color to btnBgColor and vBgColorField.
	 * 
	 * @param color
	 *            color to set
	 * @param fromProgressBar
	 *            true, if setColor is called from onProgessChanged()
	 */
	private void setBgColor(final int color, final boolean fromProgressBar) {
		Log.d(TAG, "setBgColor(" + color + ", " + fromProgressBar + ")");
		String hex = "#" + Integer.toHexString(color);
		Log.d(TAG, "color: " + hex);
		while (hex.length() < 9) {
			hex = "#0" + hex.substring(1);
			Log.d(TAG, "color: " + hex);
		}
		this.btnBgColor.setText(hex);
		this.vBgColor.setBackgroundColor(color);
		if (!fromProgressBar) {
			int trans = color >> BITSHIFT_TRANSPARENCY;
			Log.d(TAG, "transparency: " + trans);
			if (trans < 0) {
				trans = 256 + trans;
				Log.d(TAG, "transparency: " + trans);
			}
			this.sbBgTransparency.setProgress(255 - trans);
		}
	}

	/**
	 * Get text color currently set.
	 * 
	 * @return color
	 */
	private int getTextColor() {
		return Long.decode(this.btnTextColor.getText().toString()).intValue();
	}

	/**
	 * Set the text color to btnTextColor and vTextColorField.
	 * 
	 * @param color
	 *            color to set
	 */
	private void setTextColor(final int color) {
		Log.d(TAG, "setTextColor(" + color + ")");
		String hex = "#" + Integer.toHexString(color);
		Log.d(TAG, "color: " + hex);
		while (hex.length() < 9) {
			hex = "#0" + hex.substring(1);
			Log.d(TAG, "color: " + hex);
		}
		this.btnTextColor.setText(hex);
		this.vTextColor.setBackgroundColor(color);
	}
}
