package org.hjug.refactorfirst.report;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.hjug.cbc.RankedDisharmony;
import org.hjug.refactorfirst.report.ReportPalette.Family;
import org.hjug.refactorfirst.report.SimpleHtmlReport.DisharmonySpec;

/**
 * The first thing a reader sees: a few headline numbers and a short list of places to look first,
 * above the full tables. It adds no analysis of its own; it only chooses from what the report already ranks.
 *
 * <p>Cycles come first, each drawn as a ring, because a cycle is the one finding a reader cannot see in a
 * table. At most {@value #MAX_CYCLE_CARDS} are drawn, largest first.
 *
 * <p>How the list under them is chosen, in order, up to {@value #MAX_PICKS} entries:
 * <ol>
 *   <li>up to {@value #MAX_CLASSES} classes that do too much: God Classes by priority, then Brain Classes;</li>
 *   <li>the hotspot: the most-changed file with a method-level finding that is not already listed;</li>
 *   <li>if there is room left: Brain Methods, then Feature Envy, then any remaining God or Brain Classes.</li>
 * </ol>
 * Data Classes and the coupling smells are never chosen for the list: there are usually far too many of them
 * to be a starting point. They stay one click away in the "everything else" row.
 */
final class StartHereSummary {

    static final int MAX_PICKS = 5;
    static final int MAX_CLASSES = 3;
    static final int MAX_CYCLE_CARDS = 4;

    private static final String GOD = "GOD";
    private static final String BRAIN_CLASS = "BRAIN_CLASS";
    private static final String BRAIN_METHOD = "BRAIN_METHOD";
    private static final String FEATURE_ENVY = "FEATURE_ENVY";
    private static final String CYCLES_ANCHOR = "CYCLES";

    private StartHereSummary() {}

    /**
     * One entry in the "places to look first" list.
     *
     * @param className the fully qualified class the entry is about, used to say when it is also in a cycle
     * @param family what kind of thing it is; decides the tag's colour and mark
     * @param serious a solid tag when true, an outline for the milder finding types
     * @param fixFirst the tool's priority 1 of its kind, which earns the red "fix first" tag beside its own
     */
    record Pick(
            String name,
            String className,
            String badge,
            Family family,
            boolean serious,
            boolean fixFirst,
            String why,
            String action,
            String anchor,
            String key) {}

    /**
     * One class cycle, ready to draw.
     *
     * @param name the cycle's name, as the report names it
     * @param classes fully qualified names of the classes in it
     * @param links the uses between them, with the ones to cut marked
     * @param fixFirst whether this cycle holds the tool's priority 1 relationship to cut
     */
    record CycleCard(String name, List<String> classes, List<CycleRingSvg.Link> links, boolean fixFirst) {}

    /**
     * Everything the block shows about cycles.
     *
     * @param cards one per class cycle
     * @param linksToCut how many class relationships the tool suggests cutting
     * @param packageCycles how many package cycles there are
     * @param packageLinksToCut how many package relationships the tool suggests cutting
     * @param hasMaps whether this report has the class map (the simple report does not), for the links
     */
    record Cycles(List<CycleCard> cards, int linksToCut, int packageCycles, int packageLinksToCut, boolean hasMaps) {

        static Cycles none() {
            return new Cycles(List.of(), 0, 0, 0, false);
        }

        /** Every class that is in at least one cycle. */
        Set<String> classes() {
            Set<String> all = new LinkedHashSet<>();
            cards.forEach(card -> all.addAll(card.classes()));
            return all;
        }
    }

    static String render(
            String projectName,
            int classesAnalysed,
            List<DisharmonySpec> specs,
            Map<String, List<RankedDisharmony>> findingsByAnchor,
            Cycles cycles) {

        boolean anyFinding = findingsByAnchor.values().stream().anyMatch(list -> list != null && !list.isEmpty());
        if (!anyFinding && cycles.cards().isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<section id=\"START_HERE\" style=\"max-width:900px;margin:18px auto;padding:14px 18px;")
                .append("border:1px solid #d9d2c3;border-radius:12px;text-align:left;\">\n");
        // The cycle mark turns slowly, and stands still for readers who have asked for less motion.
        sb.append("<style>@keyframes rf-spin{to{transform:rotate(360deg);}}")
                .append(".rf-spin{display:inline-block;animation:rf-spin 6s linear infinite;}")
                .append("@media (prefers-reduced-motion: reduce){.rf-spin{animation:none;}}</style>\n");
        sb.append("<h2 style=\"margin:0 0 10px;\">").append(esc(projectName)).append(": start here</h2>\n");

        boolean hasCycles = !cycles.cards().isEmpty();
        sb.append("<div style=\"display:flex;gap:8px;flex-wrap:wrap;margin-bottom:12px;\">\n");
        if (hasCycles) {
            cycleTile(sb, cycles.cards().size());
        }
        tile(sb, count(findingsByAnchor, GOD) + count(findingsByAnchor, BRAIN_CLASS), "classes doing too much");
        if (!hasCycles) {
            tile(sb, 0, "dependency cycles");
        }
        tile(sb, count(findingsByAnchor, BRAIN_METHOD), "over-long, tangled methods");
        tile(sb, classesAnalysed, "classes analysed");
        sb.append("</div>\n");

        if (hasCycles) {
            renderCycles(sb, cycles);
        }

        Set<String> classesInCycles = cycles.classes();
        List<Pick> picks = pick(specs, findingsByAnchor);
        if (!picks.isEmpty()) {
            sb.append("<p style=\"margin:0 0 4px;\"><strong>")
                    .append(
                            hasCycles
                                    ? "Other places to look first"
                                    : picks.size() == 1 ? "The place to look first" : "Places to look first")
                    // A long method signature has no spaces to break at; let it wrap anywhere, or it pushes the list
                    // out of the block.
                    .append("</strong></p>\n<ol style=\"margin:0;padding-left:22px;min-width:0;max-width:100%;")
                    .append("overflow-wrap:anywhere;\">\n");
            for (Pick p : picks) {
                sb.append("<li style=\"margin:8px 0;\"><a href=\"#")
                        .append(p.anchor())
                        .append("\" style=\"font-weight:bold;\">")
                        .append(esc(p.name()))
                        .append("</a> ")
                        .append(ReportKey.tag(p.family(), p.serious(), p.badge()));
                if (p.fixFirst()) {
                    sb.append(" ").append(ReportKey.tag(Family.FIX_FIRST, true, "fix first"));
                }
                if (classesInCycles.contains(p.className())) {
                    sb.append(" ").append(ReportKey.tag(Family.CYCLE, true, "in a cycle"));
                }
                sb.append("<br/><span>")
                        .append(esc(p.why()))
                        .append("</span><br/><span style=\"color:")
                        .append(Family.INK.hex())
                        .append(";\">&#8594; ")
                        .append(esc(p.action()))
                        .append("</span></li>\n");
            }
            sb.append("</ol>\n");
        }

        if (!picks.isEmpty()) {
            sb.append(ReportKey.forFindings());
        }

        sb.append("<p style=\"margin:12px 0 0;font-size:0.9em;\">Everything found, in full below: ");
        for (DisharmonySpec spec : specs) {
            int n = count(findingsByAnchor, spec.anchorId());
            if (n > 0) {
                chip(sb, spec.anchorId(), spec.title() + " " + n);
            }
        }
        if (hasCycles) {
            chip(sb, CYCLES_ANCHOR, "Class Cycles " + cycles.cards().size());
        }
        sb.append("</p>\n</section>\n");
        return sb.toString();
    }

    /** The cycles, drawn: a statement of what the colour means, a ring per cycle, and where else they show. */
    private static void renderCycles(StringBuilder sb, Cycles cycles) {
        String cycle = Family.CYCLE.hex();
        List<CycleCard> largestFirst = new ArrayList<>(cycles.cards());
        largestFirst.sort(
                Comparator.comparingInt((CycleCard c) -> c.classes().size()).reversed());

        sb.append("<p style=\"margin:0 0 4px;color:")
                .append(cycle)
                .append(";\"><strong>")
                .append(spinningMark())
                .append(" Cycles found: ")
                .append(largestFirst.size())
                .append("</strong></p>\n");
        sb.append("<p style=\"margin:0 0 8px;font-size:0.9em;\"><strong style=\"color:")
                .append(cycle)
                .append(";\">Pink always means a cycle</strong>: classes that use each other in a loop, so none can ")
                .append("change or be tested alone. The <strong style=\"color:")
                .append(cycle)
                .append(";\">thick pink arrow &#9986;</strong> is the link the tool suggests cutting, with its ")
                .append("number of uses. <strong style=\"color:")
                .append(Family.FIX_FIRST.hex())
                .append(";\">Red means fix first.</strong></p>\n");

        sb.append("<div style=\"display:flex;gap:8px;flex-wrap:wrap;\">\n");
        int shown = Math.min(MAX_CYCLE_CARDS, largestFirst.size());
        for (int i = 0; i < shown; i++) {
            CycleCard card = largestFirst.get(i);
            sb.append("<div style=\"box-sizing:border-box;flex:1 1 180px;max-width:330px;border:1px solid #d9d2c3;")
                    .append("border-radius:10px;")
                    .append("padding:7px 9px;font-size:0.85em;\"><strong>")
                    .append(esc(simpleName(card.name())))
                    .append("</strong> &#183; ")
                    .append(card.classes().size())
                    .append(" classes");
            if (card.fixFirst()) {
                sb.append(" ").append(ReportKey.tag(Family.FIX_FIRST, true, "fix first"));
            }
            sb.append("<br/>\n").append(CycleRingSvg.render("rf-cycle-" + i, card.classes(), card.links()));
            String cuts = cutsText(card);
            if (!cuts.isEmpty()) {
                sb.append(ReportKey.cut(cuts));
            }
            sb.append("</div>\n");
        }
        sb.append("</div>\n");
        if (largestFirst.size() > shown) {
            sb.append("<p style=\"margin:6px 0 0;font-size:0.9em;\">and <a href=\"#")
                    .append(CYCLES_ANCHOR)
                    .append("\">")
                    .append(largestFirst.size() - shown)
                    .append(" more</a>, smaller.</p>\n");
        }

        sb.append("<p style=\"margin:8px 0 12px;font-size:0.9em;\">Where the cycles are shown: ");
        if (cycles.hasMaps()) {
            chip(sb, "CLASSMAP", "Class map");
            chip(sb, "WHY_CLASSES", "Why these are marked");
        }
        chip(sb, CYCLES_ANCHOR, "Class cycles table");
        if (cycles.linksToCut() > 0) {
            chip(sb, "CLASSEDGES", "Links to cut " + cycles.linksToCut());
        }
        if (cycles.packageCycles() > 0) {
            chip(sb, cycles.hasMaps() ? "PACKAGEMAP" : "PACKAGEEDGES", "Package cycles " + cycles.packageCycles());
        }
        if (cycles.packageLinksToCut() > 0) {
            chip(sb, "PACKAGEEDGES", "Package links to cut " + cycles.packageLinksToCut());
        }
        sb.append("</p>\n");
    }

    /** "cut 2 links: 7 and 3 uses", from the links of a cycle that are marked to cut. */
    private static String cutsText(CycleCard card) {
        List<String> uses = card.links().stream()
                .filter(CycleRingSvg.Link::cut)
                .map(link -> String.valueOf(link.uses()))
                .toList();
        if (uses.isEmpty()) {
            return "";
        }
        boolean single = uses.size() == 1;
        return "cut " + uses.size() + (single ? " link: " : " links: ") + String.join(" and ", uses)
                + (single && "1".equals(uses.get(0)) ? " use" : " uses");
    }

    private static String spinningMark() {
        return "<span class=\"rf-spin\">" + Family.CYCLE.mark(true) + "</span>";
    }

    /** The cycles tile: first in the row and in the cycle colour, because the cycles lead the block. */
    private static void cycleTile(StringBuilder sb, int cycles) {
        sb.append("<div style=\"flex:1;min-width:150px;border:2px solid ")
                .append(Family.CYCLE.hex())
                .append(";border-radius:10px;padding:7px 10px;\"><span style=\"font-size:1.6em;line-height:1.1;color:")
                .append(Family.CYCLE.hex())
                .append(";\"><b>")
                .append(spinningMark())
                .append(" ")
                .append(String.format(Locale.US, "%,d", cycles))
                .append("</b></span><br/><span>dependency cycles</span></div>\n");
    }

    static List<Pick> pick(List<DisharmonySpec> specs, Map<String, List<RankedDisharmony>> findingsByAnchor) {
        List<Pick> picks = new ArrayList<>();
        Set<String> keys = new LinkedHashSet<>();
        Set<String> files = new LinkedHashSet<>();

        // 1. Classes that do too much, the most serious class-level findings.
        List<RankedDisharmony> seriousClasses = new ArrayList<>(byPriority(findingsByAnchor, GOD));
        seriousClasses.addAll(byPriority(findingsByAnchor, BRAIN_CLASS));
        for (RankedDisharmony d : seriousClasses) {
            if (picks.size() < MAX_CLASSES) {
                add(picks, keys, files, classPick(d, specs, findingsByAnchor));
            }
        }

        // 2. The hotspot: the most-changed file with a method-level finding, not already listed.
        hotspot(specs, findingsByAnchor, files).ifPresent(p -> add(picks, keys, files, p));

        // 3. Room left: the next most serious things.
        for (RankedDisharmony d : byPriority(findingsByAnchor, BRAIN_METHOD)) {
            add(picks, keys, files, methodPick(d, spec(specs, BRAIN_METHOD), Family.METHOD_FINDING, typeLabel(d)));
        }
        for (RankedDisharmony d : byPriority(findingsByAnchor, FEATURE_ENVY)) {
            add(picks, keys, files, methodPick(d, spec(specs, FEATURE_ENVY), Family.METHOD_FINDING, typeLabel(d)));
        }
        for (RankedDisharmony d : seriousClasses) {
            add(picks, keys, files, classPick(d, specs, findingsByAnchor));
        }
        return picks;
    }

    private static void add(List<Pick> picks, Set<String> keys, Set<String> files, Pick pick) {
        if (pick == null || picks.size() >= MAX_PICKS || !keys.add(pick.key())) {
            return;
        }
        picks.add(pick);
        if (pick.key().startsWith("file:")) {
            files.add(pick.key().substring("file:".length(), pick.key().lastIndexOf('#')));
        }
    }

    private static Pick classPick(
            RankedDisharmony d, List<DisharmonySpec> specs, Map<String, List<RankedDisharmony>> findingsByAnchor) {
        DisharmonySpec own = specs.stream()
                .filter(s -> s.type().equals(d.getDisharmonyType()))
                .findFirst()
                .orElse(null);
        if (own == null) {
            return null;
        }
        List<String> alsoFlagged = new ArrayList<>();
        for (DisharmonySpec other : specs) {
            if (other != own
                    && findingsByAnchor.getOrDefault(other.anchorId(), List.of()).stream()
                            .anyMatch(o -> samePath(o, d))) {
                alsoFlagged.add(other.title());
            }
        }
        String why = sentence(own.problem()) + " " + changed(d)
                + (alsoFlagged.isEmpty() ? "" : " Also flagged: " + String.join(", ", alsoFlagged) + ".");
        return new Pick(
                simpleName(d.getClassName()),
                d.getClassName(),
                d.getDisharmonyType(),
                ReportPalette.forFinding(own.methodLevel()),
                ReportPalette.isSerious(own.anchorId()),
                isPriorityOne(d),
                why,
                sentence(own.solution()),
                own.anchorId(),
                "file:" + d.getPath() + "#" + own.anchorId());
    }

    private static Pick methodPick(RankedDisharmony d, DisharmonySpec spec, Family family, String badge) {
        if (spec == null) {
            return null;
        }
        String name =
                simpleName(d.getClassName()) + (d.getMethodSignature() == null ? "" : "." + d.getMethodSignature());
        return new Pick(
                name,
                d.getClassName(),
                badge,
                family,
                family != Family.METHOD_FINDING || ReportPalette.isSerious(spec.anchorId()),
                // The hotspot is chosen for how often it changes, not for its rank, so it is never "fix first".
                family == Family.METHOD_FINDING && isPriorityOne(d),
                sentence(spec.problem()) + " " + changed(d),
                sentence(spec.solution()),
                spec.anchorId(),
                "file:" + d.getPath() + "#" + spec.anchorId() + ":" + d.getMethodSignature());
    }

    private static Optional<Pick> hotspot(
            List<DisharmonySpec> specs, Map<String, List<RankedDisharmony>> findingsByAnchor, Set<String> listedFiles) {
        RankedDisharmony best = null;
        DisharmonySpec bestSpec = null;
        for (DisharmonySpec spec : specs) {
            if (!spec.methodLevel()) {
                continue;
            }
            for (RankedDisharmony d : byPriority(findingsByAnchor, spec.anchorId())) {
                if (!listedFiles.contains(d.getPath()) && (best == null || commits(d) > commits(best))) {
                    best = d;
                    bestSpec = spec;
                }
            }
        }
        if (best == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(
                methodPick(best, bestSpec, Family.HOTSPOT, "Hotspot · " + commitsText(commits(best))));
    }

    private static List<RankedDisharmony> byPriority(Map<String, List<RankedDisharmony>> findings, String anchor) {
        List<RankedDisharmony> list = findings.get(anchor);
        if (list == null) {
            return List.of();
        }
        return list.stream()
                .sorted(Comparator.comparingInt(
                        (RankedDisharmony d) -> d.getPriority() == null ? Integer.MAX_VALUE : d.getPriority()))
                .toList();
    }

    private static boolean isPriorityOne(RankedDisharmony d) {
        return d.getPriority() != null && d.getPriority() == 1;
    }

    private static DisharmonySpec spec(List<DisharmonySpec> specs, String anchor) {
        return specs.stream()
                .filter(s -> s.anchorId().equals(anchor))
                .findFirst()
                .orElse(null);
    }

    private static boolean samePath(RankedDisharmony a, RankedDisharmony b) {
        return a.getPath() != null && a.getPath().equals(b.getPath());
    }

    private static int count(Map<String, List<RankedDisharmony>> findings, String anchor) {
        List<RankedDisharmony> list = findings.get(anchor);
        return list == null ? 0 : list.size();
    }

    private static int commits(RankedDisharmony d) {
        return d.getCommitCount() == null ? 0 : d.getCommitCount();
    }

    private static String commitsText(int commits) {
        return commits + (commits == 1 ? " commit" : " commits");
    }

    private static String changed(RankedDisharmony d) {
        return "Changed in " + commitsText(commits(d)) + ".";
    }

    private static String typeLabel(RankedDisharmony d) {
        return d.getDisharmonyType() == null ? "" : d.getDisharmonyType();
    }

    /** The last part of a qualified name; nested types read Outer.Inner. */
    static String simpleName(String className) {
        if (className == null) {
            return "";
        }
        String last = className.substring(className.lastIndexOf('.') + 1);
        return last.replace('$', '.');
    }

    /** Trims trailing punctuation left in the shared descriptions and ends with a full stop. */
    private static String sentence(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        // The shared advice text carries line-break tags meant for a table cell; here it is one sentence.
        String trimmed = text.replaceAll("(?i)\\s*<br\\s*/?>\\s*", " ").strip().replaceAll("[\\s,;:.]+$", "");
        return trimmed + ".";
    }

    private static void tile(StringBuilder sb, int number, String label) {
        sb.append(
                        "<div style=\"flex:1;min-width:150px;border:1px solid #d9d2c3;border-radius:10px;padding:7px 10px;\">")
                .append("<span style=\"font-size:1.6em;line-height:1.1;\"><b>")
                .append(String.format(Locale.US, "%,d", number))
                .append("</b></span><br/><span>")
                .append(label)
                .append("</span></div>\n");
    }

    private static void chip(StringBuilder sb, String anchor, String text) {
        sb.append("<a href=\"#")
                .append(anchor)
                .append("\" style=\"display:inline-block;border:1px solid #d9d2c3;border-radius:9px;")
                .append("padding:1px 9px;margin:3px 4px 0 0;text-decoration:none;\">")
                .append(esc(text))
                .append("</a>");
    }

    private static String esc(String value) {
        return SimpleHtmlReport.escapeHtmlLabel(value);
    }
}
