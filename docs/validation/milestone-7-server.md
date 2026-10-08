# Milestone 7 native Paper validation

Validated October 7, 2026 on the disposable local Paper 26.2 build 121 server, Java 25.0.4.1+1, with Geyser 2.11.3-b1245 and Floodgate 2.2.5-b140 installed. No Java/Bedrock client session was requested or used. Client parity is assumed by design per the revised workflow, with final live checks deferred.

Final shaded artifact: GGWPWildlands-0.7.0.jar, SHA-256 `040D362998569FEFA28A7F5ACCBCD3A46CDB1E7DEB23C817285B06DB25FD91BA`. A clean build passed, followed by the final queue-cleanup build: 123 automated tests, zero failures/errors. The disposable database/configuration/previous JAR were backed up before installation. SQLite remained schema 6.

An ignored temporary M7Probe plugin exercised actual ItemStack metadata, Bukkit crafting lookup, native containers and furnace events. It is not included in the distributable. Test stations occupied an isolated platform at y=150 in rainforest_dev; only its existing origin chunk was temporarily force-loaded for headless testing. No production system force-loads chunks.

| Check | Evidence/result |
| --- | --- |
| Startup/coexistence | 20:49:14, 21:03:57 and 21:08:27: READY with both crossplay providers; final artifact loaded successfully |
| Item identity | All ten survival equipment kinds round-trip through actual native item tags; waterskin 0/3 capacity tags validated |
| Crafting | 20:49:25: all nine shaped equipment recipes and two shapeless waterskin/refill recipes match their intended server keys; kiln does not collide with ordinary furnace crafting |
| Full inventory | Actual rain-collector planner with stacked bottles and no output room returns no operation; unit tests also retain original input/fuel |
| Rain collection | Exposed collector yields exactly two clean bottles from two glass bottles; roofed collector retains both empty bottles |
| Filtration | Basic filter uses two charcoal to move contaminated → questionable → untreated; improved filter consumes one charcoal for clean water; adjacent salt bottle stays salt |
| Preservation | Sheltered heated rack converts exactly two cooked beef plus two charcoal into two preserved items |
| Native cooking | Furnace water boiler yields clean water; improved smoker yields cooked beef |
| Native fuel efficiency | 21:08:27: LOWEST/MONITOR probe observes actual smoker burn event changing 800 → 1600 ticks |
| Restart persistence | Normal stop 21:01:56 and restart: all seven station identities, inventories, salt/clean water and preserved foods survive; five barrel stations rediscovered. Verified again on final artifact at 21:08:27 |
| Disable/re-enable | Validated through config reload: disabling removes twelve crafting recipes/tasks and clears loaded station queue; re-enabling restores recipes and rediscovers the same native stations |
| Client checks | Not performed; waterskin animation/consumption, recipe-book UI, armor, station interactions/hoppers and food display remain for final combined-build acceptance |

Two probe assertions were corrected: a remaining-fuel check ran twelve minutes after fuel ignition, and its replacement initially assumed a 1600-tick baseline rather than the smoker's native 800 ticks. The final probe captures the actual before/after event and passes. Neither assertion indicated a failed production station operation.

Host-side OSHI performance-counter errors and a spark polling `NumberFormatException` were observed, as in this disposable environment's prior runs. Wildlands startup, station work, persistence and commands continued. They are not attributed to Wildlands; host profiling reliability will be considered in final polish.

Temporary stations/probe and forced chunk tickets are removed after validation. Native item/chunk persistence requires backing up world/player data alongside SQLite. No client mods or custom resource assets were introduced.
