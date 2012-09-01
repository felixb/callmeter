package de.ub0r.android.callmeter.ui.prefs;

import android.preference.Preference;

/**
 * Interface for Activities listening for updates in non-persistent
 * {@link Preference}s.
 * 
 * @author flx
 */
public interface UpdateListener {
	/** Value was updated. */
	void onUpdateValue(Preference p);
}
