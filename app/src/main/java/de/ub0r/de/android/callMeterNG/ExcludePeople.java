package de.ub0r.de.android.callMeterNG;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;

import java.util.ArrayList;

import de.ub0r.android.lib.apis.ContactsWrapper;
import de.ub0r.android.logg0r.Log;

/**
 * Preferences sub screen to exclude numbers.
 *
 * @author flx
 */
public class ExcludePeople extends ListActivity implements OnItemClickListener {

    /**
     * Tag for output.
     */
    private static final String TAG = "ExcludePeople";

    private static final int PERMISSION_REQUEST_READ_CONTACTS = 1;

    /**
     * {@link ContactsWrapper}.
     */
    static final ContactsWrapper CWRAPPER = ContactsWrapper.getInstance();

    /**
     * Prefs: Exclude people prefix.
     */
    static final String PREFS_EXCLUDE_PEOPLE_PREFIX = "exclude_people_";

    /**
     * Prefs: Exclude people count.
     */
    static final String PREFS_EXCLUDE_PEOPLE_COUNT = PREFS_EXCLUDE_PEOPLE_PREFIX
            + "n";

    /**
     * Prefs: excluded people, enable for calls.
     */
    static final String PREFS_EXCLUDE_PEOPLE_CALLS_ENABLE = // .
            PREFS_EXCLUDE_PEOPLE_PREFIX + "calls";

    /**
     * Prefs: excluded people, enable for sms.
     */
    static final String PREFS_EXCLUDE_PEOPLE_SMS_ENABLE = // .
            PREFS_EXCLUDE_PEOPLE_PREFIX + "sms";

    /**
     * Prefs: Bill calls to/from excluded people to plan #1.
     */
    static final String PREFS_EXCLUDE_PEOPLE_CALLS_PLAN1 = // .
            PREFS_EXCLUDE_PEOPLE_PREFIX + "to_plan1";

    /**
     * Prefs: Bill calls to/from excluded people to plan #2.
     */
    static final String PREFS_EXCLUDE_PEOPLE_CALLS_PLAN2 = // .
            PREFS_EXCLUDE_PEOPLE_PREFIX + "to_plan2";

    /**
     * Prefs: Bill sms to/from excluded people to plan #1.
     */
    static final String PREFS_EXCLUDE_PEOPLE_SMS_PLAN1 = // .
            PREFS_EXCLUDE_PEOPLE_PREFIX + "sms_to_plan1";

    /**
     * Prefs: Bill sms to/from excluded people to plan #2.
     */
    static final String PREFS_EXCLUDE_PEOPLE_SMS_PLAN2 = // .
            PREFS_EXCLUDE_PEOPLE_PREFIX + "sms_to_plan2";

    /**
     * Preferences: excluded numbers.
     */
    static ArrayList<ExcludedPerson> prefsExcludePeople;

    /**
     * Adapter used for the list.
     */
    private ArrayAdapter<ExcludedPerson> adapter = null;

    /**
     * Excluded person.
     *
     * @author flx
     */
    public static final class ExcludedPerson {

        /**
         * Person not found.
         */
        private static final String NOT_FOUND = "##NOTFOUND##";

        /**
         * {@link Context}.
         */
        private static Context mContext = null;

        /**
         * Number.
         */
        private final String mNumber;

        /**
         * Name.
         */
        private String mName = null;

        /**
         * Default constructor.
         *
         * @param num mNumber
         */
        public ExcludedPerson(final String num) {
            mNumber = num;
        }

        /**
         * @return mNumber
         */
        public String getNumber() {
            return mNumber;
        }

        @Override
        public String toString() {
            if (mContext != null
                    && (mName == null || !NOT_FOUND.equals(mName))
                    && !mNumber.contains("*")
                    && CallMeter.hasPermission(mContext, Manifest.permission.READ_CONTACTS)) {
                mName = ContactsWrapper.getInstance().getNameForNumber(
                        mContext.getContentResolver(), mNumber);
                if (mName == null || mName.equals(this.mNumber)) {
                    mName = NOT_FOUND;
                }
            }

            if (mName == null || NOT_FOUND.equals(mName)) {
                return mNumber;
            } else {
                return String.format("%s <%s>", mName, mNumber);
            }
        }
    }

    @Override
    protected final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // this.setContentView(R.layout.exclude_people);
        prefsExcludePeople = loadExcludedPeople(this);
        this.adapter = new ArrayAdapter<ExcludedPerson>(this,
                android.R.layout.simple_list_item_1, prefsExcludePeople);
        this.setListAdapter(this.adapter);
        this.getListView().setOnItemClickListener(this);

        CallMeter.requestPermission(this, Manifest.permission.READ_CONTACTS,
                PERMISSION_REQUEST_READ_CONTACTS, R.string.permissions_read_contacts, null);
    }

    @Override
    protected final void onResume() {
        super.onResume();
        ExcludedPerson.mContext = this;
    }

    @Override
    protected final void onPause() {
        super.onPause();
        SharedPreferences.Editor editor = PreferenceManager
                .getDefaultSharedPreferences(this).edit();
        final int s = prefsExcludePeople.size();
        editor.putInt(PREFS_EXCLUDE_PEOPLE_COUNT, s - 1);
        for (int i = 1; i < s; i++) {
            editor.putString(PREFS_EXCLUDE_PEOPLE_PREFIX + (i - 1),
                    prefsExcludePeople.get(i).getNumber());
        }
        editor.apply();
        ExcludedPerson.mContext = null;
    }

    @Override
    protected final void onActivityResult(final int requestCode,
            final int resultCode, final Intent data) {
        Log.d(TAG, "requestCode: " + requestCode);
        Log.d(TAG, "resultCode: " + requestCode);
        if (data == null || data.getData() == null) {
            return;
        }
        Log.d(TAG, "data: " + data.getData().toString());
        // get mNumber for uri
        String number = CWRAPPER.getNameAndNumber(this.getContentResolver(),
                data.getData());
        if (number == null) {
            number = "???";
        } else {
            number = number.replaceAll("[^+0-9]", "");
        }
        Log.d(TAG, number);
        if (requestCode == 0) {
            prefsExcludePeople.add(new ExcludedPerson(number));
        } else {
            prefsExcludePeople.set(requestCode, new ExcludedPerson(number));
        }
        this.adapter.notifyDataSetChanged();
    }

    public final void onItemClick(final AdapterView<?> parent, final View view,
            final int position, final long id) {
        if (position == 0) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            final EditText et = new EditText(this);
            builder.setView(et);
            builder.setTitle(R.string.exclude_people_add);
            builder.setCancelable(true);
            builder.setPositiveButton(android.R.string.ok,
                    new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog,
                                final int id) {
                            prefsExcludePeople.add(new ExcludedPerson(et
                                    .getText().toString()));
                            ExcludePeople.this.adapter.notifyDataSetChanged();
                        }
                    });
            builder.setNeutralButton(R.string.contacts_,
                    new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog,
                                final int id) {
                            final Intent intent = CWRAPPER.getPickPhoneIntent();
                            ExcludePeople.this.startActivityForResult(intent,
                                    position);
                        }
                    });
            builder.setNegativeButton(android.R.string.cancel, null);
            builder.create().show();
        } else {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(true);
            final String[] itms = new String[2];
            itms[0] = this.getString(R.string.exclude_people_edit);
            itms[1] = this.getString(R.string.exclude_people_delete);
            builder.setItems(itms, new DialogInterface.OnClickListener() {
                public void onClick(final DialogInterface dialog, // .
                        final int item) {
                    if (item == 0) { // edit
                        final AlertDialog.Builder builder2 = // .
                                new AlertDialog.Builder(ExcludePeople.this);
                        final EditText et = new EditText(ExcludePeople.this);
                        et
                                .setText(prefsExcludePeople.get(position)
                                        .getNumber());
                        builder2.setView(et);
                        builder2.setTitle(R.string.exclude_people_edit);
                        builder2.setCancelable(true);
                        builder2.setPositiveButton(android.R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(
                                            final DialogInterface dialog,
                                            final int id) {
                                        prefsExcludePeople.set(position,
                                                new ExcludedPerson(et.getText()
                                                        .toString()));
                                        ExcludePeople.this.adapter
                                                .notifyDataSetChanged();
                                    }
                                });
                        builder2.setNeutralButton(R.string.contacts_,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(
                                            final DialogInterface dialog,
                                            final int id) {
                                        final Intent intent = // .
                                                new Intent(Intent.ACTION_GET_CONTENT);
                                        intent.setType("vnd.android"
                                                + ".cursor.item/phone");
                                        ExcludePeople.this
                                                .startActivityForResult(intent,
                                                        position);
                                    }
                                });
                        builder2.setNegativeButton(android.R.string.cancel,
                                null);
                        builder2.create().show();
                    } else { // delete
                        prefsExcludePeople.remove(position);
                        ExcludePeople.this.adapter.notifyDataSetChanged();
                    }
                }

            });
            builder.create().show();
        }
    }

    /**
     * Load excluded people from preferences.
     *
     * @param context {@link Context}
     * @return list of excluded people
     */
    static ArrayList<ExcludedPerson> loadExcludedPeople(final Context context) {
        final SharedPreferences p = PreferenceManager
                .getDefaultSharedPreferences(context);
        ArrayList<ExcludedPerson> ret = new ArrayList<ExcludedPerson>();
        ret.add(new ExcludedPerson(context
                .getString(R.string.exclude_people_add)));
        final int c = p.getInt(PREFS_EXCLUDE_PEOPLE_COUNT, 0);
        for (int i = 0; i < c; i++) {
            ret.add(new ExcludedPerson(p.getString(PREFS_EXCLUDE_PEOPLE_PREFIX
                    + i, "???")));
        }
        return ret;
    }
}
