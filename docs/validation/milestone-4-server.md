# Milestone 4 console validation

October 7, 2026: disposable server .runtime/milestone-1, Paper 26.2 build 121, Java 25.0.4.1, Wildlands 0.4.0, Geyser 2.11.3 build 1245 and Floodgate 2.2.5 build 140. The stopped database, configuration and previous plugin JAR were backed up under .runtime/milestone-4-backup before upgrading.

At 19:29:34 server time, Wildlands reported Foundation ready with both providers AVAILABLE. `/season info world` reported DRY, day 1/14. `/wildlands admin season monsoon world` succeeded, and the next inspection reported MONSOON, day 1/5. All seven registered modules were ENABLED. `/wildlands reload` succeeded. `/season info world_nether` reported disabled or unmanaged. The server stopped normally at 19:30:04, saving worlds and closing Wildlands.

The server restarted normally at 19:31:40; `/season info world` still reported MONSOON, day 1/5. This confirms the admin-set season survived the real shutdown/restart. The second session was stopped normally after inspection.

No live clients were required or tested for this milestone. Server startup emitted pre-existing host performance-counter warnings and a spark statistics timeout; neither prevented startup. Crossplay presentation and multiplayer behavior remain deferred to final combined acceptance.
