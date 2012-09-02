package de.ub0r.android.callmeter.ui.prefs;

import android.app.AlertDialog.Builder;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.preference.ListPreference;

/**
 * {@link ListPreference} holding it's value in {@link ContentValues}.
 * 
 * @author flx
 */
public final class CVListPreference extends ListPreference {
	/** {@link ContentValues} for saving values. */
	private final ContentValues cv;
	/** Show a multi select dialog. */
	private final boolean m;
	/** Checked values. */
	private boolean[] checked = null;
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
	public CVListPreference(final Context context, final ContentValues values, final String key) {
		super(context);
		this.setPersistent(false);
		this.setKey(key);
		this.cv = values;
		this.m = false;
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
	 * @param multi
	 *            show multi selection
	 */
	public CVListPreference(final Context context, final ContentValues values, final String key,
			final boolean multi) {
		super(context);
		this.setPersistent(false);
		this.setKey(key);
		this.cv = values;
		this.m = multi;
		if (context instanceof UpdateListener) {
			this.ul = (UpdateListener) context;
		} else {
			this.ul = null;
		}
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

	/**
	 * Set static list.
	 * 
	 * @param values
	 *            values
	 * @param names
	 *            names
	 */
	public void setStatic(final String[] values, final String[] names) {
		this.setEntryValues(values);
		this.setEntries(names);
		if (this.m) {
			this.setCheckedArray();
		}
	}

	/**
	 * Set static list.
	 * 
	 * @param values
	 *            values
	 * @param names
	 *            names
	 */
	public void setStatic(final int values, final int names) {
		this.setEntryValues(values);
		this.setEntries(names);
		if (this.m) {
			this.setCheckedArray();
		}
	}

	/**
	 * Set list by {@link Cursor}.
	 * 
	 * @param c
	 *            {@link Cursor}
	 * @param value
	 *            index of id
	 * @param name
	 *            index of name
	 */
	public void setCursor(final Cursor c, final int value, final int name) {
		int l = c.getCount();
		String[] values = new String[l];
		String[] names = new String[l];
		if (c.moveToFirst()) {
			int i = 0;
			do {
				values[i] = c.getString(value);
				names[i] = c.getString(name);
				i++;
			} while (c.moveToNext());
		}
		this.setEntryValues(values);
		this.setEntries(names);
		c.close();
		if (this.m) {
			this.setCheckedArray();
		}
	}

	/** Initialize checked array. */
	private void setCheckedArray() {
		int l = this.getEntries().length;
		this.checked = new boolean[l];
		this.reloadCheckedArray();
	}

	/** Reload checked array. */
	private void reloadCheckedArray() {
		CharSequence[] values = this.getEntryValues();
		String s = "," + this.getValue() + ",";
		int l = this.checked.length;
		for (int i = 0; i < l; i++) {
			this.checked[i] = s.contains("," + values[i] + ",");
		}
	}

	/**
	 * @return checked values
	 */
	private String storeCheckedArray() {
		CharSequence[] values = this.getEntryValues();
		StringBuilder sb = new StringBuilder();
		sb.append(",");
		int l = this.checked.length;
		boolean empty = true;
		for (int i = 0; i < l; i++) {
			if (this.checked[i]) {
				sb.append(values[i]);
				sb.append(",");
				empty = false;
			}
		}
		if (empty) {
			return null;
		} else {
			return sb.toString();
		}
	}

	@Override
	public void setValue(final String value) {
		if (this.m) {
			String v = value;
			if (v != null) {
				v = v.replaceAll(",,", ",");
			}
			super.setValue(v);
			this.reloadCheckedArray();
		} else {
			super.setValue(value);
		}
	}

	@Override
	protected void onPrepareDialogBuilder(final Builder builder) {
		if (this.m) {
			builder.setMultiChoiceItems(this.getEntries(), this.checked,
					new DialogInterface.OnMultiChoiceClickListener() {
						public void onClick(final DialogInterface dialog, final int which,
								final boolean val) {
							CVListPreference.this.checked[which] = val;
						}
					});
		} else {
			super.onPrepareDialogBuilder(builder);
		}
	}

	@Override
	protected void onDialogClosed(final boolean positiveResult) {
		if (this.m) {
			if (positiveResult) {
				String v = this.storeCheckedArray();
				String ov = this.getValue();
				if (ov == null & v != null || ov != null && !ov.equals(v)) {
					this.setValue(v);
				}
				this.cv.put(this.getKey(), v);
			}
		} else {
			super.onDialogClosed(positiveResult);
			if (positiveResult) {
				this.cv.put(this.getKey(), this.getValue());
			}
		}
		if (positiveResult && this.ul != null) {
			this.ul.onUpdateValue(this);
		}
	}
}
