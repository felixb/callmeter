package de.ub0r.android.callmeter.ui;

import java.util.Date;

import android.content.Context;
import android.database.Cursor;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import de.ub0r.android.callmeter.R;
import de.ub0r.android.callmeter.data.DataProvider;

/**
 * Adapter binding logs to View.
 * 
 * @author flx
 */
public class LogAdapter extends ResourceCursorAdapter {

	/**
	 * Default Constructor.
	 * 
	 * @param context
	 *            {@link Context}
	 */
	public LogAdapter(final Context context) {
		super(context, R.layout.logs_item, context.getContentResolver().query(
				DataProvider.Logs.CONTENT_URI, DataProvider.Logs.PROJECTION,
				null, null, DataProvider.Logs.DATE + " DESC"), true);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void bindView(final View view, final Context context,
			final Cursor cursor) {
		final StringBuilder buf = new StringBuilder();
		final int t = cursor.getInt(DataProvider.Logs.INDEX_TYPE);
		String[] strs = context.getResources().getStringArray(
				R.array.plans_type);
		buf.append(strs[t]);
		final int dir = cursor.getInt(DataProvider.Logs.INDEX_DIRECTION);
		strs = context.getResources().getStringArray(R.array.direction_calls);
		buf.append(" (" + strs[dir] + "): ");
		final long date = cursor.getLong(DataProvider.Logs.INDEX_DATE);
		buf.append(DateFormat.getDateFormat(context).format(new Date(date)));
		buf.append(" ");
		buf.append(DateFormat.getTimeFormat(context).format(new Date(date)));
		buf.append("\n");
		buf.append(cursor.getString(DataProvider.Logs.INDEX_REMOTE));
		buf.append("\t");
		buf.append(cursor.getString(DataProvider.Logs.INDEX_AMOUNT));

		((TextView) view.findViewById(android.R.id.text1)).setText(buf
				.toString());
	}
}
