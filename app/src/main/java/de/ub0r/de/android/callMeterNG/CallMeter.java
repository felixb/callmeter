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

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import de.ub0r.android.lib.DonationHelper;
import de.ub0r.android.lib.Utils;

/**
 * The main Activity, holding all data.
 *
 * @author flx
 */
public class CallMeter extends Activity {

    /**
     * Tag for output.
     */
    public static final String DONATION_URL
            = "https://play.google.com/store/apps/details?id=de.ub0r.android.donator";

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
        new Updater(this).execute((Void[]) null);
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
}
