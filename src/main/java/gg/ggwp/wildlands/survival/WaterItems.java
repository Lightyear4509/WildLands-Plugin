package gg.ggwp.wildlands.survival;

import java.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionType;

/** Uses vanilla water bottles; names are presentation, never the authority for quality. */
public final class WaterItems {
    private final NamespacedKey qualityKey = new NamespacedKey("ggwpwildlands", "water_quality");

    public ItemStack bottle(WaterQuality quality) {
        ItemStack item = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        meta.setBasePotionType(PotionType.WATER);
        meta.getPersistentDataContainer().set(qualityKey, PersistentDataType.STRING, quality.name());
        String label = quality.name().charAt(0) + quality.name().substring(1).toLowerCase(Locale.ROOT);
        meta.displayName(Component.text(label + " Water", quality == WaterQuality.CLEAN ? NamedTextColor.AQUA : NamedTextColor.YELLOW));
        meta.lore(List.of(Component.text(quality == WaterQuality.SALT ? "Salt water: boiling does not remove salt."
                : quality == WaterQuality.CLEAN ? "Safe to drink." : "Untreated water carries an illness risk. Boil at a campfire.", NamedTextColor.GRAY)));
        item.setItemMeta(meta);
        return item;
    }

    public Optional<WaterQuality> quality(ItemStack item) {
        if (item == null || item.getType() != Material.POTION || !(item.getItemMeta() instanceof PotionMeta meta)
                || meta.getBasePotionType() != PotionType.WATER || meta.hasCustomEffects()) return Optional.empty();
        String stored = meta.getPersistentDataContainer().get(qualityKey, PersistentDataType.STRING);
        // Vanilla or unknown-provenance bottles are usable, but never assumed clean.
        if (stored == null) return Optional.of(WaterQuality.QUESTIONABLE);
        try { return Optional.of(WaterQuality.valueOf(stored)); }
        catch (IllegalArgumentException unknown) { return Optional.of(WaterQuality.QUESTIONABLE); }
    }

    public List<ItemStack> boilingInputs() {
        List<ItemStack> inputs = new ArrayList<>();
        for (WaterQuality quality : WaterQuality.values()) if (quality.canBoil()) inputs.add(bottle(quality));
        ItemStack plain = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) plain.getItemMeta();
        meta.setBasePotionType(PotionType.WATER);
        plain.setItemMeta(meta);
        inputs.add(plain);
        return List.copyOf(inputs);
    }
}
