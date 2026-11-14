package org.hjug.refactorfirst.report;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hjug.graphbuilder.VertexInfo;
import org.hjug.refactorfirst.report.ReportPalette.Family;
import org.jgrapht.Graph;
import org.jgrapht.graph.AsSubgraph;
import org.jgrapht.graph.DefaultWeightedEdge;

/**
 * Says what the report's colours and marks mean, where the reader meets them.
 *
 * <p>Everything here is built from {@link ReportPalette}, so the key cannot drift from what is drawn.
 * Plain HTML with inline styles: it works in the simple report and needs no script.
 *
 * <p>It depends on the palette and nothing else in the report, on purpose: the report classes use the key,
 * so a key that used them back would add itself to a dependency cycle, the very thing the key explains.
 */
final class ReportKey {

    /** How many names of a large cycle are listed before "and N more". */
    static final int MAX_NAMES = 8;

    /** How many cycles the reason table lists before pointing at the full table. */
    static final int MAX_ROWS = 10;

    private static final String BOX = "max-width:900px;margin:10px auto;padding:6px 12px;border:1px solid #d9d2c3;"
            + "border-radius:8px;text-align:left;font-size:0.9em;line-height:1.9;";

    private ReportKey() {}

    /** The key for the class, cycle and package maps: one entry for each thing the maps draw. */
    static String forMaps() {
        String cycle = Family.CYCLE.hex();
        String outline = swatch("background:" + Family.NEUTRAL.hex() + ";border:2px solid " + cycle
                + ";border-radius:50%;width:1.5em;height:0.9em;");
        String line = swatch("border-top:3px solid " + cycle + ";width:1.8em;height:0;vertical-align:middle;");
        String pale =
                swatch("background:" + Family.CYCLE_PALE.hex() + ";border:1px solid #999;width:1.5em;height:0.9em;");
        String plain = swatch("background:" + Family.NEUTRAL.hex()
                + ";border:1px solid #999;border-radius:50%;width:1.5em;height:0.9em;");
        String mark = "<span style=\"color:" + cycle + ";font-weight:bold;\">" + Family.CYCLE.mark() + "</span> ";
        return "<div class=\"rf-key\" style=\"" + BOX + "\"><strong>Key</strong> "
                + entry(outline + mark + "outlined: a class or package <strong>in a cycle</strong>")
                + entry(line + "thick line: a <strong>relationship to cut</strong>; cutting these breaks the cycles")
                + entry(pale + "pale box: a package in a <strong>package cycle</strong>")
                + entry(plain + "plain: <strong>nothing found</strong>")
                + "</div>\n";
    }

    /** The key for the tags on findings: each kind of problem that has its own colour, and the two level marks. */
    static String forFindings() {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"rf-key\" style=\"").append(BOX).append("\"><strong>Key</strong> ");
        sb.append(entry(tag(Family.CYCLE, false, Family.CYCLE.label()) + " " + Family.CYCLE.meaning()));
        sb.append(entry(tag(Family.GOD, false, Family.GOD.label()) + " " + Family.GOD.meaning()));
        sb.append(entry(tag(Family.BRAIN, false, Family.BRAIN.label()) + " " + Family.BRAIN.meaning()));
        sb.append(
                entry(tag(Family.BRAIN_LIGHT, true, Family.BRAIN_LIGHT.label()) + " " + Family.BRAIN_LIGHT.meaning()));
        sb.append(entry(tag(Family.ENVY, true, Family.ENVY.label()) + " " + Family.ENVY.meaning()));
        sb.append(entry(tag(Family.COUPLING, true, Family.COUPLING.label()) + " " + Family.COUPLING.meaning()));
        sb.append(entry(tag(Family.QUIET, false, Family.QUIET.label()) + " " + Family.QUIET.meaning()));
        sb.append(entry(tag(Family.HOTSPOT, false, Family.HOTSPOT.label()) + " " + Family.HOTSPOT.meaning()));
        sb.append(entry(
                ReportPalette.levelMark(false) + " a class &#160; " + ReportPalette.levelMark(true) + " a method"));
        sb.append("</div>\n");
        return sb.toString();
    }

    /**
     * A table under a map that says why things on it are marked: one row per cycle, largest first, with the
     * members of the cycle and the relationships the tool suggests cutting to break it.
     *
     * @param anchorId id for this table, also used by its "show all rows" control
     * @param noun what the cycles are made of, "Class" or "Package"; class names are shortened, package names are not
     * @param rowsAnchor anchor of the full relationships-to-cut table to link to
     * @param graph the graph the cycles and relationships belong to
     * @param cycles the cycles, as the cycle analysis returns them; may be null
     * @param toCut the relationships chosen for removal
     * @return the table, or an empty string when there are no cycles
     */
    static String whyMarked(
            String anchorId,
            String noun,
            String rowsAnchor,
            Graph<VertexInfo, DefaultWeightedEdge> graph,
            Map<VertexInfo, AsSubgraph<VertexInfo, DefaultWeightedEdge>> cycles,
            Set<DefaultWeightedEdge> toCut) {
        if (cycles == null || cycles.isEmpty()) {
            return "";
        }
        boolean shortNames = "Class".equals(noun);
        List<AsSubgraph<VertexInfo, DefaultWeightedEdge>> ordered = new ArrayList<>(cycles.values());
        ordered.sort(Comparator.comparingInt((AsSubgraph<VertexInfo, DefaultWeightedEdge> c) ->
                        c.vertexSet().size())
                .reversed()
                .thenComparing(c -> names(c, shortNames).get(0)));

        String lower = noun.toLowerCase(java.util.Locale.ROOT);
        StringBuilder sb = new StringBuilder();
        sb.append("<div align=\"center\" id=\"").append(anchorId).append("\">\n");
        sb.append("<h3 style=\"margin:14px 0 4px;\">Why these are marked: the ")
                .append(lower)
                .append(" cycles</h3>\n");
        sb.append(
                        "<table align=\"center\" border=\"5px\" style=\"max-width:100%;white-space:normal;\">\n<thead>\n<tr>\n<th>")
                .append(noun)
                .append(lower.endsWith("s") ? "es" : "s")
                .append(" in the cycle</th>\n<th>Size</th>\n<th>Relationships to cut</th>\n</tr>\n</thead>\n<tbody>\n");
        for (AsSubgraph<VertexInfo, DefaultWeightedEdge> cycle :
                ordered.subList(0, Math.min(MAX_ROWS, ordered.size()))) {
            sb.append("<tr>\n<td style=\"text-align:left;\">")
                    .append(listed(names(cycle, shortNames)))
                    .append("</td>\n<td align=\"right\">")
                    .append(cycle.vertexSet().size())
                    .append("</td>\n<td style=\"text-align:left;\">")
                    .append(cuts(graph, cycle, toCut, shortNames))
                    .append("</td>\n</tr>\n");
        }
        sb.append("</tbody>\n</table>\n");
        if (ordered.size() > MAX_ROWS) {
            sb.append("<div>The ")
                    .append(MAX_ROWS)
                    .append(" largest of ")
                    .append(ordered.size())
                    .append(" cycles are shown.</div>\n");
        }
        sb.append("<a href=\"#")
                .append(rowsAnchor)
                .append("\">See every relationship to cut, in priority order</a>\n</div>\n");
        return sb.toString();
    }

    private static List<String> names(AsSubgraph<VertexInfo, DefaultWeightedEdge> cycle, boolean shortNames) {
        return cycle.vertexSet().stream().map(v -> name(v, shortNames)).sorted().toList();
    }

    private static String name(VertexInfo vertex, boolean shortNames) {
        String full = vertex.getName();
        return shortNames ? full.substring(full.lastIndexOf('.') + 1) : full;
    }

    private static String listed(List<String> names) {
        List<String> shown = names.size() > MAX_NAMES ? names.subList(0, MAX_NAMES) : names;
        String text = esc(String.join(", ", shown));
        return names.size() > MAX_NAMES ? text + ", and " + (names.size() - MAX_NAMES) + " more" : text;
    }

    private static String cuts(
            Graph<VertexInfo, DefaultWeightedEdge> graph,
            AsSubgraph<VertexInfo, DefaultWeightedEdge> cycle,
            Set<DefaultWeightedEdge> toCut,
            boolean shortNames) {
        List<String> lines = new ArrayList<>();
        for (DefaultWeightedEdge edge : toCut) {
            if (!cycle.containsEdge(edge)) {
                continue;
            }
            int uses = (int) graph.getEdgeWeight(edge);
            lines.add(esc(name(graph.getEdgeSource(edge), shortNames))
                    + " &#8594; "
                    + esc(name(graph.getEdgeTarget(edge), shortNames))
                    + " (" + uses + (uses == 1 ? " use" : " uses") + ")");
        }
        if (lines.isEmpty()) {
            return "";
        }
        lines.sort(Comparator.naturalOrder());
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(cut(line)).append("<br/>");
        }
        return sb.toString();
    }

    /** The key above the relationships-to-cut tables: what the flags in a row mean. */
    static String forLinksToCut() {
        String flagged =
                swatch("background:" + Family.CYCLE_PALE.hex() + ";border:1px solid #999;width:1.5em;height:0.9em;");
        return "<div class=\"rf-key\" style=\"" + BOX + "\"><strong>Key</strong> "
                + entry(tag(Family.FIX_FIRST, false, "fix first") + " priority 1: cut this first")
                + entry(flagged + cycleMark() + "how many cycles the relationship is part of")
                + entry("<strong>hardest</strong>: the most uses to move, of everything in the table")
                + "</div>\n";
    }

    /** A cycle-count cell: any count above zero is the problem the table is about, so it is flagged. */
    static String cycleCountCell(String count) {
        if (!isPositive(count)) {
            return plainNumberCell(count);
        }
        return "<td align=\"right\" style=\"background:" + Family.CYCLE_PALE.hex() + ";color:"
                + Family.CYCLE_PALE.textOn() + ";font-weight:bold;\">" + count + " "
                + cycleMark().strip() + "</td>\n";
    }

    /** A findings section heading: the class or method mark in the finding's colour, then the title. */
    static String sectionTitle(String anchorId, boolean methodLevel, String title) {
        return heading(anchorId, ReportPalette.forFinding(anchorId).hex(), ReportPalette.levelMark(methodLevel), title);
    }

    /** A cycle section heading: the cycle mark in the cycle colour, then the title. */
    static String cycleSectionTitle(String anchorId, String title) {
        return heading(anchorId, Family.CYCLE.hex(), Family.CYCLE.mark(), title);
    }

    private static String heading(String anchorId, String colour, String mark, String title) {
        return "<div style=\"text-align: center;\"><a id=\"" + anchorId + "\"><h1><span style=\"color:" + colour
                + ";\">" + mark + "</span> " + title + "</h1></a></div>\n";
    }

    /** A priority cell: priority 1 carries the "fix first" tag. */
    static String priorityCell(String priority) {
        if (!"1".equals(priority)) {
            return plainNumberCell(priority);
        }
        return "<td align=\"right\" style=\"white-space:nowrap;\">" + priority + " "
                + tag(Family.FIX_FIRST, false, "fix first") + "</td>\n";
    }

    /** A strength cell: the strongest relationship in the table is the hardest to cut, and says so. */
    static String strengthCell(String strength, boolean hardest) {
        if (!hardest) {
            return plainNumberCell(strength);
        }
        return "<td align=\"right\" style=\"white-space:nowrap;font-weight:bold;\">" + strength + " hardest</td>\n";
    }

    /**
     * One plain sentence on why a relationship is in the table: the cycle it closes and the work to move it.
     *
     * @param noun what the cycles are made of, in the plural: "classes" or "packages"
     */
    static String whyLink(
            Graph<VertexInfo, DefaultWeightedEdge> graph,
            Map<VertexInfo, AsSubgraph<VertexInfo, DefaultWeightedEdge>> cycles,
            DefaultWeightedEdge edge,
            String noun) {
        int uses = (int) graph.getEdgeWeight(edge);
        String work = uses + (uses == 1 ? " use" : " uses") + " to move.";
        int in = 0;
        int largest = 0;
        if (cycles != null) {
            for (AsSubgraph<VertexInfo, DefaultWeightedEdge> cycle : cycles.values()) {
                if (cycle.containsEdge(edge)) {
                    in++;
                    largest = Math.max(largest, cycle.vertexSet().size());
                }
            }
        }
        if (in == 0) {
            return work;
        }
        return (in == 1
                        ? "Closes a cycle of " + largest + " " + noun + ". "
                        : "Part of " + in + " cycles; the largest has " + largest + " " + noun + ". ")
                + work;
    }

    /** Marks a relationship as one to cut: scissors, bold, in the cycle colour. The text must already be safe HTML. */
    static String cut(String html) {
        return "<span style=\"color:" + Family.CYCLE.hex() + ";font-weight:bold;\">&#9986; " + html + "</span>";
    }

    private static String cycleMark() {
        return "<span style=\"color:" + Family.CYCLE.hex() + ";font-weight:bold;\">" + Family.CYCLE.mark() + "</span> ";
    }

    private static String plainNumberCell(String value) {
        return "<td align=\"right\">" + value + "</td>\n";
    }

    private static boolean isPositive(String number) {
        try {
            return Integer.parseInt(number) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * A small filled label in a family's colour. A smell's tag leads with the level mark, a square for a class
     * or a diamond for a method; cycle, hotspot and fix first lead with their own mark. It never breaks across
     * lines.
     *
     * @param methodLevel whether the finding is about one method; only read for the smell families
     */
    static String tag(Family family, boolean methodLevel, String text) {
        String mark = family.isSmell() ? ReportPalette.levelMark(methodLevel) : family.mark();
        return "<span style=\"font-size:0.8em;font-weight:bold;white-space:nowrap;color:" + family.textOn()
                + ";background:" + family.hex() + ";border:1.5px solid " + family.hex()
                + ";border-radius:9px;padding:1px 8px;\">"
                + (mark.isEmpty() ? "" : mark + " ")
                + esc(text)
                + "</span>";
    }

    /** Makes text safe to place in the page. */
    private static String esc(String text) {
        return text == null
                ? ""
                : text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String swatch(String style) {
        return "<span style=\"display:inline-block;" + style + "margin-right:5px;\"></span>";
    }

    private static String entry(String html) {
        return "<span style=\"display:inline-block;margin-right:18px;white-space:nowrap;\">" + html + "</span> ";
    }
}
