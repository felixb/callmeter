/*
 * Copyright (C) 2009-2011 Felix Bechstein
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

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.ActivityNotFoundException;
import android.content.AsyncQueryHandler;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.support.v4.view.Menu;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ProgressBar;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import de.ub0r.android.callmeter.CallMeter;
import de.ub0r.android.callmeter.R;
import de.ub0r.android.callmeter.data.DataProvider;
import de.ub0r.android.callmeter.ui.prefs.PlanEdit;
import de.ub0r.android.callmeter.ui.prefs.Preferences;
import de.ub0r.android.lib.Log;

/**
 * Show plans.
 * 
 * @author flx
 */
public final class PlansFragment extends ListFragment implements
		OnClickListener, OnItemLongClickListener {
	/** Tag for output. */
	private static final String TAG = "plans";
	/** Run the dummy? */
	private static boolean doDummy = true;

	/**
	 * Adapter binding plans to View.
	 * 
	 * @author flx
	 */
	private static class PlansAdapter extends ResourceCursorAdapter {
		/** {@link BackgroundQueryHandler}. */
		private final BackgroundQueryHandler queryHandler;
		/** Token for {@link BackgroundQueryHandler}: plan summary query. */
		private static final int PLAN_QUERY_TOKEN = 0;
		/** Token for {@link BackgroundQueryHandler}: plan dummy query. */
		private static final int PLAN_QUERY_TOKEN_DUMMY = 1;

		/** {@link SharedPreferences}. */
		private final SharedPreferences p;
		/** {@link Editor}. */
		private final Editor e;
		/** Does the {@link Editor} needs commit? */
		private boolean isDirty = false;

		/** Now. */
		private long now;

		/**
		 * Handle queries in background.
		 * 
		 * @author flx
		 */
		private final class BackgroundQueryHandler extends AsyncQueryHandler {
			/** Reference to {@link Activity}. */
			private final Activity activity;

			/** Handle to views. */
			private final View vLoading, vImport;
			/** Ignore query requests. */
			private boolean ignoreQuery = false;

			/**
			 * A helper class to help make handling asynchronous queries easier.
			 * 
			 * @param context
			 *            {@link Activity}
			 * @param loading
			 *            Handle to view: loading.
			 * @param imp
			 *            Handle to view: import
			 */
			public BackgroundQueryHandler(final Activity context,
					final View loading, final View imp) {
				super(context.getContentResolver());
				this.activity = context;
				this.vLoading = loading;
				this.vImport = imp;
			}

			/**
			 * {@inheritDoc}
			 */
			@Override
			protected void onQueryComplete(final int token,
					final Object cookie, final Cursor cursor) {
				ignoreQuery = false;
				switch (token) {
				case PLAN_QUERY_TOKEN_DUMMY:
				case PLAN_QUERY_TOKEN:
					PlansAdapter.this.save();
					this.vLoading.setVisibility(View.GONE);
					PlansAdapter.this.changeCursor(cursor);
					if (cursor != null && cursor.getCount() > 0) {
						this.vImport.setVisibility(View.GONE);
					} else {
						this.vImport.setVisibility(View.VISIBLE);
					}
					((Plans) this.activity).setInProgress(-1);
					return;
				default:
					return;
				}
			}

			/**
			 * {@inheritDoc}
			 */
			@Override
			public void startQuery(final int token, final Object cookie,
					final Uri uri, final String[] projection,
					final String selection, final String[] selectionArgs,
					final String orderBy) {
				if (this.ignoreQuery) {
					return;
				}
				if (token == PLAN_QUERY_TOKEN_DUMMY) {
					this.ignoreQuery = true;
				}

				if (PlansAdapter.this.getCount() == 0) {
					vLoading.setVisibility(View.VISIBLE);
				}

				((Plans) this.activity).setInProgress(1);
				super.startQuery(token, cookie, uri, projection, selection,
						selectionArgs, orderBy);
			}
		}

		/** Text sizes. */
		private static int textSize, textSizeBigTitle, textSizeTitle,
				textSizeSpacer, textSizePBar, textSizePBarBP;

		/** Separator for the data. */
		private static String delimiter = " | ";
		/** Selected currency format. */
		private static String currencyFormat = "$%.2f";
		/** Show hours and days. */
		private static boolean pShowHours = true;
		/** Show today stats. */
		private static boolean showToday = false;
		/** Show total stats. */
		private static boolean showTotal = true;
		/** Hide zero plans. */
		private static boolean hideZero = false;
		/** Hide no cost plans. */
		private static boolean hideNoCost = false;

		/** Prepaid plan? */
		private static boolean prepaid;

		/** Visibility for {@link ProgressBar}s. */
		private final int progressBarVisability;

		/** Need a reload of preferences. */
		private static boolean needReloadPrefs = true;

		/**
		 * Reload preferences.
		 * 
		 * @param context
		 *            {@link Context}
		 * @param force
		 *            force reloading
		 */
		static void reloadPreferences(final Context context, // .
				final boolean force) {
			if (!force && !needReloadPrefs) {
				return;
			}
			Common.setDateFormat(context);
			final SharedPreferences p = PreferenceManager
					.getDefaultSharedPreferences(context);
			pShowHours = p.getBoolean(Preferences.PREFS_SHOWHOURS, true);
			currencyFormat = Preferences.getCurrencyFormat(context);
			showToday = p.getBoolean(Preferences.PREFS_SHOWTODAY, false);
			showTotal = p.getBoolean(Preferences.PREFS_SHOWTOTAL, true);
			hideZero = p.getBoolean(Preferences.PREFS_HIDE_ZERO, false);
			hideNoCost = p.getBoolean(Preferences.PREFS_HIDE_NOCOST, false);
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
		 * @param context
		 *            {@link Activity}
		 * @param loading
		 *            Handle to view: loading.
		 * @param imp
		 *            Handle to view: import
		 */
		public PlansAdapter(final Activity context, final View loading,
				final View imp) {
			super(context, R.layout.plans_item, null, true);
			queryHandler = new BackgroundQueryHandler(context, loading, imp);
			this.p = PreferenceManager.getDefaultSharedPreferences(context);
			this.e = this.p.edit();
			if (this.p.getBoolean(Preferences.PREFS_HIDE_PROGRESSBARS, false)) {
				this.progressBarVisability = View.GONE;
			} else {
				this.progressBarVisability = View.VISIBLE;
			}
		}

		/**
		 * Start ConversationList query.
		 * 
		 * @param lnow
		 *            current time shown
		 * @param dummy
		 *            use dummy query
		 */
		public final void startPlanQuery(final long lnow, final boolean dummy) {
			Log.d(TAG, "startPlanQuery(" + lnow + ")");
			this.now = lnow;
			// Cancel any pending queries
			if (dummy) {
				this.queryHandler.cancelOperation(PLAN_QUERY_TOKEN_DUMMY);
			} else {
				this.queryHandler.cancelOperation(PLAN_QUERY_TOKEN);
			}
			try {
				// Kick off the new query
				if (dummy) {
					this.queryHandler.startQuery(PLAN_QUERY_TOKEN_DUMMY, null,
							DataProvider.Plans.CONTENT_URI,
							DataProvider.Plans.PROJECTION, null, null, null);
				} else {
					this.queryHandler
							.startQuery(
									PLAN_QUERY_TOKEN,
									null,
									DataProvider.Plans.CONTENT_URI_SUM
											.buildUpon()
											.appendQueryParameter(
													DataProvider.// .
													Plans.PARAM_DATE,
													String.valueOf(lnow))
											.appendQueryParameter(
													DataProvider.// .
													Plans.PARAM_HIDE_ZERO,
													String.valueOf(hideZero))
											.appendQueryParameter(
													DataProvider.// .
													Plans.PARAM_HIDE_NOCOST,
													String.valueOf(hideNoCost))
											.appendQueryParameter(
													DataProvider.// .
													Plans.PARAM_HIDE_TODAY,
													String.valueOf(!showToday
															|| lnow >= 0L))
											.appendQueryParameter(
													DataProvider.// .
													Plans.PARAM_HIDE_ALLTIME,
													String.valueOf(!showTotal))
											.build(),
									DataProvider.Plans.PROJECTION_SUM, null,
									null, null);
				}
			} catch (SQLiteException e) {
				Log.e(TAG, "error starting query", e);
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void bindView(final View view, final Context context,
				final Cursor cursor) {
			boolean savePlan = false;
			DataProvider.Plans.Plan plan = null;
			if (cursor.getColumnIndex(DataProvider.Plans.SUM_COST) > 0) {
				plan = new DataProvider.Plans.Plan(cursor);
				savePlan = true;
			} else {
				plan = new DataProvider.Plans.Plan(cursor, this.p);
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

			if (plan.type != DataProvider.TYPE_SPACING // .
					&& plan.type != DataProvider.TYPE_TITLE) {
				spb.append(Common.formatValues(context, plan.now, plan.type,
						plan.bpCount, plan.bpBa, plan.billperiod, plan.billday,
						pShowHours));
				spb.setSpan(new StyleSpan(Typeface.BOLD), 0, spb.length(),
						Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				if (plan.type != DataProvider.TYPE_BILLPERIOD) {
					if (showTotal) {
						spb.append(delimiter
								+ Common.formatValues(context, plan.now,
										plan.type, plan.atCount, plan.atBa,
										plan.billperiod, plan.billday,
										pShowHours));
					}
					if (showToday) {
						spb.insert(
								0,
								Common.formatValues(context, plan.now,
										plan.type, plan.tdCount, plan.tdBa,
										plan.billperiod, plan.billday,
										pShowHours)
										+ delimiter);
					}
				}
				if (free > 0f || cost > 0f) {
					spb.append("\n");
					if (free > 0f) {
						spb.append("(" + String.format(currencyFormat, free)
								+ ")");
					}
					if (cost > 0f) {
						spb.append(" " + String.format(currencyFormat, cost));
					}
				}
				if (plan.limit > 0) {
					spb.insert(0, ((int) (plan.usage * CallMeter.HUNDRET))
							+ "%" + delimiter);
				}
			}

			// Log.d(TAG, "plan id: " + plan.id);
			// Log.d(TAG, "plan name: " + plan.name);
			// Log.d(TAG, "type: " + plan.type);
			// Log.d(TAG, "cost: " + cost);
			// Log.d(TAG, "limit: " + plan.limit);
			// Log.d(TAG, "limitPos: " + plan.limitPos);
			// Log.d(TAG, "text: " + spb);

			TextView twCache = null;
			ProgressBar pbCache = null;
			if (plan.type == DataProvider.TYPE_SPACING) {
				final View v = view.findViewById(R.id.spacer);
				if (textSizeSpacer > 0) {
					final LayoutParams lp = v.getLayoutParams();
					lp.height = textSizeSpacer;
					v.setLayoutParams(lp);
				}
				v.setVisibility(View.INVISIBLE);
				view.findViewById(R.id.bigtitle).setVisibility(View.GONE);
				view.findViewById(R.id.content).setVisibility(View.GONE);
				view.findViewById(R.id.period_layout).setVisibility(View.GONE);
			} else if (plan.type == DataProvider.TYPE_TITLE) {
				final TextView tw = ((TextView) view
						.findViewById(R.id.bigtitle));
				tw.setText(cursor.getString(DataProvider.Plans.INDEX_NAME));
				if (textSizeBigTitle > 0) {
					tw.setTextSize(textSizeBigTitle);
				}
				tw.setVisibility(View.VISIBLE);
				view.findViewById(R.id.spacer).setVisibility(View.GONE);
				view.findViewById(R.id.content).setVisibility(View.GONE);
				view.findViewById(R.id.period_layout).setVisibility(View.GONE);
			} else if (plan.type == DataProvider.TYPE_BILLPERIOD) {
				view.findViewById(R.id.bigtitle).setVisibility(View.GONE);
				view.findViewById(R.id.spacer).setVisibility(View.GONE);
				view.findViewById(R.id.content).setVisibility(View.GONE);
				view.findViewById(R.id.period_layout).setVisibility(
						View.VISIBLE);
				twCache = (TextView) view.findViewById(R.id.period);
				pbCache = (ProgressBar) view.findViewById(R.id.period_pb);
			} else {
				view.findViewById(R.id.bigtitle).setVisibility(View.GONE);
				view.findViewById(R.id.spacer).setVisibility(View.GONE);
				view.findViewById(R.id.period_layout).setVisibility(View.GONE);
				view.findViewById(R.id.content).setVisibility(View.VISIBLE);
				final TextView twNormTitle = (TextView) view
						.findViewById(R.id.normtitle);
				if (textSizeTitle > 0) {
					twNormTitle.setTextSize(textSizeTitle);
				}
				twNormTitle.setText(cursor
						.getString(DataProvider.Plans.INDEX_NAME));
				twCache = (TextView) view.findViewById(R.id.data);
				if (plan.limit > 0) {
					float bpos = plan.getBillPlanUsage();
					if (plan.usage >= 1) {
						pbCache = (ProgressBar) view
								.findViewById(R.id.progressbarLimitRed);
						view.findViewById(R.id.progressbarLimitGreen)
								.setVisibility(View.GONE);
						view.findViewById(R.id.progressbarLimitYellow)
								.setVisibility(View.GONE);
					} else if (bpos >= 0f && plan.usage > bpos) {
						pbCache = (ProgressBar) view
								.findViewById(R.id.progressbarLimitYellow);
						view.findViewById(R.id.progressbarLimitGreen)
								.setVisibility(View.GONE);
						view.findViewById(R.id.progressbarLimitRed)
								.setVisibility(View.GONE);
					} else {
						pbCache = (ProgressBar) view
								.findViewById(R.id.progressbarLimitGreen);
						view.findViewById(R.id.progressbarLimitYellow)
								.setVisibility(View.GONE);
						view.findViewById(R.id.progressbarLimitRed)
								.setVisibility(View.GONE);
					}
				} else {
					pbCache = (ProgressBar) view
							.findViewById(R.id.progressbarLimitYellow);
					view.findViewById(R.id.progressbarLimitGreen)
							.setVisibility(View.GONE);
					view.findViewById(R.id.progressbarLimitRed).setVisibility(
							View.GONE);
				}
			}
			if (twCache != null && pbCache != null) {
				if (spb.length() > 0) {
					twCache.setText(spb);
				} else {
					twCache.setText(null);
				}
				if (textSize > 0) {
					twCache.setTextSize(textSize);
				}
				if (plan.limit == 0
						&& plan.type == DataProvider.TYPE_BILLPERIOD) {
					pbCache.setIndeterminate(true);
					pbCache.setVisibility(View.VISIBLE);
				} else if (plan.limit == 0) {
					// plan.type != DataProvider.TYPE_BILLPERIOD
					pbCache.setVisibility(View.GONE);
				} else if (plan.limit > 0) {
					pbCache.setIndeterminate(false);
					pbCache.setMax((int) plan.limit);
					pbCache.setProgress((int) plan.limitPos);
					pbCache.setVisibility(this.progressBarVisability);
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
					pbCache.setVisibility(this.progressBarVisability);
				}
			}
			if (savePlan && this.now < 0L
					&& plan.type != DataProvider.TYPE_SPACING
					&& plan.type != DataProvider.TYPE_TITLE) {
				plan.save(this.e);
				this.isDirty = true;
			}
		}

		/**
		 * Save current stats to {@link SharedPreferences}.
		 */
		public void save() {
			if (this.isDirty) {
				Log.d(TAG, "e.commit()");
				e.commit();
				this.isDirty = false;
			}
		}
	}

	/** This fragments time stamp. */
	private long now;

	/** Handle for view. */
	private View vLoading, vImport;

	/**
	 * Get new {@link PlansFragment}.
	 * 
	 * @param now
	 *            This fragments current time
	 * @return {@link PlansFragment}
	 */
	public static PlansFragment newInstance(final long now) {
		PlansFragment f = new PlansFragment();
		Bundle args = new Bundle();
		args.putLong("now", now);
		f.setArguments(args);
		return f;
	}

	/**
	 * Force reloading preferences.
	 * 
	 * @param context
	 *            {@link Context}
	 */
	static void reloadPreferences(final Context context) {
		PlansAdapter.reloadPreferences(context, true);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setHasOptionsMenu(true);
		if (this.getArguments() == null) {
			this.now = -1L;
		} else {
			this.now = this.getArguments().getLong("now", -1L);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onPause() {
		super.onPause();
		((PlansAdapter) this.getListAdapter()).save();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public View onCreateView(final LayoutInflater inflater,
			final ViewGroup container, final Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.plans_fragment, container, false);
		this.vLoading = v.findViewById(R.id.loading);
		this.vImport = v.findViewById(R.id.import_default);
		this.vImport.setOnClickListener(this);
		return v;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onActivityCreated(final Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		PlansAdapter.reloadPreferences(this.getActivity(), false);
		PlansAdapter adapter = new PlansAdapter(this.getActivity(),
				this.vLoading, this.vImport);
		this.setListAdapter(adapter);
		if (doDummy && this.now < 0L) {
			doDummy = false;
			adapter.startPlanQuery(this.now, true);
		}
		this.getListView().setOnItemLongClickListener(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onStop() {
		super.onStop();
		if (this.now < 0L) {
			this.doDummy = true;
		}
	}

	/**
	 * Re-query database.
	 * 
	 * @param forceUpdate
	 *            force update
	 */
	public void requery(final boolean forceUpdate) {
		Log.d(TAG, "requery(" + forceUpdate + ")");
		PlansAdapter adapter = (PlansAdapter) this.getListAdapter();
		if (forceUpdate || adapter.isEmpty()) {
			adapter.startPlanQuery(this.now, false);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreateOptionsMenu(final Menu menu, // .
			final MenuInflater inflater) {
		inflater.inflate(R.menu.menu_plans, menu);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onClick(final View v) {
		switch (v.getId()) {
		case R.id.import_default:
			final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(this
					.getString(R.string.url_rulesets)));
			try {
				this.startActivity(intent);
			} catch (ActivityNotFoundException e) {
				Log.e(TAG, "no activity to load url", e);
				Toast.makeText(this.getActivity(),
						"no activity to load url: " + intent.getDataString(),
						Toast.LENGTH_LONG).show();
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
	public boolean onItemLongClick(final AdapterView<?> parent,
			final View view, final int position, final long id) {
		final Builder builder = new Builder(this.getActivity());
		builder.setItems(R.array.dialog_edit_plan,
				new android.content.DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog,
							final int which) {
						Intent intent = null;
						switch (which) {
						case 0:
							intent = new Intent(PlansFragment.this
									.getActivity(), PlanEdit.class);
							intent.setData(ContentUris.withAppendedId(
									DataProvider.Plans.CONTENT_URI, id));
							PlansFragment.this.getActivity().startActivity(
									intent);
							break;
						case 1:
							((Plans) PlansFragment.this.getActivity())
									.showLogsFragment(id);
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
}
