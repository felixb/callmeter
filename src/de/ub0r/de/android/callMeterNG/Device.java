/*
 * Copyright (C) 2010 Cyril Jaquier, Felix Bechstein
 * 
 * This file is part of NetCounter.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; If not, see <http://www.gnu.org/licenses/>.
 */

package de.ub0r.de.android.callMeterNG;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.os.Build;

/**
 * Representation of a device.
 */
public abstract class Device {
	/** Tag for output. */
	private static final String TAG = "device";

	/** Single instance. */
	private static Device instance = null;
	/** Device's interfaces. */
	private String[] mInterfaces = null;

	/**
	 * @return single instance
	 */
	public static synchronized Device getDevice() {
		Log.d(TAG, "Device: " + Build.DEVICE);
		if (instance == null) {
			Log.i(TAG, "Device: " + Build.DEVICE);
			Log.d(TAG, "DEBUG: " + debugDeviceList());
			// All the devices we know about.
			Device[] allDevices = { new DefaultDevice(), new GenericDevice(),
					new SamsungI7500Device(), new PulseDevice(),
					new DroidDevice(), new EveDevice() };
			// Iterates over all the devices and try to found the corresponding
			// one.
			for (Device device : allDevices) {
				if (Arrays.asList(device.getNames()).contains(Build.DEVICE)) {
					instance = device;
					break;
				}
			}
			// Nothing found? Use the default device.
			if (instance == null) {
				instance = allDevices[0];
			}
		}
		Log.d(TAG, instance.getClass().getName());
		return instance;
	}

	/**
	 * Read a {@link File} an return its name+its first line.
	 * 
	 * @param f
	 *            filename
	 * @return name + \n + 1st line
	 */
	private static String readFile(final String f) {
		StringBuilder sb = new StringBuilder();
		try {
			BufferedReader r = new BufferedReader(new FileReader(f), 8);
			sb.append("read: " + f);
			sb.append("\n");
			sb.append(r.readLine());
			r.close();
		} catch (IOException e) {
			Log.e(TAG, "error reading file: " + f, e);
		}
		return sb.toString();
	}

	/**
	 * Get a list of possible devices.
	 * 
	 * @return some info
	 */
	private static String debugDeviceList() {
		try {
			File f = new File(SysClassNet.SYS_CLASS_NET);
			String[] devices = f.list();
			for (String d : devices) {
				String dev = SysClassNet.SYS_CLASS_NET + d;
				Log.i(TAG, readFile(dev + "/type"));
				Log.i(TAG, readFile(dev + SysClassNet.CARRIER));
				Log.i(TAG, readFile(dev + SysClassNet.RX_BYTES));
				Log.i(TAG, readFile(dev + SysClassNet.TX_BYTES));
			}
		} catch (Exception e) {
			Log.e(TAG, "error reading /sys/", e);
		}
		return Build.PRODUCT + ":" + Build.MODEL;
	}

	/**
	 * @return device's names
	 */
	public abstract String[] getNames();

	/**
	 * @return device's device file: cell
	 */
	public abstract String getCell();

	/**
	 * @return device's device file: wifi
	 */
	public abstract String getWiFi();

	/**
	 * @return device's device file: bluetooth
	 */
	public abstract String getBluetooth();

	/**
	 * @return device's interfaces
	 */
	public final synchronized String[] getInterfaces() {
		if (this.mInterfaces == null) {
			List<String> tmp = new ArrayList<String>();
			if (this.getCell() != null) {
				tmp.add(this.getCell());
			}
			if (this.getWiFi() != null) {
				tmp.add(this.getWiFi());
			}
			if (this.getBluetooth() != null) {
				tmp.add(this.getBluetooth());
			}
			this.mInterfaces = tmp.toArray(new String[tmp.size()]);
		}
		return this.mInterfaces;
	}
}

/**
 * Generic device implementation corresponding to the emulator.
 */
class GenericDevice extends Device {
	@Override
	public String[] getNames() {
		return new String[] { "generic" };
	}

	@Override
	public String getBluetooth() {
		return null;
	}

	@Override
	public String getCell() {
		return this.getWiFi(); // for debugging purpose
	}

	@Override
	public String getWiFi() {
		return "eth0";
	}
}

/**
 * Default device implementation corresponding to the HTC Dream and HTC Magic.
 */
class DefaultDevice extends Device {
	@Override
	public String[] getNames() {
		// TODO Get the device name of the HTC Magic.
		return new String[] { "dream" };
	}

	@Override
	public String getBluetooth() {
		return "bnep0";
	}

	@Override
	public String getCell() {
		return "rmnet0";
	}

	@Override
	public String getWiFi() {
		return "tiwlan0";
	}
}

/**
 * Device implementation for the Samsung I7500. Also works with the I5700
 * (Spica).
 */
class SamsungI7500Device extends Device {
	@Override
	public String[] getNames() {
		return new String[] { "GT-I7500", "spica", "GT-I5700" };
	}

	@Override
	public String getBluetooth() {
		return "bnep0";
	}

	@Override
	public String getCell() {
		return "pdp0";
	}

	@Override
	public String getWiFi() {
		return "eth0";
	}
}

/**
 * Device implementation for the T-Mobile Pulse (Huawei U8220). Also works for
 * the Google Nexus One.
 */
class PulseDevice extends Device {
	@Override
	public String[] getNames() {
		return new String[] { "U8220", "passion" };
	}

	@Override
	public String getBluetooth() {
		return "bnep0";
	}

	@Override
	public String getCell() {
		return "rmnet0";
	}

	@Override
	public String getWiFi() {
		return "eth0";
	}
}

/**
 * Device implementation for the Motorola Droid.
 */
class DroidDevice extends Device {
	@Override
	public String[] getNames() {
		return new String[] { "sholes" };
	}

	@Override
	public String getBluetooth() {
		return "bnep0";
	}

	@Override
	public String getCell() {
		return "ppp0";
	}

	@Override
	public String getWiFi() {
		return "tiwlan0";
	}
}

/**
 * Device implementation for the LG Eve Android GW620R.
 */
class EveDevice extends Device {
	@Override
	public String[] getNames() {
		return new String[] { "EVE" };
	}

	@Override
	public String getBluetooth() {
		return "bnep0";
	}

	@Override
	public String getCell() {
		return "rmnet0";
	}

	@Override
	public String getWiFi() {
		return "wlan0";
	}
}
