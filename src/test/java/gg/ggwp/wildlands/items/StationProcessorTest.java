package gg.ggwp.wildlands.items;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import gg.ggwp.wildlands.survival.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.*;

class StationProcessorTest {
    SurvivalItems items; WaterItems water; StationProcessor processor;
    final Map<ItemStack, String> labels = new IdentityHashMap<>();
    @BeforeEach void setup() {
        items = mock(SurvivalItems.class); water = mock(WaterItems.class); processor = new StationProcessor(items, water);
        when(items.kind(any())).thenReturn(Optional.empty()); when(water.quality(any())).thenReturn(Optional.empty());
    }
    ItemStack stack(Material material, int count, String label) {
        var stack = mock(ItemStack.class); var amount = new AtomicInteger(count); labels.put(stack, label);
        when(stack.getType()).thenReturn(material); when(stack.getAmount()).thenAnswer(call -> amount.get());
        doAnswer(call -> { amount.set(call.getArgument(0)); return null; }).when(stack).setAmount(anyInt());
        when(stack.getMaxStackSize()).thenReturn(material == Material.POTION ? 1 : 64);
        when(stack.clone()).thenAnswer(call -> stack(material, amount.get(), label));
        when(stack.isSimilar(any())).thenAnswer(call -> label.equals(labels.get(call.getArgument(0)))); return stack;
    }
    @Test void collectorNeedsRainAndConservesExactlyOneBottleWithoutMutatingInput() {
        var bottles = stack(Material.GLASS_BOTTLE, 2, "empty"); var clean = stack(Material.POTION, 1, "clean");
        when(water.bottle(WaterQuality.CLEAN)).thenReturn(clean);
        ItemStack[] source = {bottles, null};
        assertTrue(processor.process(SurvivalItems.Kind.RAIN_COLLECTOR, source, false, false).isEmpty());
        var next = processor.process(SurvivalItems.Kind.RAIN_COLLECTOR, source, true, false).orElseThrow();
        assertEquals(1, next[0].getAmount()); assertSame(clean, next[1]); assertEquals(2, bottles.getAmount()); assertNull(source[1]);
    }
    @Test void fullOutputDoesNotConsumeFuelOrInput() {
        var food = stack(Material.COOKED_BEEF, 2, "beef"); var charcoal = stack(Material.CHARCOAL, 2, "charcoal");
        var preserved = stack(Material.COOKED_BEEF, 1, "preserved"); when(items.preserve(food)).thenReturn(preserved);
        assertTrue(processor.process(SurvivalItems.Kind.DRYING_RACK, new ItemStack[]{food, charcoal}, false, true).isEmpty());
        assertEquals(2, food.getAmount()); assertEquals(2, charcoal.getAmount());
    }
    @Test void filterUsesOneCharcoalAndRejectsSaltAndWaterskins() {
        var bottle = stack(Material.POTION, 1, "dirty"); var charcoal = stack(Material.CHARCOAL, 2, "fuel");
        var clean = stack(Material.POTION, 1, "clean"); when(water.bottle(WaterQuality.CLEAN)).thenReturn(clean);
        when(water.quality(bottle)).thenReturn(Optional.of(WaterQuality.SALT));
        assertTrue(processor.process(SurvivalItems.Kind.IMPROVED_FILTER, new ItemStack[]{bottle, charcoal}, false, false).isEmpty());
        when(water.quality(bottle)).thenReturn(Optional.of(WaterQuality.CONTAMINATED));
        var next = processor.process(SurvivalItems.Kind.IMPROVED_FILTER, new ItemStack[]{bottle, charcoal}, false, false).orElseThrow();
        assertSame(clean, next[0]); assertEquals(1, next[1].getAmount()); assertEquals(2, charcoal.getAmount());
        when(items.kind(bottle)).thenReturn(Optional.of(SurvivalItems.Kind.WATERSKIN));
        assertTrue(processor.process(SurvivalItems.Kind.IMPROVED_FILTER, new ItemStack[]{bottle, charcoal}, false, false).isEmpty());
    }
    @Test void rackNeedsHeatAndDryProtectionAndNeverProcessesAlreadyPreservedFood() {
        var food = stack(Material.COOKED_COD, 1, "cod"); var fuel = stack(Material.CHARCOAL, 1, "fuel");
        var preserved = stack(Material.COOKED_COD, 1, "preserved"); when(items.preserve(food)).thenReturn(preserved);
        assertTrue(processor.process(SurvivalItems.Kind.DRYING_RACK, new ItemStack[]{food, fuel}, false, false).isEmpty());
        var next = processor.process(SurvivalItems.Kind.DRYING_RACK, new ItemStack[]{food, fuel}, false, true).orElseThrow();
        assertSame(preserved, next[0]); assertNull(next[1]);
        when(items.preserved(food)).thenReturn(true);
        assertTrue(processor.process(SurvivalItems.Kind.DRYING_RACK, new ItemStack[]{food, fuel}, false, true).isEmpty());
    }
}
