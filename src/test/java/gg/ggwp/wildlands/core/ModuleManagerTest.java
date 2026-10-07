package gg.ggwp.wildlands.core;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class ModuleManagerTest {
    private static class Module implements WildlandsModule {
        final String id;
        final List<String> events;
        boolean fail;
        Module(String id, List<String> events) { this.id = id; this.events = events; }
        public String id() { return id; }
        public void enable() { events.add("+" + id); if (fail) throw new IllegalStateException("failure"); }
        public void disable() { events.add("-" + id); }
    }
    @Test void transitionsAreIdempotentAndShutdownIsReverseOrder() throws Exception {
        var events = new ArrayList<String>();
        var manager = new ModuleManager();
        manager.register(new Module("a", events)); manager.register(new Module("b", events));
        manager.apply(Map.of("a", true, "b", true));
        manager.apply(Map.of("a", true, "b", true));
        manager.close();
        manager.close();
        assertEquals(List.of("+a", "+b", "-b", "-a"), events);
    }
    @Test void failedStartupCleansPartialModuleAndRollsBackEarlierModules() {
        var events = new ArrayList<String>();
        var manager = new ModuleManager();
        manager.register(new Module("a", events));
        var broken = new Module("b", events); broken.fail = true; manager.register(broken);
        assertThrows(Exception.class, () -> manager.apply(Map.of("a", true, "b", true)));
        assertEquals(List.of("+a", "+b", "-b", "-a"), events);
        assertEquals(Map.of("a", ModuleManager.State.DISABLED, "b", ModuleManager.State.DISABLED), manager.states());
    }
    @Test void validatesRegistryAndDesiredModules() {
        var manager = new ModuleManager();
        manager.register(new Module("a", new ArrayList<>()));
        assertThrows(IllegalArgumentException.class, () -> manager.register(new Module("a", new ArrayList<>())));
        assertThrows(IllegalArgumentException.class, () -> manager.apply(Map.of("unknown", true)));
    }

    @Test void rollbackRestoresConfigurationBeforeRestartingPreviousModules() throws Exception {
        var manager = new ModuleManager();
        var setting = new java.util.concurrent.atomic.AtomicReference<>("previous");
        var observed = new ArrayList<String>();
        manager.register(new WildlandsModule() {
            public String id() { return "existing"; }
            public void enable() { observed.add(setting.get()); }
            public void disable() {}
        });
        var broken = new Module("broken", new ArrayList<>());
        broken.fail = true;
        manager.register(broken);
        manager.apply(Map.of("existing", true, "broken", false));
        setting.set("candidate");
        assertThrows(Exception.class, () -> manager.apply(
                Map.of("existing", false, "broken", true), () -> setting.set("previous")));
        assertEquals(List.of("previous", "previous"), observed);
        assertEquals(ModuleManager.State.ENABLED, manager.states().get("existing"));
        assertEquals(ModuleManager.State.DISABLED, manager.states().get("broken"));
    }
}
