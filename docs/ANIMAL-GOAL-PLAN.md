# Animal and live-readiness scope

User objective: finish animals and their AI, sounds and behaviors; keep Java/Bedrock functionality without client mods; keep overhead practical for low-end machines; fully verify the candidate before one collective live session. This goal stays active across checkpoints. Completing a smaller tested subset does not complete it.

Current authoritative checkpoint: seven species (`wildlife.3`): Jaguar, Capybara, Tapir, Toucan, Monkey, Frog and Caiman. Original Milestones 1–9 remain present. Shared population/AI/render/sound budgets and species-aware schema-8 storage are in place. The seven-species roster is the chosen complete scope for this candidate. See MILESTONE-10-ANIMAL-REVIEW.md for the completion audit and ANIMAL-LIVE-TEST.md for deferred client acceptance.

Complementary fauna implemented:

- Toucan: canopy bird, bounded local flight/perching and sparse calls.
- Monkey: curious forest group/foraging movement and avoidance, without crop/inventory theft.
- Frog: small riparian life, bounded hopping/water behavior and sparse calls.
- Caiman: limited territorial water predator with warnings/retreat, modest damage and bounded pursuit.

These add canopy, small wetland and territorial-water encounters without adding an unlimited hostile population. They are implemented in the current candidate. Keep all animals under shared caps/rotations, use actual species models/animations/sounds on both packs and preserve visible named native equivalents when presentation is unavailable. Do not list an unimplemented species as available in commands or saved records.

Before completion, inspect every roster member's actual code/assets/runtime evidence: spawn and habitat, AI transitions/activity/target eligibility, fear/fire/group/pursuit limits, sound identifiers/decoding/rate limits, UUID/species/health/home persistence, death/transformation/breeding protections, Java proxy and Bedrock definition selection, pack decline/off/unavailable fallback, cleanup on unload/death/reload/disable/reconnect. Check renderer distances/LOD/entity counts and full-population native CPU/tick/network behavior; six actors alone do not prove the final load scope.

Deliver the combined plugin, matching optional extension and both packs, editable/provenance sources, low-end setup, deployment/migration instructions and one complete live-test checklist. Make pack delivery concrete for that session. Keep actual Java/Bedrock visuals/audio/controls/FPS acceptance separate from preflight evidence. Per the user's instruction, do not stop for individual live client gates. Do not mark the goal complete until the full requested animal/readiness scope has authoritative evidence; settlement infrastructure remains separately pending unless requested as part of this goal.
