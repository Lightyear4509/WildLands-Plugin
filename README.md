# GGWP Wildlands

Server-authoritative rainforest survival project. **Milestones 1–3 are implemented.** Hydration adds drinking, water quality, campfire boiling and an optional action-bar HUD. Environment adds temperature, wetness, shade, campfire warmth and shelter status. Further live checks are deferred to the combined build at the user's request; actual test status is recorded in CROSSPLAY-COMPATIBILITY.md.

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

Install **`build/libs/GGWPWildlands-0.3.0.jar`**. The `-plain.jar` is a development artifact without SQLite; do not install it. Tests run during `build`; HTML results are in `build/reports/tests/test/index.html`.

The build targets Java 25 bytecode and pins Paper API `26.2.build.121-stable`. SQLite is bundled in the distributable, including its native libraries and JDBC service descriptor. No runtime dependency download is required by Wildlands. The Gradle wrapper distribution is SHA-256 pinned, and dependency versions are locked in gradle.lockfile. SQLite uses Paper's provided SLF4J API rather than bundling a second logging API. Java 25 may warn about SQLite native-library access unless the server is launched with --enable-native-access=ALL-UNNAMED.

## Installation

1. Install Java 25 and a Paper **26.2** server.
2. Stop the server and copy the distributable into `plugins/`.
3. Start the server. Look for **Foundation ready** in the log.
4. Configure `plugins/GGWPWildlands/config.yml`, `messages.yml`, `hydration.yml`, and `environment.yml`.
5. Run `wildlands admin debug` from the server console.

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
| `/wildlands admin debug <online-player\|uuid>` | `ggwpwildlands.admin.debug` | Yes |

The use permission defaults to everyone; both admin permissions default to operators. Debug is independently permission-gated, so grant its explicit node to delegated operators. Offline debug lookups require a UUID; names are never used as persistent identifiers. Tab completion respects permissions and player visibility.

Status shows an online player's server UUID, detected platform, and record loading state. Console status reports plugin readiness. Diagnostics show server/plugin/Java versions, storage health, queued work, pending writes, last successful save, module states, and crossplay provider availability. They do not expose Floodgate keys, XUIDs, tokens, or IP addresses.

Use `/hydration` to inspect hydration, `/wildlands hud on|off` to save your HUD preference, and `/wildlands admin hydration <online-player|uuid> <0..100>` for permission-gated administration. The HUD/status commands use `ggwpwildlands.use`; the admin setter uses `ggwpwildlands.admin` and supports console. Seasons and landmarks are not registered.

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

## Architecture and persistence

Packages separate `core`, `config`, `storage`, `commands`, and `crossplay`. `WildlandsModule` is the lifecycle extension point; `PlatformAdapter` isolates platform classification. Hydration and HUD are separate modules; survival rules are independent of crossplay detection.

The plugin registers commands immediately in a STARTING state, initializes files and SQLite asynchronously, and becomes READY only after validation, schema initialization, and module enablement. Any startup failure disables the plugin with a detailed server log.

A single dedicated storage worker owns JDBC access. Server-thread listeners capture immutable UUID/name/time records, then enqueue database work. No Bukkit player or world access occurs on that worker. Online records are saved in batched transactions; joins and quits also queue saves. Failed batches remain in memory for retry on the next save. Failed player loads retry on that cadence without requiring reconnect; diagnostics distinguish LOADING from LOAD_FAILED. Shutdown queues a final flush and waits up to 20 seconds for the worker; errors or timeout are logged prominently. As with any buffered persistence system, a process crash can lose records not yet committed.

SQLite uses prepared statements, WAL, a 5-second busy timeout, FULL synchronization, and schema versioning via `PRAGMA user_version`. Schema version 3 retains player and hydration records and adds `environment_players(uuid PRIMARY KEY, temperature, wetness)`. The migrations are transactional. Player timestamps are UTC epoch milliseconds. UPSERT preserves the earliest first-seen time and latest last-seen/name, preventing stale reconnect writes from regressing records. A newer schema is refused rather than downgraded.

Use a normal server shutdown before copying the database for backup. If copying a live database, use an SQLite-aware backup tool; copying only the `.db` file can omit committed WAL data. Never delete or replace the database to recover from a configuration error.

Floodgate/Geyser APIs are discovered through their enabled plugin classloaders. Reflection is intentionally confined to optional bridge classes, using documented public API methods. No optional API is bundled, and absence or incompatibility does not prevent startup. Integration refreshes on provider lifecycle changes and Wildlands configuration reload. UUIDs come directly from Paper for Java, Floodgate, and linked accounts.

## Validation and remaining checks

Automated tests cover configuration validation, module lifecycle/rollback, transactional UUID persistence, rename/reconnect ordering, schema downgrade refusal, failed-write retry, graceful shutdown, and optional API absence/failure. See the final validation report in `MILESTONE-1-REVIEW.md` for actual results.

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
