/*
 * Copyright (C) 2010 Felix Bechstein
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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;

import com.admob.android.ads.AdView;

/**
 * The main Activity, holding all data.
 * 
 * @author flx
 */
public class CallMeter extends Activity {
	/** Tag for output. */
	private static final String TAG = "CallMeterNG";
	/** Dialog: post donate. */
	private static final int DIALOG_POSTDONATE = 0;
	/** Dialog: about. */
	private static final int DIALOG_ABOUT = 1;
	/** Dialog: update. */
	private static final int DIALOG_UPDATE = 2;
	/** Dialog: pre donate. */
	private static final int DIALOG_PREDONATE = 3;

	/** Prefs: name for last version run. */
	private static final String PREFS_LAST_RUN = "lastrun";
	/** Prefs: name for first day. */
	static final String PREFS_BILLDAY = "billday";
	/** Prefs: name for billingmode. */
	static final String PREFS_BILLMODE = "billmode";
	/** Prefs: name for smsperiod. */
	static final String PREFS_SMSPERIOD = "smsperiod";
	/** Prefs: name for smsbillday. */
	static final String PREFS_SMSBILLDAY = "smsbillday";

	/** Prefs: split plans. */
	static final String PREFS_SPLIT_PLANS = "plans_split";
	/** Prefs: merge plans for calls. */
	static final String PREFS_MERGE_PLANS_CALLS = "plans_merge_calls";
	/** Prefs: merge plans for sms. */
	static final String PREFS_MERGE_PLANS_SMS = "plans_merge_sms";
	/** Prefs: hours for plan 1. */
	static final String PREFS_PLAN1_HOURS_PREFIX = "hours_1_";

	/** Prefs: plan1 totally free calls. */
	static final String PREFS_PLAN1_T_FREE_CALLS = "plan1_total_free_calls";
	/** Prefs: plan1 free minutes. */
	static final String PREFS_PLAN1_FREEMIN = "plan1_freemin";
	/** Prefs: plan1 totally free sms. */
	static final String PREFS_PLAN1_T_FREE_SMS = "plan1_total_free_sms";
	/** Prefs: plan1 free sms. */
	static final String PREFS_PLAN1_FREESMS = "plan1_freesms";
	/** Prefs: plan1 totally free calls. */
	static final String PREFS_PLAN2_T_FREE_CALLS = "plan2_total_free_calls";
	/** Prefs: plan1 free minutes. */
	static final String PREFS_PLAN2_FREEMIN = "plan2_freemin";
	/** Prefs: plan1 totally free sms. */
	static final String PREFS_PLAN2_T_FREE_SMS = "plan2_total_free_sms";
	/** Prefs: plan1 free sms. */
	static final String PREFS_PLAN2_FREESMS = "plan2_freesms";

	/** Prefs: custom name for plan 1. */
	static final String PREFS_NAME_PLAN1 = "plan_name1";
	/** Prefs: custom name for plan 2. */
	static final String PREFS_NAME_PLAN2 = "plan_name2";

	/** Prefs: merge sms into calls. */
	static final String PREFS_MERGE_SMS_TO_CALLS = "merge_sms_calls";
	/**
	 * Prefs: merge sms into calls; number of seconds billed for a single sms.
	 */
	static final String PREFS_MERGE_SMS_TO_CALLS_SECONDS = // .
	"merge_sms_calls_sec";
	/** Prefs: merge sms into calls; which plan to merge sms in. */
	static final String PREFS_MERGE_SMS_PLAN1 = "merge_sms_plan1";

	/** Prefs: name for all old calls in. */
	// static final String PREFS_ALL_CALLS_IN = "all_calls_in";
	/** Prefs: name for all old calls out. */
	// static final String PREFS_ALL_CALLS_OUT = "all_calls_out";
	/** Prefs: name for all old sms in. */
	// static final String PREFS_ALL_SMS_IN = "all_sms_in";
	/** Prefs: name for all old sms out. */
	// static final String PREFS_ALL_SMS_OUT = "all_sms_out";
	/** Prefs: name for date of old calls/sms. */
	// static final String PREFS_DATE_OLD = "all_date_old";
	/** Prefs: Exclude people prefix. */
	static final String PREFS_EXCLUDE_PEOPLE_PREFIX = "exclude_people_";
	/** Prefs: Exclude people count. */
	static final String PREFS_EXCLUDE_PEOPLE_COUNT = PREFS_EXCLUDE_PEOPLE_PREFIX
			+ "n";
	/** Prefs: Bill excluded people to plan #1. */
	static final String PREFS_EXCLUDE_PEOPLE_PLAN1 = PREFS_EXCLUDE_PEOPLE_PREFIX
			+ "to_plan1";
	/** Prefs: Bill excluded people to plan #2. */
	static final String PREFS_EXCLUDE_PEOPLE_PLAN2 = PREFS_EXCLUDE_PEOPLE_PREFIX
			+ "to_plan2";

	/** Prefs: enable data stats. */
	static final String PREFS_DATA_ENABLE = "data_enable";
	/** Prefs: bill each date separately. */
	static final String PREFS_DATA_EACHDAY = "data_eachday";
	/** Prefs: limit for data traffic. */
	static final String PREFS_DATA_LIMIT = "data_limit";

	/** Prefs: data in at last boot. */
	static final String PREFS_DATA_BOOT_IN = "data_boot_in";
	/** Prefs: data out at last boot. */
	static final String PREFS_DATA_BOOT_OUT = "data_boot_out";
	/** Prefs: data in after last boot. */
	static final String PREFS_DATA_RUNNING_IN = "data_running_in";
	/** Prefs: data out after last boot. */
	static final String PREFS_DATA_RUNNING_OUT = "data_running_out";
	/** Prefs: data in before bolling date. */
	static final String PREFS_DATA_PREBILLING_IN = "data_prebilling_in";
	/** Prefs: data out before billing date. */
	static final String PREFS_DATA_PREBILLING_OUT = "data_prebilling_out";
	/** Prefs: date of last billing. */
	static final String PREFS_DATA_LASTCHECK = "data_lastcheck";

	/** Prefs: billmode: 1/1. */
	static final String BILLMODE_1_1 = "1_1";

	/** ContentProvider Column: Body. */
	static final String BODY = "body";

	/** SharedPreferences. */
	private SharedPreferences preferences;

	/** Unique ID of device. */
	private String imeiHash = null;
	/** Display ads? */
	private static boolean prefsNoAds;

	/** Preferences: excluded numbers. */
	static ArrayList<String> prefsExcludePeople;
	/** ArrayAdapter for excluded numbers. */
	static ArrayAdapter<String> excludedPeaoplAdapter;

	/** Array of md5(imei) for which no ads should be displayed. */
	private static final String[] NO_AD_HASHS = { // .
	"43dcb861b9588fb733300326b61dbab9", // me
			"d9018351e0159dd931e20cc1861ac5d8", // Tommaso C.
			"2c72e52ef02a75210dc6680edab6b75d", // Danny S.
			"f39b49859c04e6ea7849b43c73bd050e", // Lukasz M.
			"225905ca10fd56ae9c4b82254fa6d490", // George K.
			"9e30468a2b516aac2d1ddf2a875ca8b8", // Alfons V.
			"4bf7f35515fb7306dc7c43c9fa88558c", // Ronny T.
			"75b9d156ebfda12a0e63da875593edc0", // Angel M.
			"80cfd25e841424e968db64de0d7d236e", // Renato P.
			"cb4d969c66def366b56200d87d3c363c", // Daniel S.
			"9044aee31eaba23bb55f7cdf01d563ec", // Ruurd O.
			"09eec4cc097d44c222470785fa19c75d", // Oleg J.
			"3273e9f7b49d65c02cfc75a2730530a8", // Pjotr d. B.
			"fe9c39f3ee0fdaffeda8ffbfe4105c7d", // Chrispen F.
			"e3563c28b3916d95b9ef126202385c2b", // Istvan P.
	};

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		this.setContentView(R.layout.main);
		// get prefs.
		this.preferences = PreferenceManager.getDefaultSharedPreferences(this);
		String v0 = this.preferences.getString(PREFS_LAST_RUN, "");
		String v1 = this.getString(R.string.app_version);
		if (!v0.equals(v1)) {
			SharedPreferences.Editor editor = this.preferences.edit();
			editor.putString(PREFS_LAST_RUN, v1);
			editor.commit();
			this.showDialog(DIALOG_UPDATE);
		}
		// get imei
		TelephonyManager mTelephonyMgr = (TelephonyManager) this
				.getSystemService(TELEPHONY_SERVICE);
		final String s = mTelephonyMgr.getDeviceId();
		prefsNoAds = false;
		if (s != null) {
			this.imeiHash = md5(s);
			for (String h : NO_AD_HASHS) {
				if (this.imeiHash.equals(h)) {
					prefsNoAds = true;
					break;
				}
			}
		}
		prefsExcludePeople = new ArrayList<String>();
		prefsExcludePeople.add(this.getString(R.string.exclude_people_add));
		final int c = this.preferences.getInt(PREFS_EXCLUDE_PEOPLE_COUNT, 0);
		for (int i = 0; i < c; i++) {
			CallMeter.prefsExcludePeople.add(this.preferences.getString(
					PREFS_EXCLUDE_PEOPLE_PREFIX + i, "???"));
		}
		excludedPeaoplAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, prefsExcludePeople);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void onResume() {
		super.onResume();
		if (!prefsNoAds) {
			((AdView) this.findViewById(R.id.ad)).setVisibility(View.VISIBLE);
		}
		// get call/sms stats
		new Updater(this).execute((Void[]) null);
		// get data stats
		new UpdaterData(this).execute((Void[]) null);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final Dialog onCreateDialog(final int id) {
		Dialog d;
		AlertDialog.Builder builder;
		switch (id) {
		case DIALOG_PREDONATE:
			builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.donate_);
			builder.setMessage(R.string.predonate);
			builder.setPositiveButton(R.string.donate_,
					new DialogInterface.OnClickListener() {
						public void onClick(final DialogInterface dialog,
								final int which) {
							try {
								CallMeter.this.startActivity(new // .
										Intent(Intent.ACTION_VIEW, // .
												Uri.parse(CallMeter.this
														.getString(// .
														R.string.donate_url))));
							} catch (ActivityNotFoundException e) {
								Log.e(TAG, "no browser", e);
							} finally {
								CallMeter.this.showDialog(DIALOG_POSTDONATE);
							}
						}
					});
			builder.setNegativeButton(android.R.string.cancel, null);
			return builder.create();
		case DIALOG_POSTDONATE:
			builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.remove_ads_);
			builder.setMessage(R.string.postdonate);
			builder.setPositiveButton(R.string.send_,
					new DialogInterface.OnClickListener() {
						public void onClick(final DialogInterface dialog,
								final int which) {
							final Intent in = new Intent(Intent.ACTION_SEND);
							in.putExtra(Intent.EXTRA_EMAIL, new String[] {
									CallMeter.this
											.getString(R.string.donate_mail),
									"" }); // FIXME: "" is a k9 hack.
							in.putExtra(Intent.EXTRA_TEXT,
									CallMeter.this.imeiHash);
							in.putExtra(Intent.EXTRA_SUBJECT, CallMeter.this
									.getString(// .
									R.string.app_name)
									+ " " + CallMeter.this.getString(// .
											R.string.donate_subject));
							in.setType("text/plain");
							CallMeter.this.startActivity(in);
						}
					});
			builder.setNegativeButton(android.R.string.cancel, null);
			return builder.create();
		case DIALOG_ABOUT:
			d = new Dialog(this);
			d.setContentView(R.layout.about);
			d.setTitle(this.getString(R.string.about_) + " v"
					+ this.getString(R.string.app_version));
			return d;
		case DIALOG_UPDATE:
			builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.changelog_);
			final String[] changes = this.getResources().getStringArray(
					R.array.updates);
			StringBuilder buf = new StringBuilder();

			buf.append(this.getString(R.string.see_about));

			for (int i = 0; i < changes.length; i++) {
				buf.append("\n\n");
				buf.append(changes[i]);
			}
			builder.setIcon(android.R.drawable.ic_menu_info_details);
			builder.setMessage(buf.toString());
			buf = null;
			builder.setCancelable(true);
			builder.setPositiveButton(android.R.string.ok,
					new DialogInterface.OnClickListener() {
						public void onClick(final DialogInterface dialog,
								final int id) {
							dialog.cancel();
						}
					});
			return builder.create();
		default:
			return null;
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
		case R.id.item_about: // start about dialog
			this.showDialog(DIALOG_ABOUT);
			return true;
		case R.id.item_settings: // start settings activity
			this.startActivity(new Intent(this, Preferences.class));
			return true;
		case R.id.item_donate:
			CallMeter.this.showDialog(DIALOG_PREDONATE);
			return true;
		case R.id.item_more:
			try {
				this.startActivity(new Intent(Intent.ACTION_VIEW, Uri
						.parse("market://search?q=pub:\"Felix Bechstein\"")));
			} catch (ActivityNotFoundException e) {
				Log.e(TAG, "no market", e);
			}
			return true;
		default:
			return false;
		}
	}

	/**
	 * Calc MD5 Hash from String.
	 * 
	 * @param s
	 *            input
	 * @return hash
	 */
	private static String md5(final String s) {
		try {
			// Create MD5 Hash
			MessageDigest digest = java.security.MessageDigest
					.getInstance("MD5");
			digest.update(s.getBytes());
			byte[] messageDigest = digest.digest();
			// Create Hex String
			StringBuilder hexString = new StringBuilder(32);
			int b;
			for (int i = 0; i < messageDigest.length; i++) {
				b = 0xFF & messageDigest[i];
				if (b < 0x10) {
					hexString.append('0' + Integer.toHexString(b));
				} else {
					hexString.append(Integer.toHexString(b));
				}
			}
			return hexString.toString();
		} catch (NoSuchAlgorithmException e) {
			Log.e(TAG, null, e);
		}
		return "";
	}
}
