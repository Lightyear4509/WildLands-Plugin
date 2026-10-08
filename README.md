# GGWP Wildlands

Server-authoritative rainforest survival project. **Milestones 1–9 are implemented in the combined 0.9.0 test build.** Hydration adds drinking, water quality, campfire boiling and an optional action-bar HUD. Environment adds temperature, wetness, shade, campfire warmth and shelter status. Seasons add per-world clocks, weather, temperature and natural crop-growth modifiers. World generation adds deterministic rainforest regions, waterways, canopy, caves and mining resources. Wildlife adds territorial jaguars. Crafting adds camp equipment, water filtration, portable supplies and food preservation. Exploration adds ruins, saved discoveries, camp waypoints, compass routes and expedition records. Polish adds bounded spawn selection, generation optimizations, a compact HUD and health diagnostics. Later live Java/Bedrock acceptance is deferred at the user's request; use [the collective checklist](docs/FINAL-TEST-CHECKLIST.md). Actual evidence is recorded in CROSSPLAY-COMPATIBILITY.md.

## Requirements and build

- JDK **25** (set `JAVA_HOME` to its installation directory).
- Internet access for the first build.
- The included Gradle **9.1.0** wrapper; no separate Gradle installation needed.

Windows PowerShell:

```powershell
.\gradlew.bat clean build
```

Linux/macOS:

```sh
sh ./gradlew clean build
```

Install **`build/libs/GGWPWildlands-0.9.0.jar`**. The `-plain.jar` is a development artifact without SQLite; do not install it. `build/distributions/GGWPWildlands-0.9.0-release.zip` bundles the plugin, configuration examples, documentation and final test checklist. Tests run during `build`; HTML results are in `build/reports/tests/test/index.html`. This is the collective build for final client testing; deferred client checks are not marked passed.

The build targets Java 25 bytecode and pins Paper API `26.2.build.121-stable`. SQLite is bundled in the distributable, including its native libraries and JDBC service descriptor. No runtime dependency download is required by Wildlands. The Gradle wrapper distribution is SHA-256 pinned, and dependency versions are locked in gradle.lockfile. SQLite uses Paper's provided SLF4J API rather than bundling a second logging API. Java 25 may warn about SQLite native-library access unless the server is launched with --enable-native-access=ALL-UNNAMED.

## Installation

1. Install Java 25 and a Paper **26.2** server.
2. Stop the server and copy the distributable into `plugins/`.
3. Start the server. Look for **Foundation ready** in the log.
4. Configure `plugins/GGWPWildlands/config.yml`, `messages.yml`, `hydration.yml`, `environment.yml`, `seasons.yml`, `worldgen.yml`, `wildlife.yml`, `crafting.yml`, `food.yml`, and `landmarks.yml`.
5. Run `wildlands admin health` from the server console, then follow the collective checklist.

Geyser-Spigot and Floodgate are optional. Install/configure their official server plugins to support Bedrock connections. Wildlands requires no client mod or resource pack. Do not install the plain and shaded JARs together. Use a full server restart for plugin updates; Bukkit/server hot reload is unsupported.

Official references: [Paper project setup](https://docs.papermc.io/paper/dev/project-setup/), [Geyser API](https://geysermc.org/wiki/geyser/getting-started-with-the-api/), [Floodgate API and proxy configuration](https://geysermc.org/wiki/floodgate/api/).

## Commands and permissions

| Command | Permission | Console |
| --- | --- | --- |
| `/wildlands`, `/wildlands help` | `ggwpwildlands.use` | Yes |
| `/wildlands status` | `ggwpwildlands.use` | Yes |
| `/wildlands reload` | `ggwpwildlands.admin` | Yes |
| `/wildlands admin modules` | `ggwpwildlands.admin` | Yes |
| `/wildlands admin debug` | `ggwpwildlands.admin.debug` | Yes |
| `/wildlands admin health` | `ggwpwildlands.admin.debug` | Yes |
| `/wildlands admin debug <online-player\|uuid>` | `ggwpwildlands.admin.debug` | Yes |

The use permission defaults to everyone; both admin permissions default to operators. Debug is independently permission-gated, so grant its explicit node to delegated operators. Offline debug lookups require a UUID; names are never used as persistent identifiers. Tab completion respects permissions and player visibility.

Status shows an online player's server UUID, detected platform, and record loading state. Console status reports plugin readiness. Health/debug show server/plugin/Java versions, storage health, queued work, pending writes, last successful save, module states, crossplay provider availability, station/recipe counts, wildlife/landmark counts, registered worlds and server TPS/average tick time. They do not expose Floodgate keys, XUIDs, tokens, or IP addresses. See [operations and balance](docs/OPERATIONS.md).

Use `/hydration` to inspect hydration, `/wildlands hud on|off` to save your HUD preference, and `/wildlands admin hydration <online-player|uuid> <0..100>` for permission-gated administration. The HUD/status commands use `ggwpwildlands.use`; the admin setter uses `ggwpwildlands.admin` and supports console. `/season [info [world]]` uses the use permission; `/wildlands admin season <season> [world]` uses the admin permission. Exploration commands are described below.

## Configuration and modules

`config.yml`:

```yaml
schema-version: 1
storage:
  file: wildlands.db
  save-interval-seconds: 30
modules:
  player-records: true
  hydration: true
  hud: true
  temperature: true
  wetness: true
  shelter: true
  seasons: true
  worldgen: true
  wildlife: true
  crafting: true
  nutrition: true
  landmarks: true
debug:
  enabled: true
```

- The database filename must be a simple name ending in `.db`, within the plugin data directory.
- Save interval must be an integer from 5 to 3600 seconds.
- Storage filename and interval changes require a restart. A reload containing either change is rejected as a whole.
- `player-records` can be disabled independently. Disabling flushes sessions, cancels the save task, and unregisters its listeners; enabling starts tracking currently online players.
- `debug.enabled: false` disables debug commands.
- `messages.yml` configures the response prefix and common error messages as plain text.
- Invalid YAML, invalid types, unsupported schema versions, and unknown modules are rejected. Initial failure disables Wildlands; reload failure retains the prior configuration.
- Future gameplay modules are not registered and cannot be enabled accidentally.

The configuration loader validates a candidate snapshot on the storage worker, then applies it on the server thread. Module lifecycle failures trigger cleanup and reverse-order rollback. Shutdown stops modules in reverse registration order.

## Survival crafting (Milestone 7)

Enable `crafting` and `nutrition` in `config.yml` for an existing installation; missing flags remain false when upgrading. Fresh installs enable both. `/wildlands crafting` explains equipment. Recipes are discovered on join and module enable; use a standard crafting table and recipe book. Recipe definitions are in `items/RecipeManager.java`.

| Equipment | Native interface | Use |
| --- | --- | --- |
| Rain Collector | Barrel | Empty glass bottles become clean water while rain reaches the open collector; roofs, dry biomes and snow prevent collection |
| Basic Water Filter | Barrel | One charcoal per step: contaminated → questionable → untreated; boil the result before drinking |
| Improved Water Filter | Barrel | One charcoal turns any unsafe freshwater bottle clean; salt is never treated |
| Cooking Rack | Smoker | Ordinary food cooking and fuel; affordable campfire/iron-nugget recipe |
| Water Boiler | Furnace | Native furnace recipe boils standard freshwater bottles; ordinary furnaces also support it |
| Charcoal Kiln | Furnace | Ordinary log-to-charcoal smelting; campfire/cobblestone construction |
| Improved Stove | Smoker | Same cooking interface with twice the fuel duration by default |
| Food Drying Rack | Barrel | One cooked meat/fish and charcoal become preserved food, under a roof within three blocks horizontally and two vertically of a lit campfire |
| Rain Cloak | Leather chestplate | Reduces rain wetness gain to 35% by default; swimming and drying remain normal |
| Waterskin | Water potion / empty bottle | Three normal drinks of clean water; native consumption retains the remaining charges and finally returns an empty skin |

The waterskin is made with three leather and three standard Clean Water bottles in a shapeless recipe. Refill its empty skin with three standard Clean Water bottles. The three bottles are transferred into the skin and consumed by crafting; there are no extra bottle outputs. Empty skins cannot scoop river water or be processed by stations. Anvil-renamed water bottles/equipment may not match exact upgrade/refill recipes; use the standard crafted items. Native consumption and placement use the main hand on either edition; no offhand dependency exists.

`crafting.yml` controls station cadence, batch size, loaded-station cap, cloak protection and stove fuel efficiency. At defaults, up to eight loaded barrel stations perform one operation every ten seconds, rotating fairly; more stations increase the time between visits. Full inventories consume nothing. Stations do not accumulate offline work, force-load chunks, or scan all containers continually. At the loaded cap, additional stations remain ordinary storage until capacity is available and they are reopened or their chunk reloads. Changing cadence or water boiling duration requires a server restart. Disable crafting to remove recipes/tasks and gear effects; stored contents and item identity remain intact. Tagged waterskins cannot be consumed while crafting or hydration is disabled, or hydration data is loading.

`food.yml` controls the modest preserved-food saturation bonus (default 1); the independently disabled `nutrition` module removes that bonus. Vanilla hunger and ordinary foods remain viable. Preservation adds no spoilage timer or food-category penalties.

Station identity uses native TileState persistent data; inventories and item tags are saved in Minecraft chunks/player inventories. Break a station normally to retain its equipment item and normal content drops. Destruction by fire/explosion can lose station identity through ordinary vanilla drops. Back up the world and player data alongside SQLite: this avoids conflicting copies of native container contents in a second database. No SQLite migration is needed for this milestone.

Operators/console can inspect `/wildlands admin crafting info` or give one item with `/wildlands admin crafting give <exact-online-player> <item>`; tab completion lists item identifiers. Giving to a full inventory is refused. Recipes do not gate or remove ordinary vanilla progression. No mod or resource pack is required. See `MILESTONE-7-REVIEW.md` and the crossplay matrix for verification and deferred client checks.

## Exploration (Milestone 8)

Enable `landmarks` in `config.yml` on existing installs. `landmarks.yml` controls bounded player sampling, discovery distance, landmark caps, expedition departure/return distances and coordinate display. Natural rainforest landmarks are registered when their chunks load; nearby players discover them automatically. The journal records first discovery times against server UUIDs, including Floodgate UUIDs. Undiscovered waypoints are not revealed to players.

| Command | Use |
| --- | --- |
| `/landmarks [page]` or `/wildlands landmarks [page]` | Review your discoveries and personal camp, twenty waypoints per page |
| `/landmark <name|uuid|camp>` | Inspect a known waypoint, direction and horizontal distance |
| `/wildlands navigate <name|uuid|camp>` | Track the route in the HUD; a compass held in your main hand is pointed at the waypoint |
| `/wildlands navigate off` | Stop the HUD route and reset a held Wildlands compass |
| `/wildlands camp [set|info]` | Save one personal camp waypoint while standing on dry ground under a roof near a lit campfire; another set replaces it |
| `/wildlands expedition` | Review trail knowledge, longest distance from camp, active/completed expeditions and preparation advice |
| `/wildlands admin landmarks list [world]` | Console-compatible registry diagnostics, capped at twenty results |
| `/wildlands admin landmarks debug <player-uuid>` | Inspect persisted exploration state by UUID |

Player commands require `ggwpwildlands.use`; administration requires `ggwpwildlands.admin`. Camp waypoints are private to their owner. Hold a normal compass in the main hand when setting a route: its native lodestone target works without building a lodestone. The compass stores that waypoint at the time of setting; rerun navigation while holding it after relocating camp, or to update another compass. Neither navigation nor reviewing discoveries teleports players or loads destination chunks. Different-world routes ask you to travel to that world first. Ordinary maps and coordinates remain available. `show-coordinates: false` hides only Wildlands' waypoint-coordinate messages, not vanilla coordinates.

At defaults, leaving 256 blocks from a saved camp starts an expedition; returning within 32 blocks records its completion. Farthest distance is straight-line horizontal distance from camp, not a simulated walking odometer. Reconnects preserve the active trip. Discovery and expedition experience grant cosmetic trail ranks (Novice, Scout, Explorer, Pathfinder), with no vanilla equipment or recipe locks, travel taxes or extra damage timers. One saved camp helps plan remote journeys; build additional physical shelters wherever useful.

Generator profile **2** adds deterministic stone-brick ruins with walkable entrances in suitable dry terrain. Fresh `worldgen.yml` defaults to profile 2. Existing configuration files and registered worlds retain profile 1 until you choose profile 2 for a **new** world; already registered profiles never change. Existing worlds gain natural landmark discovery but no automatic ruin retrofits or edits to player builds. Ruins contain no respawning loot, traps, forced enemies or client-only blocks. Their chunk-buffer generation never reads neighboring chunks. Removing the landmarks module stops discovery/navigation/trip tracking; recorded data and generated structures remain. Sampling cadence changes require restart.

SQLite schema 7 adds landmark, discovery and expedition tables, and transactionally preserves old world profiles while allowing profile 2. A spatial index in memory limits proximity checks to nearby cells; player work is batched. Database snapshots remain asynchronous and batched. Back up SQLite plus the shared world/player directories. See `MILESTONE-8-REVIEW.md` and the crossplay matrix for actual verification and deferred client acceptance.

## Architecture and persistence

Packages separate `core`, `config`, `storage`, `commands`, and `crossplay`. `WildlandsModule` is the lifecycle extension point; `PlatformAdapter` isolates platform classification. Hydration and HUD are separate modules; survival rules are independent of crossplay detection.

The plugin registers commands immediately in a STARTING state, initializes files and SQLite asynchronously, and becomes READY only after validation, schema initialization, and module enablement. Any startup failure disables the plugin with a detailed server log.

A single dedicated storage worker owns JDBC access. Server-thread listeners capture immutable UUID/name/time records, then enqueue database work. No Bukkit player or world access occurs on that worker. Online records are saved in batched transactions; joins and quits also queue saves. Failed batches remain in memory for retry on the next save. Failed player loads retry on that cadence without requiring reconnect; diagnostics distinguish LOADING from LOAD_FAILED. Shutdown queues a final flush and waits up to 20 seconds for the worker; errors or timeout are logged prominently. As with any buffered persistence system, a process crash can lose records not yet committed.

SQLite uses prepared statements, WAL, a 5-second busy timeout, FULL synchronization, and schema versioning via `PRAGMA user_version`. Schema version 7 retains player, hydration, environment, seasonal, world and wildlife records and adds landmarks, UUID discoveries and expedition state. The migrations are transactional. Player timestamps are UTC epoch milliseconds. UPSERT preserves the earliest first-seen time and latest last-seen/name, preventing stale reconnect writes from regressing records. A newer schema is refused rather than downgraded.

Use a normal server shutdown before copying the database for backup. If copying a live database, use an SQLite-aware backup tool; copying only the `.db` file can omit committed WAL data. Never delete or replace the database to recover from a configuration error.

Floodgate/Geyser APIs are discovered through their enabled plugin classloaders. Reflection is intentionally confined to optional bridge classes, using documented public API methods. No optional API is bundled, and absence or incompatibility does not prevent startup. Integration refreshes on provider lifecycle changes and Wildlands configuration reload. UUIDs come directly from Paper for Java, Floodgate, and linked accounts.

## Validation and remaining checks

Automated tests cover configuration validation, module lifecycle/rollback, transactional UUID persistence, rename/reconnect ordering, schema downgrade refusal, failed-write retry, graceful shutdown, optional API absence/failure, survival rules, native item conservation, wildlife behavior, frozen terrain fingerprints, exploration and concurrent sessions. Milestone reviews and docs/validation/ record actual results. The combined suite has 147 passing tests; live crossplay and multiplayer acceptance remains explicitly pending where noted.

Real Paper lifecycle, provider initialization, reload, and startup failure tests have passed. The local Java 26.2 and Bedrock/Geyser/Floodgate foundation checks also passed, including command permissions, reconnects, and persisted records after restart. See `CROSSPLAY-COMPATIBILITY.md`; automated fake-provider tests are not proof of live Geyser/Floodgate interoperability.

## Hydration (Milestone 2, accepted)

Fresh installations enable hydration and HUD. Existing foundation configs without these flags default them to false: explicitly add `hydration: true` and `hud: true` under `modules` to opt in. Restart after installing the new JAR. Back up the stopped server before upgrading; version-1 binaries cannot open the upgraded database.

Fill a glass bottle from a water source or water cauldron. Rivers produce UNTREATED water; other freshwater pools produce QUESTIONABLE water; swamps produce CONTAMINATED water; oceans produce SALT water. Configured spring source blocks and exposed rain filling an empty cauldron produce CLEAN water. Mixing preserves the worse quality. Washing equipment contaminates cauldron water. Vanilla/unknown water bottles are QUESTIONABLE; item names never determine quality.

Place a water bottle on a lit campfire to boil it (20 seconds by default), then collect the clean bottle. Salt water cannot be purified by boiling. Effect potions are excluded. The recipe accepts the plugin's standard bottles and plain vanilla water bottles; anvil-renamed or otherwise modified bottles may not match the exact recipe. No resource pack or client mod is required.

Normal hydration loss is 0.025 per second, sampled every five seconds; activity increases use. Offline, dead, creative and spectator players do not deplete. Below 25%, natural food-based healing is reduced. Only prolonged severe dehydration causes direct damage. Unsafe water carries configurable hunger-effect risk, rather than guaranteed illness. Respawn restores 70 hydration. Values, risks, timings and spring coordinates are in `hydration.yml`. Spring entries use `{world: world, x: 10, y: 70, z: 20}` and apply only at the exact source block; custom terrain generation is a later milestone.

Boiling-duration and storage-setting changes require a restart. Other validated hydration settings reload with `/wildlands reload`. Disabling hydration stops its gameplay effects and removes collection/drinking/boiling listeners and the recipe. Cauldron provenance tracking remains active to invalidate stale metadata while gameplay is disabled; it performs no scans. Disabling HUD stops its action-bar task. HUD preferences use the existing hydration record, which remains loaded and periodically saved while either hydration or HUD needs it. With hydration disabled, the HUD can still display enabled environment fields and `/wildlands hud on|off` still works. Disabling both consumers saves and releases those records.

Hydration does not overwrite persisted state before loading succeeds. Failed loads retry, failed writes remain pending, and reconnects can recover the latest pending snapshot. The database worker never reads Bukkit world state. Item and cauldron quality use item/chunk persistent data saved by Minecraft; external world-editing tools must clear stale cauldron tags when replacing blocks outside normal Bukkit events.

## Environment (Milestone 3)

Temperature and wetness are sampled every five seconds on the server thread and saved in the same batched SQLite worker as the other player data. Temperature uses the current biome, elevation above sea level, time of day, sprinting, local rain, water exposure, shade and nearby lit campfires. The Overworld day/night adjustment defaults to ±3 degrees; sprinting adds 2 degrees. Both are configurable. Hot conditions increase hydration loss. Cold and wet conditions reduce natural food-based healing to 75% by default, while potion healing stays unchanged. Wetness rises in local rain and water, then dries gradually and faster under a roof or near warmth.

Shelter is evaluated only around the player: a short upward roof check, four bounded wall checks, dry ground and a limited-radius lit-campfire search. It does not prescribe a building shape, force-load chunks or scan the world. `/wildlands status` shows temperature, wetness and shelter state. `/wildlands admin temperature <online-player>` lets operators inspect an online player's environment state. Tune all values in `environment.yml`; changing its sampling interval requires a restart, while other valid values reload normally. The HUD appends temperature and wetness when their modules are enabled. Both clients require no resource pack or mod.

## Seasons (Milestone 4)

`seasons.yml` configures DRY → TRANSITION_TO_WET → WET → MONSOON → TRANSITION_TO_DRY. Durations use 24,000-tick game days, advanced only while a managed world is loaded and seasons are enabled. Sleep and `/time` do not skip seasons, and server downtime does not advance them. Only explicitly listed Overworld names are managed; Nether, End and other worlds retain vanilla behavior. State is saved by world UUID, so recreating a world starts a separate clock.

Fresh installs enable seasons; existing configs without the flag default it to false. Add `seasons: true` under `modules` to opt in. Profiles specify rainfall probability, conditional thunder probability, temperature offset and natural crop-growth multiplier. Defaults evaluate weather every five minutes and the clock every ten seconds. Wet weather provides more opportunities for existing rainwater collection; seasonal weather never removes water blocks. Crop modifiers affect normal growth events, preserve maturity limits and leave bonemeal unchanged. Disabling seasons saves the clock and restores weather captured before seasonal control began. No world scans or forced chunk loads occur.

`/season` inspects your world; console may omit the world only when one configured Overworld is loaded. `/wildlands admin season monsoon world` resets that world's season counter and evaluates its weather immediately. The HUD and status display the current season. Failed loads retry without overwriting saved state; failed writes remain pending. Valid profile changes reload; changing `clock-seconds` requires restart. Weather changes apply at the next evaluation, and temperature changes at the next environment sample.

The combined build uses SQLite schema 7. Back up the stopped server's database and complete shared world directory before installing it; earlier binaries cannot open a newer schema. A rollback requires the matching database/world backup, not just an older JAR. Further live crossplay checks use the collective checklist.

## Rainforest world generation (Milestone 5)

New installations enable the `worldgen` module; existing configs must explicitly opt in. No existing world is converted, and no new world is created automatically on a fresh install. From console or an administrator account, use `/wildlands admin world create wildlands 4509` to create a separate rainforest world, and `/wildlands admin world list` to inspect registrations. Names must be safe lowercase directory names, up to 48 characters; seeds are signed 64-bit integers. Existing world directories are refused. Up to 16 worlds may be registered. Teleportation uses the server's normal administrator/world management tools.

World profiles are frozen in SQLite before creation. Registered worlds reload automatically with their original seed and generator after restart, including when `worldgen` is disabled. Disabling that module stops new creation; attached worlds retain their generator to protect existing terrain. Paper 26.2 stores these dimensions under the shared level's `dimensions/minecraft/<name>/` directory. Back up the plugin database and the entire shared level together. Missing registered world metadata fails plugin startup; loaded UUID/seed mismatches are refused. Do not configure another generator or a world-management plugin to load the same world first.

`worldgen.yml` controls the sea level, tree density, caves and ores for future worlds. Valid changes reload without altering existing worlds. Both frozen generator profiles use continuous seeded terrain with river contours, stepped waterfall valleys, floodplains, wetlands, highlands, rocky escarpments, dense rainforest, bamboo and clearings. Cave tunnels include occasional rocky entrances. Ore clusters preserve mining progression. Broad crowns at varied heights, rare enormous trees, ferns and mossy boulders provide recognizable terrain using vanilla blocks. Generated canopy leaves persist to preserve broad crowns. Profile 2 adds ruins; existing profile 1 remains unchanged. No custom assets or client mods are required.

Generation writes only the requested chunk buffer and clips neighboring tree/rock shapes at the boundary; candidates are derived from absolute coordinates, independent of chunk generation order. Vanilla terrain, decorations and structures are disabled in these custom worlds; vanilla mob generation remains enabled. `/wildlands status` shows the modeled region. Fresh seasonal defaults include `world` and `wildlands`; add a differently named world to `seasons.yml` if it should participate. New spawns use a bounded dry-ground search with a preference for clearings; restored spawns remain unchanged.

## Wildlife (Milestone 6)

The first species is Jaguar, represented by a named adult vanilla ocelot on both editions. There are no custom textures, client mods, entity disguises or packet dependencies. This representation preserves the same server hitbox and interaction behavior for both clients. Appearance and player combat still need final combined-build acceptance.

Fresh configs enable `wildlife`; existing configs default it to false. `wildlife.yml` explicitly lists managed Overworld names (default `wildlands`). Natural spawning uses loaded rainforest habitat near survival/adventure players, with four bounded attempts per minute, configurable probability, one animal per 128-block territory cell, and population caps including unloaded registered animals. No new chunks are loaded for spawning. Other world generators are not automatically populated. Console-compatible admin commands are `/wildlands admin wildlife list` and `/wildlands admin wildlife spawn <world> <x> <y> <z>`; the latter requires a loaded, dry, safe, uncrowded location within the world border and refuses nearby lit campfires.

Jaguars are neutral at a distance during the day and stalk at night. Entering their warning range produces a hiss and chat warning; backing away avoids attack. Defaults allow five seconds to retreat before close engagement. Detection is limited to 20 blocks with line of sight, territory to 40 blocks and pursuit to 20 seconds. Three nearby survival/adventure players or a lit campfire deter them. Creative/spectator players are excluded. Optional night hunting targets chickens and rabbits. Default health is 20 and each bite deals 3 ordinary, armor-sensitive damage, at most once every two seconds; Peaceful difficulty suppresses bites. No huge health pools or potion-effect penalties are introduced.

Entity decisions rotate through a capped batch; navigation updates are throttled and require a loaded search corridor. UUID homes are stored in SQLite, while ownership/home tags and the animal itself persist in Minecraft entity chunks. Unloads preserve population; death or permanent removal releases it. Failed writes retry without resurrecting a dead pending animal. Back up entity chunks and the database together. Third-party tools deleting entity files without normal Bukkit events can leave stale population records.

Disabling wildlife cancels spawning/decisions and stops paths/targets without deleting animals. Ownership bookkeeping and attack cancellation remain active to prevent accidental vanilla prey attacks from tagged cats. In the same server session these cats remain passive; after a restart they can use vanilla ocelot movement until re-enabled. Reloading world lists adopts matching loaded tagged animals. Scheduling changes require restart; other validated rules apply to subsequent decisions. Health changes apply on spawn/adoption, not as free healing of existing animals.
