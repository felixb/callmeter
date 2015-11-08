/*
 * Copyright (C) 2009-2015 Felix Bechstein
 * 
 * This file is part of Call Meter NG.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; If not, see <http://www.gnu.org/licenses/>.
 */
package de.ub0r.de.android.callMeterNG;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import de.ub0r.android.lib.DonationHelper;
import de.ub0r.android.lib.Utils;
import de.ub0r.android.logg0r.Log;

/**
 * The main Activity, holding all data.
 *
 * @author flx
 */
public class CallMeter extends Activity {

    private static final String TAG = "CallMeter";

    /**
     * Tag for output.
     */
    public static final String DONATION_URL
            = "https://play.google.com/store/apps/details?id=de.ub0r.android.donator";

    private static final int PERMISSION_REQUEST_READ_CALL_LOG = 1;
    private static final int PERMISSION_REQUEST_READ_SMS = 2;

    /**
     * 100.
     */
    static final int HUNDRED = 100;

    /**
     * Preference's name: show short title for data.
     */
    private static final String PREFS_DATA_SHORT = "data_short";

    /**
     * Display ads?
     */
    private static boolean prefsNoAds;

    private AdView mAdView;

    /**
     * {@inheritDoc}
     */
    @Override
    public final void onCreate(final Bundle savedInstanceState) {
        this.setTheme(Preferences.getTheme(this));
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        Utils.setLocale(this);
        this.setContentView(R.layout.main);

        prefsNoAds = DonationHelper.hideAds(this);

        TextView tv = (TextView) this.findViewById(R.id.calls_);
        Preferences.textSizeMedium = tv.getTextSize();
        tv = (TextView) this.findViewById(R.id.calls1_in_);
        Preferences.textSizeSmall = tv.getTextSize();

        mAdView = (AdView) findViewById(R.id.ad);
        mAdView.setVisibility(View.GONE);
    }

    @Override
    protected final void onPause() {
        mAdView.pause();
        super.onPause();
    }

    @Override
    protected final void onResume() {
        super.onResume();
        mAdView.resume();
        // get call/sms stats
        startUpdater();
        // get data stats
        new UpdaterData(this).execute((Void[]) null);
        // schedule next update
        CMBroadcastReceiver.schedNext(this);
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
                PREFS_DATA_SHORT, false)) {
            ((TextView) this.findViewById(R.id.data_)).setText(R.string.data);
        } else {
            ((TextView) this.findViewById(R.id.data_)).setText(R.string.data_);
        }

        if (!prefsNoAds) {
            mAdView.loadAd(new AdRequest.Builder().build());
            mAdView.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded() {
                    mAdView.setVisibility(View.VISIBLE);
                    super.onAdLoaded();
                }
            });
        }
    }

    private void startUpdater() {
        // request permissions before doing any real work
        if (!CallMeter.requestPermission(this, Manifest.permission.READ_CALL_LOG,
                PERMISSION_REQUEST_READ_CALL_LOG, R.string.permissions_read_call_log,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                })) {
            return;
        }
        if (!CallMeter.requestPermission(this, Manifest.permission.READ_SMS,
                PERMISSION_REQUEST_READ_CALL_LOG, R.string.permissions_read_sms,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                })) {
            return;
        }

        Updater.startUpdater(this);
    }

    @Override
    public void onRequestPermissionsResult(
            final int requestCode,
            @NonNull final String permissions[],
            @NonNull final int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_READ_CALL_LOG:
            case PERMISSION_REQUEST_READ_SMS:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // just try again.
                    startUpdater();
                } else {
                    // this app is useless without permission for reading sms
                    Log.e(TAG, "permission denied: " + requestCode + " , exit");
                    finish();
                }
                return;
        }
    }

    @Override
    protected final void onDestroy() {
        mAdView.destroy();
        super.onDestroy();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean onCreateOptionsMenu(final Menu menu) {
        MenuInflater inflater = this.getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        if (prefsNoAds) {
            menu.removeItem(R.id.item_donate);
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_settings:
                this.startActivity(new Intent(this, Preferences.class));
                return true;
            case R.id.item_donate:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(DONATION_URL)));
                return true;
            default:
                return false;
        }
    }

    public static boolean hasPermission(final Context context, final String permission) {
        return ContextCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasPermissions(final Context context, final String... permissions) {
        for (String p : permissions) {
            if (!hasPermission(context, p)) {
                return  false;
            }
        }
        return true;
    }

    public static boolean requestPermission(final Activity activity, final String permission,
            final int requestCode, final int message,
            final DialogInterface.OnClickListener onCancelListener) {
        Log.i(TAG, "requesting permission: " + permission);
        if (!hasPermission(activity, permission)) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                new AlertDialog.Builder(activity)
                        .setTitle(R.string.permissions_)
                        .setMessage(message)
                        .setCancelable(false)
                        .setNegativeButton(android.R.string.cancel, onCancelListener)
                        .setPositiveButton(android.R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(final DialogInterface dialogInterface,
                                            final int i) {
                                        ActivityCompat.requestPermissions(activity,
                                                new String[]{permission}, requestCode);
                                    }
                                })
                        .show();
            } else {
                ActivityCompat.requestPermissions(activity, new String[]{permission}, requestCode);
            }
            return false;
        } else {
            return true;
        }
    }
}
