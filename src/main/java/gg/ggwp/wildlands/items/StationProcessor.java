package gg.ggwp.wildlands.items;

import gg.ggwp.wildlands.survival.*;
import java.util.*;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/** Plans one operation on clones. A full output inventory never consumes input or fuel. */
public final class StationProcessor {
    private final SurvivalItems items;
    private final WaterItems water;
    public StationProcessor(SurvivalItems items, WaterItems water) { this.items = items; this.water = water; }
    public Optional<ItemStack[]> process(SurvivalItems.Kind kind, ItemStack[] contents, boolean rain, boolean drying) {
        int input = -1, fuel = -1; ItemStack output = null;
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i]; if (item == null || item.getAmount() <= 0) continue;
            if (item.getType() == Material.CHARCOAL && items.kind(item).isEmpty()) fuel = i;
            if (output != null || items.kind(item).isPresent()) continue;
            if (kind == SurvivalItems.Kind.RAIN_COLLECTOR && rain && item.getType() == Material.GLASS_BOTTLE) {
                input = i; output = water.bottle(WaterQuality.CLEAN);
            } else if (kind == SurvivalItems.Kind.DRYING_RACK && drying && SurvivalItems.PRESERVABLE.contains(item.getType()) && !items.preserved(item)) {
                input = i; output = items.preserve(item);
            } else if (kind == SurvivalItems.Kind.BASIC_FILTER || kind == SurvivalItems.Kind.IMPROVED_FILTER) {
                var quality = water.quality(item);
                if (quality.isPresent()) {
                    var filtered = StationRules.filter(quality.get(), kind == SurvivalItems.Kind.IMPROVED_FILTER);
                    if (filtered.isPresent()) { input = i; output = water.bottle(filtered.get()); }
                }
            }
        }
        if (output == null || kind != SurvivalItems.Kind.RAIN_COLLECTOR && fuel < 0) return Optional.empty();
        ItemStack[] next = Arrays.stream(contents).map(item -> item == null ? null : item.clone()).toArray(ItemStack[]::new);
        take(next, input);
        if (kind != SurvivalItems.Kind.RAIN_COLLECTOR) take(next, fuel);
        for (int i = 0; i < next.length; i++) if (next[i] != null && next[i].isSimilar(output)
                && next[i].getAmount() < next[i].getMaxStackSize()) {
            next[i].setAmount(next[i].getAmount() + 1); return Optional.of(next);
        }
        for (int i = 0; i < next.length; i++) if (next[i] == null || next[i].getType() == Material.AIR) {
            next[i] = output; return Optional.of(next);
        }
        return Optional.empty();
    }
    private static void take(ItemStack[] contents, int slot) {
        int remaining = contents[slot].getAmount() - 1;
        if (remaining == 0) contents[slot] = null; else contents[slot].setAmount(remaining);
    }
}
