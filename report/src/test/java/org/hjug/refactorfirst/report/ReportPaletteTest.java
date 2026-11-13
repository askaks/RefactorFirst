package org.hjug.refactorfirst.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.hjug.refactorfirst.report.ReportPalette.Family;
import org.junit.jupiter.api.Test;

class ReportPaletteTest {

    @Test
    void givenEachFamily_whenAskedForItsColour_thenItIsTheOneInTheColourTable() {
        assertEquals("#c62828", Family.FIX_FIRST.hex());
        assertEquals("#c2188f", Family.CYCLE.hex());
        assertEquals("#f7cfe9", Family.CYCLE_PALE.hex());
        assertEquals("#6a3fa0", Family.CLASS_FINDING.hex());
        assertEquals("#8a5a00", Family.METHOD_FINDING.hex());
        assertEquals("#f08c00", Family.HOTSPOT.hex());
        assertEquals("#ffffff", Family.NEUTRAL.hex());
        assertEquals("#2b2b2b", Family.INK.hex());
    }

    @Test
    void givenAllFamilies_whenColoursAreCompared_thenNoTwoShareOne() {
        Set<String> seen = new HashSet<>();
        for (Family family : Family.values()) {
            assertTrue(seen.add(family.hex()), family + " repeats " + family.hex());
        }
    }

    @Test
    void givenAllFamilies_whenColoursAreRead_thenNoneIsBlueOrGreen() {
        // Blue is kept for links and green reads as "fine", so no finding may use either.
        for (Family family : Family.values()) {
            int rgb = Integer.parseInt(family.hex().substring(1), 16);
            int red = rgb >> 16 & 0xff;
            int green = rgb >> 8 & 0xff;
            int blue = rgb & 0xff;
            boolean grey = red == green && green == blue;
            boolean greenish = green > red && green >= blue;
            boolean bluish = blue > red && green >= red;
            assertFalse(!grey && (greenish || bluish), family + " reads as blue or green: " + family.hex());
        }
    }

    @Test
    void givenAFindingFamily_whenAskedForItsMark_thenSeriousIsSolidAndMilderIsHollow() {
        assertEquals("&#9724;", Family.CLASS_FINDING.mark(true));
        assertEquals("&#9723;", Family.CLASS_FINDING.mark(false));
        assertEquals("&#9670;", Family.METHOD_FINDING.mark(true));
        assertEquals("&#9671;", Family.METHOD_FINDING.mark(false));
    }

    @Test
    void givenAFamilyWithOneMark_whenAskedForItsMark_thenSeriousnessMakesNoDifference() {
        assertEquals("&#10227;", Family.CYCLE.mark(true));
        assertEquals("&#10227;", Family.CYCLE.mark(false));
        assertEquals("&#9873;", Family.FIX_FIRST.mark(true));
        assertEquals("&#128293;", Family.HOTSPOT.mark(false));
        assertEquals("", Family.NEUTRAL.mark(true));
        assertEquals("", Family.INK.mark(true));
    }

    @Test
    void givenAFamily_whenTextSitsOnItsColour_thenTheTextColourKeepsItReadable() {
        assertEquals("#ffffff", Family.FIX_FIRST.textOn());
        assertEquals("#ffffff", Family.CYCLE.textOn());
        assertEquals("#ffffff", Family.CLASS_FINDING.textOn());
        assertEquals("#ffffff", Family.METHOD_FINDING.textOn());
        // Orange and the pale backgrounds are too light for white text.
        assertEquals(Family.INK.hex(), Family.HOTSPOT.textOn());
        assertEquals(Family.INK.hex(), Family.CYCLE_PALE.textOn());
        assertEquals(Family.INK.hex(), Family.NEUTRAL.textOn());
    }

    @Test
    void givenAFindingLevel_whenAskedForItsFamily_thenMethodsAndClassesDiffer() {
        assertEquals(Family.METHOD_FINDING, ReportPalette.forFinding(true));
        assertEquals(Family.CLASS_FINDING, ReportPalette.forFinding(false));
    }

    @Test
    void givenAFindingType_whenAskedIfSerious_thenOnlyGodBrainClassAndBrainMethodAre() {
        assertTrue(ReportPalette.isSerious("GOD"));
        assertTrue(ReportPalette.isSerious("BRAIN_CLASS"));
        assertTrue(ReportPalette.isSerious("BRAIN_METHOD"));
        assertFalse(ReportPalette.isSerious("FEATURE_ENVY"));
        assertFalse(ReportPalette.isSerious("DATA_CLASS"));
        assertFalse(ReportPalette.isSerious(null));
    }

    @Test
    void givenAFamily_whenAskedForItsName_thenItIsWhatTheKeyWillSay() {
        assertEquals("Fix first", Family.FIX_FIRST.label());
        assertEquals("Cycle", Family.CYCLE.label());
        assertEquals("Class finding", Family.CLASS_FINDING.label());
        assertEquals("Method finding", Family.METHOD_FINDING.label());
        assertEquals("Hotspot", Family.HOTSPOT.label());
    }
}
