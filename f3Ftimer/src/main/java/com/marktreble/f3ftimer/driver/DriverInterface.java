/*
 *     ___________ ______   _______
 *    / ____/__  // ____/  /_  __(_)___ ___  ___  _____
 *   / /_    /_ </ /_       / / / / __ `__ \/ _ \/ ___/
 *  / __/  ___/ / __/      / / / / / / / / /  __/ /
 * /_/    /____/_/        /_/ /_/_/ /_/ /_/\___/_/
 *
 * Open Source F3F timer UI and scores database
 *
 */

package com.marktreble.f3ftimer.driver;

public interface DriverInterface {
    void driverConnected();

    void driverDisconnected();

    void sendLaunch();

    void sendAbort();

    void sendAdditionalBuzzer();

    void sendResendTime();

    void baseA();

    void baseB();

    void finished(String time);
}
