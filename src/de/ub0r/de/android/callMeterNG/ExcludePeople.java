package de.ub0r.de.android.callMeterNG;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

/**
 * Preferences subscreen to exclude numbers.
 * 
 * @author flx
 */
public class ExcludePeople extends Activity implements OnItemClickListener {
	/**
	 * {@inheritDoc}
	 */
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.exclude_people);
		final ListView lv = (ListView) this.findViewById(R.id.list);
		lv.setAdapter(CallMeter.excludedPeaoplAdapter);
		lv.setOnItemClickListener(this);
	}

	/**
	 * {@inheritDoc}
	 */
	protected final void onPause() {
		super.onPause();
		SharedPreferences.Editor editor = CallMeter.preferences.edit();
		final int s = CallMeter.prefsExcludePeople.size();
		editor.putInt(CallMeter.PREFS_EXCLUDE_PEOPLE_COUNT, s - 1);
		for (int i = 1; i < s; i++) {
			editor.putString(CallMeter.PREFS_EXCLUDE_PEOPLE_PREFIX + (i - 1),
					CallMeter.prefsExcludePeople.get(i));
		}
		editor.commit();
	}

	/**
	 * {@inheritDoc}
	 */
	public final void onItemClick(final AdapterView<?> parent, final View view,
			final int position, final long id) {
		if (position == 0) {
			final AlertDialog.Builder builder = new AlertDialog.Builder(this);
			final EditText et = new EditText(this);
			builder.setView(et);
			builder.setTitle(R.string.exclude_people_add);
			builder.setCancelable(true);
			builder.setPositiveButton(android.R.string.ok,
					new DialogInterface.OnClickListener() {
						public void onClick(final DialogInterface dialog,
								final int id) {
							CallMeter.prefsExcludePeople.add(et.getText()
									.toString());
							CallMeter.excludedPeaoplAdapter
									.notifyDataSetChanged();
							dialog.dismiss();
						}
					});
			builder.setNegativeButton(android.R.string.cancel,
					new DialogInterface.OnClickListener() {
						public void onClick(final DialogInterface dialog,
								final int id) {
							dialog.cancel();
						}
					});
			builder.create().show();
		} else {
			final AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setCancelable(true);
			final String[] itms = new String[2];
			itms[0] = this.getString(R.string.exclude_people_edit);
			itms[1] = this.getString(R.string.exclude_people_delete);
			builder.setItems(itms, new DialogInterface.OnClickListener() {
				public void onClick(final DialogInterface dialog, final int item) {
					if (item == 0) { // edit
						final AlertDialog.Builder builder2 = new AlertDialog.Builder(
								ExcludePeople.this);
						final EditText et = new EditText(ExcludePeople.this);
						et.setText(CallMeter.prefsExcludePeople.get(position));
						builder2.setView(et);
						builder2.setTitle(R.string.exclude_people_edit);
						builder2.setCancelable(true);
						builder2.setPositiveButton(android.R.string.ok,
								new DialogInterface.OnClickListener() {
									public void onClick(
											final DialogInterface dialog,
											final int id) {
										CallMeter.prefsExcludePeople.set(
												position, et.getText()
														.toString());
										CallMeter.excludedPeaoplAdapter
												.notifyDataSetChanged();
										dialog.dismiss();
									}
								});
						builder2.setNegativeButton(android.R.string.cancel,
								new DialogInterface.OnClickListener() {
									public void onClick(
											final DialogInterface dialog,
											final int id) {
										dialog.cancel();
									}
								});
						builder2.create().show();
					} else { // delete
						CallMeter.prefsExcludePeople.remove(position);
						CallMeter.excludedPeaoplAdapter.notifyDataSetChanged();
					}
				}

			});
			builder.create().show();
		}
	}
}
