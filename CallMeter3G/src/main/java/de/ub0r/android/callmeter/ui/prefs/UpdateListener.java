package de.ub0r.android.callmeter.ui.prefs;

import android.preference.Preference;

/**
 * Interface for Activities listening for updates in non-persistent {@link Preference}s.
 *
 * @author flx
 */
public interface UpdateListener {

    /**
     * Value was updated.
     *
     * @param p {@link Preference}
     */
    void onUpdateValue(Preference p);

    /**
     * The default value was set.
     *
     * @param p     {@link Preference}
     * @param value new value
     */
    void onSetDefaultValue(Preference p, Object value);
}
