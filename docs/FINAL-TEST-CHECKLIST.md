# Combined build acceptance checklist

Use Paper 26.2 and Java 25 in a backed-up disposable world. Install only the shaded GGWPWildlands JAR. For crossplay, install compatible Geyser/Floodgate builds and configure their normal authentication. Do not disable authentication on a public server. Start normally and wait for `Foundation ready`.

These are **pending manual checks**, not claims of tests already passed. The user requested one collective client test after all milestones. Record the exact plugin/server/client/provider versions, result on each edition and reproduction steps for any failure. Run the same steps first on Java and then Bedrock; repeat multiplayer steps with both connected together. Milestones 1–3 have prior live evidence, while later client acceptance remains deferred.

| Check | Java result | Bedrock result |
| --- | --- | --- |
| Join, status, stable UUID after reconnect/restart | Pending final build | Pending final build |
| Water quality, boiling, drinking, hydration/recovery | Pending final build | Pending final build |
| Temperature, rain/immersion wetness, shelter/fire | Pending final build | Pending final build |
| Seasons, weather, crop growth and bonemeal | Pending | Pending |
| Terrain, canopy, caves, ores, chunk borders and ruins | Pending | Pending |
| Jaguar warnings, retreat, combat, fire/groups and prey | Pending | Pending |
| All crafting/stations/gear/preservation and containers | Pending | Pending |
| Journal, private camps, compass/routes and expeditions | Pending | Pending |
| HUD, permissions and module controls | Pending final build | Pending final build |
| Simultaneous sessions and restart isolation | Pending final build | Pending final build |

## Preparation

1. From console, run `wildlands admin health`, `wildlands admin modules` and `wildlands admin world create wildlands 4509`. New worlds use generator version 2; existing registered version-1 worlds retain their original terrain and receive no retroactive ruins. Move players into the custom world with your normal administrator/world-management tools.
2. Fresh defaults include `wildlands` in seasons and wildlife. For a differently named world, explicitly add its name to both configuration lists. Enable the desired modules; old configurations opt into newly added flags rather than silently changing gameplay.
3. Keep one operator for setup and one ordinary player for permission checks. Remove operator status when checking denial. Debug/health use `ggwpwildlands.admin.debug`; other administrative commands use `ggwpwildlands.admin`.
4. Leave normal production balance values in place. Temporarily shorten a setting only when needed for a focused test, record it and restore the original file afterward. Reload supported settings with `wildlands reload`; scheduling/storage changes require a normal restart.

## Water and survival

Collect empty glass bottles from river, pond, swamp, ocean and an explicitly configured spring. Inspect water labels. Rain-filled cauldrons and exposed rain collectors should give clean water. A full cauldron should supply exactly three bottles before becoming empty. Roofed collectors must not collect rain. Boil unsafe freshwater on a campfire or in a boiler; salt water must remain salt water. Consume from the main hand, including at full vanilla hunger.

Use the admin hydration setter to test reduced natural recovery below 25 and zero-hydration damage after the configured grace period. Clean water stops dehydration and causes no illness. Unsafe water carries a probability of Hunger; one safe drink does not prove the probability is broken. Verify creative/spectator exemption and persistence without offline catch-up damage.

In rain, wetness should increase outside and decrease under shelter. Swim to increase wetness; leave the pool to dry. Light/extinguish a nearby campfire and compare temperature/drying. Equip the cloak: rain exposure should be reduced, immersion should remain effective. Check altitude, shade, time of day and season temperature changes. Rain alone must not damage a player.

## Seasons, terrain and wildlife

Run `/season info`. From console, set each of the five seasons with `wildlands admin season <season> wildlands`; inspect chat/HUD, temperature and weather. Compare natural crop growth over multiple samples and ensure bonemeal remains vanilla. Disable seasons, confirm weather control stops, then re-enable and restart to check the saved clock.

Visit rivers, waterfall terraces, floodplains, wetlands, highlands, escarpments, bamboo, clearings, canopy and caves. Cross chunk borders, mine ores and inspect giant crowns/boulders/ruins. Navigate with vanilla maps and coordinates. Record terrain seams, unsafe spawn locations or waterfalls that need balance refinement. Generate new terrain through ordinary travel, monitor TPS/MSPT with admin health, and keep simultaneous explorers nearby initially before testing separate areas. Console `/forceload` over several distant new areas is a synchronous administrative workload and can stall Paper; this is not a throughput benchmark.

Find or admin-spawn a jaguar at a loaded safe habitat. Observe a warning, retreat without damage, then test close engagement with armor. Test a lit campfire, three nearby survival/adventure players, bounded pursuit and night hunting of chickens/rabbits. Test Peaceful and creative exemptions. Disable/re-enable wildlife and verify same UUID/home after restart, death cleanup and no duplicate animals. The intended visual on both editions is a named adult vanilla ocelot.

## Crafting and equipment

Run `/wildlands crafting`. Craft, place, open and break rain collectors, basic/improved filters, drying racks, cooking racks, water boilers, charcoal kilns and improved stoves. Verify recipe-book presentation and manual table crafting. Basic filtration still requires boiling; improved filtration cleans unsafe freshwater; neither handles salt. Check one charcoal consumed per operation, roof/fire preservation requirements, normal furnace/smoker cooking and improved-stove fuel advantage.

Use hoppers, stacked items and full output inventories: no duplication or input loss should occur. Check station identity and contents after breaking/replacing and restart. Drink all three waterskin charges and refill the empty skin using three standard clean water bottles; the last sip must leave an empty tagged skin. Rename-sensitive exact recipes require standard metadata. Eat preserved food and inspect hunger/saturation behavior; repetitive vanilla foods must remain viable.

## Exploration, interface and multiplayer

Discover a natural landmark and ruin. `/landmarks` should list only your discoveries; `/landmark <name>` should show the route. Hold a compass in the main hand while running `/wildlands navigate <name>`; verify the needle and HUD. Undiscovered targets must be refused. Cross worlds and inspect the compact route hint; return to resume navigation. `/wildlands navigate off` should stop the HUD route and reset the held Wildlands compass.

Build a dry roofed shelter beside a lit campfire and run `/wildlands camp set`. Check `/landmark camp`, relocate it and rerun navigation while holding the compass. Travel at least the configured departure distance (256 blocks by default), then return within the configured return distance (32). `/wildlands expedition` should record a trip and longest distance without gameplay locks or rewards that overpower vanilla equipment.

With Java and Bedrock simultaneously connected, create different camps, discover different places, drink different water and set different HUD preferences. One player's changes must never affect the other's records. Each must be unable to navigate to the other's private camp. Test group deterrence with a third survival player. Disconnect/reconnect and restart normally; verify records and active journeys remain isolated by UUID.

Run `/wildlands hud off` and `on`. With hydration disabled, temperature/wetness should remain visible; disabled fields should disappear. Check transition-season text and routes on a small Bedrock display. Test both clients' ordinary accounts receive permission denial for admin debug/health, reload, setters, world creation, equipment grants and landmark administration. Toggle gameplay modules independently and together; retained items/worlds/data must survive, tasks/recipes must stop as documented. Submit malformed configuration, confirm reload rejection with previous settings retained, then restore valid files and reload.

## Finish

Stop normally, back up the plugin data and entire shared world directory together, and retain the version/result sheet. Restore temporary settings/operator status and remove any test probes. A failed client check remains open until reproduced, fixed and retested on the affected edition.
