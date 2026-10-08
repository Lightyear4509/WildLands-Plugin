# Administration and performance

`wildlands admin health` is a console-compatible alias for the global debug summary. It reports plugin/server/Java versions, storage health/queued jobs/pending records/last save, provider detection, active module states, station/recipe counts, landmark/session counts, wildlife population and registered worlds, plus server-wide one-minute TPS and average tick time. The server metrics include all plugins and terrain generation; they do not attribute cost to Wildlands. Health and debug require the dedicated debug permission and enabled debug configuration.

Keep a stopped-server backup of the plugin database/configuration and entire shared level, including dimension and entity files. Schema 7 cannot be opened by older plugin binaries. Roll back the matching database and world backup together; replacing only the JAR is insufficient. SQLite snapshots run on one worker with prepared transactions, failed writes retained for retry and shutdown flushing. A sustained non-OK storage state or growing pending queue needs investigation before restarting or deleting data.

## Work limits

| System | Default cadence / bound |
| --- | --- |
| Hydration | Every 5 seconds; online sessions only |
| Environment and shelter | Every 5 seconds; bounded local block samples |
| HUD | Every 2 seconds; maximum 96 text characters |
| Season clock / weather | 10 seconds / 300 seconds; configured loaded worlds |
| Wildlife | At most 8 animals every 10 ticks; bounded loaded navigation; spawn attempts every 60 seconds |
| Stations | At most 8 loaded barrels every 10 seconds; tracking cap 512 |
| Discoveries | At most 32 sessions every 2 seconds; nearby spatial cells; registry cap 4096 natural landmarks per world |
| Persistence | Immutable snapshots every 30 seconds by default; serialized transactions |
| Custom terrain | Only supplied chunk buffers; deterministic absolute-coordinate features; no cross-chunk writes |

Limits are configurable within validated bounds. Increasing them increases work. Station operations rotate fairly through tracked locations; with 512 active stations and a batch of 8, a station may wait roughly 640 seconds between visits. Size batches/caps for actual active infrastructure. Lowering the cap on reload removes excess scheduled locations without changing inventories or block tags. Untracked stations remain ordinary storage until capacity opens and they are reopened or their chunk/module reloads. Disabled module items remain intact.

Terrain noise avoids unused interpolation corners and a redundant cave sheet; chunk decoration reuses already computed terrain columns. Complete block fingerprints protect frozen generator versions 1 and 2. Spawn selection searches a bounded model grid and asks Paper for one chosen dry column, avoiding the broad default biome spawn search. Restored worlds retain their existing spawn. New chunk generation still includes Paper lighting, structures/mobs and neighboring generation dependencies. Use ordinary exploration or a dedicated paced pregenerator; synchronous console force-loading of many distant new chunks can block the main thread. No tested capacity claim is made for large player counts.

## Balance

At the base resting rate, 100 hydration lasts about 67 minutes; hot activity shortens it. Moderate dehydration affects natural recovery before damage. Default severe damage begins only at zero after 120 seconds, at one damage every 30 seconds. Unsafe-water illness is probabilistic, clean water never causes it and salt cannot restore hydration or be boiled safe. Rain and seasonal water availability do not erase terrain water. Wet/cold conditions modify recovery rather than constantly damaging players.

Jaguar health is ordinary 20, warning gives five seconds to withdraw, detection is 20 blocks and pursuit is capped at 20 seconds. Armor, groups and campfires remain useful. Preserved food adds a small reserve without spoilage chores. Expedition ranks are cosmetic and do not lock vanilla progression. These are initial tunable defaults; longer live multiplayer balance sessions are pending under the user's revised testing workflow.

## Reload and optional providers

Use `wildlands reload` for validated gameplay settings/module flags. Storage and scheduling interval changes require a normal server restart; invalid reloads retain previous settings. Fresh installs enable all milestone modules, while existing files opt in to newly introduced flags. World profiles are frozen: editing worldgen.yml affects only future worlds. Missing registered world metadata is a startup error, not permission to recreate terrain.

Geyser/Floodgate are optional and are detected through guarded runtime bridges. Without them, Java gameplay remains available and health reports ABSENT. Bedrock players use their supplied server UUID, never a name as a database key. Provider authentication/configuration belongs to those plugins. Wildlands logs no keys, tokens, XUIDs or IP addresses. Update providers for supported clients independently of the pinned Paper backend; do not infer compatibility from client release names alone.

The final client test sheet is [FINAL-TEST-CHECKLIST.md](FINAL-TEST-CHECKLIST.md); actual evidence is recorded per milestone in validation/ and CROSSPLAY-COMPATIBILITY.md.
