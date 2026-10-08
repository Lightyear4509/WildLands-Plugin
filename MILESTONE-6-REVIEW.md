# Milestone 6 review — Jaguar wildlife

The complete specification was read before implementation. Only the first wildlife species is introduced. The user's revised workflow defers live Java/Bedrock acceptance to the final combined build; it remains explicitly untested.

| Requirement | Implementation and evidence |
| --- | --- |
| First species and representation | Named adult vanilla ocelot represents Jaguar on both editions, with the same server hitbox. No disguise API, custom model, client mod or mandatory pack. Actual Paper name, ownership tags, health and persistence verified. |
| Ecological habitat and population | Generated rainforest/bamboo/wetland/clearing habitats; loaded-only spawn attempts near eligible players; explicit world allowlist, global loaded/per-world caps and one home per 128-block cell. Automated habitat/config tests and real occupied/unloaded spawn refusal pass. |
| Warnings and activity cycles | Neutral at daytime distance, night stalking, hiss/chat warning before human attack, retreat when players back away. Deterministic tests prove grace period and state boundaries. Live player warning/appearance deferred. |
| Bounded pursuit and safety | Detection/line-of-sight, home radius, chase timeout, retreat cooldown, group/fire deterrence; creative/spectator exclusion and Peaceful bite suppression. Tests prove behavior limits; actual fire changed runtime state to RETREATING. |
| Prey and combat | Optional chicken/rabbit hunting, normal armor-sensitive damage with bite cooldown; modest 20 health/3 damage defaults. A controlled Paper chicken probe lost 6 health over four seconds while the jaguar was ATTACKING. Human armor/movement remains final acceptance. |
| Performance and threading | Server-thread entity/world access, rotating capped decisions, throttled pathfinding with loaded corridor checks, bounded local campfire search, rare spawn attempts. No world scans every tick, force-loaded spawn chunks, or database writes per AI decision. |
| Persistence | Entity UUID/world/home in schema-6 SQLite; owner/home PDC in chunk-persistent entities. Unload retains population; permanent removal/death releases it. Pending death overwrites failed birth writes. Tests plus real same-UUID/home/health restart and death cleanup pass. |
| Modules and administration | Independent wildlife flag; disabled attacks cancelled, paths/tasks stopped, animals preserved; lifecycle bookkeeping stays active. Explicit console spawn and capped diagnostics. Permission/lifecycle tests plus actual toggle/reload/adoption checks pass. |

Clean build: 110 tests, zero failures/errors. Real server evidence is in docs/validation/milestone-6-server.md. Existing player, environment, seasons and world data were retained through the migration. No second species, crafting or exploration mechanic is introduced here.

Final acceptance must still assess warning clarity, player combat/armor, actual fleeing/navigation, group deterrence and ocelot representation on Java and Bedrock. Wildlife performance and balance belong to Milestone 9. External entity-file deletion bypassing Bukkit events can leave stale population records; back up the database and shared level together. Generated animals use vanilla entity persistence, so abrupt process crashes share Minecraft's normal unsaved-chunk limits.
