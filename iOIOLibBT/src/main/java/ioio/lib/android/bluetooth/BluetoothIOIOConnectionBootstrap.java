/*
 * Copyright 2011 Ytai Ben-Tsvi. All rights reserved.
 *  
 * 
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 * 
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 * 
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL ARSHAN POURSOHI OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied.
 */

package ioio.lib.android.bluetooth;

import ioio.lib.api.IOIOConnection;
import ioio.lib.spi.IOIOConnectionBootstrap;
import ioio.lib.spi.IOIOConnectionFactory;
import ioio.lib.spi.NoRuntimeSupportException;

import java.util.Collection;
import java.util.Set;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

public class BluetoothIOIOConnectionBootstrap implements
		IOIOConnectionBootstrap {

	private static final String TAG = "BTIOIOConnDiscovery";
	private final BluetoothAdapter adapter_;

	@SuppressWarnings("deprecation")
	public BluetoothIOIOConnectionBootstrap(Context context) throws NoRuntimeSupportException {
		try {
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
				BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
				if (bluetoothManager != null) {
					adapter_ = bluetoothManager.getAdapter();
				} else {
					adapter_ = null;
				}
			} else {
				adapter_ = BluetoothAdapter.getDefaultAdapter();
			}
			if (adapter_ != null) {
				return;
			}
		} catch (Throwable e) {
		}
			throw new NoRuntimeSupportException(
					"Bluetooth is not supported on this device.");
		}

	@Override
	public void getFactories(Collection<IOIOConnectionFactory> result) {
		try {
			Set<BluetoothDevice> bondedDevices = adapter_.getBondedDevices();
			for (final BluetoothDevice device : bondedDevices) {
				if (device.getName().startsWith("IOIO")) {
					result.add(new IOIOConnectionFactory() {
						@Override
						public String getType() {
							return BluetoothIOIOConnection.class
									.getCanonicalName();
						}

						@Override
						public Object getExtra() {
							return new Object[] { device.getName(),
									device.getAddress() };
						}

						@Override
						public IOIOConnection createConnection() {
							return new BluetoothIOIOConnection(device);
						}
					});
				}
			}
		} catch (SecurityException e) {
			Log.e(TAG,
					"Did you forget to declare uses-permission of android.permission.BLUETOOTH?");
			throw e;
		} catch (NoClassDefFoundError e) {
			Log.w(TAG, "Bluetooth is not supported on this device.", e);
		}
	}
}
