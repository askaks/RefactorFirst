package org.hjug.graphbuilder.metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * The limits behind each finding, in a form a report can show beside a value.
 *
 * <p>{@link DisharmonyDetector} decides what is a finding; this only repeats, per metric, the bound
 * that every finding of a type has to cross, using the detector's own constants so the two cannot
 * drift apart. Where a rule accepts either of two bounds, the looser one is given, because that is
 * the one every finding of the type is guaranteed to cross. Types whose rules do not reduce to one
 * bound per metric have no entry, and nothing is claimed for them.
 */
public final class DisharmonyLimits {

    private DisharmonyLimits() {}

    /** Which side of the value counts as a finding. */
    public enum Bound {
        OVER,
        AT_LEAST,
        UNDER,
        AT_MOST
    }

    /** One bound: a metric counts towards a finding when its value is on the given side of it. */
    public record Limit(Bound bound, double value) {

        /** Whether a metric value is on the finding side of this limit. */
        public boolean crossedBy(double metricValue) {
            return switch (bound) {
                case OVER -> metricValue > value;
                case AT_LEAST -> metricValue >= value;
                case UNDER -> metricValue < value;
                case AT_MOST -> metricValue <= value;
            };
        }

        /** The limit in words, to follow "flagged": "over 5", "47 or more", "under 0.33", "5 or fewer". */
        public String describe() {
            String number = value == Math.floor(value) ? String.valueOf((long) value) : String.valueOf(value);
            return switch (bound) {
                case OVER -> "over " + number;
                case AT_LEAST -> number + " or more";
                case UNDER -> "under " + number;
                case AT_MOST -> number + " or fewer";
            };
        }

        /** The other side of the limit, in words: what is not flagged. "up to 65", "under 4", "0.33 or more". */
        public String normalText() {
            String number = value == Math.floor(value) ? String.valueOf((long) value) : String.valueOf(value);
            return switch (bound) {
                case OVER -> "up to " + number;
                case AT_LEAST -> "under " + number;
                case UNDER -> number + " or more";
                case AT_MOST -> "over " + number;
            };
        }
    }

    private static Limit over(double value) {
        return new Limit(Bound.OVER, value);
    }

    private static Limit atLeast(double value) {
        return new Limit(Bound.AT_LEAST, value);
    }

    private static Limit under(double value) {
        return new Limit(Bound.UNDER, value);
    }

    private static Limit atMost(double value) {
        return new Limit(Bound.AT_MOST, value);
    }

    private static final Map<String, Map<String, Limit>> LIMITS = Map.of(
            DisharmonyTypes.GOD_CLASS,
            Map.of(
                    "ATFD", over(DisharmonyDetector.GOD_CLASS_ATFD_FEW),
                    "WMC", atLeast(DisharmonyDetector.GOD_CLASS_WMC_VERY_HIGH),
                    "TCC", under(DisharmonyDetector.ONE_THIRD)),
            DisharmonyTypes.DATA_CLASS,
            Map.of(
                    "WOC", under(DisharmonyDetector.ONE_THIRD),
                    "PublicAttrsAndAccessors", over(DisharmonyDetector.FEW),
                    "WMC", under(DisharmonyDetector.WMC_VERY_HIGH)),
            DisharmonyTypes.BRAIN_METHOD,
            Map.of(
                    "LOC", over(DisharmonyDetector.BRAIN_METHOD_LOC),
                    "CYCLO", atLeast(DisharmonyDetector.CYCLO_HIGH),
                    "MAXNESTING", atLeast(DisharmonyDetector.MAXNESTING_DEEP),
                    "NOAV", over(DisharmonyDetector.NOAV_MANY)),
            DisharmonyTypes.BRAIN_CLASS,
            Map.of(
                    "BrainMethods", atLeast(1),
                    "LOC", atLeast(DisharmonyDetector.LOC_CLASS_VERY_HIGH),
                    "WMC", atLeast(DisharmonyDetector.WMC_VERY_HIGH),
                    "TCC", under(DisharmonyDetector.HALF)),
            DisharmonyTypes.FEATURE_ENVY,
            Map.of(
                    "ATFD", over(DisharmonyDetector.FEW),
                    "LAA", under(DisharmonyDetector.ONE_THIRD),
                    "FDP", atMost(DisharmonyDetector.FEW)),
            DisharmonyTypes.INTENSIVE_COUPLING,
            Map.of(
                    "CINT", over(DisharmonyDetector.FEW),
                    "CDISP", under(DisharmonyDetector.HALF),
                    "MAXNESTING", over(DisharmonyDetector.SHALLOW)),
            DisharmonyTypes.DISPERSED_COUPLING,
            Map.of(
                    "CINT", over(DisharmonyDetector.SHORT_MEMORY_CAP),
                    "CDISP", atLeast(DisharmonyDetector.HALF),
                    "MAXNESTING", over(DisharmonyDetector.SHALLOW)),
            DisharmonyTypes.SHOTGUN_SURGERY,
            Map.of(
                    "CM", over(DisharmonyDetector.SHORT_MEMORY_CAP),
                    "CC", over(DisharmonyDetector.MANY)));

    private static final Map<String, String> PLAIN_NAMES = Map.ofEntries(
            Map.entry("ATFD", "uses of other classes' data"),
            Map.entry("WMC", "complexity of all its methods"),
            Map.entry("TCC", "cohesion (0 to 1)"),
            Map.entry("WOC", "share of methods that do real work (0 to 1)"),
            Map.entry("PublicAttrsAndAccessors", "public fields and accessors"),
            Map.entry("LOC", "lines of code"),
            Map.entry("CYCLO", "branching"),
            Map.entry("MAXNESTING", "nesting depth"),
            Map.entry("NOAV", "variables used"),
            Map.entry("BrainMethods", "brain methods"),
            Map.entry("LAA", "share of the data it uses that is its own (0 to 1)"),
            Map.entry("FDP", "other classes whose data it uses"),
            Map.entry("CINT", "calls to other classes' methods"),
            Map.entry("CDISP", "spread of those calls across classes (0 to 1)"),
            Map.entry("CM", "methods that call it"),
            Map.entry("CC", "classes that call it"),
            Map.entry("NOM", "methods"),
            Map.entry("AMW", "average complexity per method"),
            Map.entry("NProtM", "protected members in its parent"),
            Map.entry("BUR", "share of the parent's members it uses (0 to 1)"),
            Map.entry("BOvR", "share of its methods that override the parent's (0 to 1)"),
            Map.entry("NAS", "methods it adds beyond its parent's"),
            Map.entry("PNAS", "share of its methods that are additions (0 to 1)"),
            Map.entry("Overridden", "methods it overrides"),
            Map.entry("NumberOfExtensionFunctions", "extension functions"),
            Map.entry("ExtensionReceiverTypes", "types it adds extension functions to"),
            Map.entry("PermittedSubtypes", "permitted subtypes"),
            Map.entry("HasExplicitLogic", "has logic of its own (1 = yes)"));

    /** A named point on a metric's scale, in the word the detector's source uses for it. */
    public record Level(String name, double value) {}

    private static final Map<String, List<Level>> LEVELS = Map.of(
            "WMC",
                    List.of(
                            new Level("average", DisharmonyDetector.WMC_AVERAGE),
                            new Level("high", DisharmonyDetector.WMC_HIGH),
                            new Level("very high", DisharmonyDetector.WMC_VERY_HIGH)),
            "NOM",
                    List.of(
                            new Level("average", DisharmonyDetector.NOM_AVERAGE),
                            new Level("high", DisharmonyDetector.NOM_HIGH)),
            "ATFD", List.of(new Level("few", DisharmonyDetector.FEW)),
            "NOAV", List.of(new Level("many", DisharmonyDetector.NOAV_MANY)),
            "MAXNESTING", List.of(new Level("deep", DisharmonyDetector.MAXNESTING_DEEP)),
            "TCC",
                    List.of(
                            new Level("a third", DisharmonyDetector.ONE_THIRD),
                            new Level("half", DisharmonyDetector.HALF)));

    /**
     * The named levels on a metric's scale, lowest first.
     *
     * @return the levels, or an empty list when none are named for the metric
     */
    public static List<Level> levels(String metricName) {
        return metricName == null ? List.of() : LEVELS.getOrDefault(metricName, List.of());
    }

    /**
     * The limit a metric has to cross for a finding of the given type.
     *
     * @param disharmonyType one of {@link DisharmonyTypes}
     * @param metricName the metric's name as the detector reports it, for example {@code WMC}
     * @return the limit, or empty when the type's rule has no single bound for that metric
     */
    public static Optional<Limit> of(String disharmonyType, String metricName) {
        Map<String, Limit> forType = disharmonyType == null ? null : LIMITS.get(disharmonyType);
        return forType == null || metricName == null ? Optional.empty() : Optional.ofNullable(forType.get(metricName));
    }

    /** Numbers where a higher value is the good side. Every other number with a good side is better lower. */
    private static final Set<String> HIGHER_IS_BETTER = Set.of("TCC", "LAA", "WOC");

    /**
     * Limits that describe a finding without being bad in themselves: a Data Class has low complexity, Feature
     * Envy takes from few classes, coupling is spread out or not. Crossing these is not "over the limit".
     */
    private static final Set<String> NOT_BAD_IN_ITSELF = Set.of(
            DisharmonyTypes.DATA_CLASS + "/WMC",
            DisharmonyTypes.FEATURE_ENVY + "/FDP",
            DisharmonyTypes.INTENSIVE_COUPLING + "/CDISP",
            DisharmonyTypes.DISPERSED_COUPLING + "/CDISP",
            DisharmonyTypes.BRAIN_CLASS + "/BrainMethods");

    /** The finding types that have limits, in the order the report shows them. */
    private static final List<String> TYPE_ORDER = List.of(
            DisharmonyTypes.GOD_CLASS,
            DisharmonyTypes.DATA_CLASS,
            DisharmonyTypes.BRAIN_CLASS,
            DisharmonyTypes.BRAIN_METHOD,
            DisharmonyTypes.FEATURE_ENVY,
            DisharmonyTypes.INTENSIVE_COUPLING,
            DisharmonyTypes.DISPERSED_COUPLING,
            DisharmonyTypes.SHOTGUN_SURGERY);

    private static final Map<String, String> RULES = Map.of(
            DisharmonyTypes.GOD_CLASS,
            "A class is a God Class when all three numbers are past their limit.",
            DisharmonyTypes.DATA_CLASS,
            "A class is a Data Class when few of its methods do real work and it exposes many fields while staying "
                    + "simple: more than " + DisharmonyDetector.FEW + " public fields and accessors with complexity "
                    + "under " + DisharmonyDetector.WMC_HIGH + ", or more than " + DisharmonyDetector.MANY
                    + " with complexity under " + DisharmonyDetector.WMC_VERY_HIGH + ".",
            DisharmonyTypes.BRAIN_CLASS,
            "A class is a Brain Class when its complexity is " + DisharmonyDetector.WMC_VERY_HIGH
                    + " or more and its cohesion is under " + DisharmonyDetector.HALF
                    + ", and it has either several Brain Methods and " + DisharmonyDetector.LOC_CLASS_VERY_HIGH
                    + " lines or more, or one Brain Method with " + (DisharmonyDetector.LOC_CLASS_VERY_HIGH * 2)
                    + " lines and complexity " + (DisharmonyDetector.WMC_VERY_HIGH * 2)
                    + " or more. A God Class is never also listed as a Brain Class.",
            DisharmonyTypes.BRAIN_METHOD,
            "A method is a Brain Method when all four numbers are past their limit.",
            DisharmonyTypes.FEATURE_ENVY,
            "A method has Feature Envy when all three hold: it uses a lot of other classes' data, little of its "
                    + "own, and takes it from few classes.",
            DisharmonyTypes.INTENSIVE_COUPLING,
            "A method has Intensive Coupling when it is nested code that makes many calls into few classes: more "
                    + "than " + DisharmonyDetector.SHORT_MEMORY_CAP + " calls with a spread under "
                    + DisharmonyDetector.HALF + ", or more than " + DisharmonyDetector.FEW
                    + " calls with a spread under " + DisharmonyDetector.ONE_QUARTER + ".",
            DisharmonyTypes.DISPERSED_COUPLING,
            "A method has Dispersed Coupling when all three hold: many calls to other classes, spread across many "
                    + "of them, from nested code.",
            DisharmonyTypes.SHOTGUN_SURGERY,
            "A method causes Shotgun Surgery when both hold: many methods call it, and they sit in many classes.");

    /**
     * How a finding type's numbers combine, as one plain sentence written from the detector's rule.
     *
     * @return the sentence, or empty for a type with no limits here
     */
    public static Optional<String> rule(String disharmonyType) {
        return disharmonyType == null ? Optional.empty() : Optional.ofNullable(RULES.get(disharmonyType));
    }

    /** One limit with the finding type and metric it belongs to. */
    public record Entry(String disharmonyType, String metricName, Limit limit) {}

    /** Every limit, by finding type in report order and then by metric name. */
    public static List<Entry> all() {
        List<Entry> entries = new ArrayList<>();
        for (String type : TYPE_ORDER) {
            new TreeMap<>(LIMITS.get(type)).forEach((metric, limit) -> entries.add(new Entry(type, metric, limit)));
        }
        return entries;
    }

    /** Whether a higher value of this metric is the good side. */
    public static boolean higherIsBetter(String metricName) {
        return metricName != null && HIGHER_IS_BETTER.contains(metricName);
    }

    /**
     * Whether a value past this limit is bad in itself, so that "over the limit" is the right thing to say.
     * False for limits that only describe the finding, and when there is no limit.
     */
    public static boolean crossingIsBad(String disharmonyType, String metricName) {
        return of(disharmonyType, metricName).isPresent()
                && !NOT_BAD_IN_ITSELF.contains(disharmonyType + "/" + metricName);
    }

    /** A metric's name in plain words, or the name itself when there is no plain form for it. */
    public static String plainName(String metricName) {
        return metricName == null ? "" : PLAIN_NAMES.getOrDefault(metricName, metricName);
    }
}
