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
    private GeyserIntEntityProperty phaseProperty;
    private volatile Method phaseLookup;
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
    public void bind(Class<?> registry) throws ReflectiveOperationException { phaseLookup = registry.getMethod("phase", UUID.class, UUID.class); }
    @Subscribe public void define(GeyserDefineEntitiesEvent event) { event.register(jaguar); }
    @Subscribe public void properties(GeyserDefineEntityPropertiesEvent event) {
        phaseProperty = event.registerIntegerProperty(jaguar.identifier(), Identifier.of("ggwpwildlands:phase"), 0, 4, 0);
    }
    private int phase(UUID animal, UUID viewer) {
        Method method = phaseLookup; if (method == null) return -1;
        try { return (Integer) method.invoke(null, animal, viewer); }
        catch (ReflectiveOperationException | RuntimeException unavailable) { return -1; }
    }
    @Subscribe public void spawn(ServerSpawnEntityEvent event) {
        if (!event.entityType().is(Identifier.of("minecraft:ocelot"))) return;
        int phase = phase(event.uuid(), event.connection().javaUuid());
        if (phase < 0 || phaseProperty == null || !hasPack(event.connection())) return;
        event.definition(jaguar);
        event.preSpawnConsumer(entity -> {
            entity.override(org.geysermc.geyser.api.entity.data.GeyserEntityDataTypes.VERTICAL_OFFSET, 0f);
            entity.updateProperty(phaseProperty, phase);
        });
    }
    public void update(UUID animal, int nextPhase) {
        if (phaseProperty == null || nextPhase < 0 || nextPhase > 4) return;
        for (var connection : geyserApi().onlineConnections()) {
            if (phase(animal, connection.javaUuid()) < 0 || !hasPack(connection)) continue;
            var entity = connection.entities().byUuid(animal);
            if (entity != null && entity.definition().identifier().equals(jaguar.identifier())) entity.updateProperty(phaseProperty, nextPhase);
        }
    }
}
