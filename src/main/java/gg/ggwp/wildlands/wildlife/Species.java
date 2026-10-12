package gg.ggwp.wildlands.wildlife;

import java.util.Locale;

/** Stable persisted identifiers; display names and native appearance are never animal identity. */
public enum Species {
    JAGUAR("Jaguar"), CAPYBARA("Capybara"), TAPIR("Tapir");
    private final String displayName;
    Species(String displayName) { this.displayName = displayName; }
    public String displayName() { return displayName; }
    public String key() { return name().toLowerCase(Locale.ROOT); }
    public boolean passive() { return this != JAGUAR; }
}
