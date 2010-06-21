package de.ub0r.android.callmeter.ui.prefs;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
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

	/** Type of plans. */
	private final String[] types;

	/** Id of item which is in settings mode. */
	private long edit = -1;

	/**
	 * Default Constructor.
	 * 
	 * @param context
	 *            {@link Context}
	 */
	public PlanAdapter(final Context context) {
		super(context, R.layout.prefs_plans_item, context.getContentResolver()
				.query(DataProvider.Plans.CONTENT_URI,
						DataProvider.Plans.PROJECTION, null, null,
						DataProvider.Plans.ORDER), true);
		this.types = context.getResources().getStringArray(R.array.plans_type);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void bindView(final View view, final Context context,
			final Cursor cursor) {
		final int t = cursor.getInt(DataProvider.Plans.INDEX_TYPE);
		final int id = cursor.getInt(DataProvider.Plans.INDEX_ID);
		final String title = cursor.getString(DataProvider.Plans.INDEX_NAME);
		final int limit = cursor.getInt(DataProvider.Plans.INDEX_LIMIT);

		final TextView twTitle = ((TextView) view.findViewById(R.id.normtitle));
		twTitle.setText(title);
		final TextView twType = ((TextView) view.findViewById(R.id.type));
		twType.setText(this.types[t]);
		View settings = view.findViewById(R.id.settings);
		if (id == this.edit) {
			settings.setVisibility(View.VISIBLE);
		} else {
			settings.setVisibility(View.GONE);
		}
	}

	/**
	 * Set item in edit mode.
	 * 
	 * @param id
	 *            id of item.
	 */
	final void setEdit(final long id) {
		this.edit = id;
		this.notifyDataSetChanged();
	}
}
