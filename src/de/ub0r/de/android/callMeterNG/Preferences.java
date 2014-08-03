package de.ub0r.de.android.callMeterNG;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import de.ub0r.android.lib.Log;
import de.ub0r.android.lib.Market;

/**
 * Preferences.
 * 
 * @author flx
 */
@SuppressWarnings("deprecation")
public class Preferences extends PreferenceActivity implements
		SharedPreferences.OnSharedPreferenceChangeListener {
	/** {@link Preference}: merge sms into calls. */
	private Preference prefMergeSMStoCalls = null;
	/** {@link Preference}: merge sms into plan 1. */
	private Preference prefMergeToPlan1 = null;
	/** {@link Preference}: plan1 - cost per call. */
	private Preference prefPlan1CostPerCall = null;
	/** {@link Preference}: plan2 - cost per call. */
	private Preference prefPlan2CostPerCall = null;
	/** {@link Preferences}: excluded calls to plan 1. */
	private Preference prefExcludeCallsToPlan1 = null;
	/** {@link Preferences}: excluded calls to plan 2. */
	private Preference prefExcludeCallsToPlan2 = null;
	/** {@link Preferences}: excluded sms to plan 1. */
	private Preference prefExcludeSmsToPlan1 = null;
	/** {@link Preferences}: excluded sms to plan 2. */
	private Preference prefExcludeSmsToPlan2 = null;
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
	static final int getTheme(final Context context) {
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);
		final String s = p.getString(PREFS_THEME, THEME_BLACK);
		if (s != null && THEME_LIGHT.equals(s)) {
			return R.style.Theme_Light;
		}
		return R.style.Theme;
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
	 * {@inheritDoc}
	 */
	@Override
	public final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.addPreferencesFromResource(R.xml.prefs);
		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		prefs.registerOnSharedPreferenceChangeListener(this);
		this.prefMergeSMStoCalls = this
				.findPreference(Updater.PREFS_MERGE_SMS_TO_CALLS);
		this.prefMergeToPlan1 = this
				.findPreference(Updater.PREFS_MERGE_SMS_PLAN1);
		this.prefPlan1CostPerCall = this
				.findPreference(Updater.PREFS_PLAN1_COST_PER_CALL);
		this.prefPlan2CostPerCall = this
				.findPreference(Updater.PREFS_PLAN2_COST_PER_CALL);
		this.prefExcludeCallsToPlan1 = this
				.findPreference(ExcludePeople.PREFS_EXCLUDE_PEOPLE_CALLS_PLAN1);
		this.prefExcludeCallsToPlan2 = this
				.findPreference(ExcludePeople.PREFS_EXCLUDE_PEOPLE_CALLS_PLAN2);
		this.prefExcludeSmsToPlan1 = this
				.findPreference(ExcludePeople.PREFS_EXCLUDE_PEOPLE_SMS_PLAN1);
		this.prefExcludeSmsToPlan2 = this
				.findPreference(ExcludePeople.PREFS_EXCLUDE_PEOPLE_SMS_PLAN2);

		// run check on create!
		this.onSharedPreferenceChanged(prefs, Updater.PREFS_SPLIT_PLANS);
		this.onSharedPreferenceChanged(prefs, Updater.PREFS_PLAN1_FREEMIN);
		this.onSharedPreferenceChanged(prefs, Updater.PREFS_PLAN2_FREEMIN);
		Market.setOnPreferenceClickListener(this,
				this.findPreference("more_apps"), null, "Felix+Bechstein",
				"https://github.com/felixb");
		Preference p = this.findPreference("send_logs");
		if (p != null) {
			p.setOnPreferenceClickListener(// .
			new Preference.OnPreferenceClickListener() {
				public boolean onPreferenceClick(final Preference preference) {
					Log.collectAndSendLog(Preferences.this, Preferences.this
							.getString(R.string.sendlog_install_),
							Preferences.this
									.getString(R.string.sendlog_install),
							Preferences.this.getString(R.string.sendlog_run_),
							Preferences.this.getString(R.string.sendlog_run));
					return true;
				}
			});
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onSharedPreferenceChanged(
			final SharedPreferences sharedPreferences, final String key) {
		if (key.equals(Updater.PREFS_SPLIT_PLANS)
				|| key.equals(Updater.PREFS_MERGE_PLANS_SMS)
				|| key.equals(Updater.PREFS_MERGE_PLANS_CALLS)
				|| key.equals(Updater.PREFS_MERGE_SMS_TO_CALLS)
				|| key.equals(ExcludePeople.PREFS_EXCLUDE_PEOPLE_CALLS_ENABLE)
				|| key.equals(ExcludePeople.PREFS_EXCLUDE_PEOPLE_SMS_ENABLE)) {
			final boolean b0 = sharedPreferences.getBoolean(
					Updater.PREFS_SPLIT_PLANS, false);
			final boolean b1 = sharedPreferences.getBoolean(
					Updater.PREFS_MERGE_PLANS_SMS, false);
			final boolean b2 = sharedPreferences.getBoolean(
					Updater.PREFS_MERGE_PLANS_CALLS, false);
			final boolean b3 = sharedPreferences.getBoolean(
					Updater.PREFS_MERGE_SMS_TO_CALLS, false);
			final boolean b11 = sharedPreferences.getBoolean(
					ExcludePeople.PREFS_EXCLUDE_PEOPLE_CALLS_ENABLE, true);
			final boolean b12 = sharedPreferences.getBoolean(
					ExcludePeople.PREFS_EXCLUDE_PEOPLE_SMS_ENABLE, false);

			this.prefMergeSMStoCalls.setEnabled(!b0 || b1);
			this.prefMergeToPlan1.setEnabled(b0 && b1 && !b2 && b3);
			this.prefExcludeCallsToPlan1.setEnabled(b0 && b11 && !b2);
			this.prefExcludeCallsToPlan2.setEnabled(b0 && b11 && !b2);
			this.prefExcludeSmsToPlan1.setEnabled(b0 && b12 && !b1);
			this.prefExcludeSmsToPlan2.setEnabled(b0 && b12 && !b1);
		} else if (key.equals(Updater.PREFS_PLAN1_FREEMIN)) {
			final String s = sharedPreferences.getString(
					Updater.PREFS_PLAN1_FREEMIN, "");
			this.prefPlan1CostPerCall.setEnabled(s.length() == 0
					|| s.equals("0"));
		} else if (key.equals(Updater.PREFS_PLAN2_FREEMIN)) {
			final String s = sharedPreferences.getString(
					Updater.PREFS_PLAN2_FREEMIN, "");
			this.prefPlan2CostPerCall.setEnabled(s.length() == 0
					|| s.equals("0"));
		}
	}
}
