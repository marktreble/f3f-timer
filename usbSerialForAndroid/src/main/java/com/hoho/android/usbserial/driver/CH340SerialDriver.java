/* Copyright 2011-2013 Google Inc.
 * Copyright 2013 mike wakerly <opensource@hoho.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 * Project home page: https://github.com/mik3y/usb-serial-for-android
 */

package com.hoho.android.usbserial.driver;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.util.Log;

import com.hoho.android.usbserial.driver.chinese.CH340AndroidDriver.UsbType;
import com.hoho.android.usbserial.driver.chinese.CH340AndroidDriver.UartCmd;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CH340SerialDriver implements UsbSerialDriver {

    private static final String TAG = CH340SerialDriver.class.getSimpleName();

    private final UsbDevice mDevice;
    private final UsbSerialPort mPort;

    public CH340SerialDriver(UsbDevice device) {
        mDevice = device;
        mPort = new CH340SerialPort(mDevice, 0);
    }

    @Override
    public UsbDevice getDevice() {
        return mDevice;
    }

    @Override
    public List<UsbSerialPort> getPorts() {
        return Collections.singletonList(mPort);
    }

    public class CH340SerialPort extends CommonUsbSerialPort {

        private UsbEndpoint mReadEndpoint;
        private UsbEndpoint mWriteEndpoint;

        private int DEFAULT_TIMEOUT = 500;

        public CH340SerialPort(UsbDevice device, int portNumber) {
            super(device, portNumber);
        }

        @Override
        public UsbSerialDriver getDriver() {
            return CH340SerialDriver.this;
        }


        @Override
        public void open(UsbDeviceConnection connection) throws IOException {
            if (mConnection != null) {
                throw new IOException("Already opened.");
            }

            mConnection = connection;
            boolean opened = false;
            try {
                for (int i = 0; i < mDevice.getInterfaceCount(); i++) {
                    UsbInterface usbIface = mDevice.getInterface(i);
                    if (mConnection.claimInterface(usbIface, true)) {
                        Log.d(TAG, "claimInterface " + i + " SUCCESS");
                    } else {
                        Log.d(TAG, "claimInterface " + i + " FAIL");
                    }
                }

                UsbInterface dataIface = mDevice.getInterface(mDevice.getInterfaceCount() - 1);
                for (int i = 0; i < dataIface.getEndpointCount(); i++) {
                    UsbEndpoint ep = dataIface.getEndpoint(i);
                    if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                        if (ep.getDirection() == UsbConstants.USB_DIR_IN) {
                            mReadEndpoint = ep;
                        } else {
                            mWriteEndpoint = ep;
                        }
                    }
                }

                opened = true;
            } finally {
                if (!opened) {
                    try {
                        close();
                    } catch (IOException e) {
                        // Ignore IOExceptions during close()
                    }
                }
            }
        }

        @Override
        public void close() throws IOException {
            if (mConnection == null) {
                throw new IOException("Already closed");
            }
            try {
                mConnection.close();
            } finally {
                mConnection = null;
            }
        }

        @Override
        public int read(byte[] dest, int timeoutMillis) throws IOException {
            final int numBytesRead;
            synchronized (mReadBufferLock) {
                int readAmt = Math.min(dest.length, mReadBuffer.length);
                numBytesRead = mConnection.bulkTransfer(mReadEndpoint, mReadBuffer, readAmt,
                        timeoutMillis);
                if (numBytesRead < 0) {
                    // This sucks: we get -1 on timeout, not 0 as preferred.
                    // We *should* use UsbRequest, except it has a bug/api oversight
                    // where there is no way to determine the number of bytes read
                    // in response :\ -- http://b.android.com/28023
                    return 0;
                }
                System.arraycopy(mReadBuffer, 0, dest, 0, numBytesRead);
            }
            return numBytesRead;
        }

        @Override
        public int write(byte[] src, int timeoutMillis) throws IOException {
            int offset = 0;

            while (offset < src.length) {
                final int writeLength;
                final int amtWritten;

                synchronized (mWriteBufferLock) {
                    final byte[] writeBuffer;

                    writeLength = Math.min(src.length - offset, mWriteBuffer.length);
                    if (offset == 0) {
                        writeBuffer = src;
                    } else {
                        // bulkTransfer does not support offsets, make a copy.
                        System.arraycopy(src, offset, mWriteBuffer, 0, writeLength);
                        writeBuffer = mWriteBuffer;
                    }

                    amtWritten = mConnection.bulkTransfer(mWriteEndpoint, writeBuffer, writeLength,
                            timeoutMillis);
                }
                if (amtWritten <= 0) {
                    throw new IOException("Error writing " + writeLength
                            + " bytes at offset " + offset + " length=" + src.length);
                }

                Log.d(TAG, "Wrote amt=" + amtWritten + " attempted=" + writeLength);
                offset += amtWritten;
            }
            return offset;
        }

        /**
         * Performs a control transaction on endpoint zero for this device.
         * The direction of the transfer is determined by the request type.
         * If requestType & {@link UsbConstants#USB_ENDPOINT_DIR_MASK} is
         * {@link UsbConstants#USB_DIR_OUT}, then the transfer is a write,
         * and if it is {@link UsbConstants#USB_DIR_IN}, then the transfer
         * is a read.
         *
         * @param request request ID for this transaction
         * @param value value field for this transaction
         * @param index index field for this transaction
         * @return length of data transferred (or zero) for success,
         * or negative value for failure
         *
         * public int controlTransfer(int requestType, int request, int value,
         *       int index, byte[] buffer, int length, int timeout)
         */

        public int Uart_Control_Out(int request, int value, int index)
        {
            int retval = 0;
            retval = mConnection.controlTransfer(UsbType.USB_TYPE_VENDOR | UsbType.USB_RECIP_DEVICE | UsbType.USB_DIR_OUT,
                    request, value, index, null, 0, DEFAULT_TIMEOUT);

            return retval;
        }


        @Override
        public void setParameters(int baudRate, int dataBits, int stopBits, int parity)
                throws IOException {
            int value = 0;
            int index = 0;
            char valueHigh = 0, valueLow = 0, indexHigh = 0, indexLow = 0;
            switch(parity) {
                case 0:	/*NONE*/
                    valueHigh = 0x00;
                    break;
                case 1:	/*ODD*/
                    valueHigh |= 0x08;
                    break;
                case 2:	/*Even*/
                    valueHigh |= 0x18;
                    break;
                case 3:	/*Mark*/
                    valueHigh |= 0x28;
                    break;
                case 4:	/*Space*/
                    valueHigh |= 0x38;
                    break;
                default:	/*None*/
                    valueHigh = 0x00;
                    break;
            }

            if(stopBits == 2) {
                valueHigh |= 0x04;
            }

            switch(dataBits) {
                case 5:
                    valueHigh |= 0x00;
                    break;
                case 6:
                    valueHigh |= 0x01;
                    break;
                case 7:
                    valueHigh |= 0x02;
                    break;
                case 8:
                    valueHigh |= 0x03;
                    break;
                default:
                    valueHigh |= 0x03;
                    break;
            }

            valueHigh |= 0xc0;
            valueLow = 0x9c;

            value |= valueLow;
            value |= valueHigh << 8;

            switch(baudRate) {
                case 50:
                    indexLow = 0;
                    indexHigh = 0x16;
                    break;
                case 75:
                    indexLow = 0;
                    indexHigh = 0x64;
                    break;
                case 110:
                    indexLow = 0;
                    indexHigh = 0x96;
                    break;
                case 135:
                    indexLow = 0;
                    indexHigh = 0xa9;
                    break;
                case 150:
                    indexLow = 0;
                    indexHigh = 0xb2;
                    break;
                case 300:
                    indexLow = 0;
                    indexHigh = 0xd9;
                    break;
                case 600:
                    indexLow = 1;
                    indexHigh = 0x64;
                    break;
                case 1200:
                    indexLow = 1;
                    indexHigh = 0xb2;
                    break;
                case 1800:
                    indexLow = 1;
                    indexHigh = 0xcc;
                    break;
                case 2400:
                    indexLow = 1;
                    indexHigh = 0xd9;
                    break;
                case 4800:
                    indexLow = 2;
                    indexHigh = 0x64;
                    break;
                case 9600:
                    indexLow = 2;
                    indexHigh = 0xb2;
                    break;
                case 19200:
                    indexLow = 2;
                    indexHigh = 0xd9;
                    break;
                case 38400:
                    indexLow = 3;
                    indexHigh = 0x64;
                    break;
                case 57600:
                    indexLow = 3;
                    indexHigh = 0x98;
                    break;
                case 115200:
                    indexLow = 3;
                    indexHigh = 0xcc;
                    break;
                case 230400:
                    indexLow = 3;
                    indexHigh = 0xe6;
                    break;
                case 460800:
                    indexLow = 3;
                    indexHigh = 0xf3;
                    break;
                case 500000:
                    indexLow = 3;
                    indexHigh = 0xf4;
                    break;
                case 921600:
                    indexLow = 7;
                    indexHigh = 0xf3;
                    break;
                case 1000000:
                    indexLow = 3;
                    indexHigh = 0xfa;
                    break;
                case 2000000:
                    indexLow = 3;
                    indexHigh = 0xfd;
                    break;
                case 3000000:
                    indexLow = 3;
                    indexHigh = 0xfe;
                    break;
                default:	// default baudRate "9600"
                    indexLow = 2;
                    indexHigh = 0xb2;
                    break;
            }

            index |= 0x88 |indexLow;
            index |= indexHigh << 8;

            Uart_Control_Out(UartCmd.VENDOR_SERIAL_INIT, value, index);
        }

        @Override
        public boolean getCD() throws IOException {
            return false;
        }

        @Override
        public boolean getCTS() throws IOException {
            return false;
        }

        @Override
        public boolean getDSR() throws IOException {
            return false;
        }

        @Override
        public boolean getDTR() throws IOException {
            return true;
        }

        @Override
        public void setDTR(boolean value) throws IOException {
        }

        @Override
        public boolean getRI() throws IOException {
            return false;
        }

        @Override
        public boolean getRTS() throws IOException {
            return true;
        }

        @Override
        public void setRTS(boolean value) throws IOException {
        }


    }

    public static Map<Integer, int[]> getSupportedDevices() {
        final Map<Integer, int[]> supportedDevices = new LinkedHashMap<Integer, int[]>();

        supportedDevices.put(Integer.valueOf(UsbId.CHINESE_COPY),
                new int[] {
            UsbId.ARDUINO_COPY_340
        });
        return supportedDevices;
    }

}
