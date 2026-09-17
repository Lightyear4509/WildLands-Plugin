# Milestone 1 real-server validation

Date: 2026-09-17. Host: Windows 11 x64. Java: Temurin 25.0.4.1+1-LTS. Paper: 26.2 build 121 (a2a42c5). Worlds are disposable. Minecraft EULA acceptance was explicitly provided by the user.

## Observed passes

| Scenario | Action and observed result |
| --- | --- |
| No optional plugins | Boot with only GGWPWildlands. Reached Foundation ready; console debug reported READY, SQLite OK, player-records ENABLED, Geyser ABSENT, Floodgate ABSENT. |
| Console commands | Help and aggregate debug returned expected messages without assuming a player sender. |
| Invalid reload | Replaced save interval with text. Reload rejected it and retained the active configuration. |
| Module disable | Restored valid storage values, set player-records false, reloaded. Debug reported DISABLED with plugin READY and storage OK. |
| Module enable | Restored baseline config and reloaded. Module returned to ENABLED. |
| Shutdown | Stopped via console. Plugin disabled and server exited normally; no database shutdown errors appeared. |
| Crossplay installed | Restarted with Geyser 2.11.3-b1245 (2808f7d) and Floodgate 2.2.5-b140 (8780fa4). Foundation reached READY; both bridges reported AVAILABLE. |
| Network binding | Java listener used 127.0.0.1:25565. Geyser log and UDP endpoint inspection confirmed localhost port 19132. |
| Malformed initial YAML | Separate isolated server on localhost port 25566. Broken YAML caused logged startup failure and plugin disable; Foundation ready never appeared. |
| Future schema | Separate server with user_version=99 and sentinel table/data. Startup refused migration and disabled Wildlands. After shutdown, schema 99 and sentinel value were unchanged. |

Selected console evidence:

```text
[GGWPWildlands] Foundation ready. Geyser: ABSENT; Floodgate: ABSENT
[Wildlands] State: READY; sessions: 0
[Wildlands] SQLite: OK; queued jobs: 0; pending records: 0
[Wildlands] Reload rejected. Previous configuration retained; see the server log.
[Wildlands] player-records: DISABLED
[Wildlands] Configuration reloaded.
[Wildlands] player-records: ENABLED
[GGWPWildlands] Foundation ready. Geyser: AVAILABLE (2.11.3-SNAPSHOT); Floodgate: AVAILABLE (2.2.5-SNAPSHOT (b140-8780fa4))
[Geyser-Spigot] Started Geyser on 127.0.0.1:19132
java.sql.SQLException: Unsupported database schema 99; refusing migration
[GGWPWildlands] Disabling GGWPWildlands v0.1.0
FUTURE_SCHEMA_AND_DATA_UNCHANGED
```

Raw local evidence is under ignored .runtime/milestone-1/baseline-no-crossplay.log, .runtime/milestone-1/logs/, and .runtime/failure-tests/{malformed-config,future-schema}.log. Runtime keys, player data, third-party binaries, and worlds are not committed.

Paper emitted OSHI/Windows performance-counter warnings before plugin enablement. These did not prevent server startup. They are host diagnostics, not a Wildlands failure. Attempting a command after intentionally disabling the plugin produces Paper's standard disabled-plugin command exception.

## Regression found during review

A simulated temporary SQLite failure on join left the cached session unloaded after writes recovered. A new test failed against the previous code. PlayerManager now retries incomplete loads on its configured save cadence, avoids duplicate in-flight loads, and rejects stale completion callbacks. The full 26-test suite passes with the fix. Debug now distinguishes LOADING and LOAD_FAILED.

## Actual client acceptance and restart persistence

Both real clients connected and reconnected with unchanged UUIDs. Java commands were executed at 09:19 local time; Bedrock commands at 09:22–09:23. The user confirmed command output and permission denial for admin debug on both clients. Console diagnostics independently reported the Bedrock session LOADED and platform BEDROCK. After shutdown at 09:24 and restart at 09:26, both stored records retained exactly the same first-seen and last-seen timestamps. Provider detection returned AVAILABLE again. Proxy/Geyser-only topologies and exhaustive completion UI behavior remain untested.

The Java 26.3 client mismatch was resolved by launching the specified Java 26.2 client. Milestone 1 acceptance is complete for this local topology; later milestones need new client tests.
