/*
 * Copyright (C) 2009-2013 Felix Bechstein
 * 
 * This file is part of Call Meter 3G.
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
package de.ub0r.android.callmeter.ui;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import net.lucode.hackware.magicindicator.MagicIndicator;
import net.lucode.hackware.magicindicator.ViewPagerHelper;
import net.lucode.hackware.magicindicator.buildins.commonnavigator.CommonNavigator;
import net.lucode.hackware.magicindicator.buildins.commonnavigator.abs.CommonNavigatorAdapter;
import net.lucode.hackware.magicindicator.buildins.commonnavigator.abs.IPagerIndicator;
import net.lucode.hackware.magicindicator.buildins.commonnavigator.abs.IPagerTitleView;
import net.lucode.hackware.magicindicator.buildins.commonnavigator.indicators.LinePagerIndicator;
import net.lucode.hackware.magicindicator.buildins.commonnavigator.titles.ColorTransitionPagerTitleView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

import de.ub0r.android.callmeter.CallMeter;
import de.ub0r.android.callmeter.R;
import de.ub0r.android.callmeter.data.DataProvider;
import de.ub0r.android.callmeter.data.LogRunnerReceiver;
import de.ub0r.android.callmeter.data.LogRunnerService;
import de.ub0r.android.callmeter.ui.prefs.Preferences;
import de.ub0r.android.lib.DonationHelper;
import de.ub0r.android.lib.Utils;
import de.ub0r.android.logg0r.Log;

/**
 * Callmeter's Main Activity
 *
 * @author flx
 */
public final class Plans extends AppCompatActivity implements OnPageChangeListener {

    /**
     * Tag for output.
     */
    private static final String TAG = "Plans";

    /**
     * {@link Message} for {@link Handler}: start background: LogMatcher.
     */
    public static final int MSG_BACKGROUND_START_MATCHER = 1;

    /**
     * {@link Message} for {@link Handler}: stop background: LogMatcher.
     */
    public static final int MSG_BACKGROUND_STOP_MATCHER = 2;

    /**
     * {@link Message} for {@link Handler}: start background: LogRunner.
     */
    public static final int MSG_BACKGROUND_START_RUNNER = 3;

    /**
     * {@link Message} for {@link Handler}: stop background: LogRunner.
     */
    public static final int MSG_BACKGROUND_STOP_RUNNER = 4;

    /**
     * {@link Message} for {@link Handler}: progress: LogMatcher.
     */
    public static final int MSG_BACKGROUND_PROGRESS_MATCHER = 5;

    private static final int PERMISSION_REQUEST_READ_CALL_LOG = 1;
    private static final int PERMISSION_REQUEST_READ_SMS = 2;
    private static final int PERMISSION_REQUEST_READ_CONTACTS = 3;

    /**
     * Delay for LogRunnerService to run.
     */
    private static final long DELAY_LOGRUNNER = 1500;

    /**
     * Display ads?
     */
    private static boolean prefsNoAds;

    /**
     * {@link ViewPager}.
     */
    private ViewPager pager;

    /**
     * {@link PlansFragmentAdapter}.
     */
    private PlansFragmentAdapter fadapter;

    private AdView mAdView;

    /**
     * {@link Handler} for handling messages from background process.
     */
    private final Handler handler = new Handler() {
        /** LogRunner running in background? */
        private boolean inProgressRunner = false;

        /** {@link ProgressDialog} showing LogMatcher's status. */
        private ProgressDialog statusMatcher = null;

        /** Is statusMatcher a {@link ProgressDialog}? */
        private boolean statusMatcherProgress = false;

        /** String for recalculate message. */
        private String recalc = null;

        @Override
        public synchronized void handleMessage(final Message msg) {
            Log.d(TAG, "handleMessage(", msg.what, ")");
            switch (msg.what) {
                case MSG_BACKGROUND_START_RUNNER:
                    inProgressRunner = true;
                case MSG_BACKGROUND_START_MATCHER:
                    statusMatcherProgress = false;
                    Plans.this.setInProgress(1);
                    break;
                case MSG_BACKGROUND_STOP_RUNNER:
                    inProgressRunner = false;
                    Plans.this.setInProgress(-1);
                    Plans.this.getSupportActionBar().setSubtitle(null);
                    break;
                case MSG_BACKGROUND_STOP_MATCHER:
                    Plans.this.setInProgress(-1);
                    Plans.this.getSupportActionBar().setSubtitle(null);
                    Fragment f = Plans.this.fadapter.getActiveFragment(Plans.this.pager,
                            Plans.this.fadapter.getHomeFragmentPos());
                    if (f != null && f instanceof PlansFragment) {
                        ((PlansFragment) f).requery(true);
                    }
                    break;
                case MSG_BACKGROUND_PROGRESS_MATCHER:
                    if (Plans.this.progressCount == 0) {
                        Plans.this.setProgress(1);
                    }
                    if (statusMatcher == null
                            || (!this.statusMatcherProgress || statusMatcher.isShowing())) {
                        Log.d(TAG, "matcher progress: ", msg.arg1);
                        if (statusMatcher == null || !this.statusMatcherProgress) {
                            final ProgressDialog dold = statusMatcher;
                            statusMatcher = new ProgressDialog(Plans.this);
                            statusMatcher.setCancelable(true);
                            if (recalc == null) {
                                recalc = Plans.this.getString(R.string.reset_data_progr2);
                            }
                            statusMatcher.setMessage(recalc);
                            statusMatcher.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                            statusMatcher.setMax(msg.arg2);
                            statusMatcher.setIndeterminate(false);
                            statusMatcherProgress = true;
                            Log.d(TAG, "showing dialog..");
                            try {
                                statusMatcher.show();
                                if (dold != null) {
                                    dold.dismiss();
                                }
                            } catch (Exception e) {
                                Log.w(TAG, "activity already finished?", e);
                            }
                        }
                        statusMatcher.setProgress(msg.arg1);
                    }
                    if (recalc == null) {
                        recalc = Plans.this.getString(R.string.reset_data_progr2);
                    }
                    Plans.this.getSupportActionBar().setSubtitle(
                            recalc + " " + msg.arg1 + "/" + msg.arg2);
                    break;
                default:
                    break;
            }

            if (inProgressRunner) {
                if (statusMatcher == null
                        || (msg.arg1 <= 0 && !this.statusMatcher.isShowing())) {
                    statusMatcher = new ProgressDialog(Plans.this);
                    statusMatcher.setCancelable(true);
                    statusMatcher.setMessage(Plans.this.getString(R.string.reset_data_progr1));
                    statusMatcher.setIndeterminate(true);
                    statusMatcherProgress = false;
                    try {
                        statusMatcher.show();
                    } catch (Exception e) {
                        Log.w(TAG, "activity already finished?", e);
                    }
                }
            } else {
                if (statusMatcher != null && statusMatcher.isShowing()) {
                    try {
                        statusMatcher.dismiss();
                    } catch (IllegalArgumentException e) {
                        Log.e(TAG, "error dismissing dialog", e);
                    }
                    statusMatcher = null;
                }
            }
        }
    };

    /**
     * Number of background tasks.
     */
    private int progressCount = 0;

    /**
     * {@link Handler} for outside.
     */
    private static Handler currentHandler = null;

    /**
     * Show all {@link PlansFragment}s.
     *
     * @author flx
     */
    private static class PlansFragmentAdapter extends FragmentPagerAdapter {

        /**
         * {@link FragmentManager} .
         */
        private final FragmentManager mFragmentManager;

        /**
         * List of positions.
         */
        private final Long[] positions;

        /**
         * List of bill days.
         */
        private final Long[] billDays;

        /**
         * List of titles.
         */
        private final String[] titles;

        /**
         * {@link Context}.
         */
        private final Context ctx;

        /**
         * Default constructor.
         *
         * @param context {@link Context}
         * @param fm      {@link FragmentManager}
         */
        public PlansFragmentAdapter(final Context context, final FragmentManager fm) {
            super(fm);
            mFragmentManager = fm;
            ctx = context;
            ContentResolver cr = context.getContentResolver();
            Cursor c = cr.query(DataProvider.Logs.CONTENT_URI,
                    new String[]{DataProvider.Logs.DATE}, null, null, DataProvider.Logs.DATE
                            + " ASC LIMIT 1");
            if (c == null || !c.moveToFirst()) {
                positions = new Long[]{-1L, -1L};
                billDays = positions;
                if (c != null && !c.isClosed()) {
                    c.close();
                }
            } else {
                final long minDate = c.getLong(0);
                c.close();
                c = cr.query(
                        DataProvider.Plans.CONTENT_URI,
                        DataProvider.Plans.PROJECTION,
                        DataProvider.Plans.TYPE + "=? and " + DataProvider.Plans.BILLPERIOD + "!=?",
                        new String[]{String.valueOf(DataProvider.TYPE_BILLPERIOD),
                                String.valueOf(DataProvider.BILLPERIOD_INFINITE)},
                        DataProvider.Plans.ORDER + " LIMIT 1");
                if (minDate < 0L || c == null || !c.moveToFirst()) {
                    positions = new Long[]{-1L, -1L};
                    billDays = positions;
                    if (c != null) {
                        c.close();
                    }
                } else {
                    ArrayList<Long> list = new ArrayList<>();
                    int bptype = c.getInt(DataProvider.Plans.INDEX_BILLPERIOD);
                    ArrayList<Long> bps = DataProvider.Plans.getBillDays(bptype,
                            c.getLong(DataProvider.Plans.INDEX_BILLDAY), minDate, -1);
                    if (bps != null) {
                        Log.d(TAG, "bill periods: ", bps.size());
                        if (!bps.isEmpty()) {
                            bps.remove(bps.size() - 1);
                            list.addAll(bps);
                        }
                    }
                    c.close();
                    list.add(-1L); // current time
                    list.add(-1L); // logs
                    positions = list.toArray(new Long[list.size()]);
                    int l = positions.length;
                    Arrays.sort(positions, 0, l - 2);

                    billDays = new Long[l];
                    for (int i = 0; i < l - 1; i++) {
                        long pos = positions[i];
                        billDays[i] = DataProvider.Plans.getBillDay(bptype, pos + 1L, pos,
                                false).getTimeInMillis();
                    }
                }
            }
            Common.setDateFormat(context);
            final int l = positions.length;
            titles = new String[l];
            titles[l - 2] = context.getString(R.string.now);
            titles[l - 1] = context.getString(R.string.logs);
        }

        /**
         * Get an active fragment.
         *
         * @param container {@link ViewPager}
         * @param position  position in container
         * @return null if no fragment was initialized
         */
        public Fragment getActiveFragment(final ViewPager container, final int position) {
            String name = makeFragmentName(container.getId(), position);
            return mFragmentManager.findFragmentByTag(name);
        }

        /**
         * Get a {@link Fragment}'s name.
         *
         * @param viewId container view
         * @param index  position
         * @return name of {@link Fragment}
         */
        private static String makeFragmentName(final int viewId, final int index) {
            // this might change in underlying method!
            return "android:switcher:" + viewId + ":" + index;
        }

        @Override
        public int getCount() {
            return positions.length;
        }

        @Override
        public Fragment getItem(final int position) {
            if (position == getLogsFragmentPos()) {
                return new LogsFragment();
            } else {
                return PlansFragment.newInstance(position, positions[position]);
            }
        }

        /**
         * Get position of home {@link Fragment}.
         *
         * @return position of home {@link Fragment}
         */
        public int getHomeFragmentPos() {
            return positions.length - 2;
        }

        /**
         * Get position of Logs {@link Fragment}.
         *
         * @return position of Logs {@link Fragment}
         */
        public int getLogsFragmentPos() {
            return positions.length - 1;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public CharSequence getPageTitle(final int position) {
            String ret;
            if (titles[position] == null) {
                ret = Common.formatDate(ctx, billDays[position]);
                titles[position] = ret;
            } else {
                ret = titles[position];
            }
            return ret;
        }
    }

    @Override
    public void onDestroy() {
        mAdView.destroy();
        super.onDestroy();
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setTheme(Preferences.getTheme(this));
        Utils.setLocale(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.plans);
        getSupportActionBar().setHomeButtonEnabled(true);

        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(this);
        //noinspection ConstantConditions
        if (p.getAll().isEmpty()) {
            // show intro
            startActivity(new Intent(this, IntroActivity.class));
            // set date of recordings to beginning of last month
            Calendar c = Calendar.getInstance();
            c.set(Calendar.DAY_OF_MONTH, 0);
            c.add(Calendar.MONTH, -1);
            Log.i(TAG, "set date of recording: " + c);
            p.edit().putLong(Preferences.PREFS_DATE_BEGIN, c.getTimeInMillis()).apply();
        }

        pager = (ViewPager) findViewById(R.id.pager);

        prefsNoAds = DonationHelper.hideAds(this);
        mAdView = (AdView) findViewById(R.id.ads);
        mAdView.setVisibility(View.GONE);
        if (!prefsNoAds) {
            mAdView.loadAd(new AdRequest.Builder().build());
            mAdView.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded() {
                    mAdView.setVisibility(View.VISIBLE);
                    super.onAdLoaded();
                }
            });
        } else {
            findViewById(R.id.cookieconsent).setVisibility(View.GONE);
        }

        initAdapter();
    }

    private void initAdapter() {
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
                PERMISSION_REQUEST_READ_SMS, R.string.permissions_read_sms,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                })) {
            return;
        }

        // request semi optional permission
        CallMeter.requestPermission(this, Manifest.permission.READ_CONTACTS,
                PERMISSION_REQUEST_READ_CONTACTS, R.string.permissions_read_contacts, null);

        fadapter = new PlansFragmentAdapter(this, getSupportFragmentManager());
        pager.setAdapter(fadapter);
        pager.setCurrentItem(fadapter.getHomeFragmentPos());
        pager.addOnPageChangeListener(this);

        MagicIndicator magicIndicator = findViewById(R.id.titles);
        CommonNavigator commonNavigator = new CommonNavigator(this);
        commonNavigator.setAdapter(new CommonNavigatorAdapter() {
            @Override
            public int getCount() {
                return fadapter.getCount();
            }

            @Override
            public IPagerTitleView getTitleView(final Context context, final int i) {
                ColorTransitionPagerTitleView colorTransitionPagerTitleView = new ColorTransitionPagerTitleView(context);

                TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{android.R.attr.textColorPrimary, android.R.attr.textColorSecondary});
                try {
                    colorTransitionPagerTitleView.setNormalColor(a.getColor(1, Color.GRAY));
                    colorTransitionPagerTitleView.setSelectedColor(a.getColor(0, Color.BLACK));
                } finally {
                    a.recycle();
                }
                colorTransitionPagerTitleView.setText(fadapter.getPageTitle(i));
                colorTransitionPagerTitleView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        pager.setCurrentItem(i);
                    }
                });
                return colorTransitionPagerTitleView;
            }

            @Override
            public IPagerIndicator getIndicator(final Context context) {
                LinePagerIndicator indicator = new LinePagerIndicator(context);
                indicator.setMode(LinePagerIndicator.MODE_MATCH_EDGE);
                return indicator;
            }
        });
        magicIndicator.setNavigator(commonNavigator);
        ViewPagerHelper.bind(magicIndicator, pager);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onResume() {
        super.onResume();
        Utils.setLocale(this);
        currentHandler = handler;
        setInProgress(0);
        PlansFragment.reloadPreferences(this);
        runLogRunner();
        mAdView.resume();
    }

    private void runLogRunner() {
        // schedule next update
        LogRunnerReceiver.schedNext(this, DELAY_LOGRUNNER, LogRunnerService.ACTION_RUN_MATCHER);
        LogRunnerReceiver.schedNext(this, LogRunnerService.ACTION_SHORT_RUN);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onNewIntent(final Intent intent) {
        setIntent(intent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onPause() {
        currentHandler = null;
        mAdView.pause();
        super.onPause();
    }

    /**
     * Get the current {@link Handler}.
     *
     * @return {@link Handler}.
     */
    public static Handler getHandler() {
        return currentHandler;
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
                    initAdapter();
                    runLogRunner();
                } else {
                    // this app is useless without permission for reading sms
                    Log.e(TAG, "permission denied: " + requestCode + " , exit");
                    finish();
                }
                return;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        if (prefsNoAds) {
            menu.removeItem(R.id.item_donate);
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_settings:
                startActivity(new Intent(this, Preferences.class));
                return true;
            case R.id.item_donate:
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(
                            "https://play.google.com/store/apps/details?id=de.ub0r.android.donator")));
                } catch (ActivityNotFoundException e) {
                    Log.e(TAG, "error opening play store with donation app", e);
                    Toast.makeText(this, R.string.common_google_play_services_unknown_issue,
                            Toast.LENGTH_LONG).show();
                }
                return true;
            case R.id.item_logs:
                showLogsFragment(-1L);
                return true;
            case android.R.id.home:
                pager.setCurrentItem(fadapter.getHomeFragmentPos(), true);
                Fragment f = fadapter.getActiveFragment(pager,
                        fadapter.getLogsFragmentPos());
                if (f != null && f instanceof LogsFragment) {
                    ((LogsFragment) f).setPlanId(-1L);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void showLogsFragment(final long planId) {
        int p = fadapter.getLogsFragmentPos();
        Fragment f = fadapter.getActiveFragment(pager, p);
        if (f != null && f instanceof LogsFragment) {
            ((LogsFragment) f).setPlanId(planId);
        }
        pager.setCurrentItem(p, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPageScrolled(final int position, final float positionOffset,
                               final int positionOffsetPixels) {
        // nothing to do

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPageSelected(final int position) {
        Log.d(TAG, "onPageSelected(", position, ")");
        if (position == fadapter.getLogsFragmentPos()) {
            Fragment f = fadapter.getActiveFragment(pager,
                    fadapter.getLogsFragmentPos());
            if (f != null && f instanceof LogsFragment) {
                ((LogsFragment) f).setAdapter(false);
            }
        } else {
            Fragment f = fadapter.getActiveFragment(pager, position);
            if (f != null && f instanceof PlansFragment) {
                ((PlansFragment) f).requery(false);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPageScrollStateChanged(final int state) {
        // nothing to do
    }

    /**
     * Set progress indicator.
     *
     * @param add add number of running tasks
     */
    public synchronized void setInProgress(final int add) {
        Log.d(TAG, "setInProgress(", add, ")");
        progressCount += add;

        if (progressCount < 0) {
            Log.w(TAG, "this.progressCount: " + progressCount);
            progressCount = 0;
        }

        Log.d(TAG, "progressCount: ", progressCount);
    }
}
