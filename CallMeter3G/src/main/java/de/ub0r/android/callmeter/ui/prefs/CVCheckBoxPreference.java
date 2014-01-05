package de.ub0r.android.callmeter.ui.prefs;

import android.content.ContentValues;
import android.content.Context;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import de.ub0r.android.callmeter.R;

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
    /** Show help. */
    private final boolean sh;

    /**
     * Default constructor.
     *
     * @param context {@link Context}
     * @param values  {@link ContentValues}
     * @param key     key
     */
    public CVCheckBoxPreference(final Context context, final ContentValues values,
            final String key) {
        super(context);
        setPersistent(false);
        setKey(key);
        cv = values;
        sh = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                Preferences.PREFS_SHOWHELP, true);
        if (context instanceof UpdateListener) {
            ul = (UpdateListener) context;
        } else {
            ul = null;
        }
    }

    @Override
    protected View onCreateView(final ViewGroup parent) {
        View v = super.onCreateView(parent);
        TextView tv = (TextView) v.findViewById(android.R.id.summary);
        if (tv != null) {
            tv.setMaxLines(Integer.MAX_VALUE);
        }
        return v;
    }

    @Override
    protected void onClick() {
        super.onClick();
        if (!this.sh) {
            if (isChecked()) {
                super.setSummary(getContext().getString(R.string.value) + " "
                        + getContext().getString(android.R.string.yes));
            } else {
                super.setSummary(getContext().getString(R.string.value) + " "
                        + getContext().getString(android.R.string.no));
            }
        }
        cv.put(getKey(), isChecked());
        if (ul != null) {
            ul.onUpdateValue(this);
        }
    }

    @Override
    public void setChecked(final boolean checked) {
        super.setChecked(checked);
        if (sh) {
            setSummary(getContext().getText(R.string.value) + ": " + checked);
        }
    }
}
