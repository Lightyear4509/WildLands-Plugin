# Milestone 8 native server validation

Validation on October 8, 2026 used Paper 26.2 build 121, Java 25.0.4.1, Geyser 2.11.3 build 1245 and Floodgate 2.2.5 build 140 in the ignored disposable server. No client participation was requested. Final candidate SHA-256: `96798BADF96161A8B8B59DF6D77197C37BD1EF190E66FC4AA16B19213C575237`.

- Schema 6 upgraded to 7 and existing `rainforest_dev` retained UUID `457de4f2-da59-4334-9455-69983be580ae` and generator version 1.
- New `expedition_dev`, seed 4509, was created with generator version 2 and UUID `18539dba-71ff-4240-852f-08440fd7d877`. Creation took approximately four seconds; no watchdog dump occurred.
- An ignored temporary probe asynchronously loaded the planned ruin at 600,112,-120 and used a temporary plugin chunk ticket. Native floor, open interior, entrance and corner wall passed. The manager registered `ruins-55a7c14fb07f`, UUID `55a7c14f-b07f-3fe5-b2b7-07eacf85b171`.
- A real native compass retained its lodestone coordinates and `tracked=false` through item serialization without requiring a lodestone block.
- Synthetic Java-shaped and Floodgate-shaped UUIDs saved separate discovery times and distinct expedition states using the actual storage worker. A synthetic private camp belonged only to the first UUID. These are server fixtures, not live player tests.
- Disabling and re-enabling landmarks via `/wildlands reload` succeeded; disabled state retained the registry without sampling. After a normal stop and restart with the final candidate, the probe verified the same world/ruin UUID, physical ruin, private owner access, non-owner refusal, separate discovery times, navigation target, completed trip and active expedition state.
- The first final deployment accidentally contained two plugin filenames; Paper reported ambiguity. The server was stopped, the duplicate removed and validation repeated with exactly one candidate JAR. The corrected run passed every exploration assertion.

The probe and temporary tickets were removed after validation. No validation plugin ships in the production JAR. Live client controls, display, discovery/camp journeys and final balance acceptance remain deferred at the user's request.
