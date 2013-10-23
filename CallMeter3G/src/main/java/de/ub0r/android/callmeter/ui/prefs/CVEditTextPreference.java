package de.ub0r.android.callmeter.ui.prefs;

import android.content.ContentValues;
import android.content.Context;
import android.preference.EditTextPreference;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import de.ub0r.android.callmeter.R;

/**
 * {@link EditTextPreference} holding it's value in {@link ContentValues}.
 *
 * @author flx
 */
public class CVEditTextPreference extends EditTextPreference {

    /** {@link ContentValues} for saving values. */
    private final ContentValues cv;
    /** {@link UpdateListener}. */
    private final UpdateListener ul;
    /** Default value. */
    private final String dv;
    /** Show help. */
    private final boolean sh;

    /**
     * Default constructor.
     *
     * @param context  {@link Context}
     * @param values   {@link ContentValues}
     * @param key      key
     * @param defValue default value
     */
    public CVEditTextPreference(final Context context, final ContentValues values,
            final String key, final String defValue) {
        super(context);
        setPersistent(false);
        setKey(key);
        cv = values;
        dv = defValue;
        sh = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                Preferences.PREFS_SHOWHELP, true);
        if (context instanceof UpdateListener) {
            ul = (UpdateListener) context;
        } else {
            ul = null;
        }
    }

    /**
     * Default constructor.
     *
     * @param context  {@link Context}
     * @param values   {@link ContentValues}
     * @param key      key
     * @param defValue default value as resource id
     */
    public CVEditTextPreference(final Context context, final ContentValues values,
            final String key, final int defValue) {
        this(context, values, key, context.getString(defValue));
    }

    @Override
    public void setTitle(final int titleResId) {
        super.setTitle(titleResId);
        setDialogTitle(titleResId);
    }

    @Override
    public void setTitle(final CharSequence title) {
        super.setTitle(title);
        setDialogTitle(title);
    }

    @Override
    public void setText(final String text) {
        if (TextUtils.isEmpty(text)) {
            super.setText(dv);
            if (!TextUtils.isEmpty(dv) && !"0".equals(dv) && ul != null) {
                ul.onSetDefaultValue(this, getText().toString());
            }
        } else {
            super.setText(text);
        }
        if (!this.sh) {
            String t = getText();
            setSummary(getContext().getString(R.string.value) + ": "
                    + (t == null ? "" : t.trim()));
        }
    }

    /**
     * Shortcut for getEditText().setHint().
     *
     * @param resid resource id
     */
    public void setHint(final int resid) {
        getEditText().setHint(resid);
    }

    /**
     * Shortcut for getEditText().setInputType().
     *
     * @param inputType input type
     */
    public void setInputType(final int inputType) {
        getEditText().setInputType(inputType);
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
    protected void onDialogClosed(final boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            cv.put(getKey(), getText());
            if (ul != null) {
                ul.onUpdateValue(this);
            }
        }
    }
}
