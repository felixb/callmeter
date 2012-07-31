/*
 * Copyright (C) 2009-2011 Felix Bechstein
 * 
 * This file is part of Call Meter NG.
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

import java.util.HashSet;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.widget.TextView;
import de.ub0r.android.lib.ChangelogHelper;
import de.ub0r.android.lib.DonationHelper;
import de.ub0r.android.lib.Utils;

/**
 * The main Activity, holding all data.
 * 
 * @author flx
 */
public class CallMeter extends Activity {
	/** Tag for output. */
	public static final String TAG = "main";

	/** Ad's unit id. */
	private static final String AD_UNITID = "a14c74c2173de45";

	/** Ad's keywords. */
	private static final HashSet<String> AD_KEYWORDS = new HashSet<String>();
	static {
		AD_KEYWORDS.add("android");
		AD_KEYWORDS.add("mobile");
		AD_KEYWORDS.add("handy");
		AD_KEYWORDS.add("cellphone");
		AD_KEYWORDS.add("google");
		AD_KEYWORDS.add("htc");
		AD_KEYWORDS.add("samsung");
		AD_KEYWORDS.add("motorola");
		AD_KEYWORDS.add("market");
		AD_KEYWORDS.add("app");
		AD_KEYWORDS.add("report");
		AD_KEYWORDS.add("calls");
		AD_KEYWORDS.add("game");
		AD_KEYWORDS.add("traffic");
		AD_KEYWORDS.add("data");
		AD_KEYWORDS.add("amazon");
	}

	/** 100. */
	static final int HUNDRET = 100;

	/** Preference's name: show short title for data. */
	private static final String PREFS_DATA_SHORT = "data_short";

	/** Display ads? */
	private static boolean prefsNoAds;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		this.setTheme(Preferences.getTheme(this));
		Utils.setLocale(this);
		this.setContentView(R.layout.main);

		ChangelogHelper.showChangelog(this,
				this.getString(R.string.changelog_),
				this.getString(R.string.app_name), R.array.updates,
				R.array.notes_from_dev);

		prefsNoAds = DonationHelper.hideAds(this);

		TextView tv = (TextView) this.findViewById(R.id.calls_);
		Preferences.textSizeMedium = tv.getTextSize();
		tv = (TextView) this.findViewById(R.id.calls1_in_);
		Preferences.textSizeSmall = tv.getTextSize();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void onResume() {
		super.onResume();
		// get call/sms stats
		new Updater(this).execute((Void[]) null);
		// get data stats
		new UpdaterData(this).execute((Void[]) null);
		// schedule next update
		CMBroadcastReceiver.schedNext(this);
		if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
				PREFS_DATA_SHORT, false)) {
			((TextView) this.findViewById(R.id.data_)).setText(R.string.data);
		} else {
			((TextView) this.findViewById(R.id.data_)).setText(R.string.data_);
		}
		if (!prefsNoAds) {
			Ads.loadAd(this, R.id.ad, AD_UNITID, AD_KEYWORDS);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final boolean onCreateOptionsMenu(final Menu menu) {
		MenuInflater inflater = this.getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		if (prefsNoAds) {
			menu.removeItem(R.id.item_donate);
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case R.id.item_settings:
			this.startActivity(new Intent(this, Preferences.class));
			return true;
		case R.id.item_donate:
			DonationHelper.showDonationDialog(
					this,
					this.getString(R.string.donate),
					this.getString(R.string.donate_url),
					this.getString(R.string.donate_),
					this.getString(R.string.did_paypal_donation),
					this.getString(R.string.remove_ads_),
					this.getResources().getStringArray(
							R.array.donation_messages_market),
					this.getResources().getStringArray(
							R.array.donation_messages_paypal),
					this.getResources().getStringArray(
							R.array.donation_messages_load));
			return true;
		default:
			return false;
		}
	}
}
