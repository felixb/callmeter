/*
 * Copyright (C) 2009-2012 Felix Bechstein
 * 
 * This file is part of CallMeter 3G.
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
package de.ub0r.android.callmeter.data;

import android.support.v4.util.LruCache;

/**
 * {@link LruCache} holding number to name entries.
 * 
 * @author flx
 */
public final class NameCache extends LruCache<String, String> {
	/** Maximum cache size. */
	private static final int MAXCACHESIZE = 100;

	/** Single instance. */
	private static NameCache instance;

	/**
	 * Get the cache.
	 * 
	 * @return the single {@link NameCache} instance
	 */
	public static NameCache getInstance() {
		if (instance == null) {
			instance = new NameCache();
		}
		return instance;
	}

	/** hide public constructor. */
	private NameCache() {
		super(MAXCACHESIZE);
	}

	/**
	 * Return get(key) formated with format.
	 * 
	 * @param key
	 *            key
	 * @param format
	 *            format
	 * @return formated {@link String}
	 */
	public String get(final String key, final String format) {
		String s = this.get(key);
		if (s == null) {
			return null;
		} else {
			return String.format(format, s);
		}
	}
}
