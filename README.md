# Calendar2Alarm

When normal notifications aren't enough 😂

Calendar2Alarm is a standalone Wear OS app that reads calendar events on your watch and fires full-screen alarms at each event's reminder time, with customisable sound, vibration, and snooze times.

See [Installation](docs/INSTALLATION.md) for details on how to install.

## Features

- Calendar reminders -> alarms: schedules 'alarms' from event reminder times
- Full-screen alarm UI that turns the screen on, vibrates/rings until you act, and shows dismiss/snooze/event details on screen
- Choose which synced calendars to use (only calendars with events in the next ~4 weeks appear)
- Customise alarm snooze duration, sound, and vibration presets
- Open events in either Google Calendar or Samsung Calendar (viewing only, does not change which app is used for sync).
- Respects silent modes: alarms are suppressed during Theater mode and Bedtime mode. DND only blocks alarms if your DND settings disallow them.

## Silent modes (Theater / Bedtime / DND)

| Mode | Behavior |
|---|---|
| **Theater mode** | Alarm suppressed |
| **Bedtime mode** | Alarm suppressed |
| **DND** | Alarm suppressed only if your DND settings block alarms (many setups allow alarms through) |

## What calendars work?

Calendar2Alarm reads the built-in Wear OS calendar provider, not Samsung or Google Calendar directly. Only calendars that are synced to the watch are visible. See below for restrictions

| Syncs to the watch | Does not sync to watch |
|---|---|
| Your primary Google calendar (`you@gmail.com`) | Subscribed/public calendars (URL subscribe, not a member) |
| Shared Google calendars you are a member of | Samsung My phone (local) calendars |
| Google calendars from accounts on the watch | Google accounts that exist only on your phone |

## Privacy

Calendar data is read locally from the on-watch calendar provider and preferences and scheduled alarm metadata stay in app storage on the watch.

## License

This project uses the [GNU General Public License v3.0](https://www.gnu.org/licenses/gpl-3.0.html). See [LICENSE](LICENSE) for the full legal text.
