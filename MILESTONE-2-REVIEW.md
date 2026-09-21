# Milestone 2 review — accepted

Scope: hydration, source identification, drinking, boiling, basic HUD and persistence. Milestones 3–9 remain unimplemented. Java and Bedrock acceptance was completed with real clients through Geyser/Floodgate.

| Requirement | Implementation/evidence | Remaining validation |
| --- | --- | --- |
| Hydration 0–100 with slow loss | Validated immutable records; five-second sampling; default 0.025/second; activity multipliers; pure-rule tests | Java and Bedrock delayed dehydration/recovery reported passing under accelerated test timing; normal long-duration pacing remains covered by configuration and rule tests |
| Recovery before direct danger | Food-based natural healing multiplier below 25; delayed damage at severe threshold; boundary and event-handler tests | Java and Bedrock delayed damage/recovery confirmed; live natural-healing comparison remains covered by event-handler regression testing |
| Water sources and quality | Biome classification, explicit spring blocks, event-driven rain cauldron provenance; conservative mixing; tests | Spring, salt rejection, cauldron exhaustion, and rainwater passed on both clients; river/pond classification is covered by automated source tests |
| Drinking and unsafe risk | Vanilla water consumption; configurable probabilistic hunger effect; effect potions excluded | Java and Bedrock drinking and unboiled swamp-water Hunger behavior confirmed; probabilistic default boundaries covered by tests |
| Boiling | Exact-choice vanilla campfire recipe, 20-second default; salt excluded | Boiling and salt rejection confirmed on both clients; modified-item limitation documented below |
| Minimal crossplay HUD | Plain action bar every two seconds; persistent on/off preference; independent module | User reported HUD toggle behavior working on both editions; persistence and command behavior are automated |
| UUID persistence | Transactional schema-1 to schema-2 migration, serial SQLite worker, retained failed writes and stale-session guard | Both real UUID snapshots survived shutdown/restart unchanged; reconnect loading is covered by live command checks and automated stale-session tests |
| Commands and administration | /hydration, /wildlands status/hud, permission-gated admin hydration; finite numeric validation | Automated permission tests pass; user reported client permission denial working on both editions |
| Configuration/modules | hydration.yml validation; independent hydration/hud flags; existing installs opt in; module rollback | Real console reload/toggles and startup without providers passed |
| Performance | No per-tick scans or database writes; server-thread Bukkit access; bounded player sampling and batched snapshots | Multiplayer load/balance belongs to later polish; ordinary live use monitored |
| No client mods | Vanilla bottles/campfires and action-bar text; no resource pack required | Both real clients connected and used boiling/drinking |

The final build passed 54 automated tests. Actual server/client evidence is in `docs/validation/milestone-2-server.md`; `CROSSPLAY-COMPATIBILITY.md` distinguishes client-confirmed interactions from automated coverage.

Known operational limits: boiling recipe exact matching excludes anvil-renamed/customized bottles; source quality for vanilla freshwater is biome-based until custom world generation exists; springs are explicit coordinates; external block edits that bypass Bukkit events can leave cauldron metadata stale. Boiling-duration changes require restart. No later environment, season, world-generation, wildlife, or progression systems have been added.

Milestone 2 is ready to commit. Milestone 3 has not started and requires a fresh complete reading of `SPEC.md` before implementation.
