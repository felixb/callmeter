package de.ub0r.android.callmeter.ui.prefs;

import android.content.ContentValues;
import android.content.Context;
import android.preference.EditTextPreference;

/**
 * {@link EditTextPreference} holding it's value in {@link ContentValues}.
 * 
 * @author flx
 */
public final class CVEditTextPreference extends EditTextPreference {

	/** {@link ContentValues} for saving values. */
	private final ContentValues cv;
	/** {@link UpdateListener}. */
	private final UpdateListener ul;

	/**
	 * Default constructor.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param values
	 *            {@link ContentValues}
	 * @param key
	 *            key
	 */
	public CVEditTextPreference(final Context context, final ContentValues values, final String key) {
		super(context);
		this.setPersistent(false);
		this.setKey(key);
		this.cv = values;
		if (context instanceof UpdateListener) {
			this.ul = (UpdateListener) context;
		} else {
			this.ul = null;
		}
	}

	@Override
	protected void onDialogClosed(final boolean positiveResult) {
		super.onDialogClosed(positiveResult);
		if (positiveResult) {
			this.cv.put(this.getKey(), this.getText());
			if (this.ul != null) {
				this.ul.onUpdateValue(this);
			}
		}
	}
}
