package gg.ggwp.wildlands.ui;

import gg.ggwp.wildlands.seasons.Season;
import java.util.ArrayList;

/** Short plain text fits both editions; detailed values remain available in chat. */
public final class HudFormatter {
    public static final int MAX_LENGTH = 96;
    private HudFormatter() {}
    public static String format(Double hydration, Double temperature, Double wetness, Season season, String route) {
        var fields = new ArrayList<String>();
        if (hydration != null) fields.add("Hydration " + Math.round(hydration) + "%");
        if (temperature != null) fields.add("Temp " + Math.round(temperature) + "°C");
        if (wetness != null) fields.add("Wet " + Math.round(wetness) + "%");
        if (season != null) fields.add("Season " + switch (season) {
            case DRY -> "Dry"; case WET -> "Wet"; case MONSOON -> "Monsoon";
            case TRANSITION_TO_WET -> "To wet"; case TRANSITION_TO_DRY -> "To dry";
        });
        if (route != null && !route.isBlank()) fields.add("Route " + (route.startsWith("In ") ? "other world" : route));
        String text = String.join(" | ", fields);
        return text.length() <= MAX_LENGTH ? text : text.substring(0, MAX_LENGTH - 3) + "...";
    }
}
