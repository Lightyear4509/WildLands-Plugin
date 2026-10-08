# Milestone 9 combined server validation

October 8, 2026: disposable Paper 26.2 build 121 on Java 25.0.4.1; GGWPWildlands 0.9.0. Geyser 2.11.3 build 1245 and Floodgate 2.2.5 build 140 were first removed while stopped, then restored for a second normal startup. Data/configuration and the previous plugin JAR were backed up. No live client participation is claimed.

## Automated verification

147 tests passed with zero failures/errors, including 32 simultaneous Java-shaped/Floodgate-shaped hydration sessions with independent drinking/HUD preferences and real SQLite restart reads; two-player exploration/private-camp isolation; lower station-cap reload without inventory changes; stale corpse removal without recreated wildlife registry/AI; health permission/completion; compact HUD; deterministic spawn and exact noise equivalence. Full chunk fingerprints captured before optimization match both frozen profiles across five representative chunks each.

An isolated 24-chunk cave-column microbenchmark retained exactly 47,743 cave samples before and after. Warm iterations were approximately 23–26 ms before and 23–24 ms after on this host. This small synthetic sample is not a server capacity claim. Removing redundant calculations and preserving output were the principal requirements.

## Native Paper checks

- Without either optional provider, the combined plugin reached READY at 07:59:01 and admin health reported ABSENT/ABSENT, SQLite OK, no pending records and all twelve modules enabled.
- Existing `rainforest_dev` retained UUID `457de4f2-da59-4334-9455-69983be580ae` and profile 1; `expedition_dev` retained UUID `18539dba-71ff-4240-852f-08440fd7d877` and profile 2.
- Creating new profile-2 `polish_dev`, seed 4509, completed at 07:59:04 with UUID `519b72ad-3130-46fb-a14e-4145d3db2f3d`, approximately three seconds after registration. Native spawn feet/head clearance and solid dry ground passed.
- An ignored temporary probe requested four distant chunks sequentially through Paper's asynchronous chunk API at chunk coordinates (1000,1000), (-1000,1000), (1000,-1000), (-1000,-1000). All contained intact bedrock; measured end-to-end requests were 1740, 1897, 2049 and 2253 ms. Each temporary ticket was removed.
- The earlier Milestone 5 synchronous force-load workload was replayed in the new disposable world at block positions (-1024,-800), (-848,-896), (-1024,-64), (-1024,160). Commands completed between 08:00:39 and 08:00:43 without a watchdog dump in this run. All forced tickets were removed. This does not establish unrestricted simultaneous exploration throughput.
- Disabling all eleven gameplay modules while retaining player records succeeded. Health showed zero crafting recipes and stopped wildlife/landmark gameplay. An invalid hydration flag was rejected with the prior disabled settings retained. Restoring valid enabled settings succeeded; all twelve modules, twelve crafting recipes and saved registries returned.
- A probe command issued in the same console batch as an asynchronous reload initially observed the previous disabled state. Running it after reload completion passed. This was verification timing, not a plugin failure.
- Normal shutdown flushed/saved successfully. With Geyser/Floodgate restored, a second startup reloaded all three worlds with the same UUID/profile and detected both provider APIs. The probe and global health checks were repeated after READY.

The host logged unrelated Paper OSHI/performance-counter and spark monitoring errors during these runs. No Wildlands startup or gameplay exception occurred; the intentional invalid-reload rejection was expected. Host pauses also caused wall-clock gaps, so timings are reported only for the actual generation requests.

The validation probe is ignored and removed from the server after testing; it is not included in the production JAR or release ZIP. The final artifact checksum accompanies the build. Later client presentation, real simultaneous multiplayer, broad terrain traversal and extended balance/load acceptance remain pending in ../FINAL-TEST-CHECKLIST.md.
