package gg.ggwp.wildlands.crossplay;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

// Isolates optional APIs: no provider classes are linked when the provider is absent.
abstract class OptionalApiBridge {
    private final String pluginName, apiClass, accessor, lookup;
    private final boolean booleanResult;
    private Object api;
    private Method query;
    private String status = "ABSENT";
    private final Logger logger;

    OptionalApiBridge(String pluginName, String apiClass, String accessor, String lookup,
                      boolean booleanResult, Logger logger) {
        this.pluginName = pluginName;
        this.apiClass = apiClass;
        this.accessor = accessor;
        this.lookup = lookup;
        this.booleanResult = booleanResult;
        this.logger = logger;
    }
    final void refresh(PluginManager manager) {
        api = null;
        query = null;
        Plugin plugin = manager.getPlugin(pluginName);
        if (plugin == null || !plugin.isEnabled()) { status = "ABSENT"; return; }
        try {
            Class<?> type = Class.forName(apiClass, true, plugin.getClass().getClassLoader());
            api = type.getMethod(accessor).invoke(null);
            if (api == null) throw new IllegalStateException("API not initialized");
            query = type.getMethod(lookup, UUID.class);
            status = "AVAILABLE (" + plugin.getPluginMeta().getVersion() + ")";
        } catch (ReflectiveOperationException | LinkageError | RuntimeException failure) { fail(failure); }
    }
    final boolean contains(UUID uuid) {
        if (api == null || query == null) return false;
        try {
            Object result = query.invoke(api, uuid);
            return booleanResult ? Boolean.TRUE.equals(result) : result != null;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException failure) {
            fail(failure);
            return false;
        }
    }
    private void fail(Throwable failure) {
        api = null;
        query = null;
        status = "UNAVAILABLE (API error)";
        logger.warning(pluginName + " integration unavailable: " + failure.getClass().getSimpleName()
                + ". Wildlands will continue; player platform may be undetected.");
    }
    public final String status() { return status; }
}
