package org.hjug.refactorfirst.report;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.hjug.refactorfirst.report.ReportPalette.Family;

/**
 * Draws one cycle as a ring: its classes placed round a circle, its links as curved arrows, and the
 * links the tool suggests cutting drawn thick in the cycle colour with scissors and the number of uses.
 *
 * <p>A cycle is a loop, so it is drawn as one; a straight line of arrows hides that. The output is an
 * inline SVG with no script, so it works in the simple report too. Colours come from
 * {@link ReportPalette}. Like {@link ReportKey}, this uses nothing else from the report, so that the
 * report classes can use it without it joining a dependency cycle of its own.
 */
final class CycleRingSvg {

    /** A cycle with more classes than this is listed by name: a ring that crowded cannot be read. */
    static final int MAX_CLASSES = 12;

    /** From this many classes, each class is a dot on the ring with its name outside it, so names cannot overlap. */
    static final int OUTSIDE_FROM = 6;

    private static final int WIDE_WIDTH = 680;
    private static final int WIDE_HEIGHT = 300;
    private static final double WIDE_RADIUS = 92;
    private static final double DOT = 6;

    static final int WIDTH = 318;
    static final int HEIGHT = 214;

    private static final double RADIUS = 70;
    private static final double PILL_HALF_HEIGHT = 11;
    private static final double CHAR_WIDTH = 6.3;

    private CycleRingSvg() {}

    /**
     * One "uses" link between two classes of the cycle.
     *
     * @param from fully qualified name of the class that uses
     * @param to fully qualified name of the class that is used
     * @param uses how many uses the link stands for
     * @param cut whether this is a link the tool suggests cutting to break the cycle
     */
    record Link(String from, String to, int uses, boolean cut) {}

    /**
     * Renders the ring.
     *
     * @param id unique on the page; the arrowheads are defined once per drawing under this id
     * @param classNames the fully qualified names of the classes in the cycle, in the order to place them
     * @param links the links between them; a link naming a class that is not in the list is ignored
     * @return an inline SVG, or a plain list of names when there are more than {@value #MAX_CLASSES} classes
     */
    static String render(String id, List<String> classNames, List<Link> links) {
        int count = classNames.size();
        if (count > MAX_CLASSES) {
            return listed(classNames);
        }
        boolean outside = count >= OUTSIDE_FROM;
        int width = outside ? WIDE_WIDTH : WIDTH;
        int height = outside ? WIDE_HEIGHT : HEIGHT;
        double radius = outside ? WIDE_RADIUS : RADIUS;
        double centreX = width / 2.0;
        double centreY = height / 2.0;
        // Four classes need a wider ring: the ones at the sides would otherwise sit on top of each other.
        double radiusX = radius * (count == 4 ? 1.22 : 1);
        double radiusY = radius * (count == 4 ? 1.15 : 1);

        List<String> labels = new ArrayList<>();
        double[] xs = new double[count];
        double[] ys = new double[count];
        double[] angles = new double[count];
        for (int i = 0; i < count; i++) {
            // Start at the top and go round; two classes sit left and right instead of top and bottom.
            double angle = -Math.PI / 2 + i * 2 * Math.PI / Math.max(count, 1) + (count == 2 ? Math.PI / 2 : 0);
            angles[i] = angle;
            labels.add(shortName(classNames.get(i)));
            xs[i] = centreX + radiusX * Math.cos(angle);
            ys[i] = centreY + radiusY * Math.sin(angle);
            if (!outside) {
                // Keep a long name inside the drawing.
                double half = pillWidth(labels.get(i)) / 2;
                xs[i] = Math.max(half + 1, Math.min(width - half - 1, xs[i]));
            }
        }

        String cycle = Family.CYCLE.hex();
        String ink = Family.INK.hex();
        StringBuilder svg = new StringBuilder();
        svg.append("<svg viewBox=\"0 0 ")
                .append(width)
                .append(' ')
                .append(height)
                .append("\" width=\"100%\" style=\"max-width:")
                .append(width)
                .append("px;font:11.5px sans-serif;\" role=\"img\" aria-label=\"")
                .append(esc("A cycle of " + String.join(", ", labels)))
                .append("\">\n<defs>\n");
        svg.append(arrowhead(id + "-link", ink, 9, 0.45));
        svg.append(arrowhead(id + "-cut", cycle, 11, 1));
        svg.append("</defs>\n");
        svg.append("<ellipse cx=\"")
                .append(n(centreX))
                .append("\" cy=\"")
                .append(n(centreY))
                .append("\" rx=\"")
                .append(n(radiusX))
                .append("\" ry=\"")
                .append(n(radiusY))
                .append("\" fill=\"none\" stroke=\"")
                .append(Family.CYCLE_PALE.hex())
                .append("\" stroke-width=\"10\"/>\n");

        for (Link link : links) {
            int from = classNames.indexOf(link.from());
            int to = classNames.indexOf(link.to());
            if (from < 0 || to < 0 || from == to) {
                continue;
            }
            double dx = xs[to] - xs[from];
            double dy = ys[to] - ys[from];
            double length = Math.hypot(dx, dy);
            // The curve bends to the left of its direction, so a pair of opposite links bend apart.
            double normalX = -dy / length;
            double normalY = dx / length;
            double bend = count == 2 ? radius * 0.95 : (length > radius * 1.9 ? 26 : 20);
            double controlX = (xs[from] + xs[to]) / 2 + normalX * bend;
            double controlY = (ys[from] + ys[to]) / 2 + normalY * bend;
            double[] start = outside
                    ? dotEdge(xs[from], ys[from], controlX, controlY)
                    : pillEdge(xs[from], ys[from], controlX, controlY, pillWidth(labels.get(from)));
            double[] end = outside
                    ? dotEdge(xs[to], ys[to], controlX, controlY)
                    : pillEdge(xs[to], ys[to], controlX, controlY, pillWidth(labels.get(to)));

            svg.append("<path d=\"M")
                    .append(n(start[0]))
                    .append(',')
                    .append(n(start[1]))
                    .append(" Q")
                    .append(n(controlX))
                    .append(',')
                    .append(n(controlY))
                    .append(' ')
                    .append(n(end[0]))
                    .append(',')
                    .append(n(end[1]))
                    .append("\" fill=\"none\" ");
            if (link.cut()) {
                svg.append("stroke=\"")
                        .append(cycle)
                        .append("\" stroke-width=\"2.8\" marker-end=\"url(#")
                        .append(id)
                        .append("-cut)\"/>\n");
                double labelX = (start[0] + 2 * controlX + end[0]) / 4 + normalX * 13;
                double labelY = (start[1] + 2 * controlY + end[1]) / 4 + normalY * 13 + 4;
                svg.append("<text x=\"")
                        .append(n(labelX))
                        .append("\" y=\"")
                        .append(n(labelY))
                        .append("\" text-anchor=\"middle\" font-weight=\"bold\" fill=\"")
                        .append(cycle)
                        .append("\" stroke=\"#ffffff\" stroke-width=\"3\" paint-order=\"stroke\">&#9986; ")
                        .append(link.uses())
                        .append("</text>\n");
            } else {
                svg.append("stroke=\"")
                        .append(ink)
                        .append("\" stroke-opacity=\"0.45\" stroke-width=\"1.3\" marker-end=\"url(#")
                        .append(id)
                        .append("-link)\"/>\n");
            }
        }

        for (int i = 0; outside && i < count; i++) {
            double cos = Math.cos(angles[i]);
            double sin = Math.sin(angles[i]);
            String anchor = Math.abs(cos) < 0.3 ? "middle" : cos > 0 ? "start" : "end";
            // A name at the very top or bottom sits above or below its dot; the rest sit beside theirs.
            double lift = sin > 0.9 ? 9 : sin < -0.9 ? -3 : 4;
            svg.append("<circle class=\"rf-dot\" cx=\"")
                    .append(n(xs[i]))
                    .append("\" cy=\"")
                    .append(n(ys[i]))
                    .append("\" r=\"")
                    .append(n(DOT))
                    .append("\" fill=\"")
                    .append(Family.NEUTRAL.hex())
                    .append("\" stroke=\"")
                    .append(cycle)
                    .append("\" stroke-width=\"2\"/>\n<text x=\"")
                    .append(n(xs[i] + (DOT + 8) * cos))
                    .append("\" y=\"")
                    .append(n(ys[i] + (DOT + 8) * sin + lift))
                    .append("\" text-anchor=\"")
                    .append(anchor)
                    .append("\" fill=\"")
                    .append(ink)
                    .append("\">")
                    .append(esc(labels.get(i)))
                    .append("</text>\n");
        }
        for (int i = 0; !outside && i < count; i++) {
            double pill = pillWidth(labels.get(i));
            svg.append("<rect x=\"")
                    .append(n(xs[i] - pill / 2))
                    .append("\" y=\"")
                    .append(n(ys[i] - PILL_HALF_HEIGHT))
                    .append("\" width=\"")
                    .append(n(pill))
                    .append("\" height=\"")
                    .append(n(PILL_HALF_HEIGHT * 2))
                    .append("\" rx=\"")
                    .append(n(PILL_HALF_HEIGHT))
                    .append("\" fill=\"")
                    .append(Family.NEUTRAL.hex())
                    .append("\" stroke=\"")
                    .append(cycle)
                    .append("\" stroke-width=\"1.4\"/>\n<text x=\"")
                    .append(n(xs[i]))
                    .append("\" y=\"")
                    .append(n(ys[i] + 4))
                    .append("\" text-anchor=\"middle\" fill=\"")
                    .append(ink)
                    .append("\">")
                    .append(esc(labels.get(i)))
                    .append("</text>\n");
        }
        svg.append("</svg>\n");
        return svg.toString();
    }

    /** Where a link meets a dot: on the dot's edge, on the side facing the curve's control point. */
    private static double[] dotEdge(double x, double y, double towardsX, double towardsY) {
        double dx = towardsX - x;
        double dy = towardsY - y;
        double length = Math.max(Math.hypot(dx, dy), 0.001);
        return new double[] {x + dx / length * (DOT + 2), y + dy / length * (DOT + 2)};
    }

    /** The name a reader needs: no package, and for a nested class no outer class either. */
    static String shortName(String fullyQualifiedName) {
        String name = fullyQualifiedName.substring(fullyQualifiedName.lastIndexOf('.') + 1);
        return name.substring(name.lastIndexOf('$') + 1);
    }

    private static String listed(List<String> classNames) {
        List<String> shown = new ArrayList<>();
        for (String name : classNames.subList(0, MAX_CLASSES)) {
            shown.add(shortName(name));
        }
        return "<div style=\"text-align:left;\">" + esc(String.join(", ", shown)) + ", and "
                + (classNames.size() - MAX_CLASSES) + " more</div>\n";
    }

    private static double pillWidth(String label) {
        return label.length() * CHAR_WIDTH + 14;
    }

    /** Where a line from a pill's centre towards a point leaves the pill, so arrows stop at its edge. */
    private static double[] pillEdge(double x, double y, double towardsX, double towardsY, double pillWidth) {
        double dx = towardsX - x;
        double dy = towardsY - y;
        double length = Math.hypot(dx, dy);
        double unitX = dx / length;
        double unitY = dy / length;
        double reach = Math.min(
                (pillWidth / 2 + 3) / Math.max(Math.abs(unitX), 1e-6),
                (PILL_HALF_HEIGHT + 3) / Math.max(Math.abs(unitY), 1e-6));
        return new double[] {x + unitX * reach, y + unitY * reach};
    }

    private static String arrowhead(String id, String colour, int size, double opacity) {
        return "<marker id=\"" + id + "\" viewBox=\"0 0 10 10\" refX=\"9\" refY=\"5\" markerWidth=\"" + size
                + "\" markerHeight=\"" + size + "\" markerUnits=\"userSpaceOnUse\" orient=\"auto\">"
                + "<path d=\"M0,0L10,5L0,10z\" fill=\"" + colour + "\" fill-opacity=\"" + opacity + "\"/></marker>\n";
    }

    private static String n(double value) {
        return String.format(Locale.US, "%.1f", value);
    }

    private static String esc(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
