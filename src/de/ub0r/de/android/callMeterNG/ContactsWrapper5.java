/*
 * Copyright (C) 2010 Felix Bechstein
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

import android.content.Intent;
import android.net.Uri;
import android.provider.ContactsContract;

/**
 * Helper class to set/unset background for api5 systems.
 * 
 * @version 5
 * @author flx
 */
public final class ContactsWrapper5 extends ContactsWrapper {
	/** {@link Uri} for persons, content filter. */
	private static final Uri API5_URI_CONTENT_FILTER = // .
	ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI;

	/** Projection for persons query, filter. */
	private static final String[] API5_PROJECTION_FILTER = // .
	new String[] { ContactsContract.Data.CONTACT_ID,
			ContactsContract.Data.DISPLAY_NAME,
			ContactsContract.CommonDataKinds.Phone.NUMBER };

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Uri getUriFilter() {
		return API5_URI_CONTENT_FILTER;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String[] getProjectionFilter() {
		return API5_PROJECTION_FILTER;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Intent getPickPhoneIntent() {
		final Intent i = new Intent(Intent.ACTION_GET_CONTENT);
		i.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
		return i;
	}
}
