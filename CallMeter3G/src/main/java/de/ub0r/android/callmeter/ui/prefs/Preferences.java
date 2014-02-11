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
package de.ub0r.android.callmeter.ui.prefs;

import org.apache.http.HttpStatus;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Currency;
import java.util.Locale;

import de.ub0r.android.callmeter.CallMeter;
import de.ub0r.android.callmeter.R;
import de.ub0r.android.callmeter.TrackingUtils;
import de.ub0r.android.callmeter.data.DataProvider;
import de.ub0r.android.callmeter.data.DataProvider.XmlMetaData;
import de.ub0r.android.callmeter.data.Device;
import de.ub0r.android.callmeter.data.ExportProvider;
import de.ub0r.android.callmeter.data.LogRunnerService;
import de.ub0r.android.callmeter.data.RuleMatcher;
import de.ub0r.android.callmeter.ui.Common;
import de.ub0r.android.callmeter.ui.TrackingSherlockPreferenceActivity;
import de.ub0r.android.lib.Market;
import de.ub0r.android.lib.Utils;
import de.ub0r.android.logg0r.Log;
import de.ub0r.android.logg0r.LogCollector;

/**
 * Preferences.
 *
 * @author flx
 */
public final class Preferences extends TrackingSherlockPreferenceActivity implements
        OnPreferenceClickListener {

    /** Tag for output. */
    private static final String TAG = "Preferences";

    /** Standard buffer size. */
    public static final int BUFSIZE = 1024;

    /** Action for exporting CSV file. */
    public static final String ACTION_EXPORT_CSV = "export_csv";

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
    /** Preference's name: show last bill day instead of first. */
    public static final String PREFS_SHOW_TARGET_BILLDAY = "show_target_billday";
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
     * @param p {@link SharedPreferences}
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
     * @param context {@link Context}
     * @return theme
     */
    public static int getTheme(final Context context) {
        final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        final String s = p.getString(PREFS_THEME, THEME_LIGHT);
        if (s != null && THEME_BLACK.equals(s)) {
            TrackingUtils.setDimension(context, 1, "dark");
            return R.style.Theme_SherlockCallMeter;
        } else {
            TrackingUtils.setDimension(context, 1, "light");
            return R.style.Theme_SherlockCallMeter_Light;
        }
    }

    /**
     * Get Text size from Preferences.
     *
     * @param context {@link Context}
     * @return text size
     */
    public static int getTextsize(final Context context) {
        final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        final String s = p.getString(PREFS_TEXTSIZE, null);
        int size = Utils.parseInt(s, 0);
        TrackingUtils.setDimension(context, 3, size);
        return size;
    }

    /**
     * Get Text size for big titles from Preferences.
     *
     * @param context {@link Context}
     * @return text size
     */
    public static int getTextsizeBigTitle(final Context context) {
        final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        final String s = p.getString(PREFS_TEXTSIZE_BIGTITLE, null);
        int size = Utils.parseInt(s, 0);
        TrackingUtils.setDimension(context, 4, size);
        return size;
    }

    /**
     * Get Text size for titles from Preferences.
     *
     * @param context {@link Context}
     * @return text size
     */
    public static int getTextsizeTitle(final Context context) {
        final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        final String s = p.getString(PREFS_TEXTSIZE_TITLE, null);
        int size = Utils.parseInt(s, 0);
        TrackingUtils.setDimension(context, 5, size);
        return size;
    }

    /**
     * Get Text size for spacers from Preferences.
     *
     * @param context {@link Context}
     * @return text size
     */
    public static int getTextsizeSpacer(final Context context) {
        final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        final String s = p.getString(PREFS_TEXTSIZE_SPACER, null);
        int size = Utils.parseInt(s, 0);
        TrackingUtils.setDimension(context, 6, size);
        return size;
    }

    /**
     * Get Text size for progress bars from Preferences.
     *
     * @param context {@link Context}
     * @return text size
     */
    public static int getTextsizeProgressBar(final Context context) {
        final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        final String s = p.getString(PREFS_TEXTSIZE_PBAR, null);
        int size = Utils.parseInt(s, 0);
        TrackingUtils.setDimension(context, 7, size);
        return size;
    }

    /**
     * Get Text size for progress bars of billing periods from Preferences.
     *
     * @param context {@link Context}
     * @return text size
     */
    public static int getTextsizeProgressBarBP(final Context context) {
        final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        final String s = p.getString(PREFS_TEXTSIZE_PBARBP, null);
        int size = Utils.parseInt(s, 0);
        TrackingUtils.setDimension(context, 8, size);
        return size;
    }

    /**
     * Get the currency symbol from {@link SharedPreferences}.
     *
     * @param context {@link Context}
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
     * @param context {@link Context}
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
            Log.d(TAG, "custom currency format: ", pcs);
            String c = getCurrencySymbol(context);
            Log.d(TAG, "custom currency symbol: ", c);
            if (c.equals("$")) {
                c = "\\$";
                Log.d(TAG, "custom currency symbol: ", c);
            } else if (c.equals("%")) {
                c = "%%";
                Log.d(TAG, "custom currency symbol: ", c);
            }
            String ret = "$%.2f";
            try {
                ret = pcs.replaceAll("\\$", c).replaceAll("\u20AC", c).replaceAll("\u0440", c);
            } catch (ArrayIndexOutOfBoundsException e) {
                Log.e(TAG, "could not parse currency format", e);
            }
            Log.d(TAG, "custom currency format: ", ret);
            return ret;
        }
    }

    /**
     * Get the date format from {@link SharedPreferences}.
     *
     * @param context {@link Context}
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
     * @param context   {@link Context}
     * @param isDefault true, if default rule set is loaded
     */
    public static void setDefaultPlan(final Context context, final boolean isDefault) {
        final Editor e = PreferenceManager.getDefaultSharedPreferences(context).edit();
        e.putBoolean(PREFS_ISDEFAULT, isDefault);
        e.commit();
    }

    /**
     * Delete data from logs.
     *
     * @param type type to delete; -1 for all
     */
    private void resetData(final int type) {
        if (type < 0 || type == DataProvider.TYPE_CALL) {
            Editor e = PreferenceManager.getDefaultSharedPreferences(this).edit();
            LogRunnerService.setLastData(e, DataProvider.TYPE_CALL, 0, 0L);
            e.commit();
        }
        if (type < 0) {
            getContentResolver().delete(DataProvider.Logs.CONTENT_URI, null, null);
        } else {
            getContentResolver().delete(DataProvider.Logs.CONTENT_URI,
                    DataProvider.Logs.TYPE + " = " + type, null);
        }
        RuleMatcher.resetAlert(this);
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
                TrackingUtils.sendClick(this, "reset_data#yes", null);
                Preferences.this.resetData(DataProvider.TYPE_CALL);
                Preferences.this.resetData(DataProvider.TYPE_SMS);
                Preferences.this.resetData(DataProvider.TYPE_MMS);
            }
        });
        builder.setNeutralButton(R.string.reset_data_data_, new OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, final int which) {
                TrackingUtils.sendClick(this, "reset_data#yes+data", null);
                Preferences.this.resetData(-1);
            }
        });
        builder.setNegativeButton(android.R.string.no, null);
        builder.show();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.setLocale(this);
        setTitle(R.string.settings);
        addPreferencesFromResource(R.xml.prefs);

        Market.setOnPreferenceClickListener(this, findPreference("more_apps"), null,
                Market.SEARCH_APPS, Market.ALT_APPS);
        Preference p = findPreference("send_logs");
        if (p != null) {
            p.setOnPreferenceClickListener(this);
        }
        p = findPreference("send_devices");
        if (p != null) {
            p.setOnPreferenceClickListener(this);
        }
        p = findPreference("reset_data");
        if (p != null) {
            p.setOnPreferenceClickListener(this);
        }
        onNewIntent(getIntent());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onResume() {
        super.onResume();
        checkSimplePrefs();
    }

    /**
     * Check availability of "simple preferences".
     */
    @SuppressWarnings("deprecation")
    private void checkSimplePrefs() {
        boolean overrideNo = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
                PREFS_PREPAID, false);
        final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(this);
        boolean enabled = !overrideNo && p.getBoolean(PREFS_ISDEFAULT, false);
        Preference pr = findPreference("simple_settings");
        assert pr != null;
        if (enabled) {
            TrackingUtils.setDimension(this, 2, "1");
            pr.setOnPreferenceClickListener(null);
            pr.setSummary(R.string.simple_preferences_hint);
        } else {
            TrackingUtils.setDimension(this, 2, "0");
            pr.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(final Preference preference) {
                    Toast.makeText(Preferences.this, R.string.simple_preferences_deactivated,
                            Toast.LENGTH_LONG).show();
                    Preferences.this.startActivity(new Intent("IMPORT", null, Preferences.this,
                            PreferencesPlain.class));
                    return true;
                }
            });
            pr.setSummary(R.string.simple_preferences_deactivated);
        }
    }

    /**
     * Get a {@link InputStream} from {@link Uri}.
     *
     * @param cr  {@link ContentResolver}
     * @param uri {@link Uri}
     * @return {@link InputStream}
     */
    private InputStream getStream(final ContentResolver cr, final Uri uri) throws IOException {
        String scheme = uri.getScheme();
        assert scheme != null;
        if (uri.toString().equals("content://default")) {
            return IS_DEFAULT;
        } else if (scheme.equals("content") || scheme.equals("file")) {
            try {
                return cr.openInputStream(uri);
            } catch (IOException e) {
                Log.e(TAG, "error in reading export: " + uri.toString(), e);
                return null;
            }
        } else if (scheme.equals("http") || scheme.equals("https")) {
            String url = uri.toString();
            HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
            if (con.getResponseCode() != HttpStatus.SC_OK) {
                Log.e(TAG,
                        "error in reading export: " + url + "Response: " + con
                                .getResponseMessage());
                return null;
            }
            return new BufferedInputStream(con.getInputStream());
        } else {
            throw new IllegalArgumentException("invalid Uri: " + uri);
        }
    }

    /**
     * Import data previously exported.
     *
     * @param context {@link Context}
     * @param uri     {@link Uri}
     */
    private void importData(final Context context, final Uri uri) {
        Log.d(TAG, "importData(ctx, ", uri, ")");
        final ProgressDialog d1 = new ProgressDialog(this);
        d1.setCancelable(true);
        d1.setMessage(getString(R.string.import_progr));
        d1.setIndeterminate(true);
        d1.show();

        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(final Void... params) {
                StringBuilder sb = new StringBuilder();
                try {
                    InputStream is = Preferences.this.getStream(
                            Preferences.this.getContentResolver(), uri);
                    if (is != IS_DEFAULT) {
                        final BufferedReader r = new BufferedReader(new InputStreamReader(is),
                                BUFSIZE);
                        String line = r.readLine();
                        while (line != null) {
                            // Log.d(TAG, "read new line: ", line);
                            sb.append(line);
                            sb.append("\n");
                            line = r.readLine();
                        }
                        is.close();
                        r.close();
                    } else {
                        sb.append("DEFAULT");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "error in reading export: " + uri, e);
                    return null;
                }
                return sb.toString();
            }

            @SuppressWarnings("deprecation")
            @Override
            protected void onPostExecute(final String result) {
                Log.d(TAG, "import:\n", result);
                try {
                    d1.dismiss();
                } catch (Exception e) { // ignore any exception
                    Log.e(TAG, "cannot dismiss dialog", e);
                }
                if (result == null || result.length() == 0) {
                    Toast.makeText(Preferences.this, R.string.err_export_read, Toast.LENGTH_LONG)
                            .show();
                    return;
                }
                Builder builder = new Builder(Preferences.this);
                if (result.equals("DEFAULT")) {
                    builder.setMessage(R.string.import_rules_default_hint);
                } else if (result.startsWith("<")) {
                    XmlMetaData m = DataProvider.parseXml(context, result);
                    if (m == null) {
                        Toast.makeText(Preferences.this, R.string.err_export_read,
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    StringBuilder sb = new StringBuilder();
                    sb.append(Preferences.this.getString(R.string.import_rules_hint));
                    sb.append("\n");
                    if (m.country != null) {
                        sb.append(context.getString(R.string.country));
                        sb.append(": ");
                        sb.append(m.country);
                        sb.append("\n");
                    }
                    if (m.provider != null) {
                        sb.append(context.getString(R.string.provider));
                        sb.append(": ");
                        sb.append(m.provider);
                        sb.append("\n");
                    }
                    if (m.title != null) {
                        sb.append(context.getString(R.string.plan));
                        sb.append(": ");
                        sb.append(m.title);
                        sb.append("\n");
                    }
                    builder.setMessage(sb.toString().trim());
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
                        new AsyncTask<Void, Void, Boolean>() {
                            @Override
                            protected Boolean doInBackground(final Void... params) {
                                return DataProvider.importData(Preferences.this, result);
                            }

                            @Override
                            protected void onPostExecute(final Boolean result) {
                                if (!result) {
                                    try {
                                        Toast.makeText(context, R.string.err_export_read,
                                                Toast.LENGTH_LONG).show();
                                    } catch (Exception e) {
                                        Log.w(TAG, "activity already finished?", e);
                                    }
                                }
                                d1.dismiss();
                                Preferences.this.checkSimplePrefs();
                            }
                        }.execute((Void) null);
                    }
                });
                builder.show();
            }
        }.execute((Void) null);
    }

    /**
     * Export data.
     *
     * @param context   {@link Context}
     * @param country   country
     * @param provider  provider
     * @param descr     description of the exported rule set
     * @param fn        one of the predefined file names from {@link DataProvider}.
     * @param recipient recipient of export
     */
    static void exportData(final Context context, final String country, final String provider,
            final String descr, final String fn, final String recipient) {
        if (descr == null
                || (recipient != null && !"sdcard".equals(recipient)
                && fn.equals(ExportProvider.EXPORT_RULESET_FILE) && (TextUtils
                .isEmpty(country) || TextUtils.isEmpty(provider) || TextUtils
                .isEmpty(descr)))) {
            Builder builder = new Builder(context);
            EditText et0, et1, et2;
            if (fn.equals(ExportProvider.EXPORT_RULESET_FILE)) {
                View v = LayoutInflater.from(context).inflate(R.layout.dialog_export, null);
                assert v != null;
                builder.setView(v);
                et0 = (EditText) v.findViewById(R.id.country);
                if (!TextUtils.isEmpty(country)) {
                    et0.setText(country);
                }
                et1 = (EditText) v.findViewById(R.id.provider);
                et1.setText(provider);
                et2 = (EditText) v.findViewById(R.id.plan);
                et2.setText(descr);
            } else {
                et0 = null;
                et1 = null;
                et2 = new EditText(context);
                builder.setView(et2);
            }
            final EditText etCountry = et0;
            final EditText etProvider = et1;
            final EditText etPlan = et2;
            builder.setCancelable(true);
            builder.setTitle(R.string.export_rules_descr);
            builder.setNegativeButton(android.R.string.cancel, null);
            builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
                @Override
                public void onClick(final DialogInterface dialog, final int which) {
                    String s0, s1, s2;
                    //noinspection ConstantConditions
                    s0 = etCountry == null ? null : etCountry.getText().toString().trim();
                    //noinspection ConstantConditions
                    s1 = etProvider == null ? null : etProvider.getText().toString().trim();
                    //noinspection ConstantConditions
                    s2 = etPlan == null ? null : etPlan.getText().toString().trim();
                    exportData(context, s0, s1, s2, fn, recipient);
                }
            });
            builder.show();
        } else {
            final ProgressDialog d = new ProgressDialog(context);
            d.setIndeterminate(true);
            d.setMessage(context.getString(R.string.export_progr));
            d.setCancelable(false);
            d.show();

            // run task in background
            final AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {
                @Override
                protected String doInBackground(final Void... params) {
                    if (fn.equals(ExportProvider.EXPORT_RULESET_FILE)) {
                        return DataProvider.backupRuleSet(context, country, provider, descr);
                    } else if (fn.equals(ExportProvider.EXPORT_LOGS_FILE)) {
                        return DataProvider.backupLogs(context, descr);
                    } else if (fn.equals(ExportProvider.EXPORT_NUMGROUPS_FILE)) {
                        return DataProvider.backupNumGroups(context, descr);
                    } else if (fn.equals(ExportProvider.EXPORT_HOURGROUPS_FILE)) {
                        return DataProvider.backupHourGroups(context, descr);
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(final String result) {
                    Log.d(TAG, "export:\n", result);
                    System.out.println("\n" + result);
                    d.dismiss();
                    if (result != null && result.length() > 0) {
                        Uri uri = null;
                        int resChooser = -1;
                        if (fn.equals(ExportProvider.EXPORT_RULESET_FILE)) {
                            uri = ExportProvider.EXPORT_RULESET_URI;
                            resChooser = R.string.export_rules_;
                        } else if (fn.equals(ExportProvider.EXPORT_LOGS_FILE)) {
                            uri = ExportProvider.EXPORT_LOGS_URI;
                            resChooser = R.string.export_logs_;
                        } else if (fn.equals(ExportProvider.EXPORT_NUMGROUPS_FILE)) {
                            uri = ExportProvider.EXPORT_NUMGROUPS_URI;
                            resChooser = R.string.export_numgroups_;
                        } else if (fn.equals(ExportProvider.EXPORT_HOURGROUPS_FILE)) {
                            uri = ExportProvider.EXPORT_HOURGROUPS_URI;
                            resChooser = R.string.export_hourgroups_;
                        }
                        Intent intent = null;
                        if (!"sdcard".equals(recipient)) {
                            intent = new Intent(Intent.ACTION_SEND);
                            intent.setType(ExportProvider.EXPORT_MIMETYPE);
                            intent.putExtra(Intent.EXTRA_STREAM, uri);
                            intent.putExtra(Intent.EXTRA_SUBJECT, "Call Meter 3G export");
                            if (!TextUtils.isEmpty(recipient)) {
                                intent.putExtra(Intent.EXTRA_EMAIL, new String[]{recipient});
                                intent.putExtra(Intent.EXTRA_TEXT, context.getString(
                                        R.string.export_rules_body, country, provider, descr));
                            }
                            intent.addCategory(Intent.CATEGORY_DEFAULT);
                        }

                        try {
                            final File d = new File(Environment.getExternalStorageDirectory(),
                                    DataProvider.PACKAGE);
                            final File f = new File(d, fn);
                            //noinspection ResultOfMethodCallIgnored
                            f.mkdirs();
                            if (f.exists()) {
                                //noinspection ResultOfMethodCallIgnored
                                f.delete();
                            }
                            //noinspection ResultOfMethodCallIgnored
                            f.createNewFile();
                            FileWriter fw = new FileWriter(f);
                            fw.append(result);
                            fw.close();
                            final String t = context.getString(R.string.exported_) + " "
                                    + f.getAbsolutePath();
                            Toast.makeText(context, t, Toast.LENGTH_LONG).show();
                            // call an exporting app with the uri to the
                            // preferences
                            if (intent != null) {
                                context.startActivity(Intent.createChooser(intent,
                                        context.getString(resChooser)));
                            }
                        } catch (IOException e) {
                            Log.e(TAG, "error writing export file", e);
                            Toast.makeText(context, R.string.err_export_write, Toast.LENGTH_LONG)
                                    .show();
                        }
                    }
                }
            };
            task.execute((Void) null);
        }
    }

    /**
     * Export logs as CSV file.
     *
     * @param context {@link Context}
     */
    static void exportLogsCsv(final Context context) {
        final ProgressDialog d = new ProgressDialog(context);
        d.setIndeterminate(true);
        d.setMessage(context.getString(R.string.export_progr));
        d.setCancelable(false);
        d.show();

        // run task in background
        final AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(final Void... params) {
                File d = new File(Environment.getExternalStorageDirectory(), DataProvider.PACKAGE);
                File f = new File(d, "logs-"
                        + DateFormat.format("yyyyMMddkkmmss", System.currentTimeMillis()) + ".csv");
                //noinspection ResultOfMethodCallIgnored
                f.mkdirs();
                if (f.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    f.delete();
                }
                try {
                    // build csv file and save it to sd card
                    FileWriter w = new FileWriter(f);
                    Cursor c = context.getContentResolver().query(
                            DataProvider.Logs.CONTENT_URI_JOIN, DataProvider.Logs.PROJECTION_JOIN,
                            null, null, null);
                    assert c != null;
                    w.append("date;type;direction;my_number/sim_id;roamed;remote_number;"
                            + "amount;billed_amount;cost;plan;rule\n");
                    String[] types = context.getResources().getStringArray(R.array.plans_type);
                    String[] directions = context.getResources().getStringArray(
                            R.array.direction_calls);
                    String cFormat = getCurrencyFormat(context);
                    if (c.moveToFirst()) {
                        do {
                            w.append(DateFormat.format("yyyyMMddkkmmss;",
                                    c.getLong(DataProvider.Logs.INDEX_DATE)));
                            int t = c.getInt(DataProvider.Logs.INDEX_TYPE);
                            w.append(types[t]).append(";");
                            int dir = c.getInt(DataProvider.Logs.INDEX_DIRECTION);
                            w.append(directions[dir]).append(";");
                            w.append(c.getString(DataProvider.Logs.INDEX_MYNUMBER)).append(";");
                            w.append(c.getString(DataProvider.Logs.INDEX_ROAMED)).append(";");
                            w.append(c.getString(DataProvider.Logs.INDEX_REMOTE)).append(";");
                            long a = c.getLong(DataProvider.Logs.INDEX_AMOUNT);
                            float ba = c.getFloat(DataProvider.Logs.INDEX_BILL_AMOUNT);
                            float cost = c.getFloat(DataProvider.Logs.INDEX_COST);
                            w.append(Common.formatAmount(t, a, true)).append(";");
                            w.append(Common.formatAmount(t, ba, true)).append(";");
                            w.append(String.format(cFormat, cost)).append(";");
                            w.append(c.getString(DataProvider.Logs.INDEX_PLAN_NAME)).append(";");
                            w.append(c.getString(DataProvider.Logs.INDEX_RULE_NAME)).append("\n");
                        } while (c.moveToNext());
                    }

                    c.close();
                    w.close();
                    // return file name
                    return f.getAbsolutePath();
                } catch (IOException e) {
                    Log.e(TAG, "error writing csv file", e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(final String result) {
                Log.d(TAG, "csv.task.onPostExecute(", result, ")");
                d.dismiss();
                if (TextUtils.isEmpty(result)) {
                    Log.e(TAG, "error writing export file: " + result);
                    Toast.makeText(context, R.string.err_export_write, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(context, context.getString(R.string.exported_) + " " + result,
                            Toast.LENGTH_LONG).show();
                }
            }
        };
        task.execute((Void) null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onNewIntent(final Intent intent) {
        final Uri uri = intent.getData();
        String a = intent.getAction();
        Log.d(TAG, "new intent: ", a);
        Log.d(TAG, "intent: ", uri);
        if (ACTION_EXPORT_CSV.equals(a)) {
            Log.d(TAG, "export csv");
            TrackingUtils.sendEvent(this, "data", "export", "csv", null);
            exportLogsCsv(this);
        } else if (uri != null) {
            Log.d(TAG, "importing: ", uri.toString());
            TrackingUtils.sendEvent(this, "data", "import", uri.toString(), null);
            importData(this, uri);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onPreferenceClick(final Preference preference) {
        final String k = preference.getKey();
        assert k != null;
        if (k.equals("send_logs")) {
            LogCollector.collectAndSendLogs(this, "android@ub0r.de",
                    getString(R.string.sendlog_install_),
                    getString(R.string.sendlog_install),
                    getString(R.string.sendlog_run_),
                    getString(R.string.sendlog_run));
            return true;
        } else if (k.equals("send_devices")) {
            final Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"android@ub0r.de", ""});
            intent.putExtra(Intent.EXTRA_TEXT, Device.debugDeviceList(this));
            intent.putExtra(Intent.EXTRA_SUBJECT, "Call Meter 3G: Device List");
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "no mail", e);
                Toast.makeText(this, "no mail app found", Toast.LENGTH_LONG).show();
            }
            return true;
        } else if (k.equals("reset_data")) {
            TrackingUtils.sendClick(this, "reset_data", null);
            resetDataDialog();
            return true;
        }
        return false;
    }
}
