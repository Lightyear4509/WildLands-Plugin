# Crossplay compatibility — Milestone 1

No client-side mods or resource packs are required. This milestone uses normal server commands/chat and server UUIDs; it introduces no survival mechanics, custom visuals, or HUD.

**The local Java + Bedrock foundation acceptance checks passed on 2026-09-17.** Scope: Paper 26.2 build 121, Java client 26.2, Bedrock installed package 1.26.5101.0, Geyser 2.11.3 build 1245, Floodgate 2.2.5 build 140, Windows 11, both clients on the same PC. Server logs independently confirm both logins/reconnects and the Bedrock protocol reported by Geyser. The user confirmed commands worked and admin debug denied permission on both clients.

| Feature | Java behavior | Bedrock behavior via Geyser/Floodgate | Tested |
| --- | --- | --- | --- |
| Plugin startup and shutdown | Standard Paper plugin | Same server plugin | Real Paper startup/shutdown passed, with and without optional providers |
| Status command and chat output | Plain chat commands | Same command and message format | Both clients: user-confirmed working |
| Debug permission denial | Server permission nodes | Same server permission nodes | Both non-op clients: user-confirmed denied |
| Tab completion visibility | Server permission nodes and visibility | Edition-specific completion UI | Automated permission/visibility tests; exhaustive live UI completion not tested |
| UUID record persistence | Paper UUID primary key | Floodgate-provided UUID primary key; no prefix heuristics | Both actual client records: stable reconnect UUIDs; stored timestamps unchanged after server restart |
| Configuration reload and module toggles | Server/console administration | Same server behavior | Automated tests and actual console reload/toggles passed; toggling while both clients online not separately tested |
| Admin diagnostics | Permission-gated plain chat or console | Equivalent messages | Actual console aggregate and per-UUID lookups passed; client denial passed |
| Geyser/Floodgate absent | No dependency required | Bedrock transport must be provided separately | Real Paper startup without either provider passed |
| Local Geyser API initialization | Available when installed | Geyser transport initializes | Actual provider initialization and Bedrock connection passed |
| Floodgate API player detection | Java or undetected unless API identifies player | Positively detects Bedrock UUID | Actual Bedrock session reported BEDROCK and LOADED |
| Incompatible optional API | Continues with degraded diagnostics | Platform may be undetected | Automated fake-provider failure tests only |
| Geyser-only online authentication | Standard Java authentication | Requires suitable Java account authentication | Not tested as a separate topology |
| Proxy forwarding | Standard backend records | Requires correct Floodgate forwarding | Not tested |
| Resource packs | None | None | Not applicable |

See [server evidence](docs/validation/milestone-1-server.md) and [milestone review](MILESTONE-1-REVIEW.md). These results do not establish compatibility for future gameplay features; each milestone requires its own tests.

## Detection limits

BEDROCK means an installed API positively identified the UUID. JAVA_OR_UNDETECTED does not guarantee Java Edition. Geyser running only on a proxy is invisible to this backend unless Floodgate forwarding and the backend Floodgate plugin are configured correctly.

Follow the [official Floodgate backend instructions](https://geysermc.org/wiki/floodgate/api/): configure forwarding on the proxy and matching Floodgate key material on trusted backend installations. Wildlands does not read keys, parse name prefixes, derive UUIDs from XUIDs, or contact account services.

Changing authentication/account-linking topology can change the UUID presented by the server. This milestone does not merge accounts or migrate UUIDs automatically.

## Repeating client acceptance

Use a disposable Paper 26.2 installation running Java 25 and the shaded Wildlands JAR.

1. Start without Geyser/Floodgate. Confirm Foundation ready, console help/debug, and normal shutdown.
2. Start with local Geyser/Floodgate. Connect Java 26.2 to the Java port and a supported Bedrock client to Geyser's UDP port.
3. On both clients, run /wildlands status and /wildlands admin debug. Non-operators must see status but receive permission denial for debug.
4. Disconnect/reconnect each client. Verify stable UUIDs, loaded records, and positive Bedrock classification.
5. Stop/restart the server normally. Query both UUIDs from console and confirm first-seen/last-seen data persists.
6. Exercise invalid reload rejection and player-record module toggles on the disposable server.
7. For any additional deployment topology (proxy, Geyser-only auth), test it explicitly before rollout; no live result is claimed here.

Record client/server/provider versions and actual observations. Assets directories assets/java/ and assets/bedrock/ remain reserved for future milestones.
