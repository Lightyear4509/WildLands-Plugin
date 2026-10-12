# Three-species wildlife checkpoint evidence

October 11, 2026 local time. Paper 26.2 build 121, Java 25.0.4.1, Geyser 2.11.3 build 1245 and Floodgate 2.2.5 build 140. Candidate `0.10.0-wildlife.2`. This is progress on the active animal/testing goal, not its completion audit.

## Automated and assets

The regression suite passes 179 tests in 73 suites. It includes existing survival/world/exploration tests plus species-aware migration, legacy UUID/home preservation, each species after reopen/death, unknown-species rejection, shared caps including unloaded animals, habitats, passive fear/rest/forage/group/water transitions, config bounds, ownership guards and each Bedrock definition/base mapping. Presentation tests check cancellation fallback, unchanged stationary transforms, range/authorization, cleanup and pack gating.

All three species have separate Java item models and Bedrock entity/geometry/animation/render-controller files. The asset integrity test walks the complete current roster rather than Jaguar alone. Fifteen original Ogg cues decode to finite non-silent samples. Regeneration with canonical Ogg serial/CRC fields reproduces all 94 pack source files byte-for-byte. Neither decoding nor reference validation proves client playback.

## Native actor fixture

The disposable server was backed up before upgrading its schema-7 database to schema 8. Geyser loads one optional extension and registers three custom entities. Wildlands reaches READY and SQLite remains healthy with all original twelve modules enabled.

A temporary probe loads a bounded test area and spawns four Capybara, one Tapir and one Jaguar. The fifth Capybara in that cell is refused. Health is 16, 24 and 20 respectively. Native float navigation is enabled. The pig bases are adult, persistent, unsaddled and nonbreeding. Every actor constructs seven species-specific Paper models and a matching native interaction width. Cleanup removes every temporary model/proxy while preserving the animal.

The first fixture used an uncontained pool above a floating platform; flowing water pushed its Capybara off the platform and they died of falling. It was cleaned up and replaced by a contained, walled shallow pool. Those setup failures are not recorded as passed persistence tests. The contained fixture remains alive and exposes SWIMMING and GROUPING states; Tapir is RESTING in daytime and FORAGING at night. A controlled hurt event produces RETREATING. The probe does not involve human players or a rendered model view.

After adding local water destinations, a further five-second probe observes actual horizontal Capybara movement, not just the SWIMMING label. All six actors remain loaded/alive, and temporary cosmetic entity counts return to zero.

## Restart and absence

After normal shutdown, Geyser/Floodgate JARs were temporarily removed. Wildlands again reaches READY, reports both providers ABSENT and bridge=false. The same six actor UUIDs load from the saved chunk with their species/home metadata. One Capybara retains 15 health after injury, other Capybara retain 16, Tapir retains its deliberately set 7 health and Jaguar retains 20. Reload/adoption does not heal them or change species. Model construction/cleanup works without provider classes. Fixture cleanup leaves registered=0, loaded=0 and displays=0. Provider JARs are restored afterward.

## Performance and limits of evidence

The contained six-animal headless run reports 20.00 TPS and about 0.55 ms average tick on a JVM limited to two active CPU threads with a 2 GB maximum heap. This is a small controlled fixture, not a full-population benchmark or an FPS claim about a low-end client. Fresh model settings reduce the Java cosmetic budget to 12 models/96 transient entities, 32-block viewing and six-tick interpolation; static/LOD updates are bounded and tested. Larger roster/load measurements remain ongoing.

There are no new live Java/Bedrock results. Actual resource-pack acceptance, visible movement, custom audio, aiming, touch controls and mixed-pack/mixed-client tracking remain deferred. The existing Windows OSHI/spark performance-counter warnings are unrelated environment warnings. No new Wildlands/provider error was observed in the successful contained/restart runs.
