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
        String mark = "<span style=\"color:" + cycle + ";font-weight:bold;\">" + Family.CYCLE.mark(true) + "</span> ";
        return "<div class=\"rf-key\" style=\"" + BOX + "\"><strong>Key</strong> "
                + entry(outline + mark + "outlined: a class or package <strong>in a cycle</strong>")
                + entry(line + "thick line: a <strong>relationship to cut</strong>; cutting these breaks the cycles")
                + entry(pale + "pale box: a package in a <strong>package cycle</strong>")
                + entry(plain + "plain: <strong>nothing found</strong>")
                + "</div>\n";
    }

    /** The key for the tags on findings: every family that can appear as a tag, with what it means. */
    static String forFindings() {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"rf-key\" style=\"").append(BOX).append("\"><strong>Key</strong> ");
        for (Family family :
                List.of(Family.FIX_FIRST, Family.CYCLE, Family.CLASS_FINDING, Family.METHOD_FINDING, Family.HOTSPOT)) {
            sb.append(entry(tag(family, true, family.label()) + " " + family.meaning()));
        }
        sb.append(entry("A solid tag is a serious finding; an outlined tag is a milder one."));
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
            sb.append("<span style=\"color:")
                    .append(Family.CYCLE.hex())
                    .append(";font-weight:bold;\">&#9986; ")
                    .append(line)
                    .append("</span><br/>");
        }
        return sb.toString();
    }

    /**
     * A small rounded label. Solid: the family's colour behind the text. Outline: the family's colour as
     * border and text on white, for the milder finding types. The family's mark leads the text either way.
     */
    static String tag(Family family, boolean solid, String text) {
        String colours = solid
                ? "color:" + family.textOn() + ";background:" + family.hex() + ";border:1.5px solid " + family.hex()
                : "color:" + family.hex() + ";background:" + Family.NEUTRAL.hex() + ";border:1.5px solid "
                        + family.hex();
        String mark = family.mark(solid);
        return "<span style=\"font-size:0.8em;font-weight:bold;" + colours + ";border-radius:9px;padding:1px 8px;\">"
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
