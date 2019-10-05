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

package com.marktreble.f3ftimer.constants;

/*
 * Internal Communications protocol
 */

public class IComm {
    public final static String RCV_UPDATE = "com.marktreble.f3ftimer.onUpdate";
    public final static String RCV_UPDATE_FROM_UI = "com.marktreble.f3ftimer.onUpdateFromUI";
    public final static String RCV_LIVE_UPDATE = "com.marktreble.f3ftimer.onLiveUpdate";

    public final static String MSG_SERVICE_CALLBACK = "com.marktreble.f3ftimer.service_callback";
    public final static String MSG_UI_CALLBACK = "com.marktreble.f3ftimer.ui_callback";
    public final static String MSG_VALUE = "com.marktreble.f3ftimer.value";

    public final static String MSG_UI_UPDATE = "com.marktreble.f3ftimer.ui_update";
}
