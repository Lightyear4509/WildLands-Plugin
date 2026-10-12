# Seven-species native preflight — 2026-10-12

Candidate: `0.10.0-wildlife.3`, Paper 26.2 build 121, Temurin Java 25.0.4.1, Windows 11. Disposable registered `rainforest_dev` world. Launch allocation: `-XX:ActiveProcessorCount=2 -Xms512M -Xmx2G`. Provider-present runs used Geyser 2.11.3 build 1245 and Floodgate 2.2.5 build 140; provider-absent runs removed both plugin JARs. Native probe source is under `tools/native-probe/`, compiled by the normal tests and built separately with `nativeProbeJar`. No human client participated in these new runs.

## Verified

- Build and 186 automated tests pass: species configuration/legacy defaults, invalid/partial predator config, shared caps/cells, activity/fear/pursuit rules, all stored species, native ownership/carrier mismatch and interaction guards, Geyser definition selection, negotiation/fallback, render lifecycle/budgets, sound limits and separate pack references.
- All 226 pack source files regenerate byte-identically. All 41 distinct Ogg cues decode to finite non-silent audio. This does not prove animal realism or client playback.
- Nine native bodies cover all seven species, including two Toucans/Monkeys. Carrier identity, configured health, persistent/untamed state and paused Frog AI pass. Each creates seven real Paper ItemDisplays with its own item-model identifiers and a native-size Interaction hitbox; removal leaves neither visual leaks nor lost animals.
- Those nine UUIDs and deliberately injured health survive multiple graceful restarts. Day snapshots show Toucan FLITTING, Monkey FORAGING/GROUPING, Frog HOPPING/SWIMMING, Tapir RESTING and Capybara SWIMMING. Night snapshots show Toucan PERCHING, Monkey RESTING and Tapir FORAGING.
- Actual horizontal movement passes for Toucan, Frog, Monkey and Caiman. Native snapshots show Toucan above ground at y241.86–242.17 over a y240 perch and Caiman physically inside water near y239.51. The fixture's models are inspected headlessly; no client appearance is inferred.
- The complete population fixture holds 32 registered/loaded animals (2 Jaguar, 8 Capybara, 6 Tapir, 4 Toucan, 4 Monkey, 6 Frog, 2 Caiman). The 33rd spawn is refused by the shared world cap.
- All 32 UUIDs are reloaded/adopted without Geyser/Floodgate; bridge diagnostics report false and the plugin remains READY. Wildlife disable preserves native bodies, restores bird gravity, clears presentation and re-enable adopts the fixture.
- A dry lit campfire triggers a native Caiman RETREATING state with two seconds of state age after four seconds of fixture time. The block is restored. Pure-state tests separately check warning-before-bite, finite pursuit and group deterrence; no live-player Caiman bite claim is made.
- Local pack helper GET/HEAD returns the exact Java ZIP, matching SHA-1 `ab9d08cb8e1f4fc2e80bbf8764e0d62ffcecb52a`; an unrelated file route returns 404. It was exercised on loopback port 8766. Client pack download itself is pending.

## Bounded load measurements

Each valid run loads the same 5×5 disposable area, warms for 200 ticks and captures 1200 ServerTickEndEvent durations (60 seconds). All existing survival modules remain enabled. There are no connected players, new terrain generation, target combat or human packet/FPS measurements. Model stress runs call the production transform updater at six-tick intervals on 84 real displays/12 proxies, bypassing only viewer gating.

| Run | Mean tick ms | p95 ms | Max ms |
| --- | ---: | ---: | ---: |
| Zero animals, providers present | 0.905 | 2.433 | 9.139 |
| 32 animals, native fallback, providers present | 1.707 | 3.263 | 8.164 |
| 32 animals plus 12 models, providers present | 1.575 | 2.972 | 18.437 |
| 32 animals plus 12 models covering all seven species, providers absent | 1.666 | 3.280 | 7.146 |

The first model run uses Jaguar/Capybara/Tapir views; the complementary provider-absent run includes all seven species. Small differences between runs are ordinary short-run variation, not evidence that models reduce work. These measurements stay below the 50-ms tick budget in this fixture and support conservative defaults; they do not establish performance on an arbitrary low-end PC, full combat/server workload or a large multiplayer session.

## Failures corrected during preflight

Toucan native navigation entered its state without moving; controlled loaded-airspace flight replaced that route. Frog stopped at an air-over-water shoreline; supported water landings were added. Turtle-based Caiman navigation stopped short of water; wide-body clearance and bounded cached-goal swimming corrected it. Bird gravity is restored on shutdown/unload/pruning. Follow-up movement checks pass.

Two fire-fixture attempts were invalid: a water-edge fire could extinguish, and a 32-actor diagnostic list only shows the first 16. The final fixture places a dry fire and directly checks that Caiman's actual memory; it passes. One load baseline was invalidated by starting a partial population during measurement and is excluded. The full fixture initially overlapped an older pool; reset terrain and overlap guards corrected the probe. A Windows EINVAL on rewriting freshly encoded audio was fixed using atomic canonicalized-file replacement; subsequent regeneration/decoding passes.

Final cleanup reports registered=0, loaded=0, animals=0 and displays=0, and releases all fixture tickets. Health reports READY, SQLite OK, no queued/pending work and optional providers absent. The probe is removed and normal crossplay plugins/packs are restored before handoff.

The final clean release build passes all 186 tests with zero failures/errors. ZIP/JAR integrity, Java 25 bytecode, bundled SQLite, no mandatory Geyser classes and complete release contents are verified. A final startup with both providers restored, the matching seven-entity extension/packs and the prepared loopback URL reports READY, SQLite OK and all 12 modules enabled. The native probe is absent. The test server and pack helper are stopped after verification.

Actual Java/Bedrock visuals, facing, animation interpolation, sound playback, pack refusal/reconnect, aiming/attack forwarding, mixed clients, long-session balance, network traffic and FPS are **not tested here**. They remain the single deferred collective session in `docs/ANIMAL-LIVE-TEST.md`. Historical Milestones 1–9 client evidence remains separate. Settlement work is still pending.
