# Disposable Paper verification tool

Build with `gradlew.bat nativeProbeJar` (Java 25). The resulting `Wildlands-Native-Probe-0.10.0-wildlife.3.jar` is **not** the gameplay plugin and is not included as an installed JAR in the release. Source compilation is checked by the normal test task.

Use only on a backed-up disposable Paper 26.2 server with this exact Wildlands candidate, an already registered generated `rainforest_dev` world, wildlife managing that world, all seven species enabled, default shared population/cell limits and visuals enabled. Copy the probe JAR to `plugins/` and restart. It places a contained test platform/pools around x/z 2030–2070 at y239 and temporarily loads a 5×5 test chunk area. These destructive fixture operations belong only on disposable worlds. All probe commands are console only. The gameplay plugin does not force-load these chunks.

Commands:

- `rosterprobe spawn`: nine bodies spanning all seven species; injures each once and saves IDs under the probe's data directory.
- `rosterprobe inspect`: reloads the area, checks those nine UUIDs/injured health, carriers, real model pieces/hitbox dimensions and cleanup. Repeat after a server restart.
- `rosterprobe day` / `night`, `motion`, `debug`, `fire`: activity snapshots, physical movement, cached intent/physics and dry lit-campfire retreat. Wait for each asynchronous result before proceeding. Motion checks require daytime and already loaded actors.
- `rosterprobe cleanup`: restores the renderer, removes only saved fixture bodies and releases test chunk tickets. It does not restore terrain; discard the disposable world afterward.
- `rosterprobe bench`: loads the same area, warms for 200 ticks, then collects 1200 native ServerTickEndEvent durations (milliseconds). **Wait for BENCH RESULT.** Do not spawn, remove or change anything during a measurement.
- `rosterprobe full`: from a clean fixture, prepares 32 animals spanning the roster and verifies the 33rd is refused by the shared world cap. Keep them alive for the entire run.
- `rosterprobe models`: with 32 actors, adds 84 real model displays/12 proxies and exercises the production transform updater every six ticks. It temporarily replaces the viewer-gated renderer task and starts another benchmark. This headless stress test measures native entity/update work; it has no connected client or packet/FPS measurements. Restore using `cleanup`.
- `rosterprobe retain`: checks all 32 saved bodies after restart/optional-provider removal.
- `rosterprobe toggle`: checks wildlife disable/re-enable preserves bodies, restores bird gravity and clears visuals. Run outside a benchmark.

For comparable measurements: clean up, run the zero-animal benchmark to completion, create the full fixture, run its benchmark to completion, then run the model stress benchmark to completion. Keep the same chunks/modules/providers/CPU allocation. Failed or mixed-population measurements are invalid. Remove the probe before handing the server to normal players.

Reflection in this tool intentionally inspects the exact candidate's private renderer/session state; it is a verification fixture, not a supported third-party API. Actual Java/Bedrock rendering, audio, negotiation and interactions are covered by `docs/ANIMAL-LIVE-TEST.md` at the deferred collective session.
