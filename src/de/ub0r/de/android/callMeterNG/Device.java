/*
 * Copyright (C) 2009-2011 Cyril Jaquier, Felix Bechstein
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
import java.util.List;

import android.os.Build;
import de.ub0r.android.lib.Log;

/**
 * Representation of a device.
 */
public abstract class Device {
	/** Tag for output. */
	private static final String TAG = "device";

	/** Size of read buffer. */
	private static final int BUFSIZE = 8;

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
			instance = new DiscoverableDevice();
			Log.i(TAG, "Interface: " + instance.getCell());
		}
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
			BufferedReader r = new BufferedReader(new FileReader(f), BUFSIZE);
			sb.append("read: " + f);
			sb.append("\t");
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
	public static String debugDeviceList() {
		StringBuilder sb = new StringBuilder();
		sb.append("product: ");
		sb.append(Build.PRODUCT);
		sb.append("\nmodel: ");
		sb.append(Build.MODEL);
		sb.append("\n");
		try {
			final File f = new File(SysClassNet.SYS_CLASS_NET);
			final String[] devices = f.list();
			sb.append("\n#devices: ");
			sb.append(devices.length);
			for (String d : devices) {
				final String dev = SysClassNet.SYS_CLASS_NET + d;
				try {
					sb.append("\n\ndevice: ");
					sb.append(dev);
					sb.append("\n");
					sb.append(readFile(dev + "/type"));
					sb.append("\n");
					sb.append(readFile(dev + SysClassNet.CARRIER));
					sb.append("\n");
					sb.append(readFile(dev + SysClassNet.RX_BYTES));
					sb.append("\n");
					sb.append(readFile(dev + SysClassNet.TX_BYTES));
					sb.append("\n");
				} catch (Exception e) {
					sb.append("\nERROR: " + e + "\n");
					Log.e(TAG, "ERROR reading " + dev, e);
				}
			}
		} catch (Exception e) {
			sb.append("\nERROR: " + e + "\n");
			Log.e(TAG, "error reading /sys/", e);
		}
		return sb.toString();
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
 * Automatically discover the network interfaces. No real magic here, just try
 * different possible solutions.
 */
class DiscoverableDevice extends Device {
	/** Tag for output. */
	private static final String TAG = "Device";

	/** List of possible cell interfaces. */
	private static final String[] CELL_INTERFACES = { //
	"rmnet0", "pdp0", "ppp0", "vsnet0" //
	};

	/** List of possible wifi interfaces. */
	private static final String[] WIFI_INTERFACES = { //
	"eth0", "tiwlan0", "wlan0", "athwlan0", "eth1" //
	};

	/** My cell interface. */
	private String mCell = null;
	/** My wifi interface. */
	private String mWiFi = null;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String[] getNames() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getBluetooth() {
		return "bnep0";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getCell() {
		if (this.mCell == null) {
			if (Build.DEVICE.startsWith("GT-")) {
				this.mCell = "pdp0";
			} else if (Build.PRODUCT.equals("sdk")) {
				this.mCell = "eth0";
			} else {
				for (String inter : CELL_INTERFACES) {
					if (SysClassNet.isUp(inter)) {
						this.mCell = inter;
						break;
					}
				}
			}
			Log.i(TAG, "Cell interface: " + this.mCell);
		}
		return this.mCell;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getWiFi() {
		if (this.mWiFi == null) {
			for (String inter : WIFI_INTERFACES) {
				if (SysClassNet.isUp(inter)) {
					Log
							.i(this.getClass().getName(), "WiFi interface: "
									+ inter);
					this.mWiFi = inter;
					break;
				}
			}
		}
		return this.mWiFi;
	}
}
