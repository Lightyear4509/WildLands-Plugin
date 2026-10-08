# Milestone 10 — settlement infrastructure and wildlife expansion

This document records the user's requested next milestone. No Milestone 10 runtime feature or asset is implemented yet. The existing 0.9.0 build remains unchanged.

## Stages

1. Build one modeled Jaguar prototype: editable model/texture sources, species animation states, Java presentation, Bedrock presentation/mappings and species sound assets. Select and document a rendering adapter that works with the pinned Paper/Geyser setup. Verify adapter lifecycle and pack contents automatically/on the disposable server; defer live appearance acceptance to the collective build.
2. Upgrade Jaguar presentation without replacing its established server AI or persisted UUID homes. Add the proposed Capybara and Tapir with bounded population/habitat rules and their own behavior, animation and sound profiles.
3. Add named settlements, UUID membership/permissions and member-visible shared waypoints. Preserve private camps; do not introduce land claims or equipment locks.
4. Add rain-fed storage, charcoal treatment and clean-water dispensing with explicit conservation, full-output handling and atomic persistence. Keep the existing native station inventory authority where applicable instead of duplicating items across two storage systems.
5. Build one combined milestone artifact, update the crossplay matrix and checklist, review against SPEC.md, commit and push completed work. Carry forward the user's deferred live-test workflow; no assumed rendering or client result is marked tested.

## Proposed first wildlife roster

| Species | Model/animation needs | Behavior and AI | Sound profile |
| --- | --- | --- | --- |
| Jaguar | Species appearance; idle, walk, stalking, attack and retreat states | Preserve territory, warnings, bounded pursuit, night prey hunting and fire/group deterrence | Quiet idle, warning, attack, hurt and death cues |
| Capybara | Species appearance; idle, walk, swimming and rest states | Passive wetland groups, water preference, bounded local avoidance and escape | Sparse group/idle cues, hurt/death cues and native water movement where suitable |
| Tapir | Species appearance; idle, walk, forage and retreat states | Forest habitat, local foraging/rest cycle, shy avoidance and bounded retreat | Sparse contact/idle calls and hurt/death cues |

These are starting recommendations drawn from SPEC.md, not a promise to add every listed rainforest species in one milestone. Foraging is ecological presentation; it must not silently destroy player crops or require constant feeding. No giant health pools, unlimited detection or indefinite chase behavior.

## Crossplay and assets

Server AI, damage, hitboxes, spawning and persistence remain authoritative and identical across editions. Models and sounds are presentation layered over that state. Provide separate Java and Bedrock pack outputs, source assets, identifiers and mapping documentation. Use original or properly licensed assets and record provenance; do not copy third-party models or animal recordings without permission.

Geyser's [resource-pack guidance](https://geysermc.org/wiki/geyser/packs/) requires Bedrock pack delivery rather than assuming a Java pack is translated. Its [custom-item documentation](https://geysermc.org/wiki/geyser/custom-items/) describes explicit mappings. Paper [display entities](https://docs.papermc.io/paper/dev/display-entities/) offer a Java presentation mechanism, but this alone does not establish Bedrock compatibility. Geyser's [Rainbow guidance](https://geysermc.org/wiki/other/rainbow/) points to community display-entity extensions. These are options to investigate in the prototype, not selected runtime dependencies or validated compatibility claims. Do not use Rainbow's client mod as a requirement for players.

Custom visuals/sounds require downloaded resource-pack content. No Fabric/Forge/NeoForge/OptiFine or other client mod is permitted. An optional server rendering bridge may be necessary; evaluate its compatibility and distribution before selecting it. The plugin must still start without crossplay providers or a rendering bridge, using named vanilla equivalents when presentation is unavailable or a player declines a pack. Fallback animals must remain visible and interactable, not invisible hitboxes.

## Verification and performance

- Model/texture/sound references resolve in both pack outputs; manifests/identifiers are distinct and valid. Record actual client results separately from file validation.
- Behavior transitions, target eligibility, group/fire deterrence, sound cooldowns and loaded-terrain navigation are bounded and tested. Presentation does not become the AI authority.
- Reload, entity death, player reconnect, chunk unload and shutdown remove temporary visuals without duplicating animals or leaking entities. Retain UUID/home/species state across restart.
- Settlement membership is UUID-based and permission-gated. Nonmembers cannot access private member waypoints or administrative actions; normal vanilla building remains unrestricted.
- Water input/output/fuel accounting handles salt rejection, full inventories, hoppers, concurrency, failed writes and restart without duplication or loss.
- Use distributed budgets across all species and settlements. No world scans, per-tick SQLite writes or forced chunks for routine gameplay.
- Final collective client session covers both editions' models, animations, sounds, hitboxes, declined-pack fallback, group AI, settlements and water infrastructure.
