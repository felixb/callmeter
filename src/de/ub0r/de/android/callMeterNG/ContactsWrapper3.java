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
import android.provider.Contacts;
import android.provider.Contacts.PeopleColumns;
import android.provider.Contacts.Phones;
import android.provider.Contacts.PhonesColumns;
import android.provider.Contacts.People.Extensions;

/**
 * Implement {@link ContactsWrapper} for API 3 and 4.
 * 
 * @version 3
 * @author flx
 */
@SuppressWarnings("deprecation")
public final class ContactsWrapper3 extends ContactsWrapper {
	/** {@link Uri} for persons, content filter. */
	private static final Uri URI_CONTENT_FILTER = // .
	Contacts.Phones.CONTENT_FILTER_URL;

	/** Projection for persons query, filter. */
	private static final String[] PROJECTION_FILTER = // .
	new String[] { Extensions.PERSON_ID, PeopleColumns.DISPLAY_NAME,
			PhonesColumns.NUMBER };

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Uri getUriFilter() {
		return URI_CONTENT_FILTER;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String[] getProjectionFilter() {
		return PROJECTION_FILTER;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Intent getPickPhoneIntent() {
		final Intent i = new Intent(Intent.ACTION_GET_CONTENT);
		i.setType(Phones.CONTENT_ITEM_TYPE);
		return i;
	}
}
