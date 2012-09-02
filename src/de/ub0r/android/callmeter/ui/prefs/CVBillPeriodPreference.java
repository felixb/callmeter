package de.ub0r.android.callmeter.ui.prefs;

import android.app.AlertDialog.Builder;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.ListPreference;
import android.widget.EditText;
import de.ub0r.android.callmeter.R;

/**
 * {@link ListPreference} for bill mode holding it's value in
 * {@link ContentValues}.
 * 
 * @author flx
 */
public final class CVBillPeriodPreference extends ListPreference {
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
	public CVBillPeriodPreference(final Context context, final ContentValues values,
			final String key) {
		super(context);
		this.setPersistent(false);
		this.setKey(key);
		this.cv = values;
		this.setEntryValues(R.array.billmodes);
		this.setEntries(R.array.billmodes);
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

	@Override
	protected void onDialogClosed(final boolean positiveResult) {
		final String ov = this.getValue();
		super.onDialogClosed(positiveResult);
		if (positiveResult) {
			String v = this.getValue();
			if (v == null || !v.contains("/")) {
				Builder b = new Builder(this.getContext());
				final EditText et = new EditText(this.getContext());
				et.setText(ov);
				b.setView(et);
				b.setCancelable(false);
				b.setTitle(this.getTitle());
				b.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface paramDialogInterface,
							final int paramInt) {
						CVBillPeriodPreference.this.setValue(ov);
					}
				});
				b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog, final int which) {
						String nv = et.getText().toString();
						CVBillPeriodPreference.this.setValue(nv);
						CVBillPeriodPreference.this.cv.put(CVBillPeriodPreference.this.getKey(), nv);
					}
				});
				b.show();
			} else {
				this.cv.put(this.getKey(), v);
			}
			if (this.ul != null) {
				this.ul.onUpdateValue(this);
			}
		}
	}
}
