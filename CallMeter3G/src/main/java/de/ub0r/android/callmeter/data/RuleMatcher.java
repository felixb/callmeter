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
package de.ub0r.android.callmeter.data;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;

import de.ub0r.android.callmeter.CallMeter;
import de.ub0r.android.callmeter.R;
import de.ub0r.android.callmeter.ui.Plans;
import de.ub0r.android.callmeter.ui.prefs.Preferences;
import de.ub0r.android.lib.Utils;
import de.ub0r.android.logg0r.Log;

/**
 * Class matching logs via rules to plans.
 *
 * @author flx
 */
public final class RuleMatcher {

    /**
     * Tag for output.
     */
    private static final String TAG = "RuleMatcher";

    /**
     * Minimal number length for converting national to international numbers.
     */
    private static final int NUMBER_MIN_LENGTH = 7;

    /**
     * Steps for updating the GUI.
     */
    private static final int PROGRESS_STEPS = 25;

    /**
     * Strip leading zeros.
     */
    private static boolean stripLeadingZeros = false;

    /**
     * International number prefix.
     */
    private static String intPrefix = "";

    /**
     * Concat prefix and number without leading zeros at number.
     */
    private static boolean zeroPrefix = true;

    /**
     * A single Rule.
     *
     * @author flx
     */
    private static class Rule {

        /**
         * Get the {@link NumbersGroup}.
         *
         * @param cr   {@link ContentResolver}
         * @param gids ids of group
         * @return {@link NumbersGroup}s
         */
        static NumbersGroup[] getNumberGroups(final ContentResolver cr, final String gids) {
            if (gids == null) {
                return null;
            }
            final String[] split = gids.split(",");
            ArrayList<NumbersGroup> list = new ArrayList<NumbersGroup>();
            for (String s : split) {
                if (s == null || s.length() == 0 || s.equals("-1")) {
                    continue;
                }
                final NumbersGroup ng = new NumbersGroup(cr, Utils.parseLong(s, -1L));
                if (ng.numbers.size() > 0) {
                    list.add(ng);
                }
            }
            if (list.size() == 0) {
                return null;
            }
            return list.toArray(new NumbersGroup[list.size()]);
        }

        /**
         * Get the {@link HoursGroup}.
         *
         * @param cr   {@link ContentResolver}
         * @param gids id of group
         * @return {@link HoursGroup}s
         */
        static HoursGroup[] getHourGroups(final ContentResolver cr, final String gids) {
            if (gids == null) {
                return null;
            }
            final String[] split = gids.split(",");
            ArrayList<HoursGroup> list = new ArrayList<HoursGroup>();
            for (String s : split) {
                if (s == null || s.length() == 0 || s.equals("-1")) {
                    continue;
                }
                final HoursGroup ng = new HoursGroup(cr, Utils.parseLong(s, -1L));
                if (ng.hours.size() > 0) {
                    list.add(ng);
                }
            }
            if (list.size() == 0) {
                return null;
            }
            return list.toArray(new HoursGroup[list.size()]);
        }

        /**
         * Group of numbers.
         */
        private static final class NumbersGroup {

            /**
             * List of numbers.
             */
            private final ArrayList<String> numbers = new ArrayList<String>();

            /**
             * Default Constructor.
             *
             * @param cr    {@link ContentResolver}
             * @param what0 argument
             */
            private NumbersGroup(final ContentResolver cr, final long what0) {
                //noinspection ConstantConditions
                final Cursor cursor = cr.query(
                        ContentUris.withAppendedId(DataProvider.Numbers.GROUP_URI, what0),
                        DataProvider.Numbers.PROJECTION, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    final boolean doPrefix = intPrefix.length() > 1;
                    do {
                        String s = cursor.getString(DataProvider.Numbers.INDEX_NUMBER);
                        if (s == null || s.length() == 0) {
                            continue;
                        }
                        if (stripLeadingZeros) {
                            s = s.replaceFirst("^00*", "");
                        }
                        if (doPrefix && !s.startsWith("%")) {
                            s = national2international(intPrefix, zeroPrefix, s);
                        }
                        numbers.add(s);
                    } while (cursor.moveToNext());
                }
                if (cursor != null && !cursor.isClosed()) {
                    cursor.close();
                }
            }

            /**
             * Convert national number to international. Old format internationals were converted to
             * new format.
             *
             * @param iPrefix default prefix
             * @param zPrefix concat prefix and number without leading zeros at number
             * @param number  national number
             * @return international number
             */
            private static String national2international(final String iPrefix,
                    final boolean zPrefix, final String number) {
                if (number.length() < NUMBER_MIN_LENGTH && !number.endsWith("%")) {
                    return number;
                } else if (number.startsWith("0800") || number.startsWith("00800")) {
                    return number;
                } else if (number.startsWith("+")) {
                    return number;
                } else if (number.startsWith("00")) {
                    return "+" + number.substring(2);
                } else if (number.startsWith("0")) {
                    return iPrefix + number.substring(1);
                } else if (iPrefix.length() > 1 && number.startsWith(iPrefix.substring(1))) {
                    return "+" + number;
                } else if (zPrefix) {
                    return iPrefix + number;
                } else {
                    return number;
                }
            }

            /**
             * Match a given log.
             *
             * @param log {@link Cursor} representing log
             * @return true if log matches
             */
            boolean match(final Cursor log) {
                String number = log.getString(DataProvider.Logs.INDEX_REMOTE);
                if (number == null) {
                    return false;
                }
                if (number.length() == 0) {
                    return false;
                }
                Log.d(TAG, "NumbersGroup.match(", number, ")");
                if (number.length() > 1) {
                    if (stripLeadingZeros) {
                        number = number.replaceFirst("^00*", "");
                    }
                    if (intPrefix.length() > 1) {
                        number = national2international(intPrefix, zeroPrefix, number);
                    }
                }
                final int l = numbers.size();
                for (int i = 0; i < l; i++) {
                    String n = numbers.get(i);
                    if (n == null) {
                        Log.w(TAG, "numbers[", i, "] = null");
                        return false;
                    }
                    int nl = n.length();
                    if (nl <= 1) {
                        Log.w(TAG, "#numbers[", i, "] = ", nl);
                        return false;
                    }

                    if (n.startsWith("%")) {
                        if (n.endsWith("%")) {
                            if (nl == 2) {
                                Log.w(TAG, "numbers[", i, "] = ", n);
                                return false;
                            }
                            if (number.contains(n.substring(1, nl - 1))) {
                                Log.d(TAG, "match: ", n);
                                return true;
                            }
                        } else {
                            if (number.endsWith(n.substring(1))) {
                                Log.d(TAG, "match: ", n);
                                return true;
                            }
                        }
                    } else if (n.endsWith("%")) {
                        if (number.startsWith(n.substring(0, nl - 1))) {
                            Log.d(TAG, "match: ", n);
                            return true;
                        }
                    } else if (PhoneNumberUtils.compare(number, n)) {
                        Log.d(TAG, "match: ", n);
                        return true;
                    }
                    Log.v(TAG, "no match: ", n);
                }
                return false;
            }
        }

        /**
         * Group of hours.
         */
        private static final class HoursGroup {

            /**
             * List of hours.
             */
            private final SparseArray<HashSet<Integer>> hours = new SparseArray<HashSet<Integer>>();

            /**
             * Entry for monday - sunday.
             */
            private static final int ALL_WEEK = 0;

            /**
             * Entry for monday.
             */
            private static final int MON = 1;
            /** Entry for tuesday. */
            // private static final int TUE = 2;
            /** Entry for wednesday. */
            // private static final int WED = 3;
            /** Entry for thrusday. */
            // private static final int THU = 4;
            /** Entry for friday. */
            // private static final int FRI = 5;

            /**
             * Entry for satadurday.
             */
            private static final int SAT = 6;

            /**
             * Entry for sunday.
             */
            private static final int SUN = 7;

            /**
             * Entry for monday - friday.
             */
            private static final int MON_FRI = 8;

            /**
             * Default Constructor.
             *
             * @param cr    {@link ContentResolver}
             * @param what0 argument
             */
            private HoursGroup(final ContentResolver cr, final long what0) {
                //noinspection ConstantConditions
                final Cursor cursor = cr.query(
                        ContentUris.withAppendedId(DataProvider.Hours.GROUP_URI, what0),
                        DataProvider.Hours.PROJECTION, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        final int d = cursor.getInt(DataProvider.Hours.INDEX_DAY);
                        final int h = cursor.getInt(DataProvider.Hours.INDEX_HOUR);
                        HashSet<Integer> hs = hours.get(d);
                        if (hs == null) {
                            hs = new HashSet<Integer>();
                            hs.add(h);
                            hours.put(d, hs);
                        } else {
                            hs.add(h);
                        }
                    } while (cursor.moveToNext());
                }
                if (cursor != null && !cursor.isClosed()) {
                    cursor.close();
                }
            }

            /**
             * Internal var for match().
             */
            private static final Calendar CAL = Calendar.getInstance();

            /**
             * Match a given log.
             *
             * @param log {@link Cursor} representing log
             * @return true if log matches
             */
            boolean match(final Cursor log) {
                long date = log.getLong(DataProvider.Logs.INDEX_DATE);
                CAL.setTimeInMillis(date);
                final int d = (CAL.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY) % SUN;
                final int h = CAL.get(Calendar.HOUR_OF_DAY) + 1;
                int l = hours.size();
                for (int i = 0; i < l; i++) {
                    int k = hours.keyAt(i);
                    if (k == ALL_WEEK || (k == MON_FRI && d < SAT && d >= MON) || k % SUN == d) {
                        for (int v : hours.get(k)) {
                            if (v == 0 || v == h) {
                                return true;
                            }
                        }
                    }
                }
                return false;
            }
        }

        /**
         * Id.
         */
        private final int id;

        /**
         * ID of plan referred by this rule.
         */
        private final int planId;

        /**
         * Kind of rule.
         */
        private final int what;

        /**
         * My own number.
         */
        private final String myNumber;

        /**
         * Is roamed?
         */
        private final int roamed;

        /**
         * Is direction?
         */
        private final int direction;

        /**
         * Match hours?
         */
        private final HoursGroup[] inhours, exhours;

        /**
         * Match numbers?
         */
        private final NumbersGroup[] innumbers, exnumbers;

        /**
         * Match only if limit is not reached?
         */
        private final boolean limitNotReached;

        /**
         * Match only websms.
         */
        private final int iswebsms;

        /**
         * Match only specific websms connector.
         */
        private final String iswebsmsConnector;

        /**
         * Match only sipcalls.
         */
        private final int issipcall;

        /**
         * Load a {@link Rule}.
         *
         * @param cr              {@link ContentResolver}
         * @param overwritePlanId overwrite plan id
         * @param cursor          {@link Cursor}
         */
        Rule(final ContentResolver cr, final Cursor cursor, final int overwritePlanId) {
            id = cursor.getInt(DataProvider.Rules.INDEX_ID);
            if (overwritePlanId >= 0) {
                planId = overwritePlanId;
            } else {
                planId = cursor.getInt(DataProvider.Rules.INDEX_PLAN_ID);
            }
            what = cursor.getInt(DataProvider.Rules.INDEX_WHAT);
            direction = cursor.getInt(DataProvider.Rules.INDEX_DIRECTION);
            String s = cursor.getString(DataProvider.Rules.INDEX_MYNUMBER);
            if (TextUtils.isEmpty(s)) {
                myNumber = null;
            } else {
                myNumber = s;
            }
            roamed = cursor.getInt(DataProvider.Rules.INDEX_ROAMED);
            inhours = getHourGroups(cr, cursor.getString(DataProvider.Rules.INDEX_INHOURS_ID));
            exhours = getHourGroups(cr, cursor.getString(DataProvider.Rules.INDEX_EXHOURS_ID));
            innumbers = getNumberGroups(cr,
                    cursor.getString(DataProvider.Rules.INDEX_INNUMBERS_ID));
            exnumbers = getNumberGroups(cr,
                    cursor.getString(DataProvider.Rules.INDEX_EXNUMBERS_ID));
            limitNotReached = cursor.getInt(DataProvider.Rules.INDEX_LIMIT_NOT_REACHED) > 0;
            if (cursor.isNull(DataProvider.Rules.INDEX_IS_WEBSMS)) {
                iswebsms = DataProvider.Rules.NO_MATTER;
            } else {
                final int i = cursor.getInt(DataProvider.Rules.INDEX_IS_WEBSMS);
                if (i >= 0) {
                    iswebsms = i;
                } else {
                    iswebsms = DataProvider.Rules.NO_MATTER;
                }
            }
            s = cursor.getString(DataProvider.Rules.INDEX_IS_WEBSMS_CONNETOR);
            if (TextUtils.isEmpty(s)) {
                iswebsmsConnector = "";
            } else {
                iswebsmsConnector = " AND " + DataProvider.WebSMS.CONNECTOR + " LIKE '%"
                        + s.toLowerCase() + "%'";
            }
            if (cursor.isNull(DataProvider.Rules.INDEX_IS_SIPCALL)) {
                issipcall = DataProvider.Rules.NO_MATTER;
            } else {
                final int i = cursor.getInt(DataProvider.Rules.INDEX_IS_SIPCALL);
                if (i >= 0) {
                    issipcall = i;
                } else {
                    issipcall = DataProvider.Rules.NO_MATTER;
                }
            }
        }

        /**
         * @return {@link Rule}'s id
         */
        int getId() {
            return id;
        }

        /**
         * @return {@link Plan}'s id
         */
        int getPlanId() {
            return planId;
        }

        /**
         * Internal var for match().
         */
        private static final String[] S1 = new String[1];

        /**
         * Math a log.
         *
         * @param cr  {@link ContentResolver}
         * @param log {@link Cursor} representing the log.
         * @return matched?
         */
        boolean match(final ContentResolver cr, final Cursor log) {
            Log.d(TAG, "match()");
            Log.d(TAG, "what: ", what);
            final int t = log.getInt(DataProvider.Logs.INDEX_TYPE);
            Log.d(TAG, "type: ", t);
            boolean ret = false;

            if (roamed == 0 || roamed == 1) {
                // rule.roamed=0: yes
                // rule.roamed=1: no
                // log.roamed=0: not roamed
                // log.roamed=1: roamed
                ret = log.getInt(DataProvider.Logs.INDEX_ROAMED) != roamed;
                Log.d(TAG, "ret after romaing: ", ret);
                if (!ret) {
                    return false;
                }
            }

            if (direction >= 0 && direction != DataProvider.Rules.NO_MATTER) {
                ret = log.getInt(DataProvider.Logs.INDEX_DIRECTION) == direction;
                Log.d(TAG, "ret after direction: ", ret);
                if (!ret) {
                    return false;
                }
            }

            switch (what) {
                case DataProvider.Rules.WHAT_CALL:
                    ret = (t == DataProvider.TYPE_CALL);
                    if (ret && issipcall != DataProvider.Rules.NO_MATTER) {
                        final long d = log.getLong(DataProvider.Logs.INDEX_DATE);
                        Log.d(TAG, "match sipcall: ", issipcall);
                        S1[0] = String.valueOf(d);
                        if (issipcall == 1) {
                            // match no sipcall
                            final Cursor c = cr.query(DataProvider.SipCall.CONTENT_URI,
                                    DataProvider.SipCall.PROJECTION,
                                    DataProvider.SipCall.DATE + " = ?", S1, null);
                            if (c != null && c.getCount() > 0) {
                                ret = false;
                            }
                            if (c != null && !c.isClosed()) {
                                c.close();
                            }
                        } else {
                            // match only sipcall
                            final Cursor c = cr.query(DataProvider.SipCall.CONTENT_URI,
                                    DataProvider.SipCall.PROJECTION,
                                    DataProvider.SipCall.DATE + " = ?", S1, null);
                            ret = c != null && c.getCount() > 0;
                            if (c != null && !c.isClosed()) {
                                c.close();
                            }
                        }
                        Log.d(TAG, "match sipcall: ", issipcall, "; ", ret);
                    }
                    break;
                case DataProvider.Rules.WHAT_DATA:
                    ret = (t == DataProvider.TYPE_DATA);
                    break;
                case DataProvider.Rules.WHAT_MMS:
                    ret = (t == DataProvider.TYPE_MMS);
                    break;
                case DataProvider.Rules.WHAT_SMS:
                    ret = (t == DataProvider.TYPE_SMS);
                    if (ret && iswebsms != DataProvider.Rules.NO_MATTER) {
                        final long d = log.getLong(DataProvider.Logs.INDEX_DATE);
                        Log.d(TAG, "match websms: ", iswebsms);
                        S1[0] = String.valueOf(d);
                        if (iswebsms == 1) {
                            // match no websms
                            final Cursor c = cr.query(DataProvider.WebSMS.CONTENT_URI,
                                    DataProvider.WebSMS.PROJECTION,
                                    DataProvider.WebSMS.DATE + " = ?",
                                    S1, null);
                            if (c != null && c.getCount() > 0) {
                                ret = false;
                            }
                            if (c != null && !c.isClosed()) {
                                c.close();
                            }
                        } else {
                            // match only websms
                            final Cursor c = cr.query(DataProvider.WebSMS.CONTENT_URI,
                                    DataProvider.WebSMS.PROJECTION,
                                    DataProvider.WebSMS.DATE + " = ? "
                                            + iswebsmsConnector, S1, null
                            );
                            ret = c != null && c.getCount() > 0;
                            if (c != null && !c.isClosed()) {
                                c.close();
                            }
                        }
                        Log.d(TAG, "match websms: ", iswebsms, "; ", ret);
                    }
                    break;
                default:
                    break;
            }
            Log.d(TAG, "ret after type: ", ret);
            if (!ret) {
                return false;
            }
            if (limitNotReached) {
                final Plan p = plans.get(planId);
                if (p != null) {
                    p.checkBillday(log);
                    ret = p.getRemainingLimit() > 0f;
                }
                if (!ret) {
                    Log.d(TAG, "limit reached: ", planId);
                }
            }
            Log.d(TAG, "ret after limit: ", ret);
            if (!ret) {
                return false;
            }

            if (myNumber != null) {
                // FIXME: do equals?
                ret = myNumber.equals(log.getString(DataProvider.Logs.INDEX_MYNUMBER));
                Log.d(TAG, "ret after mynumber: ", ret);
                if (!ret) {
                    return false;
                }
            }

            if (inhours != null) {
                ret = false;
                for (HoursGroup inhour : inhours) {
                    ret = inhour.match(log);
                    if (ret) {
                        break;
                    }
                }
            }
            Log.d(TAG, "ret after inhours: ", ret);
            if (!ret) {
                return false;
            }
            if (exhours != null) {
                for (HoursGroup exhour : exhours) {
                    ret = !exhour.match(log);
                    if (!ret) {
                        break;
                    }
                }
            }
            Log.d(TAG, "ret after exhours: ", ret);
            if (!ret) {
                return false;
            }
            if (innumbers != null) {
                ret = false;
                for (NumbersGroup innumber : innumbers) {
                    ret = innumber.match(log);
                    if (ret) {
                        break;
                    }
                }
            }
            Log.d(TAG, "ret after innumbers: ", ret);
            if (!ret) {
                return false;
            }
            if (exnumbers != null) {
                for (NumbersGroup exnumber : exnumbers) {
                    ret = !exnumber.match(log);
                    if (!ret) {
                        break;
                    }
                }
            }
            Log.d(TAG, "ret after exnumbers: ", ret);
            return ret;
        }
    }

    /**
     * A single Plan.
     *
     * @author flx
     */
    private static class Plan {

        /**
         * Id.
         */
        private final int id;

        /**
         * Name of plan.
         */
        private final String name;

        /**
         * Type of log.
         */
        private final int type;

        /**
         * Type of limit.
         */
        private final int limitType;

        /**
         * Limit.
         */
        private final long limit;

        /**
         * Billmode.
         */
        private final int billModeFirstLength, billModeNextLength;

        /**
         * Billday.
         */
        private final Calendar billday;

        /**
         * Billperiod.
         */
        private final int billperiod;

        /**
         * Cost per item.
         */
        private final float costPerItem;

        /**
         * Cost per amount.
         */
        private final float costPerAmount1, costPerAmount2;

        /**
         * Cost per item in limit.
         */
        private final float costPerItemInLimit;

        /**
         * Cost per amount in limit.
         */
        private final float costPerAmountInLimit1, costPerAmountInLimit2;

        /**
         * Units for mixed plans.
         */
        private final int upc, ups, upm, upd;

        /**
         * Strip first x seconds.
         */
        private final int stripSeconds;

        /**
         * Strip everything but first x seconds.
         */
        private final int stripPast;

        /**
         * Parent plan id.
         */
        private final int ppid;

        /**
         * PArent plan. Set in RuleMatcher.load().
         */
        private Plan parent = null;

        /**
         * Time of next alert.
         */
        private long nextAlert = 0;

        /**
         * Last valid billday.
         */
        private Calendar currentBillday = null;

        /**
         * Time of nextBillday.
         */
        private long nextBillday = -1L;

        /**
         * Amount billed this period.
         */
        private float billedAmount = 0f;

        /**
         * Cost billed this period.
         */
        private float billedCost = 0f;

        /**
         * {@link ContentResolver}.
         */
        private final ContentResolver cResolver;

        /**
         * Load a {@link Plan}.
         *
         * @param cr     {@link ContentResolver}
         * @param cursor {@link Cursor}
         */
        Plan(final ContentResolver cr, final Cursor cursor) {
            cResolver = cr;
            id = cursor.getInt(DataProvider.Plans.INDEX_ID);
            name = cursor.getString(DataProvider.Plans.INDEX_NAME);
            type = cursor.getInt(DataProvider.Plans.INDEX_TYPE);
            limitType = cursor.getInt(DataProvider.Plans.INDEX_LIMIT_TYPE);
            final long l = DataProvider.Plans.getLimit(type, limitType,
                    cursor.getFloat(DataProvider.Plans.INDEX_LIMIT));
            if (limitType == DataProvider.LIMIT_TYPE_UNITS
                    && type == DataProvider.TYPE_DATA) {
                // normally amount is saved as kB, here it is B
                limit = l * CallMeter.BYTE_KB;
            } else {
                limit = l;
            }

            costPerItem = cursor.getFloat(DataProvider.Plans.INDEX_COST_PER_ITEM);
            costPerAmount1 = cursor.getFloat(DataProvider.Plans.INDEX_COST_PER_AMOUNT1);
            costPerAmount2 = cursor.getFloat(DataProvider.Plans.INDEX_COST_PER_AMOUNT2);
            costPerItemInLimit = cursor
                    .getFloat(DataProvider.Plans.INDEX_COST_PER_ITEM_IN_LIMIT);
            costPerAmountInLimit1 = cursor
                    .getFloat(DataProvider.Plans.INDEX_COST_PER_AMOUNT_IN_LIMIT1);
            costPerAmountInLimit2 = cursor
                    .getFloat(DataProvider.Plans.INDEX_COST_PER_AMOUNT_IN_LIMIT2);
            upc = cursor.getInt(DataProvider.Plans.INDEX_MIXED_UNITS_CALL);
            ups = cursor.getInt(DataProvider.Plans.INDEX_MIXED_UNITS_SMS);
            upm = cursor.getInt(DataProvider.Plans.INDEX_MIXED_UNITS_MMS);
            upd = cursor.getInt(DataProvider.Plans.INDEX_MIXED_UNITS_DATA);
            nextAlert = cursor.getLong(DataProvider.Plans.INDEX_NEXT_ALERT);
            stripSeconds = cursor.getInt(DataProvider.Plans.INDEX_STRIP_SECONDS);
            stripPast = cursor.getInt(DataProvider.Plans.INDEX_STRIP_PAST);

            final long bp = cursor.getLong(DataProvider.Plans.INDEX_BILLPERIOD_ID);
            if (bp >= 0) {
                //noinspection ConstantConditions
                final Cursor c = cr.query(
                        ContentUris.withAppendedId(DataProvider.Plans.CONTENT_URI, bp),
                        DataProvider.Plans.PROJECTION, null, null, null);
                if (c != null && c.moveToFirst()) {
                    billday = Calendar.getInstance();
                    billday.setTimeInMillis(c.getLong(DataProvider.Plans.INDEX_BILLDAY));
                    billperiod = c.getInt(DataProvider.Plans.INDEX_BILLPERIOD);
                } else {
                    billperiod = DataProvider.BILLPERIOD_INFINITE;
                    billday = null;
                }
                if (c != null && !c.isClosed()) {
                    c.close();
                }
            } else {
                billperiod = DataProvider.BILLPERIOD_INFINITE;
                billday = null;
            }
            final String billmode = cursor.getString(DataProvider.Plans.INDEX_BILLMODE);
            if (billmode != null && billmode.contains("/")) {
                String[] billmodes = billmode.split("/");
                billModeFirstLength = Utils.parseInt(billmodes[0], 1);
                billModeNextLength = Utils.parseInt(billmodes[1], 1);
            } else {
                billModeFirstLength = 1;
                billModeNextLength = 1;
            }
            ppid = DataProvider.Plans.getParent(cr, id);
        }

        /**
         * Get {@link Plan}'s id.
         *
         * @return {@link Plan}'s id
         */
        long getId() {
            return id;
        }

        /**
         * Get upc/upd/upm/ups according to log type.
         *
         * @param logType log type
         * @return units per *
         */
        int getUP(final int logType) {
            switch (logType) {
                case DataProvider.TYPE_CALL:
                    return upc;
                case DataProvider.TYPE_DATA:
                    return upd;
                case DataProvider.TYPE_MMS:
                    return upm;
                case DataProvider.TYPE_SMS:
                    return ups;
                default:
                    return 0;
            }
        }

        /**
         * Check if this log is starting a new billing period.
         *
         * @param log {@link Cursor} pointing to log
         */
        void checkBillday(final Cursor log) {
            // skip for infinite bill periods
            if (billperiod == DataProvider.BILLPERIOD_INFINITE) {
                return;
            }

            // check whether date is in current bill period
            final long d = log.getLong(DataProvider.Logs.INDEX_DATE);
            if (currentBillday == null || nextBillday < d
                    || d < currentBillday.getTimeInMillis()) {
                final Calendar now = Calendar.getInstance();
                now.setTimeInMillis(d);
                currentBillday = DataProvider.Plans.getBillDay(billperiod, billday,
                        now, false);
                if (currentBillday == null) {
                    return;
                }
                final Calendar nbd = DataProvider.Plans.getBillDay(billperiod, billday,
                        now, true);
                if (nbd == null) {
                    return;
                }
                nextBillday = nbd.getTimeInMillis();

                // load old stats
                final DataProvider.Plans.Plan plan = DataProvider.Plans.Plan.getPlan(
                        cResolver, id, now, false, false);
                if (plan == null) {
                    billedAmount = 0f;
                    billedCost = 0f;
                } else {
                    billedAmount = plan.bpBa;
                    billedCost = plan.cost;
                }
            }
            if (parent != null) {
                parent.checkBillday(log);
            }
        }

        /**
         * @return remaining limit before it is reached.
         */
        float getRemainingLimit() {
            Log.d(TAG, "getRemainingLimit(): ", id);
            if (parent != null && limitType == DataProvider.LIMIT_TYPE_NONE) {
                Log.d(TAG, "check parent");
                return parent.getRemainingLimit();
            } else {
                Log.d(TAG, "ltype: ", limitType);
                switch (limitType) {
                    case DataProvider.LIMIT_TYPE_COST:
                        Log.d(TAG, "bc<lt ", billedCost * CallMeter.HUNDRED, "<", limit);
                        return limit - billedCost * CallMeter.HUNDRED;
                    case DataProvider.LIMIT_TYPE_UNITS:
                        Log.d(TAG, "ba<lt ", billedAmount, "<", limit);
                        return limit - billedAmount;
                    default:
                        return 0;
                }
            }
        }

        /**
         * Round up time with bill mode in mind.
         *
         * @param time time
         * @return rounded time
         */
        private long roundTime(final long time) {
            // 0 => 0
            if (time <= 0) {
                return 0;
            }
            final long fl = billModeFirstLength;
            final long nl = billModeNextLength;
            // !0 ..
            if (time <= fl) { // round first slot
                return fl;
            }
            if (nl == 0) {
                return fl;
            }
            if (nl == 1) {
                return time;
            }
            final long nt = time - fl;
            if (nt % nl == 0) {
                return time;
            }
            // round up to next full slot
            return fl + ((nt / nl) + 1) * nl;
        }

        /**
         * Update {@link Plan}.
         *
         * @param amount billed amount
         * @param cost   billed cost
         * @param t      type of log
         */
        void updatePlan(final float amount, final float cost, final int t) {
            billedAmount += amount;
            billedCost += cost;
            final Plan pp = parent;
            if (pp != null) {
                if (type != DataProvider.TYPE_MIXED && pp.type == DataProvider.TYPE_MIXED) {
                    switch (t) {
                        case DataProvider.TYPE_CALL:
                            pp.billedAmount += amount * pp.upc / CallMeter.SECONDS_MINUTE;
                            break;
                        case DataProvider.TYPE_MMS:
                            pp.billedAmount += amount * pp.upm;
                            break;
                        case DataProvider.TYPE_SMS:
                            pp.billedAmount += amount * pp.ups;
                            break;
                        default:
                            break;
                    }
                } else {
                    pp.billedAmount += amount;
                }
                parent.billedCost += cost;
            }
        }

        /**
         * Get billed amount for amount.
         *
         * @param log {@link Cursor} pointing to log
         * @return billed amount.
         */
        float getBilledAmount(final Cursor log) {
            long amount = log.getLong(DataProvider.Logs.INDEX_AMOUNT);
            final int t = log.getInt(DataProvider.Logs.INDEX_TYPE);
            float ret;
            switch (t) {
                case DataProvider.TYPE_CALL:
                    ret = roundTime(amount);
                    if (stripSeconds > 0) {
                        ret -= stripSeconds;
                        if (ret < 0f) {
                            ret = 0f;
                        }
                    }
                    if (stripPast > 0 && ret > stripPast) {
                        ret = stripPast;
                    }
                    break;
                default:
                    ret = amount;
                    break;
            }

            if (type == DataProvider.TYPE_MIXED) {
                switch (t) {
                    case DataProvider.TYPE_CALL:
                        ret = ret * upc / CallMeter.SECONDS_MINUTE;
                        break;
                    case DataProvider.TYPE_SMS:
                        ret = ret * ups;
                        break;
                    case DataProvider.TYPE_MMS:
                        ret = ret * upm;
                        break;
                    case DataProvider.TYPE_DATA:
                        ret = ret * upd / CallMeter.BYTE_MB;
                    default:
                        break;
                }
            }
            return ret;
        }

        /**
         * Get cost for amount.
         *
         * @param log     {@link Cursor} pointing to log
         * @param bAmount billed amount
         * @return cost
         */
        float getCost(final Cursor log, final float bAmount) {
            final int t = log.getInt(DataProvider.Logs.INDEX_TYPE);
            final int pt = type;

            float ret = 0f;
            float as0; // split amount: before limit
            float as1; // split amount: after limit
            Plan p;
            float f = 1; // factor for mixed plans with limits merging this plan
            if (parent != null && limitType == DataProvider.LIMIT_TYPE_NONE) {
                p = parent;
                if (pt != DataProvider.TYPE_MIXED && p.type == DataProvider.TYPE_MIXED) {
                    f = 1f / p.getUP(t);
                    switch (t) {
                        case DataProvider.TYPE_CALL:
                            f *= CallMeter.SECONDS_MINUTE;
                            break;
                        case DataProvider.TYPE_DATA:
                            f *= CallMeter.BYTE_MB;
                            break;
                        default:
                            // nothing to do
                            break;
                    }
                }
            } else {
                p = this;
            }
            // split amount at limit
            float remaining = p.getRemainingLimit() * f;
            if (p.limitType == DataProvider.LIMIT_TYPE_NONE || remaining <= 0f) {
                as0 = 0;
                as1 = bAmount;
            } else if (p.limitType == DataProvider.LIMIT_TYPE_UNITS && remaining < bAmount) {
                as0 = remaining;
                as1 = bAmount - remaining;
            } else { // TODO: fix for LIMIT_TYPE_COST
                as0 = bAmount;
                as1 = 0;
            }

            if (t == DataProvider.TYPE_SMS || pt == DataProvider.TYPE_MIXED) {
                ret += as0 * costPerItemInLimit + as1 * costPerItem;
            } else {
                ret += as0 > 0f ? costPerItemInLimit : costPerItem;
            }

            switch (t) {
                case DataProvider.TYPE_CALL:
                    if (bAmount <= billModeFirstLength) {
                        // bAmount is most likely < remaining
                        ret += (as0 * costPerAmountInLimit1 + as1 * costPerAmount1)
                                / CallMeter.SECONDS_MINUTE;
                    } else if (as0 == 0f) {
                        ret += costPerAmount1 * billModeFirstLength
                                / CallMeter.SECONDS_MINUTE;
                        ret += costPerAmount2 * (bAmount - billModeFirstLength)
                                / CallMeter.SECONDS_MINUTE;
                    } else if (as1 == 0f) {
                        ret += costPerAmountInLimit1 * billModeFirstLength
                                / CallMeter.SECONDS_MINUTE;
                        ret += costPerAmountInLimit2 * (bAmount - billModeFirstLength)
                                / CallMeter.SECONDS_MINUTE;
                    } else if (as0 == billModeFirstLength) {
                        ret += costPerAmountInLimit1 * billModeFirstLength
                                / CallMeter.SECONDS_MINUTE;
                        ret += costPerAmount2 * (bAmount - billModeFirstLength)
                                / CallMeter.SECONDS_MINUTE;
                    } else if (as0 > billModeFirstLength) {
                        ret += costPerAmountInLimit1 * billModeFirstLength
                                / CallMeter.SECONDS_MINUTE;
                        ret += (as0 - billModeFirstLength) * costPerAmountInLimit2
                                / CallMeter.SECONDS_MINUTE;
                        ret += as1 * costPerAmount2 / CallMeter.SECONDS_MINUTE;
                    } else { // as0 < billModeFirstLength && as0 > 0 && as1 > 0
                        ret += as0 * costPerAmountInLimit1 / CallMeter.SECONDS_MINUTE;
                        ret += (billModeFirstLength - as0) * costPerAmount1
                                / CallMeter.SECONDS_MINUTE;
                        ret += costPerAmount2 * (bAmount - billModeFirstLength)
                                / CallMeter.SECONDS_MINUTE;
                    }
                    break;
                case DataProvider.TYPE_DATA:
                    ret += (as0 * costPerAmountInLimit1 + as1 * costPerAmount1)
                            / CallMeter.BYTE_MB;
                    break;
                default:
                    break;
            }
            return ret;
        }

        /**
         * Get amount of free cost.
         *
         * @param log  {@link Cursor} pointing to log
         * @param cost cost calculated by getCost()
         * @return free cost
         */
        float getFree(final Cursor log, final float cost) {
            if (limitType != DataProvider.LIMIT_TYPE_COST) {
                if (parent != null) {
                    return parent.getFree(log, cost);
                }
                return 0f;
            }
            final float l = ((float) limit) / CallMeter.HUNDRED;
            if (l <= billedCost) {
                return 0f;
            }
            if (l >= billedCost + cost) {
                return cost;
            }
            return l - billedCost;
        }

        @Override
        public String toString() {
            return "RuleMatcher.Plan: " + name;
        }
    }

    /**
     * List of {@link Rule}s.
     */
    private static ArrayList<Rule> rules = null;

    /**
     * List of {@link Plan}s.
     */
    private static SparseArray<Plan> plans = null;

    /**
     * Default constructor.
     */
    private RuleMatcher() {
    }

    /**
     * Load {@link Rule}s and {@link Plan}s.
     *
     * @param context {@link Context}
     */
    private static void load(final Context context) {
        Log.d(TAG, "load()");
        if (rules != null && plans != null) {
            return;
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        stripLeadingZeros = prefs.getBoolean(Preferences.PREFS_STRIP_LEADING_ZEROS, false);
        intPrefix = prefs.getString(Preferences.PREFS_INT_PREFIX, "");
        zeroPrefix = !intPrefix.equals("+44") && !intPrefix.equals("+49");

        final ContentResolver cr = context.getContentResolver();

        // load rules
        rules = new ArrayList<Rule>();
        Cursor cursor = cr.query(DataProvider.Rules.CONTENT_URI, DataProvider.Rules.PROJECTION,
                DataProvider.Rules.ACTIVE + ">0", null, DataProvider.Rules.ORDER);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                rules.add(new Rule(cr, cursor, -1));
            } while (cursor.moveToNext());
        }
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

        // load plans
        plans = new SparseArray<Plan>();
        cursor = cr.query(DataProvider.Plans.CONTENT_URI, DataProvider.Plans.PROJECTION,
                DataProvider.Plans.WHERE_REALPLANS, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                final int i = cursor.getInt(DataProvider.Plans.INDEX_ID);
                plans.put(i, new Plan(cr, cursor));
            } while (cursor.moveToNext());
        }
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
        // update parent references
        int l = plans.size();
        for (int i = 0; i < l; i++) {
            Plan p = plans.valueAt(i);
            p.parent = plans.get(p.ppid);
        }
    }

    /**
     * Reload Rules and plans.
     */
    static void flush() {
        Log.d(TAG, "flush()");
        rules = null;
        plans = null;
    }

    /**
     * Unmatch all logs.
     *
     * @param context {@link Context}
     */
    public static void unmatch(final Context context) {
        Log.d(TAG, "unmatch()");
        ContentValues cv = new ContentValues();
        final ContentResolver cr = context.getContentResolver();
        cv.put(DataProvider.Logs.PLAN_ID, DataProvider.NO_ID);
        cv.put(DataProvider.Logs.RULE_ID, DataProvider.NO_ID);
        // reset all but manually set plans
        cr.update(DataProvider.Logs.CONTENT_URI, cv, DataProvider.Logs.RULE_ID
                        + " is null or NOT (" + DataProvider.Logs.RULE_ID + " = "
                        + DataProvider.NOT_FOUND
                        + " AND " + DataProvider.Logs.PLAN_ID + " != " + DataProvider.NOT_FOUND
                        + ")",
                null
        );
        resetAlert(context);
    }

    public static void resetAlert(final Context context) {
        Log.d(TAG, "resetAlert()");
        ContentValues cv = new ContentValues();
        final ContentResolver cr = context.getContentResolver();
        cv.put(DataProvider.Plans.NEXT_ALERT, 0);
        cr.update(DataProvider.Plans.CONTENT_URI, cv, null, null);
        flush();
    }

    /**
     * Internal ar for matchLog().
     */
    private static final String WHERE = DataProvider.Logs.ID + " = ?";

    /**
     * Match a single log record given as {@link Cursor}.
     *
     * @param cr  {@link ContentResolver}
     * @param ops List of {@link ContentProviderOperation}s
     * @param log {@link Cursor} representing the log
     * @return true if a log was matched
     */
    private static boolean matchLog(final ContentResolver cr,
            final ArrayList<ContentProviderOperation> ops, final Cursor log) {
        if (cr == null) {
            Log.e(TAG, "matchLog(null, ops, log)");
            return false;
        }
        if (log == null) {
            Log.e(TAG, "matchLog(cr, ops, null)");
            return false;
        }
        final long lid = log.getLong(DataProvider.Logs.INDEX_ID);
        final int t = log.getInt(DataProvider.Logs.INDEX_TYPE);
        Log.d(TAG, "matchLog(cr, ", lid, ")");
        boolean matched = false;
        if (rules == null) {
            Log.e(TAG, "rules = null");
            return false;
        }
        if (plans == null) {
            Log.e(TAG, "plans = null");
            return false;
        }
        for (final Rule r : rules) {
            if (r == null || !r.match(cr, log) || plans == null) {
                continue;
            }
            Log.d(TAG, "matched rule: ", r.getId());
            final Plan p = plans.get(r.getPlanId());
            if (p != null) {
                final long pid = p.getId();
                final long rid = r.getId();
                Log.d(TAG, "found plan: ", pid);
                p.checkBillday(log);
                final float ba = p.getBilledAmount(log);
                final float bc = p.getCost(log, ba);
                ContentProviderOperation op = ContentProviderOperation
                        .newUpdate(DataProvider.Logs.CONTENT_URI)
                        .withValue(DataProvider.Logs.PLAN_ID, pid)
                        .withValue(DataProvider.Logs.RULE_ID, rid)
                        .withValue(DataProvider.Logs.BILL_AMOUNT, ba)
                        .withValue(DataProvider.Logs.COST, bc)
                        .withValue(DataProvider.Logs.FREE, p.getFree(log, bc))
                        .withSelection(WHERE, new String[]{String.valueOf(lid)})
                        .build();
                p.updatePlan(ba, bc, t);
                ops.add(op);
                matched = true;
                break;
            }
        }
        if (!matched) {
            ContentProviderOperation op = ContentProviderOperation
                    .newUpdate(DataProvider.Logs.CONTENT_URI)
                    .withValue(DataProvider.Logs.PLAN_ID, DataProvider.NOT_FOUND)
                    .withValue(DataProvider.Logs.RULE_ID, DataProvider.NOT_FOUND)
                    .withSelection(WHERE, new String[]{String.valueOf(lid)})
                    .build();
            ops.add(op);
        }
        return matched;
    }

    /**
     * Match a single log record.
     *
     * @param cr  {@link ContentResolver}
     * @param lid id of log item
     * @param pid id of plan
     */
    public static void matchLog(final ContentResolver cr, final long lid, final int pid) {
        if (cr == null) {
            Log.e(TAG, "matchLog(null, lid, pid)");
            return;
        }
        if (lid < 0L || pid < 0L) {
            Log.e(TAG, "matchLog(cr, " + lid + "," + pid + ")");
            return;
        }
        Log.d(TAG, "matchLog(cr, ", lid, ",", pid, ")");

        if (plans == null) {
            Log.e(TAG, "plans = null");
            return;
        }
        final Plan p = plans.get(pid);
        if (p == null) {
            Log.e(TAG, "plan=null");
            return;
        }
        final Cursor log = cr.query(DataProvider.Logs.CONTENT_URI, DataProvider.Logs.PROJECTION,
                DataProvider.Logs.ID + " = ?", new String[]{String.valueOf(lid)}, null);
        if (log == null) {
            return;
        }
        if (!log.moveToFirst()) {
            Log.e(TAG, "no log: " + log);
            log.close();
            return;
        }
        final int t = log.getInt(DataProvider.Logs.INDEX_TYPE);
        p.checkBillday(log);
        final ContentValues cv = new ContentValues();
        cv.put(DataProvider.Logs.PLAN_ID, pid);
        final float ba = p.getBilledAmount(log);
        cv.put(DataProvider.Logs.BILL_AMOUNT, ba);
        final float bc = p.getCost(log, ba);
        cv.put(DataProvider.Logs.COST, bc);
        cv.put(DataProvider.Logs.FREE, p.getFree(log, bc));
        p.updatePlan(ba, bc, t);
        cr.update(DataProvider.Logs.CONTENT_URI, cv, DataProvider.Logs.ID + " = ?",
                new String[]{String.valueOf(lid)});
        log.close();
    }

    /**
     * Match all unmatched logs.
     *
     * @param context    {@link Context}
     * @param showStatus post status to dialog/handler
     * @return true if a log was matched
     */
    static synchronized boolean match(final Context context, final boolean showStatus) {
        Log.d(TAG, "match(ctx, ", showStatus, ")");
        long start = System.currentTimeMillis();
        boolean ret = false;
        load(context);
        final ContentResolver cr = context.getContentResolver();
        final Cursor cursor = cr.query(DataProvider.Logs.CONTENT_URI, DataProvider.Logs.PROJECTION,
                DataProvider.Logs.PLAN_ID + " = " + DataProvider.NO_ID, null,
                DataProvider.Logs.DATE + " ASC");
        if (cursor != null && cursor.moveToFirst()) {
            final int l = cursor.getCount();
            Handler h;
            if (showStatus) {
                h = Plans.getHandler();
                if (h != null) {
                    final Message m = h.obtainMessage(Plans.MSG_BACKGROUND_PROGRESS_MATCHER);
                    m.arg1 = 0;
                    m.arg2 = l;
                    m.sendToTarget();
                }
            }
            try {
                ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
                int i = 1;
                do {
                    ret |= matchLog(cr, ops, cursor);
                    if (i % PROGRESS_STEPS == 0 || (i < PROGRESS_STEPS && i % CallMeter.TEN == 0)) {
                        h = Plans.getHandler();
                        if (h != null) {
                            final Message m = h
                                    .obtainMessage(Plans.MSG_BACKGROUND_PROGRESS_MATCHER);
                            m.arg1 = i;
                            m.arg2 = l;
                            Log.d(TAG, "send progress: ", i, "/", l);
                            m.sendToTarget();
                        } else {
                            Log.d(TAG, "send progress: ", i, " handler=null");
                        }
                        Log.d(TAG, "save logs..");
                        cr.applyBatch(DataProvider.AUTHORITY, ops);
                        ops.clear();
                        Log.d(TAG, "sleeping..");
                        try {
                            Thread.sleep(CallMeter.MILLIS);
                        } catch (InterruptedException e) {
                            Log.e(TAG, "sleep interrupted", e);
                        }
                        Log.d(TAG, "sleep finished");
                    }
                    ++i;
                } while (cursor.moveToNext());
                if (ops.size() > 0) {
                    cr.applyBatch(DataProvider.AUTHORITY, ops);
                }
            } catch (IllegalStateException e) {
                Log.e(TAG, "illegal state in RuleMatcher's loop", e);
            } catch (OperationApplicationException e) {
                Log.e(TAG, "illegal operation in RuleMatcher's loop", e);
            } catch (RemoteException e) {
                Log.e(TAG, "remote exception in RuleMatcher's loop", e);
            }
        }
        try {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        } catch (IllegalStateException e) {
            Log.e(TAG, "illegal state while closing cursor", e);
        }

        if (ret) {
            final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
            final boolean a80 = p.getBoolean(Preferences.PREFS_ALERT80, true);
            final boolean a100 = p.getBoolean(Preferences.PREFS_ALERT100, true);
            // check for alerts
            if ((a80 || a100) && plans != null && plans.size() > 0) {
                final long now = System.currentTimeMillis();
                int alert = 0;
                Plan alertPlan = null;
                int l = plans.size();
                for (int i = 0; i < l; i++) {
                    final Plan plan = plans.valueAt(i);
                    if (plan == null) {
                        continue;
                    }
                    if (plan.nextAlert > now) {
                        Log.d(TAG, "%s: skip alert until: %d now=%d", plan, plan.nextAlert, now);
                        continue;
                    }
                    int used = DataProvider.Plans.getUsed(plan.type, plan.limitType,
                            plan.billedAmount, plan.billedCost);
                    int usedRate = plan.limit > 0 ?
                            (int) ((used * CallMeter.HUNDRED) / plan.limit)
                            : 0;
                    if (a100 && usedRate >= CallMeter.HUNDRED) {
                        alert = usedRate;
                        alertPlan = plan;
                    } else if (a80 && alert < CallMeter.EIGHTY && usedRate >= CallMeter.EIGHTY) {
                        alert = usedRate;
                        alertPlan = plan;
                    }
                }
                if (alert > 0) {
                    final NotificationManager mNotificationMgr = (NotificationManager) context
                            .getSystemService(Context.NOTIFICATION_SERVICE);
                    final String t = String.format(context.getString(R.string.alerts_message),
                            alertPlan.name, alert);
                    NotificationCompat.Builder b = new NotificationCompat.Builder(context);
                    b.setSmallIcon(android.R.drawable.stat_notify_error);
                    b.setTicker(t);
                    b.setWhen(now);
                    b.setContentTitle(context.getString(R.string.alerts_title));
                    b.setContentText(t);
                    Intent i = new Intent(context, Plans.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    b.setContentIntent(PendingIntent.getActivity(
                            context, 0, i, PendingIntent.FLAG_CANCEL_CURRENT));
                    mNotificationMgr.notify(0, b.build());
                    // set nextAlert to beginning of next day
                    Calendar cal = Calendar.getInstance();
                    cal.add(Calendar.DAY_OF_MONTH, 1);
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    alertPlan.nextAlert = cal.getTimeInMillis();
                    final ContentValues cv = new ContentValues();
                    cv.put(DataProvider.Plans.NEXT_ALERT, alertPlan.nextAlert);
                    cr.update(DataProvider.Plans.CONTENT_URI, cv, DataProvider.Plans.ID + " = ?",
                            new String[]{String.valueOf(alertPlan.id)});
                }
            }
        }
        long end = System.currentTimeMillis();
        Log.i(TAG, "match(): ", end - start, "ms");
        return ret;
    }
}
