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
package de.ub0r.android.callmeter;

import java.util.Currency;
import java.util.Locale;

import android.app.Application;
import de.ub0r.android.lib.Log;

/**
 * @author flx
 */
public final class CallMeter extends Application {
	/** Tag for output. */
	private static final String TAG = "App";

	/** 100. */
	public static final int HUNDRET = 100;
	/** Days of a week. */
	public static final int DAYS_WEEK = 7;
	/** Hours of a day. */
	public static final int HOURS_DAY = 24;
	/** Seconds of a minute. */
	public static final int SECONDS_MINUTE = 60;

	/** {@link Currency} symbol. */
	public static String currencySymbol = "$";
	/** {@link Currency} fraction digits. */
	public static int currencyDigits = 2;

	@Override
	public void onCreate() {
		super.onCreate();
		Log.init("CallMeter3G");

		try {
			final Currency cur = Currency.getInstance(Locale.getDefault());
			currencySymbol = cur.getSymbol();
			currencyDigits = cur.getDefaultFractionDigits();
		} catch (Exception e) {
			Log.w(TAG, "error getting currency", e);
		}
	}
}
