package gg.ggwp.wildlands.wildlife;

import gg.ggwp.wildlands.storage.WildlifeRecord;
import java.util.*;

/** One world-wide cap includes unloaded animals of every species. */
public final class PopulationRules {
    private PopulationRules() { }
    public static boolean permits(Collection<WildlifeRecord> records, UUID world, Species species, int x, int z,
                                  int worldCap, int speciesCap, int cellCap) {
        long cell = HabitatManager.cell(x, z);
        int total = 0, own = 0, local = 0;
        for (var record : records) if (record.alive() && record.worldUuid().equals(world)) {
            total++;
            if (record.species() == species) {
                own++;
                if (HabitatManager.cell((int) Math.floor(record.homeX()), (int) Math.floor(record.homeZ())) == cell) local++;
            }
        }
        return total < worldCap && own < speciesCap && local < cellCap;
    }
}
