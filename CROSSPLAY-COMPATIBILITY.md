# Crossplay compatibility — Milestone 1

No client-side mods or resource packs are required. This milestone uses normal server commands/chat and server UUIDs; it introduces no survival mechanics, custom visuals, or HUD.

**The local Java + Bedrock foundation acceptance checks passed on 2026-09-17.** Scope: Paper 26.2 build 121, Java client 26.2, Bedrock installed package 1.26.5101.0, Geyser 2.11.3 build 1245, Floodgate 2.2.5 build 140, Windows 11, both clients on the same PC. Server logs independently confirm both logins/reconnects and the Bedrock protocol reported by Geyser. The user confirmed commands worked and admin debug denied permission on both clients.

| Feature | Java behavior | Bedrock behavior via Geyser/Floodgate | Tested |
| --- | --- | --- | --- |
| Plugin startup and shutdown | Standard Paper plugin | Same server plugin | Real Paper startup/shutdown passed, with and without optional providers |
| Status command and chat output | Plain chat commands | Same command and message format | Both clients: user-confirmed working |
| Debug permission denial | Server permission nodes | Same server permission nodes | Both non-op clients: user-confirmed denied |
| Tab completion visibility | Server permission nodes and visibility | Edition-specific completion UI | Automated permission/visibility tests; exhaustive live UI completion not tested |
| UUID record persistence | Paper UUID primary key | Floodgate-provided UUID primary key; no prefix heuristics | Both actual client records: stable reconnect UUIDs; stored timestamps unchanged after server restart |
| Configuration reload and module toggles | Server/console administration | Same server behavior | Automated tests and actual console reload/toggles passed; toggling while both clients online not separately tested |
| Admin diagnostics | Permission-gated plain chat or console | Equivalent messages | Actual console aggregate and per-UUID lookups passed; client denial passed |
| Geyser/Floodgate absent | No dependency required | Bedrock transport must be provided separately | Real Paper startup without either provider passed |
| Local Geyser API initialization | Available when installed | Geyser transport initializes | Actual provider initialization and Bedrock connection passed |
| Floodgate API player detection | Java or undetected unless API identifies player | Positively detects Bedrock UUID | Actual Bedrock session reported BEDROCK and LOADED |
| Incompatible optional API | Continues with degraded diagnostics | Platform may be undetected | Automated fake-provider failure tests only |
| Geyser-only online authentication | Standard Java authentication | Requires suitable Java account authentication | Not tested as a separate topology |
| Proxy forwarding | Standard backend records | Requires correct Floodgate forwarding | Not tested |
| Resource packs | None | None | Not applicable |

See [server evidence](docs/validation/milestone-1-server.md) and [milestone review](MILESTONE-1-REVIEW.md). These results do not establish compatibility for future gameplay features; each milestone requires its own tests.

## Detection limits

BEDROCK means an installed API positively identified the UUID. JAVA_OR_UNDETECTED does not guarantee Java Edition. Geyser running only on a proxy is invisible to this backend unless Floodgate forwarding and the backend Floodgate plugin are configured correctly.

Follow the [official Floodgate backend instructions](https://geysermc.org/wiki/floodgate/api/): configure forwarding on the proxy and matching Floodgate key material on trusted backend installations. Wildlands does not read keys, parse name prefixes, derive UUIDs from XUIDs, or contact account services.

Changing authentication/account-linking topology can change the UUID presented by the server. This milestone does not merge accounts or migrate UUIDs automatically.

## Repeating client acceptance

Use a disposable Paper 26.2 installation running Java 25 and the shaded Wildlands JAR.

1. Start without Geyser/Floodgate. Confirm Foundation ready, console help/debug, and normal shutdown.
2. Start with local Geyser/Floodgate. Connect Java 26.2 to the Java port and a supported Bedrock client to Geyser's UDP port.
3. On both clients, run /wildlands status and /wildlands admin debug. Non-operators must see status but receive permission denial for debug.
4. Disconnect/reconnect each client. Verify stable UUIDs, loaded records, and positive Bedrock classification.
5. Stop/restart the server normally. Query both UUIDs from console and confirm first-seen/last-seen data persists.
6. Exercise invalid reload rejection and player-record module toggles on the disposable server.
7. For any additional deployment topology (proxy, Geyser-only auth), test it explicitly before rollout; no live result is claimed here.

Record client/server/provider versions and actual observations. Assets directories assets/java/ and assets/bedrock/ remain reserved for future milestones.

## Milestone 2 — accepted

Java and Bedrock hydration acceptance used Paper 26.2 build 121, Java client 26.2, Bedrock installed package 1.26.5101.0, Geyser 2.11.3 build 1245, and Floodgate 2.2.5 build 140. Real-client results were recorded from 2026-09-18 through 2026-09-21.

| Feature | Java | Bedrock through Geyser/Floodgate | Tested |
| --- | --- | --- | --- |
| Water collection and quality labels | Vanilla bottles with server metadata | Same vanilla item and server metadata | Java and Bedrock user-confirmed spring water, salt-water identification, three-bottle cauldron exhaustion, and rainwater; river/pond mapping is automated |
| Severe dehydration and recovery | Server damage and drink recovery | Same server effects | Java and Bedrock user-confirmed damage after temporary 30-second grace period and cessation after drinking |
| Drinking and unsafe-water risk | Vanilla consume interaction | Translated vanilla consume interaction | Java and Bedrock user-confirmed drinking and unboiled swamp-water Hunger behavior under deterministic test settings |
| Campfire boiling | Exact water-bottle recipe | Same server recipe | Java and Bedrock user-confirmed working; salt rejection also confirmed |
| Rain cauldrons and configured springs | Server-authoritative quality | Same sources | Automated provenance/config tests; user-confirmed spring and rainwater behavior passed on both editions |
| Hydration HUD and preferences | Action-bar text and commands | Action-bar text and commands | Automated persistence/commands pass; user-confirmed HUD toggles work on both editions |
| Reconnect/restart hydration | SQLite UUID snapshot | Floodgate UUID snapshot | Automated SQLite/reconnect tests; both real UUID snapshots survived server restart unchanged |
| Module toggles and admin setter | Server permissions | Same permission model | Console module toggles and Java admin setter passed; user-confirmed client permission denial on both editions |

No client-side mods or resource packs are required. Untested compatibility remains limited to nonstandard inventory edge cases and third-party world editors bypassing normal Bukkit cauldron events; these are documented operational limits, not missing Milestone 2 gameplay.

## Milestone 3 — implemented; remaining live checks deferred

| Feature | Java | Bedrock through Geyser/Floodgate | Tested |
| --- | --- | --- | --- |
| Temperature and wetness HUD | Plain action-bar text | Same translated action-bar text | Both editions confirmed normal display, environment-only HUD and off/on commands with hydration disabled |
| Shelter status and shade | Server-local block checks | Same server calculation | Automated shelter detection passes; Bedrock user-confirmed Sheltered status; isolated shade and Java shelter checks pending |
| Rain wetness and shelter drying | Server-authoritative exposure and drying | Same server calculation | Java and Bedrock user-confirmed wetness rising outside and falling inside shelter |
| Water immersion | Server-authoritative water exposure | Same server calculation | Java and Bedrock user-confirmed rising wetness; both console samples reached 100% |
| Campfire warmth | Vanilla lit campfire detection | Same vanilla block state | Automated rules pass; both editions confirmed warmth label and drying; controlled fire changes raised displayed temperature by 8°C on each |
| Environment persistence | UUID-keyed SQLite snapshot | Floodgate UUID-keyed SQLite snapshot | Automated migration/repository tests pass; real Java and Bedrock records retained across restart, including Bedrock wetness 100%; exact initial reconnect value not isolated from sampling |

No custom visuals, resource packs, or client-side mods are introduced by this candidate.

## Milestone 4 — implemented; live checks deferred

Season profiles and world-UUID persistence have no client assets. Java/Bedrock parity is assumed by design at the user's request; final combined-build acceptance remains outstanding. No seasonal gameplay is marked live-tested.

| Feature | Java | Bedrock through Geyser/Floodgate | Tested |
| --- | --- | --- | --- |
| Season clock and persistence | Per-world server clock | Same server clock | Automated boundary, wraparound, configuration, migration and restart repository tests pass |
| Rain and thunderstorms | Vanilla server weather | Geyser translates vanilla weather | Automated probability and world-weather tests pass; live display deferred |
| Temperature modifier | Server environment calculation | Same calculation | Automated profile/lifecycle tests pass; live HUD observation deferred |
| Natural crop growth | Server growth events | Same crops and block ages | Automated growth/cancellation/maturity rules pass; live crop checks deferred |
| Season commands and HUD | Chat and action bar | Same server messages | Automated command permissions and lifecycle tests pass; live UI checks deferred |

At final acceptance, run `/season info` on both clients, inspect the season HUD, set each season from console, observe weather/temperature changes, compare natural crop growth and bonemeal, then verify restart persistence and disabling seasons. Neither client needs a mod or resource pack.

## Milestone 5 — implemented; live checks deferred

| Feature | Java | Bedrock through Geyser/Floodgate | Tested |
| --- | --- | --- | --- |
| Rainforest regions, rivers, wetlands and highlands | Custom server terrain with vanilla blocks/biomes | Same authoritative terrain, translated vanilla visuals | Automated deterministic model and chunk-buffer tests; real Paper creation, river biome/water, waterfall water, highland/cliff ground and bedrock/grass checks pass; live navigation deferred |
| Canopy, bamboo, enormous trees and rocky landmarks | Vanilla jungle logs/leaves, bamboo, ferns and mossy blocks | Vanilla block equivalents | Automated chunk generation and clipping tests; live visual/playability review deferred |
| Caves and ores | Server cave tunnels and normal mineable ore blocks | Same tunnels and resources | Automated terrain/cave model and generation tests; live mining deferred |
| World creation, region status and restart profiles | Permission-gated server command and frozen SQLite profiles | Same commands, UUID and shared world | Automated permissions, profile persistence and failed-write recovery tests; real creation, safe-name/existing-world refusal, restart UUID preservation and restoration with creation disabled pass; live region chat deferred |

Parity is assumed by design per the user's revised workflow. Final acceptance must visit representative regions, cross chunk boundaries, mine caves/ores, inspect crowns/bamboo and waterfalls, and compare region status on both clients. Resource packs and client mods are not required. Vanilla structures are not generated in custom worlds; ruins and discovery mechanics arrive in Milestone 8.

## Milestone 6 — implemented; live checks deferred

| Feature | Java | Bedrock through Geyser/Floodgate | Tested |
| --- | --- | --- | --- |
| Jaguar representation | Named adult vanilla ocelot | Same translated vanilla entity and server hitbox | Real Paper spawn/name/health/ownership tags pass; client visual and hitbox acceptance deferred |
| Habitat and populations | Loaded rainforest habitat, territory/population caps | Same server decisions | Automated habitat/cell/config tests; console occupied-territory and unloaded-location refusal pass |
| Warnings, stalking and bounded attacks | Server chat/hiss, pathfinding and damage | Equivalent chat/sound/entity behavior | Automated behavior rules, permissions and disabled attack guard pass; player combat/navigation live checks deferred |
| Fire/group deterrence and prey hunting | Server-local rules, chickens/rabbits | Same animals and rules | Automated deterrence/prey rules pass; real campfire RETREATING state and controlled chicken damage (3 per two seconds) pass; group/player/client checks deferred |
| UUID homes and entity persistence | SQLite home plus chunk-persistent animal | Same server UUID/entity state | Automated registry restart, unload/removal and failed-write death tests pass; actual same-UUID/home/health restart, disable/re-enable adoption and death cleanup pass |

Final acceptance must encounter a warning on both editions, back away without damage, test close engagement, armor, fire and three-player deterrence, bounded pursuit, night hunting and module toggles. No wildlife behavior is marked live-client tested. Jaguar appearance intentionally uses a vanilla ocelot equivalent; no Java-only model is introduced.

## Milestone 7 — implemented; live checks deferred

| Feature | Java | Bedrock through Geyser/Floodgate | Tested |
| --- | --- | --- | --- |
| Equipment recipes and upgrades | Native crafting table/recipe book | Same server recipes and vanilla items | Automated configuration/permission checks; all eleven native crafting/refill recipe matches pass; client recipe-book controls deferred |
| Rain collector and basic/improved filters | Barrel inventory with tagged water potions | Same translated barrel and potion interface | Automated conservation/full-inventory/salt tests and native server station operations pass; client interactions deferred |
| Cooking rack, kiln, boiler and improved stove | Native furnace/smoker recipes and fuel | Same native cooking and fuel | Native Paper water/food cooking checks pass; client interaction deferred |
| Rain cloak | Dyed leather chestplate; server rain modifier | Same armor and wetness calculation | Automated rain-only protection and immersion/drying rules pass; client equipping/display deferred |
| Three-drink waterskin and refill | Water potion consumption with native replacement | Equivalent translated potion consumption | Automated native replacement, disabled/loading safeguards and actual tags/refill recipe pass; client consumption/animation deferred |
| Food preservation and nutrition | Native cooked food with modest saturation bonus | Same food consumption and server hunger | Automated after-consumption timing/capping and native preservation operations pass; client hunger display deferred |
| Station/item persistence and toggles | Native item/chunk data | Same server data | Automated lifecycle cleanup; real restart/module-toggle checks recorded in docs/validation/milestone-7-server.md |

Final combined-build acceptance must craft/place/open/break each station on both editions, exercise rain/roof collection, filter fresh versus salt water, boil/cook, preserve/eat food, equip a cloak in rain and immersion, drink all three waterskin charges and refill it. Check a full inventory, hoppers, recipe-book presentation, main-hand consumption at full hunger, module toggles and reconnect/restart. Parity is assumed by design under the user's revised workflow; none of these client checks is marked tested. Items and stations use vanilla visuals and interfaces, so separate custom assets or client mods are not required.
