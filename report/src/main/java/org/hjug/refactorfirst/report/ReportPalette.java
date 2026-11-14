package org.hjug.refactorfirst.report;

import java.util.Map;

/**
 * Every colour and mark the report uses, named once.
 *
 * <p>A colour says what kind of problem a finding is, so a God Class and a Brain Class look
 * different, and a Brain Class and a Brain Method share one hue. The mark in front of a tag says
 * the level: a square for a class, a diamond for a method. Rare findings get the loud colours and
 * common ones the quiet colours, so a loud colour stays a signal.
 *
 * <p>Red is kept for "fix first" and for how far a number is over its limit, because readers take
 * red as a warning. Blue is left to links. Green has exactly two uses: smells as a group, and
 * Feature Envy in a darker green.
 *
 * <p>Marks are HTML character references: they are only ever written into the page.
 */
final class ReportPalette {

    private ReportPalette() {}

    /** One meaning, with the colour and mark that stand for it everywhere in the report. */
    enum Family {
        /** The tool's priority 1 of any kind. */
        FIX_FIRST("Fix first", "the tool's priority 1 of its kind", "#c62828", "&#9873;"),
        /** Classes that use each other in a loop, and the link to cut to break the loop. */
        CYCLE(
                "Cycle",
                "classes that use each other in a loop, so none can change or be tested alone",
                "#c2188f",
                "&#10227;"),
        /** The cycle colour as a background: flagged table cells, a package that is in a cycle. */
        CYCLE_PALE("Cycle, background", "a package that is in a package cycle", "#f7cfe9", "&#10227;"),
        /** Smells as a group: the tab, its count, the border of a smells panel. */
        SMELL("Smell", "a code smell of any kind", "#6cac3a", ""),
        /** A class that does too much and reaches into other classes' data. Rare, so the loudest colour. */
        GOD("God Class", "a class that does too much", "#d4a017", ""),
        /** A class too complex to hold in your head. */
        BRAIN("Brain Class", "a class that is too complex", "#6a3fa0", ""),
        /** The same problem at the size of one method, in a lighter shade of the same colour. */
        BRAIN_LIGHT("Brain Method", "a method that is too complex", "#8f6cc4", ""),
        /** A method more interested in another class's data than its own. */
        ENVY("Feature Envy", "a method that works on another class's data", "#12805c", ""),
        /** The coupling smells. Common, so a quiet colour. */
        COUPLING("Coupling", "too many calls to or from other classes", "#7a4a1e", ""),
        /** Data classes, the inheritance smells and anything without a colour of its own. */
        QUIET("Other smell", "a milder, more common smell", "#5f6b7a", ""),
        /** The most-changed file that has a finding. */
        HOTSPOT("Hotspot", "the most-changed file that has a finding", "#f08c00", "&#128293;"),
        /** Nothing found. */
        NEUTRAL("Nothing found", "nothing found", "#ffffff", ""),
        /** Ordinary text, advice lines and outlines. */
        INK("Text", "ordinary text and outlines", "#2b2b2b", "");

        private final String label;
        private final String meaning;
        private final String hex;
        private final String mark;

        Family(String label, String meaning, String hex, String mark) {
            this.label = label;
            this.meaning = meaning;
            this.hex = hex;
            this.mark = mark;
        }

        /** The name a reader sees in the key. */
        String label() {
            return label;
        }

        /** What it means, as the key says it after the name. */
        String meaning() {
            return meaning;
        }

        /** The colour as a CSS and GraphViz hex value. */
        String hex() {
            return hex;
        }

        /** The colour for text written on this colour: dark where the colour is too light for white. */
        String textOn() {
            return switch (this) {
                case HOTSPOT, CYCLE_PALE, NEUTRAL, GOD, SMELL -> INK.hex;
                default -> "#ffffff";
            };
        }

        /** The family's own mark; empty for the smell families, whose mark says the level instead. */
        String mark() {
            return mark;
        }

        /** Whether this family is one kind of smell, so its tag leads with the level mark. */
        boolean isSmell() {
            return switch (this) {
                case GOD, BRAIN, BRAIN_LIGHT, ENVY, COUPLING, QUIET -> true;
                default -> false;
            };
        }
    }

    private static final Map<String, Family> BY_ANCHOR = Map.of(
            "GOD", Family.GOD,
            "BRAIN_CLASS", Family.BRAIN,
            "BRAIN_METHOD", Family.BRAIN_LIGHT,
            "FEATURE_ENVY", Family.ENVY,
            "INTENSIVE_COUPLING", Family.COUPLING,
            "DISPERSED_COUPLING", Family.COUPLING,
            "SHOTGUN_SURGERY", Family.COUPLING);

    /** The family of a finding type, named by its report anchor id. Types without their own colour are quiet. */
    static Family forFinding(String anchorId) {
        return anchorId == null ? Family.QUIET : BY_ANCHOR.getOrDefault(anchorId, Family.QUIET);
    }

    /** The mark that says the level: a square for a class, a diamond for a method. */
    static String levelMark(boolean methodLevel) {
        return methodLevel ? "&#9670;" : "&#9724;";
    }
}
