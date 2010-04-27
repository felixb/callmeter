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

	/** {@link ContactsWrapper}. */
	static final ContactsWrapper CWRAPPER = ContactsWrapper.getInstance();

	/** Prefs: Exclude people prefix. */
	static final String PREFS_EXCLUDE_PEOPLE_PREFIX = "exclude_people_";
	/** Prefs: Exclude people count. */
	static final String PREFS_EXCLUDE_PEOPLE_COUNT = PREFS_EXCLUDE_PEOPLE_PREFIX
			+ "n";

	/** Prefs: excluded people, enable for calls. */
	static final String PREFS_EXCLUDE_PEOPLE_CALLS_ENABLE = // .
	PREFS_EXCLUDE_PEOPLE_PREFIX + "calls";
	/** Prefs: excluded people, enable for sms. */
	static final String PREFS_EXCLUDE_PEOPLE_SMS_ENABLE = // .
	PREFS_EXCLUDE_PEOPLE_PREFIX + "sms";

	/** Prefs: Bill calls to/from excluded people to plan #1. */
	static final String PREFS_EXCLUDE_PEOPLE_CALLS_PLAN1 = // .
	PREFS_EXCLUDE_PEOPLE_PREFIX + "to_plan1";
	/** Prefs: Bill calls to/from excluded people to plan #2. */
	static final String PREFS_EXCLUDE_PEOPLE_CALLS_PLAN2 = // .
	PREFS_EXCLUDE_PEOPLE_PREFIX + "to_plan2";
	/** Prefs: Bill sms to/from excluded people to plan #1. */
	static final String PREFS_EXCLUDE_PEOPLE_SMS_PLAN1 = // .
	PREFS_EXCLUDE_PEOPLE_PREFIX + "sms_to_plan1";
	/** Prefs: Bill sms to/from excluded people to plan #2. */
	static final String PREFS_EXCLUDE_PEOPLE_SMS_PLAN2 = // .
	PREFS_EXCLUDE_PEOPLE_PREFIX + "sms_to_plan2";

	/**
	 * Excluded person.
	 * 
	 * @author flx
	 */
	public static final class ExcludedPerson {
		/** Person not found. */
		private static final String NOT_FOUND = "##NOTFOUND##";

		/** {@link Context}. */
		private static Context context = null;

		/** Number. */
		private final String number;
		/** Name. */
		private String name = null;

		/**
		 * Default constructor.
		 * 
		 * @param num
		 *            number
		 */
		public ExcludedPerson(final String num) {
			this.number = num;
		}

		/**
		 * @return number
		 */
		public String getNumber() {
			return this.number;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String toString() {
			if (this.name == null && this.name != NOT_FOUND //
					&& context != null) {
				this.name = ContactsWrapper.getNameForAddress(context,
						this.number);
				if (this.name == null || this.name.equals(this.number)) {
					this.name = NOT_FOUND;
				}
			}
			if (this.name == null || this.name == NOT_FOUND) {
				return this.number;
			} else {
				return this.name + " <" + this.number + ">";
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.exclude_people);
		final ListView lv = (ListView) this.findViewById(R.id.list);
		lv.setAdapter(CallMeter.excludedPeopleAdapter);
		lv.setOnItemClickListener(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void onResume() {
		super.onResume();
		ExcludedPerson.context = this;
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
					CallMeter.prefsExcludePeople.get(i).getNumber());
		}
		editor.commit();
		ExcludedPerson.context = null;
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
		final String[] proj = CWRAPPER.getProjectionFilter();
		Cursor c = this.managedQuery(data.getData(), proj, null, null, null);
		if (!c.moveToFirst()) {
			return;
		}
		String number = c.getString(2).replaceAll("( |-|\\.|\\(|\\)|\\<|\\>)",
				"").trim();
		Log.d(TAG, number);
		if (requestCode == 0) {
			CallMeter.prefsExcludePeople.add(new ExcludedPerson(number));
		} else {
			CallMeter.prefsExcludePeople.set(requestCode, new ExcludedPerson(
					number));
		}
		CallMeter.excludedPeopleAdapter.notifyDataSetChanged();
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
							CallMeter.prefsExcludePeople
									.add(new ExcludedPerson(et.getText()
											.toString()));
							CallMeter.excludedPeopleAdapter
									.notifyDataSetChanged();
						}
					});
			builder.setNeutralButton(R.string.contacts_,
					new DialogInterface.OnClickListener() {
						public void onClick(final DialogInterface dialog,
								final int id) {
							final Intent intent = CWRAPPER.getPickPhoneIntent();
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
						et.setText(CallMeter.prefsExcludePeople.get(position)
								.getNumber());
						builder2.setView(et);
						builder2.setTitle(R.string.exclude_people_edit);
						builder2.setCancelable(true);
						builder2.setPositiveButton(android.R.string.ok,
								new DialogInterface.OnClickListener() {
									public void onClick(
											final DialogInterface dialog,
											final int id) {
										CallMeter.prefsExcludePeople.set(
												position, new ExcludedPerson(et
														.getText().toString()));
										CallMeter.excludedPeopleAdapter
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
						CallMeter.excludedPeopleAdapter.notifyDataSetChanged();
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
	static ArrayList<ExcludedPerson> loadExcludedPeople(final Context context) {
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);
		ArrayList<ExcludedPerson> ret = new ArrayList<ExcludedPerson>();
		ret.add(new ExcludedPerson(context
				.getString(R.string.exclude_people_add)));
		final int c = p.getInt(PREFS_EXCLUDE_PEOPLE_COUNT, 0);
		for (int i = 0; i < c; i++) {
			ret.add(new ExcludedPerson(p.getString(PREFS_EXCLUDE_PEOPLE_PREFIX
					+ i, "???")));
		}
		return ret;
	}
}
