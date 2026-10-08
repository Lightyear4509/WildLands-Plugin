package gg.ggwp.wildlands.items;

import gg.ggwp.wildlands.core.WildlandsPlugin;
import gg.ggwp.wildlands.survival.*;
import java.util.*;
import org.bukkit.*;
import org.bukkit.inventory.*;
import static gg.ggwp.wildlands.items.SurvivalItems.Kind.*;

public final class RecipeManager {
    private final WildlandsPlugin plugin;
    private final SurvivalItems items;
    private final List<NamespacedKey> keys = new ArrayList<>();
    public RecipeManager(WildlandsPlugin plugin, SurvivalItems items) { this.plugin = plugin; this.items = items; }
    private NamespacedKey key(String name) { return new NamespacedKey("ggwpwildlands", name); }
    private void add(Keyed recipe) {
        if (!plugin.getServer().addRecipe((Recipe) recipe)) throw new IllegalStateException("Recipe already registered: " + recipe.getKey());
        keys.add(recipe.getKey());
    }
    private void shaped(SurvivalItems.Kind kind, String[] shape, Object... ingredients) {
        var recipe = new ShapedRecipe(key(kind.name().toLowerCase(Locale.ROOT)), items.create(kind)).shape(shape);
        for (int i = 0; i < ingredients.length; i += 2) {
            char symbol = (char) ingredients[i]; Object ingredient = ingredients[i + 1];
            if (ingredient instanceof Material material) recipe.setIngredient(symbol, material);
            else recipe.setIngredient(symbol, RecipeChoice.exactChoice(List.of(items.create((SurvivalItems.Kind) ingredient))));
        }
        add(recipe);
    }
    public void enable() {
        shaped(RAIN_COLLECTOR, new String[]{"G G", "B B", "SSS"}, 'G', Material.GLASS, 'B', Material.BAMBOO, 'S', Material.OAK_SLAB);
        shaped(BASIC_FILTER, new String[]{" S ", "GCG", "BBB"}, 'S', Material.SAND, 'G', Material.GRAVEL, 'C', Material.CHARCOAL, 'B', Material.BAMBOO);
        shaped(IMPROVED_FILTER, new String[]{"ICI", "SFS", "III"}, 'I', Material.IRON_INGOT, 'C', Material.CHARCOAL, 'S', Material.SAND, 'F', BASIC_FILTER);
        shaped(DRYING_RACK, new String[]{"SSS", "B B", "BBB"}, 'S', Material.STRING, 'B', Material.BAMBOO);
        shaped(COOKING_RACK, new String[]{"III", " C ", "S S"}, 'I', Material.IRON_NUGGET, 'C', Material.CAMPFIRE, 'S', Material.STICK);
        shaped(CHARCOAL_KILN, new String[]{"CCC", "CFC", "CCC"}, 'C', Material.COBBLESTONE, 'F', Material.CAMPFIRE);
        shaped(WATER_BOILER, new String[]{" I ", "IBI", " F "}, 'I', Material.IRON_INGOT, 'B', Material.BUCKET, 'F', Material.FURNACE);
        shaped(IMPROVED_STOVE, new String[]{"BBB", "IRI", "BBB"}, 'B', Material.BRICKS, 'I', Material.IRON_INGOT, 'R', COOKING_RACK);
        shaped(RAIN_CLOAK, new String[]{"L L", "LSL", "LLL"}, 'L', Material.LEATHER, 'S', Material.STRING);
        var clean = new WaterItems().bottle(WaterQuality.CLEAN);
        var skin = new ShapelessRecipe(key("waterskin"), items.waterskin(3));
        skin.addIngredient(3, Material.LEATHER);
        for (int i = 0; i < 3; i++) skin.addIngredient(RecipeChoice.exactChoice(List.of(clean)));
        add(skin);
        var refill = new ShapelessRecipe(key("refill_waterskin"), items.waterskin(3));
        refill.addIngredient(RecipeChoice.exactChoice(List.of(items.waterskin(0))));
        for (int i = 0; i < 3; i++) refill.addIngredient(RecipeChoice.exactChoice(List.of(clean)));
        add(refill);
        add(new FurnaceRecipe(key("boiler_water"), clean, RecipeChoice.exactChoice(new WaterItems().boilingInputs()), 0,
                plugin.configuration().hydration().boilingSeconds() * 20));
    }
    public void disable() {
        for (NamespacedKey key : keys) plugin.getServer().removeRecipe(key);
        keys.clear();
    }
    public List<NamespacedKey> keys() { return List.copyOf(keys); }
}
