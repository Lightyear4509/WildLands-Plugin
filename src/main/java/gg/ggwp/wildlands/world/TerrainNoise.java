package gg.ggwp.wildlands.world;

/** Stateless seeded lattice noise, independent of generation order and chunk borders. */
public final class TerrainNoise {
    private TerrainNoise() {}
    public static long hash(long seed, long x, long y, long z) {
        long value = seed ^ x * 0x9E3779B97F4A7C15L ^ y * 0xC2B2AE3D27D4EB4FL ^ z * 0x165667B19E3779F9L;
        value = (value ^ value >>> 30) * 0xBF58476D1CE4E5B9L;
        value = (value ^ value >>> 27) * 0x94D049BB133111EBL;
        return value ^ value >>> 31;
    }
    public static double unit(long seed, long x, long y, long z) {
        return (hash(seed, x, y, z) >>> 11) * 0x1.0p-53;
    }
    private static double smooth(double t) { return t * t * t * (t * (t * 6 - 15) + 10); }
    private static double mix(double a, double b, double t) { return a + (b - a) * t; }
    public static double sample(long seed, double x, double y, double z) {
        long ix = (long) Math.floor(x), iy = (long) Math.floor(y), iz = (long) Math.floor(z);
        double fx = smooth(x - ix), fy = smooth(y - iy), fz = smooth(z - iz);
        double low = mix(mix(unit(seed, ix, iy, iz), unit(seed, ix + 1, iy, iz), fx),
                mix(unit(seed, ix, iy + 1, iz), unit(seed, ix + 1, iy + 1, iz), fx), fy);
        double high = mix(mix(unit(seed, ix, iy, iz + 1), unit(seed, ix + 1, iy, iz + 1), fx),
                mix(unit(seed, ix, iy + 1, iz + 1), unit(seed, ix + 1, iy + 1, iz + 1), fx), fy);
        return mix(low, high, fz) * 2 - 1;
    }
    public static double surface(long seed, int x, int z, double scale) {
        return sample(seed, x / scale, 0, z / scale);
    }
}
