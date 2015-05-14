package de.ub0r.android.callmeter;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Fields;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.analytics.tracking.android.Tracker;

import android.app.Activity;
import android.content.Context;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;

import de.ub0r.android.logg0r.Log;

/**
 * @author flx
 */
public class TrackingUtils {

    private static final String TAG = "TrackingUtils";

    private static int sEnableAnalytics = -1;

    private static String sTrackingId;

    private TrackingUtils() {
        // hide constructor
    }

    public static void startTracking(final Activity context) {
        if (context == null) {
            return;
        }
        if (sEnableAnalytics < 0) {
            sEnableAnalytics = PreferenceManager.getDefaultSharedPreferences(context)
                    .getBoolean("enable_analytics", true) ? 1 : 0;
        }
        if (sEnableAnalytics == 1) {
            EasyTracker tr = EasyTracker.getInstance(context);
            tr.activityStart(context);
        }
    }

    public static void stopTracking(final Activity context) {
        if (context == null) {
            return;
        }
        if (sEnableAnalytics == 1) {
            EasyTracker.getInstance(context).activityStop(context);
        }
    }


    public static void setDimension(final Context context, final int id, final int value) {
        setDimension(context, id, String.valueOf(value));
    }

    public static void setDimension(final Context context, final int id, final String value) {
        if (sEnableAnalytics != 1) {
            return;
        }
        Tracker tr = getTracker(context);
        if (tr == null) {
            return;
        }
        tr.set(Fields.customDimension(id), value);
    }

    public static void sendView(final Fragment fragment) {
        if (fragment == null) {
            Log.w(TAG, "fragment=null");
            return;
        }
        sendView(fragment, fragment.getClass().getName());
    }

    public static void sendView(final Object context, final String view) {
        Tracker tr = getTracker(context);
        if (tr == null) {
            return;
        }
        tr.send(MapBuilder
                        .createAppView()
                        .set(Fields.SCREEN_NAME, view)
                        .build()
        );
    }

    public static void clearView(final Object context) {
        Tracker tr = getTracker(context);
        if (tr == null) {
            return;
        }
        tr.set(Fields.SCREEN_NAME, null);
    }

    public static void sendEvent(final Object context, final String category, final String action,
            final String label, final Long value) {
        Tracker tr = getTracker(context);
        if (tr == null) {
            return;
        }
        tr.send(MapBuilder.createEvent(category, action, getView(context, label), value).build());
    }

    public static void sendMenu(final Object context, final String label) {
        sendEvent(context, "UX", "menu", label, null);
    }

    public static void sendClick(final Object context, final String label, final Long id) {
        sendEvent(context, "UX", "click", label, id);
    }

    public static void sendLongClick(final Object context, final String label, final Long id) {
        sendEvent(context, "UX", "longClick", label, id);
    }

    private static Tracker getTracker(final Object context) {
        if (context == null) {
            Log.w(TAG, "context=null");
            return null;
        }
        if (context instanceof Tracker) {
            return (Tracker) context;
        } else if (context instanceof Context) {
            return getTrackerFromContext((Context) context);
        } else if (context instanceof Fragment) {
            Activity a = ((Fragment) context).getActivity();
            if (a == null) {
                Log.w(TAG, "fragment's activity=null");
                return null;
            } else {
                return getTrackerFromContext(a);
            }
        } else {
            Log.w(TAG, "invalid context: " + context);
            return null;
        }
    }

    private static String getView(final Object context, final String view) {
        if (context == null) {
            Log.w(TAG, "context=null");
            return view;
        }
        if (context instanceof Tracker) {
            return view;
        } else if (context instanceof Context) {
            return getViewFromContext((Context) context, view);
        } else if (context instanceof Fragment) {
            Activity a = ((Fragment) context).getActivity();
            if (a == null) {
                Log.w(TAG, "fragment.activity=null");
                return view;
            } else {
                return getViewFromContext(a, view);
            }
        } else {
            Log.w(TAG, "invalid context: " + context);
            return view;
        }
    }

    private static Tracker getTrackerFromContext(final Context context) {
        if (sEnableAnalytics < 0) {
            sEnableAnalytics = PreferenceManager.getDefaultSharedPreferences(context)
                    .getBoolean("enable_analytics", true) ? 1 : 0;
        }
        if (sEnableAnalytics == 1) {
            GoogleAnalytics ga = GoogleAnalytics.getInstance(context);
            Tracker tr = ga.getDefaultTracker();
            if (tr == null) {
                if (sTrackingId == null) {
                    sTrackingId = context.getString(R.string.ga_trackingId);
                }
                tr = ga.getTracker(sTrackingId);
            }
            return tr;
        } else {
            return null;
        }
    }

    private static String getViewFromContext(final Context context, final String view) {
        if (view.contains("/")) {
            return view;
        } else {
            return context.getClass().getName() + "/" + view;
        }
    }
}
