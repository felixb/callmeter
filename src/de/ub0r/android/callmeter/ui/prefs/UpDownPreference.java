package de.ub0r.android.callmeter.ui.prefs;

import android.content.Context;
import android.preference.Preference;
import android.view.View;
import android.view.View.OnClickListener;
import de.ub0r.android.callmeter.R;

public class UpDownPreference extends Preference implements OnClickListener {
	static interface OnUpDownClickListener {
		void onUpDownClick(Preference preference, int direction);
	}

	private final OnUpDownClickListener mCallback;

	public UpDownPreference(final Context context, final OnUpDownClickListener callback) {
		super(context);
		this.setPersistent(false);
		this.mCallback = callback;
		if (callback != null) {
			this.setWidgetLayoutResource(R.layout.preference_widget_updown);
		}
	}

	@Override
	protected void onBindView(final View view) {
		super.onBindView(view);
		view.setOnClickListener(this);
		if (this.mCallback != null) {
			view.findViewById(R.id.button_up).setOnClickListener(this);
			view.findViewById(R.id.button_down).setOnClickListener(this);
		}
	}

	@Override
	public void onClick(final View v) {
		switch (v.getId()) {
		case R.id.button_up:
			this.mCallback.onUpDownClick(this, -1);
			return;
		case R.id.button_down:
			this.mCallback.onUpDownClick(this, 1);
			return;
		default:
			if (this.getOnPreferenceClickListener() != null) {
				this.getOnPreferenceClickListener().onPreferenceClick(this);
			}
			return;
		}
	}
}
