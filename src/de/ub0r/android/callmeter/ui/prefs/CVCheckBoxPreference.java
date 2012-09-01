package de.ub0r.android.callmeter.ui.prefs;

import android.content.ContentValues;
import android.content.Context;
import android.preference.CheckBoxPreference;

/**
 * {@link CheckBoxPreference} holding it's value in {@link ContentValues}.
 * 
 * @author flx
 */
public final class CVCheckBoxPreference extends CheckBoxPreference {
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
	public CVCheckBoxPreference(final Context context, final ContentValues values, final String key) {
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
	protected void onClick() {
		super.onClick();
		this.cv.put(this.getKey(), this.isChecked());
		if (this.ul != null) {
			this.ul.onUpdateValue(this);
		}
	}
}
