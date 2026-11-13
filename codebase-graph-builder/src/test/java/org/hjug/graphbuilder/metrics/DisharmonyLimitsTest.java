package org.hjug.graphbuilder.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hjug.graphbuilder.metrics.DisharmonyLimits.Bound;
import org.hjug.graphbuilder.metrics.DisharmonyLimits.Limit;
import org.junit.jupiter.api.Test;

class DisharmonyLimitsTest {

    @Test
    void givenAGodClass_whenItsLimitsAreRead_thenTheyAreTheDetectorsOwn() {
        assertEquals(
                new Limit(Bound.OVER, DisharmonyDetector.GOD_CLASS_ATFD_FEW),
                DisharmonyLimits.of(DisharmonyTypes.GOD_CLASS, "ATFD").orElseThrow());
        assertEquals(
                new Limit(Bound.AT_LEAST, DisharmonyDetector.GOD_CLASS_WMC_VERY_HIGH),
                DisharmonyLimits.of(DisharmonyTypes.GOD_CLASS, "WMC").orElseThrow());
        assertEquals(
                new Limit(Bound.UNDER, DisharmonyDetector.ONE_THIRD),
                DisharmonyLimits.of(DisharmonyTypes.GOD_CLASS, "TCC").orElseThrow());
    }

    @Test
    void givenTheSameMetricInTwoTypes_whenItsLimitIsRead_thenEachTypeHasItsOwn() {
        assertEquals(
                Bound.UNDER,
                DisharmonyLimits.of(DisharmonyTypes.INTENSIVE_COUPLING, "CDISP")
                        .orElseThrow()
                        .bound());
        assertEquals(
                Bound.AT_LEAST,
                DisharmonyLimits.of(DisharmonyTypes.DISPERSED_COUPLING, "CDISP")
                        .orElseThrow()
                        .bound());
    }

    @Test
    void givenATypeOrMetricWithNoSingleBound_whenItsLimitIsRead_thenNothingIsClaimed() {
        assertTrue(DisharmonyLimits.of(DisharmonyTypes.REFUSED_PARENT_BEQUEST, "BUR")
                .isEmpty());
        assertTrue(DisharmonyLimits.of(DisharmonyTypes.GOD_CLASS, "LOC").isEmpty());
        assertTrue(DisharmonyLimits.of(null, "WMC").isEmpty());
        assertTrue(DisharmonyLimits.of(DisharmonyTypes.GOD_CLASS, null).isEmpty());
    }

    @Test
    void givenALimit_whenAValueIsCompared_thenEachBoundCountsItsOwnSide() {
        assertTrue(new Limit(Bound.OVER, 5).crossedBy(6));
        assertFalse(new Limit(Bound.OVER, 5).crossedBy(5));
        assertTrue(new Limit(Bound.AT_LEAST, 47).crossedBy(47));
        assertTrue(new Limit(Bound.UNDER, 0.33).crossedBy(0.2));
        assertFalse(new Limit(Bound.UNDER, 0.33).crossedBy(0.33));
        assertTrue(new Limit(Bound.AT_MOST, 5).crossedBy(5));
    }

    @Test
    void givenALimit_whenDescribed_thenItReadsAsWordsAfterFlagged() {
        assertEquals("over 5", new Limit(Bound.OVER, 5).describe());
        assertEquals("47 or more", new Limit(Bound.AT_LEAST, 47).describe());
        assertEquals("under 0.33", new Limit(Bound.UNDER, 0.33).describe());
        assertEquals("5 or fewer", new Limit(Bound.AT_MOST, 5).describe());
    }

    @Test
    void givenAMetric_whenAskedForItsPlainName_thenAbbreviationsAreSpelledOut() {
        assertEquals("complexity of all its methods", DisharmonyLimits.plainName("WMC"));
        assertEquals("uses of other classes' data", DisharmonyLimits.plainName("ATFD"));
        assertEquals("share of the parent's members it uses (0 to 1)", DisharmonyLimits.plainName("BUR"));
        assertEquals("SomethingNew", DisharmonyLimits.plainName("SomethingNew"), "an unknown metric keeps its name");
    }
}
