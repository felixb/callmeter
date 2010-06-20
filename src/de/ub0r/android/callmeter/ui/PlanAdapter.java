package de.ub0r.android.callmeter.ui;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import de.ub0r.android.callmeter.R;
import de.ub0r.android.callmeter.data.DataProvider;

/**
 * Adapter binding plans to View.
 * 
 * @author flx
 */
public class PlanAdapter extends ResourceCursorAdapter {

	/**
	 * Default Constructor.
	 * 
	 * @param context
	 *            {@link Context}
	 */
	public PlanAdapter(final Context context) {
		super(context, R.layout.plans_item, context.getContentResolver().query(
				DataProvider.Plans.CONTENT_URI, DataProvider.Plans.PROJECTION,
				null, null, DataProvider.Plans.ORDER), true);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void bindView(final View view, final Context context,
			final Cursor cursor) {
		final int t = cursor.getInt(DataProvider.Plans.INDEX_TYPE);
		if (t == DataProvider.TYPE_SPACING) {
			view.findViewById(R.id.bigtitle).setVisibility(View.INVISIBLE);
			view.findViewById(R.id.content).setVisibility(View.GONE);
		} else if (t == DataProvider.TYPE_TITLE) {
			final TextView tw = ((TextView) view.findViewById(R.id.bigtitle));
			tw.setText(cursor.getString(DataProvider.Plans.INDEX_NAME));
			tw.setVisibility(View.VISIBLE);
			view.findViewById(R.id.content).setVisibility(View.GONE);
		} else {
			view.findViewById(R.id.bigtitle).setVisibility(View.GONE);
			view.findViewById(R.id.content).setVisibility(View.VISIBLE);
			((TextView) view.findViewById(R.id.normtitle)).setText(cursor
					.getString(DataProvider.Plans.INDEX_NAME));
			final int limit = cursor.getInt(DataProvider.Plans.INDEX_LIMIT);
			final int usedMonth = cursor
					.getInt(DataProvider.Plans.INDEX_USED_MONTH);
			if (limit > 0) {
				final ProgressBar pb = (ProgressBar) view
						.findViewById(R.id.progressbarLimit);
				pb.setMax(limit);
				pb.setProgress(usedMonth);
				pb.setVisibility(View.VISIBLE);
			} else {
				view.findViewById(R.id.progressbarLimit).setVisibility(
						View.INVISIBLE);
			}
			final int usedAll = cursor
					.getInt(DataProvider.Plans.INDEX_USED_ALL);
			final int usedCount = cursor
					.getInt(DataProvider.Plans.INDEX_USED_COUNT);
			final float usedCost = cursor
					.getFloat(DataProvider.Plans.INDEX_COST);
			// TODO: print data to screen
		}
	}
}
