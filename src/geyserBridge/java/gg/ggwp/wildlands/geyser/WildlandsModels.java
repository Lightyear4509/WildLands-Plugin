package gg.ggwp.wildlands.geyser;

import java.lang.reflect.Method;
import java.util.UUID;
import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.entity.custom.CustomEntityDefinition;
import org.geysermc.geyser.api.entity.property.type.GeyserIntEntityProperty;
import org.geysermc.geyser.api.event.java.ServerSpawnEntityEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineEntitiesEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineEntityPropertiesEvent;
import org.geysermc.geyser.api.event.bedrock.SessionLoadResourcePacksEvent;
import org.geysermc.geyser.api.event.bedrock.SessionDisconnectEvent;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.api.util.Identifier;

/** Separate optional Geyser extension: never ships provider classes inside the Paper plugin. */
public final class WildlandsModels implements Extension {
    private final CustomEntityDefinition jaguar = CustomEntityDefinition.of(Identifier.of("ggwpwildlands:jaguar"));
    private final java.util.Map<String, CustomEntityDefinition> definitions = new java.util.LinkedHashMap<>();
    private final java.util.Map<String, GeyserIntEntityProperty> phaseProperties = new java.util.HashMap<>();
    public WildlandsModels() {
        definitions.put("jaguar", jaguar);
        for (String species : java.util.List.of("capybara", "tapir")) definitions.put(species, CustomEntityDefinition.of(Identifier.of("ggwpwildlands:" + species)));
    }
    private volatile Method phaseLookup;
    private volatile Method speciesLookup;
    private final java.util.Map<GeyserConnection, PackNegotiationMonitor> packs = new java.util.concurrent.ConcurrentHashMap<>();
    private static final UUID PACK = UUID.fromString("abe98b0d-4cf4-423b-9b0f-2029a4732301");
    @Subscribe(postOrder = org.geysermc.event.PostOrder.LAST) public void packs(SessionLoadResourcePacksEvent event) {
        var monitor = new PackNegotiationMonitor(event.resourcePacks().stream().anyMatch(pack -> PACK.equals(pack.uuid())));
        monitor.attach(event.connection()); packs.put(event.connection(), monitor);
    }
    @Subscribe public void disconnect(SessionDisconnectEvent event) { packs.remove(event.connection()); }
    public boolean hasPack(UUID player) {
        var connection = geyserApi().connectionByUuid(player);
        return connection != null && hasPack(connection);
    }
    private boolean hasPack(GeyserConnection connection) { var monitor = packs.get(connection); return monitor != null && monitor.loaded(); }
    public void bind(Class<?> registry) throws ReflectiveOperationException { phaseLookup = registry.getMethod("phase", UUID.class, UUID.class); speciesLookup = registry.getMethod("species", UUID.class, UUID.class); }
    @Subscribe public void define(GeyserDefineEntitiesEvent event) { definitions.values().forEach(event::register); }
    @Subscribe public void properties(GeyserDefineEntityPropertiesEvent event) {
        definitions.forEach((key, definition) -> phaseProperties.put(key, event.registerIntegerProperty(definition.identifier(), Identifier.of("ggwpwildlands:phase"), 0, 4, 0)));
    }
    private int phase(UUID animal, UUID viewer) {
        Method method = phaseLookup; if (method == null) return -1;
        try { return (Integer) method.invoke(null, animal, viewer); }
        catch (ReflectiveOperationException | RuntimeException unavailable) { return -1; }
    }
    private String species(UUID animal, UUID viewer) {
        Method method = speciesLookup; if (method == null) return "";
        try { return (String) method.invoke(null, animal, viewer); }
        catch (ReflectiveOperationException | RuntimeException unavailable) { return ""; }
    }
    @Subscribe public void spawn(ServerSpawnEntityEvent event) {
        if (!event.entityType().is(Identifier.of("minecraft:ocelot")) && !event.entityType().is(Identifier.of("minecraft:pig"))) return;
        String species = species(event.uuid(), event.connection().javaUuid());
        String base = species.equals("jaguar") ? "minecraft:ocelot" : "minecraft:pig";
        if (!definitions.containsKey(species) || !event.entityType().is(Identifier.of(base))) return;
        int phase = phase(event.uuid(), event.connection().javaUuid());
        var property = phaseProperties.get(species);
        if (phase < 0 || property == null || !hasPack(event.connection())) return;
        event.definition(definitions.get(species));
        event.preSpawnConsumer(entity -> {
            entity.override(org.geysermc.geyser.api.entity.data.GeyserEntityDataTypes.VERTICAL_OFFSET, 0f);
            entity.updateProperty(property, phase);
        });
    }
    public void update(UUID animal, int nextPhase) {
        if (nextPhase < 0 || nextPhase > 4) return;
        for (var connection : geyserApi().onlineConnections()) {
            if (phase(animal, connection.javaUuid()) < 0 || !hasPack(connection)) continue;
            var entity = connection.entities().byUuid(animal);
            String species = species(animal, connection.javaUuid());
            var definition = definitions.get(species); var property = phaseProperties.get(species);
            if (entity != null && definition != null && property != null && entity.definition().identifier().equals(definition.identifier())) entity.updateProperty(property, nextPhase);
        }
    }
}
