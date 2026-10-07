# Milestone 3 review — ready for local commit

Scope: temperature, wetness, shade, campfire warmth and shelter detection only. Seasons, custom world generation, wildlife, custom survival crafting, landmarks and later milestones remain unimplemented.

| Requirement | Implementation | Validation status |
| --- | --- | --- |
| Temperature | Biome-derived baseline adjusted for elevation, time of day, sprinting, local rain, immersion, shade and campfire warmth | Automated rules pass; both editions confirmed normal display and campfire temperature rise |
| Wetness | Rain/swimming increase wetness; dry conditions, roofs and campfires accelerate drying | Automated tests pass; both editions confirmed rain/shelter drying, immersion and campfire drying |
| Shade and shelter | Bounded roof, enclosure, ground and campfire checks around the player | Automated bounded scans pass; Bedrock confirmed Sheltered status and campfire warmth; isolated shade check pending |
| Hydration interaction | Only heat raises hydration loss; cold/wet has no direct damage in this milestone | Unit test covers hot multiplier; integrated live behavior pending |
| Persistence | Schema-3 `environment_players` table keyed by UUID, batched with retry semantics | Repository tests and real Java/Bedrock restart retention pass, including Bedrock wetness 100%; exact initial reconnect value not isolated |
| Crossplay and HUD | Plain action bar/status text; no custom client asset | Both editions confirmed normal display and environment-only HUD/off-on commands |
| Configuration/modules | `environment.yml`; independent `temperature`, `wetness`, and `shelter` module flags | Empty-session live toggles passed; reload regression/full build passed; updated artifact started and reloaded successfully September 25 |

Rain exposure uses Paper's local entity rain check; immersion includes standing water and bubble columns independently of swimming pose. Cold/wet natural-healing reduction has automated module, game-mode and healing-reason coverage. Schema-2 validation runs before migration, preserving malformed databases unchanged. Reload rollback restores the previous configuration before re-enabling prior modules. HUD preferences retain existing hydration records while either consumer needs them, with gameplay guards independent of preference tracking.

Temperature and wetness share immutable UUID records and a server-thread sampling session while exposing separate modules. Shelter scans stay bounded and within loaded chunks. Physical shade/warmth calculations remain available with shelter status reporting disabled. Season influence and specialized survival equipment belong to later milestones.

The October 7 clean build passed all 74 tests. Both clients passed the core display, exposure, shelter drying, campfire and independent-HUD checks. Live hot-threshold depletion, cold/wet healing-rate measurement, exact initial reconnect sampling and broad balance/load tests were not performed and remain deferred to the final combined build. Some disposable server sessions ended without shutdown or crash logs; the cause remains unresolved. The earlier unsafe immersion procedure was corrected with a verified shallow pool and prompt return to dry ground.

On October 7 the user explicitly deferred remaining live checks and authorized progression with automated verification. This overrides the prior per-milestone live gate without changing tested-status claims. See `docs/validation/milestone-3-server.md` and `CROSSPLAY-COMPATIBILITY.md` for evidence. Milestone 3 is ready for its local commit under the revised gate; Milestone 4 may follow afterward.
