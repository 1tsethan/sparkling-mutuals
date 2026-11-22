# Sparkling Mutuals editions

Standard, Full and Full No Auto Update share Safari mode tracking, party sync, HUDs and history. Each build targets Minecraft 26.1.2, 26.2 or 26.3. Install one Sparkling Mutuals JAR per instance.

| Edition | File for version V and Minecraft M | Configuration directory |
| --- | --- | --- |
| Standard | `sparkling-mutuals-standard-V+mcM.jar` | `config/sparkling-mutuals-legit` |
| Full | `sparkling-mutuals-V+mcM.jar` | `config/sparkling-mutuals` |
| Full No Auto Update | `sparkling-mutuals-full-noautoupdate-V+mcM.jar` | `config/sparkling-mutuals` |

Existing legit users can replace their old JAR with Standard while Minecraft is closed. Standard reads the existing legit configuration and history. Existing upstream users can replace their old JAR with either Full edition and continue using the original configuration and history. Each configuration namespace stays intact when switching editions.

Standard applies local visibility checks to critter markers and discovery alerts. Its build excludes Full gameplay automation, through-terrain rendering and hidden discoveries. Standard's `/sm debug` logs party sync and run status. Party sync exchanges Safari progress with the backend and compatible clients. Describe that exchange and the limited purpose of its data accurately on the Modrinth listing. Review Hypixel's [modification rules](https://support.hypixel.net/hc/en-us/articles/6472550754962-Hypixel-Allowed-Modifications) before using the mod.

Full checks GitHub Releases. Auto Update starts off. Open the update screen from settings and use **Download Update** to stage a verified compatible Full JAR. Enable **Auto Update** to stage future compatible downloads automatically. **Ready to install on restart** means the download has been verified and is waiting for the game process to exit. Let the installer finish after closing the game before reopening it. If an installation fails, the update screen offers a retry. A verified copy of the previous JAR remains in the Full config's `updates` folder.

To restore a previous JAR, close Minecraft, move the active Sparkling Mutuals JAR out of `mods`, and copy the desired rollback JAR into `mods` using that same active filename. Full No Auto Update gets manual upgrades from GitHub Releases. Standard gets updates through the Modrinth app once listed.

See [builds](MULTI_VERSION_BUILDS.md) and [shipping](SHIPPING.md) for release preparation.
