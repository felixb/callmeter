/*
 * Copyright (C) 2009-2013 Felix Bechstein
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
package android.preference;

import java.util.Calendar;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.DatePicker;
import de.ub0r.android.lib.Utils;

/**
 * A {@link Preference} to choose a date.
 */
public final class DatePreference extends DialogPreference implements
		DatePicker.OnDateChangedListener {
	/** Global default value. */
	public static final long DEFAULT_VALUE = 1262300400001L; // 1.1.2010
	/** Default value. */
	private long defValue = DEFAULT_VALUE;

	/**
	 * Default constructor.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param attrs
	 *            {@link AttributeSet}
	 */
	public DatePreference(final Context context, final AttributeSet attrs) {
		super(context, attrs);
		this.setPersistent(true);
	}

	/**
	 * Default constructor.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param attrs
	 *            {@link AttributeSet}
	 * @param defStyle
	 *            default style
	 */
	public DatePreference(final Context context, final AttributeSet attrs, final int defStyle) {
		super(context, attrs, defStyle);
		this.setPersistent(true);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected View onCreateDialogView() {
		final Calendar c = this.getPersitendCalendar(this.defValue);
		final DatePicker dp = new DatePicker(this.getContext());
		dp.init(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH), this);
		return dp;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setDefaultValue(final Object defaultValue) {
		super.setDefaultValue(defaultValue);
		if (defaultValue instanceof String) {
			this.defValue = Utils.parseLong((String) defaultValue, this.defValue);
		} else if (defaultValue instanceof Long) {
			this.defValue = (Long) defaultValue;
		}
	}

	/**
	 * Get a {@link Calendar} set to the value of this {@link Preference}.
	 * 
	 * @param defaultValue
	 *            default date
	 * @return {@link Calendar}
	 */
	private Calendar getPersitendCalendar(final long defaultValue) {
		final long date = this.getPersistedLong(defaultValue);
		final Calendar c = Calendar.getInstance();
		c.setTimeInMillis(date);
		return c;
	}

	/**
	 * {@inheritDoc}
	 */
	public void onDateChanged(final DatePicker view, final int year, final int monthOfYear,
			final int dayOfMonth) {
		Calendar c = Calendar.getInstance();
		c.set(year, monthOfYear, dayOfMonth);
		this.persistLong(c.getTimeInMillis());
	}
}