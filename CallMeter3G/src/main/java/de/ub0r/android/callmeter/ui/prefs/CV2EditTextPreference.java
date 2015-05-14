package de.ub0r.android.callmeter.ui.prefs;

import android.content.ContentValues;
import android.content.Context;
import android.preference.EditTextPreference;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import de.ub0r.android.callmeter.R;

/**
 * {@link EditTextPreference} showing 2 {@link EditText}s and holding it's value in {@link
 * ContentValues}.
 *
 * @author flx
 */
public final class CV2EditTextPreference extends EditTextPreference {

    /**
     * {@link ContentValues} for saving values.
     */
    private final ContentValues cv;

    /**
     * {@link UpdateListener}.
     */
    private final UpdateListener ul;

    /**
     * Single mode.
     */
    private final boolean sm;

    /**
     * Default value.
     */
    private final String dv1, dv2;

    /**
     * Key 2.
     */
    private final String k2;

    /**
     * Value 2.
     */
    private String v2;

    /**
     * {@link EditText}s in multi mode.
     */
    private EditText et1, et2;

    /**
     * Input type.
     */
    private int it = -1;

    /**
     * Hint.
     */
    private int h = -1;

    /**
     * Show help.
     */
    private final boolean sh;

    /**
     * Default constructor.
     *
     * @param context    {@link Context}
     * @param values     {@link ContentValues}
     * @param key1       key 1
     * @param key2       key 2
     * @param singleMode single mode
     * @param defValue1  default value 1
     * @param defValue2  default value 2
     */
    public CV2EditTextPreference(final Context context, final ContentValues values,
            final String key1, final String key2, final boolean singleMode, final String defValue1,
            final String defValue2) {
        super(context);
        setPersistent(false);
        setKey(key1);
        k2 = key2;
        cv = values;
        sm = singleMode;
        dv1 = defValue1;
        dv2 = defValue2;
        sh = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                Preferences.PREFS_SHOWHELP, true);
        if (context instanceof UpdateListener) {
            ul = (UpdateListener) context;
        } else {
            ul = null;
        }
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
        setText(text, text);
    }

    /**
     * Set text.
     *
     * @param text1 text 1
     * @param text2 text 2
     */
    public void setText(final String text1, final String text2) {
        if (TextUtils.isEmpty(text1)) {
            super.setText(dv1);
        } else {
            super.setText(text1);
        }
        if (sm) {
            if (!this.sh) {
                setSummary(getContext().getString(R.string.value) + " " + getText());
            }
        } else {
            if (TextUtils.isEmpty(text2)) {
                v2 = dv2;
            } else {
                v2 = text2;
            }

            if (et1 != null) {
                et1.setText(getText());
                et2.setText(v2);
            }
            if (!this.sh) {
                if (TextUtils.isEmpty(v2) || v2.equals(getText())) {
                    setSummary(getContext().getString(R.string.value) + ": "
                            + getText());
                } else {
                    setSummary(getContext().getString(R.string.value) + ": "
                            + getText() + "/" + v2);
                }
            }
        }
    }

    /**
     * Shortcut for getEditText().setHint().
     *
     * @param resid resource id
     */
    public void setHint(final int resid) {
        if (sm) {
            getEditText().setHint(resid);
        } else if (et1 != null) {
            et1.setHint(resid);
            et2.setHint(resid);
        }
        h = resid;
    }

    /**
     * Set InputType.
     *
     * @param inputType input type
     */
    public void setInputType(final int inputType) {
        if (sm) {
            getEditText().setInputType(inputType);
        } else if (et1 != null) {
            et1.setInputType(inputType);
            et2.setInputType(inputType);
        }
        it = inputType;
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
    protected View onCreateDialogView() {
        if (sm) {
            return super.onCreateDialogView();
        } else {
            View v = LayoutInflater.from(getContext()).inflate(R.layout.doubleedit, null);
            et1 = (EditText) v.findViewById(android.R.id.text1);
            et2 = (EditText) v.findViewById(android.R.id.text2);
            et1.setText(getText());
            et2.setText(v2);
            if (it != -1) {
                et1.setInputType(it);
                et2.setInputType(it);
            }
            if (h != -1) {
                et1.setHint(h);
                et2.setHint(h);
            }
            return v;
        }
    }

    @Override
    protected void onDialogClosed(final boolean positiveResult) {
        if (sm) {
            super.onDialogClosed(positiveResult);
        }
        if (positiveResult) {
            if (sm || et1 == null) {
                String v = getText();
                cv.put(getKey(), v);
                cv.put(k2, v);
            } else {
                setText(et1.getText().toString(), et2.getText().toString());
                cv.put(getKey(), getText());
                cv.put(k2, v2);
            }
            if (ul != null) {
                ul.onUpdateValue(this);
            }
        }
    }
}
