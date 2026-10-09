# GGWP Wildlands --- Rainforest Survival

## Project Goal

Build **GGWP Wildlands**, a realistic-but-fun rainforest survival
overhaul for Minecraft.

Target: - Paper 26.2 - Java 25 - Geyser + Floodgate for Bedrock
crossplay

The experience should make players think about shelter, hydration,
weather, temperature, food, wildlife, navigation, exploration, and
settlement building while still feeling like Minecraft.

**Core principle:** realism should create interesting decisions, not
chores. Difficulty should come from learning and adapting to the
environment, not arbitrary punishment.

## Crossplay Requirement

Every gameplay feature must be fully usable by Java Edition and Bedrock
Edition players through Geyser/Floodgate.

Do not require Fabric, Forge, NeoForge, or any client-side Java mod.

Use a server-authoritative Paper plugin. Maintain separate Java and
Bedrock resource-pack assets when custom visuals are needed. If a
feature cannot be represented identically on both editions, provide
equivalent functionality.

No gameplay feature is complete until Java and Bedrock behavior has been
tested.

## Survival Philosophy

Avoid mechanics that simply punish players on timers. Hydration,
temperature, weather, shelter, and wildlife should create decisions and
preparation opportunities.

Example: exploring during a hot afternoon increases hydration use; the
player finds a river and chooses between untreated water or establishing
camp to treat it.

Do not create difficulty primarily through huge enemy health pools,
constant damage, or excessive negative effects.

## World Generation

Create a deterministic custom rainforest world substantially different
from a vanilla Jungle biome.

Regions: - Dense Rainforest --- primary wilderness - Tropical Rivers ---
water, fishing, transportation - Floodplains --- fertile terrain with
seasonal risk - Wetlands --- abundant resources with drawbacks -
Rainforest Highlands --- cooler terrain and minerals - Bamboo Forests
--- specialized resources - Jungle Clearings --- settlement
opportunities - Waterfall Valleys --- water and landmarks - Cave
Networks --- underground exploration - Rocky Escarpments --- elevation,
cliffs, minerals

Generate large trees and layered canopy while preserving playability.
Prefer interconnected river networks where practical.

Natural landmarks should include waterfalls, enormous trees, rock
formations, cliffs, caves, river junctions, clearings, and ruins.
Terrain should be recognizable enough that players can eventually
navigate partly by landmarks.

## Hydration

Track hydration from 0--100. Normal loss must be slow.

Factors: - temperature - activity - sprinting - combat - weather -
season - shade - time of day

Conceptual formula:

    hydrationLoss = baseRate * activityMultiplier * temperatureMultiplier * environmentMultiplier

Moderate dehydration should affect recovery/efficiency before direct
danger occurs.

Water quality: - Rainwater: CLEAN - Spring: CLEAN - River: UNTREATED -
Pond: QUESTIONABLE - Swamp: CONTAMINATED - Ocean: SALT

Treatment progression: Boiling -\> Basic Filtration -\> Improved
Filtration -\> Settlement Water System

Unsafe water should introduce risk rather than guaranteed punishment.

## Seasons

Use rainforest seasons: - DRY - TRANSITION_TO_WET - WET - MONSOON -
TRANSITION_TO_DRY

Dry season: less rain, warmer days, reduced water availability, easier
land travel. Wet season: frequent rain, greater water availability,
faster plant growth, higher humidity. Monsoon: heavy storms, difficult
travel, high water availability, increased lowland flood risk where
practical.

All durations must be configurable.

Example:

    seasons:
      enabled: true
      dry:
        days: 14
      transition-wet:
        days: 4
      wet:
        days: 14
      monsoon:
        days: 5
      transition-dry:
        days: 4

## Temperature

Calculate environmental temperature from biome, elevation, season, time,
weather, shade, water exposure, nearby heat, activity, and applicable
equipment.

Temperature should mostly modify other systems: - HOT -\> faster
hydration loss - COLD + WET -\> reduced comfort/recovery - COMFORTABLE
-\> normal behavior

Only prolonged extreme conditions should become directly dangerous.

## Wetness

Track wetness from 0--100. Rain, swimming, and water exposure increase
it. Shelter, warmth, dry weather, and time reduce it.

Wetness interacts with temperature. Rain itself should not automatically
harm players.

## Shelter

Detect roof coverage, partial enclosure, dry ground, nearby heat, and
weather protection. Do not require predetermined building shapes.

Possible status:

    SHELTER
    Protected
    Dry
    Temperature: Comfortable
    Rest Quality: Good

Emergency shelters should work; better shelters provide modest
advantages.

## Fire and Camp Infrastructure

Campfires should support cooking, water boiling, warmth, drying, light,
basic food preservation, and limited wildlife deterrence.

Progression: Campfire -\> Cooking Rack -\> Water Boiler -\> Rain
Collector -\> Charcoal Kiln -\> Water Filter -\> Improved Stove

Preserve vanilla crafting where practical.

## Food and Agriculture

Keep vanilla hunger recognizable. Do not simulate detailed calories or
micronutrients.

Optional broad categories: fruit, vegetables, roots, protein, fish,
nuts, prepared meals. Variety may provide modest benefits, but
repetitive foods must remain viable.

Agriculture may respond to soil, rainfall, temperature, season, and
water availability. Farming should remain understandable and not become
tedious.

## Wildlife

Wildlife should feel ecological rather than like extra monsters.

Potential species: jaguar, tapir, capybara, monkey, toucan, parrot,
snake, frog, caiman, fish, insects.

Most animals should be PASSIVE, NEUTRAL, or TERRITORIAL.

Use behaviors such as territory, stalking, warnings, retreat, defending
young, hunting prey, avoiding fire/groups, activity cycles, and habitat
preferences. Predators should not detect players from absurd distances
or chase indefinitely.

Populations should correspond to habitat.

## Navigation and Exploration

Encourage maps, compasses, rivers, mountains, waterfalls, landmarks, and
player camps. Do not remove coordinates unless explicitly configured.

Implement discoverable landmarks and a way to review discoveries.

## Progression

Maintain vanilla equipment progression while adding survival
progression.

Primitive: - stone tools - basic shelter - campfire - basic water
treatment

Camp: - rain collector - cooking equipment - food storage - survival
supplies - iron tools

Settlement: - agriculture - filtration - food preservation - improved
shelter - navigation equipment

Expedition: - specialized/portable survival gear - diamond tools -
remote camps - improved navigation

Wilderness Mastery: - permanent settlement - advanced infrastructure -
long expeditions - Netherite - deep-wilderness exploration

Do not artificially lock vanilla progression merely to extend playtime.

## HUD

Keep the HUD minimal and Geyser-friendly.

Example:

    Hydration   ████████░░ 82%
    Temp        Warm
    Wetness     15%
    Season      Wet

Prefer action bars/boss bars or similarly reliable interfaces.

Commands: - /wildlands hud on - /wildlands hud off

## Player Data

Persist UUID, hydration, wetness, relevant temperature state, discovered
landmarks, progression, settings, and relevant seasonal state.

Never use player names as primary identifiers. Floodgate players must
work correctly.

## Performance

Never scan the entire world every tick.

Avoid full-world block scans, constant container scans, per-tick
database writes, unnecessary force-loading, and expensive AI every tick.

Starting guidelines: - environment sampling: 1--2 seconds - hydration:
5--10 seconds - temperature: \~5 seconds - shelter detection: \~5
seconds - wildlife AI: distributed/batched - database saves: batched -
season evaluation: infrequent

Use Paper's scheduler correctly. Do not access world state
asynchronously when the API requires server-thread access. Perform
database/file work asynchronously where safe.

## Persistence

Use SQLite for significant persistent data and YAML for configuration.

Potential tables: - players - landmarks - discoveries - season_state -
wildlife - settlements

Use prepared statements and sensible batching/transactions.

## Configuration

Suggested files: - config.yml - worldgen.yml - seasons.yml -
hydration.yml - temperature.yml - wildlife.yml - food.yml - messages.yml

Administrators must be able to disable modules independently:

    modules:
      hydration: true
      temperature: true
      wetness: true
      seasons: true
      wildlife: true
      shelter: true
      nutrition: true
      landmarks: true

Validate configuration and fail safely where practical.

## Architecture

Use modular services:

    GGWPWildlands/
    ├── core/
    │   ├── WildlandsPlugin
    │   ├── ModuleManager
    │   └── PlayerManager
    ├── survival/
    │   ├── HydrationService
    │   ├── TemperatureService
    │   ├── WetnessService
    │   ├── ShelterService
    │   └── NutritionService
    ├── seasons/
    │   ├── Season
    │   ├── SeasonManager
    │   └── WeatherManager
    ├── world/
    │   ├── WorldGenerator
    │   ├── BiomeManager
    │   ├── LandmarkManager
    │   └── StructureManager
    ├── wildlife/
    │   ├── WildlifeManager
    │   ├── HabitatManager
    │   └── behaviors/
    ├── items/
    │   ├── CustomItemManager
    │   ├── RecipeManager
    │   └── SurvivalItems
    ├── ui/
    │   ├── HudManager
    │   └── MessageManager
    ├── crossplay/
    │   ├── GeyserBridge
    │   ├── FloodgateBridge
    │   └── PlatformAdapter
    ├── storage/
    │   ├── Database
    │   ├── PlayerRepository
    │   └── WorldRepository
    └── commands/

Use interfaces where platform-specific implementations may be required.
Avoid unnecessary module coupling.

## Commands

Player: - /wildlands - /wildlands status - /wildlands hud - /wildlands
help - /season - /season info - /hydration - /landmarks - /landmark
`<name>`{=html}

Admin: - /wildlands reload - /wildlands admin season `<season>`{=html} -
/wildlands admin hydration `<player>`{=html} `<value>`{=html} -
/wildlands admin temperature `<player>`{=html} - /wildlands admin debug
`<player>`{=html} - /wildlands admin modules

Permissions: - ggwpwildlands.use - ggwpwildlands.admin -
ggwpwildlands.admin.debug

Console-compatible admin commands must not assume a player sender.

## Resource Packs

Maintain: - assets/java/ - assets/bedrock/

Create and maintain CROSSPLAY-COMPATIBILITY.md with Java, Bedrock, and
Tested status for every crossplay-facing feature.

Do not assume Java assets automatically work on Bedrock.

## Testing

Create automated tests where practical.

Every gameplay milestone requires manual testing with: - Java Edition -
Bedrock Edition through Geyser/Floodgate

Never mark a feature tested unless it was actually tested. Use
disposable worlds for world-generation development.

# Development Milestones

## Milestone 1 --- Foundation

Implement: - Paper 26.2 project - Java 25 - configuration system -
module system - SQLite - UUID player persistence - command framework -
Geyser/Floodgate detection - debug system

Plugin must load reliably before survival mechanics.

## Milestone 2 --- Hydration

Implement only hydration, water-source identification, drinking,
treatment/boiling, basic HUD, and persistence. Test Java + Bedrock.

## Milestone 3 --- Environment

Implement temperature, wetness, shade, campfire warmth, and shelter
detection.

## Milestone 4 --- Seasons

Implement season clock, rainfall changes, temperature effects, crop
modifiers, and weather behavior.

## Milestone 5 --- World Generation

Implement rainforest terrain and regions using disposable development
worlds.

## Milestone 6 --- Wildlife

Begin with one species, preferably Jaguar. Perfect behavior and
crossplay representation before adding more species.

## Milestone 7 --- Survival Crafting

Add rain collector, water filter, cooking equipment, survival equipment,
and food preservation.

## Milestone 8 --- Exploration

Add landmarks, ruins, discoveries, navigation, and expedition mechanics.

## Milestone 9 --- Polish

Optimize performance, balance systems, improve resource packs, fix
Bedrock inconsistencies, conduct multiplayer testing, and improve admin
tools/documentation.

## Milestone 10 --- Settlement Infrastructure and Wildlife Expansion

Added at the user's request after completion of Milestones 1–9. This is
approved scope, not a claim that the full milestone already exists. The
first optional modeled Jaguar prototype is implemented; the remaining
species and settlement stages are still pending.

Settlement infrastructure:

- Named shared settlements with UUID-based owners and members.
- Shared camp waypoints and member navigation, retaining private camps.
- Rain-fed water storage, charcoal-powered freshwater treatment and
  clean-water dispensing, with conservation and duplication safeguards.
- Optional infrastructure without land claims or artificial restrictions
  on vanilla progression.

Wildlife expansion:

- Species-specific models, textures, animations, sounds and behavior.
- Initial proposed roster: upgrade Jaguar presentation; add Capybara
  and Tapir. Other rainforest species remain future expansion work.
- Jaguar retains stalking, territorial warnings, bounded pursuit, prey
  hunting and fire/group deterrence. Capybara emphasizes wetland groups,
  swimming and avoidance; Tapir emphasizes forest foraging, rest and
  retreat. Animals should feel ecological rather than like extra monsters.
- Species configuration, habitat/population limits, UUID persistence,
  bounded server-authoritative AI and administrator diagnostics.
- Idle, movement, warning, hurt and death sounds as appropriate to each
  species, with throttling so groups do not produce constant noise.
- Shared gameplay/hitboxes across clients and separate Java/Bedrock
  resource-pack assets and mappings for custom presentation. Resource
  packs may be downloaded by clients; no client-side mod is permitted.
- Validate a single modeled Jaguar prototype and its rendering adapters
  before expanding the roster. Do not assume Java entity models or
  display rendering translate automatically through Geyser. Any needed
  server-side bridge must be identified and documented explicitly.
- When custom presentation is unavailable or a pack is declined,
  retain usable named vanilla equivalents with the same gameplay.

Implement this milestone in stages, preserving the existing architecture.
Maintain independent settlement/wildlife configuration and safe optional
provider behavior. Test conservation, access control, AI transitions,
population caps, animation/display cleanup, sound limits and restart
persistence automatically and on a disposable server where practical.
At the user's request, defer additional live Java/Bedrock checks to one
collective milestone build; never record deferred client checks as passed.

# Codex Development Rules

1.  Read this entire specification before implementing a milestone.
2.  Implement only the requested milestone unless a prerequisite is
    required.
3.  Never introduce client-side mod requirements.
4.  Preserve Java/Bedrock feature parity.
5.  Build after meaningful implementation changes.
6.  Fix compilation errors before declaring a milestone complete.
7.  Add automated tests where practical.
8.  Do not replace working architecture unnecessarily.
9.  Keep systems modular and configurable.
10. Document important architectural decisions.
11. Update README.md when build/install behavior changes.
12. Update CROSSPLAY-COMPATIBILITY.md for crossplay-facing changes.
13. Never mark untested features as tested.
14. Clearly identify manual Java/Bedrock testing requirements.
15. Prioritize stability and data safety over adding features quickly.

# Initial Codex Task

Read SPEC.md completely before making changes.

Implement **Milestone 1 only: Foundation**.

Create a production-quality Paper 26.2 plugin targeting Java 25.

Implement: - modular plugin architecture - configuration loading -
module manager - SQLite persistence - UUID-based player records -
/wildlands command framework - Geyser/Floodgate detection -
administrator debug framework

The plugin must run when Geyser/Floodgate are absent while
detecting/integrating with them when installed.

Do not implement hydration, seasons, temperature, custom world
generation, wildlife, custom items, or later gameplay systems yet.

Do not introduce client-side mod requirements.

Build the project and fix all compilation errors. Add automated tests
where practical.

Create/update README.md with build and installation instructions. Create
CROSSPLAY-COMPATIBILITY.md.

Before finishing: 1. Review implementation against Milestone 1. 2. Build
the project. 3. Report what was implemented. 4. Report what was tested.
5. Identify anything requiring manual testing or further work.

# Long-Term Vision

GGWP Wildlands should feel like Minecraft survival rebuilt around
understanding and adapting to a living wilderness.

At first, the rainforest should feel unfamiliar and threatening. As
players learn reliable water sources, weather patterns, animal habitats,
shelter design, seasonal resource changes, navigation, and expedition
preparation, the environment should become increasingly manageable.

The goal is not to make Minecraft brutally difficult.

The goal is to **make surviving the wilderness interesting**.
