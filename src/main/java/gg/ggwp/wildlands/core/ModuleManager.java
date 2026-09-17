package gg.ggwp.wildlands.core;

import java.util.*;

public final class ModuleManager implements AutoCloseable {
    public enum State { DISABLED, ENABLED, FAILED }
    private final Map<String, WildlandsModule> modules = new LinkedHashMap<>();
    private final Map<String, State> states = new LinkedHashMap<>();

    public void register(WildlandsModule module) {
        if (modules.putIfAbsent(module.id(), module) != null)
            throw new IllegalArgumentException("Duplicate module " + module.id());
        states.put(module.id(), State.DISABLED);
    }

    public void apply(Map<String, Boolean> desired) throws Exception {
        if (!modules.keySet().equals(desired.keySet())) throw new IllegalArgumentException("Module configuration mismatch");
        var previous = new LinkedHashMap<>(states);
        try {
            for (String id : modules.keySet()) transition(id, desired.get(id));
        } catch (Exception failure) {
            var ids = new ArrayList<>(modules.keySet());
            Collections.reverse(ids);
            for (String id : ids) {
                try { transition(id, previous.get(id) == State.ENABLED); }
                catch (Exception rollback) { failure.addSuppressed(rollback); }
            }
            throw failure;
        }
    }

    private void transition(String id, boolean enabled) throws Exception {
        State target = enabled ? State.ENABLED : State.DISABLED;
        if (states.get(id) == target) return;
        WildlandsModule module = modules.get(id);
        try {
            if (enabled) {
                // A failed lifecycle may have allocated partial resources.
                if (states.get(id) == State.FAILED) module.disable();
                module.enable();
            } else module.disable();
            states.put(id, target);
        } catch (Exception failure) {
            states.put(id, State.FAILED);
            throw failure;
        }
    }

    public Map<String, State> states() { return Collections.unmodifiableMap(new LinkedHashMap<>(states)); }

    @Override public void close() throws Exception {
        Exception error = null;
        var ids = new ArrayList<>(modules.keySet());
        Collections.reverse(ids);
        for (String id : ids) {
            try { transition(id, false); }
            catch (Exception failure) {
                if (error == null) error = failure; else error.addSuppressed(failure);
            }
        }
        if (error != null) throw error;
    }
}
