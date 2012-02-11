package de.ub0r.android.callmeter.ui.prefs;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

/**
 * {@link ArrayAdapter} holding a list of preferences.
 * 
 * @author flx
 */
final class PreferenceAdapter extends ArrayAdapter<Preference> {
	/** Show help. */
	private final boolean showHelp;

	/**
	 * Default constructor.
	 * 
	 * @param context
	 *            {@link Context}
	 */
	PreferenceAdapter(final Context context) {
		super(context, 0);
		this.showHelp = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
				Preferences.PREFS_SHOWHELP, true);
	}

	/**
	 * Load all data from {@link Cursor}.
	 * 
	 * @param cursor
	 *            {@link Cursor}
	 */
	public void load(final Cursor cursor) {
		final int l = this.getCount();
		for (int i = 0; i < l; i++) {
			final Preference p = this.getItem(i);
			p.load(cursor);
		}
	}

	/**
	 * Save all data to {@link ContentValues}.
	 * 
	 * @return {@link ContentValues}
	 */
	public ContentValues save() {
		final ContentValues values = new ContentValues();
		final int l = this.getCount();
		for (int i = 0; i < l; i++) {
			final Preference p = this.getItem(i);
			p.save(values);
		}
		return values;
	}

	/**
	 * Get a {@link Preference} by name.
	 * 
	 * @param name
	 *            name of {@link Preference}
	 * @return {@link Preference} or null
	 */
	Preference getPreference(final String name) {
		final int l = this.getCount();
		for (int i = 0; i < l; i++) {
			final Preference p = this.getItem(i);
			if (name.equals(p.name)) {
				return p;
			}
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public View getView(final int position, final View convertView, final ViewGroup parent) {
		return this.getItem(position).getView(convertView, parent, this.showHelp);
	}

	/**
	 * Show/hide a {@link Preference}.
	 * 
	 * @param name
	 *            name of {@link Preference}
	 * @param hide
	 *            hide the {@link Preference}?
	 */
	public void hide(final String name, final boolean hide) {
		final Preference p = this.getPreference(name);
		if (p != null) {
			p.hide(hide);
		}
	}
}
