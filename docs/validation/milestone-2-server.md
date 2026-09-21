# Milestone 2 validation — in progress

Milestone 2 is not accepted or committed. Milestones 3–9 have not started.

## Observed runtime results

On 2026-09-17 the disposable Paper 26.2 build 121 server was stopped normally. Its foundation database was copied to `.runtime/milestone-2-backup/foundation.db` before installing the 0.2.0 candidate. The server runs Java 25 with Geyser 2.11.3 build 1245 and Floodgate 2.2.5 build 140.

At 10:09:18 local time the candidate reported READY, SQLite OK with no pending writes, and all three modules (player-records, hydration, hud) ENABLED. Both crossplay APIs were AVAILABLE. This demonstrates real startup and recipe registration, not successful client gameplay. Console configuration reload passed at 10:09:53.

A disposable platform at x=0..31, y=99, z=0..15 contains river, swamp, ocean and jungle water pools, a campfire, an empty cauldron and an unknown-provenance filled cauldron. Temporary forced chunk loading was removed after setup. World spawn is 15,100,12. Existing players require teleporting to the platform and bottle supplies before interaction tests.

Client HUD test instructions have been sent; no Milestone 2 client observations have been received. Java uses 127.0.0.1:25565; Bedrock uses 127.0.0.1:19132.

The runtime log also contains pre-existing Windows OSHI counter warnings and a later Spark memory-monitor `NumberFormatException: Infinite or NaN`. These originate outside Wildlands; no claim of a completely warning-free server is made.

## Outstanding acceptance

- Deploy the latest post-startup inventory-guard/status fixes before final testing.
- Test all water source classes, including configured springs and naturally filled rain cauldrons.
- Test collection in each hand, stacked bottles, full inventory, protection cancellation, and cauldron depletion.
- Test drinking, illness probabilities under controlled settings, moderate recovery reduction, prolonged severe dehydration, respawn and activity pacing.
- Test campfire boiling on both clients, salt exclusion, and unchanged effect potions.
- Test action-bar appearance and HUD preferences, permissions, online toggles and reload rejection.
- Verify both real UUID hydration records across reconnect and restart.
- Re-test startup without optional providers for this candidate.
- Review the final diff against SPEC.md and update the compatibility matrix only with actual results.

## Latest-candidate absence and toggle checks

The latest built 0.2.0 artifact was deployed after normal shutdown. With both optional provider JARs temporarily removed, Paper reached READY at 22:49:43, SQLite reported OK, and hydration/HUD were ENABLED. Console `/hydration` safely requested an in-game sender; an admin value of NaN was rejected. Reload disabled hydration and HUD (confirmed 22:50:12), then re-enabled both (confirmed 22:50:26), without lifecycle or recipe-registration failures. The server stopped normally. Raw evidence is retained in `.runtime/milestone-2-backup/no-providers.log`. Provider JARs were restored afterward. These tests had no players online and do not replace client acceptance.

The configured test spring is at world:26,99,6; bottle supplies are in the chest at 15,100,10. Glass barriers surround the platform. Configuration reload accepted the spring location. The final-cauldron-bottle fix passed its regression test (54 tests total). Its built artifact was copied to the stopped server and both SHA-256 hashes matched: `4695C220A436E41CED9B15CAA175A45F3A5694C876485E5CD72E32D8C17AE989`. This supersedes the earlier deployment-pending note for inventory/status fixes. Live gameplay remains untested.

## First client sessions (observations pending)

Java Lightyear45 connected at 23:00:04 and ran `/hydration`, `/wildlands hud`, and `/wildlands hud on` before disconnecting at 23:01:09. Bedrock .Lightyear4509 connected at 23:02:10 and ran `/hydration` at 23:02:30, then was killed by a drowned at 23:02:46 near the old login location and disconnected at 23:04:09. No Wildlands exception appears in this session log. Client-visible output has been requested; command-log evidence alone does not prove HUD rendering.

A read-only JDBC query afterward found both UUID hydration records: Java 98.438 with HUD enabled and dry_seconds=0; Bedrock 100.000 with HUD enabled and dry_seconds=0. This proves snapshots exist for both real UUIDs after disconnect, but does not yet prove reconnect/restart preservation or explain Bedrock's lack of observed depletion. Move returning test players to the prepared platform before further interactions.

## Java water test evidence

Lightyear45 reconnected at 23:06:29, was teleported to the platform, received 16 bottles, and was set to 40 hydration through the actual admin command. At 23:07:34 server entity data showed a CONTAMINATED water bottle with the correct vanilla water potion and persistent quality tag. At 23:07:51 the campfire contained two contaminated bottles with cooking totals of 400 ticks (20 seconds). At 23:08:30 the campfire was empty and the selected item was an empty glass bottle. Java disconnected at 23:08:42. A subsequent read-only database query showed Java hydration 99.250, HUD enabled, dry_seconds=0; Bedrock remained 100.000/HUD enabled/dry_seconds=0.

These observations support live collection, recipe acceptance and hydration restoration. They do not independently establish the exact output bottle label, the number or type of drinks, HUD rendering, or Bedrock water behavior. User confirmation and remaining acceptance interactions are still pending. Global world spawn was moved to 15,100,100 to keep spawn protection outside the platform; Java's personal respawn point is 15,100,12.

On 2026-09-18 the user explicitly confirmed Java connection, boiling water, drinking it, and verifying that it worked. Java boiling/drinking acceptance is therefore confirmed. This confirmation does not explicitly cover HUD toggles, all source classes, unsafe-water effects, or Bedrock water interactions.

Bedrock reconnected on 2026-09-18 at 12:31:41 at the test platform (approximately 14.79,100,8.88) and disconnected at 12:32:36. This is evidence of a platform visit, not proof of any specific water interaction; its result has been requested.

## Real hydration snapshot restart check (2026-09-18)

With no players online, pre-restart JDBC reads returned Java 99.250 and Bedrock 99.875, both HUD=true and dry_seconds=0. The server stopped normally at 12:34:42 and reached READY again at 12:36:17 with both providers available. Reads after shutdown and after READY returned the same values. Debug at 12:36:36 confirmed SQLite OK and all modules enabled. Both actual UUID hydration snapshots therefore survived the restart; client-visible reload values still need checking on reconnect.

A debug command submitted before startup finished failed within Paper's command dispatcher because its command source had a null world. Reissuing after READY succeeded. No Wildlands stack frame was involved; future test commands should wait for READY.

On 2026-09-18 the user confirmed that Bedrock could perform the same boiling and drinking workflow as Java. Both editions therefore pass the basic live boiling/drinking check. This does not imply confirmation of separate HUD toggles, salt rejection, rainwater provenance, every source classification, or permission denial; those observations remain distinct.

The user's subsequent response, "i think both of those work as well", followed the question about HUD off/on and admin hydration permission denial on both editions. Record these as tentative user-reported successes, not independently observed definitive passes. No water edge-case result was supplied by that response.

## Source-interaction follow-up (2026-09-19)

Java Lightyear45 connected at 08:36:33 and received bottles at 08:36:57. At 08:37:17, server entity data showed a vanilla water potion named Salt Water with water_quality=SALT and the salt/boiling warning lore. The user subsequently confirmed that Java interactions were working in response to the spring, salt and cauldron test instructions. This is a user-reported interaction pass; the log independently verifies the salt bottle representation, but not exact cauldron counts or spring output.

Bedrock .Lightyear4509 connected at 08:39:14 and disconnected at 08:41:13. The live console confirmed zero online players afterward. The result of this specific Bedrock source/cauldron test has been requested; its connection alone is not recorded as a pass. Earlier boiling/drinking confirmations remain valid.

The user explicitly confirmed all three Bedrock follow-up checks passed: configured spring produced Clean Water, ocean water remained Salt Water and refused boiling, and a full cauldron yielded exactly three bottles before becoming empty. Together with the Java confirmation, these source-interaction checks pass on both editions. Rainwater and live dehydration/unsafe-water effects remain separate outstanding checks.

## Effects-test setup (2026-09-19 08:43)

With zero players online, backed up the disposable server hydration.yml to .runtime/milestone-2-backup/hydration-before-effects.yml. Temporarily set severe damage delay to 30 seconds, interval to 10 seconds, and contaminated-water illness probability to 1. Reload succeeded at 08:43:00; rain was started for 600 seconds. Source defaults are unchanged. Restore this backup and reload after live effects testing. These temporary values make effect delivery deterministic; default probability behavior is covered separately by rule tests. No effect test result is claimed yet.

The recovery regression now exercises the actual HydrationService event handler: hydration 17 halves SATIATED healing from 2 to 1; MAGIC healing stays 2; creative SATIATED healing stays 2. The full build passed all 54 tests after rerunning with filesystem access (the restricted run failed JUnit temporary-directory creation with AccessDeniedException). No production code or deployed artifact changed during this follow-up.

## Disposable-server watchdog incident (2026-09-21)

The disposable Paper process stopped after a watchdog timeout while it had been left idle. The server-thread trace was in Paper's bundled Spark `PaperWorldInfoProvider.pollCounts`, and the concurrent log thread was blocked in Windows file rotation; no `gg.ggwp.wildlands` stack frame appeared. This is not attributed to Wildlands. The temporary accelerated effects settings were restored to normal values in the stopped server configuration. The Java effects session that began before the interruption did not produce a result and remains pending.

At 08:47:44 the live console still reported zero connected players. Restored hydration.yml from the pre-effects backup before waiting for client availability; deterministic effects settings must be reapplied deliberately when a tester is ready. No live effects result was obtained.

## Live Bedrock effects session (2026-09-19)

Bedrock connected at 09:02:55. Temporary deterministic effects settings were re-enabled and reload succeeded at 09:03:23. Supplied two vanilla water potions with CLEAN quality tags as recovery controls (console-created controls do not test source collection). Health was 20 at 09:03:23. Set hydration to zero at 09:03:30; a health query at 09:04:00 returned 19, consistent with the configured 30-second grace period. Client observation and drink recovery are pending. Placed a fresh empty rain-test cauldron at 7,100,8 and started rain at 09:03:45; no rain collection result yet.

The user confirmed Bedrock dehydration damage began and drinking stopped it. This passes the live severe-dehydration/recovery interaction under the temporary accelerated timing. Default long-delay boundaries remain covered by automated rules. The next requested interaction is drinking unboiled swamp water to verify hydration restoration and Hunger with the temporary illness chance of 1.

The user subsequently confirmed that both Java and Bedrock were tested and worked for dehydration and the Hunger effect after drinking unboiled swamp water. This completes the live effects checks on both editions. The temporary deterministic configuration is not the shipped default: normal values are preserved in the backup and were restored before the restarted server began. Rainwater collection remains the only distinct source interaction still awaiting live confirmation.

The user then confirmed rainwater works on both Java and Bedrock. This completes Milestone 2's live crossplay acceptance: source identification, collection, drinking, boiling, salt rejection, unsafe-water risk, dehydration/recovery, HUD controls, permissions, persistence, configuration toggles, and rainwater all have automated and/or real-client evidence appropriate to their behavior. The temporary platform force-load used for the rain test was removed.

At 09:05:48 no clients were connected for further effects checks. Restored normal hydration configuration from the preserved backup; reapply deterministic illness settings only when live testing resumes. Bedrock dehydration/recovery remains a confirmed pass, while unsafe-water and rainwater observations remain pending.
