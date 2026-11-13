package org.hjug.refactorfirst.report;

import java.util.Set;

/**
 * Every colour and mark the report uses, named once.
 *
 * <p>Each kind of finding has its own colour, so a colour means one thing in every view. Red is
 * kept for "fix first" whatever the kind, because readers take red as a warning. Blue is left to
 * links and green is not used, because both read as "nothing wrong here".
 *
 * <p>Every family that marks a finding also has a mark, so the report still reads without colour.
 * Marks are HTML character references: they are only ever written into the page.
 */
final class ReportPalette {

    private ReportPalette() {}

    /** One meaning, with the colour and mark that stand for it everywhere in the report. */
    enum Family {
        /** The tool's priority 1 of any kind. */
        FIX_FIRST("Fix first", "#c62828", "&#9873;", "&#9873;"),
        /** Classes that use each other in a loop, and the link to cut to break the loop. */
        CYCLE("Cycle", "#c2188f", "&#10227;", "&#10227;"),
        /** The cycle colour as a background: flagged table cells, a package that is in a cycle. */
        CYCLE_PALE("Cycle, background", "#f7cfe9", "&#10227;", "&#10227;"),
        /** A finding about a whole class. */
        CLASS_FINDING("Class finding", "#6a3fa0", "&#9724;", "&#9723;"),
        /** A finding about one method. */
        METHOD_FINDING("Method finding", "#8a5a00", "&#9670;", "&#9671;"),
        /** The most-changed file that has a finding. */
        HOTSPOT("Hotspot", "#f08c00", "&#128293;", "&#128293;"),
        /** Nothing found. */
        NEUTRAL("Nothing found", "#ffffff", "", ""),
        /** Ordinary text, advice lines and outlines. */
        INK("Text", "#2b2b2b", "", "");

        private final String label;
        private final String hex;
        private final String solidMark;
        private final String hollowMark;

        Family(String label, String hex, String solidMark, String hollowMark) {
            this.label = label;
            this.hex = hex;
            this.solidMark = solidMark;
            this.hollowMark = hollowMark;
        }

        /** The name a reader sees in the key. */
        String label() {
            return label;
        }

        /** The colour as a CSS and GraphViz hex value. */
        String hex() {
            return hex;
        }

        /** The colour for text written on this colour: white, or dark where the colour is too light for white. */
        String textOn() {
            return this == HOTSPOT || this == CYCLE_PALE || this == NEUTRAL ? INK.hex : "#ffffff";
        }

        /** The mark, solid for a serious finding and hollow for a milder one where the family has both. */
        String mark(boolean serious) {
            return serious ? solidMark : hollowMark;
        }
    }

    /** Finding types serious enough for a solid tag; the rest get an outline. */
    private static final Set<String> SERIOUS = Set.of("GOD", "BRAIN_CLASS", "BRAIN_METHOD");

    /** The family of a finding, by whether it is about a method or a whole class. */
    static Family forFinding(boolean methodLevel) {
        return methodLevel ? Family.METHOD_FINDING : Family.CLASS_FINDING;
    }

    /** Whether a finding type, named by its report anchor id, is one of the serious ones. */
    static boolean isSerious(String anchorId) {
        return anchorId != null && SERIOUS.contains(anchorId);
    }
}
