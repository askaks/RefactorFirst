package org.hjug.graphbuilder.metrics;

import java.util.Map;
import java.util.Optional;

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

    /** A metric's name in plain words, or the name itself when there is no plain form for it. */
    public static String plainName(String metricName) {
        return metricName == null ? "" : PLAIN_NAMES.getOrDefault(metricName, metricName);
    }
}
