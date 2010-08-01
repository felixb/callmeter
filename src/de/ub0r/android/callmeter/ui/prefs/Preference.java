package de.ub0r.android.callmeter.ui.prefs;

import java.util.Calendar;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.AlertDialog.Builder;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.database.Cursor;
import android.net.Uri;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import de.ub0r.android.callmeter.R;
import de.ub0r.android.lib.DbUtils;

/**
 * Hold Preferences saved to / loaded from a database.
 * 
 * @author flx
 */
abstract class Preference {
	/**
	 * Preference holding a {@link TextView}.
	 * 
	 * @author flx
	 */
	static final class TextPreference extends Preference {
		/** Default value. */
		private final String defaultValue;
		/** Current value. */
		private String value = null;
		/** {@link EditText} in {@link Dialog}. */
		private EditText etDialog = null;
		/** {@link EditText}'s inputType. */
		private final int iType;

		/**
		 * Default Constructor.
		 * 
		 * @param ctx
		 *            {@link Context}
		 * @param prefName
		 *            name of {@link Preference}
		 * @param defValue
		 *            default value of {@link Preference}
		 * @param text
		 *            resource id of the title text
		 * @param help
		 *            resource id of the help text
		 * @param inputType
		 *            {@link EditText}'s inputType
		 */
		protected TextPreference(final Context ctx, final String prefName,
				final String defValue, final int text, final int help,
				final int inputType) {
			super(ctx, prefName, R.layout.prefadapter_item, text, help);
			this.defaultValue = defValue;
			this.iType = inputType;
		}

		@Override
		void load(final Cursor cursor) {
			this.value = cursor.getString(cursor.getColumnIndex(this.name));
		}

		@Override
		void save(final ContentValues values) {
			values.put(this.name, this.value);

		}

		@Override
		Dialog createDialog() {
			final AlertDialog.Builder builder = new AlertDialog.Builder(
					this.context);
			builder.setCancelable(true);
			builder.setTitle(this.resText);
			final EditText et = new EditText(this.context);
			this.etDialog = et;
			et.setInputType(this.iType);
			et.setText(this.value);
			builder.setView(et);
			builder.setNegativeButton(android.R.string.cancel, null);
			builder.setNeutralButton(R.string.help_,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(final DialogInterface dialog,
								final int which) {
							TextPreference.this.showHelp();
						}
					});
			builder.setPositiveButton(android.R.string.ok,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(final DialogInterface dialog,
								final int which) {
							final String v = et.getText().toString();
							if (v != null && v.length() > 0) {
								TextPreference.this.value = v;
							} else {
								TextPreference.this.value = // .
								TextPreference.this.defaultValue;
							}
						}
					});
			return builder.create();
		}

		@Override
		void updateDialog(final Dialog d) {
			this.etDialog.setText(this.value);
		}

		@Override
		String getHint() {
			if (this.value != null && this.value.length() > 0) {
				return this.value;
			} else {
				return this.defaultValue;
			}
		}
	}

	/**
	 * Preference holding a list of values.
	 * 
	 * @author flx
	 */
	static final class ListPreference extends Preference {
		/** Default value. */
		private final int defaultValue;
		/** Current value. */
		private int value = -1;
		/** List of values. */
		private final String[] strValues;

		/**
		 * Default Constructor.
		 * 
		 * @param ctx
		 *            {@link Context}
		 * @param prefName
		 *            name of {@link Preference}
		 * @param defValue
		 *            default value of {@link Preference}
		 * @param text
		 *            resource id of the title text
		 * @param help
		 *            resource id of the help text
		 * @param values
		 *            resource id of the string array of values
		 */
		protected ListPreference(final Context ctx, final String prefName,
				final int defValue, final int text, final int help,
				final int values) {
			super(ctx, prefName, R.layout.prefadapter_item, text, help);
			this.defaultValue = defValue;
			this.strValues = ctx.getResources().getStringArray(values);
		}

		@Override
		void load(final Cursor cursor) {
			this.value = cursor.getInt(cursor.getColumnIndex(this.name));
		}

		@Override
		void save(final ContentValues values) {
			values.put(this.name, this.value);

		}

		@Override
		Dialog createDialog() {
			final AlertDialog.Builder builder = new AlertDialog.Builder(
					this.context);
			builder.setCancelable(true);
			builder.setTitle(this.resText);
			builder.setSingleChoiceItems(this.strValues, this.value,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(final DialogInterface dialog,
								final int which) {
							ListPreference.this.value = which;
							ListPreference.this.dismissDialog();
						}
					});
			builder.setNegativeButton(android.R.string.cancel, null);
			builder.setNeutralButton(R.string.help_,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(final DialogInterface dialog,
								final int which) {
							ListPreference.this.showHelp();
						}
					});
			return builder.create();
		}

		@Override
		void updateDialog(final Dialog d) {
			((AlertDialog) d).getListView().setSelection(this.value);
		}

		@Override
		String getHint() {
			if (this.value > -1) {
				return this.strValues[this.value];
			} else {
				return this.strValues[this.defaultValue];
			}
		}

		/**
		 * @return value
		 */
		public int getValue() {
			if (this.value > -1) {
				return this.value;
			} else {
				return this.defaultValue;
			}
		}
	}

	/**
	 * Preference holding a list of items from a cursor.
	 * 
	 * @author flx
	 */
	static final class CursorPreference extends Preference {
		/** Current value. */
		private long value = -1;
		/** Name of current value. */
		private String valueName = null;
		/** {@link Uri} to data. */
		private final Uri uri;
		/** Projection. 0: _id, 1: _name */
		private final String[] projection = new String[2];
		/** Selection for query. */
		private final String selection;

		/**
		 * Default Constructor.
		 * 
		 * @param ctx
		 *            {@link Context}
		 * @param prefName
		 *            name of {@link Preference}
		 * @param text
		 *            resource id of the title text
		 * @param help
		 *            resource id of the help text
		 * @param u
		 *            {@link Uri} to data
		 * @param id
		 *            id in projection
		 * @param name
		 *            name in projection
		 * @param sel
		 *            selection for query
		 */
		protected CursorPreference(final Context ctx, final String prefName,
				final int text, final int help, final Uri u, final String id,
				final String name, final String sel) {
			super(ctx, prefName, R.layout.prefadapter_item, text, help);
			this.uri = u;
			this.projection[0] = id;
			this.projection[1] = name;
			this.selection = sel;
		}

		@Override
		void load(final Cursor cursor) {
			this.value = cursor.getLong(cursor.getColumnIndex(this.name));
		}

		@Override
		void save(final ContentValues values) {
			values.put(this.name, this.value);

		}

		@Override
		Dialog createDialog() {
			final Cursor cursor = this.context.getContentResolver().query(
					this.uri, this.projection, this.selection, null,
					this.projection[1]);
			int sel = -1;
			int i = 0;
			if (cursor != null && cursor.moveToFirst() && this.value >= 0) {
				do {
					if (this.value == cursor.getLong(0)) {
						sel = i;
						break;
					}
					++i;
				} while (cursor.moveToNext());
			}
			final AlertDialog.Builder builder = new AlertDialog.Builder(
					this.context);
			builder.setCancelable(true);
			builder.setTitle(this.resText);

			builder.setSingleChoiceItems(cursor, sel, this.projection[1],
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(final DialogInterface dialog,
								final int which) {
							final ListView lv = ((AlertDialog) dialog)
									.getListView();
							CursorPreference.this.value = lv.getAdapter()
									.getItemId(which);
							CursorPreference.this.valueName = null;
							CursorPreference.this.dismissDialog();
						}
					});
			builder.setNegativeButton(android.R.string.cancel, null);
			builder.setNeutralButton(R.string.help_,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(final DialogInterface dialog,
								final int which) {
							CursorPreference.this.showHelp();
						}
					});
			final AlertDialog d = builder.create();
			return d;
		}

		@Override
		void updateDialog(final Dialog d) {
			final ListView lv = ((AlertDialog) d).getListView();
			final ListAdapter a = lv.getAdapter();
			final int l = a.getCount();
			for (int i = 0; i < l; i++) {
				if (this.value == a.getItemId(i)) {
					lv.setSelection(i);
					return;
				}
			}
		}

		@Override
		String getHint() {
			if (this.valueName == null) {
				Cursor cursor = this.context.getContentResolver().query(
						this.uri,
						this.projection,
						DbUtils.sqlAnd(this.selection, this.projection[0]
								+ " == " + this.value), null, null);
				if (cursor != null && cursor.moveToFirst()) {
					this.valueName = cursor.getString(1);
				}
				if (cursor != null && !cursor.isClosed()) {
					cursor.close();
				}
			}
			if (this.valueName == null) {
				this.valueName = this.context.getString(R.string.none);
			}
			return this.valueName;
		}

		/**
		 * @return value
		 */
		public long getValue() {
			return this.value;
		}
	}

	/**
	 * Preference holding a date.
	 * 
	 * @author flx
	 */
	static final class DatePreference extends Preference {
		/** Current value. */
		private final Calendar value = Calendar.getInstance();

		/**
		 * Default Constructor.
		 * 
		 * @param ctx
		 *            {@link Context}
		 * @param prefName
		 *            name of {@link Preference}
		 * @param text
		 *            resource id of the title text
		 * @param help
		 *            resource id of the help text
		 */
		protected DatePreference(final Context ctx, final String prefName,
				final int text, final int help) {
			super(ctx, prefName, R.layout.prefadapter_item, text, help);
		}

		@Override
		void load(final Cursor cursor) {
			this.value.setTimeInMillis(cursor.getLong(cursor
					.getColumnIndex(this.name)));
		}

		@Override
		void save(final ContentValues values) {
			values.put(this.name, this.value.getTimeInMillis());

		}

		@Override
		Dialog createDialog() {
			final DatePickerDialog d = new DatePickerDialog(this.context,
					new OnDateSetListener() {
						@Override
						public void onDateSet(final DatePicker view,
								final int year, final int monthOfYear,
								final int dayOfMonth) {
							DatePreference.this.value.set(year, monthOfYear,
									dayOfMonth);
						}
					}, this.value.get(Calendar.YEAR), this.value
							.get(Calendar.MONTH), this.value
							.get(Calendar.DAY_OF_MONTH));

			d.setCancelable(true);
			d.setTitle(this.resText);

			return d;
		}

		@Override
		void updateDialog(final Dialog d) {
			((DatePickerDialog) d).updateDate(this.value.get(Calendar.YEAR),
					this.value.get(Calendar.MONTH), this.value
							.get(Calendar.DAY_OF_MONTH));
		}

		@Override
		String getHint() {
			final String format = Preferences.getDateFormat(this.context);
			if (format == null) {
				return DateFormat.getDateFormat(this.context).format(
						this.value.getTime());
			} else {
				return String
						.format(format, this.value, this.value, this.value);
			}
		}

		/**
		 * @return value
		 */
		public Calendar getValue() {
			return this.value;
		}
	}

	/** {@link Context}. */
	protected final Context context;
	/** Inner {@link Dialog}. */
	private Dialog dialog = null;
	/** Resource id: Help text. */
	private final int resHelp;
	/** Resource id: Text. */
	protected final int resText;
	/** Resource id: Layout. */
	private final int resLayout;
	/** Name of {@link Preference}. */
	final String name;
	/** State of {@link Preference}. */
	private boolean hide = false;

	/**
	 * Default Constructor.
	 * 
	 * @param ctx
	 *            {@link Context}
	 * @param prefName
	 *            name of this {@link Preference}
	 * @param layout
	 *            layout to inflate
	 * @param text
	 *            resource id of the title text
	 * @param help
	 *            resource id of the help text
	 */
	protected Preference(final Context ctx, final String prefName,
			final int layout, final int text, final int help) {
		this.context = ctx;
		this.name = prefName;
		this.resLayout = layout;
		this.resText = text;
		this.resHelp = help;
	}

	/**
	 * Get a View that displays the data at the specified position in the data
	 * set. You can either create a View manually or inflate it from an XML
	 * layout file. When the View is inflated, the parent View (GridView,
	 * ListView...) will apply default layout parameters unless you use
	 * inflate(int, android.view.ViewGroup, boolean) to specify a root view and
	 * to prevent attachment to the root.
	 * 
	 * @param convertView
	 *            The old view to reuse, if possible. Note: You should check
	 *            that this view is non-null and of an appropriate type before
	 *            using. If it is not possible to convert this view to display
	 *            the correct data, this method can create a new view.
	 * @param parent
	 *            The parent that this view will eventually be attached to
	 * @return A View corresponding to the data at the specified position.
	 */
	final View getView(final View convertView, final ViewGroup parent) {
		final LayoutInflater inflater = (LayoutInflater) this.context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if (this.hide) {
			return new View(this.context);
		}
		// TODO: use convertView
		final View ret = inflater.inflate(this.resLayout, null);
		((TextView) ret.findViewById(android.R.id.text1)).setText(this.resText);
		((TextView) ret.findViewById(android.R.id.text2)).setText(this
				.getHint());
		return ret;
	}

	/**
	 * Load values from {@link Cursor}.
	 * 
	 * @param cursor
	 *            {@link Cursor}
	 */
	abstract void load(final Cursor cursor);

	/**
	 * Save inner values to {@link ContentValues}.
	 * 
	 * @param values
	 *            {@link ContentValues}
	 */
	abstract void save(final ContentValues values);

	/**
	 * @return hint shown under name
	 */
	abstract String getHint();

	/**
	 * Create a {@link Dialog}.
	 * 
	 * @return the {@link Dialog} to be shown
	 */
	abstract Dialog createDialog();

	/**
	 * Update the {@link Dialog}.
	 * 
	 * @param d
	 *            {@link Dialog} to be shown
	 */
	abstract void updateDialog(final Dialog d);

	/**
	 * Show the {@link Dialog}.
	 * 
	 * @param listener
	 *            {@link OnDismissListener}
	 */
	final void showDialog(final OnDismissListener listener) {
		if (this.dialog == null) {
			this.dialog = this.createDialog();
		} else {
			this.updateDialog(this.dialog);
		}
		if (this.dialog != null) {
			this.dialog.setOnDismissListener(listener);
			this.dialog.show();
		}
	}

	/** Dismiss the {@link Dialog}. */
	final void dismissDialog() {
		this.dialog.dismiss();
	}

	/**
	 * Hide/show preference.
	 * 
	 * @param h
	 *            true to hide the View
	 */
	final void hide(final boolean h) {
		this.hide = h;
	}

	/**
	 * @return true if {@link Preference} should be hidden.
	 */
	final boolean isHidden() {
		return this.hide;
	}

	/** Show help text. */
	protected final void showHelp() {
		final Builder b = new Builder(this.context);
		b.setMessage(this.resHelp);
		b.setPositiveButton(android.R.string.ok, null);
		b.show();
	}
}
