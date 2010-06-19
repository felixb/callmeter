package de.ub0r.android.callmeter.ui;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.widget.ResourceCursorAdapter;
import de.ub0r.android.callmeter.data.DataProvider;

public class PlanAdapter extends ResourceCursorAdapter {

	public PlanAdapter(final Context context) {
		super(context, android.R.layout.simple_list_item_1, context
				.getContentResolver().query(DataProvider.Plans.CONTENT_URI,
						null, null, null, null), true);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void bindView(final View view, final Context context,
			final Cursor cursor) {
		// TODO Auto-generated method stub

	}
}
