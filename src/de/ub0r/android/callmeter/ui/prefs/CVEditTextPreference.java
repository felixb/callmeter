package de.ub0r.android.callmeter.ui.prefs;

import android.content.ContentValues;
import android.content.Context;
import android.preference.EditTextPreference;
import android.text.TextUtils;

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
	/** Default value. */
	private final String dv;

	/**
	 * Default constructor.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param values
	 *            {@link ContentValues}
	 * @param key
	 *            key
	 * @param defValue
	 *            default value
	 */
	public CVEditTextPreference(final Context context, final ContentValues values,
			final String key, final String defValue) {
		super(context);
		this.setPersistent(false);
		this.setKey(key);
		this.cv = values;
		this.dv = defValue;
		if (context instanceof UpdateListener) {
			this.ul = (UpdateListener) context;
		} else {
			this.ul = null;
		}
	}

	/**
	 * Default constructor.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param values
	 *            {@link ContentValues}
	 * @param key
	 *            key
	 * @param defValue
	 *            default value as resource id
	 */
	public CVEditTextPreference(final Context context, final ContentValues values,
			final String key, final int defValue) {
		this(context, values, key, context.getString(defValue));
	}

	@Override
	public void setTitle(final int titleResId) {
		super.setTitle(titleResId);
		this.setDialogTitle(titleResId);
	}

	@Override
	public void setTitle(final CharSequence title) {
		super.setTitle(title);
		this.setDialogTitle(title);
	}

	@Override
	public void setText(final String text) {
		if (TextUtils.isEmpty(text)) {
			super.setText(this.dv);
		} else {
			super.setText(text);
		}
	}

	/**
	 * Shortcut for getEditText().setHint().
	 * 
	 * @param resid
	 *            resource id
	 */
	public void setHint(final int resid) {
		this.getEditText().setHint(resid);
	}

	/**
	 * Shortcut for getEditText().setInputType().
	 * 
	 * @param inputType
	 *            input type
	 */
	public void setInputType(final int inputType) {
		this.getEditText().setInputType(inputType);
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
