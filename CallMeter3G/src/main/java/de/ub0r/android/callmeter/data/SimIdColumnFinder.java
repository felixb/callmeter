package de.ub0r.android.callmeter.data;

import android.content.ContentResolver;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;

import de.ub0r.android.callmeter.BuildConfig;
import de.ub0r.android.logg0r.Log;

public class SimIdColumnFinder {

    private static final String TAG = "SimIdColumnFinder";

    private static final String[] SIM_ID_COLUMN_NAMES = {"sim_id", "simid", "sub_id", "subscription_id", "sim_slot",
            "sim_sn", "subscription"};

    private static SimIdColumnFinder sInstance;

    private HashMap<Uri, String> mCache = new HashMap<>();

    private SimIdColumnFinder() {
    }

    public synchronized static SimIdColumnFinder getsInstance() {
        if (sInstance == null) {
            sInstance = new SimIdColumnFinder();
        }

        return sInstance;
    }

    /**
     * Get column id holding sim_id, simid or whatever.
     */
    public int getSimIdColumn(final ContentResolver cr, final Uri uri, final Cursor c) {
        if (c == null) {
            return -1;
        }

        if (mCache.containsKey(uri)) {
            final String simIdColumnName = mCache.get(uri);
            if (simIdColumnName == null) {
                return -1;
            } else {
                return c.getColumnIndex(simIdColumnName);
            }
        }

        ArrayList<String> simIdColumns = getAllSimIdColumnNames(c);
        if (simIdColumns.size() == 0) {
            if (BuildConfig.DEBUG_LOG) {
                printColumnNames(c);
            }

            Log.d(TAG, "no sim_id column found");
            mCache.put(uri, null);
            return -1;
        } else if (simIdColumns.size() == 1) {
            Log.d(TAG, "found a single sim id column");
            final String simIdColumnName = simIdColumns.get(0);
            mCache.put(uri, simIdColumnName);
            return c.getColumnIndex(simIdColumnName);
        } else {
            Log.d(TAG, "found multiple sim_id columns: ", simIdColumns.size());
            // select simid column based on content of rows (e.g. which row has column with val != -1 if more than one one exists)
            for (String simIdColumnName : simIdColumns) {
                if (getSimIdCountForColumn(cr, uri, simIdColumnName) > 0) {
                    mCache.put(uri, simIdColumnName);
                    return c.getColumnIndex(simIdColumnName);
                }
            }
            final String firstColumnName = simIdColumns.get(0);
            Log.w(TAG, "no sim_id values found for any column, picking the first one: ", firstColumnName);
            return c.getColumnIndex(firstColumnName);
        }
    }

    public String getSecondSimId(final ContentResolver cr, final Uri uri) {
        try {
            String secondSimId = null;
            Cursor c = cr.query(uri, null, "1=2", null, null);
            assert c != null;
            int id = getSimIdColumn(cr, uri, c);
            if (id < 0) {
                return null;
            }
            String name = c.getColumnName(id);
            c.close();
            c = cr.query(uri, null, name + ">0", null, name + " DESC");
            assert c != null;
            if (c.moveToFirst()) {
                secondSimId = c.getString(id);
            }
            c.close();
            Log.d(TAG, "second sim id: ", uri, ": ", secondSimId);
            return secondSimId;
        } catch (SQLiteException e) {
            Log.w(TAG, "sim_id check for calls failed", e);
            return null;
        }
    }

    @NonNull
    private static ArrayList<String> getAllSimIdColumnNames(final Cursor c) {
        ArrayList<String> simIdColumns = new ArrayList<>();
        for (String name : SIM_ID_COLUMN_NAMES) {
            int id = c.getColumnIndex(name);
            if (id >= 0) {
                Log.d(TAG, "sim_id column found: ", name);
                simIdColumns.add(name);
            }
        }
        return simIdColumns;
    }

    private Cursor getSimIdsForColumn(final ContentResolver cr, final Uri uri, final String name) {
        return cr.query(uri, new String[]{name}, name + " != ?", new String[]{"-1"}, null);
    }

    private int getSimIdCountForColumn(final ContentResolver cr, final Uri uri, final String name) {
        final Cursor cursor = getSimIdsForColumn(cr, uri, name);
        if (cursor != null) {
            final int count = cursor.getCount();
            Log.d(TAG, "found ", count, " sim_id values for column ", name);
            cursor.close();
            return count;
        }
        Log.e(TAG, "error getting cursor on ", uri);
        return 0;
    }

    /**
     * Print out column names.
     */
    private void printColumnNames(final Cursor c) {
        Log.i(TAG, "table schema for cursor: ", c);
        int l = c.getColumnCount();
        Log.i(TAG, "column count: ", l);
        for (int i = 0; i < l; ++i) {
            Log.i(TAG, "column: ", c.getColumnName(i));
        }
    }
}
