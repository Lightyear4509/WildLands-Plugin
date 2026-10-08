package gg.ggwp.wildlands.items;

import gg.ggwp.wildlands.survival.*;
import java.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;

/** Native types provide equivalent controls and inventory persistence on both editions. */
public final class SurvivalItems {
    public enum Kind {
        RAIN_COLLECTOR(Material.BARREL, "Rain Collector", "Add empty bottles. Open sky and rain collect clean water."),
        BASIC_FILTER(Material.BARREL, "Basic Water Filter", "Add fresh water bottles and charcoal. Treat again before drinking."),
        IMPROVED_FILTER(Material.BARREL, "Improved Water Filter", "Add fresh water bottles and charcoal for clean water. Salt cannot be filtered."),
        DRYING_RACK(Material.BARREL, "Food Drying Rack", "Add cooked food and charcoal. Place under a roof near a lit campfire."),
        COOKING_RACK(Material.SMOKER, "Cooking Rack", "Cook food with ordinary smoker fuel."),
        CHARCOAL_KILN(Material.FURNACE, "Charcoal Kiln", "Smelt logs into charcoal using ordinary furnace fuel."),
        IMPROVED_STOVE(Material.SMOKER, "Improved Stove", "Cook food with longer-lasting fuel."),
        WATER_BOILER(Material.FURNACE, "Water Boiler", "Boil fresh water bottles with furnace fuel. Salt stays salty."),
        RAIN_CLOAK(Material.LEATHER_CHESTPLATE, "Rain Cloak", "Wear to reduce rain wetness. Swimming still soaks you."),
        WATERSKIN(Material.POTION, "Waterskin", "Three clean-water drinks. Refill the empty skin with three clean bottles.");
        public final Material material; public final String label, description;
        Kind(Material material, String label, String description) {
            this.material = material; this.label = label; this.description = description;
        }
        public boolean station() { return ordinal() <= WATER_BOILER.ordinal(); }
        public boolean barrel() { return material == Material.BARREL; }
    }
    public static final NamespacedKey KIND = new NamespacedKey("ggwpwildlands", "survival_item");
    public static final NamespacedKey CHARGES = new NamespacedKey("ggwpwildlands", "water_charges");
    private static final NamespacedKey PRESERVED = new NamespacedKey("ggwpwildlands", "preserved_food");
    public static final Set<Material> PRESERVABLE = Set.of(Material.COOKED_BEEF, Material.COOKED_PORKCHOP,
            Material.COOKED_CHICKEN, Material.COOKED_MUTTON, Material.COOKED_RABBIT, Material.COOKED_COD, Material.COOKED_SALMON);
    private final WaterItems water = new WaterItems();
    public ItemStack create(Kind kind) {
        if (kind == Kind.WATERSKIN) return waterskin(3);
        ItemStack item = new ItemStack(kind.material);
        var meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(KIND, PersistentDataType.STRING, kind.name());
        meta.displayName(Component.text(kind.label, NamedTextColor.GREEN));
        meta.lore(List.of(Component.text(kind.description, NamedTextColor.GRAY)));
        if (meta instanceof LeatherArmorMeta armor) armor.setColor(Color.fromRGB(76, 105, 62));
        item.setItemMeta(meta); return item;
    }
    public Optional<Kind> kind(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return Optional.empty();
        String value = item.getItemMeta().getPersistentDataContainer().get(KIND, PersistentDataType.STRING);
        if (value == null) return Optional.empty();
        try {
            Kind kind = Kind.valueOf(value);
            return item.getType() == kind.material || kind == Kind.WATERSKIN && item.getType() == Material.GLASS_BOTTLE
                    ? Optional.of(kind) : Optional.empty();
        } catch (IllegalArgumentException invalid) { return Optional.empty(); }
    }
    public ItemStack waterskin(int charges) {
        if (charges < 0 || charges > 3) throw new IllegalArgumentException("Waterskin capacity is 0..3");
        ItemStack item = charges == 0 ? new ItemStack(Material.GLASS_BOTTLE) : water.bottle(WaterQuality.CLEAN);
        var meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(KIND, PersistentDataType.STRING, Kind.WATERSKIN.name());
        meta.getPersistentDataContainer().set(CHARGES, PersistentDataType.INTEGER, charges);
        meta.setMaxStackSize(1);
        meta.displayName(Component.text("Waterskin (" + charges + "/3)", NamedTextColor.AQUA));
        meta.lore(List.of(Component.text(charges == 0 ? "Refill at a crafting table with three clean water bottles."
                : "Clean water. Drink normally; the skin retains remaining water.", NamedTextColor.GRAY)));
        item.setItemMeta(meta); return item;
    }
    public int charges(ItemStack item) {
        if (kind(item).orElse(null) != Kind.WATERSKIN) return -1;
        Integer value = item.getItemMeta().getPersistentDataContainer().get(CHARGES, PersistentDataType.INTEGER);
        return value == null || value < 0 || value > 3 || (value == 0) != (item.getType() == Material.GLASS_BOTTLE) ? -1 : value;
    }
    public boolean preserved(ItemStack item) {
        return item != null && PRESERVABLE.contains(item.getType()) && item.hasItemMeta()
                && Byte.valueOf((byte) 1).equals(item.getItemMeta().getPersistentDataContainer().get(PRESERVED, PersistentDataType.BYTE));
    }
    public ItemStack preserve(ItemStack input) {
        if (!PRESERVABLE.contains(input.getType())) throw new IllegalArgumentException("Food is not preservable");
        ItemStack result = input.clone(); result.setAmount(1);
        var meta = result.getItemMeta();
        meta.getPersistentDataContainer().set(PRESERVED, PersistentDataType.BYTE, (byte) 1);
        meta.displayName(Component.text("Preserved " + input.getType().name().toLowerCase(Locale.ROOT).replace('_', ' '), NamedTextColor.GOLD));
        meta.lore(List.of(Component.text("Camp-prepared food: a small saturation bonus when eaten.", NamedTextColor.GRAY)));
        result.setItemMeta(meta); return result;
    }
}
