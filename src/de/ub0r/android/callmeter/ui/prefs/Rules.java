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
package de.ub0r.android.callmeter.ui.prefs;

import android.app.AlertDialog.Builder;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import de.ub0r.android.callmeter.R;
import de.ub0r.android.callmeter.data.DataProvider;
import de.ub0r.android.callmeter.data.RuleMatcher;
import de.ub0r.android.lib.Utils;

/**
 * {@link SherlockPreferenceActivity} for setting rules.
 * 
 * @author flx
 */
public final class Rules extends SherlockPreferenceActivity implements OnPreferenceClickListener {
	/** Tag for output. */
	// private static final String TAG = "pr";

	/** Item menu: edit. */
	private static final int WHICH_EDIT = 0;
	/** Item menu: move up. */
	private static final int WHICH_UP = 1;
	/** Item menu: move down. */
	private static final int WHICH_DOWN = 2;
	/** Item menu: delete. */
	private static final int WHICH_DELETE = 3;

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Utils.setLocale(this);

		this.addPreferencesFromResource(R.xml.group_prefs);
	}

	@Override
	protected void onResume() {
		super.onResume();
		this.reload();
	}

	/**
	 * Reload rules from ContentProvider.
	 */
	@SuppressWarnings("deprecation")
	private void reload() {
		PreferenceScreen ps = (PreferenceScreen) this.findPreference("container");
		ps.removeAll();
		Cursor c = this.getContentResolver().query(DataProvider.Rules.CONTENT_URI,
				DataProvider.Rules.PROJECTION, null, null, null);
		if (c.moveToFirst()) {
			String[] types = this.getResources().getStringArray(R.array.rules_type);
			do {
				Preference p = new Preference(this);
				p.setPersistent(false);
				p.setKey("group_" + c.getInt(DataProvider.Rules.INDEX_ID));
				p.setTitle(c.getString(DataProvider.Rules.INDEX_NAME));

				String hint = "";
				final int t = c.getInt(DataProvider.Rules.INDEX_WHAT);
				if (t >= 0 && t < types.length) {
					hint += types[t];
				} else {
					hint += "???";
				}
				int i = c.getInt(DataProvider.Rules.INDEX_LIMIT_NOT_REACHED);
				if (i == 1) {
					hint += " & " + this.getString(R.string.limitnotreached_);
				}
				i = c.getInt(DataProvider.Rules.INDEX_ROAMED);
				if (i == 0) {
					hint += " & \u00AC " + this.getString(R.string.roamed_);
				} else if (i == 1) {
					hint += " & " + this.getString(R.string.roamed_);
				}
				i = c.getInt(DataProvider.Rules.INDEX_DIRECTION);
				if (i >= 0 && i < DataProvider.Rules.NO_MATTER) {
					String[] strs;
					final Resources r = this.getResources();
					if (t == DataProvider.TYPE_SMS) {
						strs = r.getStringArray(R.array.direction_sms);
					} else if (t == DataProvider.TYPE_MMS) {
						strs = r.getStringArray(R.array.direction_mms);
					} else if (t == DataProvider.TYPE_DATA) {
						strs = r.getStringArray(R.array.direction_data);
					} else {
						strs = r.getStringArray(R.array.direction_calls);
					}
					hint += " & " + strs[i];
				}
				String s = c.getString(DataProvider.Rules.INDEX_INHOURS_ID);
				if (s != null && !s.equals("-1")) {
					hint += " & " + this.getString(R.string.hourgroup_);
				}
				s = c.getString(DataProvider.Rules.INDEX_EXHOURS_ID);
				if (s != null && !s.equals("-1")) {
					hint += " & " + this.getString(R.string.exhourgroup_);
				}
				s = c.getString(DataProvider.Rules.INDEX_INNUMBERS_ID);
				if (s != null && !s.equals("-1")) {
					hint += " & " + this.getString(R.string.numbergroup_);
				}
				s = c.getString(DataProvider.Rules.INDEX_EXNUMBERS_ID);
				if (s != null && !s.equals("-1")) {
					hint += " & " + this.getString(R.string.exnumbergroup_);
				}
				p.setSummary(hint);

				p.setOnPreferenceClickListener(this);
				ps.addPreference(p);
			} while (c.moveToNext());
		}
		c.close();
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		this.getSupportMenuInflater().inflate(R.menu.menu_group, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case R.id.item_add:
			Preferences.setDefaultPlan(this, false);
			final ContentValues cv = new ContentValues();
			cv.put(DataProvider.Rules.NAME, this.getString(R.string.rules_new));
			final Uri uri = this.getContentResolver().insert(DataProvider.Rules.CONTENT_URI, cv);
			final Intent intent = new Intent(this, RuleEdit.class);
			intent.setData(uri);
			this.startActivity(intent);
			return true;
		default:
			return false;
		}
	}

	/**
	 * Move an item.
	 * 
	 * @param u
	 *            item's {@link Uri}
	 * @param diretion
	 *            +1/-1
	 */
	private void move(final Uri u, final int diretion) {
		Cursor c0 = this.getContentResolver().query(u, DataProvider.Rules.PROJECTION, null, null,
				null);
		if (c0.moveToFirst()) {
			int o0 = c0.getInt(DataProvider.Rules.INDEX_ORDER);
			String w;
			String o;
			if (diretion < 0) {
				w = DataProvider.Rules.ORDER + "<?";
				o = DataProvider.Rules.ORDER + " DESC";
			} else {
				w = DataProvider.Rules.ORDER + ">?";
				o = DataProvider.Rules.ORDER + " ASC";
			}
			Cursor c1 = this.getContentResolver().query(DataProvider.Rules.CONTENT_URI,
					DataProvider.Rules.PROJECTION, w, new String[] { String.valueOf(o0) }, o);
			if (c1.moveToFirst()) {
				ContentValues values = new ContentValues();
				values.put(DataProvider.Rules.ORDER, o0);
				this.getContentResolver().update(
						ContentUris.withAppendedId(DataProvider.Rules.CONTENT_URI,
								c1.getInt(DataProvider.Rules.INDEX_ID)), values, null, null);
			}
			c1.close();

			ContentValues values = new ContentValues();
			values.put(DataProvider.Rules.ORDER, o0 + diretion);
			this.getContentResolver().update(u, values, null, null);

			this.reload();
		}
		c0.close();
	}

	@Override
	public boolean onPreferenceClick(final Preference preference) {
		String k = preference.getKey();
		if (k != null && k.startsWith("group_")) {
			final int id = Integer.parseInt(k.substring("group_".length()));
			final Uri uri = ContentUris.withAppendedId(DataProvider.Rules.CONTENT_URI, id);
			final Builder builder = new Builder(this);
			builder.setItems(R.array.dialog_edit_up_down_delete,
					new android.content.DialogInterface.OnClickListener() {
						@Override
						public void onClick(final DialogInterface dialog, final int which) {
							switch (which) {
							case WHICH_EDIT:
								final Intent intent = new Intent(Rules.this, RuleEdit.class);
								intent.setData(uri);
								Rules.this.startActivity(intent);
								break;
							case WHICH_UP:
								Rules.this.move(uri, -1);
								Preferences.setDefaultPlan(Rules.this, false);
								RuleMatcher.unmatch(Rules.this);
								break;
							case WHICH_DOWN:
								Rules.this.move(uri, 1);
								Preferences.setDefaultPlan(Rules.this, false);
								RuleMatcher.unmatch(Rules.this);
								break;
							case WHICH_DELETE:
								Rules.this.getContentResolver().delete(
										ContentUris.withAppendedId(DataProvider.Rules.CONTENT_URI,
												id), null, null);
								Rules.this.reload();
								Preferences.setDefaultPlan(Rules.this, false);
								RuleMatcher.unmatch(Rules.this);
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
		return false;
	}
}
