# Privacy Policy for Calendar2Alarm

**Developer:** Kattcrazy  
**Contact:** [GitHub issues](https://github.com/kattcrazy/Calendar2Alarm/issues)

This policy describes how the Calendar2Alarm Wear OS app handles information.

## Summary

Calendar2Alarm reads calendar events from your watch's built-in calendar provider and schedules local alarms from their reminder times. There is no developer-run server, no user account, and no analytics or advertising in the app.

## Information the app accesses

When you use the app, it may access:

- Calendar event titles, times, descriptions, and reminder times (via `READ_CALENDAR`)
- Which calendars you choose to monitor
- Alarm preferences you set (snooze duration, sound, vibration pattern)
- Which calendar app you prefer for opening events (Google or Samsung Calendar)

This data is read from or stored on your watch using Android local storage. The developer does not receive it.

## How data is used

Your data is used only to:

- Find upcoming events with reminders and schedule alarms on your watch
- Show event details on the alarm screen when an alarm fires
- Open the matching event in your chosen calendar app when you dismiss an alarm
- Remember your settings between app launches

The developer does not sell, share, or use your calendar data for advertising.

## Network and third parties

The app does not connect to developer servers. It does not include analytics or ad SDKs.

Calendar sync itself is handled by your watch's calendar provider (for example Google Calendar or Samsung Calendar), not by Calendar2Alarm. Those apps and Google account services have their own privacy policies.

When you dismiss an alarm, the app may open Google Calendar or Samsung Calendar using a deep link you choose in settings. Only the event identifier needed to open that event is passed.

## Device backup

Your app preferences and scheduled alarm metadata may be included in your Google/Android device backup if you use that feature. Backup is controlled by your device and Google account settings, not by the developer.

## Data retention and deletion

Data remains on your watch until you change settings in the app, clear app data, or uninstall the app. Uninstalling removes locally stored preferences and scheduled alarm metadata managed by the app.

## Changes to this policy

This policy may be updated from time to time. The current version will always be in this repository at `docs/PRIVACY.md`.

## Open source

Source code is available at [github.com/kattcrazy/Calendar2Alarm](https://github.com/kattcrazy/Calendar2Alarm) under the [GNU GPL v3.0](../LICENSE).
