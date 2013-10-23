package de.ub0r.android.callmeter.ui;

import android.app.Activity;

import de.ub0r.android.callmeter.TrackingUtils;

/**
 * @author flx
 */
public class TrackingActivity extends Activity {

    @Override
    public void onStart() {
        super.onStart();
        TrackingUtils.startTracking(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        TrackingUtils.stopTracking(this);
    }

}
