/*
 * Copyright (C) 2009-2011 Felix Bechstein
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

import android.app.AlertDialog.Builder;
import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.commonsware.cwac.tlv.TouchListView;
import com.commonsware.cwac.tlv.TouchListView.DropListener;

import de.ub0r.android.callmeter.R;
import de.ub0r.android.callmeter.data.DataProvider;
import de.ub0r.android.callmeter.data.RuleMatcher;
import de.ub0r.android.lib.Log;
import de.ub0r.android.lib.Utils;

/**
 * {@link ListActivity} for setting plans.
 * 
 * @author flx
 */
public class Plans extends ListActivity implements OnClickListener,
		OnItemClickListener, OnItemLongClickListener {
	/** Tag for output. */
	private static final String TAG = "pp";

	/**
	 * Adapter binding plans to View.
	 * 
	 * @author flx
	 */
	private static class PlanAdapter extends ResourceCursorAdapter {
		/** Type of plans. */
		private final String[] types;

		/**
		 * Default Constructor.
		 * 
		 * @param context
		 *            {@link Context}
		 */
		public PlanAdapter(final Context context) {
			super(context, R.layout.prefs_plans_item, context
					.getContentResolver().query(DataProvider.Plans.CONTENT_URI,
							DataProvider.Plans.PROJECTION, null, null,
							DataProvider.Plans.ORDER), true);
			this.types = context.getResources().getStringArray(
					R.array.plans_type);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public final void bindView(final View view, final Context ctxt,
				final Cursor cursor) {
			final TextView twTitle = ((TextView) view
					.findViewById(R.id.normtitle));
			twTitle.setText(cursor.getString(DataProvider.Plans.INDEX_NAME));
			final TextView twType = ((TextView) view.findViewById(R.id.type));
			final int i = cursor.getInt(DataProvider.Plans.INDEX_TYPE);
			String hint;
			if (i >= 0 && i < this.types.length) {
				hint = this.types[i];
			} else {
				hint = "???";
			}
			final String s = cursor
					.getString(DataProvider.Plans.INDEX_MERGED_PLANS);
			if (s != null && s.length() > 0) {
				hint += ", " + ctxt.getString(R.string.merge_plans_);
			}
			twType.setText(hint);
		}
	}

	/** Plans. */
	private PlanAdapter adapter = null;

	/** Item menu: edit. */
	private static final int WHICH_EDIT = 0;
	/** Item menu: delete. */
	private static final int WHICH_DELETE = 1;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Utils.setLocale(this);
		this.setTitle(this.getString(R.string.settings) + " > "
				+ this.getString(R.string.plans));
		this.setContentView(R.layout.list_ok_add_touch);
		this.adapter = new PlanAdapter(this);
		this.setListAdapter(this.adapter);
		((TouchListView) this.getListView())
				.setDropListener(new DropListener() {
					@Override
					public void drop(final int from, final int to) {
						Plans.this.move(from, to);
					}
				});
		this.getListView().setOnItemClickListener(this);
		this.getListView().setOnItemLongClickListener(this);
		this.findViewById(R.id.ok).setOnClickListener(this);
		this.findViewById(R.id.add).setOnClickListener(this);
		this.findViewById(R.id.import_default).setOnClickListener(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void onResume() {
		super.onResume();
		Utils.setLocale(this);
		this.showImportHint();
		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		if (prefs.getBoolean(Preferences.PREFS_ADVANCED, false)) {
			this.findViewById(R.id.ok_add).setVisibility(View.VISIBLE);
		} else {
			this.findViewById(R.id.ok_add).setVisibility(View.GONE);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void onStop() {
		super.onStop();
		RuleMatcher.unmatch(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onClick(final View v) {
		switch (v.getId()) {
		case R.id.add:
			Intent intent = new Intent(this, PlanEdit.class);
			this.startActivity(intent);
			break;
		case R.id.ok:
			this.finish();
			break;
		case R.id.import_default:
			intent = new Intent(Intent.ACTION_VIEW, Uri.parse(this
					.getString(R.string.url_rulesets)));
			try {
				this.startActivity(intent);
			} catch (ActivityNotFoundException e) {
				Log.e(TAG, "no activity to load url", e);
				Toast.makeText(this,
						"no activity to load url: " + intent.getDataString(),
						Toast.LENGTH_LONG).show();
			}
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
		final Intent intent = new Intent(this, PlanEdit.class);
		intent.setData(ContentUris.withAppendedId(
				DataProvider.Plans.CONTENT_URI, id));
		this.startActivity(intent);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final boolean onItemLongClick(final AdapterView<?> parent,
			final View view, final int position, final long id) {
		final Builder builder = new Builder(this);
		builder.setItems(R.array.dialog_edit_delete,
				new android.content.DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog,
							final int which) {
						switch (which) {
						case WHICH_EDIT:
							final Intent intent = new Intent(// .
									Plans.this, PlanEdit.class);
							intent.setData(ContentUris.withAppendedId(
									DataProvider.Plans.CONTENT_URI, id));
							Plans.this.startActivity(intent);
							break;
						case WHICH_DELETE:
							Plans.this.getContentResolver().delete(
									ContentUris.withAppendedId(DataProvider.// .
											Plans.CONTENT_URI, id), null, null);
							break;
						default:
							break;
						}
					}
				});
		builder.setNegativeButton(android.R.string.cancel, null);
		builder.show();
		return true;
	}

	/** Set the visibility for the import hint. */
	private void showImportHint() {
		int v = View.GONE;
		if (this.getListView().getCount() == 0) {
			v = View.VISIBLE;
		}
		this.findViewById(R.id.import_default).setVisibility(v);
	}

	/**
	 * Move item.
	 * 
	 * @param from
	 *            from
	 * @param to
	 *            to
	 */
	private void move(final int from, final int to) {
		Log.d(TAG, "move(" + from + "," + to + ")");
		if (from == to) {
			return;
		}
		final ContentResolver cr = this.getContentResolver();
		final int l = this.adapter.getCount();
		Log.d(TAG, "move(): l=" + l);
		long[] ids = new long[l];
		int i = 0;
		int dir;
		if (from < to) {
			dir = 1;
		} else {
			dir = -1;
		}
		Log.d(TAG, "move(): dir=" + dir);
		for (i = 0; i < l; i++) {
			final long id = this.adapter.getItemId(i);
			ids[i] = id;
			Log.d(TAG, "move(): ids[" + i + "]=" + ids[i]);
		}

		final long oldid = ids[from];
		Log.d(TAG, "move(): oldid=" + oldid);
		for (i = from + dir; (from < to && i <= to) || // .
				(from > to && i >= to); i += dir) {
			ids[i - dir] = ids[i];
			Log.d(TAG, "move(): ids[" + (i - dir) + "]=" + ids[i - dir]);
		}
		ids[to] = oldid;
		Log.d(TAG, "move(): ids[" + to + "]=" + ids[to]);

		ContentValues cv = new ContentValues();
		for (i = 0; i < l; i++) {
			cv.clear();
			cv.put(DataProvider.Plans.ORDER, i);
			cr.update(ContentUris.withAppendedId(
					DataProvider.Plans.CONTENT_URI, ids[i]), cv, null, null);
		}
	}
}
