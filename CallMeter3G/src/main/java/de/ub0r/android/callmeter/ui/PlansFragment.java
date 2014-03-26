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

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.ResourceCursorAdapter;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.UnknownFormatConversionException;

import de.ub0r.android.callmeter.CallMeter;
import de.ub0r.android.callmeter.R;
import de.ub0r.android.callmeter.TrackingUtils;
import de.ub0r.android.callmeter.data.DataProvider;
import de.ub0r.android.callmeter.ui.prefs.PlanEdit;
import de.ub0r.android.callmeter.ui.prefs.Preferences;
import de.ub0r.android.logg0r.Log;

/**
 * Show plans.
 *
 * @author flx
 */
public final class PlansFragment extends SherlockListFragment implements OnClickListener,
        OnItemLongClickListener, LoaderCallbacks<Cursor> {

    /** Tag for output. */
    private static final String TAG = "PlansFragment";
    /** Run the dummy? */
    private static boolean doDummy = true;
    /** Show today stats. */
    private static boolean showToday = false;
    /** Show total stats. */
    private static boolean showTotal = true;
    /** Hide zero plans. */
    private static boolean hideZero = false;
    /** Hide no cost plans. */
    private static boolean hideNoCost = false;
    /** Ignore query requests. */
    private boolean ignoreQuery = false;

    /** Unique id for dummy loader. */
    private static final int UID_DUMMY = -3;

    /**
     * Adapter binding plans to View.
     *
     * @author flx
     */
    private static class PlansAdapter extends ResourceCursorAdapter {

        /**
         * View holder.
         *
         * @author flx
         */
        private class ViewHolder {

            /** {@link View}s. */
            View vPeriodLayout, vContent, vSpacer;
            /** {@link TextView}s. */
            TextView tvBigtitle, tvPeriod, tvBilldayLable, tvTitle, tvData;
            /** {@link ProgressBar}s. */
            ProgressBar pbPeriod, pbLimitGreen, pbLimitYellow, pbLimitRed;
        }

        /** {@link SharedPreferences}. */
        private final SharedPreferences p;
        /** {@link Editor}. */
        private final Editor e;
        /** Does the {@link Editor} needs commit? */
        private boolean isDirty = false;

        /** Now. */
        private final long now;

        /** Text sizes. */
        private static int textSize, textSizeBigTitle, textSizeTitle, textSizeSpacer, textSizePBar,
                textSizePBarBP;

        /** Separator for the data. */
        private static String delimiter = " | ";
        /** Selected currency format. */
        private static String currencyFormat = "$%.2f";
        /** Show hours and days. */
        private static boolean pShowHours = true;
        /** Show target bill day. */
        private static boolean pShowTargetBillDay = false;
        /** First/last bill day shown. */
        private static int billDayResId = R.string.billday_;

        /** Prepaid plan? */
        private static boolean prepaid;

        /** Visibility for {@link ProgressBar}s. */
        private final int progressBarVisability;

        /** Need a reload of preferences. */
        private static boolean needReloadPrefs = true;

        /**
         * Reload preferences.
         *
         * @param context {@link Context}
         * @param force   force reloading
         */
        static void reloadPreferences(final Context context, final boolean force) {
            if (!force && !needReloadPrefs) {
                return;
            }
            Common.setDateFormat(context);
            final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
            pShowHours = p.getBoolean(Preferences.PREFS_SHOWHOURS, true);
            pShowTargetBillDay = p.getBoolean(Preferences.PREFS_SHOW_TARGET_BILLDAY, false);
            billDayResId = pShowTargetBillDay ? R.string.billday_last : R.string.billday_;
            currencyFormat = Preferences.getCurrencyFormat(context);
            delimiter = p.getString(Preferences.PREFS_DELIMITER, " | ");
            prepaid = p.getBoolean(Preferences.PREFS_PREPAID, false);

            textSize = Preferences.getTextsize(context);
            textSizeBigTitle = Preferences.getTextsizeBigTitle(context);
            textSizeTitle = Preferences.getTextsizeTitle(context);
            textSizeSpacer = Preferences.getTextsizeSpacer(context);
            textSizePBar = Preferences.getTextsizeProgressBar(context);
            textSizePBarBP = Preferences.getTextsizeProgressBarBP(context);
        }

        /**
         * Default Constructor.
         *
         * @param context {@link Activity}
         * @param n       now
         */
        public PlansAdapter(final Activity context, final long n) {
            super(context, R.layout.plans_item, null, true);
            now = n;
            p = PreferenceManager.getDefaultSharedPreferences(context);
            e = p.edit();
            if (p.getBoolean(Preferences.PREFS_HIDE_PROGRESSBARS, false)) {
                progressBarVisability = View.GONE;
            } else {
                progressBarVisability = View.VISIBLE;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void bindView(final View view, final Context context, final Cursor cursor) {
            ViewHolder holder = (ViewHolder) view.getTag();
            if (holder == null) {
                holder = new ViewHolder();
                holder.pbPeriod = (ProgressBar) view.findViewById(R.id.period_pb);
                holder.pbLimitGreen = (ProgressBar) view.findViewById(R.id.progressbarLimitGreen);
                holder.pbLimitYellow = (ProgressBar) view.findViewById(R.id.progressbarLimitYellow);
                holder.pbLimitRed = (ProgressBar) view.findViewById(R.id.progressbarLimitRed);
                holder.vPeriodLayout = view.findViewById(R.id.period_layout);
                holder.vContent = view.findViewById(R.id.content);
                holder.vSpacer = view.findViewById(R.id.spacer);
                holder.tvBigtitle = (TextView) view.findViewById(R.id.bigtitle);
                holder.tvPeriod = (TextView) view.findViewById(R.id.period);
                holder.tvBilldayLable = (TextView) view.findViewById(R.id.billday_lable);
                holder.tvTitle = (TextView) view.findViewById(R.id.normtitle);
                holder.tvData = (TextView) view.findViewById(R.id.data);
                view.setTag(holder);
            }

            boolean savePlan = false;
            DataProvider.Plans.Plan plan = null;
            if (cursor.getColumnIndex(DataProvider.Plans.SUM_COST) > 0) {
                plan = new DataProvider.Plans.Plan(cursor);
                savePlan = true;
            } else {
                plan = new DataProvider.Plans.Plan(cursor, p);
            }

            SpannableStringBuilder spb = new SpannableStringBuilder();
            float cost;
            float free;
            if (prepaid) {
                cost = plan.getAccumCostPrepaid();
                free = 0;
            } else {
                cost = plan.getAccumCost();
                free = plan.getFree();
            }

            if (plan.type != DataProvider.TYPE_SPACING && plan.type != DataProvider.TYPE_TITLE) {
                if (plan.hasBa) {
                    long bd = plan.getBillDay(plan.type == DataProvider.TYPE_BILLPERIOD
                            && pShowTargetBillDay);
                    spb.append(Common.formatValues(context, plan.now, plan.type, plan.bpCount,
                            plan.bpBa, plan.billperiod, bd, pShowHours));
                    spb.setSpan(new StyleSpan(Typeface.BOLD), 0, spb.length(),
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    if (plan.type != DataProvider.TYPE_BILLPERIOD) {
                        if (showTotal) {
                            spb.append(delimiter
                                    + Common.formatValues(context, plan.now, plan.type,
                                    plan.atCount, plan.atBa, plan.billperiod, plan.billday,
                                    pShowHours));
                        }
                        if (showToday) {
                            spb.insert(
                                    0,
                                    Common.formatValues(context, plan.now, plan.type, plan.tdCount,
                                            plan.tdBa, plan.billperiod, plan.billday, pShowHours)
                                            + delimiter);
                        }
                    }
                }
                if (free > 0f || cost > 0f) {
                    if (spb.length() > 0) {
                        spb.append("\n");
                    }
                    if (free > 0f) {
                        String s;
                        try {
                            s = String.format(currencyFormat, free);
                        } catch (UnknownFormatConversionException ex) {
                            Log.e(TAG, "unknown format error with format '" + currencyFormat
                                    + "' and free=" + free, ex);
                            s = "$";
                        }
                        spb.append("(" + s + ")");
                    }
                    if (cost > 0f) {
                        String s;
                        try {
                            s = String.format(currencyFormat, cost);
                        } catch (UnknownFormatConversionException ex) {
                            Log.e(TAG, "unknown format error with format '" + currencyFormat
                                    + "' and cost=" + cost, ex);
                            s = "$";
                        }
                        spb.append(" " + s);
                    }
                }
                if (plan.limit > 0) {
                    spb.insert(0, ((int) (plan.usage * CallMeter.HUNDRET)) + "%" + delimiter);
                }
            }

            Log.v(TAG, "plan id: ", plan.id);
            Log.v(TAG, "plan name: ", plan.name);
            Log.v(TAG, "type: ", plan.type);
            Log.v(TAG, "cost: ", cost);
            Log.v(TAG, "limit: ", plan.limit);
            Log.v(TAG, "limitPos: ", plan.limitPos);
            Log.v(TAG, "text: ", spb);

            TextView tvCache = null;
            ProgressBar pbCache = null;
            if (plan.type == DataProvider.TYPE_SPACING) {
                if (textSizeSpacer > 0) {
                    final LayoutParams lp = holder.vSpacer.getLayoutParams();
                    lp.height = textSizeSpacer;
                    holder.vSpacer.setLayoutParams(lp);
                }
                holder.vSpacer.setVisibility(View.INVISIBLE);
                holder.tvBigtitle.setVisibility(View.GONE);
                holder.vContent.setVisibility(View.GONE);
                holder.vPeriodLayout.setVisibility(View.GONE);
            } else if (plan.type == DataProvider.TYPE_TITLE) {
                holder.tvBigtitle.setText(cursor.getString(DataProvider.Plans.INDEX_NAME));
                if (textSizeBigTitle > 0) {
                    holder.tvBigtitle.setTextSize(textSizeBigTitle);
                }
                holder.tvBigtitle.setVisibility(View.VISIBLE);
                holder.vSpacer.setVisibility(View.GONE);
                holder.vContent.setVisibility(View.GONE);
                holder.vPeriodLayout.setVisibility(View.GONE);
            } else if (plan.type == DataProvider.TYPE_BILLPERIOD) {
                holder.tvBigtitle.setVisibility(View.GONE);
                holder.vSpacer.setVisibility(View.GONE);
                holder.vContent.setVisibility(View.GONE);
                holder.vPeriodLayout.setVisibility(View.VISIBLE);
                holder.tvBilldayLable.setText(billDayResId);
                tvCache = holder.tvPeriod;
                pbCache = holder.pbPeriod;
            } else {
                holder.tvBigtitle.setVisibility(View.GONE);
                holder.vSpacer.setVisibility(View.GONE);
                holder.vPeriodLayout.setVisibility(View.GONE);
                holder.vContent.setVisibility(View.VISIBLE);
                if (textSizeTitle > 0) {
                    holder.tvTitle.setTextSize(textSizeTitle);
                }
                holder.tvTitle.setText(cursor.getString(DataProvider.Plans.INDEX_NAME));
                tvCache = holder.tvData;
                if (plan.limit > 0) {
                    float bpos = plan.getBillPlanUsage();
                    if (plan.usage >= 1) {
                        pbCache = holder.pbLimitRed;
                        holder.pbLimitGreen.setVisibility(View.GONE);
                        holder.pbLimitYellow.setVisibility(View.GONE);
                    } else if (bpos >= 0f && plan.usage > bpos) {
                        pbCache = holder.pbLimitYellow;
                        holder.pbLimitGreen.setVisibility(View.GONE);
                        holder.pbLimitRed.setVisibility(View.GONE);
                    } else {
                        pbCache = holder.pbLimitGreen;
                        holder.pbLimitYellow.setVisibility(View.GONE);
                        holder.pbLimitRed.setVisibility(View.GONE);
                    }
                } else {
                    pbCache = holder.pbLimitYellow;
                    holder.pbLimitGreen.setVisibility(View.GONE);
                    holder.pbLimitRed.setVisibility(View.GONE);
                }
            }
            if (tvCache != null && pbCache != null) {
                if (spb.length() > 0) {
                    tvCache.setText(spb);
                } else {
                    tvCache.setText(null);
                }
                if (textSize > 0) {
                    tvCache.setTextSize(textSize);
                }
                if (plan.limit == 0) {
                    pbCache.setVisibility(View.GONE);
                } else if (plan.limit > 0) {
                    pbCache.setIndeterminate(false);
                    pbCache.setMax((int) plan.limit);
                    pbCache.setProgress((int) plan.limitPos);
                    pbCache.setVisibility(progressBarVisability);
                    int pbs = 0;
                    if (plan.type == DataProvider.TYPE_BILLPERIOD) {
                        pbs = textSizePBarBP;
                    } else {
                        pbs = textSizePBar;
                    }
                    if (pbs > 0) {
                        final LayoutParams lp = pbCache.getLayoutParams();
                        lp.height = pbs;
                        pbCache.setLayoutParams(lp);
                    }
                } else {
                    pbCache.setIndeterminate(true);
                    pbCache.setVisibility(progressBarVisability);
                }
            }
            if (savePlan && now < 0L && plan.type != DataProvider.TYPE_SPACING
                    && plan.type != DataProvider.TYPE_TITLE) {
                plan.save(e);
                isDirty = true;
            }
        }

        /**
         * Save current stats to {@link SharedPreferences}.
         */
        public void save() {
            if (isDirty) {
                Log.d(TAG, "e.commit()");
                e.commit();
                isDirty = false;
            }
        }
    }

    /** This fragments time stamp. */
    private long now;
    /** Unique id of this fragment. */
    private int uid;
    /** Is loader running? */
    private boolean inProgress;

    /** Handle for view. */
    private View vLoading, vImport;

    /**
     * Get new {@link PlansFragment}.
     *
     * @param uid unique id for this fragment
     * @param now This fragments current time
     * @return {@link PlansFragment}
     */
    public static PlansFragment newInstance(final int uid, final long now) {
        PlansFragment f = new PlansFragment();
        Bundle args = new Bundle();
        args.putLong("now", now);
        args.putInt("uid", uid);
        f.setArguments(args);
        return f;
    }

    /**
     * Force reloading preferences.
     *
     * @param context {@link Context}
     */
    static void reloadPreferences(final Context context) {
        PlansAdapter.reloadPreferences(context, true);
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        showToday = p.getBoolean(Preferences.PREFS_SHOWTODAY, false);
        showTotal = p.getBoolean(Preferences.PREFS_SHOWTOTAL, false);
        hideZero = p.getBoolean(Preferences.PREFS_HIDE_ZERO, false);
        hideNoCost = p.getBoolean(Preferences.PREFS_HIDE_NOCOST, false);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        Bundle args = getArguments();
        if (args == null) {
            now = -1L;
            uid = -1;
        } else {
            now = args.getLong("now", -1L);
            uid = args.getInt("uid", -1);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPause() {
        super.onPause();
        ((PlansAdapter) getListAdapter()).save();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.plans_fragment, container, false);
        vLoading = v.findViewById(R.id.loading);
        vImport = v.findViewById(R.id.import_default);
        vImport.setOnClickListener(this);
        return v;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        PlansAdapter adapter = new PlansAdapter(getActivity(), now);
        setListAdapter(adapter);
        getListView().setOnItemLongClickListener(this);

        LoaderManager lm = getLoaderManager();
        if (lm.getLoader(uid) != null) {
            getLoaderManager().initLoader(uid, null, this);
        } else if (doDummy && now < 0L) {
            doDummy = false;
            getLoaderManager().initLoader(UID_DUMMY, null, this);
        }
        TrackingUtils.sendView(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStop() {
        super.onStop();
        if (now < 0L) {
            doDummy = true;
        }
    }

    /**
     * Set progress indicator.
     *
     * @param add add number of running tasks
     */
    private synchronized void setInProgress(final int add) {
        Log.d(TAG, "setInProgress(", add, ")");
        if (add == 0) {
            ((Plans) getActivity()).setInProgress(add);
        } else if (add > 0 && !this.inProgress) {
            ((Plans) getActivity()).setInProgress(add);
            inProgress = true;
        } else if (add < 0) {
            ((Plans) getActivity()).setInProgress(add);
            inProgress = false;
        }
    }

    /**
     * Re-query database.
     *
     * @param forceUpdate force update
     */
    public void requery(final boolean forceUpdate) {
        Log.d(TAG, "requery(", forceUpdate, ")");
        if (!this.ignoreQuery) {
            LoaderManager lm = getLoaderManager();
            if (forceUpdate && lm.getLoader(uid) != null) {
                lm.restartLoader(uid, null, this);
            } else {
                lm.initLoader(uid, null, this);
            }
        } else {
            Log.d(TAG, "requery(", forceUpdate, "): ignore");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        inflater.inflate(R.menu.menu_plans, menu);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onClick(final View v) {
        switch (v.getId()) {
            case R.id.import_default:
                TrackingUtils.sendLongClick(this, "import_default", null);
                final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(this
                        .getString(R.string.url_rulesets)));
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Log.e(TAG, "no activity to load url", e);
                    Toast.makeText(getActivity(),
                            "no activity to load url: " + intent.getDataString(), Toast.LENGTH_LONG)
                            .show();
                }
                break;
            default:
                break;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onItemLongClick(final AdapterView<?> parent, final View view,
            final int position, final long id) {
        TrackingUtils.sendLongClick(this, "plan", id);
        final Builder builder = new Builder(getActivity());
        builder.setItems(R.array.dialog_edit_plan,
                new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
                        Intent intent = null;
                        switch (which) {
                            case 0:
                                TrackingUtils.sendLongClick(this, "plan#edit", id);
                                intent = new Intent(PlansFragment.this.getActivity(),
                                        PlanEdit.class);
                                intent.setData(ContentUris.withAppendedId(
                                        DataProvider.Plans.CONTENT_URI, id));
                                PlansFragment.this.getActivity().startActivity(intent);
                                break;
                            case 1:
                                TrackingUtils.sendLongClick(this, "plan#showLogs", id);
                                ((Plans) PlansFragment.this.getActivity()).showLogsFragment(id);
                                break;
                            default:
                                break;
                        }
                    }
                });
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.show();
        return true;
    }

    @Override
    public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
        Log.d(TAG, "onCreateLoader(", id, ",", args, ")");
        setInProgress(1);
        PlansAdapter adapter = (PlansAdapter) getListAdapter();
        if ((adapter == null || adapter.getCount() == 0) && vLoading != null) {
            vLoading.setVisibility(View.VISIBLE);
        }

        if (id == UID_DUMMY) {
            ignoreQuery = true;
            final String where = PreferenceManager.getDefaultSharedPreferences(getActivity())
                    .getString("dummy_where", null);
            return new CursorLoader(getActivity(), DataProvider.Plans.CONTENT_URI,
                    DataProvider.Plans.PROJECTION_BASIC, where, null, DataProvider.Plans.ORDER
                    + " ASC");
        } else {
            return new CursorLoader(getActivity(), DataProvider.Plans.CONTENT_URI_SUM
                    .buildUpon()
                    .appendQueryParameter(DataProvider.Plans.PARAM_DATE, String.valueOf(now))
                    .appendQueryParameter(DataProvider.Plans.PARAM_HIDE_ZERO,
                            String.valueOf(hideZero))
                    .appendQueryParameter(DataProvider.Plans.PARAM_HIDE_NOCOST,
                            String.valueOf(hideNoCost))
                    .appendQueryParameter(DataProvider.Plans.PARAM_HIDE_TODAY,
                            String.valueOf(!showToday || now >= 0L))
                    .appendQueryParameter(DataProvider.Plans.PARAM_HIDE_ALLTIME,
                            String.valueOf(!showTotal)).build(), DataProvider.Plans.PROJECTION_SUM,
                    null, null, null);
        }
    }

    @Override
    public void onLoadFinished(final Loader<Cursor> loader, final Cursor data) {
        Log.d(TAG, "onLoadFinished()");
        if (getActivity() == null) {
            Log.w(TAG, "ignore loaded data, activity finished");
            return;
        }
        ignoreQuery = false;
        PlansAdapter adapter = (PlansAdapter) getListAdapter();
        adapter.save();
        if (data != null && data.getCount() > 0) {
            if (now < 0L && data.getColumnIndex(DataProvider.Plans.SUM_COST) > 0) {
                StringBuilder sb = new StringBuilder(DataProvider.Plans.ID + " in (-1");
                try {
                    if (!data.isClosed() && data.moveToFirst()) {
                        do {
                            sb.append(",").append(data.getLong(DataProvider.Plans.INDEX_ID));
                        } while (data.moveToNext());
                    }
                    sb.append(")");
                    PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
                            .putString("dummy_where", sb.toString()).commit();
                } catch (IllegalStateException ex) {
                    Log.e(TAG, "could not walk through cursor to save shown plans", ex);
                }
            }
            vImport.setVisibility(View.GONE);
        } else {
            vImport.setVisibility(View.VISIBLE);
        }
        vLoading.setVisibility(View.GONE);
        try {
            adapter.swapCursor(data);
        } catch (IllegalStateException ex) {
            Log.e(TAG, "could not set cursor to adapter", ex);
            adapter.swapCursor(null);
        }
        setInProgress(-1);
    }

    @Override
    public void onLoaderReset(final Loader<Cursor> loader) {
        Log.d(TAG, "onLoaderReset()");
        try {
            ((PlansAdapter) getListAdapter()).swapCursor(null);
        } catch (Exception e) {
            Log.w(TAG, "error removing cursor", e);
        }
    }
}
