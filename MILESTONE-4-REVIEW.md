# Milestone 4 review — Seasons

Scope reviewed against the complete SPEC.md on October 7, 2026. Further live Java/Bedrock acceptance is deferred to the combined build by the user's explicit instruction. No deferred checks are reported as tested.

| Requirement | Implementation and verification |
| --- | --- |
| Season clock | Five configurable seasons, per loaded managed Overworld, UUID state, deterministic tick clock. Boundary, wraparound, large increments and changed-duration tests pass. No wall-clock catch-up or `/time` dependency. |
| Rainfall and weather | Configurable rain probability and conditional thunder probability; bounded periodic world-weather evaluation. Tests verify probabilities, vanilla weather setters and duration limits. Existing rain collection benefits from rainfall. |
| Temperature | Per-season offsets feed the existing environment service; disabling seasons returns a neutral offset. Lifecycle/profile tests pass. |
| Crops | Event-driven natural ageable crop modifiers, cancellation for slower growth and bounded extra growth for faster seasons. No scans; no bonemeal interception. Pure rule tests cover maturity and multipliers. |
| Configuration and modules | Validated seasons.yml, explicitly configured Overworld names, independently toggleable module, safe config reload. Clock interval changes require restart. |
| Persistence | Transactional schema-4 migration retains earlier player data; prepared statements and batched world-UUID records. Pending writes retain latest state; shutdown flushes. Migration, repository and lifecycle tests pass. |
| Commands and HUD | `/season [info [world]]`, permission-gated console-compatible admin setter, status and action-bar season field. Command permission tests pass. |
| Data safety and cleanup | Async load completion cannot resurrect a disabled session; no default overwrite on load failure; retries; weather snapshot captured only immediately before mutation and restored on release. Lifecycle tests pass. |

Automated build: 89 tests passed. Real Paper 26.2 build 121 with Java 25, Geyser and Floodgate loaded version 0.4.0 successfully; console season inspection, MONSOON setter, module listing and configuration reload passed. The Nether was correctly reported unmanaged. Normal shutdown completed without Wildlands errors. Restart evidence is recorded in docs/validation/milestone-4-server.md.

Seasons affect weather and existing temperature, crops and rainwater collection; they do not drain water blocks or implement flooding. Practical lowland flooding depends on later world terrain and remains an optional design item. World generation, wildlife, crafting and exploration are not part of this milestone. Live season HUD, weather appearance, crop growth and crossplay balance remain in final acceptance.
