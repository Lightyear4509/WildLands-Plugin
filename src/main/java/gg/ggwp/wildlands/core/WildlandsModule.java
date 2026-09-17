package gg.ggwp.wildlands.core;

public interface WildlandsModule {
    String id();
    void enable() throws Exception;
    void disable() throws Exception;
}
