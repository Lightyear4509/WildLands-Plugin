package gg.ggwp.wildlands.crossplay;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.plugin.*;
import org.junit.jupiter.api.Test;

class OptionalApiBridgeTest {
    public static class FakeApi {
        static boolean fail;
        public static FakeApi getInstance() { return new FakeApi(); }
        public boolean isPlayer(UUID uuid) { if (fail) throw new IllegalStateException("incompatible"); return true; }
    }
    private PluginManager manager(Plugin plugin) {
        return (PluginManager) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{PluginManager.class},
                (proxy, method, args) -> method.getName().equals("getPlugin") ? plugin : null);
    }
    private Plugin provider() {
        return (Plugin) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Plugin.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("isEnabled")) return true;
                    if (method.getName().equals("getPluginMeta"))
                        return new PluginDescriptionFile("test", "1", "test.Main");
                    return null;
                });
    }
    private OptionalApiBridge bridge(String api) {
        return new OptionalApiBridge("test", api, "getInstance", "isPlayer", true, Logger.getAnonymousLogger()) {};
    }
    @Test void absentProviderDoesNotLoadApiClasses() {
        var bridge = bridge("not.installed.Api");
        bridge.refresh(manager(null));
        assertEquals("ABSENT", bridge.status());
        assertFalse(bridge.contains(UUID.randomUUID()));
    }
    @Test void incompatibleProviderDegradesSafely() {
        var bridge = bridge("not.installed.Api");
        bridge.refresh(manager(provider()));
        assertTrue(bridge.status().startsWith("UNAVAILABLE"));
        assertFalse(bridge.contains(UUID.randomUUID()));
    }
    @Test void detectsPlayersAndHandlesRuntimeApiFailure() {
        FakeApi.fail = false;
        var bridge = bridge(FakeApi.class.getName());
        bridge.refresh(manager(provider()));
        assertTrue(bridge.contains(UUID.randomUUID()));
        FakeApi.fail = true;
        assertFalse(bridge.contains(UUID.randomUUID()));
        assertTrue(bridge.status().startsWith("UNAVAILABLE"));
        FakeApi.fail = false;
        bridge.refresh(manager(provider()));
        assertTrue(bridge.contains(UUID.randomUUID()));
    }
}
