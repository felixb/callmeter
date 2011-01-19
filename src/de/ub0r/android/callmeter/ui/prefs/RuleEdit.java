/*
 * Copyright (C) 2009-2010 Felix Bechstein
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
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnDismissListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import de.ub0r.android.callmeter.R;
import de.ub0r.android.callmeter.data.DataProvider;
import de.ub0r.android.callmeter.ui.prefs.Preference.BoolPreference;
import de.ub0r.android.callmeter.ui.prefs.Preference.CursorPreference;
import de.ub0r.android.callmeter.ui.prefs.Preference.ListPreference;
import de.ub0r.android.callmeter.ui.prefs.Preference.TextPreference;
import de.ub0r.android.lib.DbUtils;
import de.ub0r.android.lib.Log;
import de.ub0r.android.lib.Utils;

/**
 * Edit a single Plan.
 * 
 * @author flx
 */
public class RuleEdit extends ListActivity implements OnClickListener,
		OnItemClickListener, OnDismissListener {
	/** Tag for debug out. */
	private static final String TAG = "re";

	/** {@link PreferenceAdapter}. */
	private PreferenceAdapter adapter = null;

	/** Id of edited filed. */
	private long rid = -1;

	/** Array holding {@link String}s. */
	private String[] inOutNomatterCalls = null;
	/** Array holding {@link String}s. */
	private String[] inOutNomatterSms = null;
	/** Array holding {@link String}s. */
	private String[] inOutNomatterMms = null;
	/** Array holding {@link String}s. */
	private String[] inOutNomatterData = null;
	/** Array holding {@link String}s. */
	private String[] yesNoNomatter = null;

	/**
	 * Get a {@link String}-Array for ListView.
	 * 
	 * @param base
	 *            base array without no_matter_
	 * @return array with no_matter_
	 */
	private String[] getStrings(final int base) {
		switch (base) {
		case R.array.direction_calls:
			if (this.inOutNomatterCalls == null) {
				final String[] tmp1 = new String[3];
				final String[] tmp2 = this.getResources().getStringArray(base);
				tmp1[0] = tmp2[0];
				tmp1[1] = tmp2[1];
				tmp1[2] = this.getString(R.string.no_matter_);
				this.inOutNomatterCalls = tmp1;
			}
			return this.inOutNomatterCalls;
		case R.array.direction_sms:
			if (this.inOutNomatterSms == null) {
				final String[] tmp1 = new String[3];
				final String[] tmp2 = this.getResources().getStringArray(base);
				tmp1[0] = tmp2[0];
				tmp1[1] = tmp2[1];
				tmp1[2] = this.getString(R.string.no_matter_);
				this.inOutNomatterSms = tmp1;
			}
			return this.inOutNomatterSms;
		case R.array.direction_mms:
			if (this.inOutNomatterMms == null) {
				final String[] tmp1 = new String[3];
				final String[] tmp2 = this.getResources().getStringArray(base);
				tmp1[0] = tmp2[0];
				tmp1[1] = tmp2[1];
				tmp1[2] = this.getString(R.string.no_matter_);
				this.inOutNomatterMms = tmp1;
			}
			return this.inOutNomatterMms;
		case R.array.direction_data:
			if (this.inOutNomatterData == null) {
				final String[] tmp1 = new String[3];
				final String[] tmp2 = this.getResources().getStringArray(base);
				tmp1[0] = tmp2[0];
				tmp1[1] = tmp2[1];
				tmp1[2] = this.getString(R.string.no_matter_);
				this.inOutNomatterData = tmp1;
			}
			return this.inOutNomatterData;
		default:
			if (this.yesNoNomatter == null) {
				final String[] tmp1 = new String[3];
				tmp1[0] = this.getString(R.string.yes);
				tmp1[1] = this.getString(R.string.no);
				tmp1[2] = this.getString(R.string.no_matter_);
				this.yesNoNomatter = tmp1;
			}
			return this.yesNoNomatter;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Utils.setLocale(this);
		this.setTitle(this.getString(R.string.settings) + " > "
				+ this.getString(R.string.rules) + " > "
				+ this.getString(R.string.edit_));
		this.setContentView(R.layout.list_ok_cancel);

		this.getListView().setOnItemClickListener(this);
		this.findViewById(R.id.ok).setOnClickListener(this);
		this.findViewById(R.id.cancel).setOnClickListener(this);

		this.fillFields();
		this.fillPlan();
		this.showHideFileds();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void onResume() {
		super.onResume();
		Utils.setLocale(this);
		this.showHideFileds();
		final PreferenceAdapter a = this.adapter;
		a.getPreference(DataProvider.Rules.INHOURS_ID).refreshDialog();
		a.getPreference(DataProvider.Rules.EXHOURS_ID).refreshDialog();
		a.getPreference(DataProvider.Rules.INNUMBERS_ID).refreshDialog();
		a.getPreference(DataProvider.Rules.EXNUMBERS_ID).refreshDialog();
	}

	/**
	 * Set plan's and what0 value.
	 */
	private void fillPlan() {
		final int t = ((ListPreference) this.adapter
				.getPreference(DataProvider.Rules.WHAT)).getValue();
		String where = null;
		switch (t) {
		case DataProvider.Rules.WHAT_CALL:
			where = DataProvider.Plans.TYPE + " = " + DataProvider.TYPE_CALL
					+ " OR " + DataProvider.Plans.TYPE + " = "
					+ DataProvider.TYPE_MIXED;
			break;
		case DataProvider.Rules.WHAT_DATA:
			where = DataProvider.Plans.TYPE + " = " + DataProvider.TYPE_DATA;
			break;
		case DataProvider.Rules.WHAT_SMS:
		case DataProvider.Rules.WHAT_MMS:
			where = DataProvider.Plans.TYPE + " = " + DataProvider.TYPE_SMS
					+ " OR " + DataProvider.Plans.TYPE + " = "
					+ DataProvider.TYPE_MMS + " OR " + DataProvider.Plans.TYPE
					+ " = " + DataProvider.TYPE_MIXED;
			break;
		default:
			where = DataProvider.Plans.WHERE_REALPLANS;
			break;
		}
		where = DbUtils.sqlAnd(where, DataProvider.Plans.MERGED_PLANS
				+ " IS NULL");
		Log.d(TAG, "plans.where: " + where);
		((CursorPreference) this.adapter
				.getPreference(DataProvider.Rules.PLAN_ID)).setCursor(where);
	}

	/**
	 * Get a new {@link PreferenceAdapter} for this {@link ListActivity}.
	 * 
	 * @return {@link PreferenceAdapter}
	 */
	private PreferenceAdapter getAdapter() {
		final PreferenceAdapter ret = new PreferenceAdapter(this);
		ret.add(new TextPreference(this, DataProvider.Rules.NAME, this
				.getString(R.string.rules_new), R.string.name_,
				R.string.name_help, InputType.TYPE_CLASS_TEXT));
		ret.add(new ListPreference(this, DataProvider.Rules.WHAT,
				DataProvider.Rules.WHAT_CALL, R.string.what_,
				R.string.what_help, R.array.rules_type));
		ret.add(new CursorPreference(this, DataProvider.Rules.PLAN_ID,
				R.string.plan_, R.string.plan_help, -1, -1, -1,
				DataProvider.Plans.CONTENT_URI, DataProvider.Plans.ID,
				DataProvider.Plans.NAME, null, false, null, null, null));
		ret
				.add(new BoolPreference(this,
						DataProvider.Rules.LIMIT_NOT_REACHED,
						R.string.limitnotreached_,
						R.string.limitnotreached_help, this));
		ret.add(new ListPreference(this, DataProvider.Rules.DIRECTION,
				DataProvider.Rules.NO_MATTER, R.string.direction_,
				R.string.direction_help, this
						.getStrings(R.array.direction_calls)));
		ret.add(new ListPreference(this, DataProvider.Rules.ROAMED,
				DataProvider.Rules.NO_MATTER, R.string.roamed_,
				R.string.roamed_help, this.getStrings(-1)));
		ret.add(new ListPreference(this, DataProvider.Rules.IS_WEBSMS,
				DataProvider.Rules.NO_MATTER, R.string.iswebsms_,
				R.string.iswebsms_help, this.getStrings(-1)));
		ret.add(new TextPreference(this, DataProvider.Rules.IS_WEBSMS_CONNETOR,
				"", R.string.iswebsms_connector_,
				R.string.iswebsms_connector_help, InputType.TYPE_CLASS_TEXT));
		ret.add(new ListPreference(this, DataProvider.Rules.IS_SIPCALL,
				DataProvider.Rules.NO_MATTER, R.string.issipcall_,
				R.string.issipcall_help, this.getStrings(-1)));
		final DialogInterface.OnClickListener editHours = // .
		new DialogInterface.OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog, final int which) {
				RuleEdit.this.startActivity(new Intent(RuleEdit.this, // .
						HourGroups.class));
			}
		};
		ret.add(new CursorPreference(this, DataProvider.Rules.INHOURS_ID,
				R.string.hourgroup_, R.string.hourgroup_help,
				R.string.edit_groups_, R.string.clear_, -1,
				DataProvider.HoursGroup.CONTENT_URI,
				DataProvider.HoursGroup.ID, DataProvider.HoursGroup.NAME, null,
				true, editHours, null, null));
		ret.add(new CursorPreference(this, DataProvider.Rules.EXHOURS_ID,
				R.string.exhourgroup_, R.string.exhourgroup_help,
				R.string.edit_groups_, R.string.clear_, -1,
				DataProvider.HoursGroup.CONTENT_URI,
				DataProvider.HoursGroup.ID, DataProvider.HoursGroup.NAME, null,
				true, editHours, null, null));
		final DialogInterface.OnClickListener editNumbers = // .
		new DialogInterface.OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog, final int which) {
				RuleEdit.this.startActivity(new Intent(RuleEdit.this, // .
						NumberGroups.class));
			}
		};
		ret.add(new CursorPreference(this, DataProvider.Rules.INNUMBERS_ID,
				R.string.numbergroup_, R.string.numbergroup_help,
				R.string.edit_groups_, -1, -1,
				DataProvider.NumbersGroup.CONTENT_URI,
				DataProvider.NumbersGroup.ID, DataProvider.NumbersGroup.NAME,
				null, true, editNumbers, null, null));
		ret.add(new CursorPreference(this, DataProvider.Rules.EXNUMBERS_ID,
				R.string.exnumbergroup_, R.string.exnumbergroup_help,
				R.string.edit_groups_, R.string.clear_, -1,
				DataProvider.NumbersGroup.CONTENT_URI,
				DataProvider.NumbersGroup.ID, DataProvider.NumbersGroup.NAME,
				null, true, editNumbers, null, null));
		return ret;
	}

	/**
	 * Fill the fields with data from the cursor.
	 */
	private void fillFields() {
		final Uri uri = this.getIntent().getData();
		int nid = -1;
		Cursor cursor = null;
		if (uri != null) {
			cursor = this.getContentResolver().query(uri,
					DataProvider.Rules.PROJECTION, null, null, null);
			if (cursor == null || !cursor.moveToFirst()) {
				cursor = null;
				this.rid = -1;
			}
		}
		if (cursor != null) {
			nid = cursor.getInt(DataProvider.Rules.INDEX_ID);
		}
		if (this.rid == -1 || nid != this.rid) {
			this.rid = nid;
			this.adapter = this.getAdapter();
			this.getListView().setAdapter(this.adapter);
		}
		if (cursor != null && !cursor.isClosed()) {
			this.adapter.load(cursor);
			cursor.close();
		}
	}

	/**
	 * Show or hide fields based on data in there.
	 */
	private void showHideFileds() {
		final int t = ((ListPreference) this.adapter
				.getPreference(DataProvider.Rules.WHAT)).getValue();
		switch (t) {
		case DataProvider.Rules.WHAT_SMS:
			this.adapter.hide(DataProvider.Rules.IS_WEBSMS, false);
			this.adapter.hide(DataProvider.Rules.IS_WEBSMS_CONNETOR,
					((ListPreference) this.adapter
							.getPreference(DataProvider.Rules.IS_WEBSMS))
							.getValue() != 0);
			this.adapter.hide(DataProvider.Rules.IS_SIPCALL, true);
			break;
		case DataProvider.Rules.WHAT_CALL:
			this.adapter.hide(DataProvider.Rules.IS_SIPCALL, false);
			this.adapter.hide(DataProvider.Rules.IS_WEBSMS, true);
			this.adapter.hide(DataProvider.Rules.IS_WEBSMS_CONNETOR, true);
			break;
		default:
			this.adapter.hide(DataProvider.Rules.IS_SIPCALL, true);
			this.adapter.hide(DataProvider.Rules.IS_WEBSMS, true);
			this.adapter.hide(DataProvider.Rules.IS_WEBSMS_CONNETOR, true);
			break;
		}

		switch (t) {
		case DataProvider.Rules.WHAT_DATA:
			this.adapter.hide(DataProvider.Rules.INNUMBERS_ID, true);
			this.adapter.hide(DataProvider.Rules.EXNUMBERS_ID, true);
			break;
		default:
			this.adapter.hide(DataProvider.Rules.INNUMBERS_ID, false);
			this.adapter.hide(DataProvider.Rules.EXNUMBERS_ID, false);
			break;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onClick(final View v) {
		switch (v.getId()) {
		case R.id.ok:
			final ContentValues cv = this.adapter.save();
			Uri uri = this.getIntent().getData();
			if (uri == null) {
				uri = this.getContentResolver().insert(
						DataProvider.Rules.CONTENT_URI, cv);
			} else {
				this.getContentResolver().update(uri, cv, null, null);
			}
			this.rid = -1;
			final Intent intent = new Intent(this, RuleEdit.class);
			intent.setData(uri);
			this.setResult(RESULT_OK, new Intent(intent));
			this.finish();
			break;
		case R.id.cancel:
			this.rid = -1;
			this.setResult(RESULT_CANCELED);
			this.finish();
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
		final Preference p = this.adapter.getItem(position);
		p.showDialog(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onDismiss(final DialogInterface dialog) {
		this.showHideFileds();
		this.fillPlan();
		this.adapter.notifyDataSetInvalidated();
	}
}
