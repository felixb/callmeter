/*
 * Copyright (C) 2010 Felix Bechstein, The Android Open Source Project
 * 
 * This file is part of CallMeter.
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

package de.ub0r.de.android.callMeterNG;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

/**
 * Wrap around contacts API.
 * 
 * @author flx
 */
public abstract class ContactsWrapper {
	/** Tag for output. */
	private static final String TAG = "CallMeter.cw";

	/**
	 * Static singleton instance of {@link ContactsWrapper} holding the
	 * SDK-specific implementation of the class.
	 */
	private static ContactsWrapper sInstance;

	/**
	 * Get instance.
	 * 
	 * @return {@link ContactsWrapper}
	 */
	public static final ContactsWrapper getInstance() {
		if (sInstance == null) {

			String className;

			/**
			 * Check the version of the SDK we are running on. Choose an
			 * implementation class designed for that version of the SDK.
			 * Unfortunately we have to use strings to represent the class
			 * names. If we used the conventional
			 * ContactAccessorSdk5.class.getName() syntax, we would get a
			 * ClassNotFoundException at runtime on pre-Eclair SDKs. Using the
			 * above syntax would force Dalvik to load the class and try to
			 * resolve references to all other classes it uses. Since the
			 * pre-Eclair does not have those classes, the loading of
			 * ContactAccessorSdk5 would fail.
			 */
			int sdkVersion = Integer.parseInt(Build.VERSION.SDK);
			// Cupcake style
			if (sdkVersion < Build.VERSION_CODES.ECLAIR) {
				className = "de.ub0r.de.android.callMeterNG.ContactsWrapper3";
			} else {
				className = "de.ub0r.de.android.callMeterNG.ContactsWrapper5";
			}

			// Find the required class by name and instantiate it.
			try {
				Class<? extends ContactsWrapper> clazz = Class.forName(
						className).asSubclass(ContactsWrapper.class);
				sInstance = clazz.newInstance();
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
			Log.d(TAG, "getInstance(): " + sInstance.getClass().getName());
		}
		return sInstance;
	}

	/**
	 * Get {@link Uri} for filter contacts by address.
	 * 
	 * @return {@link Uri}
	 */
	public abstract Uri getUriFilter();

	/**
	 * Get projection for filter contacts by address.
	 * 
	 * @return projection
	 */
	public abstract String[] getProjectionFilter();

	/**
	 * Resolve address to name.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param address
	 *            address
	 * @return name, address if not found
	 */
	public final String getNameForAddress(final Context context,
			final String address) {
		final ContactsWrapper w = getInstance();
		final Uri uri = Uri.withAppendedPath(w.getUriFilter(), address);
		final String[] proj = w.getProjectionFilter();
		final Cursor cursor = context.getContentResolver().query(uri, proj,
				null, null, null);
		if (cursor != null && cursor.moveToFirst()) {
			final String ret = cursor.getString(1);
			if (ret != null) {
				return ret;
			}
		}
		return address;
	}
}
