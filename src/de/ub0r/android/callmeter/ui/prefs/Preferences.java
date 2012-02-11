/*
 * Copyright (C) 2009-2012 Felix Bechstein
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
package de.ub0r.android.callmeter.ui.prefs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.Currency;
import java.util.Locale;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.appwidget.AppWidgetManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;
import de.ub0r.android.callmeter.CallMeter;
import de.ub0r.android.callmeter.R;
import de.ub0r.android.callmeter.data.DataProvider;
import de.ub0r.android.callmeter.data.Device;
import de.ub0r.android.callmeter.ui.HelpActivity;
import de.ub0r.android.callmeter.widget.StatsAppWidgetConfigure;
import de.ub0r.android.callmeter.widget.StatsAppWidgetProvider;
import de.ub0r.android.lib.Log;
import de.ub0r.android.lib.Market;
import de.ub0r.android.lib.Utils;

/**
 * Preferences.
 * 
 * @author flx
 */
public final class Preferences extends PreferenceActivity implements OnPreferenceChangeListener,
		OnPreferenceClickListener {
	/** Tag for output. */
	private static final String TAG = "prefs";

	/** Standard buffer size. */
	public static final int BUFSIZE = 1024;

	/** Preference's name: is default rule set. */
	public static final String PREFS_ISDEFAULT = "is_default_ruleset";
	/** Preference's name: show advanced preferences. */
	public static final String PREFS_ADVANCED = "advanced_preferences";
	/** Preference's name: split messages at 160chars. */
	public static final String PREFS_SPLIT_SMS_AT_160 = "split_at_160";
	/** Preference's name: alert at 80% usage. */
	public static final String PREFS_ALERT80 = "alert_80";
	/** Preference's name: alert at 100% usage. */
	public static final String PREFS_ALERT100 = "alert_100";
	/** Preference's name: update interval. */
	public static final String PREFS_UPDATE_INTERVAL = "update_interval";
	/** Preference's name: update interval (data). */
	public static final String PREFS_UPDATE_INTERVAL_DATA = "update_interval_data";
	/** Preference's name: beginning of record. */
	public static final String PREFS_DATE_BEGIN = "date_begin";
	/** Preference's name: delete old logs before x days. */
	public static final String PREFS_DELETE_OLD_LOGS = "delete_old_logs";
	/** Preference's name: prepaid plan. */
	public static final String PREFS_PREPAID = "prepaid";

	/** Preference's name: theme. */
	private static final String PREFS_THEME = "theme";
	/** Theme: black. */
	private static final String THEME_BLACK = "black";
	/** Theme: light. */
	private static final String THEME_LIGHT = "light";
	/** Preference's name: text size. */
	private static final String PREFS_TEXTSIZE = "textsize";
	/** Preference's name: text size for big titles. */
	private static final String PREFS_TEXTSIZE_BIGTITLE = "textsize_bigtitle";
	/** Preference's name: text size for titles. */
	private static final String PREFS_TEXTSIZE_TITLE = "textsize_title";
	/** Preference's name: text size for spacers. */
	private static final String PREFS_TEXTSIZE_SPACER = "textsize_spacer";
	/** Preference's name: text size for progress bars. */
	private static final String PREFS_TEXTSIZE_PBAR = "textsize_pbar";
	/** Preference's name: text size for progress bars of billing periods. */
	private static final String PREFS_TEXTSIZE_PBARBP = "textsize_pbarbp";
	/** Preference's name: show hours. */
	public static final String PREFS_SHOWHOURS = "show_hours";
	/** Preference's name: hide zero. */
	public static final String PREFS_HIDE_ZERO = "hide_zero";
	/** Preference's name: hide no cost plans. */
	public static final String PREFS_HIDE_NOCOST = "hide_nocost";
	/** Preference's name: hide progress bars in main view. */
	public static final String PREFS_HIDE_PROGRESSBARS = "hide_progressbars";
	/** Preference's name: custom delimiter. */
	public static final String PREFS_DELIMITER = "custom_delimiter";
	/** Preference's name: currency symbol. */
	private static final String PREFS_CURRENCY_SYMBOL = "currency_symbol";
	/** Preference's name: currency format. */
	private static final String PREFS_CURRENCY_FORMAT = "currency_format";
	/** Preference's name: date format. */
	private static final String PREFS_DATE_FORMAT = "date_format";
	/** Preference's name: show today. */
	public static final String PREFS_SHOWTODAY = "show_today";
	/** Preference's name: show total. */
	public static final String PREFS_SHOWTOTAL = "show_total";
	/** Preference's name: show help. */
	public static final String PREFS_SHOWHELP = "show_help";
	/** Preference's name: show length/cost of call. */
	public static final String PREFS_SHOWCALLINFO = "show_callinfo";
	/** Preference's name: strip leading zeros. */
	public static final String PREFS_STRIP_LEADING_ZEROS = "strip_leading_zeros";
	/** Preference's name: international prefix. */
	public static final String PREFS_INT_PREFIX = "intPrefix";
	/** Preference's name: ask for plan. */
	public static final String PREFS_ASK_FOR_PLAN = "ask_for_plan";
	/** Preference's name: ask for plan, auto hide. */
	public static final String PREFS_ASK_FOR_PLAN_AUTOHIDE = "autohide";
	/** Preference's name: default plan for ask for plan. */
	public static final String PREFS_ASK_FOR_PLAN_DEFAULT = "ask_for_plan_default";

	/** Default rule set. */
	private static final InputStream IS_DEFAULT = new InputStream() {
		@Override
		public int read() throws IOException {
			return 0;
		}
	};

	/** {@link Currency} symbol. */
	private static String defaultCurrencySymbol = null;
	/** {@link Currency} fraction digits. */
	private static int defaultCurrencyDigits = 2;

	/**
	 * Get time stamp to delete logs before that date.
	 * 
	 * @param p
	 *            {@link SharedPreferences}
	 * @return time in milliseconds
	 */
	public static long getDeleteLogsBefore(final SharedPreferences p) {
		if (p == null) {
			return -1L;
		}
		final long dlb = Utils.parseLong(p.getString(PREFS_DELETE_OLD_LOGS, "90"), -1L);
		if (dlb < 0L) {
			return dlb;
		}
		return System.currentTimeMillis() - (dlb * CallMeter.MILLIS * CallMeter.SECONDS_DAY);
	}

	/**
	 * Get Theme from Preferences.
	 * 
	 * @param context
	 *            {@link Context}
	 * @return theme
	 */
	public static int getTheme(final Context context) {
		final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
		final String s = p.getString(PREFS_THEME, THEME_LIGHT);
		if (s != null && THEME_BLACK.equals(s)) {
			return R.style.Theme_SherlockUb0r;
		}
		return R.style.Theme_SherlockUb0r_Light;
	}

	/**
	 * Get Text size from Preferences.
	 * 
	 * @param context
	 *            {@link Context}
	 * @return text size
	 */
	public static int getTextsize(final Context context) {
		final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
		final String s = p.getString(PREFS_TEXTSIZE, null);
		return Utils.parseInt(s, 0);
	}

	/**
	 * Get Text size for big titles from Preferences.
	 * 
	 * @param context
	 *            {@link Context}
	 * @return text size
	 */
	public static int getTextsizeBigTitle(final Context context) {
		final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
		final String s = p.getString(PREFS_TEXTSIZE_BIGTITLE, null);
		return Utils.parseInt(s, 0);
	}

	/**
	 * Get Text size for titles from Preferences.
	 * 
	 * @param context
	 *            {@link Context}
	 * @return text size
	 */
	public static int getTextsizeTitle(final Context context) {
		final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
		final String s = p.getString(PREFS_TEXTSIZE_TITLE, null);
		return Utils.parseInt(s, 0);
	}

	/**
	 * Get Text size for spacers from Preferences.
	 * 
	 * @param context
	 *            {@link Context}
	 * @return text size
	 */
	public static int getTextsizeSpacer(final Context context) {
		final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
		final String s = p.getString(PREFS_TEXTSIZE_SPACER, null);
		return Utils.parseInt(s, 0);
	}

	/**
	 * Get Text size for progress bars from Preferences.
	 * 
	 * @param context
	 *            {@link Context}
	 * @return text size
	 */
	public static int getTextsizeProgressBar(final Context context) {
		final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
		final String s = p.getString(PREFS_TEXTSIZE_PBAR, null);
		return Utils.parseInt(s, 0);
	}

	/**
	 * Get Text size for progress bars of billing periods from Preferences.
	 * 
	 * @param context
	 *            {@link Context}
	 * @return text size
	 */
	public static int getTextsizeProgressBarBP(final Context context) {
		final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
		final String s = p.getString(PREFS_TEXTSIZE_PBARBP, null);
		return Utils.parseInt(s, 0);
	}

	/**
	 * Get the currency symbol from {@link SharedPreferences}.
	 * 
	 * @param context
	 *            {@link Context}
	 * @return currency symbol
	 */
	public static String getCurrencySymbol(final Context context) {
		final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
		final String pcs = p.getString(PREFS_CURRENCY_SYMBOL, "");
		if (pcs.length() == 0) {
			if (defaultCurrencySymbol == null) {
				try {
					final Currency cur = Currency.getInstance(Locale.getDefault());
					defaultCurrencySymbol = cur.getSymbol();
					defaultCurrencyDigits = cur.getDefaultFractionDigits();
				} catch (IllegalArgumentException e) {
					Log.e(TAG, "error getting currency", e);
					defaultCurrencySymbol = "$";
					defaultCurrencyDigits = 2;
				}
			}
			return defaultCurrencySymbol;
		} else {
			return pcs;
		}
	}

	/**
	 * Get the currency format from {@link SharedPreferences}.
	 * 
	 * @param context
	 *            {@link Context}
	 * @return currency format
	 */
	public static String getCurrencyFormat(final Context context) {
		final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
		final String pcs = p.getString(PREFS_CURRENCY_FORMAT, "");
		if (pcs.length() == 0) {
			if (defaultCurrencySymbol == null) {
				try {
					final Currency cur = Currency.getInstance(Locale.getDefault());
					defaultCurrencySymbol = cur.getSymbol();
					defaultCurrencyDigits = cur.getDefaultFractionDigits();
				} catch (IllegalArgumentException e) {
					Log.e(TAG, "error getting currency", e);
					defaultCurrencySymbol = "$";
					defaultCurrencyDigits = 2;
				}
			}
			return "%." + defaultCurrencyDigits + "f" + getCurrencySymbol(context);
		} else {
			Log.d(TAG, "custom currency format: " + pcs);
			String c = getCurrencySymbol(context);
			Log.d(TAG, "custom currency symbol: " + c);
			if (c.equals("$")) {
				c = "\\$";
				Log.d(TAG, "custom currency symbol: " + c);
			} else if (c.equals("%")) {
				c = "%%";
				Log.d(TAG, "custom currency symbol: " + c);
			}
			final String ret = pcs.replaceAll("\\$", c).replaceAll("\u20AC", c)
					.replaceAll("\u0440", c);
			Log.d(TAG, "custom currency format: " + ret);
			return ret;
		}
	}

	/**
	 * Get the date format from {@link SharedPreferences}.
	 * 
	 * @param context
	 *            {@link Context}
	 * @return date format
	 */
	public static String getDateFormat(final Context context) {
		final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
		final String pcs = p.getString(PREFS_DATE_FORMAT, "").replaceAll("\\$", "%");
		if (pcs.length() == 0) {
			return null;
		} else {
			return pcs;
		}
	}

	/**
	 * Set available of "simple preferences".
	 * 
	 * @param context
	 *            {@link Context}
	 * @param isDefault
	 *            true, if default rule set is loaded
	 */
	public static void setDefaultPlan(final Context context, final boolean isDefault) {
		final Editor e = PreferenceManager.getDefaultSharedPreferences(context).edit();
		e.putBoolean(PREFS_ISDEFAULT, isDefault);
		e.commit();
	}

	/**
	 * Delete data from logs.
	 * 
	 * @param type
	 *            type to delete; -1 for all
	 */
	private void resetData(final int type) {
		if (type < 0) {
			this.getContentResolver().delete(DataProvider.Logs.CONTENT_URI, null, null);
		} else {
			this.getContentResolver().delete(DataProvider.Logs.CONTENT_URI,
					DataProvider.Logs.TYPE + " = " + type, null);
		}
	}

	/**
	 * Reset internal Logs.
	 */
	private void resetDataDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.reset_data_);
		builder.setMessage(R.string.reset_data_hint);
		builder.setCancelable(false);
		builder.setPositiveButton(android.R.string.yes, new OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog, final int which) {
				Preferences.this.resetData(DataProvider.TYPE_CALL);
				Preferences.this.resetData(DataProvider.TYPE_SMS);
				Preferences.this.resetData(DataProvider.TYPE_MMS);
			}
		});
		builder.setNeutralButton(R.string.reset_data_data_, new OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog, final int which) {
				Preferences.this.resetData(-1);
			}
		});
		builder.setNegativeButton(android.R.string.no, null);
		builder.show();
	}

	/**
	 * Export data.
	 * 
	 * @param descr
	 *            description of the exported rule set
	 * @param fn
	 *            one of the predefined file names from {@link DataProvider}.
	 * @param recipient
	 *            recipient of export
	 */
	private void exportData(final String descr, final String fn, final String recipient) {
		if (descr == null) {
			final EditText et = new EditText(this);
			Builder builder = new Builder(this);
			builder.setView(et);
			builder.setCancelable(true);
			builder.setTitle(R.string.export_rules_descr);
			builder.setNegativeButton(android.R.string.cancel, null);
			builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
				@Override
				public void onClick(final DialogInterface dialog, final int which) {
					Preferences.this.exportData(et.getText().toString(), fn, recipient);
				}
			});
			builder.show();
		} else {
			final ProgressDialog d = new ProgressDialog(this);
			d.setIndeterminate(true);
			d.setMessage(this.getString(R.string.export_progr));
			d.setCancelable(false);
			d.show();

			// run task in background
			final AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {
				@Override
				protected String doInBackground(final Void... params) {
					if (fn.equals(DataProvider.EXPORT_RULESET_FILE)) {
						return DataProvider.backupRuleSet(Preferences.this, descr);
					} else if (fn.equals(DataProvider.EXPORT_LOGS_FILE)) {
						return DataProvider.backupLogs(Preferences.this, descr);
					} else if (fn.equals(DataProvider.EXPORT_NUMGROUPS_FILE)) {
						return DataProvider.backupNumGroups(Preferences.this, descr);
					} else if (fn.equals(DataProvider.EXPORT_HOURGROUPS_FILE)) {
						return DataProvider.backupHourGroups(Preferences.this, descr);
					}
					return null;
				}

				@Override
				protected void onPostExecute(final String result) {
					Log.d(TAG, "export:\n" + result);
					System.out.println("\n" + result);
					d.dismiss();
					if (result != null && result.length() > 0) {
						Uri uri = null;
						int resChooser = -1;
						if (fn.equals(DataProvider.EXPORT_RULESET_FILE)) {
							uri = DataProvider.EXPORT_RULESET_URI;
							resChooser = R.string.export_rules_;
						} else if (fn.equals(DataProvider.EXPORT_LOGS_FILE)) {
							uri = DataProvider.EXPORT_LOGS_URI;
							resChooser = R.string.export_logs_;
						} else if (fn.equals(DataProvider.EXPORT_NUMGROUPS_FILE)) {
							uri = DataProvider.EXPORT_NUMGROUPS_URI;
							resChooser = R.string.export_numgroups_;
						} else if (fn.equals(DataProvider.EXPORT_HOURGROUPS_FILE)) {
							uri = DataProvider.EXPORT_HOURGROUPS_URI;
							resChooser = R.string.export_hourgroups_;
						}
						final Intent intent = new Intent(Intent.ACTION_SEND);
						intent.setType(DataProvider.EXPORT_MIMETYPE);
						intent.putExtra(Intent.EXTRA_STREAM, uri);
						intent.putExtra(Intent.EXTRA_SUBJECT, "Call Meter 3G export");
						if (!TextUtils.isEmpty(recipient)) {
							intent.putExtra(Intent.EXTRA_EMAIL, new String[] { recipient });
						}
						intent.addCategory(Intent.CATEGORY_DEFAULT);

						try {
							final File d = Environment.getExternalStorageDirectory();
							final File f = new File(d, DataProvider.PACKAGE + File.separator + fn);
							f.mkdirs();
							if (f.exists()) {
								f.delete();
							}
							f.createNewFile();
							FileWriter fw = new FileWriter(f);
							fw.append(result);
							fw.close();
							final String t = Preferences.this.getString(R.string.exported_) + " "
									+ f.getAbsolutePath();
							Toast.makeText(Preferences.this, t, Toast.LENGTH_LONG).show();
							// call an exporting app with the uri to the
							// preferences
							Preferences.this.startActivity(Intent.createChooser(intent,
									Preferences.this.getString(resChooser)));
						} catch (IOException e) {
							Log.e(TAG, "error writing export file", e);
							Toast.makeText(Preferences.this, R.string.err_export_write,
									Toast.LENGTH_LONG).show();
						}
					}
				}
			};
			task.execute((Void) null);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Utils.setLocale(this);
		this.setTitle(R.string.settings);
		this.addPreferencesFromResource(R.xml.prefs);

		Preference p = this.findPreference(PREFS_ADVANCED);
		if (p != null) {
			p.setOnPreferenceChangeListener(this);
		}
		p = this.findPreference(PREFS_PREPAID);
		if (p != null) {
			p.setOnPreferenceChangeListener(this);
		}

		Market.setOnPreferenceClickListener(this, this.findPreference("more_apps"), null,
				Market.SEARCH_APPS, Market.ALT_APPS);
		p = this.findPreference("send_logs");
		if (p != null) {
			p.setOnPreferenceClickListener(this);
		}
		p = this.findPreference("send_devices");
		if (p != null) {
			p.setOnPreferenceClickListener(this);
		}
		p = this.findPreference("reset_data");
		if (p != null) {
			p.setOnPreferenceClickListener(this);
		}
		p = this.findPreference("export_rules");
		if (p != null) {
			p.setOnPreferenceClickListener(this);
		}
		p = this.findPreference("export_rules_dev");
		if (p != null) {
			p.setOnPreferenceClickListener(this);
		}
		p = this.findPreference("export_logs");
		if (p != null) {
			p.setOnPreferenceClickListener(this);
		}
		p = this.findPreference("export_numgroups");
		if (p != null) {
			p.setOnPreferenceClickListener(this);
		}
		p = this.findPreference("export_hourgroups");
		if (p != null) {
			p.setOnPreferenceClickListener(this);
		}
		p = this.findPreference("import_rules");
		if (p != null) {
			p.setOnPreferenceClickListener(this);
		}
		p = this.findPreference("import_rules_default");
		if (p != null) {
			p.setOnPreferenceClickListener(this);
		}

		this.onNewIntent(this.getIntent());
	}

	/** Load widget list. */
	private void loadWidgets() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		Preference p = this.findPreference("widgets");
		if (p != null && p instanceof PreferenceScreen) {
			PreferenceScreen ps = (PreferenceScreen) p;
			ps.removeAll();
			int[] ids = AppWidgetManager.getInstance(this).getAppWidgetIds(
					new ComponentName(this, StatsAppWidgetProvider.class));
			boolean added = false;
			if (ids != null && ids.length > 0) {
				for (int id : ids) {
					if (prefs.getLong(StatsAppWidgetProvider.WIDGET_PLANID + id, -1) <= 0) {
						Log.w(TAG, "skip widget: " + id);
						continue;
					}
					added = true;
					p = new Preference(this);
					p.setTitle(this.getString(R.string.widget_) + " #" + id);
					final int fid = id;
					p.setOnPreferenceClickListener(new OnPreferenceClickListener() {
						@Override
						public boolean onPreferenceClick(final Preference preference) {
							Intent i = new Intent(Preferences.this, StatsAppWidgetConfigure.class);
							i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, fid);
							Preferences.this.startActivity(i);
							return true;
						}
					});
					ps.addPreference(p);
				}
			}
			if (!added) {
				p = new Preference(this);
				p.setTitle(R.string.widgets_no_widgets_);
				p.setSummary(R.string.widgets_no_widgets_hint);
				ps.addPreference(p);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onResume() {
		super.onResume();
		final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(this);
		this.checkSimplePrefs(p.getBoolean(PREFS_PREPAID, false));
		this.loadWidgets();
	}

	/**
	 * Check availability of "simple preferences".
	 * 
	 * @param overrideNo
	 *            override decision, true will disable "simple preferences"
	 */
	private void checkSimplePrefs(final boolean overrideNo) {
		final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(this);
		boolean enabled = !overrideNo && p.getBoolean(PREFS_ISDEFAULT, false);
		Preference pr = this.findPreference("simple_settings");
		pr.setEnabled(enabled);
		if (enabled) {
			pr.setSummary(R.string.simple_preferences_hint);
		} else {
			pr.setSummary(R.string.simple_preferences_deactivated);
		}

	}

	/**
	 * Get a {@link InputStream} from {@link Uri}.
	 * 
	 * @param cr
	 *            {@link ContentResolver}
	 * @param uri
	 *            {@link Uri}
	 * @return {@link InputStream}
	 */
	private InputStream getStream(final ContentResolver cr, final Uri uri) {
		if (uri.toString().equals("content://default")) {
			return IS_DEFAULT;
		} else if (uri.toString().startsWith("import")) {
			String url;
			if (uri.getScheme().equals("imports")) {
				url = "https:/";
			} else {
				url = "http:/";
			}
			url += uri.getPath();
			final HttpGet request = new HttpGet(url);
			Log.d(TAG, "url: " + url);
			try {
				final HttpResponse response = new DefaultHttpClient().execute(request);
				int resp = response.getStatusLine().getStatusCode();
				if (resp != HttpStatus.SC_OK) {
					return null;
				}
				return response.getEntity().getContent();
			} catch (IOException e) {
				Log.e(TAG, "error in reading export: " + url, e);
				return null;
			}
		} else if (uri.toString().startsWith("content://") || uri.toString().startsWith("file://")) {
			try {
				return cr.openInputStream(uri);
			} catch (IOException e) {
				Log.e(TAG, "error in reading export: " + uri.toString(), e);
				return null;
			}
		}
		Log.d(TAG, "getStream() returns null, " + uri.toString());
		return null;
	}

	/**
	 * Import data previously exported.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param uri
	 *            {@link Uri}
	 */
	private void importData(final Context context, final Uri uri) {
		Log.d(TAG, "importData(ctx, " + uri + ")");
		final ProgressDialog d1 = new ProgressDialog(this);
		d1.setCancelable(true);
		d1.setMessage(this.getString(R.string.import_progr));
		d1.setIndeterminate(true);
		d1.show();

		new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(final Void... params) {
				StringBuilder sb = new StringBuilder();
				try {
					final InputStream is = Preferences.this.getStream(
							Preferences.this.getContentResolver(), uri);
					if (is != IS_DEFAULT) {
						final BufferedReader bufferedReader = new BufferedReader(
								new InputStreamReader(is), BUFSIZE);
						String line = bufferedReader.readLine();
						while (line != null) {
							sb.append(line);
							sb.append("\n");
							line = bufferedReader.readLine();
						}
					} else {
						sb.append("DEFAULT");
					}
				} catch (Exception e) {
					Log.e(TAG, "error in reading export: " + e.toString(), e);
					return null;
				}
				return sb.toString();
			}

			@Override
			protected void onPostExecute(final String result) {
				Log.d(TAG, "import:\n" + result);
				d1.dismiss();
				if (result == null || result.length() == 0) {
					Toast.makeText(Preferences.this, R.string.err_export_read, Toast.LENGTH_LONG)
							.show();
					return;
				}
				Builder builder = new Builder(Preferences.this);
				if (result.equals("DEFAULT")) {
					builder.setMessage(R.string.import_rules_default_hint);
				} else {
					String[] lines = result.split("\n");
					if (lines.length <= 2) {
						Toast.makeText(Preferences.this, R.string.err_export_read,
								Toast.LENGTH_LONG).show();
						return;
					}
					builder.setMessage(Preferences.this.getString(R.string.import_rules_hint)
							+ "\n" + URLDecoder.decode(lines[1]));
				}
				builder.setCancelable(true);
				builder.setTitle(R.string.import_rules_);
				builder.setNegativeButton(android.R.string.cancel, null);
				builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog, final int which) {
						d1.setCancelable(false);
						d1.setIndeterminate(true);
						d1.show();
						new AsyncTask<Void, Void, Void>() {
							@Override
							protected Void doInBackground(final Void... params) {
								DataProvider.importData(Preferences.this, result);
								return null;
							}

							@Override
							protected void onPostExecute(final Void result) {
								d1.dismiss();
							}
						}.execute((Void) null);
					}
				});
				builder.show();
			}
		}.execute((Void) null);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onNewIntent(final Intent intent) {
		final Uri uri = intent.getData();
		Log.d(TAG, "new intent: " + intent.getAction());
		Log.d(TAG, "intent: " + intent.getData());
		if (uri != null) {
			Log.d(TAG, "importing: " + uri.toString());
			this.importData(this, uri);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onPreferenceChange(final Preference preference, final Object newValue) {
		final String k = preference.getKey();
		if (k.equals(PREFS_ADVANCED)) {
			if (newValue.equals(true)) {
				Preferences.this.startActivity(new Intent(Preferences.this, HelpActivity.class));
			}
			return true;
		} else if (k.equals(PREFS_PREPAID)) {
			this.checkSimplePrefs((Boolean) newValue);
			return true;
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onPreferenceClick(final Preference preference) {
		final String k = preference.getKey();
		if (k.equals("send_logs")) {
			Log.collectAndSendLog(Preferences.this);
			return true;
		} else if (k.equals("send_devices")) {
			final Intent intent = new Intent(Intent.ACTION_SEND);
			intent.setType("text/plain");
			intent.putExtra(Intent.EXTRA_EMAIL, new String[] { "android+callmeter@ub0r.de", "" });
			intent.putExtra(Intent.EXTRA_TEXT, Device.debugDeviceList(this));
			intent.putExtra(Intent.EXTRA_SUBJECT, "Call Meter 3G: Device List");
			try {
				Preferences.this.startActivity(intent);
			} catch (ActivityNotFoundException e) {
				Log.e(TAG, "no mail", e);
				Toast.makeText(Preferences.this, "no mail app found", Toast.LENGTH_LONG).show();
			}
			return true;
		} else if (k.equals("reset_data")) {
			this.resetDataDialog();
			return true;
		} else if (k.equals("export_rules")) {
			Preferences.this.exportData(null, DataProvider.EXPORT_RULESET_FILE, null);
			return true;
		} else if (k.equals("export_rules_dev")) {
			Preferences.this.exportData(null, DataProvider.EXPORT_RULESET_FILE,
					"android+callmeter@ub0r.de");
			return true;
		} else if (k.equals("export_logs")) {
			Preferences.this.exportData(null, DataProvider.EXPORT_LOGS_FILE, null);
			return true;
		} else if (k.equals("export_numgroups")) {
			Preferences.this.exportData(null, DataProvider.EXPORT_NUMGROUPS_FILE, null);
			return true;
		} else if (k.equals("export_hourgroups")) {
			Preferences.this.exportData(null, DataProvider.EXPORT_HOURGROUPS_FILE, null);
			return true;
		} else if (k.equals("import_rules")) {
			Preferences.this.startActivity(new Intent(Intent.ACTION_VIEW, Uri
					.parse(Preferences.this.getString(R.string.url_rulesets))));
			return true;
		} else if (k.equals("import_rules_default")) {
			final Intent i = new Intent(Preferences.this, Preferences.class);
			i.setData(Uri.parse("content://default"));
			Preferences.this.startActivity(i);
			return true;
		}
		return false;
	}
}
