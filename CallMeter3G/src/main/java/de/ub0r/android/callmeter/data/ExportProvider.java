package de.ub0r.android.callmeter.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;

import de.ub0r.android.logg0r.Log;

public class ExportProvider extends ContentProvider {

    /** Tag for output. */
    private static final String TAG = "ExportProvider";

    /** Authority. */
    public static final String AUTHORITY = DataProvider.PACKAGE + ".export";

    /** Mime type for export. */
    public static final String EXPORT_MIMETYPE = "application/android.callmeter.export";
    /** {@link Uri} for export Content. */
    public static final Uri EXPORT_RULESET_URI = Uri.parse("content://" + AUTHORITY + "/ruleset");
    /** {@link Uri} for export Content. */
    public static final Uri EXPORT_LOGS_URI = Uri.parse("content://" + AUTHORITY + "/logs");
    /** {@link Uri} for export Content. */
    public static final Uri EXPORT_NUMGROUPS_URI = Uri.parse("content://" + AUTHORITY
            + "/numgroups");
    /** {@link Uri} for export Content. */
    public static final Uri EXPORT_HOURGROUPS_URI = Uri.parse("content://" + AUTHORITY
            + "/hourgroups");
    /** Filename for the actual export file. */
    public static final String EXPORT_RULESET_FILE = "ruleset.xml";
    /** Filename for the actual logs file. */
    public static final String EXPORT_LOGS_FILE = "logs.xml";
    /** Filename for the actual number groups file. */
    public static final String EXPORT_NUMGROUPS_FILE = "numgroups.xml";
    /** Filename for the actual hour groups file. */
    public static final String EXPORT_HOURGROUPS_FILE = "hourgroups.xml";

    /** {@link UriMatcher}. */
    private static final UriMatcher URI_MATCHER;
    /** Internal id: export. */
    private static final int EXPORT_RULESET = 200;
    /** Internal id: export. */
    private static final int EXPORT_LOGS = 201;
    /** Internal id: export. */
    private static final int EXPORT_NUMGROUPS = 202;
    /** Internal id: export. */
    private static final int EXPORT_HOURGROUPS = 203;

    static {
        URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
        URI_MATCHER.addURI(AUTHORITY, "ruleset", EXPORT_RULESET);
        URI_MATCHER.addURI(AUTHORITY, "logs", EXPORT_LOGS);
        URI_MATCHER.addURI(AUTHORITY, "numgroups", EXPORT_NUMGROUPS);
        URI_MATCHER.addURI(AUTHORITY, "hourgroups", EXPORT_HOURGROUPS);
    }

    @Override
    public int delete(final Uri uri, final String selection, final String[] selectionArgs) {
        throw new IllegalStateException("Unsupported operation: delete(" + uri + ")");
    }

    @Override
    public String getType(final Uri uri) {
        switch (URI_MATCHER.match(uri)) {
            case EXPORT_RULESET:
            case EXPORT_LOGS:
            case EXPORT_NUMGROUPS:
            case EXPORT_HOURGROUPS:
                return EXPORT_MIMETYPE;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @Override
    public Uri insert(final Uri uri, final ContentValues values) {
        throw new IllegalStateException("Unsupported operation: insert(" + uri + ")");
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(final Uri uri, final String[] projection, final String selection,
            final String[] selectionArgs, final String sortOrder) {
        Log.d(TAG, "export proj:, ", projection);
        String fn;
        final int uid = URI_MATCHER.match(uri);

        switch (uid) {
            case EXPORT_RULESET:
                fn = EXPORT_RULESET_FILE;
                break;
            case EXPORT_LOGS:
                fn = EXPORT_LOGS_FILE;
                break;
            case EXPORT_NUMGROUPS:
                fn = EXPORT_NUMGROUPS_FILE;
                break;
            case EXPORT_HOURGROUPS:
                fn = EXPORT_HOURGROUPS_FILE;
                break;
            default:
                throw new IllegalArgumentException("Unknown Uri " + uri);
        }

        final int l = projection.length;
        Object[] retArray = new Object[l];
        for (int i = 0; i < l; i++) {
            if (projection[i].equals(OpenableColumns.DISPLAY_NAME)) {
                retArray[i] = fn;
            } else if (projection[i].equals(OpenableColumns.SIZE)) {
                final File d = Environment.getExternalStorageDirectory();
                final File f = new File(d, DataProvider.PACKAGE + File.separator + fn);
                retArray[i] = f.length();
            }
        }
        final MatrixCursor ret = new MatrixCursor(projection, 1);
        ret.addRow(retArray);
        Log.d(TAG, "query(): ", ret.getCount());
        return ret;
    }

    @Override
    public int update(final Uri uri, final ContentValues values, final String selection,
            final String[] selectionArgs) {
        throw new IllegalStateException("Unsupported operation: update(" + uri + ")");
    }

    @Override
    public ParcelFileDescriptor openFile(final Uri uri, final String mode)
            throws FileNotFoundException {
        Log.d(TAG, "openFile(", uri.toString(), ")");
        final File d = Environment.getExternalStorageDirectory();
        String fn = null;
        if (uri.equals(EXPORT_RULESET_URI)) {
            fn = EXPORT_RULESET_FILE;
        } else if (uri.equals(EXPORT_LOGS_URI)) {
            fn = EXPORT_LOGS_FILE;
        } else if (uri.equals(EXPORT_NUMGROUPS_URI)) {
            fn = EXPORT_NUMGROUPS_FILE;
        } else if (uri.equals(EXPORT_HOURGROUPS_URI)) {
            fn = EXPORT_HOURGROUPS_FILE;
        }
        if (fn == null) {
            return null;
        }
        final File f = new File(d, DataProvider.PACKAGE + File.separator + fn);
        return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY);
    }
}
