<h1 align="center">Sparkling Mutuals</h1>

<p align="center">
  <img src="src/main/resources/assets/sparkling-mutuals/icon.png" width="80" height="80" alt="Sparkling Mutuals logo">
</p>

<p align="center">
  <a href="https://discord.com/channels/1567856303341051905/1567859199491186749"><img src="https://img.shields.io/badge/Discord-Community-5865F2?style=for-the-badge&amp;logo=discord&amp;logoColor=white" alt="Discord"></a>
  <a href="https://kotlinlang.org/"><img src="https://img.shields.io/badge/Made%20with-Kotlin-7F52FF?style=for-the-badge&amp;logo=kotlin&amp;logoColor=white" alt="Made with Kotlin"></a>
  <a href="https://github.com/1tsethan/sparkling-mutuals/releases/latest"><img src="https://img.shields.io/github/downloads/1tsethan/sparkling-mutuals/total?label=Downloads&amp;style=for-the-badge&amp;logo=github" alt="Downloads"></a>
</p>

A Fabric client mod for Hypixel SkyBlock’s Critter Safari, maintained as the legit edition in this repository.

This edition keeps Unique, Full Clear and Sparkle progress tracking and compatible party sync. Target markers and discovery alerts require a current local sighting, normal range and terrain visibility. It removes gameplay and party automation. See [edition behavior and validation](docs/LEGIT_EDITION.md). This project is not endorsed or approved by Hypixel; consult the current [allowed modifications rules](https://support.hypixel.net/hc/en-us/articles/6472550754962-Hypixel-Allowed-Modifications).

Install only one Sparkling Mutuals edition at a time. Settings and saves are separate in `config/sparkling-mutuals-legit`; the mod does not import or move another edition's files. Future Modrinth distribution is planned; no Modrinth release is published by this setup.

## Installation

1. Set up **Minecraft 26.1.2, 26.2, or 26.3** with **Java 25** and [Fabric Loader](https://fabricmc.net/use/installer/) **0.19.5 or newer**.
2. Put [Fabric API](https://modrinth.com/mod/fabric-api) for your Minecraft version and [Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin) in your `mods` folder.
3. Download Sparkling Mutuals from [GitHub Releases](https://github.com/1tsethan/sparkling-mutuals/releases/latest). Pick the JAR whose `+mc` suffix matches your Minecraft version, put it in `mods`, and remove any older Sparkling Mutuals JAR.
4. Launch the Fabric profile and use `/sm` to open settings.

## Features

### Safari

| Feature | What it does |
| --- | --- |
| Safari modes | Track Sparkling checks, one catch per species, or a Full Clear with `/sparkle`, `/unique`, or `/fc`. |
| Progress HUDs | See remaining critters, checks, captures, and supplies needed to finish the run. |
| Critter ESP | Draw terrain-occluded boxes and nameplates for currently visible eligible critters, with configurable colors. |
| Floor-drop and structure ESP | Mark currently visible floor drops and observed structure sources. Floor drops use a generic label until identified through normal play. |
| Sparkling alerts | Alert only after local visible identification. Particles, remote reports and cached locations cannot trigger a discovery alert. |
| Sparkling lookups | Compare personal and party discoveries with `/sparkling`. Individual cards include total and per-biome captures, also available in party chat with `!captures [IGN]`. |
| Sparkling rates | See estimated runs per Sparkling with `/rates`. |
| Doomspiral throw timer | Time capsule throws around Doomspiral's emergence with a configurable tick countdown. |
| Lake Cleared alert | Show a personal alert when your lake objectives are complete. |
| Safari visual cleanup | Hide selected chat messages and capsules, and customize darkness effects. |

### Party and entry

| Feature | What it does |
| --- | --- |
| Party sync | [Share Safari objectives and confirmed critter progress](docs/PARTY_SYNC.md) with compatible SafariUtils parties. |
| Hotspot assigning | Coordinate biome assignments with `/hotspot`; party members can use `!hotspot on` or `!hotspot off`. |
| Party commands | Share missing critters, tickets, PBs, streaks, and recorded Emerald Dye counts. Use `!commands` for the list. |
| Teammate ESP | Draw boxes and names for currently visible party members with terrain occlusion. |
| Entry cooldown HUD | Show the retry countdown after the server refuses entry to an instance. |
| Miria contests | Track contest progress, ticket readiness, warnings, and customizable ticket-earned announcements. |
| Honeyhives | See Honeyhive locations and refill timers. |

### History and economy

| Feature | What it does |
| --- | --- |
| Run history | Review runs and catches in `/sm history`, or import older runs from Minecraft logs. |
| Personal bests | Track critter and biome completion times, including supported party records. |
| Achievements | Unlock milestones for Safari runs, catches, discoveries, and other progress. |
| Profit history | Review daily, weekly, and session profit. Start a new session with `/newsession`. |
| Hunting Box value | Estimate the combined value of shards in your Hunting Box. |
| Shard sale guidance | Compare Bazaar sell offers, instant selling, and salvaging Safari shards. |
| Profitable critters | Rank critters by ticket type with `/profit` and track targets with an optional checklist HUD. |

### Customization

| Feature | What it does |
| --- | --- |
| HUD layout | Move and resize HUDs with `/sm gui` and choose how each panel expands. |
| HUD appearance | Customize colors, borders, transparency, resource-pack fonts, text shadows, and pixel-aligned scaling. |
| HUD text customization | Set custom headers and separate header, label, and value colors for supported HUDs. |
| Custom critter names | Rename any of the 37 critters for local display while keeping tracking and party messages unchanged. |
| Critter nameplates | Configure optional per-critter nameplates with custom text, distance, size, color, and backgrounds. |
| Banners and catch effects | Customize Safari banners, alerts, and catch celebrations. |

### Other SkyBlock features

| Feature | What it does |
| --- | --- |
| Skills overflow | Show skill XP beyond the level cap in tooltips. |

See the [documentation index](docs/README.md) for feature guides and details.

## Credits

Based on [Sparkling Mutuals by JohnCEO and JohnKuudra](https://github.com/johnceoskyblock/sparkling-mutuals). Original licensing and third-party notices are preserved. Some credits describe historical upstream features removed from this edition.


- **SafariUtils** by Serkosea, built on MrCloudy2’s CritterMod. Adapted the settings and Warp List layouts, HUD snapping and icons, Biome Objectives and contest HUDs, encounter banners, catch effects, nameplates and pity tracking, chat filtering, and parts of waypoint, floor-drop, birdfeeder, Scrappy, and Hideyho tracking. Its shared party-sync protocol enables cross-mod coordination. [Repository](https://github.com/Serkosea/safariutils) · [Party-sync notice](licenses/SafariUtils-PartySync-NOTICE.txt)
- **CritterMod** by MrCloudy2. Adapted the critter roster and catch quotas, the Safari area lookup, progress HUD layout, Sparkling announcement behavior, and parts of Rockmite mound and Snoozle wall tracking. [Repository](https://github.com/MrCloudy2/critterMod)
- **ShinyHunter** provided the basis for the `/sparkling` profile card and biome capture stats, Hideyho quest button matching, and hiding Safari paintings. [Repository](https://github.com/javabetter/ShinyHunter)
- **Nebulune** provided critter model identification, palette and box offsets for Safari ESP, and click timing for the Rockmite mound helper. [Repository](https://github.com/Gaeritag/Nebulune)
- **SkyHanni** provided the basis for skill overflow tooltips and level-up messages, capsule hiding, the Phantomleaf Solver, and the Ubik's Cube cooldown timer. The Safari area data also traces back to SkyHanni. [Repository](https://github.com/SkyHanniStudios/SkyHanni)
- **SkyOcean** and its Vanguard Gambling feature inspired the Doomspiral Lottery and informed the original reel animation. [Repository](https://github.com/meowdding/SkyOcean)
- **NoammAddons** informed the Doomspiral throw timer’s server tick clock, the Safari entry cooldown HUD, and teammate ESP. [Repository](https://github.com/Noamm9/NoammAddons)

See [third-party licenses and notices](licenses/) for attribution and license terms.
