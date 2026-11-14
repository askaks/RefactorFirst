package org.hjug.refactorfirst.report;

import java.util.ArrayList;
import java.util.List;
import org.hjug.graphbuilder.metrics.DisharmonyLimits;
import org.hjug.graphbuilder.metrics.DisharmonyTypes;
import org.hjug.refactorfirst.report.ReportPalette.Family;

/**
 * The "What the numbers mean" section: for each measure the report shows, what it measures, the
 * value that gets it flagged, the reasoning behind that value and what a flag may signal, with the
 * sources as numbered citations at the end.
 *
 * <p>The flagged values are read from {@link DisharmonyLimits}, so they cannot drift from the
 * detector. The explanations are this report's own plain-language reading; where a number's origin
 * is stated, it is what the detector's source cites.
 *
 * <p>Depends on the palette and the limits only, so it can never become part of a dependency cycle
 * among the report classes.
 */
final class NumbersGuide {

    private NumbersGuide() {}

    static final String SECTION_ANCHOR = "WHAT_THE_NUMBERS_MEAN";

    /** The key of the entry about cycles, which has no metric behind it. */
    static final String CYCLES = "CYCLES";

    /** The key of the entry about priority and "fix first". */
    static final String PRIORITY = "PRIORITY";

    /**
     * One measure.
     *
     * @param key the metric's name as the detector reports it, or {@link #CYCLES} or {@link #PRIORITY}
     * @param redFlag the flagged value in words, only for entries with no metric behind them
     * @param source the number of the source in {@link #SOURCES}, from 1
     */
    record Entry(String key, String title, String measures, String redFlag, String why, int source, String signals) {}

    private static final List<String> TYPES = List.of(
            DisharmonyTypes.GOD_CLASS,
            DisharmonyTypes.BRAIN_CLASS,
            DisharmonyTypes.DATA_CLASS,
            DisharmonyTypes.BRAIN_METHOD,
            DisharmonyTypes.FEATURE_ENVY,
            DisharmonyTypes.INTENSIVE_COUPLING,
            DisharmonyTypes.DISPERSED_COUPLING,
            DisharmonyTypes.SHOTGUN_SURGERY);

    private static final List<String> SOURCES = List.of(
            "Michele Lanza and Radu Marinescu, <a href=\"https://link.springer.com/book/10.1007/3-540-39538-5\">"
                    + "Object-Oriented Metrics in Practice</a>, Springer, 2006. The smells, their measures and "
                    + "the limits, as this tool's detector cites them.",
            "Nico Zazworka, Carolyn Seaman and Forrest Shull, "
                    + "<a href=\"https://dl.acm.org/doi/10.1145/1985362.1985372\">Prioritizing Design Debt Investment "
                    + "Opportunities</a>, 2011. Ranking a finding by how often its file changes.",
            "This tool's own cycle analysis: it builds the graph of which class uses which, finds the loops, and "
                    + "picks the links whose removal breaks them.");

    private static final List<Entry> ENTRIES = List.of(
            new Entry(
                    CYCLES,
                    "Cycles",
                    "Classes, or packages, that depend on each other in a loop.",
                    "Any cycle is shown. There is no acceptable number; the target is none.",
                    "A loop means no part of it can be changed, tested or reused without the others.",
                    3,
                    "Often one misplaced helper or one shared type. The number on each link to cut is how many "
                            + "uses have to move to break the loop: one or two is a stray reference, dozens is a "
                            + "real relationship that needs an interface between the two sides."),
            new Entry(
                    PRIORITY,
                    "Priority and \"fix first\"",
                    "Where a finding ranks among the others of its kind. It combines how far the numbers are over "
                            + "their limits with how often the file changes in the project's history.",
                    "Priority 1 of each kind carries the red \"fix first\" tag.",
                    "A smell in a file nobody touches costs little. The same smell in a file that changes every "
                            + "week is paid for every week.",
                    2,
                    "The place where cleaning up pays back soonest. A finding with a low priority can usually wait."),
            new Entry(
                    "WMC",
                    "Complexity of all methods in a class",
                    "Every branch point (if, loop, case) in every method of the class, added up. A rough count of "
                            + "the paths someone has to keep in their head.",
                    null,
                    "The source names three levels for this measure. A class is only flagged at the top one, so "
                            + "an ordinary large class is left alone.",
                    1,
                    "The class is doing several jobs at once. A change to one job risks breaking another, and it "
                            + "is hard to test in pieces."),
            new Entry(
                    "ATFD",
                    "Uses of other classes' data",
                    "How many times the class or method reads fields or properties that belong to other classes.",
                    null,
                    "Reaching into a few outside values is normal. Past a handful, the code is working on data it "
                            + "does not own.",
                    1,
                    "The behaviour lives in the wrong place. It probably belongs with the data it keeps reading."),
            new Entry(
                    "TCC",
                    "Cohesion, 0 to 1",
                    "The share of method pairs in a class that use at least one field in common. 1 means every "
                            + "method works on the same data; 0 means none do. Here lower is worse.",
                    null,
                    "Below a third, most methods have nothing in common with each other.",
                    1,
                    "Several unrelated classes are sharing one file. The groups of methods that do share fields "
                            + "are candidates to split out."),
            new Entry(
                    "LOC",
                    "Lines of code",
                    "Lines of code in the method, or in the whole class for a Brain Class.",
                    null,
                    "The method limit is half of what the source counts as a high number of lines for a whole "
                            + "class: one method should not be half a large class.",
                    1,
                    "The method does several steps that could each be named and tested alone."),
            new Entry(
                    "CYCLO",
                    "Branching",
                    "The number of separate paths through a method: one, plus one for every if, loop, case and "
                            + "similar decision.",
                    null,
                    "The limit is the source's high level for an average method, rounded up. It is strict on its "
                            + "own, which is why a method must also be long, deeply nested and use many variables "
                            + "before it is flagged.",
                    1,
                    "Too many cases handled in one place. Each branch needs its own test, and they rarely all "
                            + "have one."),
            new Entry(
                    "MAXNESTING",
                    "Nesting depth",
                    "How many blocks deep the code goes: an if inside a loop inside an if is three.",
                    null,
                    "Each level is one more condition a reader has to remember to know when a line runs.",
                    1,
                    "Tangled conditions. Returning early, or moving the inner block into its own method, usually "
                            + "flattens it."),
            new Entry(
                    "NOAV",
                    "Variables used",
                    "How many different variables a method reads or writes: its parameters, its locals and the "
                            + "fields it touches.",
                    null,
                    "The source ties its word \"many\" to how much a person can hold in short-term memory.",
                    1,
                    "The method is keeping track of too many things at once, often because it does more than one "
                            + "job."),
            new Entry(
                    "LAA",
                    "Share of the data it uses that is its own, 0 to 1",
                    "Of all the fields a method reads, the share that belong to its own class. Here lower is worse.",
                    null,
                    "Under a third means most of what the method works with belongs to someone else.",
                    1,
                    "The method is in the wrong class."),
            new Entry(
                    "FDP",
                    "Other classes whose data it uses",
                    "How many different classes the foreign data comes from.",
                    null,
                    "Envy is aimed at something: when the outside data comes from only a few classes, there is a "
                            + "clear place the method could move to.",
                    1,
                    "Together with the two measures above it, a method that would be at home in one of those "
                            + "classes."),
            new Entry(
                    "CINT",
                    "Calls to other classes' methods",
                    "How many different methods of other classes a method calls.",
                    null,
                    "A handful of outside calls is ordinary work. Past that, the method depends on a lot it does "
                            + "not control.",
                    1,
                    "A change in any of those methods can break this one."),
            new Entry(
                    "CDISP",
                    "Spread of those calls across classes, 0 to 1",
                    "Whether the outside calls go to a few classes (near 0) or to many (near 1).",
                    null,
                    "Half is the dividing line the source uses between two different problems: calls bunched on "
                            + "a few classes, and calls scattered across many.",
                    1,
                    "Bunched: the method is tied tightly to a few classes and may belong with them. Scattered: "
                            + "it reaches across the whole system and is touched by changes anywhere."),
            new Entry(
                    "CM",
                    "Methods that call it",
                    "How many methods outside its own class call this method.",
                    null,
                    "Past what a person can hold in mind, nobody can check every caller by hand.",
                    1,
                    "Changing this method means changing, or at least re-testing, all of them."),
            new Entry(
                    "CC",
                    "Classes that call it",
                    "How many different classes those callers are in.",
                    null,
                    "Many callers in one class is one conversation. The same callers spread over many classes is "
                            + "many.",
                    1,
                    "One change here fans out across the codebase."),
            new Entry(
                    "WOC",
                    "Share of methods that do real work, 0 to 1",
                    "Of a class's public members, the share that are real methods, not fields or plain get and "
                            + "set. Here lower is worse.",
                    null,
                    "Under a third, the class is mostly exposed data.",
                    1,
                    "The logic for this data lives in other classes. Often harmless for a plain record of data; "
                            + "worth a look when those other classes are also flagged."),
            new Entry(
                    "PublicAttrsAndAccessors",
                    "Public fields and accessors",
                    "How many public fields and plain get or set methods a class has.",
                    null,
                    "A few exposed values is normal. More than that, and the class is offering its insides to "
                            + "everyone.",
                    1,
                    "Other classes are doing this class's work for it."));

    /** The page anchor of a measure's entry. */
    static String anchor(String key) {
        return "WHY_" + key;
    }

    /** Whether there is an entry for a measure, so a link to it will land somewhere. */
    static boolean has(String key) {
        return key != null && ENTRIES.stream().anyMatch(e -> e.key().equals(key));
    }

    /** A small "why?" link to a measure's entry, or nothing when it has none. */
    static String whyLink(String key, String text) {
        return has(key)
                ? "<a href=\"#" + anchor(key) + "\" style=\"font-size:0.85em;font-weight:normal;\">" + text + "</a>"
                : "";
    }

    /** The whole section, ready to place in the page. */
    static String render() {
        StringBuilder sb = new StringBuilder();
        sb.append("<section id=\"")
                .append(SECTION_ANCHOR)
                .append("\" style=\"max-width:900px;margin:24px auto;padding:14px 18px;border:1px solid #d9d2c3;")
                .append("border-radius:12px;text-align:left;\">\n");
        sb.append("<style>.rf-why{padding:10px;border-bottom:1px solid #e4dfd3;border-radius:6px;}")
                .append(".rf-why:target{background:#fff7d6;}")
                .append(".rf-why-row{display:flex;gap:12px;margin-top:3px;font-size:0.9em;line-height:1.45;}")
                .append(".rf-why-row>span:first-child{flex:0 0 130px;color:#666;}")
                .append(".rf-why-row>span:last-child{flex:1;min-width:0;white-space:normal;}</style>\n");
        sb.append("<h2 style=\"margin:0 0 4px;\">What the numbers mean</h2>\n");
        sb.append("<p style=\"margin:0 0 8px;font-size:0.9em;\">For each number this report shows: what it measures, ")
                .append("the value that gets it flagged, why that value, and what a flag may signal. A finding is ")
                .append("only reported when all of its limits are crossed together.</p>\n");
        for (Entry entry : ENTRIES) {
            sb.append("<div class=\"rf-why\" id=\"")
                    .append(anchor(entry.key()))
                    .append("\"><strong>")
                    .append(entry.title())
                    .append("</strong>");
            String usedFor = String.join(" &#183; ", typesUsing(entry.key()));
            if (!usedFor.isEmpty()) {
                sb.append(" <span style=\"font-size:0.85em;color:#777;\">")
                        .append(usedFor)
                        .append("</span>");
            }
            row(sb, "What it measures", entry.measures());
            row(sb, "Red flag", entry.redFlag() != null ? entry.redFlag() : redFlag(entry.key()));
            row(sb, "Why that number", entry.why() + " <sup>[" + entry.source() + "]</sup>");
            row(sb, "What it may signal", entry.signals());
            sb.append("</div>\n");
        }
        sb.append("<p style=\"margin:12px 10px 4px;font-size:0.85em;line-height:1.6;\"><strong>Sources</strong><br/>");
        for (int i = 0; i < SOURCES.size(); i++) {
            sb.append(i + 1).append(". ").append(SOURCES.get(i)).append("<br/>");
        }
        sb.append("The explanations are this report's own plain-language reading, not quotations.</p>\n");
        sb.append("</section>\n");
        return sb.toString();
    }

    private static void row(StringBuilder sb, String label, String html) {
        sb.append("<div class=\"rf-why-row\"><span>")
                .append(label)
                .append("</span><span>")
                .append(html)
                .append("</span></div>");
    }

    private static String number(double value) {
        return value == Math.floor(value) ? String.valueOf((long) value) : String.valueOf(value);
    }

    /** The finding types whose rule has a limit for this measure. */
    private static List<String> typesUsing(String key) {
        List<String> types = new ArrayList<>();
        for (String type : TYPES) {
            if (DisharmonyLimits.of(type, key).isPresent()) {
                types.add(type);
            }
        }
        return types;
    }

    /** The named levels, then the flagged value for each finding type that uses the measure. */
    private static String redFlag(String key) {
        StringBuilder sb = new StringBuilder();
        for (DisharmonyLimits.Level level : DisharmonyLimits.levels(key)) {
            sb.append("<span style=\"display:inline-block;padding:0 7px;margin-right:4px;border-radius:3px;")
                    .append("background:#e8e8e8;font-size:0.9em;\">")
                    .append(level.name())
                    .append(" ")
                    .append(number(level.value()))
                    .append("</span>");
        }
        List<String> flags = new ArrayList<>();
        for (String type : TYPES) {
            DisharmonyLimits.of(type, key).ifPresent(limit -> flags.add(type + ": " + limit.describe()));
        }
        if (!flags.isEmpty()) {
            sb.append("<strong style=\"color:")
                    .append(Family.FIX_FIRST.hex())
                    .append(";\">Flagged</strong> ")
                    .append(String.join("; ", flags))
                    .append(".");
        }
        return sb.toString();
    }
}
