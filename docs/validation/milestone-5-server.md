# Milestone 5 server validation

October 7, 2026: disposable Paper 26.2 build 121, Java 25.0.4.1, Wildlands 0.5.0, Geyser 2.11.3 build 1245 and Floodgate 2.2.5 build 140. The stopped schema-4 database, configuration and previous JAR were backed up in .runtime/milestone-5-backup before the schema-5 upgrade.

At 19:49:14 console created `rainforest_dev` with seed 4509. Creation completed at 19:49:23 with UUID `457de4f2-da59-4334-9455-69983be580ae`. Existing-world creation (`world`) and unsafe name (`../bad`) were refused. Actual blocks at (0,-64,0) and (0,64,0) matched bedrock and the modeled grass surface; both checks printed PASS. The server saved 654 custom-world chunks and stopped normally at 19:52:24.

This check caught an obsolete assumption about the world directory/uid.dat layout. The implementation was corrected to Paper's shared level directory (`dimensions/minecraft/<name>/`) and metadata.dat, using `Server.getLevelDirectory()` and `WorldCreator.ofKey()`. See [Paper's configuration layout](https://docs.papermc.io/paper/reference/configuration/) and [WorldCreator API](https://jd.papermc.io/paper/26.2/org/bukkit/WorldCreator.html). Tests now verify safe dimension path resolution and forbid reserved vanilla dimension names.

The corrected build restarted at 19:54:33 with `modules.worldgen: false`. The saved rainforest dimension loaded with its custom generator and the same UUID, before Foundation ready. New-world creation remained disabled. No live Java/Bedrock client results are claimed; visual traversal, mining, navigation and multiplayer acceptance remain deferred to the combined build.

At 19:56:22–23 server checks passed: river water at (-1024,63,-800), RIVER biome at that point, waterfall water at (-848,72,-896), highland grass at (-1024,102,-64), and rocky stone at (-1024,103,160). Temporary chunk tickets were explicitly removed. The server stopped normally at 19:56:27.

Loading these four distant areas back-to-back triggered a 10-second watchdog thread dump at 19:55:52 while the command waited for new chunks. It was not a crash; the server recovered and completed all checks. This is evidence for Milestone 9 performance work, not proof of multiplayer generation throughput. Final clean build: 99 tests, zero failures/errors. Logs are retained in the ignored disposable server directory.
