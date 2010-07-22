/*
 * Copyright (C) 2009-2010 Felix Bechstein, The Android Open Source Project
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

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.widget.EditText;
import android.widget.Toast;
import de.ub0r.android.callmeter.R;
import de.ub0r.android.callmeter.data.DataProvider;
import de.ub0r.android.lib.Log;

/**
 * Preferences.
 * 
 * @author flx
 */
public class Preferences extends PreferenceActivity {
	/** Tag for output. */
	private static final String TAG = "prefs";

	/** Standard buffer size. */
	public static final int BUFSIZE = 1024;

	/** Preference's name: theme. */
	private static final String PREFS_THEME = "theme";
	/** Theme: black. */
	private static final String THEME_BLACK = "black";
	/** Theme: light. */
	private static final String THEME_LIGHT = "light";
	/** Preference's name: textsize. */
	private static final String PREFS_TEXTSIZE = "textsize";
	/** Textsize: small. */
	private static final String TEXTSIZE_SMALL = "small";
	/** Textsize: medium. */
	private static final String TEXTSIZE_MEDIUM = "medium";
	/** Textsize: small. */
	public static float textSizeSmall = 14;
	/** Textsize: medium. */
	public static float textSizeMedium = 18;

	/**
	 * Get Theme from Preferences.
	 * 
	 * @param context
	 *            {@link Context}
	 * @return theme
	 */
	public static final int getTheme(final Context context) {
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);
		final String s = p.getString(PREFS_THEME, THEME_BLACK);
		if (s != null && THEME_LIGHT.equals(s)) {
			return android.R.style.Theme_Light;
		}
		return android.R.style.Theme_Black;
	}

	/**
	 * Get Textsize from Preferences.
	 * 
	 * @param context
	 *            {@link Context}
	 * @return text size
	 */
	static final float getTextsize(final Context context) {
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);
		final String s = p.getString(PREFS_TEXTSIZE, TEXTSIZE_SMALL);
		if (s != null && TEXTSIZE_MEDIUM.equals(s)) {
			return textSizeMedium;
		}
		return textSizeSmall;
	}

	/**
	 * Delete data from logs.
	 * 
	 * @param type
	 *            type to delete; -1 for all
	 */
	private void resetData(final int type) {
		if (type < 0) {
			this.getContentResolver().delete(DataProvider.Logs.CONTENT_URI,
					null, null);
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
		builder.setNeutralButton(R.string.reset_data_data_,
				new OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog,
							final int which) {
						Preferences.this.resetData(-1);
					}
				});
		builder.setNegativeButton(android.R.string.no, null);
		builder.show();
	}

	/**
	 * Export Rules.
	 * 
	 * @param descr
	 *            description of the exported rule set
	 */
	private void exportRules(final String descr) {
		if (descr == null) {
			final EditText et = new EditText(this);
			Builder builder = new Builder(this);
			builder.setView(et);
			builder.setCancelable(true);
			builder.setTitle(R.string.export_rules_descr);
			builder.setNegativeButton(android.R.string.cancel, null);
			builder.setPositiveButton(android.R.string.ok,
					new OnClickListener() {
						@Override
						public void onClick(final DialogInterface dialog,
								final int which) {
							Preferences.this.exportRules(et.getText()
									.toString());
						}
					});
			builder.show();
		} else {
			final ProgressDialog d = new ProgressDialog(this);
			d.setIndeterminate(true);
			d.setMessage(this.getString(R.string.export_rules_progr));
			d.setCancelable(false);
			d.show();

			// run task in background
			final AsyncTask<Void, Void, String> task = // .
			new AsyncTask<Void, Void, String>() {
				@Override
				protected String doInBackground(final Void... params) {
					final String ret = DataProvider.backupRuleSet(
							Preferences.this, descr);
					return ret;
				}

				@Override
				protected void onPostExecute(final String result) {
					Log.d(TAG, "export:\n" + result);
					System.out.println("\n" + result);
					d.dismiss();
					if (result != null && result.length() > 0) {
						final Intent intent = new Intent(Intent.ACTION_SEND);
						intent.setType(DataProvider.EXPORT_MIMETYPE);
						intent.putExtra(Intent.EXTRA_STREAM,
								DataProvider.EXPORT_URI);
						intent.putExtra(Intent.EXTRA_SUBJECT, // .
								"Call Meter 3G export");
						intent.addCategory(Intent.CATEGORY_DEFAULT);

						try {
							final File d = Environment
									.getExternalStorageDirectory();
							final File f = new File(d, DataProvider.PACKAGE
									+ File.separator + // .
									DataProvider.EXPORT_FILE);
							f.mkdirs();
							if (f.exists()) {
								f.delete();
							}
							f.createNewFile();
							FileWriter fw = new FileWriter(f);
							fw.append(result);
							fw.close();
							// call an exporting app with the uri to the
							// preferences
							Preferences.this.startActivity(Intent
									.createChooser(intent, Preferences.this
											.getString(// .
											R.string.export_rules_)));
						} catch (IOException e) {
							Log.e(TAG, "error writing export file", e);
							Toast.makeText(Preferences.this,
									R.string.err_export_write,
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
	public final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setTitle(R.string.settings);
		this.addPreferencesFromResource(R.xml.prefs);
		// final SharedPreferences prefs = PreferenceManager
		// .getDefaultSharedPreferences(this);

		Preference p = this.findPreference("send_logs");
		if (p != null) {
			p.setOnPreferenceClickListener(// .
					new Preference.OnPreferenceClickListener() {
						public boolean onPreferenceClick(
								final Preference preference) {
							Log.collectAndSendLog(Preferences.this);
							return true;
						}
					});
		}
		p = this.findPreference("reset_data");
		if (p != null) {
			p.setOnPreferenceClickListener(// .
					new Preference.OnPreferenceClickListener() {
						public boolean onPreferenceClick(
								final Preference preference) {
							Preferences.this.resetDataDialog();
							return true;
						}
					});
		}
		p = this.findPreference("export_rules");
		if (p != null) {
			p.setOnPreferenceClickListener(// .
					new Preference.OnPreferenceClickListener() {
						public boolean onPreferenceClick(
								final Preference preference) {
							Preferences.this.exportRules(null);
							return true;
						}
					});
		}
		p = this.findPreference("import_rules");
		if (p != null) {
			p.setOnPreferenceClickListener(// .
					new Preference.OnPreferenceClickListener() {
						public boolean onPreferenceClick(
								final Preference preference) {
							Preferences.this.startActivity(new Intent(
									Intent.ACTION_VIEW, Uri
											.parse(Preferences.this.getString(// .
													R.string.url_rulesets))));
							return true;
						}
					});
		}

		this.onNewIntent(this.getIntent());
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
		if (uri.toString().startsWith("import")) {
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
				final HttpResponse response = new DefaultHttpClient()
						.execute(request);
				int resp = response.getStatusLine().getStatusCode();
				if (resp != HttpStatus.SC_OK) {
					return null;
				}
				return response.getEntity().getContent();
			} catch (IOException e) {
				Log.e(TAG, "error in reading export: " + url, e);
				return null;
			}
		} else if (uri.toString().startsWith("content://")) {
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
	 * Import a given rule set.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param uri
	 *            {@link Uri}
	 */
	private void importRuleSet(final Context context, final Uri uri) {
		final ProgressDialog d1 = new ProgressDialog(this);
		d1.setCancelable(true);
		d1.setMessage(this.getString(R.string.import_rules_progr));
		d1.setIndeterminate(true);
		d1.show();

		new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(final Void... params) {
				StringBuilder sb = new StringBuilder();
				try {
					final BufferedReader bufferedReader = // .
					new BufferedReader(new InputStreamReader(// .
							Preferences.this.getStream(Preferences.this
									.getContentResolver(), uri)), BUFSIZE);
					String line = bufferedReader.readLine();
					while (line != null) {
						sb.append(line);
						sb.append("\n");
						line = bufferedReader.readLine();
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
					Toast.makeText(Preferences.this, R.string.err_export_read,
							Toast.LENGTH_LONG).show();
					return;
				}
				String[] lines = result.split("\n");
				if (lines.length <= 2) {
					Toast.makeText(Preferences.this, R.string.err_export_read,
							Toast.LENGTH_LONG).show();
					return;
				}
				Builder builder = new Builder(Preferences.this);
				builder.setCancelable(true);
				builder.setTitle(R.string.import_rules_);
				builder.setMessage(Preferences.this
						.getString(R.string.import_rules_hint)
						+ "\n" + URLDecoder.decode(lines[1]));
				builder.setNegativeButton(android.R.string.cancel, null);
				builder.setPositiveButton(android.R.string.ok,
						new OnClickListener() {
							@Override
							public void onClick(final DialogInterface dialog,
									final int which) {
								d1.setCancelable(false);
								d1.setIndeterminate(true);
								d1.show();
								new AsyncTask<Void, Void, Void>() {

									@Override
									protected Void doInBackground(
											final Void... params) {
										DataProvider.importRuleSet(
												Preferences.this, result);
										return null;
									}

									@Override
									protected void onPostExecute(
											final Void result) {
										d1.dismiss();
									}
								} // .
										.execute((Void) null);
							}
						});
				builder.show();
			}
		} // .
				.execute((Void) null);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void onNewIntent(final Intent intent) {
		final Uri uri = intent.getData();
		Log.d(TAG, "new intent: " + intent.getAction());
		Log.d(TAG, "intent: " + intent.getData());
		if (uri != null) {
			Log.d(TAG, "importing: " + uri.toString());
			this.importRuleSet(this, uri);
		}
	}
}
