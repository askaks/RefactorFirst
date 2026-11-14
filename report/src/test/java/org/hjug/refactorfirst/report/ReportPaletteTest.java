package org.hjug.refactorfirst.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hjug.refactorfirst.report.ReportPalette.Family;
import org.junit.jupiter.api.Test;

class ReportPaletteTest {

    @Test
    void givenEachFamily_whenAskedForItsColour_thenItIsTheOneInTheColourTable() {
        assertEquals("#c62828", Family.FIX_FIRST.hex());
        assertEquals("#c2188f", Family.CYCLE.hex());
        assertEquals("#f7cfe9", Family.CYCLE_PALE.hex());
        assertEquals("#6cac3a", Family.SMELL.hex());
        assertEquals("#d4a017", Family.GOD.hex());
        assertEquals("#6a3fa0", Family.BRAIN.hex());
        assertEquals("#8f6cc4", Family.BRAIN_LIGHT.hex());
        assertEquals("#12805c", Family.ENVY.hex());
        assertEquals("#7a4a1e", Family.COUPLING.hex());
        assertEquals("#5f6b7a", Family.QUIET.hex());
        assertEquals("#f08c00", Family.HOTSPOT.hex());
        assertEquals("#ffffff", Family.NEUTRAL.hex());
        assertEquals("#2b2b2b", Family.INK.hex());
    }

    @Test
    void givenEveryFamily_whenTheirColoursAreCompared_thenNoTwoShareOne() {
        Set<String> seen = new HashSet<>();
        for (Family family : Family.values()) {
            assertTrue(seen.add(family.hex()), family + " repeats a colour");
        }
    }

    @Test
    void givenAGodClassAndABrainClass_whenAskedForTheirFamilies_thenTheyDiffer() {
        assertEquals(Family.GOD, ReportPalette.forFinding("GOD"));
        assertNotEquals(
                ReportPalette.forFinding("GOD").hex(),
                ReportPalette.forFinding("BRAIN_CLASS").hex());
    }

    @Test
    void givenABrainClassAndABrainMethod_whenAskedForTheirFamilies_thenTheyAreTheDarkAndLightOfOneHue() {
        assertEquals(Family.BRAIN, ReportPalette.forFinding("BRAIN_CLASS"));
        assertEquals(Family.BRAIN_LIGHT, ReportPalette.forFinding("BRAIN_METHOD"));
    }

    @Test
    void givenTheCouplingSmells_whenAskedForTheirFamilies_thenTheyShareOne() {
        for (String anchor : List.of("INTENSIVE_COUPLING", "DISPERSED_COUPLING", "SHOTGUN_SURGERY")) {
            assertEquals(Family.COUPLING, ReportPalette.forFinding(anchor), anchor);
        }
    }

    @Test
    void givenACommonOrUnknownSmell_whenAskedForItsFamily_thenItIsTheQuietOne() {
        for (String anchor : List.of("DATA_CLASS", "RPB", "TB", "SIG_DUP", "SOMETHING_NEW")) {
            assertEquals(Family.QUIET, ReportPalette.forFinding(anchor), anchor);
        }
        assertEquals(Family.QUIET, ReportPalette.forFinding(null));
        assertEquals(Family.ENVY, ReportPalette.forFinding("FEATURE_ENVY"));
    }

    @Test
    void givenEveryFamily_whenItsColourIsRead_thenOnlySmellAndEnvyAreGreenAndNoneIsBlue() {
        for (Family family : Family.values()) {
            int r = Integer.parseInt(family.hex().substring(1, 3), 16);
            int g = Integer.parseInt(family.hex().substring(3, 5), 16);
            int b = Integer.parseInt(family.hex().substring(5, 7), 16);
            boolean green = g > r + 25 && g > b + 25;
            // Purple has more red than green; a blue has green at least level with red.
            boolean blue = b > r + 25 && b > g + 25 && g >= r;
            assertFalse(blue, family + " reads as blue, which is left to links");
            assertEquals(family == Family.SMELL || family == Family.ENVY, green, family + " green?");
        }
    }

    @Test
    void givenALevel_whenAskedForItsMark_thenAClassIsASquareAndAMethodIsADiamond() {
        assertEquals("&#9724;", ReportPalette.levelMark(false));
        assertEquals("&#9670;", ReportPalette.levelMark(true));
    }

    @Test
    void givenAFamily_whenAskedForItsOwnMark_thenOnlyTheNonSmellFamiliesHaveOne() {
        assertEquals("&#10227;", Family.CYCLE.mark());
        assertEquals("&#9873;", Family.FIX_FIRST.mark());
        assertEquals("&#128293;", Family.HOTSPOT.mark());
        for (Family family : Family.values()) {
            if (family.isSmell()) {
                assertEquals("", family.mark(), family + " takes the level mark instead");
            }
        }
        assertFalse(Family.SMELL.isSmell(), "the group colour is never a tag on one finding");
    }

    @Test
    void givenALightColour_whenTextIsWrittenOnIt_thenTheTextIsDark() {
        for (Family light : List.of(Family.GOD, Family.SMELL, Family.HOTSPOT, Family.CYCLE_PALE, Family.NEUTRAL)) {
            assertEquals(Family.INK.hex(), light.textOn(), light.toString());
        }
        for (Family dark : List.of(Family.BRAIN, Family.BRAIN_LIGHT, Family.ENVY, Family.COUPLING, Family.QUIET)) {
            assertEquals("#ffffff", dark.textOn(), dark.toString());
        }
    }

    @Test
    void givenEveryFamily_whenAskedForItsLabelAndMeaning_thenBothAreThere() {
        for (Family family : Family.values()) {
            assertFalse(family.label().isBlank(), family.toString());
            assertFalse(family.meaning().isBlank(), family.toString());
        }
    }
}
