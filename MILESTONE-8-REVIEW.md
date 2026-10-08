# Milestone 8 review — exploration

SPEC.md was read completely before implementation. The user authorized all milestones and deferred further live Java/Bedrock checks to the final collective build. This milestone adds exploration only.

| Requirement | Implementation | Verification |
| --- | --- | --- |
| Landmarks | Deterministic natural region landmarks, stable UUIDs, bounded loaded-chunk registration and nearby discovery | Planner, multiplayer isolation and native Paper registration tests |
| Ruins | Walkable mossy stone ruins in new generator-version-2 worlds; chunk-local generation without live terrain edits | Chunk clipping/profile tests; real floor, walls, entrance and restart checks |
| Discoveries | UUID-keyed SQLite journal with earliest discovery time, pagination and asynchronous recovery | Transaction/restart tests; separate Java-shaped and Floodgate-shaped synthetic UUIDs persisted on Paper |
| Navigation | Known-landmark/private-camp targets, bearing/distance HUD, native lodestone compass metadata | Permission/ownership/unknown-target tests, native compass serialization |
| Expeditions | Sheltered personal camps, departure/return tracking, longest distance and cosmetic ranks | Camp relocation/roof checks, pure expedition rules and active/completed restart persistence |
| Modular/data safety | Independent landmarks flag, validated landmarks.yml, schema-7 migration, batched snapshots and failed-write retention | Genuine schema-6 migration preserving world/wildlife data, transaction rollback/retry and real module reloads |
| Crossplay | Vanilla blocks, compass, chat and action bar; identical authoritative rules | Architecture review and Geyser/Floodgate coexistence; client acceptance explicitly deferred |

Generator versions 1 and 2 remain frozen in each saved world profile. Existing version-1 worlds retain their terrain and gain natural discoveries without retroactive ruins or destructive edits. New version-2 worlds add ruins. Camps are private navigation markers, not claims, teleports or progression locks. Compass targets can reference unloaded locations without loading chunks; rerun navigation while holding the compass after relocating a camp.

The clean Java 25 build passed 137 tests with zero failures. Native Paper restart evidence is in docs/validation/milestone-8-server.md. Live discovery messages, compass presentation, physical camp interaction and multiplayer journeys on both client editions remain untested and are deferred to the final checklist. Milestone 9 remains.
