# GGWP Wildlands

Server-authoritative rainforest survival project. **This release implements Milestone 1 only: the foundation.** It does not change vanilla survival, terrain, mobs, recipes, weather, or player HUDs.

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

Install **`build/libs/GGWPWildlands-0.1.0.jar`**. The `-plain.jar` is a development artifact without SQLite; do not install it. Tests run during `build`; HTML results are in `build/reports/tests/test/index.html`.

The build targets Java 25 bytecode and pins Paper API `26.2.build.121-stable`. SQLite is bundled in the distributable, including its native libraries and JDBC service descriptor. No runtime dependency download is required by Wildlands. The Gradle wrapper distribution is SHA-256 pinned, and dependency versions are locked in gradle.lockfile. SQLite uses Paper's provided SLF4J API rather than bundling a second logging API. Java 25 may warn about SQLite native-library access unless the server is launched with --enable-native-access=ALL-UNNAMED.

## Installation

1. Install Java 25 and a Paper **26.2** server.
2. Stop the server and copy the distributable into `plugins/`.
3. Start the server. Look for **Foundation ready** in the log.
4. Configure `plugins/GGWPWildlands/config.yml` and `messages.yml`.
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

Later commands such as hydration, seasons, landmarks, and HUD are deliberately not registered.

## Configuration and modules

`config.yml`:

```yaml
schema-version: 1
storage:
  file: wildlands.db
  save-interval-seconds: 30
modules:
  player-records: true
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

Packages separate `core`, `config`, `storage`, `commands`, and `crossplay`. `WildlandsModule` is the lifecycle extension point; `PlatformAdapter` isolates platform classification. Only the player-records module exists at this milestone. Empty implementations of later gameplay services have not been introduced.

The plugin registers commands immediately in a STARTING state, initializes files and SQLite asynchronously, and becomes READY only after validation, schema initialization, and module enablement. Any startup failure disables the plugin with a detailed server log.

A single dedicated storage worker owns JDBC access. Server-thread listeners capture immutable UUID/name/time records, then enqueue database work. No Bukkit player or world access occurs on that worker. Online records are saved in batched transactions; joins and quits also queue saves. Failed batches remain in memory for retry on the next save. Failed player loads retry on that cadence without requiring reconnect; diagnostics distinguish LOADING from LOAD_FAILED. Shutdown queues a final flush and waits up to 20 seconds for the worker; errors or timeout are logged prominently. As with any buffered persistence system, a process crash can lose records not yet committed.

SQLite uses prepared statements, WAL, a 5-second busy timeout, FULL synchronization, and schema versioning via `PRAGMA user_version`. The schema contains only `players(uuid PRIMARY KEY, last_known_name, first_seen, last_seen)`; timestamps are UTC epoch milliseconds. UPSERT preserves the earliest first-seen time and latest last-seen/name, preventing stale reconnect writes from regressing records. A newer schema is refused rather than downgraded.

Use a normal server shutdown before copying the database for backup. If copying a live database, use an SQLite-aware backup tool; copying only the `.db` file can omit committed WAL data. Never delete or replace the database to recover from a configuration error.

Floodgate/Geyser APIs are discovered through their enabled plugin classloaders. Reflection is intentionally confined to optional bridge classes, using documented public API methods. No optional API is bundled, and absence or incompatibility does not prevent startup. Integration refreshes on provider lifecycle changes and Wildlands configuration reload. UUIDs come directly from Paper for Java, Floodgate, and linked accounts.

## Validation and remaining checks

Automated tests cover configuration validation, module lifecycle/rollback, transactional UUID persistence, rename/reconnect ordering, schema downgrade refusal, failed-write retry, graceful shutdown, and optional API absence/failure. See the final validation report in `MILESTONE-1-REVIEW.md` for actual results.

Real Paper lifecycle, provider initialization, reload, and startup failure tests have passed. The local Java 26.2 and Bedrock/Geyser/Floodgate foundation checks also passed, including command permissions, reconnects, and persisted records after restart. See `CROSSPLAY-COMPATIBILITY.md`; automated fake-provider tests are not proof of live Geyser/Floodgate interoperability.
