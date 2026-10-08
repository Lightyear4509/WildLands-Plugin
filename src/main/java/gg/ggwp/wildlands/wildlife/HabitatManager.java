package gg.ggwp.wildlands.wildlife;

import gg.ggwp.wildlands.world.Region;

/** Habitat and density decisions do not depend on player edition or entity name. */
public final class HabitatManager {
    private HabitatManager() {}
    public static boolean jaguar(Region region) {
        return region == Region.DENSE_RAINFOREST || region == Region.BAMBOO_FORESTS
                || region == Region.WETLANDS || region == Region.JUNGLE_CLEARINGS;
    }
    public static long cell(int x, int z) {
        return ((long) Math.floorDiv(x, 128) << 32) | (Math.floorDiv(z, 128) & 0xffffffffL);
    }
}
