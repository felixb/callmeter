package de.ub0r.android.callmeter.ui.prefs;

import android.app.AlertDialog.Builder;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import de.ub0r.android.callmeter.R;

/**
 * {@link ListPreference} for bill mode holding it's value in {@link ContentValues}.
 *
 * @author flx
 */
public final class CVBillModePreference extends ListPreference {

    private final Context ctx;

    /**
     * {@link ContentValues} for saving values.
     */
    private final ContentValues cv;

    /**
     * {@link UpdateListener}.
     */
    private final UpdateListener ul;

    /**
     * Show help.
     */
    private final boolean sh;

    /**
     * Default constructor.
     *
     * @param context {@link Context}
     * @param values  {@link ContentValues}
     * @param key     key
     */
    public CVBillModePreference(final Context context, final ContentValues values,
            final String key) {
        super(context);
        ctx = context;
        setPersistent(false);
        setKey(key);
        cv = values;
        setEntryValues(R.array.billmodes);
        setEntries(R.array.billmodes);
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
    public void setValue(final String value) {
        super.setValue(value);
        if (!this.sh) {
            setSummary(getContext().getText(R.string.value) + ": " + getValue());
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
    protected void onDialogClosed(final boolean positiveResult) {
        final String ov = getValue();
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            String v = getValue();
            if (v == null || !v.contains("/")) { // custom bill mode
                Builder b = new Builder(getContext());
                final EditText et = new EditText(getContext());
                et.setText(ov);
                b.setView(et);
                b.setCancelable(false);
                b.setTitle(getTitle());
                b.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface paramDialogInterface,
                            final int paramInt) {
                        CVBillModePreference.this.setValue(ov);
                    }
                });
                b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
                        String nv = et.getText().toString().trim();
                        final String[] t = nv.toString().split("/");
                        if (t.length != 2 || !TextUtils.isDigitsOnly(t[0])
                                || !TextUtils.isDigitsOnly(t[1])) {
                            Toast.makeText(CVBillModePreference.this.ctx, R.string.missing_slash,
                                    Toast.LENGTH_LONG).show();
                            CVBillModePreference.this.setValue(ov);
                        } else {
                            CVBillModePreference.this.setValue(nv);
                            CVBillModePreference.this.cv
                                    .put(CVBillModePreference.this.getKey(), nv);
                            if (CVBillModePreference.this.ul != null) {
                                CVBillModePreference.this.ul
                                        .onUpdateValue(CVBillModePreference.this);
                            }
                        }
                    }
                });
                b.show();
            } else {
                cv.put(getKey(), v);
                if (ul != null) {
                    ul.onUpdateValue(this);
                }
            }
        }
    }
}
