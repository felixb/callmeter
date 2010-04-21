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

import android.os.Build;
import android.util.Log;

/**
 * Wrap around Telephony API.
 * 
 * @author flx
 */
public abstract class TelephonyWrapper {
	/** Tag for output. */
	private static final String TAG = "CallMeter.tw";

	/**
	 * Static singleton instance of {@link TelephonyWrapper} holding the
	 * SDK-specific implementation of the class.
	 */
	private static TelephonyWrapper sInstance;

	/**
	 * Get instance.
	 * 
	 * @return {@link TelephonyWrapper}
	 */
	public static final TelephonyWrapper getInstance() {
		if (sInstance == null) {

			String className;

			/**
			 * Check the version of the SDK we are running on. Choose an
			 * implementation class designed for that version of the SDK.
			 * Unfortunately we have to use strings to represent the class
			 * names. If we used the conventional
			 * TelephonyWrapper4.class.getName() syntax, we would get a
			 * ClassNotFoundException at runtime on pre-Donut SDKs. Using the
			 * above syntax would force Dalvik to load the class and try to
			 * resolve references to all other classes it uses. Since the
			 * pre-Donut does not have those classes, the loading of
			 * TelephonyWrapper4 would fail.
			 */
			int sdkVersion = Integer.parseInt(Build.VERSION.SDK);
			// Cupcake style
			if (sdkVersion < Build.VERSION_CODES.DONUT) {
				className = "de.ub0r.de.android.callMeterNG.TelephonyWrapper3";
			} else {
				className = "de.ub0r.de.android.callMeterNG.TelephonyWrapper4";
			}

			// Find the required class by name and instantiate it.
			try {
				Class<? extends TelephonyWrapper> clazz = Class.forName(
						className).asSubclass(TelephonyWrapper.class);
				sInstance = clazz.newInstance();
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
			Log.d(TAG, "getInstance(): " + sInstance.getClass().getName());
		}
		return sInstance;
	}

	/**
	 * Calculates the number of SMS's required to encode the message body and
	 * the number of characters remaining until the next message, given the
	 * current encoding.
	 * 
	 * @param messageBody
	 *            the message to encode
	 * @param use7bitOnly
	 *            if true, characters that are not part of the radio specific
	 *            (GSM / CDMA) alphabet encoding are converted to as a single
	 *            space characters. If false, a messageBody containing non-GSM
	 *            or non-CDMA alphabet characters are encoded using 16-bit
	 *            encoding.
	 * @return an int[4] with int[0] being the number of SMS's required, int[1]
	 *         the number of code units used, and int[2] is the number of code
	 *         units remaining until the next message. int[3] is the encoding
	 *         type that should be used for the message.
	 */
	public abstract int[] calculateLength(String messageBody,
			boolean use7bitOnly);
}
