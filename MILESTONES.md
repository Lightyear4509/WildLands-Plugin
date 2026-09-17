# Milestone execution and acceptance

The current user goal is to complete all nine milestones from SPEC.md, test each thoroughly before proceeding, and commit every milestone. The original Milestone 1-only request is superseded by that goal; feature work still proceeds in milestone order.

| Milestone | Scope | Current state |
| --- | --- | --- |
| 1 | Foundation, modules, configuration, SQLite, UUID records, commands, crossplay bridges, debug | Accepted: 26 automated tests, real server checks, Java/Bedrock commands and reconnects, permission denial, and restart persistence passed |
| 2 | Hydration, water identification, drinking, treatment/boiling, HUD, persistence | Not started |
| 3 | Temperature, wetness, shade, fire warmth, shelter | Not started |
| 4 | Seasons, rainfall, temperature effects, crops, weather | Not started |
| 5 | Deterministic rainforest regions and terrain, disposable development worlds | Not started |
| 6 | One polished wildlife species, initially Jaguar, with crossplay representation | Not started |
| 7 | Survival crafting and camp/settlement equipment | Not started |
| 8 | Landmarks, ruins, discoveries, navigation, expeditions | Not started |
| 9 | Performance, balance, resource packs, multiplayer/crossplay polish, admin tools/docs | Not started |

## Gate for each milestone

1. Read the complete specification and inspect current code.
2. Implement only that milestone and necessary prerequisites.
3. Run relevant automated tests and a clean build; correct failures.
4. Exercise real Paper lifecycle, persistence, configuration, and integration behavior as applicable.
5. For gameplay, manually test Java and Bedrock through Geyser/Floodgate; record actual versions, observations, and unresolved defects.
6. Review every requirement, update README and crossplay matrix, and commit the milestone.
7. Proceed only once its required acceptance checks pass. A checkpoint commit is not evidence that a pending gate passed.

## Milestone 1 acceptance environment

The user confirmed both client editions are available on this PC and explicitly accepted the Minecraft EULA for the local test server.

The local test server runs Paper 26.2 build 121, Geyser 2.11.3 build 1245, and Floodgate 2.2.5 build 140. Java connects at 127.0.0.1:25565; Bedrock at 127.0.0.1:19132.

A reported outdated-server error followed installation of Java client 26.3. Official checks on 2026-09-17 found only alpha Paper 26.3 builds and no released ViaVersion 26.3 translation. Installed Geyser advertises Java backend support through 26.2. The user launched Java release 26.2 successfully. Both real clients then passed the foundation command/reconnect checks and denied admin debug. Bedrock detection and both database records were verified, including preserved timestamps after server restart.

See docs/validation/milestone-1-server.md and CROSSPLAY-COMPATIBILITY.md. All milestones remain part of the goal.
