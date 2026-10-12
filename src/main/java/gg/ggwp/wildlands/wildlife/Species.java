package gg.ggwp.wildlands.wildlife;

import java.util.Locale;

/** Stable persisted identifiers; display names and native appearance are never animal identity. */
public enum Species {
    JAGUAR("Jaguar"), CAPYBARA("Capybara"), TAPIR("Tapir"),
    TOUCAN("Toucan"), MONKEY("Monkey"), FROG("Frog"), CAIMAN("Caiman");
    private final String displayName;
    Species(String displayName) { this.displayName = displayName; }
    public String displayName() { return displayName; }
    public String key() { return name().toLowerCase(Locale.ROOT); }
    public boolean passive() { return this != JAGUAR && this != CAIMAN; }
    public boolean aquatic() { return this == CAPYBARA || this == FROG || this == CAIMAN; }
    public boolean grouped() { return this == CAPYBARA || this == MONKEY; }
    public String nativeId() { return switch (this) {
        case JAGUAR -> "minecraft:ocelot"; case CAPYBARA, TAPIR -> "minecraft:pig";
        case TOUCAN -> "minecraft:parrot"; case MONKEY -> "minecraft:cat";
        case FROG -> "minecraft:frog"; case CAIMAN -> "minecraft:turtle";
    }; }
    public Class<? extends org.bukkit.entity.Mob> nativeClass() { return switch (this) {
        case JAGUAR -> org.bukkit.entity.Ocelot.class; case CAPYBARA, TAPIR -> org.bukkit.entity.Pig.class;
        case TOUCAN -> org.bukkit.entity.Parrot.class; case MONKEY -> org.bukkit.entity.Cat.class;
        case FROG -> org.bukkit.entity.Frog.class; case CAIMAN -> org.bukkit.entity.Turtle.class;
    }; }
}
