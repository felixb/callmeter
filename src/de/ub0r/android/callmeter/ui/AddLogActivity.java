/*
 * Copyright (C) 2009-2013 Felix Bechstein
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

import java.util.Calendar;
import java.util.Date;

import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.ContentValues;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import de.ub0r.android.callmeter.CallMeter;
import de.ub0r.android.callmeter.R;
import de.ub0r.android.callmeter.data.DataProvider;
import de.ub0r.android.callmeter.data.LogRunnerService;
import de.ub0r.android.callmeter.ui.prefs.Preferences;
import de.ub0r.android.lib.Log;
import de.ub0r.android.lib.Utils;

/**
 * Add a log entry.
 * 
 * @author flx
 */
public final class AddLogActivity extends SherlockActivity implements OnClickListener,
		OnDateSetListener, OnTimeSetListener {
	/** Tag for output. */
	private static final String TAG = "addlog";

	/** {@link Spinner}s. */
	private Spinner spType, spDirection;
	/** {@link EditText}s. */
	private EditText etLength, etRemote;
	private TextView tvDate, tvTime;
	/** {@link CheckBox}. */
	private CheckBox cbRoamed;
	private Calendar cal;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		this.setTheme(Preferences.getTheme(this));
		Utils.setLocale(this);
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.logs_add);
		CallMeter.fixActionBarBackground(this.getSupportActionBar(), this.getResources(),
				R.drawable.bg_striped, R.drawable.bg_striped_split);

		this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		this.setTitle(R.string.add_log);

		if (savedInstanceState == null) {
			this.cal = Calendar.getInstance();
		} else {
			this.cal = Calendar.getInstance();
			this.cal.setTimeInMillis(savedInstanceState.getLong("cal"));
		}

		this.spType = (Spinner) this.findViewById(R.id.type);
		this.spDirection = (Spinner) this.findViewById(R.id.direction);
		this.etLength = (EditText) this.findViewById(R.id.length);
		this.etRemote = (EditText) this.findViewById(R.id.remote);
		this.tvDate = (TextView) this.findViewById(R.id.date);
		this.tvTime = (TextView) this.findViewById(R.id.time);
		this.cbRoamed = (CheckBox) this.findViewById(R.id.roamed);
		this.spType.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(final AdapterView<?> parent, final View view,
					final int position, final long id) {
				switch (position) {
				case DataProvider.Rules.WHAT_CALL:
					AddLogActivity.this.etLength.setHint(R.string.length_hint_call);
					AddLogActivity.this.etLength.setVisibility(View.VISIBLE);
					AddLogActivity.this.etRemote.setVisibility(View.VISIBLE);
					break;
				case DataProvider.Rules.WHAT_DATA:
					AddLogActivity.this.etLength.setHint(R.string.length_hint_data);
					AddLogActivity.this.etLength.setVisibility(View.VISIBLE);
					AddLogActivity.this.etRemote.setVisibility(View.GONE);
					break;
				case DataProvider.Rules.WHAT_MMS:
				case DataProvider.Rules.WHAT_SMS:
					AddLogActivity.this.etLength.setVisibility(View.GONE);
					AddLogActivity.this.etRemote.setVisibility(View.VISIBLE);
					break;
				default:
					break;
				}
			}

			@Override
			public void onNothingSelected(final AdapterView<?> parent) {
				// nothing to do
			}
		});
		this.tvDate.setOnClickListener(this);
		this.tvTime.setOnClickListener(this);
		this.updateDateTime();
	}

	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putLong("cal", this.cal.getTimeInMillis());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		this.getSupportMenuInflater().inflate(R.menu.menu_addlogs, menu);
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			this.finish();
			return true;
		case R.id.item_add:
			final int t = this.spType.getSelectedItemPosition();
			final int d = this.spDirection.getSelectedItemPosition();
			long l = Utils.parseLong(this.etLength.getText().toString(), 0L);
			final String r = this.etRemote.getText().toString();
			final boolean roamed = this.cbRoamed.isChecked();
			final ContentValues cv = new ContentValues();
			switch (t) {
			case DataProvider.Rules.WHAT_CALL:
				cv.put(DataProvider.Logs.TYPE, DataProvider.TYPE_CALL);
				break;
			case DataProvider.Rules.WHAT_DATA:
				cv.put(DataProvider.Logs.TYPE, DataProvider.TYPE_DATA);
				l *= CallMeter.BYTE_KB;
				break;
			case DataProvider.Rules.WHAT_MMS:
				cv.put(DataProvider.Logs.TYPE, DataProvider.TYPE_MMS);
				l = 1;
				break;
			case DataProvider.Rules.WHAT_SMS:
				cv.put(DataProvider.Logs.TYPE, DataProvider.TYPE_SMS);
				l = 1;
				break;
			default:
				Log.e(TAG, "unknown type");
				Toast.makeText(this, "unknown type", Toast.LENGTH_LONG).show();
				return true;
			}
			cv.put(DataProvider.Logs.DIRECTION, d);
			cv.put(DataProvider.Logs.AMOUNT, l);
			cv.put(DataProvider.Logs.PLAN_ID, DataProvider.NO_ID);
			cv.put(DataProvider.Logs.RULE_ID, DataProvider.NO_ID);
			cv.put(DataProvider.Logs.REMOTE, r);
			cv.put(DataProvider.Logs.DATE, this.cal.getTimeInMillis());
			if (roamed) {
				cv.put(DataProvider.Logs.ROAMED, 1);
			}
			this.getContentResolver().insert(DataProvider.Logs.CONTENT_URI, cv);
			LogRunnerService.update(this, null);
			if (!this.isFinishing()) {
				this.finish();
			}
			return true;
		case R.id.item_cancel:
			if (!this.isFinishing()) {
				this.finish();
			}
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onClick(final View v) {
		switch (v.getId()) {
		case R.id.date:
			DatePickerDialog dpd = new DatePickerDialog(this, this, this.cal.get(Calendar.YEAR),
					this.cal.get(Calendar.MONTH), this.cal.get(Calendar.DAY_OF_MONTH));
			dpd.show();
			break;
		case R.id.time:
			TimePickerDialog dtp = new TimePickerDialog(this, this,
					this.cal.get(Calendar.HOUR_OF_DAY), this.cal.get(Calendar.MINUTE),
					DateFormat.is24HourFormat(this));
			dtp.show();
			break;
		default:
			break;
		}
	}

	@Override
	public void onTimeSet(final TimePicker view, final int hourOfDay, final int minute) {
		this.cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
		this.cal.set(Calendar.MINUTE, minute);
		this.cal.set(Calendar.SECOND, 0);
		this.cal.set(Calendar.MILLISECOND, 1);
		this.updateDateTime();
	}

	@Override
	public void onDateSet(final DatePicker view, final int year, final int monthOfYear,
			final int dayOfMonth) {
		this.cal.set(Calendar.YEAR, year);
		this.cal.set(Calendar.MONTH, monthOfYear);
		this.cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
		this.updateDateTime();
	}

	private void updateDateTime() {
		Date date = this.cal.getTime();
		this.tvDate.setText(DateFormat.getDateFormat(this).format(date));
		this.tvTime.setText(DateFormat.getTimeFormat(this).format(date));
	}

}
