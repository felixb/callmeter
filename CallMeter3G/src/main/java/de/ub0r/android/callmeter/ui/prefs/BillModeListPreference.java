package de.ub0r.android.callmeter.ui.prefs;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.ListPreference;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.EditText;
import android.widget.Toast;

import de.ub0r.android.callmeter.R;

public class BillModeListPreference extends ListPreference {

    private final Context ctx;

    public BillModeListPreference(final Context context) {
        super(context);
        ctx = context;
    }

    public BillModeListPreference(final Context context, final AttributeSet attr) {
        super(context, attr);
        ctx = context;
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
                        BillModeListPreference.this.setValue(ov);
                    }
                });
                b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
                        String nv = et.getText().toString().trim();
                        final String[] t = nv.toString().split("/");
                        if (t.length != 2 || !TextUtils.isDigitsOnly(t[0])
                                || !TextUtils.isDigitsOnly(t[1])) {
                            Toast.makeText(BillModeListPreference.this.ctx, R.string.missing_slash,
                                    Toast.LENGTH_LONG).show();
                            BillModeListPreference.this.setValue(ov);
                        } else {
                            BillModeListPreference.this.setValue(nv);
                        }
                    }
                });
                b.show();
            }
        }
    }
}
