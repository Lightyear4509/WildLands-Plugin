# Milestone 10: first Jaguar presentation prototype

Version `0.10.0-prototype.1` starts stage 1 of the approved plan. It preserves the existing Jaguar AI, damage, health, UUID/home persistence and native ocelot fallback. The completed Milestones 1–9 systems remain present. It does not yet add Capybara, Tapir or settlements and must not be treated as the complete Milestone 10 release.

## Build outputs and installation

Run the normal Java 25 Gradle build. It creates:

- `build/libs/GGWPWildlands-0.10.0-prototype.1.jar`: shaded Paper plugin; install only this Wildlands plugin JAR.
- `build/libs/Wildlands-Geyser-Models-0.10.0-prototype.1.jar`: optional Geyser extension.
- `build/packs/Wildlands-Java-Wildlife-0.10.0-prototype.1.zip`: Java 26.2 pack, resource format 88.0.
- `build/packs/Wildlands-Bedrock-Wildlife-0.10.0-prototype.1.mcpack`: separate Bedrock pack.
- `build/distributions/GGWPWildlands-0.10.0-prototype.1-release.zip`: all outputs, assets/source documentation and configuration examples.

Stop the server before replacing JARs. The prototype remains disabled by default in `plugins/GGWPWildlands/wildlife-visuals.yml`. Wildlife itself uses its existing module flag and `wildlife.yml` world/population rules. Enable visuals only in a development/test deployment until the deferred appearance and interaction checks pass.

For Java, host the generated ZIP over HTTPS and configure `java.url` and its SHA-1 in `wildlife-visuals.yml`. PowerShell `Get-FileHash <zip> -Algorithm SHA1` gives the hash. Pack URL/hash changes require a server restart to prevent overlapping negotiation of different content under the same pack ID. The pack is optional, never forced. `/wildlands visuals on` offers it; the model activates only on a successful load event for Wildlands' own pack. Decline/download failure, `/wildlands visuals off`, distance, module shutdown and disconnect retain/restore the native animal.

For Bedrock, install Geyser-Spigot and Floodgate as usual. Put the optional extension in `plugins/Geyser-Spigot/extensions/` and the Bedrock `.mcpack` in `plugins/Geyser-Spigot/packs/`, then restart. Use `/wildlands visuals on` after joining. The bridge checks that the Wildlands pack was offered and negotiation completed; missing, declined or unsupported detection stays on the native ocelot. `/wildlands visuals off` restores the native presentation. Preferences are session-only; reconnect begins on the fallback. Do not require client mods or force resource-pack acceptance.

Geyser config version 8 defaults to forced packs. To permit declined-pack connections, set both `gameplay.force-resource-packs: false` and `gameplay.enable-integrated-pack: false`; enabling Geyser's integrated pack automatically forces acceptance again. Disabling its integrated pack also disables that pack's extra vanilla-content presentation. Wildlands does not silently change these provider-wide settings. If the operator forces packs, Geyser can disconnect a declining client before Wildlands receives a player session; the fallback cannot override that policy.

## Adapters and authority

Java uses seven original item models on transient Paper ItemDisplays. Displays and a transient Interaction proxy are hidden by default, shown only to nearby players who loaded the pack, and limited by `max-animals` (1–64, default 32). The proxy has the native animal's width/height. Valid attacks forward to the real mob through Paper's native player attack path, preserving cooldowns, armor, damage events and Jaguar provocation. The proxy has no health, drops, inventory, AI or saved identity. The original animal is hidden only from that opted-in Java viewer; other players continue to see it. Displays update every four ticks with interpolation, never force chunks and are removed on detach/unload/death/disable or lost viewers. Crash leftovers are tagged and cleaned on loaded-entity events.

Bedrock uses Geyser's [experimental custom-entity API](https://geysermc.org/wiki/geyser/custom-entities/) to replace only registered Jaguar UUIDs for opted-in, pack-ready connections. It keeps the original server entity ID and hitbox. The extension registers `ggwpwildlands:jaguar` and the integer `ggwpwildlands:phase` property (idle=0, stalking=1, warning=2, attacking=3, retreating=4). The extension reads immutable concurrent UUID/state data; it never reads Bukkit world/entity state from Geyser threads. It is separate from the Paper plugin so absent providers cannot cause mandatory class loading.

The tested server provider is Geyser 2.11.3 build 1245. Because its public API has no pack-acceptance event, a small guarded compatibility observer wraps the existing Bedrock packet handler through reflection. It delegates every packet to the original handler and observes only resource-pack responses. It enables models only when the specific Wildlands pack was offered and ordered HAVE_ALL_PACKS/COMPLETED responses succeeded without refusal. Unknown provider methods, failed reflection and absent packs fail closed to visible native entities. This internal adapter is version-sensitive; revalidate when upgrading Geyser. Do not advertise support for arbitrary Geyser versions on the strength of these tests.

Geyser does not convert Java packs. [Bedrock pack delivery](https://geysermc.org/wiki/geyser/packs/) is configured separately. No display-entity translation extension, ModelEngine, OptiFine, Fabric or other client mod is required by this approach.

The original voxel model has rosette textures and independently moving legs/head/tail. Java animates walking, crouching and warning poses over the existing AI. Bedrock declares idle/walk/stalking/warning/attack/death animation clips; client appearance remains unverified. Five original synthesized cues cover idle, warning, attack, hurt and death. They are stylized cues, not animal field recordings. Cue emission is limited to one per animal per two seconds and at most four across the manager per second. Existing native sound equivalents play for viewers without the pack.

## Verification and remaining work

Automated checks cover accepted/declined/unrelated/late Java pack events, display budgets, authorized/in-range proxy attacks, detach/shutdown cleanup, animation bounds, UUID/viewer filtering, missing/declined/malformed Bedrock negotiation and unchanged packet delegation. Pack checks resolve model/texture/animation/sound references and validate manifests and Ogg signatures. Native Paper probes create real displays, check model tags and native hitbox dimensions, remove every temporary entity and preserve the authoritative animal. Provider startup and absence are checked separately.

Actual Java/Bedrock model appearance, animation timing, sound playback, aiming/click controls, negotiation and simultaneous mixed-pack players remain deferred to the collective client session. File validation and simulated negotiation are not live client acceptance.

Next stages must expand the roster and per-species AI/configuration/persistence, add shared settlements and conservative water infrastructure, and deliver the completed collective Milestone 10 build. Additional hurt/death pose polish, entity-interaction coverage, per-client provider upgrade checks and longer load testing belong in that work. See `MILESTONE-10-PLAN.md` and the crossplay matrix.
