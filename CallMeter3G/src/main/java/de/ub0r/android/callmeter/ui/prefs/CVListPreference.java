package de.ub0r.android.callmeter.ui.prefs;

import android.app.AlertDialog.Builder;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import de.ub0r.android.callmeter.R;

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
    /** Show help. */
    private final boolean sh;

    /**
     * Default constructor.
     *
     * @param context {@link Context}
     * @param values  {@link ContentValues}
     * @param key     key
     */
    public CVListPreference(final Context context, final ContentValues values, final String key) {
        super(context);
        setPersistent(false);
        setKey(key);
        cv = values;
        m = false;
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
     * @param context {@link Context}
     * @param values  {@link ContentValues}
     * @param key     key
     * @param multi   show multi selection
     */
    public CVListPreference(final Context context, final ContentValues values, final String key,
            final boolean multi) {
        super(context);
        setPersistent(false);
        setKey(key);
        cv = values;
        m = multi;
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

    /**
     * Set static list.
     *
     * @param values values
     * @param names  names
     */
    public void setStatic(final String[] values, final String[] names) {
        setEntryValues(values);
        setEntries(names);
        if (this.m) {
            setCheckedArray();
        }
    }

    /**
     * Set static list.
     *
     * @param values values
     * @param names  names
     */
    public void setStatic(final int values, final int names) {
        setEntryValues(values);
        setEntries(names);
        if (this.m) {
            setCheckedArray();
        }
    }

    /**
     * Set list by {@link Cursor}.
     *
     * @param c     {@link Cursor}
     * @param value index of id
     * @param name  index of name
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
        setEntryValues(values);
        setEntries(names);
        c.close();
        if (this.m) {
            setCheckedArray();
        }
    }

    /** Initialize checked array. */
    private void setCheckedArray() {
        int l = getEntries().length;
        checked = new boolean[l];
        reloadCheckedArray();
    }

    /** Reload checked array. */
    private void reloadCheckedArray() {
        CharSequence[] values = getEntryValues();
        String s = "," + getValue() + ",";
        int l = checked.length;
        for (int i = 0; i < l; i++) {
            checked[i] = s.contains("," + values[i] + ",");
        }
    }

    /**
     * @return checked values
     */
    private String storeCheckedArray() {
        CharSequence[] values = getEntryValues();
        StringBuilder sb = new StringBuilder();
        sb.append(",");
        int l = checked.length;
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
            reloadCheckedArray();
            if (!this.sh) {
                StringBuilder sb = new StringBuilder();
                sb.append(this.getContext().getString(R.string.value));
                sb.append(": ");
                for (int i = 0; i < checked.length; i++) {
                    if (this.checked[i]) {
                        CharSequence e = getEntries()[i];
                        sb.append(e == null ? "?" : e.toString().trim());
                        sb.append(", ");
                    }
                }
                String s = sb.toString();
                if (s.endsWith(", ")) {
                    s = s.substring(0, s.length() - 2);
                }
                setSummary(s);
            }
        } else {
            super.setValue(value);
            if (!this.sh) {
                CharSequence e = getEntry();
                setSummary(this.getContext().getString(R.string.value) + ": "
                        + (e == null ? "" : e.toString().trim()));
            }
        }
    }

    @Override
    protected void onPrepareDialogBuilder(final Builder builder) {
        if (this.m) {
            builder.setMultiChoiceItems(this.getEntries(), checked,
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
        if (this.m) {
            if (positiveResult) {
                String v = storeCheckedArray();
                String ov = getValue();
                if (ov == null & v != null || ov != null && !ov.equals(v)) {
                    setValue(v);
                }
                cv.put(this.getKey(), v);
            }
        } else {
            super.onDialogClosed(positiveResult);
            if (positiveResult) {
                cv.put(this.getKey(), getValue());
            }
        }
        if (positiveResult && ul != null) {
            ul.onUpdateValue(this);
        }
    }
}
