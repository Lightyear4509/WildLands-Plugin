# Milestone 10 Jaguar prototype validation

Date: October 9, 2026. Artifact: `0.10.0-prototype.1`. This records the first implementation stage, not completion of Milestone 10.

## Automated build

Java 25 / Gradle 9.1.0 clean build succeeds. The final regression run passes **162 tests in 69 suites**, zero failures/errors/skips. New tests cover model/texture/animation/Ogg references, config/hash validation, animation bounds, UUID/viewer filtering, Java pack rejection/unrelated/late status, native attack authorization and exact range, display cap/cleanup, sound burst/cooldown limits, visual reload and Bedrock pack negotiation/delegation/fallback. Existing 147 Milestones 1–9 tests remain passing. The optional bridge compiles separately against timestamp-pinned Geyser API/events; main-plugin dependencies remain locked.

## Native server probes

Disposable Paper 26.2 build 121, Temurin Java 25.0.4.1, Geyser 2.11.3 build 1245 and Floodgate 2.2.5 build 140. The plugin reaches READY, SQLite remains OK and the existing twelve modules enable. Geyser enables WildlandsModels and registers one custom entity. Health diagnostics detect the optional bridge.

A temporary native probe loads one disposable chunk, constructs a small dry spawn platform and asks WildlifeManager to spawn an authoritative Jaguar. It verifies native health, seven real ItemDisplays with namespaced model metadata, default-hidden/nonpersistent flags and a native Interaction with matching ocelot width/height. Detach removes all eight temporary entities and leaves the original mob valid. The probe then removes its animal and releases its chunk ticket. No temporary display or animal remains.

With Geyser and Floodgate JARs temporarily removed from the stopped disposable server, Wildlands again reaches READY with both providers ABSENT and bridge=false. The same real entity/model/hitbox/cleanup probe passes. Providers are restored afterward. No main-plugin provider class-loading failure occurs. The native probe JAR is removed before finishing; test server shutdown is normal.

The server's existing Windows OSHI performance-counter warning is unrelated to this plugin. No new Wildlands/Geyser extension error was observed during these checks.

## Limits of this evidence

There was no new human client session. Real pack-download/negotiation, visual geometry/texture/animation playback, custom sound playback, mixed-pack client tracking and mouse/touch combat controls remain **untested**. The pack observer's packet delegation and ordered accepted/refused/missing/unknown-provider states are exercised with a deterministic protocol-shaped test shim, not a claimed real Bedrock negotiation. Native entity construction does not establish visual parity. The user deferred live checks to a collective build.

The disposable provider retains its prior integrated/forced-pack configuration during these headless checks. Actual declined-pack acceptance needs the optional-provider settings documented in MILESTONE-10-PROTOTYPE.md; no native declined-client result is claimed here.

No database migration is introduced by this prototype. Capybara, Tapir, shared settlements and settlement water accounting remain later implementation stages. Their test results cannot be inferred from this checkpoint.

## Artifact checksums (SHA-256)

| Artifact | SHA-256 |
| --- | --- |
| GGWPWildlands-0.10.0-prototype.1.jar | DC6DF8221492386778F2CE8569E118E4EACFD0C8FF37589B06AEFC400736858F |
| Wildlands-Geyser-Models-0.10.0-prototype.1.jar | C92A6DD7F086F0A3BBEF418870C49068AA901776DE4F8AFB2E27BD3B2D6C2498 |
| Wildlands-Java-Wildlife-0.10.0-prototype.1.zip | F33EA9AD2BAF63246368DF3DBB78DB6B9B1A616CBBB7854A64B638096534D032 |
| Wildlands-Bedrock-Wildlife-0.10.0-prototype.1.mcpack | F93791271F48534184B3F3E11DDA7D124E8C3E0CA2ECE5830B4A3129A3094FA7 |
