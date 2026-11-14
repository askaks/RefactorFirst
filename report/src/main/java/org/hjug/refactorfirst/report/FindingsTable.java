package org.hjug.refactorfirst.report;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.hjug.cbc.RankedDisharmony;
import org.hjug.graphbuilder.metrics.DisharmonyLimits;
import org.hjug.graphbuilder.metrics.DisharmonyLimits.Limit;
import org.hjug.graphbuilder.metrics.DisharmonyMetric;
import org.hjug.refactorfirst.report.ReportPalette.Family;

/**
 * The parts of a findings table that say why a row is there: the rule in one sentence above the table,
 * one column per number under a heading in the finding's colour, and one line under the table with what
 * counts as normal for each number.
 *
 * <p>Every limit is read from {@link DisharmonyLimits}; none is written here. Nothing new is measured:
 * these are the numbers the rule already used.
 */
final class FindingsTable {

    private FindingsTable() {}

    private static final String[] COUNT_WORDS = {"", "", "both", "all three", "all four", "all five", "all six"};

    /** How a finding's numbers combine, in one sentence, or nothing when the type has no limits to combine. */
    static String ruleLine(String disharmonyType) {
        return DisharmonyLimits.rule(disharmonyType)
                .map(rule -> "<p style=\"text-align:center;font-size:0.95em;margin:4px auto 8px;max-width:900px;\">"
                        + emphasise(NumbersGuide.escape(rule)) + "</p>\n")
                .orElse("");
    }

    private static String emphasise(String sentence) {
        for (String word : List.of("all three", "all four", "both hold")) {
            sentence = sentence.replace(word, "<strong>" + word + "</strong>");
        }
        return sentence;
    }

    /** The heading over the group of number columns: "What makes it a God Class", in the finding's colour. */
    static String groupHeading(String anchorId, String disharmonyType, int columns) {
        Family family = ReportPalette.forFinding(anchorId);
        return "<th colspan=\"" + columns + "\" style=\"border-top:7px solid " + family.hex() + ";background:"
                + tint(family.hex()) + ";color:" + Family.INK.hex() + ";\">What makes it " + named(disharmonyType)
                + "</th>\n";
    }

    /** One heading cell per number, named in plain words. The limit is not here; it is under the table. */
    static String metricHeadings(String anchorId, List<DisharmonyMetric> metrics) {
        String background = tint(ReportPalette.forFinding(anchorId).hex());
        StringBuilder sb = new StringBuilder();
        for (DisharmonyMetric m : metrics) {
            sb.append("<th style=\"background:")
                    .append(background)
                    .append(";color:")
                    .append(Family.INK.hex())
                    .append(";white-space:normal;min-width:95px;max-width:170px;\">")
                    .append(NumbersGuide.escape(capitalise(DisharmonyLimits.plainName(m.getName()))))
                    .append("</th>\n");
        }
        return sb.toString();
    }

    /** One cell per number: the value and, where being past the limit is bad in itself, how far past. */
    static String metricCells(RankedDisharmony rd) {
        List<DisharmonyMetric> metrics = rd.getRankedMetrics();
        if (metrics == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (DisharmonyMetric m : metrics) {
            sb.append("<td align=\"right\" style=\"white-space:nowrap;\"><strong>")
                    .append(BulletGraph.number(m.getValue()))
                    .append("</strong>");
            DisharmonyLimits.of(rd.getDisharmonyType(), m.getName())
                    .filter(limit -> DisharmonyLimits.crossingIsBad(rd.getDisharmonyType(), m.getName()))
                    .ifPresent(limit -> sb.append("<br/><span style=\"font-size:0.85em;color:")
                            .append(Family.FIX_FIRST.hex())
                            .append(";\">")
                            .append(pastLimit(
                                    m.getValue(), limit.value(), DisharmonyLimits.higherIsBetter(m.getName())))
                            .append("</span>"));
            sb.append("</td>\n");
        }
        return sb.toString();
    }

    /** How far past the limit, short enough for a table cell: "6.4&times; the limit", "just over", "0.16 short". */
    static String pastLimit(double value, double limit, boolean higherIsBetter) {
        if (higherIsBetter) {
            double shortBy = Math.round((limit - value) * 100) / 100.0;
            return shortBy <= 0 ? "at the limit" : BulletGraph.number(shortBy) + " short";
        }
        if (value == limit) {
            return "at the limit";
        }
        double times = limit == 0 ? 0 : value / limit;
        return times >= 1.5 ? String.format(Locale.US, "%.1f", times) + "&times; the limit" : "just over";
    }

    /**
     * The line under a table: each number's limit once, with the number's name as the link to its entry in
     * "What the numbers mean". A limit that only describes the finding reads "counts when ...", not "normal".
     */
    static String normalLine(String anchorId, RankedDisharmony sample) {
        List<DisharmonyMetric> metrics = sample.getRankedMetrics();
        if (metrics == null) {
            return "";
        }
        String type = sample.getDisharmonyType();
        int judged = 0;
        int higher = 0;
        for (DisharmonyMetric m : metrics) {
            if (DisharmonyLimits.crossingIsBad(type, m.getName())) {
                judged++;
                if (DisharmonyLimits.higherIsBetter(m.getName())) {
                    higher++;
                }
            }
        }
        boolean oneDirection = judged > 0 && (higher == 0 || higher == judged);
        List<String> parts = new ArrayList<>();
        for (DisharmonyMetric m : metrics) {
            Limit limit = DisharmonyLimits.of(type, m.getName()).orElse(null);
            if (limit == null) {
                continue;
            }
            String name = NumbersGuide.nameLink(m.getName());
            if (!DisharmonyLimits.crossingIsBad(type, m.getName())) {
                parts.add(name + ": counts when " + limit.describe());
            } else {
                boolean sayHigher = !oneDirection && DisharmonyLimits.higherIsBetter(m.getName());
                parts.add(name + " " + limit.normalText() + (sayHigher ? ", higher is better" : ""));
            }
        }
        if (parts.isEmpty()) {
            return "";
        }
        String direction = "";
        if (oneDirection) {
            String which = higher == 0 ? "lower" : "higher";
            direction = judged == 1 && parts.size() == 1
                    ? "; " + which + " is better"
                    : judged == parts.size() && judged < COUNT_WORDS.length
                            ? "; " + which + " is better for " + COUNT_WORDS[judged]
                            : "; " + which + " is better";
        }
        return "<div style=\"width:max-content;max-width:95%;margin:8px auto 0;padding:4px 12px;text-align:left;"
                + "font-size:0.9em;border-left:4px solid "
                + ReportPalette.forFinding(anchorId).hex()
                + ";background:" + tint(ReportPalette.forFinding(anchorId).hex()) + ";\"><strong>What counts as normal"
                + "</strong> (the tool's limits, taken from the book" + direction + "): "
                + String.join(" &#183; ", parts) + "</div>\n";
    }

    /** One link under every table to how Priority and the two ranks are read. */
    static String priorityLine() {
        return "<div style=\"text-align:center;font-size:0.9em;margin:4px 0 0;\"><a href=\"#"
                + NumbersGuide.anchor(NumbersGuide.PRIORITY)
                + "\">How to read Priority, Change Proneness Rank and Effort Rank</a></div>\n";
    }

    /** The colour at about one part in seven on white: enough to group columns without shouting. */
    static String tint(String hex) {
        int r = Integer.parseInt(hex.substring(1, 3), 16);
        int g = Integer.parseInt(hex.substring(3, 5), 16);
        int b = Integer.parseInt(hex.substring(5, 7), 16);
        return String.format("#%02x%02x%02x", pale(r), pale(g), pale(b));
    }

    private static int pale(int channel) {
        return (int) Math.round(255 - (255 - channel) * 0.15);
    }

    /** "a God Class", "a Brain Method", but "Feature Envy" and "Intensive Coupling" with no article. */
    private static String named(String disharmonyType) {
        String name = NumbersGuide.escape(disharmonyType);
        return name.endsWith("Class") || name.endsWith("Method") ? "a " + name : name;
    }

    private static String capitalise(String text) {
        return text.isEmpty() ? text : text.substring(0, 1).toUpperCase(Locale.ROOT) + text.substring(1);
    }
}
