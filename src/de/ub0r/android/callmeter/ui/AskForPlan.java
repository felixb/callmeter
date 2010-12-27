/*
 * Copyright (C) 2009-2010 Felix Bechstein
 * 
 * This file is part of CallMeter 3G.
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

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnDismissListener;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import de.ub0r.android.callmeter.CallMeter;
import de.ub0r.android.callmeter.R;
import de.ub0r.android.callmeter.data.DataProvider;
import de.ub0r.android.callmeter.data.RuleMatcher;
import de.ub0r.android.callmeter.ui.prefs.Preferences;
import de.ub0r.android.lib.Log;
import de.ub0r.android.lib.Utils;

/**
 * Display Ask for plan {@link Activity}.
 * 
 * @author flx
 */
public final class AskForPlan extends Activity implements OnClickListener,
		OnDismissListener {
	/** Tag for output. */
	private static final String TAG = "afp";

	/** Extra providing id of call. */
	public static final String EXTRA_ID = "id";
	/** Extra providing date of call. */
	public static final String EXTRA_DATE = "date";
	/** Extra providing amount of call. */
	public static final String EXTRA_AMOUNT = "amount";

	/** Task updating timeout field. */
	private AsyncTask<Void, Void, Void> timeoutTask = null;

	/** {@link TextView} holding timeout. */
	private TextView tvTimeout = null;
	/** {@link CheckBox} holding set default. */
	private CheckBox cbSetDefault = null;

	/** Default plan id. */
	private long defaultPlanId = -1L;

	/** Data of call. */
	private long id, date, amount;

	/** Maximal number of plans. */
	private final int maxPlans = 10;
	/** Ids of plans. */
	private final long[] planIds = new long[this.maxPlans];
	/** Ids of plans' {@link Button}s. */
	private final int[] planBtns = new int[] { R.id.btn00, R.id.btn01,
			R.id.btn02, R.id.btn03, R.id.btn04, R.id.btn05, R.id.btn06,
			R.id.btn07, R.id.btn08, R.id.btn09 };

	/** Inner {@link Dialog}. */
	private Dialog d = null;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.d = new Dialog(this);
		this.d.setTitle(R.string.select_plan_);
		this.d.setContentView(R.layout.ask_for_plan);
		this.d.setCancelable(true);
		this.d.setOnDismissListener(this);

		final Intent intent = this.getIntent();
		this.id = intent.getLongExtra(EXTRA_ID, -1L);
		this.date = intent.getLongExtra(EXTRA_DATE, -1L);
		this.amount = intent.getLongExtra(EXTRA_AMOUNT, -1L);
		if (this.id < 0L) {
			Log.e(TAG, "no id:" + this.id);
			this.finish();
			return;
		}

		this.tvTimeout = (TextView) this.d.findViewById(R.id.autohide);
		this.cbSetDefault = (CheckBox) this.d.findViewById(R.id.set_default);
		this.d.findViewById(R.id.cancel).setOnClickListener(this);

		final Cursor c = this.getContentResolver().query(
				DataProvider.Plans.CONTENT_URI, DataProvider.Plans.PROJECTION,
				DataProvider.Plans.TYPE + " = " + DataProvider.TYPE_CALL, null,
				null);
		if (c == null || !c.moveToFirst()) {
			Log.e(TAG, "no plans: " + c);
			if (c != null && !c.isClosed()) {
				c.close();
			}
			this.finish();
			return;
		}

		int i = 0;
		do {
			this.planIds[i] = c.getLong(DataProvider.Plans.INDEX_ID);

			final Button v = (Button) this.d.findViewById(this.planBtns[i]);
			v.setVisibility(View.VISIBLE);
			v.setOnClickListener(this);
			v.setText(c.getString(DataProvider.Plans.INDEX_NAME));
			++i;
		} while (i < this.maxPlans && c.moveToNext());

		if (!c.isClosed()) {
			c.close();
		}

		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(this);
		this.defaultPlanId = p.getLong(Preferences.PREFS_ASK_FOR_PLAN_DEFAULT,
				-1L);
		for (i = 0; i < this.maxPlans; i++) {
			if (this.planIds[i] == this.defaultPlanId) {
				final int bid = this.planBtns[i];
				Log.d(TAG, "request focus: " + bid);
				final Button v = (Button) this.d.findViewById(bid);
				v.requestFocus();
				v.setTextAppearance(this, android.R.style.TextAppearance_Large);
				break;
			}
		}
		String s = p.getString(Preferences.PREFS_ASK_FOR_PLAN_AUTOHIDE, "");
		final int timeout = Utils.parseInt(s, 0);
		if (timeout > 0) {
			this.timeoutTask = new AsyncTask<Void, Void, Void>() {
				private int count = timeout;

				@Override
				protected Void doInBackground(final Void... params) {
					while (this.count > 0) {
						try {
							Thread.sleep(CallMeter.MILLIS);
						} catch (InterruptedException e) {
							Log.e(TAG, "intr. count=" + this.count, e);
						}
						--this.count;
						this.publishProgress((Void) null);
					}
					return null;
				}

				@Override
				protected void onProgressUpdate(final Void... values) {
					String s = String.format(AskForPlan.this
							.getString(R.string.autohide_in_), this.count);
					AskForPlan.this.tvTimeout.setText(s);
				}

				@Override
				protected void onPostExecute(final Void result) {
					if (!this.isCancelled() && !AskForPlan.this.isFinishing()) {
						Log.i(TAG, "autohide dialog");
						final long pid = AskForPlan.this.defaultPlanId;
						if (pid >= 0L) {
							Log.i(TAG, "setPlan(" + pid + ")");
							RuleMatcher.matchLog(AskForPlan.this
									.getContentResolver(), AskForPlan.this.id,
									pid);
						}
						AskForPlan.this.d.cancel();
						AskForPlan.this.finish();
					}
				}
			};
			this.timeoutTask.execute((Void) null);
		} else {
			this.tvTimeout.setVisibility(View.GONE);
		}

		this.d.show();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onPause() {
		super.onPause();
		if (this.timeoutTask != null) {
			this.timeoutTask.cancel(true);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onClick(final View v) {
		final int vid = v.getId();
		switch (vid) {
		case R.id.cancel:
			this.d.cancel();
			this.finish();
			break;
		default:
			for (int i = 0; i < this.maxPlans; i++) {
				if (vid == this.planBtns[i]) {
					final long pid = this.planIds[i];
					Log.d(TAG, "setPlan(" + pid + ")");
					RuleMatcher.matchLog(this.getContentResolver(), this.id,
							pid);
					if (this.cbSetDefault.isChecked()) {
						final Editor e = PreferenceManager
								.getDefaultSharedPreferences(this).edit();
						e.putLong(Preferences.PREFS_ASK_FOR_PLAN_DEFAULT, pid);
						e.commit();
					}
					this.d.cancel();
					this.finish();
					break;
				}
			}
			break;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onDismiss(final DialogInterface dialog) {
		this.finish();
	}
}
