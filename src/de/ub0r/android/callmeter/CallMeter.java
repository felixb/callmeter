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
package de.ub0r.android.callmeter;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.preference.PreferenceManager;

import com.actionbarsherlock.app.ActionBar;

import de.ub0r.android.callmeter.data.DataProvider;
import de.ub0r.android.lib.Log;
import de.ub0r.android.lib.Utils;

/**
 * @author flx
 */
public final class CallMeter extends Application {
	/** Tag for output. */
	private static final String TAG = "App";

	/** Minimum date. */
	public static final long MIN_DATE = 10000000000L;
	/** Milliseconds per seconds. */
	public static final long MILLIS = 1000L;

	/** 80. */
	public static final int EIGHTY = 80;
	/** 100. */
	public static final int HUNDRET = 100;
	/** Days of a week. */
	public static final int DAYS_WEEK = 7;
	/** Hours of a day. */
	public static final int HOURS_DAY = 24;
	/** Seconds of a minute. */
	public static final int SECONDS_MINUTE = 60;
	/** Seconds of a hour. */
	public static final int SECONDS_HOUR = 60 * SECONDS_MINUTE;
	/** Seconds of a day. */
	public static final int SECONDS_DAY = 24 * SECONDS_HOUR;

	/** 10. */
	public static final int TEN = 10;

	/** Bytes: kB. */
	public static final long BYTE_KB = 1024L;
	/** Bytes: MB. */
	public static final long BYTE_MB = BYTE_KB * BYTE_KB;
	/** Bytes: GB. */
	public static final long BYTE_GB = BYTE_MB * BYTE_KB;
	/** Bytes: TB. */
	public static final long BYTE_TB = BYTE_GB * BYTE_KB;

	/** Preference's name: last backup. */
	private static final String PREFS_LASTBACKUP = "lastbackup";
	/** Period for backups. */
	private static final long BACKUP_PERIOD = 1000L * 60L * 60L * 24L;

	@Override
	public void onCreate() {
		super.onCreate();
		Log.init("CallMeter3G");
		Log.d(TAG, "init");
		Utils.setLocale(this);
		SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(this);
		if (System.currentTimeMillis() - p.getLong(PREFS_LASTBACKUP, 0L) > BACKUP_PERIOD) {
			if (DataProvider.doBackup(this)) {
				p.edit().putLong(PREFS_LASTBACKUP, System.currentTimeMillis()).commit();
			}
		}
	}

	/**
	 * Fix ActionBar background. See http://b.android.com/15340.
	 * 
	 * @param ab
	 *            {@link ActionBar}
	 * @param r
	 *            {@link Resources}
	 * @param bg
	 *            res id of background {@link BitmapDrawable}
	 * @param bgSplit
	 *            res id of background {@link BitmapDrawable} in split mode
	 */
	public static void fixActionBarBackground(final ActionBar ab, final Resources r, final int bg,
			final int bgSplit) {
		// This is a workaround for http://b.android.com/15340 from
		// http://stackoverflow.com/a/5852198/132047
		BitmapDrawable d = (BitmapDrawable) r.getDrawable(bg);
		d.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
		ab.setBackgroundDrawable(d);
		if (bgSplit >= 0) {
			d = (BitmapDrawable) r.getDrawable(bgSplit);
			d.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
			ab.setSplitBackgroundDrawable(d);
		}
	}
}
