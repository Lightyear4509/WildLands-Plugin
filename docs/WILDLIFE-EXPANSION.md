# Seven-species animal candidate

`0.10.0-wildlife.3` combines the established survival modules with Jaguar, Capybara, Tapir, Toucan, Monkey, Frog and Caiman. These cover forest predators/herbivores, wetland groups, canopy life, small riparian wildlife and a territorial river predator. This is the chosen roster for the current animal goal; additional species are optional future work. Settlement infrastructure remains a separate pending Milestone 10 stage.

## Roster and behavior

| Species | Native authority/fallback | Server behavior | Original presentation |
| --- | --- | --- | --- |
| Jaguar | Adult Ocelot | Stalking, warnings before player bites, bounded pursuit, chicken/rabbit hunting, fire/group/home retreat | Rosettes, crouch/walking/head/tail poses |
| Capybara | Adult Pig | Wetland groups, local water entry/swimming, gathering, foraging/rest, shy retreat | Stocky brown body, short legs, chirps/grunts |
| Tapir | Adult Pig | Forest foraging at night, daytime rest, avoidance and bounded home return | Dark body, short trunk, whistles |
| Toucan | Parrot | Loaded canopy destinations, bounded local flight and landing, nighttime perching, shy avoidance | Colorful beak, wings, calls |
| Monkey | Untamed adult Cat | Forest groups/foraging, nighttime rest and avoidance; no crop/inventory theft | Brown body, long arms/tail, chatter |
| Frog | Frog | Dry-ground hops, nearby water entry/local swimming, avoidance; paused vanilla tongue AI | Small green body, hopping poses and croaks |
| Caiman | Adult Turtle | Water entry/swimming, territorial player warnings, modest bites, finite pursuit and fire/group retreat; no vanilla egg behavior | Low olive body, snout/tail and growls |

Native bodies keep health, hitboxes, UUIDs, damage and persistence on the server for both editions. Toucan, Frog and Caiman pause competing vanilla AI and use bounded native-physics movement; other carriers keep controlled native navigation. Owned feeding/taming/riding/breeding/transformation is guarded; untagged vanilla mobs remain ordinary Minecraft animals. Foraging never destroys crops or items. Native fallback drops remain unchanged; this adds no custom loot progression or client mod.

## Shared work and configurable limits

`wildlife.yml` keeps a managed-world list and one shared population/AI/spawn budget: 32 registered animals per world, 64 loaded, eight decisions per ten-tick callback, one natural-spawn callback per minute and four bounded attempts. SQLite homes count unloaded animals. Adding a species does not add another controller or database writer. Natural spawning rotates enabled species in loaded generated habitat; aquatic species need nearby water. Capybara/Monkey groups can spawn at most three members, each independently passing safety and caps. Toucan/Monkey canopy selection uses cached height-map columns.

`wildlife-expansion.yml` contains species flags, probabilities, health, world/cell caps, avoidance, home radius and fear duration. Fresh per-world caps are Jaguar 8, Capybara 12, Tapir 8, Toucan 6, Monkey 6, Frog 8 and Caiman 4; these are subordinate to the shared 32, not additive populations. Cell caps are respectively 1/4/2/2/3/4/1 per 128-block cell. Old expansion files deliberately keep missing new species disabled and preserve their old Jaguar cap of 32. Merge the complete bundled sections to enable them.

Caiman additionally requires its complete predator limits: default detection 12 blocks, warning 6, engagement 3, four-second warning, twelve-second chase/retreat, three-player deterrence, two damage per bite, two-second bite interval, 1.8-block bite range and five-block fire radius. Its generic `avoidance-range` is not a second predator trigger; `warning-range` governs territorial warnings. Its `roam-radius` and `flee-seconds` also supply predator home/retreat limits. Partial predator configuration fails validation rather than silently ignoring settings.

Water searches examine at most 81 positions and two heights, with loaded checks before access; wide Caiman bodies require water clearance. Navigation has a loaded corridor margin and throttled intent requests. Bird/swim motion uses cached goals, loaded local collision/support checks and the shared sample callback; hopping is limited to once per two seconds. No gameplay controller force-loads chunks or writes SQL per tick. Disabling wildlife preserves bodies/identity, stops paths and cached movement, restores bird gravity and removes presentation. Reload caps health without healing existing animals.

Fresh presentation defaults are opt-in, 12 models, 32-block range, six-tick updates and 16-block animation LOD: at most 84 Java displays plus 12 interaction proxies. `animate: false` keeps static models. Unchanged transforms/positions are not resent. Lower an older file's 32-model cap explicitly. Animation interval and pack URL/hash changes require restart. Sound limits are shared: one cue per animal per two seconds and four globally per second. Seven 64×64 textures and simple cuboids keep assets small. The 41 original cues are stylized syntheses, not zoological recordings. All decode to finite non-silent audio; all 226 pack source files regenerate byte-identically.

## Install and test

Install the matching Paper plugin, optional Geyser extension and separate Java/Bedrock packs from the release ZIP. Use Java 25/Paper 26.2 build 121. Geyser 2.11.3 build 1245/Floodgate 2.2.5 build 140 are the native preflight providers; the guarded negotiation observer is version-sensitive and fails closed. Java packs do not automatically convert to Bedrock. Declining/off/unavailable presentation keeps visible named native equivalents.

Back up worlds and the entire plugin data folder while stopped. Schema 8 adds species identity to existing wildlife records, defaults old rows to Jaguar and keeps UUID/world/home. Migrations and writes remain prepared, batched and transactional. Unknown species fail closed. Older JARs refuse schema 8; rollback needs the complete matching backup.

Commands: `/wildlands admin wildlife list`; `spawn <species> <world> <x> <y> <z>` under that prefix; legacy spawn without species selects Jaguar. `/wildlands visuals on|off` is session-only and pack-gated. Use [the collective client checklist](ANIMAL-LIVE-TEST.md) for exact matching artifacts, same-PC pack delivery and all seven animals' acceptance checks.

The current suite passes 186 tests. Native checks cover all carriers, models/hitboxes and cleanup, physical movement/activity, UUID/injured-health restart, shared population refusal and bounded load. See [native evidence](validation/seven-species-server.md) and the earlier three-species validation. These are preflight evidence, not passed client rendering/audio/controls/FPS tests. Actual Java/Bedrock pack negotiation, appearance, aiming and longer mixed-client balance/performance remain deferred to that one session.
