package de.ub0r.de.android.callMeterNG;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Contacts.PhonesColumns;
import android.util.Log;
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
	/** Tag for output. */
	private static final String TAG = "CallMeterNG.ex";

	/** Prefs: Exclude people prefix. */
	static final String PREFS_EXCLUDE_PEOPLE_PREFIX = "exclude_people_";
	/** Prefs: Exclude people count. */
	static final String PREFS_EXCLUDE_PEOPLE_COUNT = PREFS_EXCLUDE_PEOPLE_PREFIX
			+ "n";
	/** Prefs: Bill excluded people to plan #1. */
	static final String PREFS_EXCLUDE_PEOPLE_PLAN1 = PREFS_EXCLUDE_PEOPLE_PREFIX
			+ "to_plan1";
	/** Prefs: Bill excluded people to plan #2. */
	static final String PREFS_EXCLUDE_PEOPLE_PLAN2 = PREFS_EXCLUDE_PEOPLE_PREFIX
			+ "to_plan2";

	/**
	 * {@inheritDoc}
	 */
	@Override
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
	@Override
	protected final void onPause() {
		super.onPause();
		SharedPreferences.Editor editor = PreferenceManager
				.getDefaultSharedPreferences(this).edit();
		final int s = CallMeter.prefsExcludePeople.size();
		editor.putInt(PREFS_EXCLUDE_PEOPLE_COUNT, s - 1);
		for (int i = 1; i < s; i++) {
			editor.putString(PREFS_EXCLUDE_PEOPLE_PREFIX + (i - 1),
					CallMeter.prefsExcludePeople.get(i));
		}
		editor.commit();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void onActivityResult(final int requestCode,
			final int resultCode, final Intent data) {
		Log.d(TAG, "requestCode: " + requestCode);
		Log.d(TAG, "resultCode: " + requestCode);
		if (data == null || data.getData() == null) {
			return;
		}
		Log.d(TAG, "data: " + data.getData().toString());
		// get number for uri
		Cursor c = this.managedQuery(data.getData(), null, null, null, null);
		if (!c.moveToFirst()) {
			return;
		}
		int i = c.getColumnIndex(PhonesColumns.NUMBER);
		if (i < 0) {
			Log.e(TAG, "switch to data1");
			i = c.getColumnIndex("data1");
		}
		if (i < 0) {
			Log.e(TAG, "unable to find row");
			return;
		}
		Log.e(TAG, "row: " + i);
		String number = c.getString(i).replace(" ", "").replace("-", "")
				.replace(".", "").replace("(", "").replace(")", "").replace(
						"<", "").replace(">", "").trim();
		Log.d(TAG, number);
		if (requestCode == 0) {
			CallMeter.prefsExcludePeople.add(number);
		} else {
			CallMeter.prefsExcludePeople.set(requestCode, number);
		}
		CallMeter.excludedPeaoplAdapter.notifyDataSetChanged();
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
						}
					});
			builder.setNeutralButton(R.string.contacts_,
					new DialogInterface.OnClickListener() {
						public void onClick(final DialogInterface dialog,
								final int id) {
							final Intent intent = // .
							new Intent(Intent.ACTION_GET_CONTENT);
							intent.setType("vnd.android.cursor.item/phone");
							ExcludePeople.this.startActivityForResult(intent,
									position);
						}
					});
			builder.setNegativeButton(android.R.string.cancel, null);
			builder.create().show();
		} else {
			final AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setCancelable(true);
			final String[] itms = new String[2];
			itms[0] = this.getString(R.string.exclude_people_edit);
			itms[1] = this.getString(R.string.exclude_people_delete);
			builder.setItems(itms, new DialogInterface.OnClickListener() {
				public void onClick(final DialogInterface dialog, // .
						final int item) {
					if (item == 0) { // edit
						final AlertDialog.Builder builder2 = // .
						new AlertDialog.Builder(ExcludePeople.this);
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
									}
								});
						builder2.setNeutralButton(R.string.contacts_,
								new DialogInterface.OnClickListener() {
									public void onClick(
											final DialogInterface dialog,
											final int id) {
										final Intent intent = // .
										new Intent(Intent.ACTION_GET_CONTENT);
										intent.setType("vnd.android"
												+ ".cursor.item/phone");
										ExcludePeople.this
												.startActivityForResult(intent,
														position);
									}
								});
						builder2.setNegativeButton(android.R.string.cancel,
								null);
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

	/**
	 * Load excluded people from preferences.
	 * 
	 * @param context
	 *            {@link Context}
	 * @return list of excluded people
	 */
	static ArrayList<String> loadExcludedPeople(final Context context) {
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);
		ArrayList<String> ret = new ArrayList<String>();
		ret.add(context.getString(R.string.exclude_people_add));
		final int c = p.getInt(PREFS_EXCLUDE_PEOPLE_COUNT, 0);
		for (int i = 0; i < c; i++) {
			ret.add(p.getString(PREFS_EXCLUDE_PEOPLE_PREFIX + i, "???"));
		}
		return ret;
	}
}
