package gg.ggwp.wildlands.wildlife.presentation;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Cross-classloader bridge reads immutable values only. Never exposes Bukkit objects off-thread. */
public final class PresentationRegistry {
    private static final Map<UUID, Integer> JAGUARS = new ConcurrentHashMap<>();
    private static final Set<UUID> BEDROCK_VIEWERS = ConcurrentHashMap.newKeySet();
    private PresentationRegistry() { }
    public static int phase(UUID animal, UUID viewer) {
        return BEDROCK_VIEWERS.contains(viewer) ? JAGUARS.getOrDefault(animal, -1) : -1;
    }
    static void publish(UUID animal, int phase) { JAGUARS.put(animal, phase); }
    static void remove(UUID animal) { JAGUARS.remove(animal); }
    static void viewer(UUID viewer, boolean enabled) { if (enabled) BEDROCK_VIEWERS.add(viewer); else BEDROCK_VIEWERS.remove(viewer); }
    static void clear() { JAGUARS.clear(); BEDROCK_VIEWERS.clear(); }
}
