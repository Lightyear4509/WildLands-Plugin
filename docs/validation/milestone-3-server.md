# Milestone 3 server validation

## Candidate deployment — 2026-09-24

The current full build passed 70 tests with zero failures or errors. Built and deployed shaded JAR SHA-256 values matched: `62E72E2FD4B235E671CF28F7AF093123AF1725EF4709D1B8340F3C4E854B1B6E`.

The disposable Paper 26.2 build 121 server started using Java 25.0.4.1. Wildlands 0.3.0 reached READY at 15:23:49. Console debug at 15:23:55 reported SQLite OK, no queued jobs or pending records, and all six modules enabled. Geyser 2.11.3 build 1245 and Floodgate 2.2.5 build 140 initialized; Java listens on 127.0.0.1:25565 and Bedrock on 127.0.0.1:19132.

This deployment includes the migration validation, cold/wet recovery, local rain and immersion, chunk-boundary guards, daily temperature cycle, sprint warmth, and independent module-status fixes. The earlier candidate startup evidence in the Milestone 2 log predates those fixes.

The checks below cover startup, empty-session module changes, Java status/HUD display, and one real player record across restart. Other environment gameplay and Bedrock acceptance remain outstanding; startup success alone does not complete Milestone 3 acceptance.

## Console module lifecycle checks

With no players connected, reload at 15:24:48 disabled temperature and wetness while leaving shelter enabled. Debug at 15:24:52 confirmed those states and SQLite OK with no queued or pending writes. Reload at 15:25:06 enabled temperature and wetness and disabled shelter; module inspection at 15:25:11 confirmed the requested combination. Restored all three modules and reloaded successfully at 15:25:24. These checks establish empty-session lifecycle behavior; they do not prove online session retention or client-visible gameplay.

## Persistence prerequisites

A read-only SQLite query after these checks found zero rows in `environment_players`; the live console also reported zero connected clients. A restart of this empty table would not verify player environment persistence. A real client session is needed before comparing saved temperature/wetness across restart. The client connection and status check has been requested.

## Java display and restart persistence — September 24–25

Java player `Lightyear45` connected on September 24 at 15:32:48, ran `/wildlands status` at 15:33:16, and disconnected at 15:33:22. The user confirmed that chat status and the HUD displayed temperature and wetness correctly.

Before the normal shutdown at 15:34:10, a read-only query returned UUID `24e93b5d-e762-42c4-94bc-a15203e358f6`, temperature `25.264`, wetness `0.000`, and one environment record. After restart, the September 25 read-only query returned the same UUID, values (to three decimal places), and row count. Console debug at 12:13:38 reported READY, SQLite OK, zero pending records/jobs, and all six modules enabled. This verifies retention of this saved record across restart; it does not establish reconnect hydration into an online environment session, nonzero wetness retention, or Bedrock environment behavior.

The server's Paper update lookup reported a DNS failure for `fill.papermc.io`; the live console remained responsive. No network-version lookup success is claimed.

## Reload configuration regression — September 25

Review found that module enable callbacks used the previous configuration during reload. The candidate configuration is now installed before lifecycle transitions, and failure restores the previous configuration before rollback re-enables prior modules. A regression test verifies rollback ordering and restored module states. The full Gradle build passed after this change. This source change is not yet deployed to the running test server; the deployment hash above still identifies the earlier artifact.

## Updated deployment — September 25

After confirming zero connected players, the server stopped normally at 12:19:55. The updated JAR was installed; build and deployed SHA-256 both read `47385ABE907BE3DE0BEAC0A81A688D3E11E0ABE875EB4C5FC3A32005F7717652`. This build passed 71 tests with zero failures/errors. Paper completed startup at 12:21:14; console debug at 12:21:25 reported READY, SQLite OK, zero queued/pending records, both crossplay providers available, and all six modules enabled.

A three-wall oak shelter was added to the disposable platform: roof spans `(20,103,9)` through `(24,103,13)`; walls cover north, east and west, leaving the south entrance open. The standing test position is `(22,100,11)`. Console fill commands succeeded. This is a test fixture, not evidence of player shelter detection. The one chunk temporarily loaded to place it was then released.

## Shelter detection regression coverage — September 25

Added tests that exercise the actual bounded shelter scan against a mocked world: exposed versus roofed ground, three enclosing walls with an open entrance, liquid at the player's feet, and lit/unlit campfires at and beyond the configured radius. The complete build passed 73 tests with zero failures/errors after correcting the test world's location lookup and material mocks. Production sources and the deployed JAR did not change in this test-only addition. These checks do not replace live Java/Bedrock environment acceptance.

## Independent HUD lifecycle fix — September 25, not yet deployed

Review found that disabling hydration removed the session holding the HUD preference, hiding environment fields as well. Hydration records now remain tracked while either hydration gameplay or HUD needs them. Disabled hydration skips gameplay updates and event effects; HUD rendering omits hydration while retaining enabled environment fields. The service regression now checks preference updates while hydration is disabled, rejected hydration mutations/drinks, unchanged hydration on respawn, and session release when the last consumer stops. The full build passed. This production change still requires deployment and live module-toggle validation; the running server uses the earlier artifact identified above.

## HUD rendering regression and deployment — September 25

The scheduled HUD callback now has regression coverage verifying environment text and normal color with hydration disabled, preference-off suppression, task cancellation, and preference tracking lifecycle. The full build passed 74 tests with zero failures. The deprecated respawn-event constructor in the service test was replaced by an event mock.

The previous server session handle was missing and neither local test port was listening. The updated JAR was copied into the disposable server; both build and deployment SHA-256 were `ED5F69C16B7EEC746B648EDD93B3960D4561CA7D22BC549989EF2F04ABF18A73`. A new server process started at 12:41:00. Startup and live module results are recorded below when verified.

The plugin reached READY at 12:41:47. Reload at 12:42:00 disabled hydration while retaining HUD and all environment modules. Debug at 12:42:10 confirmed the intended states and SQLite OK with zero queued/pending work. Restored hydration and reloaded successfully at 12:42:14. These were empty-session lifecycle checks; online preference retention and client HUD rendering still require live acceptance.

## Bedrock environment session — September 25

Floodgate player `.Lightyear4509` joined at 12:43:02 with UUID `00000000-0000-0000-0009-01fe76725ed6`. Console environment inspection at 12:43:11 reported temperature 17°C, wetness 0%, and Exposed. After teleporting the test player into the prepared shelter at 12:43:18, inspection at 12:43:27 reported temperature 15°C, wetness 0%, and Sheltered. This verifies server-side environment sampling and shelter detection for a real Floodgate player. Client display confirmation was requested and is pending; the two-degree difference is consistent with shade but is not a controlled isolation of the time-of-day factor.

The user subsequently confirmed “Sheltered and both displays work” on Bedrock. Rain testing began at 12:43:52; results remain separate from this display confirmation.

The user also confirmed that wetness increased outside in rain and decreased inside the shelter. This records the Bedrock rain-exposure and shelter-drying checks as passed. Java rain/drying, campfire warmth, immersion, and the remaining online module/persistence checks are not implied by that confirmation.

On September 28 the user confirmed Java also passed the same rain/shelter-drying test. Rain-exposure wetness increase and shelter drying are therefore user-confirmed on both editions. Campfire warmth, immersion and online module/persistence checks remain outstanding. The old server session handle is no longer available, and neither local test port was listening when checked on September 28.

## Nonzero wetness restart retention — September 28

Before starting the stopped server, a read-only database query found two environment records: Bedrock UUID `00000000-0000-0000-0009-01fe76725ed6` at temperature 23.492 and wetness 100.000; Java UUID `24e93b5d-e762-42c4-94bc-a15203e358f6` at temperature 25.264 and wetness 0.000. Paper reached READY at 13:03:26 with both crossplay providers available. A second read-only query after startup, before either player connected, returned the same two records and values to three decimal places. This establishes nonzero Bedrock wetness retention across server startup, not yet online session restoration. Weather was cleared for the next immersion/warmth test, and the user selected Bedrock for that session.

Bedrock reconnected at 13:04:04. Console inspection at 13:04:25 showed an active environment record with 40% wetness after dry-weather sampling. After teleporting the player into the shallow pool at 13:04:34, inspection at 13:04:42 showed 100% wetness and 13°C. The user confirmed the HUD wetness rises in the pool. This records Bedrock immersion as passed; the later online value alone does not prove the exact initial restored value before sampling.

The test player drowned at 13:04:57 while waiting for the immersion response. A teleport beside the campfire was issued at 13:05:14, but respawn readiness must be confirmed before interpreting subsequent warmth/drying observations. This was a test procedure failure; future immersion checks must keep the player's head above water or end exposure promptly.

At 13:05:56 entity inspection confirmed the player had respawned with 20 health. The player was moved to dry ground at `(6.5,100,8.5)` at 13:06:03. The campfire at `(4,100,8)` was extinguished at 13:06:11; inspection at 13:06:17 reported 18°C, wetness 50%, Exposed. The fire was lit at 13:06:23. Before a subsequent console sample could be collected, the process handle became unavailable and neither local test port was listening. The available log contains no lit-state temperature sample. Client observation was requested; warmth and faster drying remain unverified by this interrupted comparison.

The user could not observe that interrupted result. The restarted server reached READY at 13:08:59, and Bedrock rejoined at 13:09:32. A repeated dry-ground comparison at `(6.5,100,8.5)` reported 16°C and wetness 40% with the campfire unlit at 13:10:02. After lighting it, the 13:10:16 inspection reported 24°C, wetness 0%, and campfire warmth. This verifies server-side warmth recognition and drying for a connected Bedrock player; client display confirmation has been requested. Exact drying-rate comparison was not isolated by these two samples.

The user confirmed all reported campfire observations matched on Bedrock. Hydration was then disabled by reload at 13:10:54, leaving HUD and environment enabled for an online HUD-independence test. Its client result is pending.

The user confirmed all Bedrock HUD-independence checks passed: environment values stayed visible without a hydration value, and `/wildlands hud off` followed by `on` hid and restored the environment HUD. Hydration was restored afterward.

For the Java immersion retest, the pool floor at `(2..3,98,2..3)` was made solid. A loaded-chunk console predicate at 13:12:37 verified stone at `(2,98,2)`, water at `(2,99,2)`, and air at headroom heights 100 and 101. The temporary chunk load was released afterward. The user selected Java for the remaining parity checks.

## Java parity session — October 7

Sandbox command setup failed, but the escalated read-only workspace probe succeeded. The stopped server was started with the existing tested JAR; built/deployed SHA-256 still matched `ED5F69C16B7EEC746B648EDD93B3960D4561CA7D22BC549989EF2F04ABF18A73`. Paper reached READY at 19:06:47. Java `Lightyear45` joined at 19:07:17 with the existing UUID. Before teleporting into the pool, the console predicate again verified solid floor, one-block water, and clear headroom.

Inspection at 19:07:34 reported 0% wetness before immersion. At 19:07:50, wetness was 100%, and the player was promptly returned to dry ground. The user confirmed rising wetness on Java. The unlit campfire sample at 19:08:15 showed 17°C and wetness 50%; after lighting the fire, inspection at 19:08:36 showed 25°C, wetness 0%, and campfire warmth. Client confirmation of that comparison is pending.

The user confirmed all Java campfire observations matched. Hydration was disabled by reload at 19:09:24. The user confirmed temperature/wetness remained visible without hydration and that `/wildlands hud off` and `on` hid/restored the HUD. After the user disconnected, hydration was restored at 19:10:24. Debug reported READY, SQLite OK, zero queued/pending records, and all six modules enabled. Java and Bedrock therefore both passed immersion, campfire warmth/drying, and online HUD independence, in addition to their earlier rain/shelter and normal display checks.

The final October 7 `clean build` passed 74 tests with zero failures/errors. The user then revised the live-testing gate: remaining client checks are deferred to one combined build after the remaining milestones. Deferred healing/depletion-rate, reconnect sampling and balance checks are not marked tested. Milestone 3 can be committed under that revised instruction.
