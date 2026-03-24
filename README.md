<h1 align="center">
  <img src="src/main/resources/assets/iq/icon.png" alt="IQ Logo" width="100"><br>
  IQ Addons
</h1>

<div align="center">
  <b>A Hypixel SkyBlock mod made especially for Kuudra</b><br>
  <sub>Clean, minimal, and focused on what matters</sub>
</div>

<br>

<div align="center">

[![Discord](https://img.shields.io/badge/Discord-Join%20Us-5865F2?style=for-the-badge&logo=discord&logoColor=white)](https://discord.gg/HdhXhCWcW9)
[![GitHub Downloads](https://img.shields.io/github/downloads/pehenrii/IQ/total?style=for-the-badge&logo=github&label=Downloads&color=2ea043)](https://github.com/pehenrii/IQ/releases)
[![Made with Java](https://img.shields.io/badge/Made%20With-Java%2021-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org)
[![Fabric](https://img.shields.io/badge/Fabric-1.21.x-DBD0B4?style=for-the-badge&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAOCAYAAAAfSC3RAAAACXBIWXMAAAsTAAALEwEAmpwYAAAA)](https://fabricmc.net)
[![License](https://img.shields.io/github/license/pehenrii/IQ?style=for-the-badge&color=blue)](https://github.com/pehenrii/IQ/blob/master/LICENSE)

</div>

---

## 🎯 What is IQ?

**IQ** is a lightweight Fabric mod designed to enhance your Kuudra experience on Hypixel SkyBlock. While it includes some general features, the core focus is on providing **useful, non-intrusive QoL improvements** for every phase of Kuudra runs.

From the very beginning, our goal has been to keep IQ **clean and minimal** — offering only useful features with no unnecessary clutter.

---

## ✨ Features

### Phase 1 — Supplies

<details>
<summary><b>🎯 Pearl Waypoints</b></summary>
<br>
Shows precise waypoints for pearl throws based on your position. Includes stand block indicators and timing labels.

- Automatically detects your current area
- Shows optimal pearl landing spots
- Displays recommended throw timing
- Fully customizable via JSON config
</details>

<details>
<summary><b>📦 Supply Waypoints</b></summary>
<br>
Draws beacon beams at supply crate locations carried by giants.

- Real-time tracking of supply carriers
- Customizable waypoint colors
- Automatic updates as giants move
</details>

<details>
<summary><b>🏔️ Pile Waypoints</b></summary>
<br>
Displays beacons at all crate pile locations.

- Highlights remaining piles
- Different color for "no pre" piles
- Automatically hides completed piles
</details>

<details>
<summary><b>⏱️ Supply Timers</b></summary>
<br>
Tracks and displays supply pickup times for all players.

- Shows player name and pickup time
- Color-coded based on speed
- Clean HUD overlay
</details>

<details>
<summary><b>🔔 Supply Alerts</b></summary>
<br>
Multiple alert systems to keep you informed:

- **No Pre Alert** — Automatically announces missing pre supplies to party chat
- **Already Picking Alert** — Shows title when someone else is picking your supply
- **Second Supply Alert** — Announces second supply position (Shop/X Cannon/Square)
- **Supply Recover Message** — Sends your custom chat message when you recover a supply
- **Supply Giant Hitbox Alert** — Highlights and alerts when you recover inside giant hitbox
</details>

<details>
<summary><b>📈 Supply Progress Widget</b></summary>
<br>
Replaces the default supply title with a clean, movable widget.

- Live 0/6 progress updates
- Better visibility than vanilla title spam
- Fully HUD-editable position
</details>

---

### Phase 2 — Build

<details>
<summary><b>🏗️ Build Progress Overlay</b></summary>
<br>
Displays a comprehensive build progress HUD.

- Current build percentage
- Fresh count tracker
- ETA estimation
- Color-coded progress bar
</details>

<details>
<summary><b>🔥 Fresh Alerts & Timers</b></summary>
<br>
Complete fresh tracking system:

- **Fresh Message** — Sends party message when you fresh (with build %)
- **Fresh Timers** — Renders countdown above fresher's heads
- **Fresh Countdown** — Personal HUD countdown for your fresh
- **Fresh Highlight** — Separate temporary glow override for players who are fresh
</details>

<details>
<summary><b>👷 Build Helper Waypoints</b></summary>
<br>
Shows beacon beams at build piles with progress-based colors.

- Red → Orange → Yellow → Green color progression
- Displays pile progress percentage
- Helps prioritize which piles need attention
</details>

<details>
<summary><b>👤 Elle Highlight</b></summary>
<br>
Draws a visible hitbox around Elle during the build phase for easy tracking.
</details>

<details>
<summary><b>🔊 Ballista Build Sound Replace</b></summary>
<br>
Replaces the default Ballista build sound during Phase 2 with IQ custom audio.
</details>

---

### Phase 3 — Stun

<details>
<summary><b>💚 Kuudra HP Bossbar</b></summary>
<br>
Custom health display for Kuudra.

- Shows current HP and percentage
- Color-coded health bar
- Damage calculator during boss phase
</details>

<details>
<summary><b>📍 Stun Waypoints</b></summary>
<br>
Displays waypoints at optimal stun positions.
</details>

<details>
<summary><b>🎯 Kuudra Hitbox</b></summary>
<br>
Renders Kuudra's hitbox with a glowing outline for easier targeting.

- Customizable color
- Works through walls
</details>

<details>
<summary><b>🚫 Block Useless Perks</b></summary>
<br>
Prevents accidentally purchasing useless perks from the perk menu.

- Blocks: Steady Hands, Bomberman, Auto Revive, Human Cannonball, Elle's Lava Rod, Elle's Pickaxe
</details>

---

### Phase 4 — Boss Fight

<details>
<summary><b>🧭 Kuudra Direction Alert</b></summary>
<br>
Shows which direction Kuudra will spawn from.

- Color-coded direction indicator
- Large on-screen title alert
</details>

<details>
<summary><b>⚔️ Rend Damage Tracker</b></summary>
<br>
Tracks when teammates deal Rend damage to Kuudra.

- Shows damage amount
- Displays timing since boss start
- Color-coded damage tiers
</details>

<details>
<summary><b>⚠️ Danger Zone Alert</b></summary>
<br>
Alerts when you're standing on tentacle danger zones.

- **JUMP!** alert on yellow/orange/red terracotta
- Audio notification
</details>

<details>
<summary><b>🦴 Backbone Alert</b></summary>
<br>
Tracks Bonemerang backbone timing with on-screen progress and Rend sync alert.
</details>

<details>
<summary><b>🙈 Hide Kuudra Damage Title</b></summary>
<br>
Hides the vanilla Kuudra damage title (e.g. ☠ 240M/240M❤) for a cleaner boss screen.
</details>

---

### General Features

<details>
<summary><b>📊 Custom Splits</b></summary>
<br>
Comprehensive run timing display.

- Individual phase times with color grading
- Overall run time
- Pace estimation
- Customizable thresholds
</details>

<details>
<summary><b>👥 Team Highlight</b></summary>
<br>
Highlights your teammates with the normal glowing effect during runs.

- Distinguishes real players from NPCs
- Customizable highlight color
</details>

<details>
<summary><b>💜 Mana Drain Notify</b></summary>
<br>
Announces Extreme Focus mana usage to party chat with affected player count.
</details>

<details>
<summary><b>🔔 Party Join Sound</b></summary>
<br>
Plays a notification sound when someone joins your party.
</details>

<details>
<summary><b>🔇 Hide Mob Nametags</b></summary>
<br>
Prevents Kuudra mob nametags from rendering, reducing visual clutter.
</details>

<details>
<summary><b>🏆 Personal Best Tracker</b></summary>
<br>
Tracks your best Kuudra run time and notifies you when you beat your PB.
</details>

<details>
<summary><b>💰 Kuudra Profit Tracker</b></summary>
<br>
Calculates profit/loss per run with configurable pricing logic.

- Bazaar and Auction-aware pricing
- Includes keys, essence, books, armor and pet bonus adjustments
- Hourly rate and session tracking
</details>

<details>
<summary><b>🧰 Chest Utilities</b></summary>
<br>
Utility set for chest-focused runs.

- **Chest Value Display** — Shows chest value when opening reward chests
- **Chest Counter Tracker** — Tracks progress toward the 60 chest cap
- **Chest Counter Party Reminders** — Optional milestone/cap announcements in party chat
- **Croesus Helper** — Highlights already opened chests in Croesus/Vesuvius menus
</details>

<details>
<summary><b>🔁 Auto Requeue</b></summary>
<br>
Automatically queues the next Kuudra run after boss completion with configurable delay.
</details>

<details>
<summary><b>📣 Kuudra Notifications</b></summary>
<br>
Centralized event notifications for key run moments.

- Build started / build done
- Supplies done
- Ichor used
- Cannonball purchased
- No pre reminder
- SOS (pre-stun) reminder
- Phase change alerts
- Optional notification sound
</details>

<details>
<summary><b>🪄 Ability Announce</b></summary>
<br>
Announces selected ability casts in party chat.

- Spirit Spark
- Hollowed Rush
- Raging Wind
- Ichor Pool
- Mana Drain
</details>

<details>
<summary><b>🎮 Party Commands</b></summary>
<br>
Supports chat-based party commands triggered by `!` messages.

- Warp / transfer / promote / kick shortcuts
- Quick joininstance commands (`!t1` to `!t5`)
- Ping and TPS replies
- Share runs, chest progress, and profit stats in party chat
</details>

<details>
<summary><b>👔 Wardrobe Keybinds</b></summary>
<br>
Lets you swap Wardrobe sets instantly using configurable keybinds with optional sound feedback.
</details>

<details>
<summary><b>🚨 Limbo Alert</b></summary>
<br>
Detects SkyBlock limbo kicks and alerts your party automatically.
</details>

<details>
<summary><b>🧹 Extra Visual Cleanup</b></summary>
<br>
Additional clutter-reduction options for cleaner gameplay.

- Hide Kuudra vanilla boss bar
- Hide selected useless armor stands
</details>

---

## Pearl Waypoints Customization

Advanced users can edit `pearl_waypoints.json` to customize pearl throw locations. The mod will automatically reload changes.

---

## 💬 Support

Need help or have suggestions?

- 🐛 **Bug Reports:** Open an [issue on GitHub](https://github.com/pehenrii/IQ/issues)
- 💡 **Feature Requests:** Join our [Discord](https://discord.gg/HdhXhCWcW9)
- 💬 **General Help:** Ask in our Discord server

---

## 🤝 Contributing

Contributions are welcome! If you'd like to contribute:

1. Fork the repository
2. Create a feature branch (`git checkout -b feat/amazing-feature`)
3. Commit your changes (`git commit -m 'feat(amazing) add amazing feature'`)
4. Push to the branch (`git push origin feat/amazing-feature`)
5. Open a Pull Request

---

## 📜 License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

---

## 👥 Credits

<table>
  <tr>
    <td align="center"><b>PeHenrii</b><br><sub>Lead Developer</sub></td>
    <td align="center"><b>DarkJota</b><br><sub>Mind behind the features</sub></td>
  </tr>
</table>

---

<div align="center">
  <sub>Made with ❤️ for the Kuudra community</sub><br>
  <sub>⭐ Star us on GitHub if you find this useful!</sub>
</div>
