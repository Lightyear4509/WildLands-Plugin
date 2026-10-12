# Animal implementation and preflight review

This review covers the user's expanded animal/live-readiness goal within Milestone 10. The chosen complete candidate roster is Jaguar, Capybara, Tapir, Toucan, Monkey, Frog and Caiman. It retains Milestones 1–9. Settlement membership, shared navigation and water infrastructure remain a separate pending stage.

| Requirement | Implemented / evidence |
| --- | --- |
| Species models, textures, animation and sounds | Seven independent Java model sets and Bedrock geometry/animation sets; seven 64×64 textures; 41 original synthesized cues. Pack references, decoder checks and deterministic regeneration pass. |
| Ecological AI and behavior | Forest/wetland/canopy habitats, activity cycles, grouping/foraging/rest, hopping/flight/swimming, player warnings/avoidance, modest territorial bites, finite chase/home/fear bounds and fire/group deterrence. Pure-state tests and native physical movement/activity/fire probes pass. |
| Server authority / no client mods | Real native bodies retain UUID, health, damage, hitboxes and persistence. Optional Java displays/interaction proxies and the separate optional Geyser extension only change presentation. |
| Crossplay and provider absence | Seven native-base definition mappings, pack/viewer gates and failure fallback tests pass. Native Geyser startup and startup/adoption of all 32 animals without either provider pass. Live client appearance/audio/controls remain explicitly deferred. |
| Modular configuration and safe storage | Validated independent flags/caps/settings, old missing-species sections stay disabled, partial predator configuration rejected, shared controller and schema-8 prepared/batched persistence. Nine injured UUIDs survive restart; all 32 bodies adopt without providers. |
| Practical overhead | Shared 32/world cap, eight distributed decisions/10 ticks, throttled cached movement, no gameplay chunk force-loading/per-tick SQL, 12-model cap, six-tick updates, distance LOD/static mode, shared sound limits. Native 32-animal and 84-display/12-proxy runs stay below the fixture's 50-ms budget. No arbitrary-PC FPS guarantee. |
| Lifecycle and guards | Native species/carrier ownership, feeding/taming/riding/breeding/transformation/portal guards, health reload without healing, fallback on missing/declined/off packs, gravity restoration and model cleanup. Final fixture cleanup returns native registry/models/SQLite live rows to zero. |
| Build and collective delivery | Java 25/Paper 26.2 target; 186 tests pass; bundled shaded plugin, separate extension and both packs, configuration examples, asset provenance/exporter, loopback pack helper, verification source and one complete client checklist. |

Detailed native measurements, corrected failures and limits are in `docs/validation/seven-species-server.md`. Install and collective checks are in `docs/ANIMAL-LIVE-TEST.md`; implementation/settings are in `docs/WILDLIFE-EXPANSION.md`. The crossplay matrix separates each actual preflight result from pending client acceptance.

This is ready for the requested collective live session after final clean-build/bundle checks. It is not a claim that unobserved Java/Bedrock rendering, audible playback, aiming, mixed-pack tracking, long-session balance/network traffic or client FPS have passed. Those are the remaining acceptance work in that session; no individual live check is required before delivering this candidate.
