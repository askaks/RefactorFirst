package org.hjug.refactorfirst.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hjug.cbc.RankedDisharmony;
import org.hjug.git.ScmLogInfo;
import org.hjug.graphbuilder.metrics.DisharmonyMetric;
import org.hjug.graphbuilder.metrics.DisharmonyMetric.Direction;
import org.hjug.graphbuilder.metrics.DisharmonyTypes;
import org.hjug.metrics.DisharmonyInstance;
import org.hjug.refactorfirst.report.ReportPalette.Family;
import org.hjug.refactorfirst.report.SimpleHtmlReport.DisharmonySpec;
import org.junit.jupiter.api.Test;

/** The findings tables: each row says, in numbers and words, what made it a finding. */
class FindingsTableTest {

    private final SimpleHtmlReport report = new SimpleHtmlReport();

    @Test
    void givenAGodClass_whenItsNumbersAreShown_thenEachIsACellWithHowFarPastTheLimit() {
        String cells = FindingsTable.metricCells(godClass(1));

        assertEquals(3, cells.split("<td", -1).length - 1, cells);
        assertTrue(cells.contains("<strong>9</strong>"), cells);
        assertTrue(cells.contains("1.8&times; the limit"), cells);
        assertTrue(cells.contains("<strong>61</strong>"), cells);
        assertTrue(cells.contains("just over"), cells);
        assertTrue(cells.contains("<strong>0.2</strong>"), cells);
        assertTrue(cells.contains("0.13 short"), cells);
        assertFalse(cells.contains("flagged"), cells);
        assertFalse(cells.contains("why?"), cells);
    }

    @Test
    void givenAFindingTypeWithNoLimits_whenItsNumbersAreShown_thenNoLimitIsInvented() {
        RankedDisharmony refused =
                ranked(DisharmonyTypes.REFUSED_PARENT_BEQUEST, 1, new DisharmonyMetric("NOM", 12, Direction.ASCENDING));

        String cells = FindingsTable.metricCells(refused);

        assertTrue(cells.contains("<strong>12</strong>"), cells);
        assertFalse(cells.contains("limit"), cells);
        assertEquals("", FindingsTable.normalLine("RPB", refused));
        assertEquals("", FindingsTable.ruleLine(DisharmonyTypes.REFUSED_PARENT_BEQUEST));
    }

    @Test
    void givenALimitThatIsNotBadInItself_whenShown_thenItGetsNoRedTextAndReadsCountsWhen() {
        RankedDisharmony envy = ranked(
                DisharmonyTypes.FEATURE_ENVY,
                1,
                new DisharmonyMetric("ATFD", 10, Direction.ASCENDING),
                new DisharmonyMetric("LAA", 0.17, Direction.DESCENDING),
                new DisharmonyMetric("FDP", 1, Direction.ASCENDING));

        String cells = FindingsTable.metricCells(envy);
        String line = FindingsTable.normalLine("FEATURE_ENVY", envy);

        assertEquals(2, cells.split("color:" + Family.FIX_FIRST.hex(), -1).length - 1, cells);
        assertTrue(line.contains("counts when 5 or fewer"), line);
        assertTrue(line.contains("0.33 or more, higher is better"), line);
    }

    @Test
    void givenABrainMethod_whenTheNormalLineIsWritten_thenEachLimitIsThereOnceWithItsNameAsTheLink() {
        RankedDisharmony brain = ranked(
                DisharmonyTypes.BRAIN_METHOD,
                1,
                new DisharmonyMetric("LOC", 205, Direction.ASCENDING),
                new DisharmonyMetric("CYCLO", 26, Direction.ASCENDING),
                new DisharmonyMetric("MAXNESTING", 6, Direction.ASCENDING),
                new DisharmonyMetric("NOAV", 19, Direction.ASCENDING));

        String line = FindingsTable.normalLine("BRAIN_METHOD", brain);

        assertTrue(line.contains("<a href=\"#WHY_LOC\">lines of code</a> up to 65"), line);
        assertTrue(line.contains("under 4"), line);
        assertTrue(line.contains("lower is better for all four"), line);
        assertTrue(line.contains("the tool's limits, taken from the book"), line);
        assertFalse(line.contains("why?"), line);
    }

    @Test
    void givenAGodClass_whenTheNormalLineIsWritten_thenTheNumberThatRunsTheOtherWaySaysSo() {
        String line = FindingsTable.normalLine("GOD", godClass(1));

        assertTrue(line.contains("0.33 or more, higher is better"), line);
        assertFalse(line.contains("for all three"), line);
    }

    @Test
    void givenEverySmellWithLimits_whenAskedForItsRule_thenThereIsOneSentence() {
        for (String type : List.of(
                DisharmonyTypes.GOD_CLASS,
                DisharmonyTypes.DATA_CLASS,
                DisharmonyTypes.BRAIN_CLASS,
                DisharmonyTypes.BRAIN_METHOD,
                DisharmonyTypes.FEATURE_ENVY,
                DisharmonyTypes.INTENSIVE_COUPLING,
                DisharmonyTypes.DISPERSED_COUPLING,
                DisharmonyTypes.SHOTGUN_SURGERY)) {
            assertTrue(FindingsTable.ruleLine(type).contains(type), type);
        }
        assertTrue(FindingsTable.ruleLine(DisharmonyTypes.GOD_CLASS).contains("<strong>all three</strong>"));
        assertTrue(FindingsTable.ruleLine(DisharmonyTypes.DATA_CLASS).contains("under 31"));
        assertTrue(FindingsTable.ruleLine(DisharmonyTypes.BRAIN_CLASS).contains("390 lines"));
    }

    @Test
    void givenTheDefaultTable_whenRendered_thenTheNumbersAreColumnsUnderOneColouredHeading() {
        String html = report.renderDisharmonyInfo("", spec("GOD"), false, List.of(godClass(1), godClass(2)));

        assertFalse(html.contains("Why it is here"), html);
        assertTrue(html.contains("colspan=\"3\""), html);
        assertTrue(html.contains("What makes it a God Class"), html);
        assertTrue(html.contains("border-top:7px solid " + Family.GOD.hex()), html);
        assertTrue(html.contains(">Uses of other classes' data</th>"), html);
        for (String name : List.of(
                "Priority", "Change Proneness Rank", "Effort Rank", "Most Recent Commit Date", "Commit Count")) {
            assertTrue(html.contains("<th rowspan=\"2\">" + name + "</th>"), name);
        }
        assertEquals(1, html.split("fix first</span>", -1).length - 1, "only priority 1 is fix first");
        assertTrue(html.contains("What counts as normal"), html);
        assertTrue(html.contains("href=\"#WHY_PRIORITY\""), html);
        assertTrue(html.contains("<h1><span style=\"color:" + Family.GOD.hex()), html);
        assertFalse(html.contains("flagged"), html);
    }

    @Test
    void givenTheDetailedTable_whenRendered_thenEachMetricValueCarriesItsLimit() {
        String html = report.renderDisharmonyInfo("", spec("GOD"), true, List.of(godClass(1)));

        assertTrue(html.contains("61 <span"), html);
        assertTrue(html.contains("47 or more"), html);
    }

    @Test
    void givenALongRatio_whenShown_thenItIsShownToTwoPlaces() {
        RankedDisharmony brain =
                ranked(DisharmonyTypes.BRAIN_CLASS, 1, new DisharmonyMetric("TCC", 1.0 / 3, Direction.DESCENDING));

        assertTrue(FindingsTable.metricCells(brain).contains("<strong>0.33</strong>"));
    }

    private static RankedDisharmony godClass(int priority) {
        return ranked(
                DisharmonyTypes.GOD_CLASS,
                priority,
                new DisharmonyMetric("ATFD", 9, Direction.ASCENDING),
                new DisharmonyMetric("WMC", 61, Direction.ASCENDING),
                new DisharmonyMetric("TCC", 0.2, Direction.DESCENDING));
    }

    private static RankedDisharmony ranked(String type, int priority, DisharmonyMetric... metrics) {
        String path = "src/app/Big" + priority + ".java";
        DisharmonyInstance instance =
                new DisharmonyInstance(type, "app.Big" + priority, path, "app", null, List.of(metrics));
        instance.setOverallRank(1);
        ScmLogInfo log = new ScmLogInfo(path, "app.Big" + priority, 1, 2, 5);
        log.setChangePronenessRank(1);
        RankedDisharmony ranked = new RankedDisharmony(instance, log);
        ranked.setPriority(priority);
        return ranked;
    }

    private static DisharmonySpec spec(String anchorId) {
        return SimpleHtmlReport.DISHARMONY_SPECS.stream()
                .filter(s -> s.anchorId().equals(anchorId))
                .findFirst()
                .orElseThrow();
    }
}
