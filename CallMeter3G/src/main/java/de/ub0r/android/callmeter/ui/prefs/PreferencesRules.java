package de.ub0r.android.callmeter.ui.prefs;

import com.actionbarsherlock.app.SherlockPreferenceActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import de.ub0r.android.callmeter.R;
import de.ub0r.android.lib.Utils;
import de.ub0r.android.logg0r.Log;

public class PreferencesRules extends SherlockPreferenceActivity {

    private static final String TAG = "PreferencesRules";

    public static final String EXTRA_JSON = "json";

    public static final String EXTRA_COUNTRY = "country";

    private static class RuleComparator implements Comparator<JSONObject> {

        @Override
        public int compare(final JSONObject lhs, final JSONObject rhs) {
            try {
                return lhs.getString("title").compareTo(rhs.getString("title"));
            } catch (JSONException e) {
                Log.e(TAG, "JSONError", e);
                return 0;
            }
        }
    }

    private static class ProviderComperator implements Comparator<String> {

        @Override
        public int compare(final String lhs, final String rhs) {
            if (lhs.equals(rhs)) {
                return 0;
            }
            if ("all".equals(lhs)) {
                return -1;
            }
            if ("all".equals(rhs)) {
                return 1;
            }
            return lhs.compareToIgnoreCase(rhs);
        }
    }

    private static class OnRuleClickListener implements OnPreferenceClickListener {

        private final Context c;

        private final JSONObject j;

        public OnRuleClickListener(final Context context, final JSONObject json) {
            c = context;
            j = json;
        }

        @Override
        public boolean onPreferenceClick(final Preference preference) {
            try {
                final String p = j.getString("provider").trim();
                final String t = j.getString("title").trim();
                final String d = j.isNull("description") ? null : j.getString(
                        "description").trim();
                final String ld = j.isNull("longdescription") ? null : j.getString(
                        "longdescription").trim();
                final String link = j.isNull("link") ? null : j.getString("link").trim();

                final String url = j.getString("importurl").trim();
                Builder b = new Builder(c);
                b.setCancelable(true);
                if ("all".equals(p)) {
                    b.setTitle(t);
                } else {
                    b.setTitle(p + " - " + t);
                }
                if (ld == null) {
                    b.setMessage(d);
                } else {
                    b.setMessage(ld);
                }
                b.setPositiveButton(R.string.import_, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
                        OnRuleClickListener.this.c.startActivity(new Intent(Intent.ACTION_VIEW, Uri
                                .parse(url), OnRuleClickListener.this.c, Preferences.class));
                    }
                });
                if (link != null) {
                    b.setNeutralButton(R.string.import_link_,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(final DialogInterface dialog, final int which) {
                                    OnRuleClickListener.this.c.startActivity(new Intent(
                                            Intent.ACTION_VIEW, Uri.parse(link)));
                                }
                            });
                }
                b.show();
                return true;
            } catch (JSONException e) {
                Log.e(TAG, "JSONError", e);
                Toast.makeText(c, R.string.err_export_read, Toast.LENGTH_LONG).show();
                return false;
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.setLocale(this);

        String k = getIntent().getStringExtra(EXTRA_COUNTRY);
        setTitle(k);

        addPreferencesFromResource(R.xml.group_prefs);
        PreferenceScreen base = (PreferenceScreen) findPreference("container");
        PreferenceManager pm = getPreferenceManager();

        try {
            JSONObject json = new JSONObject(getIntent().getStringExtra(EXTRA_JSON));
            JSONArray ja = json.getJSONArray(k);

            ArrayList<String> providers = new ArrayList<String>();
            HashMap<String, ArrayList<JSONObject>> map
                    = new HashMap<String, ArrayList<JSONObject>>();

            int l = ja.length();
            for (int i = 0; i < l; i++) {
                try {
                    JSONObject jo = ja.getJSONObject(i);
                    String provider = jo.getString("provider");

                    ArrayList<JSONObject> list = map.get(provider);
                    if (list == null) {
                        providers.add(provider);
                        list = new ArrayList<JSONObject>();
                        map.put(provider, list);
                    }
                    list.add(jo);
                } catch (JSONException e) {
                    Log.e(TAG, "JSONError", e);
                    Toast.makeText(this, R.string.err_export_read, Toast.LENGTH_LONG).show();
                }
            }

            Collections.sort(providers, new ProviderComperator());
            RuleComparator rc = new RuleComparator();
            for (String provider : providers) {
                PreferenceGroup pg = new PreferenceCategory(this);
                pg.setPersistent(false);
                pg.setTitle(provider);
                base.addPreference(pg);

                // walk through list
                ArrayList<JSONObject> list = map.get(provider);
                Collections.sort(list, rc);
                for (JSONObject jo : list) {
                    PreferenceScreen ps = pm.createPreferenceScreen(this);
                    ps.setPersistent(false);
                    ps.setTitle(jo.getString("title").trim());
                    if (!jo.isNull("description")) {
                        ps.setSummary(jo.getString("description").trim());
                    }
                    ps.setOnPreferenceClickListener(new OnRuleClickListener(this, jo));
                    pg.addPreference(ps);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "JSONError", e);
            Toast.makeText(this, R.string.err_export_read, Toast.LENGTH_LONG).show();
            finish();
        }
    }
}
