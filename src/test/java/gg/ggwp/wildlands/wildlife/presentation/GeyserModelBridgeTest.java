package gg.ggwp.wildlands.wildlife.presentation;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import gg.ggwp.wildlands.geyser.WildlandsModels;
import java.util.UUID;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.entity.custom.CustomEntityDefinition;
import org.geysermc.geyser.api.entity.definition.JavaEntityType;
import org.geysermc.geyser.api.entity.property.type.GeyserIntEntityProperty;
import org.geysermc.geyser.api.event.java.ServerSpawnEntityEvent;
import org.geysermc.geyser.api.event.lifecycle.*;
import org.geysermc.geyser.api.util.Identifier;
import org.junit.jupiter.api.*;

class GeyserModelBridgeTest {
    @AfterEach void cleanup() { PresentationRegistry.clear(); }
    @Test void onlyRegisteredJaguarWithOptedInViewerGetsReplacement() throws Exception {
        var definition = mock(CustomEntityDefinition.class);
        var identifier = mock(Identifier.class);
        when(definition.identifier()).thenReturn(identifier);
        try (var ids = mockStatic(Identifier.class); var factory = mockStatic(CustomEntityDefinition.class)) {
            ids.when(() -> Identifier.of(anyString())).thenReturn(identifier);
            factory.when(() -> CustomEntityDefinition.of(any(Identifier.class))).thenReturn(definition);
            var bridge = new WildlandsModels(); bridge.bind(PresentationRegistry.class);
            var define = mock(GeyserDefineEntitiesEvent.class); bridge.define(define); verify(define).register(definition);
            var properties = mock(GeyserDefineEntityPropertiesEvent.class);
            when(properties.registerIntegerProperty(any(), any(), eq(0), eq(4), eq(0))).thenReturn(mock(GeyserIntEntityProperty.class));
            bridge.properties(properties);
            var spawn = mock(ServerSpawnEntityEvent.class); var type = mock(JavaEntityType.class); var connection = mock(GeyserConnection.class);
            UUID animal = UUID.randomUUID(), viewer = UUID.randomUUID();
            when(spawn.uuid()).thenReturn(animal); when(spawn.entityType()).thenReturn(type); when(type.is(any(Identifier.class))).thenReturn(true);
            when(spawn.connection()).thenReturn(connection); when(connection.javaUuid()).thenReturn(viewer);
            bridge.spawn(spawn); verify(spawn, never()).definition(any());
            PresentationRegistry.publish(animal, 2); bridge.spawn(spawn); verify(spawn, never()).definition(any());
            PresentationRegistry.viewer(viewer, true); bridge.spawn(spawn); verify(spawn, never()).definition(any());
            var monitor = new gg.ggwp.wildlands.geyser.PackNegotiationMonitor(true);
            monitor.attach(PackNegotiationTest.connection()); monitor.observe("HAVE_ALL_PACKS"); monitor.observe("COMPLETED");
            var packs = bridge.getClass().getDeclaredField("packs"); packs.setAccessible(true);
            @SuppressWarnings("unchecked") var cache = (java.util.Map<GeyserConnection, gg.ggwp.wildlands.geyser.PackNegotiationMonitor>) packs.get(bridge);
            cache.put(connection, monitor);
            bridge.spawn(spawn); verify(spawn).definition(definition); verify(spawn).preSpawnConsumer(any());
            reset(spawn); when(spawn.entityType()).thenReturn(type); when(type.is(any(Identifier.class))).thenReturn(false);
            bridge.spawn(spawn); verify(spawn, never()).definition(any());
        }
    }
}
