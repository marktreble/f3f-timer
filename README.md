# f3f-timer

Race Management software for F3F Racing

This software is for maintaining the list of pilots, rounds and scores during an f3f race.
If you don't know what f3f is, then see http://www.rc-soar.com/bmfa_league/f3f.htm

The app does not do the timing itself. This is done using external hardware which connects to the Android device over a USB connection. This is the only way to ensure minimal latency in the timing.

The app currently supports 2 flavours of USB boards. You can either use an <a href="https://www.arduino.cc">Arduino</a>, or a <a href="https://www.sparkfun.com/products/10748">IOIO<a> to connect hardware to the app. These boards will work with most Android devices, however we have found that they tend to work with one or the other.

###Arduino
We have an arduino based system which is still under development (29/6/15). Details will be added here soon.

The Arduino does have limited compatibility with Android Devices. Extra compatibility can be acheieved by <a href="https://en.wikipedia.org/wiki/Rooting_(Android_OS)">rooting</a> your device, but we have tried to make this work with off-the-shelf devices. See the compatibility table below for details of which devices are known to work.

###IOIO
We have used the IOIO successfully in the MK III Timer System developed by Jon Edison. Full constuction details can be found at http://www.nymrsc.org.uk/Jons%20Timer%20mk3.htm

###Compatibility

Devices connect using Android's <a href="http://source.android.com/accessories/protocol.html">Open Accessory Protocol</a>. Older devices may not support this, but some can communicate through <a href="http://developer.android.com/tools/help/adb.html">ADB</a> also. If your device does not support Open Accessory, then you can try adb by going to Settings > Developers Options and enable USB Debugging. Otherwise USB Debugging should be switched off.

The following table lists know device compatibility:

| Device                  | IOIO (ADB) | IOIO (Open Accessory) | Arduino |
| ----------------------- | ---------- | --------------------- | ------- |
| Nexus 7                 |            | ✔                     |         |
| Asus Transformer TF101  | ✔          |                       |         |
| Samsung Galaxy S4       |            |                       |✔        |
| Leagoo Lead 3           |            | ✔                     |         |

