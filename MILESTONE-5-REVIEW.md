# Milestone 5 review — World generation

The full SPEC.md was read before implementation. The user's October 7 instruction defers further live Java/Bedrock gates to the final combined build; those checks remain explicitly untested.

| Requirement | Implementation and evidence |
| --- | --- |
| Deterministic rainforest terrain | Seeded continuous lattice fields use absolute coordinates; chunk-local generation ignores callback Random and generation order. Model and chunk-buffer tests pass, including negative boundaries. |
| Regions | Dense rainforest, tropical rivers, floodplains, wetlands, highlands, bamboo forests, clearings, waterfall valleys and rocky escarpments; cave networks are underground. Representative model sampling finds every region. |
| Waterways and recognizable terrain | Shared river contours, carved channels, stepped elevated river levels, cliff/highland relief and mossy boulders. Real Paper water, biome and ground checks match model coordinates. |
| Large trees and layered canopy | Trees at varied heights, rare 32-block giants, broad crowns, branches, bamboo and ferns. Neighbor candidates are reconstructed from a shared lattice and writes clipped to the current chunk. Persistent generated leaves retain broad crowns. Live visual/playability review is deferred. |
| Underground exploration and progression | Connected tunnel fields with occasional rocky entrances, deep lava, bedrock and clustered vanilla ores. No vanilla progression gates. Automated cave and chunk-generation checks pass; actual mining is deferred. |
| Disposable development worlds | Console explicitly created rainforest_dev on the disposable server; existing directories, reserved dimension names and path traversal are refused. No existing world was converted. |
| Persistence and configuration | Transactional schema 5 retains all earlier data; immutable seed/profile and world UUID are saved in SQLite. New worlds use validated worldgen.yml; existing profiles are frozen. Failed identity writes remain pending for retry and shutdown flush. |
| Lifecycle and independence | Registered dimensions reload through the modern Paper API, even with new creation disabled. Missing saved metadata fails startup; loaded identity/profile mismatches are refused. Existing worlds retain their generator when the creation module is disabled. |
| Crossplay and commands | Vanilla blocks/biomes and server chat; no client mods or assets. Admin creation/listing is permission-gated; status reports modeled region. Live parity is assumed by design, pending final acceptance. |

Final clean build passed all 99 tests. Real Paper startup, creation, safe-name/existing-world refusal, shutdown, same-UUID restart, disabled creation and representative block/biome checks passed. See docs/validation/milestone-5-server.md.

Outstanding final acceptance: traversability and canopy appearance on both editions, mining and waterfall visuals, region command presentation, and multiplayer generation throughput. Four distant console chunk loads triggered a recovered watchdog warning; optimize and reassess this in Milestone 9. Seasonal flooding remains optional and is not implemented; ruins and discoveries belong to Milestone 8. Wildlife and survival equipment are not included in this milestone.
