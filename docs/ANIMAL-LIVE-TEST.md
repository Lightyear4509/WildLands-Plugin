# Collective animal candidate: 0.10.0-wildlife.3

This is one combined build of the existing survival modules and seven animals: Jaguar, Capybara, Tapir, Toucan, Monkey, Frog and Caiman. Settlement infrastructure remains pending. Automated/native evidence is separate from this **not yet performed** client acceptance session.

## Install the matching set

1. Stop Paper and back up its worlds plus `plugins/GGWPWildlands/` together. Schema 8 requires that full backup for rollback to an older plugin.
2. Install `GGWPWildlands-0.10.0-wildlife.3.jar`, removing its older JAR. Use Java 25 and Paper 26.2 build 121; launch Java client release **26.2**.
3. Enable wildlife for your generated rainforest world in `wildlife.yml`. Merge the new species sections from the bundled `wildlife-expansion.yml`: older files deliberately leave missing species disabled. Keep the shared world cap at 32 and the visual cap at 12 for this first session.
4. For custom Bedrock visuals, install the matching optional `Wildlands-Geyser-Models-0.10.0-wildlife.3.jar` in `plugins/Geyser-Spigot/extensions/` and `Wildlands-Bedrock-Wildlife-0.10.0-wildlife.3.mcpack` in its `packs/` folder. Remove older Wildlands extension/pack copies. The native preflight used Geyser 2.11.3 build 1245 and Floodgate 2.2.5 build 140. The guarded negotiation adapter is version-sensitive.
5. For both clients on this PC, run `python tools/serve_wildlife_pack.py build/packs/Wildlands-Java-Wildlife-0.10.0-wildlife.3.zip`. The release ZIP includes this helper in `asset-tools/`; pass the matching ZIP's path there. Copy its printed URL and SHA-1 into `wildlife-visuals.yml`, set `enabled: true`, and restart Paper. Keep the helper running. It binds only to 127.0.0.1 and serves only that ZIP. Use HTTPS hosting instead for clients on another PC; the loopback URL cannot work remotely.
6. Join with each edition, accept the matching pack, then use `/wildlands visuals on`. Java connects to `127.0.0.1:25565`; Bedrock to `127.0.0.1:19132`. Confirm status, HUD and permission checks from earlier milestones still work.

The helper uses port 8765 by default; `--port 8766` is available if occupied. Pack delivery must stay available during joins. SHA-1 is computed from the exact ZIP, not the source folder. Changing URL/hash or the animation interval requires restart.

## One session, both editions

Use operator commands on a disposable generated world. `/wildlands admin wildlife spawn <species> <world> <x> <y> <z>` needs loaded safe dry ground. Spawn away from lit campfires. The old form without a species still spawns Jaguar. For aquatic animals, place dry spawning ground beside a river or pool; monkeys/toucans benefit from trees with landing space. Wait for the distributed controller to sample animals.

| Animal | Observe on Java and Bedrock |
| --- | --- |
| Jaguar | Rosettes, moving legs/tail, stalk/warning pose and sounds; a nearby survival player receives a warning before modest bites; backing off, a lit campfire or three allies deter it; no endless chase |
| Capybara | Stocky body/short legs, sparse chirps, local water entry/swimming and groups; shy retreat when approached/hurt; bounded home return |
| Tapir | Dark body and trunk, sparse whistles, rest during day/forage at night, avoidance and home return |
| Toucan | Large colorful beak, wings, bounded flight/landing and nighttime perching; no suspended bird after module disable; sparse calls |
| Monkey | Long arms/tail, forest foraging/grouping and nighttime rest, calls and avoidance; no item/crop theft |
| Frog | Small green body, hopping and local swimming, sparse croaks/jump cue; no vanilla tongue attacks |
| Caiman | Low body/snout/tail, water entry/swimming; territorial warning before bites, finite pursuit and fire/group retreat; no egg-laying |

For every animal, check correct facing, texture, animation timing, audible idle/hurt/death cues and hitbox alignment. Java attacks on the visible proxy must reach the real animal with normal cooldowns; Bedrock attacks must use the same body/health. Compare survival and creative modes. Feeding, taming, riding, breeding and transformations must not bypass owned species identity. Untagged vanilla animals must still behave normally.

Check simultaneous clients with different pack preferences: both accepted; Java declined/Bedrock accepted; Java accepted/Bedrock declined; and visuals off. Named native fallbacks remain playable (Ocelot, Pig, Pig, Parrot, Cat, Frog, Turtle respectively). Geyser's `gameplay.force-resource-packs` and `enable-integrated-pack` must both be false to allow a declined-pack test; forced-pack policy can disconnect players before Wildlands sees them. Wildlands never silently changes that provider policy.

Reconnect, travel out of tracking range/back, unload/reload the area, reload configuration and restart the server. Verify UUID/species/home/injured health, restoration of native bodies and no leftover displays/proxies. Disable wildlife and visuals independently. Leave several players at 32 registered animals for at least 15 minutes: record TPS/average tick time, any server errors, client FPS and network/animation smoothness. Compare 12 models with `animate: false`, six models and visuals off on the lowest-end available PC. Native headless measurements do not establish client FPS.

Record edition/version, provider version, pack SHA-1, settings, test result and exact reproduction for failures. Mark each client result only after observing it. All these client acceptance checks remain pending until that session.
