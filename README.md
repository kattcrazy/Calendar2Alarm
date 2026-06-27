# Calendar2Alarm

When normal notifications aren't enough 😂

Calendar2Alarm is a standalone Wear OS app that reads calendar events on your watch and fires full-screen alarms at each event's reminder time, with customisable sound, vibration, and snooze times.

See [Installation](docs/INSTALLATION.md) for details on how to install.

## Features

- Calendar events with notifications set (even if notifications for your calendar app are off) convert to 'alarms'
- Alarms turn the screen on, vibrates/ring until you act, and show event details
- Choose which synced calendars to use (only ones with events in the next ~4 weeks appear)
- Customise alarm snooze duration, sound, and vibration presets
- Open events in your calendar app

## What calendars work?

Calendar2Alarm reads the built-in Wear OS calendar provider, not Samsung or Google Calendar directly. Only calendars that are synced to the watch are visible. See below for restrictions

| Syncs to the watch | Does not sync to watch |
|---|---|
| Your primary Google calendar (`you@gmail.com`) | Subscribed/public calendars (URL subscribe, not a member) |
| Shared Google calendars you are a member of | Samsung My phone (local) calendars |
| Google calendars from accounts on the watch | Google accounts that exist only on your phone |

## Privacy

Calendar data is read locally on your watch. See the [privacy policy](docs/PRIVACY.md) for details.

## License

This project uses the [GNU General Public License v3.0](https://www.gnu.org/licenses/gpl-3.0.html). See [LICENSE](LICENSE) for the full legal text. In short: you can use, change, and share it freely. If you distribute a modified version, you must offer it under the same license and share the source too, so the work (and its derivatives) stay open. You cannot take this code, tweak it, and ship it as a closed product.

## About 

When normal notifications aren't enough, what do you do? Please open an issue if something isn't working right or you have an idea.

If this decreases the amount of important meetings you miss... consider supporting me [here](https://kattcrazy.nz/product/support-me/) :)
