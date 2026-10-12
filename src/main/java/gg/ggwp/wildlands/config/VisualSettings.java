package gg.ggwp.wildlands.config;

import java.net.URI;
import org.bukkit.configuration.ConfigurationSection;

/** Optional presentation; disabled settings never affect authoritative wildlife. */
public record VisualSettings(boolean enabled, String javaUrl, String javaSha1, int maxAnimals, int animationTicks, int viewDistance, boolean animate) {
    public VisualSettings(boolean enabled, String javaUrl, String javaSha1, int maxAnimals) { this(enabled, javaUrl, javaSha1, maxAnimals, 4, 32, true); }
    public static VisualSettings disabled() { return new VisualSettings(false, "", "", 12, 6, 32, true); }
    public VisualSettings {
        if (javaUrl == null || javaSha1 == null || maxAnimals < 1 || maxAnimals > 64) throw new IllegalArgumentException("Invalid wildlife visual settings");
        if (animationTicks < 4 || animationTicks > 20 || viewDistance < 8 || viewDistance > 64) throw new IllegalArgumentException("Invalid wildlife visual work limits");
        if (!javaUrl.isEmpty()) {
            URI uri = URI.create(javaUrl);
            if (!"https".equals(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null)
                throw new IllegalArgumentException("Java pack URL must use HTTPS without credentials");
            if (!javaSha1.matches("[a-fA-F0-9]{40}")) throw new IllegalArgumentException("Java pack SHA-1 must contain 40 hex characters");
        } else if (!javaSha1.isEmpty()) throw new IllegalArgumentException("Pack hash requires a URL");
    }
    public static VisualSettings parse(ConfigurationSection yaml) {
        if (!(yaml.get("schema-version") instanceof Integer version) || version != 1
                || !(yaml.get("enabled") instanceof Boolean enabled)
                || !(yaml.get("java.url") instanceof String url)
                || !(yaml.get("java.sha1") instanceof String hash)
                || !(yaml.get("max-animals") instanceof Integer max)) throw new IllegalArgumentException("Invalid wildlife-visuals.yml");
        Object ticks = yaml.get("animation-ticks", 6), distance = yaml.get("view-distance", 32), animate = yaml.get("animate", true);
        if (!(ticks instanceof Integer t) || !(distance instanceof Integer d) || !(animate instanceof Boolean a)) throw new IllegalArgumentException("Invalid wildlife visual work limit types");
        return new VisualSettings(enabled, url, hash, max, t, d, a);
    }
}
