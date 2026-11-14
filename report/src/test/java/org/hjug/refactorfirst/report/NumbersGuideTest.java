package org.hjug.refactorfirst.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class NumbersGuideTest {

    private static final List<String> LIMITED = List.of(
            "WMC",
            "ATFD",
            "TCC",
            "LOC",
            "CYCLO",
            "MAXNESTING",
            "NOAV",
            "LAA",
            "FDP",
            "CINT",
            "CDISP",
            "CM",
            "CC",
            "WOC",
            "PublicAttrsAndAccessors");

    @Test
    void givenEveryMeasureWithALimit_whenRendered_thenItHasAnEntryWithAllFourLines() {
        String html = NumbersGuide.render();

        for (String metric : LIMITED) {
            assertTrue(html.contains("id=\"" + NumbersGuide.anchor(metric) + "\""), metric);
            assertTrue(NumbersGuide.has(metric), metric);
        }
        int entries = count(html, "class=\"rf-why\"");
        assertEquals(LIMITED.size() + 2, entries, "the measures, plus cycles and priority");
        for (String line : List.of("What it measures", "Red flag", "Why that number", "What it may signal")) {
            assertEquals(entries, count(html, ">" + line + "<"), line);
        }
    }

    @Test
    void givenAMeasure_whenItsRedFlagIsWritten_thenTheValuesComeFromTheDetectorsLimits() {
        String html = NumbersGuide.render();
        String wmc = html.substring(html.indexOf("id=\"WHY_WMC\""), html.indexOf("id=\"WHY_ATFD\""));

        assertTrue(wmc.contains("average 14") && wmc.contains("high 31") && wmc.contains("very high 47"), wmc);
        assertTrue(wmc.contains("God Class: 47 or more"), wmc);
        assertTrue(
                wmc.contains("God Class &#183; Brain Class &#183; Data Class"), "says which findings use it: " + wmc);
    }

    @Test
    void givenCycles_whenRendered_thenTheEntryGivesNoNumberToAimFor() {
        String html = NumbersGuide.render();
        String cycles = html.substring(html.indexOf("id=\"WHY_CYCLES\""), html.indexOf("id=\"WHY_PRIORITY\""));
        assertTrue(cycles.contains("the target is none"), cycles);
    }

    @Test
    void givenTheSection_whenRendered_thenSourcesComeLastAsNumberedCitations() {
        String html = NumbersGuide.render();

        assertTrue(html.lastIndexOf("What it measures") < html.indexOf("<strong>Sources</strong>"), html);
        assertTrue(html.contains("1. Michele Lanza and Radu Marinescu"), html);
        assertTrue(html.contains("Object-Oriented Metrics in Practice"), html);
        assertTrue(html.contains("Prioritizing Design Debt Investment"), html);
        assertTrue(
                html.contains("<sup>[1]</sup>") && html.contains("<sup>[2]</sup>") && html.contains("<sup>[3]</sup>"));
        assertTrue(html.contains("not quotations"), "says the wording is the report's own");
    }

    @Test
    void givenALinkLandsOnAnEntry_whenRendered_thenTheEntryIsHighlightedWithoutScript() {
        String html = NumbersGuide.render();
        assertTrue(html.contains(".rf-why:target{background:#fff7d6;}"), html);
        assertFalse(html.contains("<script"), html);
    }

    @Test
    void givenAMeasureWithoutAnEntry_whenAskedForAWhyLink_thenThereIsNone() {
        assertEquals("", NumbersGuide.whyLink("Overridden", "why?"));
        assertFalse(NumbersGuide.has(null));
        assertTrue(NumbersGuide.whyLink("WMC", "why 47?").contains("href=\"#WHY_WMC\""));
    }

    @Test
    void givenTheGuide_whenItsSourceIsRead_thenItUsesNoReportClassThatCouldUseItBack() throws Exception {
        String source = Files.readString(Path.of("src/main/java/org/hjug/refactorfirst/report/NumbersGuide.java"));
        for (String other : List.of("SimpleHtmlReport", "StartHereSummary", "HtmlReport", "ReportKey")) {
            assertFalse(source.contains(other), "NumbersGuide must not use " + other);
        }
    }

    private static int count(String text, String part) {
        return text.split(Pattern.quote(part), -1).length - 1;
    }

    @Test
    void guideOpensWithOneTableOfEveryLimit() {
        String html = NumbersGuide.render();

        assertTrue(html.contains("All the limits"), html);
        assertTrue(html.contains("taken from the book"));
        assertTrue(html.contains("not an industry standard"));
        assertTrue(html.contains("<th>Number</th><th>Finding</th><th>Normal</th><th>Better</th>"));
        assertTrue(html.contains("<td>Brain Method</td><td>up to 65</td><td>lower</td>"), html);
        assertTrue(html.contains("<td>Feature Envy</td><td>counts when 5 or fewer</td><td>&#8212;</td>"), html);
    }

    @Test
    void priorityEntrySaysWhichWayEachNumberRuns() {
        String html = NumbersGuide.render();

        assertTrue(html.contains("higher means it changes more"));
        assertTrue(html.contains("higher means harder"));
        assertTrue(html.contains("1 is first"));
        assertTrue(html.contains("On a tie, the file with more commits goes first"));
    }

    @Test
    void nameLinkIsPlainTextWhenThereIsNoEntry() {
        assertEquals("methods", NumbersGuide.nameLink("NOM"));
        assertTrue(NumbersGuide.nameLink("WMC").startsWith("<a href=\"#WHY_WMC\">"));
    }
}
