# Installation

Download `calendartoalarm-wear.apk` from [Releases](https://github.com/kattcrazy/Calendar2Alarm/releases).

Package id: `kattcrazy.calendartoalarm` (renamed from `kattcrazy.calendar2alarm`). If you still have the old app installed:

```bash
adb uninstall kattcrazy.calendar2alarm
```

You need a PC with [ADB](https://developer.android.com/tools/releases/platform-tools) and wireless debugging enabled on your Wear OS watch.

## 1. Connect the watch

1. Settings -> About watch -> tap Serial number 5 times.
2. Settings -> Developer options -> Wireless debugging -> Pair new device.
3. On your PC:

   ```bash
   adb pair WATCH_IP:PAIRING_PORT
   adb connect WATCH_IP:CONNECTION_PORT
   ```

4. Check `adb devices` shows the watch.

Galaxy Watch 6/7: if pairing fails, turn off Bluetooth briefly so Wi‑Fi debugging stays up.

## 2. Install

```bash
adb install -r calendartoalarm-wear.apk
```

## 3. Set up on the watch

1. Open Calendar2Alarm.
2. Grant calendar, notifications, exact alarms, and full-screen alarm when prompted (use the buttons on the main screen for any that are missing).
3. Turn Enabled on.
4. Pick calendars in Calendars.
5. Choose Calendar app (Google or Samsung) for opening events.
6. Tap Sync.

Make sure Google Calendar on the watch already shows your upcoming events before expecting alarms to work.

Re-run after uninstall or reinstall.
