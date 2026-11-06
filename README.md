# 🔴 StreamSync

<div align="center">

![Version](https://img.shields.io/badge/version-1.0-blue.svg?style=for-the-badge)
![Minecraft](https://img.shields.io/badge/minecraft-1.16--1.26.2-green.svg?style=for-the-badge)
![Software](https://img.shields.io/badge/software-Spigot%20%7C%20Paper%20%7C%20Purpur-orange.svg?style=for-the-badge)
[![Discord](https://img.shields.io/badge/Discord-Join%20Community-5865F2?style=for-the-badge&logo=discord&logoColor=white)](https://discord.gg/BMFkMYf3rq)

**Interactive TikTok LIVE integration plugin for Minecraft servers.**

Connect your TikTok stream directly to Minecraft events — trigger spawns, traps, effects, and challenges in real time!

[Join Discord](https://discord.gg/BMFkMYf3rq) • [Download on BuiltByBit](https://builtbybit.com/)

</div>

---

## 🌟 Key Features

* **Direct TikTok LIVE Connection:** Connect seamlessly to any active TikTok stream via WebSocket without third-party paid proxies.
* **Interactive Live Actions:** Spawns, potion effects, traps (cobweb, obsidian cage), custom mobs, explosions, lightning, jumpscares, inventory surprises, and screen freezes.
* **Comprehensive Stream Challenges (Goals):**
  * 6 Challenge Modes: `SURVIVE`, `KILL_PLAYERS`, `KILL_MOBS`, `GET_ITEM`, `MINE_BLOCKS`, and `RANDOM`.
  * Deepslate & variant block detection (mining deepslate iron ore counts toward iron ore goals).
  * Real-time BossBar and ActionBar displays with smooth percentage tracking.
  * **New Position Teleport (`newGoalNewPosition`):** Teleports streamer to a fresh, safe distant location on challenge win.
  * Auto-restart challenge cycle for continuous streaming.
* **In-Game Settings Manager (`/ss settings`):**
  * Create, edit, and remove chat triggers dynamically in-game with tab completion.
  * Toggle live configuration values (`likes-enabled`, `gifts-enabled`, `chat-triggers-enabled`, etc.).
  * Configure challenges and boolean rules directly without editing files.
* **Event Support:**
  * **Chat Triggers:** Viewers run chat commands (e.g., `!creeper`, `!tnt`, `!freeze`, `!heart`).
  * **TikTok Gifts:** Different rewards or punishments for specific gifts (Rose, Panda, Lion, TikTok, etc.).
  * **Stream Likes:** Triggers configurable actions every *X* likes accumulated.
  * **Followers:** Triggers rewards, heals, or celebratory effects when someone follows.
* **Multi-Streamer & Smart Auto-Targeting:** Supports multiple registered streamers, or automatically locks onto the sole online player.
* **Event Queue System:** Thread-safe async processing queue with rate-limiting to prevent server lag during high viewer interaction.
* **PlaceholderAPI Integration:** Comprehensive placeholders for Tablist, Scoreboard, and holograms.
* **Modern Aesthetic:** Clean cyan & white color scheme (`&#74ACF6` & `&f`) with full HEX support.

---

## 📌 Placeholders

Requires [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/). Use these placeholders in your scoreboard, tablist, or chat:

| Placeholder | Description | Example Output |
| :--- | :--- | :--- |
| `%streamsync_status%` | Connection status to TikTok Live | `ONLINE` / `OFFLINE` |
| `%streamsync_tiktok_user%` | Currently connected TikTok account | `worstek` |
| `%streamsync_goal_type%` | Active challenge type | `SURVIVE` / `MINE_BLOCKS` |
| `%streamsync_goal_target%` | Target value or duration | `24m` / `25` |
| `%streamsync_goal_current%` | Current progress counter | `18` |
| `%streamsync_goal_progress%` | Formatted progress string | `18/25` |
| `%streamsync_goal_percent%` | Goal completion percentage | `72%` |
| `%streamsync_goal_time%` | Remaining survival time | `23m 39s` |
| `%streamsync_streamers%` | Comma-separated list of active streamers | `Worstek` |
| `%streamsync_queue_size%` | Number of events waiting in the queue | `0` |

---

## ⌨️ Commands

| Command | Description |
| :--- | :--- |
| `/ss connect <username>` | Connect directly to a TikTok live stream |
| `/ss disconnect` | Disconnect from the current live stream |
| `/ss status` | Check connection and active streamers status |
| `/ss streamer <add\|remove\|list> [player]` | Manage streamer players who receive live stream events |
| `/ss settings actions list` | View all configured chat triggers |
| `/ss settings actions create <name> <action> [options]` | Create an interactive action directly in-game |
| `/ss settings actions remove <name>` | Delete an action trigger |
| `/ss settings config <key> [value]` | View or toggle config settings in real time |
| `/ss settings goal list` | View active challenge configuration |
| `/ss settings goal start` | Start the configured challenge |
| `/ss settings goal stop` | Stop the active challenge |
| `/ss settings goal set <type> [target] [target-type]` | Set challenge type and target parameters |
| `/ss settings goal settings [key] [true\|false]` | Toggle challenge options (`die-lose`, `newGoalNewPosition`, etc.) |
| `/ss settings goal time <add\|remove> <time>` | Add or remove time during a `SURVIVE` challenge (e.g. `1m`, `30s`) |
| `/ss settings goal test <win\|lose>` | Simulate challenge victory or failure |
| `/ss test <gift\|likes\|chat\|follow> [args]` | Simulate stream events for testing and setup |
| `/ss reload` | Reload all YAML configuration files |
| `/ss clear` | Clear pending events from the queue |

---

## 🔒 Permissions

* `streamsync.admin` — Grants full access to all StreamSync commands and settings (default: OP).

---

## 🎬 Action Types

When creating actions in `actions.yml` or using `/ss settings actions create`:

* `SPAWN_MOB` — Spawns customized mobs (e.g., `CREEPER`, `ZOMBIE`, `WARDEN`) with custom names, fire, or glowing.
* `EFFECT` — Gives potion effects (e.g., `BLINDNESS`, `LEVITATION`, `DARKNESS`, `SPEED`).
* `ITEM_GIVE` — Gives items directly to the streamer (e.g., `DIAMOND`, `TOTEM_OF_UNDYING`).
* `TRAP` — Creates temporary block cages around the player (`COBWEB`, `OBSIDIAN`, `ICE`, `SLIME_BLOCK`).
* `HEART` — Heals, damages, or modifies the streamer's maximum health.
* `FREEZE` — Immobilizes the player for a set number of seconds.
* `SLAP` — Knocks the player back with sound and light damage.
* `JUMPSCARE_SOUND` — Plays intense jumpscare sounds (e.g., Elder Guardian, Creeper primer).
* `LAUNCH_UP` — Launches the streamer high into the air.
* `LIGHTNING` — Strikes harmless or damaging lightning at the player.
* `EXPLOSION` — Spawns non-destructive visual or physical explosions.
* `IGNITE` — Sets the player on fire for a specified duration.
* `SPIN` — Dramatically rotates the player's camera.
* `ANVIL_FALL` — Drops an anvil from the sky above the player.
* `LOOK` — Forces the player to suddenly look straight up at the sky or down at the ground.
* `COMMAND` — Executes any custom console or player command.

---

## ⚙️ Configuration Preview

### `config.yml`
```yaml
tiktok:
  enabled: true
  username: "worsteck"

likes-enabled: true
gifts-enabled: true
chat-triggers-enabled: true
follow-enabled: true
chat-show: true

queue:
  max-size: 50
  process-interval-ticks: 20
```

### `goal.yml`
```yaml
goal:
  type: "SURVIVE"
  target: "24m"
  die-lose: true
  goal-completed-clearInventory: false
  newGoalNewPosition: true

  bossbar:
    enabled: true
    title: "&#74ACF6● &fSurvive: &#74ACF6%time% &8(&f%percent%%&8)"
    color: "BLUE"
    style: "SOLID"

  actionbar:
    enabled: false
    text: "&#74ACF6● &fChallenge: &#74ACF6%type% &8| &fProgress: &#74ACF6%current%&7/&#74ACF6%target%"
```

---

## 📥 Installation

1. Download `StreamSync-1.0.jar`.
2. Place the `.jar` into your server's `plugins/` directory.
3. *(Optional)* Install [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) for scoreboard & tablist integration.
4. Start your server.
5. Connect your live stream in-game: `/ss connect <your_tiktok_username>`.

---

## 📬 Support & Contact

- **Discord:** worstek
- **Telegram:** worstek
- **Community:** [Join Discord](https://discord.gg/BMFkMYf3rq)
- **BuiltByBit:** [Worstek Profile](https://builtbybit.com/conversations/add?to=Worstek)
