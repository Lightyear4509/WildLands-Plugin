package gg.ggwp.wildlands.wildlife.presentation;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class AssetIntegrityTest {
    private JsonObject json(Path path) throws Exception { return JsonParser.parseString(Files.readString(path)).getAsJsonObject(); }
    @Test void separatePacksResolveAllModelsTexturesAndSoundAssets() throws Exception {
        Path java = Path.of("assets/java/pack"), bedrock = Path.of("assets/bedrock/pack");
        var metadata = json(java.resolve("pack.mcmeta")).getAsJsonObject("pack");
        assertEquals(88, metadata.getAsJsonArray("min_format").get(0).getAsInt());
        var manifest = json(bedrock.resolve("manifest.json"));
        assertNotEquals(manifest.getAsJsonObject("header").get("uuid"), manifest.getAsJsonArray("modules").get(0).getAsJsonObject().get("uuid"));
        for (String bone : List.of("body", "head", "front_left", "front_right", "rear_left", "rear_right", "tail")) {
            var item = json(java.resolve("assets/ggwpwildlands/items/jaguar/" + bone + ".json"));
            assertEquals("ggwpwildlands:jaguar/" + bone, item.getAsJsonObject("model").get("model").getAsString());
            assertTrue(json(java.resolve("assets/ggwpwildlands/models/jaguar/" + bone + ".json")).getAsJsonArray("elements").size() > 0);
        }
        assertEquals(64, ImageIO.read(java.resolve("assets/ggwpwildlands/textures/entity/jaguar.png").toFile()).getWidth());
        var geometry = json(bedrock.resolve("models/entity/jaguar.geo.json")).getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        assertEquals(7, geometry.getAsJsonArray("bones").size());
        assertTrue(geometry.getAsJsonArray("bones").get(1).getAsJsonObject().getAsJsonArray("pivot").get(2).getAsDouble() < 0);
        assertTrue(geometry.getAsJsonArray("bones").get(6).getAsJsonObject().getAsJsonArray("pivot").get(2).getAsDouble() > 0);
        var entity = json(bedrock.resolve("entity/jaguar.entity.json")).getAsJsonObject("minecraft:client_entity").getAsJsonObject("description");
        assertEquals("ggwpwildlands:jaguar", entity.get("identifier").getAsString());
        var animations = json(bedrock.resolve("animations/jaguar.animation.json")).getAsJsonObject("animations");
        for (var animation : entity.getAsJsonObject("animations").entrySet()) assertTrue(animations.has(animation.getValue().getAsString()));
        var javaSounds = json(java.resolve("assets/ggwpwildlands/sounds.json"));
        var bedrockSounds = json(bedrock.resolve("sounds/sound_definitions.json")).getAsJsonObject("sound_definitions");
        for (String cue : List.of("idle", "warning", "attack", "hurt", "death")) {
            assertTrue(javaSounds.has("jaguar." + cue)); assertTrue(bedrockSounds.has("ggwpwildlands:jaguar." + cue));
            for (Path path : List.of(java.resolve("assets/ggwpwildlands/sounds/jaguar/" + cue + ".ogg"), bedrock.resolve("sounds/jaguar/" + cue + ".ogg"))) {
                byte[] bytes = Files.readAllBytes(path); assertTrue(bytes.length > 1000);
                assertEquals("OggS", new String(bytes, 0, 4, StandardCharsets.US_ASCII));
            }
        }
    }
}
