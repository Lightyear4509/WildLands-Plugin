# Milestone 10 first-stage implementation review

The full SPEC.md was read before implementation. This checkpoint starts the approved Jaguar prototype stage and preserves the completed Milestones 1–9. **Milestone 10 remains in progress.**

| SPEC.md requirement | Checkpoint result |
| --- | --- |
| Single modeled Jaguar before expanding roster | Original voxel model/texture, animation profiles and five synthesized cues supplied in separate Java/Bedrock packs |
| Preserve established Jaguar AI and UUID homes | Existing controller/storage retained; presentation reads its phase and never becomes damage/AI authority |
| No client-side mods | Optional native resource packs; optional server-side Geyser extension; no Fabric/Forge/OptiFine requirements |
| Separate rendering adapters | Java transient displays/Interaction proxy; Bedrock custom-entity API replaces only registered UUIDs for pack-ready viewers |
| Unavailable/declined-pack fallback | Visible named native equivalent; own Java pack success required; guarded Bedrock negotiation detection; unknown provider shape fails closed |
| Bounded work, sound limits and cleanup | Display/distance cap, interpolated four-tick updates, per-animal/global sound limits, nonpersistent tagged visuals and lifecycle removal |
| Modular configuration and optional providers | Separate validated wildlife-visuals.yml, existing wildlife lifecycle, separately compiled/distributed extension and native provider-absence startup |
| Capybara/Tapir behavior, AI, presentation and species persistence | Not implemented in this first-stage checkpoint |
| Settlement owners/members, shared navigation and water infrastructure | Not implemented in this first-stage checkpoint |
| Automated/native verification | 162 tests pass; actual Paper model/hitbox creation and cleanup, Geyser registration and absent-provider checks recorded |
| Full Java/Bedrock live acceptance | Deferred by user; never marked passed |

README, CROSSPLAY-COMPATIBILITY.md, milestone plan/status, asset provenance, prototype installation/architecture and the collective checklist are updated. The prototype is off by default and versioned explicitly; it is not presented as a production-complete wildlife expansion or completed collective Milestone 10 build.

Remaining work includes the roster and settlement stages, pose/interaction polish, actual pack negotiation/rendering/audio acceptance and mixed-client/load checks. Geyser's experimental entity API and guarded internal pack observer require revalidation on provider upgrades. See docs/MILESTONE-10-PROTOTYPE.md and docs/validation/milestone-10-prototype-server.md.
