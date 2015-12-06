# f3f-timer

Race Management software for F3F Racing

This software is for maintaining the list of pilots, rounds and scores during an f3f race.
If you don't know what f3f is, then see http://www.rc-soar.com/bmfa_league/f3f.htm

The app does not do the timing itself. This is done using external hardware which connects to the Android device over a USB connection. This is the only way to ensure minimal latency in the timing.

The app currently supports 2 flavours of USB boards. You can either use an <a href="https://www.arduino.cc">Arduino</a>, or a <a href="https://www.sparkfun.com/products/10748">IOIO<a> to connect hardware to the app. These boards will work with most Android devices, however we have found that they tend to work with one or the other.

###Arduino
We have an arduino based system which is still under development (29/6/15). Details will be added here soon.

The Arduino does have limited compatibility with Android Devices. Extra compatibility can be achieved by <a href="https://en.wikipedia.org/wiki/Rooting_(Android_OS)">rooting</a> your device, but we have tried to make this work with off-the-shelf devices. See the <a href="https://github.com/marktreble/f3f-timer/blob/master/README.md#compatibility">compatibility</a> table below for details of which devices are known to work.

**Update**
We now have compatibility with a variety of USB controllers which will work with Genuine Arduinos and the cheap copies.
Support has been added for:
- CdcAcm
- Ftdi
- Cp21
- Prolific
- CH340

###IOIO
We have used the IOIO successfully in the MK III Timer System developed by Jon Edison. Full constuction details can be found at http://www.nymrsc.org.uk/Jons%20Timer%20mk3.htm

###Compatibility

Devices connect using Android's <a href="http://source.android.com/accessories/protocol.html">Open Accessory Protocol</a>. Older devices may not support this, but some can communicate through <a href="http://developer.android.com/tools/help/adb.html">ADB</a> also. If your device does not support Open Accessory, then you can try adb by going to Settings > Developers Options and enabling USB Debugging. Otherwise USB Debugging should be switched off.

The following table lists known device compatibility:

| Device                  | IOIO (ADB) | IOIO (Open Accessory) | Arduino |
| ----------------------- | ---------- | --------------------- | ------- |
| Nexus 7                 |            | ✔                     |         |
| Asus Transformer TF101  | ✔          |                       |         |
| Samsung Galaxy S4       |            |                       |✔        |
| Leagoo Lead 3           |            | ✔                     |         |
| HTC One M9              |            | ✔                     |         |

###Features

- Permanent database of pilot names (Pilot Manager App)
- Quick and easy race set up (Just type a name, and select pilots from the list)
- Random shuffle of flying order
- Flying order offset each round
- Real-time results over WiFi (Requires mobile wifi hotspot)
- Real-time results through Results Manager App
- In-app group scoring
- Multi-lingual voice synthesis
- Transfer of race data between devices over bluetooth

###Installation

The latest stable release can be downloaded here:
<a href="http://www.marktreble.co.uk/clients/f3f/F3Ftimer6.12.15.apk">F3Ftimer6.12.15.apk</a>

###Getting Started

You can't do anything without some pilots, so open the 'Pilot Manager' and use the menu to add some pilots.

Now open the 'Race Manager' (accessible from the Pilot Manager's menu, or by going back to your apps screen). From the menu, choose Settings. Under 'Input Source', choose the correct driver for your hardware. There is also a 'Demo Mode' option which allows you to emulate the hardware in it's absence.

Press the 'Back' key to exit the settings screen, and choose 'New Race' from the menu. Enter the name of your race, and press 'Next'.

Now choose pilots from the list. The order picked will be the flying order, although this can be changed later if required. If you make a mistake, you can press the 'Back' key to undo your last selection. Press 'Next'.

The next screen shows the pilots in flying order. You can perform a random shuffle (S) or manually adjust the order of any pilots (M). Press (+) to add any pilots you may have missed. Press 'Next'.

You are now ready to race!

The next pilot to fly is highlighted, and a short press on their name will start their flight. Pilot names can also be long pressed for other functions such as reflights, retirements, zero-score or manual time entry. All other function can be found from the main menu.

###Thanks
Thanks to Tilman Baumann (German Translations) and Alejandro Gil Garcia (Spanish Translations)






