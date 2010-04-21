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

/**
 * Wrap around Telephony API.
 * 
 * @version 3
 * @author flx
 */
public final class TelephonyWrapper3 extends TelephonyWrapper {

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("deprecation")
	@Override
	public int[] calculateLength(final String messageBody,
			final boolean use7bitOnly) {
		return android.telephony.gsm.SmsMessage.calculateLength(messageBody,
				use7bitOnly);
	}
}
