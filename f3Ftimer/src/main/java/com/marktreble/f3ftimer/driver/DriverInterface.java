/*
 * DriverInterface
 * Interface declaration which all HID drivers must implement
 */

package com.marktreble.f3ftimer.driver;

public interface DriverInterface {
	public void driverConnected();
	public void driverDisconnected();
	public void sendLaunch();
	public void sendAbort();
	public void sendAdditionalBuzzer();
	public void sendResendTime();

    public void baseA();
    public void baseB();
    public void finished(String time);
}
