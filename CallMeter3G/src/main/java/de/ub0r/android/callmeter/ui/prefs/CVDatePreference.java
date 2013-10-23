package de.ub0r.android.callmeter.ui.prefs;

import java.util.Calendar;

import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.ContentValues;
import android.content.Context;
import android.os.Build;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.TimePicker;
import de.ub0r.android.callmeter.R;
import de.ub0r.android.lib.Log;

/**
 * DatePreference holding it's value in {@link ContentValues}.
 * 
 * @author flx
 */
public final class CVDatePreference extends DialogPreference implements OnTimeSetListener {
	private static final String TAG = "CVDatePreference";

	/** {@link ContentValues} for saving values. */
	private final ContentValues cv;
	/** {@link UpdateListener}. */
	private final UpdateListener ul;
	/** Show date and time? */
	private final boolean dt;
	/** Current date. */
	private final Calendar v = Calendar.getInstance();
	/** {@link DatePicker}. */
	private DatePicker dp = null;
	/** Show help. */
	private final boolean sh;

	/**
	 * Default constructor.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param values
	 *            {@link ContentValues}
	 * @param key
	 *            key
	 * @param showDateAndTime
	 *            Show date and time?
	 */
	public CVDatePreference(final Context context, final ContentValues values, final String key,
			final boolean showDateAndTime) {
		super(context, null);
		this.setPersistent(false);
		this.setKey(key);
		this.cv = values;
		this.dt = showDateAndTime;
		this.sh = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
				Preferences.PREFS_SHOWHELP, true);
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
	protected View onCreateView(final ViewGroup parent) {
		View view = super.onCreateView(parent);
		TextView tv = (TextView) view.findViewById(android.R.id.summary);
		if (tv != null) {
			tv.setMaxLines(Integer.MAX_VALUE);
		}
		return view;
	}

	@Override
	protected View onCreateDialogView() {
		this.dp = new DatePicker(this.getContext());
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			this.dp.setCalendarViewShown(true);
			this.dp.setSpinnersShown(false);
			this.dp.setMaxDate(System.currentTimeMillis());
		}
		this.updateDialog();
		return this.dp;
	}

	/**
	 * Set date and time as unix time stamp.
	 * 
	 * @param time
	 *            time stamp
	 */
	public void setValue(final long time) {
		this.v.setTimeInMillis(time);
		if (!this.sh) {
			this.setSummary(this.getContext().getString(R.string.value) + ": "
					+ DateFormat.getDateFormat(this.getContext()).format(this.v.getTime()));
		}
		this.updateDialog();
	}

	/**
	 * Set date and time as unix time stamp.
	 * 
	 * @param time
	 *            calendar
	 */
	public void setValue(final Calendar time) {
		if (time == null) {
			this.setValue(System.currentTimeMillis());
		} else {
			this.setValue(time.getTimeInMillis());
		}
	}

	/** Update {@link DatePicker}. */
	private void updateDialog() {
		if (this.dp == null) {
			return;
		}
		try {
			this.dp.updateDate(this.v.get(Calendar.YEAR), this.v.get(Calendar.MONTH),
					this.v.get(Calendar.DAY_OF_MONTH));
		} catch (ArrayIndexOutOfBoundsException e) {
			Log.e(TAG, "date array out of bound", e);
			this.v.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR));
			this.setValue(this.v);
		}
	}

	@Override
	protected void onDialogClosed(final boolean positiveResult) {
		if (positiveResult) {
			this.v.set(this.dp.getYear(), this.dp.getMonth(), this.dp.getDayOfMonth());
			this.cv.put(this.getKey(), this.v.getTimeInMillis());
			if (this.ul != null) {
				this.ul.onUpdateValue(this);
			}
			if (this.dt) {
				TimePickerDialog tpd = new TimePickerDialog(this.getContext(), this,
						this.v.get(Calendar.HOUR_OF_DAY), this.v.get(Calendar.MINUTE), true);
				tpd.setTitle(this.getTitle());
				tpd.setCancelable(true);
				tpd.show();
			}
		}
	}

	@Override
	public void onTimeSet(final TimePicker view, final int hourOfDay, final int minute) {
		this.v.set(Calendar.HOUR_OF_DAY, hourOfDay);
		this.v.set(Calendar.MINUTE, minute);
		this.cv.put(this.getKey(), this.v.getTimeInMillis());
		if (this.ul != null) {
			this.ul.onUpdateValue(this);
		}
	}
}
