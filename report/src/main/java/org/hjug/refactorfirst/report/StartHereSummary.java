package org.hjug.refactorfirst.report;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.hjug.cbc.RankedCycle;
import org.hjug.cbc.RankedDisharmony;
import org.hjug.refactorfirst.report.SimpleHtmlReport.DisharmonySpec;

/**
 * The first thing a reader sees: a few headline numbers and a short list of places to look first,
 * above the full tables. It adds no analysis of its own; it only chooses from what the report already ranks.
 *
 * <p>How the list is chosen, in order, up to {@value #MAX_PICKS} entries:
 * <ol>
 *   <li>up to {@value #MAX_CLASSES} classes that do too much: God Classes by priority, then Brain Classes;</li>
 *   <li>the largest class cycle;</li>
 *   <li>the hotspot: the most-changed file with a method-level finding that is not already listed;</li>
 *   <li>if there is room left: Brain Methods, then Feature Envy, then any remaining God or Brain Classes.</li>
 * </ol>
 * Data Classes and the coupling smells are never chosen for the list: there are usually far too many of them
 * to be a starting point. They stay one click away in the "everything else" row.
 */
final class StartHereSummary {

    static final int MAX_PICKS = 5;
    static final int MAX_CLASSES = 3;

    private static final String GOD = "GOD";
    private static final String BRAIN_CLASS = "BRAIN_CLASS";
    private static final String BRAIN_METHOD = "BRAIN_METHOD";
    private static final String FEATURE_ENVY = "FEATURE_ENVY";
    private static final String CYCLES_ANCHOR = "CYCLES";

    private StartHereSummary() {}

    /** One entry in the "places to look first" list. */
    record Pick(String name, String badge, String badgeColour, String why, String action, String anchor, String key) {}

    static String render(
            String projectName,
            int classesAnalysed,
            List<DisharmonySpec> specs,
            Map<String, List<RankedDisharmony>> findingsByAnchor,
            List<RankedCycle> cycles) {

        boolean anyFinding = findingsByAnchor.values().stream().anyMatch(list -> list != null && !list.isEmpty());
        if (!anyFinding && cycles.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<section id=\"START_HERE\" style=\"max-width:900px;margin:18px auto;padding:14px 18px;")
                .append("border:1px solid #d9d2c3;border-radius:12px;text-align:left;\">\n");
        sb.append("<h2 style=\"margin:0 0 10px;\">").append(esc(projectName)).append(": start here</h2>\n");

        sb.append("<div style=\"display:flex;gap:8px;flex-wrap:wrap;margin-bottom:12px;\">\n");
        tile(sb, count(findingsByAnchor, GOD) + count(findingsByAnchor, BRAIN_CLASS), "classes doing too much");
        tile(sb, cycles.size(), "dependency cycles");
        tile(sb, count(findingsByAnchor, BRAIN_METHOD), "over-long, tangled methods");
        tile(sb, classesAnalysed, "classes analysed");
        sb.append("</div>\n");

        List<Pick> picks = pick(specs, findingsByAnchor, cycles);
        if (!picks.isEmpty()) {
            sb.append("<p style=\"margin:0 0 4px;\"><strong>")
                    .append(picks.size() == 1 ? "The place to look first" : "Places to look first")
                    .append("</strong></p>\n<ol style=\"margin:0;padding-left:22px;\">\n");
            for (Pick p : picks) {
                sb.append("<li style=\"margin:8px 0;\"><a href=\"#")
                        .append(p.anchor())
                        .append("\" style=\"font-weight:bold;\">")
                        .append(esc(p.name()))
                        .append("</a> <span style=\"font-size:0.8em;font-weight:bold;color:#fff;background:")
                        .append(p.badgeColour())
                        .append(";border-radius:9px;padding:1px 8px;\">")
                        .append(esc(p.badge()))
                        .append("</span><br/><span>")
                        .append(esc(p.why()))
                        .append("</span><br/><span style=\"color:#2e7d32;\">")
                        .append(esc(p.action()))
                        .append("</span></li>\n");
            }
            sb.append("</ol>\n");
        }

        sb.append("<p style=\"margin:12px 0 0;font-size:0.9em;\">Everything found, in full below: ");
        for (DisharmonySpec spec : specs) {
            int n = count(findingsByAnchor, spec.anchorId());
            if (n > 0) {
                chip(sb, spec.anchorId(), spec.title() + " " + n);
            }
        }
        if (!cycles.isEmpty()) {
            chip(sb, CYCLES_ANCHOR, "Class Cycles " + cycles.size());
        }
        sb.append("</p>\n</section>\n");
        return sb.toString();
    }

    static List<Pick> pick(
            List<DisharmonySpec> specs,
            Map<String, List<RankedDisharmony>> findingsByAnchor,
            List<RankedCycle> cycles) {
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

        // 2. The largest cycle.
        cycles.stream()
                .max(Comparator.comparingInt((RankedCycle c) -> c.getVertexSet().size()))
                .ifPresent(c -> add(picks, keys, files, cyclePick(c)));

        // 3. The hotspot: the most-changed file with a method-level finding, not already listed.
        hotspot(specs, findingsByAnchor, files).ifPresent(p -> add(picks, keys, files, p));

        // 4. Room left: the next most serious things.
        for (RankedDisharmony d : byPriority(findingsByAnchor, BRAIN_METHOD)) {
            add(picks, keys, files, methodPick(d, spec(specs, BRAIN_METHOD), "#b26a00", typeLabel(d)));
        }
        for (RankedDisharmony d : byPriority(findingsByAnchor, FEATURE_ENVY)) {
            add(picks, keys, files, methodPick(d, spec(specs, FEATURE_ENVY), "#b26a00", typeLabel(d)));
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
                d.getDisharmonyType(),
                "#c62828",
                why,
                sentence(own.solution()),
                own.anchorId(),
                "file:" + d.getPath() + "#" + own.anchorId());
    }

    private static Pick methodPick(RankedDisharmony d, DisharmonySpec spec, String colour, String badge) {
        if (spec == null) {
            return null;
        }
        String name =
                simpleName(d.getClassName()) + (d.getMethodSignature() == null ? "" : "." + d.getMethodSignature());
        return new Pick(
                name,
                badge,
                colour,
                sentence(spec.problem()) + " " + changed(d),
                sentence(spec.solution()),
                spec.anchorId(),
                "file:" + d.getPath() + "#" + spec.anchorId() + ":" + d.getMethodSignature());
    }

    private static Pick cyclePick(RankedCycle cycle) {
        int classes = cycle.getVertexSet().size();
        return new Pick(
                simpleName(cycle.getCycleName()) + " cycle",
                "Cycle · " + classes + " classes",
                "#b26a00",
                "The largest knot: " + classes
                        + " classes that depend on each other in a loop, so none can change or be tested alone.",
                "See which relationships to cut to break it apart.",
                CYCLES_ANCHOR,
                "cycle:" + cycle.getCycleName());
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
        return Optional.ofNullable(methodPick(best, bestSpec, "#1565c0", "Hotspot · " + commitsText(commits(best))));
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
        String trimmed = text.strip().replaceAll("[\\s,;:.]+$", "");
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
