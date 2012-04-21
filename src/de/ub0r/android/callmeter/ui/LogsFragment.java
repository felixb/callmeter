/*
 * Copyright (C) 2009-2012 Felix Bechstein
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

import android.app.AlertDialog.Builder;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.ResourceCursorAdapter;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import de.ub0r.android.callmeter.R;
import de.ub0r.android.callmeter.data.DataProvider;
import de.ub0r.android.callmeter.data.LogRunnerService;
import de.ub0r.android.callmeter.ui.prefs.Preferences;
import de.ub0r.android.lib.DbUtils;
import de.ub0r.android.lib.Log;

/**
 * Callmeter's Log {@link LogFragment}.
 * 
 * @author flx
 */
public final class LogsFragment extends SherlockListFragment implements OnClickListener,
		OnItemLongClickListener, LoaderCallbacks<Cursor> {
	/** Tag for output. */
	private static final String TAG = "logs";

	/** Prefs: {@link ToggleButton} state for calls. */
	private static final String PREF_CALL = "_logs_call";
	/** Prefs: {@link ToggleButton} state for sms. */
	private static final String PREF_SMS = "_logs_sms";
	/** Prefs: {@link ToggleButton} state for mms. */
	private static final String PREF_MMS = "_logs_mms";
	/** Prefs: {@link ToggleButton} state for data. */
	private static final String PREF_DATA = "_logs_data";
	/** Prefs: {@link ToggleButton} state for in. */
	private static final String PREF_IN = "_in";
	/** Prefs: {@link ToggleButton} state for out. */
	private static final String PREF_OUT = "_out";

	/** {@link ToggleButton}s. */
	private ToggleButton tbCall, tbSMS, tbMMS, tbData, tbIn, tbOut, tbPlan;
	/** Show hours and days. */
	private boolean showHours = true;
	/** Currency format. */
	private String cformat;

	/** Selected plan id. */
	private long planId = -1;

	/** Unique id for this {@link LogFragment}s loader. */
	private static final int LOADER_UID = -2;

	/**
	 * Adapter binding logs to View.
	 * 
	 * @author flx
	 */
	public class LogAdapter extends ResourceCursorAdapter {

		/** Column ids. */
		private int idPlanName, idRuleName;

		/**
		 * Default Constructor.
		 * 
		 * @param context
		 *            {@link Context}
		 */
		public LogAdapter(final Context context) {
			super(context, R.layout.logs_item, null, true);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public final Cursor swapCursor(final Cursor cursor) {
			Cursor c = super.swapCursor(cursor);
			this.idPlanName = cursor.getColumnIndex(DataProvider.Plans.NAME);
			this.idRuleName = cursor.getColumnIndex(DataProvider.Rules.NAME);
			return c;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public final void bindView(final View view, final Context context, final Cursor cursor) {
			final StringBuilder buf = new StringBuilder();
			final int t = cursor.getInt(DataProvider.Logs.INDEX_TYPE);
			String[] strs = context.getResources().getStringArray(R.array.plans_type);
			buf.append(strs[t]);
			final int dir = cursor.getInt(DataProvider.Logs.INDEX_DIRECTION);
			strs = context.getResources().getStringArray(R.array.direction_calls);
			buf.append(" (" + strs[dir] + "): ");
			final long date = cursor.getLong(DataProvider.Logs.INDEX_DATE);
			buf.append(Common.formatDate(context, date));
			buf.append(" ");
			buf.append(DateFormat.getTimeFormat(context).format(new Date(date)));
			((TextView) view.findViewById(android.R.id.text1)).setText(buf.toString());

			((TextView) view.findViewById(R.id.plan)).setText(cursor.getString(this.idPlanName));
			((TextView) view.findViewById(R.id.rule)).setText(cursor.getString(this.idRuleName));

			String s = cursor.getString(DataProvider.Logs.INDEX_REMOTE);
			if (s == null || s.trim().length() == 0) {
				view.findViewById(R.id.remote).setVisibility(View.GONE);
				view.findViewById(R.id.remote_).setVisibility(View.GONE);
			} else {
				final TextView tw = (TextView) view.findViewById(R.id.remote);
				tw.setVisibility(View.VISIBLE);
				view.findViewById(R.id.remote_).setVisibility(View.VISIBLE);
				tw.setText(s);
			}

			final long amount = cursor.getLong(DataProvider.Logs.INDEX_AMOUNT);
			s = Common.formatAmount(t, amount, LogsFragment.this.showHours);
			if (s == null || s.trim().length() == 0 || s.equals("1")) {
				view.findViewById(R.id.length).setVisibility(View.GONE);
				view.findViewById(R.id.length_).setVisibility(View.GONE);
			} else {
				final TextView tw = (TextView) view.findViewById(R.id.length);
				tw.setVisibility(View.VISIBLE);
				view.findViewById(R.id.length_).setVisibility(View.VISIBLE);
				tw.setText(s);
			}
			final float ba = cursor.getFloat(DataProvider.Logs.INDEX_BILL_AMOUNT);
			TextView tw = (TextView) view.findViewById(R.id.blength);
			if (amount != ba) {
				tw.setText(Common.formatAmount(t, ba, LogsFragment.this.showHours));
				tw.setVisibility(View.VISIBLE);
				view.findViewById(R.id.blength_).setVisibility(View.VISIBLE);
			} else {
				tw.setVisibility(View.GONE);
				view.findViewById(R.id.blength_).setVisibility(View.GONE);
			}
			final float cost = cursor.getFloat(DataProvider.Logs.INDEX_COST);
			final float free = cursor.getFloat(DataProvider.Logs.INDEX_FREE);
			tw = (TextView) view.findViewById(R.id.cost);
			if (cost > 0f) {
				String c;
				if (free == 0f) {
					c = String.format(LogsFragment.this.cformat, cost);
				} else if (free >= cost) {
					c = "(" + String.format(LogsFragment.this.cformat, cost) + ")";
				} else {
					c = "(" + String.format(LogsFragment.this.cformat, free) + ") "
							+ String.format(LogsFragment.this.cformat, cost - free);
				}
				tw.setText(c);
				tw.setVisibility(View.VISIBLE);
				view.findViewById(R.id.cost_).setVisibility(View.VISIBLE);
			} else {
				tw.setVisibility(View.GONE);
				view.findViewById(R.id.cost_).setVisibility(View.GONE);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setHasOptionsMenu(true);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onActivityCreated(final Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		this.setListAdapter(new LogAdapter(this.getActivity()));
		this.getListView().setOnItemLongClickListener(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
			final Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.logs, container, false);
		this.tbCall = (ToggleButton) v.findViewById(R.id.calls);
		this.tbCall.setOnClickListener(this);
		this.tbSMS = (ToggleButton) v.findViewById(R.id.sms);
		this.tbSMS.setOnClickListener(this);
		this.tbMMS = (ToggleButton) v.findViewById(R.id.mms);
		this.tbMMS.setOnClickListener(this);
		this.tbData = (ToggleButton) v.findViewById(R.id.data);
		this.tbData.setOnClickListener(this);
		this.tbIn = (ToggleButton) v.findViewById(R.id.in);
		this.tbIn.setOnClickListener(this);
		this.tbOut = (ToggleButton) v.findViewById(R.id.out);
		this.tbOut.setOnClickListener(this);
		this.tbPlan = (ToggleButton) v.findViewById(R.id.plan);
		this.tbPlan.setOnClickListener(this);
		final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(this
				.getActivity());
		this.tbCall.setChecked(p.getBoolean(PREF_CALL, true));
		this.tbSMS.setChecked(p.getBoolean(PREF_SMS, true));
		this.tbMMS.setChecked(p.getBoolean(PREF_MMS, true));
		this.tbData.setChecked(p.getBoolean(PREF_DATA, true));
		this.tbIn.setChecked(p.getBoolean(PREF_IN, true));
		this.tbOut.setChecked(p.getBoolean(PREF_OUT, true));

		String[] directions = this.getResources().getStringArray(R.array.direction_calls);
		this.tbIn.setText(directions[DataProvider.DIRECTION_IN]);
		this.tbIn.setTextOn(directions[DataProvider.DIRECTION_IN]);
		this.tbIn.setTextOff(directions[DataProvider.DIRECTION_IN]);
		this.tbOut.setText(directions[DataProvider.DIRECTION_OUT]);
		this.tbOut.setTextOn(directions[DataProvider.DIRECTION_OUT]);
		this.tbOut.setTextOff(directions[DataProvider.DIRECTION_OUT]);
		directions = null;

		if (this.planId >= 0L) {
			this.setPlanId(planId);
		}

		return v;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onResume() {
		super.onResume();
		Common.setDateFormat(this.getActivity());
		this.showHours = PreferenceManager.getDefaultSharedPreferences(this.getActivity())
				.getBoolean(Preferences.PREFS_SHOWHOURS, true);
		this.cformat = Preferences.getCurrencyFormat(this.getActivity());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onStop() {
		super.onStop();
		final Editor e = PreferenceManager.getDefaultSharedPreferences(this.getActivity()).edit();
		e.putBoolean(PREF_CALL, this.tbCall.isChecked());
		e.putBoolean(PREF_SMS, this.tbSMS.isChecked());
		e.putBoolean(PREF_MMS, this.tbMMS.isChecked());
		e.putBoolean(PREF_DATA, this.tbData.isChecked());
		e.putBoolean(PREF_IN, this.tbIn.isChecked());
		e.putBoolean(PREF_OUT, this.tbOut.isChecked());
		e.commit();
	}

	/**
	 * Set Adapter.
	 * 
	 * @param forceUpdate
	 *            force update
	 */
	public void setAdapter(final boolean forceUpdate) {
		LogAdapter adapter = (LogAdapter) this.getListAdapter();
		if (!forceUpdate && adapter != null && !adapter.isEmpty()) {
			return;
		}

		String where = DataProvider.Logs.TABLE + "." + DataProvider.Logs.TYPE + " in (-1";
		if (this.tbCall.isChecked()) {
			where += "," + DataProvider.TYPE_CALL;
		}
		if (this.tbSMS.isChecked()) {
			where += "," + DataProvider.TYPE_SMS;
		}
		if (this.tbMMS.isChecked()) {
			where += "," + DataProvider.TYPE_MMS;
		}
		if (this.tbData.isChecked()) {
			where += "," + DataProvider.TYPE_DATA;
		}
		where += ") and " + DataProvider.Logs.TABLE + "." + DataProvider.Logs.DIRECTION + " in (-1";
		if (this.tbIn.isChecked()) {
			where += "," + DataProvider.DIRECTION_IN;
		}
		if (this.tbOut.isChecked()) {
			where += "," + DataProvider.DIRECTION_OUT;
		}
		where += ")";

		if (this.planId > 0L && this.tbPlan.isChecked()) {
			where = DbUtils.sqlAnd(DataProvider.Logs.TABLE + "." + DataProvider.Logs.PLAN_ID + "="
					+ this.planId, where);
		}
		Bundle args = new Bundle(1);
		args.putString("where", where);

		LoaderManager lm = this.getLoaderManager();
		if (lm.getLoader(LOADER_UID) == null) {
			lm.initLoader(LOADER_UID, args, this);
		} else {
			lm.restartLoader(LOADER_UID, args, this);
		}
	}

	/**
	 * Set filter to show only given plan. Set to -1 for no filter.
	 * 
	 * @param id
	 *            plan's id
	 */
	public void setPlanId(final long id) {
		this.planId = id;
		if (tbPlan != null) {
			if (id < 0L) {
				this.tbPlan.setVisibility(View.GONE);
			} else {
				String p = DataProvider.Plans.getName(this.getActivity().getContentResolver(),
						this.planId);
				this.tbPlan.setText(p);
				this.tbPlan.setTextOn(p);
				this.tbPlan.setTextOff(p);
				this.tbPlan.setVisibility(View.VISIBLE);
			}
		}
		if (this.isVisible()) {
			this.setAdapter(true);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onClick(final View v) {
		this.setAdapter(true);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
		inflater.inflate(R.menu.menu_logs, menu);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case R.id.item_add:
			this.getActivity().startActivity(new Intent(this.getActivity(), AddLogActivity.class));
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onItemLongClick(final AdapterView<?> parent, final View view,
			final int position, final long id) {
		final Builder b = new Builder(this.getActivity());
		b.setCancelable(true);
		b.setItems(R.array.dialog_delete, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog, final int which) {
				LogsFragment.this
						.getActivity()
						.getContentResolver()
						.delete(ContentUris.withAppendedId(DataProvider.Logs.CONTENT_URI, id),
								null, null);
				LogsFragment.this.setAdapter(true);
				LogRunnerService.update(LogsFragment.this.getActivity(), null);
			}
		});
		b.setNegativeButton(android.R.string.cancel, null);
		b.show();
		return true;
	}

	@Override
	public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
		Log.d(TAG, "onCreateLoader(" + id + "," + args + ")");
		((Plans) this.getActivity()).setProgress(1);
		String where = null;
		if (args != null) {
			where = args.getString("where");
		}
		return new CursorLoader(this.getActivity(), DataProvider.Logs.CONTENT_URI_JOIN,
				DataProvider.Logs.PROJECTION_JOIN, where, null, DataProvider.Logs.DATE + " DESC");
	}

	@Override
	public void onLoadFinished(final Loader<Cursor> loader, final Cursor data) {
		Log.d(TAG, "onLoadFinished()");
		((LogAdapter) this.getListAdapter()).swapCursor(data);
		((Plans) this.getActivity()).setProgress(-1);
	}

	@Override
	public void onLoaderReset(final Loader<Cursor> loader) {
		Log.d(TAG, "onLoaderReset()");
		try {
			((LogAdapter) this.getListAdapter()).swapCursor(null);
		} catch (Exception e) {
			Log.w(TAG, "error removing cursor", e);
		}
	}
}
