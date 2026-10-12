package gg.ggwp.wildlands.wildlife;

import gg.ggwp.wildlands.world.Region;

/** Habitat and density decisions do not depend on player edition or entity name. */
public final class HabitatManager {
    private HabitatManager() {}
    public static boolean jaguar(Region region) {
        return region == Region.DENSE_RAINFOREST || region == Region.BAMBOO_FORESTS
                || region == Region.WETLANDS || region == Region.JUNGLE_CLEARINGS;
    }
    public static boolean accepts(Species species, Region region) {
        return switch (species) {
            case JAGUAR -> jaguar(region);
            case CAPYBARA -> region == Region.WETLANDS || region == Region.FLOODPLAINS || region == Region.TROPICAL_RIVERS;
            case TAPIR -> region == Region.DENSE_RAINFOREST || region == Region.BAMBOO_FORESTS || region == Region.RAINFOREST_HIGHLANDS;
            case TOUCAN, MONKEY -> region == Region.DENSE_RAINFOREST || region == Region.BAMBOO_FORESTS || region == Region.WATERFALL_VALLEYS;
            case FROG -> region == Region.WETLANDS || region == Region.FLOODPLAINS || region == Region.TROPICAL_RIVERS || region == Region.WATERFALL_VALLEYS;
            case CAIMAN -> region == Region.WETLANDS || region == Region.FLOODPLAINS || region == Region.TROPICAL_RIVERS;
        };
    }
    public static long cell(int x, int z) {
        return ((long) Math.floorDiv(x, 128) << 32) | (Math.floorDiv(z, 128) & 0xffffffffL);
    }
}
