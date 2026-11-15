# LiteLauncher

> **Fork Notice:** **LiteLauncher** ([`Mightyiest/Olauncher`](https://github.com/Mightyiest/Olauncher)) is **heavily forked from the original [Olauncher](https://github.com/tanujnotes/Olauncher)** by [@tanujnotes](https://github.com/tanujnotes) under the [GNU GPLv3](https://www.gnu.org/licenses/gpl-3.0.en.html) license.

---

## Downloads
- Grab the latest **PlayProtect-safe** APK from [GitHub Releases](https://github.com/Mightyiest/Olauncher/releases):
  - **`LiteLauncher-PlayProtect-<version>-release.apk`** *(Recommended)*: Signed, R8-optimized release build (`app.litelauncher`) with `BIND_ACCESSIBILITY_SERVICE` / `MyAccessibilityService` removed so Google Play Protect Enhanced Fraud Protection allows direct installation.
  - **`LiteLauncher-PlayProtect-<version>-debug.apk`**: Debug build (`app.litelauncher.debug`, installs side-by-side).

---

## Enhancements in This Fork
- **Location-Based Weather Label**: Shows `City · Temp Condition` under the Home date using coarse location with automatic IP-geolocation fallback and multi-provider forecasts (**MET Norway**, **wttr.in**, and **Open-Meteo**).
- **Mindful App Pause**: 4-second breath countdown overlay with today's foreground usage before opening selected apps.
- **Biometric / PIN Lock for Hidden Apps**: Protects hidden apps access, lock disabling, and backup export using fingerprint, face, or device screen lock.
- **Custom Font Picker**: Choose `Light`, `System`, `Mono`, `Serif`, or `Condensed` across the Home screen and App Drawer.
- **JSON Backup & Restore**: Export and restore settings, app lists, renames, fonts, and weather/lock preferences via Android's system file picker with zero storage permissions.
- **Stability & Performance**: Main-thread I/O offloading, `DeviceAdmin` lock improvements, contact search deduplication, and hardened `SharedPreferences` handling.

---

## Upstream Attribution
- Original project: [tanujnotes/Olauncher](https://github.com/tanujnotes/Olauncher)
- License: [GNU GPLv3](https://www.gnu.org/licenses/gpl-3.0.en.html)
