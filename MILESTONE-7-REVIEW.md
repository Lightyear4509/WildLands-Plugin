# Milestone 7 review — survival crafting

SPEC.md was read completely before implementation. The user authorized proceeding through milestones without further live Java/Bedrock checks and requested one combined build at the end. This review covers crafting only; no Milestone 8 exploration features were added.

| Requirement | Implementation | Verification |
| --- | --- | --- |
| Rain collector | Tagged barrel converts one empty bottle per active rainy visit; requires exposed sky and rain-capable biome | Pure inventory conservation/full-capacity tests and real Paper open/roofed collectors |
| Water filter | Basic unsafe-water improvement plus improved safe freshwater treatment; charcoal cost and salt rejection | Rules/inventory tests and actual native inventories |
| Cooking equipment | Cooking rack, charcoal kiln, water boiler, improved stove; native interfaces/recipes | Native recipe matching and real furnace/smoker cooking |
| Survival equipment | Wearable rain cloak and refillable three-drink waterskin | Rain/immersion/drying rules, native consumption replacement guards, real item tags and recipe match |
| Food preservation | Sheltered campfire drying rack preserves cooked meat/fish; independent nutrition adds a small saturation reserve | Conservation/rack-condition tests, native preservation, delayed bonus/hunger cap/module-disable tests |
| Modular/configurable | Independent crafting/nutrition flags and validated crafting.yml/food.yml; recipes/tasks removed on disable | Validation, upgrade opt-in and lifecycle tests; real Paper reload checks |
| Crossplay | Vanilla containers, armor, cooked food and water potions; main-hand use; no Java-only controls/assets | Architecture review; Geyser/Floodgate coexistence; live interaction deferred |
| Performance/data safety | Bounded rotating loaded-station queue, clone-plan-commit inventory transaction, native world/player persistence | Unit conservation tests, server restart inventory/tag verification; no per-tick SQL or chunk forcing |

The database remains schema 6. Native container inventories and item metadata remain authoritative in chunk/player data; duplicating inventories in SQLite would introduce rollback and duplication risks. Existing UUID-based survival/season/world/wildlife records remain unchanged. Physical crafted upgrades provide progression without locking vanilla equipment.

All recipes remain accessible without client mods or resource packs. The kiln uses a distinct campfire/cobblestone recipe instead of colliding with the vanilla furnace recipe. Standard item metadata is required by exact upgrade/refill/boiling recipes. Cooking racks/kilns use recognizable vanilla functionality; improved stoves provide the fuel advantage. Preservation has no spoilage chores.

Automated verification: 123 tests, zero failures/errors; Java 25 compilation and shaded build passed. Real Paper evidence is recorded in docs/validation/milestone-7-server.md. Initial probe assertions incorrectly checked already-expired fuel and assumed the furnace's burn duration for a smoker. The corrected probe captured the native smoker burn event before/after the modifier: 800 → 1600 ticks. Both were probe issues; all native gameplay operations passed.

Remaining acceptance: live recipe controls, waterskin consumption/animation, armor use, container interactions/hoppers, food display and multiplayer behavior on both editions. These are deferred to the final collective build, explicitly untested. Milestones 8 and 9 remain.
