package org.hjug.refactorfirst.report;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.hjug.refactorfirst.report.ReportPalette.Family;

/**
 * One value drawn against its limit, as a bullet graph: grey bands for the named levels, a thin
 * bar for the value in the finding's colour, and a dark line at the limit.
 *
 * <p>The form is Stephen Few's bullet graph, with the bar in the finding's colour so it ties to
 * the tag above it. Each graph is scaled to its own value, so lengths are not comparable between
 * graphs; the "times the limit" text beside each one is.
 *
 * <p>Depends on the palette only, so it can never become part of a dependency cycle.
 */
final class BulletGraph {

    private BulletGraph() {}

    /** A named point on the scale. */
    record Level(String name, double value) {}

    private static final String[] GREYS = {"#f3f3f3", "#e2e2e2", "#cfcfcf"};
    private static final String BEYOND = "#bdbdbd";

    /**
     * @param lowerIsWorse true for ratios such as cohesion, where the finding side is below the limit
     * @param levels named points to label under the graph, lowest first
     */
    static String render(double value, double limit, boolean lowerIsWorse, List<Level> levels, Family family) {
        double max = lowerIsWorse ? Math.max(1, limit * 1.3) : Math.max(value * 1.08, limit * 1.3);
        StringBuilder sb =
                new StringBuilder("<div class=\"rf-bullet\" style=\"position:relative;height:20px;margin:4px 0 "
                        + (levels.isEmpty() ? "10px" : "30px") + ";\">");
        if (lowerIsWorse) {
            band(sb, 0, limit, max, BEYOND);
            band(sb, limit, max, max, GREYS[0]);
        } else {
            List<Double> edges = new ArrayList<>(List.of(0.0));
            levels.stream().map(Level::value).filter(v -> v > 0 && v < limit).forEach(edges::add);
            edges.add(limit);
            int bands = edges.size() - 1;
            for (int i = 0; i < bands; i++) {
                band(sb, edges.get(i), edges.get(i + 1), max, GREYS[Math.min(i, 2)]);
            }
            band(sb, limit, max, max, BEYOND);
        }
        sb.append("<div style=\"position:absolute;left:0;top:6px;height:8px;width:")
                .append(pct(value, max))
                .append(";background:")
                .append(family.hex())
                .append(";\"></div>");
        sb.append("<div style=\"position:absolute;top:-3px;width:3px;height:26px;background:")
                .append(Family.INK.hex())
                .append(";left:")
                .append(pct(limit, max))
                .append(";\"></div>");
        for (Level level : levels) {
            if (level.value() > max) {
                continue;
            }
            sb.append("<div style=\"position:absolute;top:26px;transform:translateX(-50%);font-size:10px;")
                    .append("line-height:1.15;text-align:center;white-space:nowrap;color:#666;left:")
                    .append(pct(level.value(), max))
                    .append(";\">")
                    .append(level.name())
                    .append("<br/>")
                    .append(number(level.value()))
                    .append("</div>");
        }
        return sb.append("</div>").toString();
    }

    /**
     * How far past the limit, in words: "2.9&times; the limit", "just over the limit of 5", "at the limit of 5",
     * or for a measure where lower is worse "0.01 short of the limit of 0.33".
     */
    static String overText(double value, double limit, boolean lowerIsWorse, String limitText) {
        if (lowerIsWorse) {
            double shortBy = Math.round((limit - value) * 100) / 100.0;
            return shortBy <= 0
                    ? "at the limit of " + limitText
                    : "<strong>" + number(shortBy) + "</strong> short of the limit of " + limitText;
        }
        if (value == limit) {
            return "at the limit of " + limitText;
        }
        double times = limit == 0 ? 0 : value / limit;
        return times >= 1.5
                ? "<strong>" + String.format(Locale.US, "%.1f", times) + "&times;</strong> the limit"
                : "just over the limit of " + limitText;
    }

    private static void band(StringBuilder sb, double from, double to, double max, String colour) {
        double left = Math.min(from / max, 1) * 100;
        double right = Math.min(to / max, 1) * 100;
        sb.append("<div style=\"position:absolute;top:0;height:20px;left:")
                .append(String.format(Locale.US, "%.1f%%", left))
                .append(";width:")
                .append(String.format(Locale.US, "%.1f%%", right - left))
                .append(";background:")
                .append(colour)
                .append(";\"></div>");
    }

    private static String pct(double value, double max) {
        return String.format(Locale.US, "%.1f%%", Math.min(value / max, 1) * 100);
    }

    /** Whole numbers as they are; ratios to two places at most. */
    static String number(double value) {
        if (value == Math.floor(value)) {
            return String.valueOf((long) value);
        }
        String text = String.format(Locale.US, "%.2f", value);
        return text.endsWith("0") ? text.substring(0, text.length() - 1) : text;
    }
}
