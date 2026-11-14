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
    void givenAGodClass_whenAskedWhyItIsListed_thenEachNumberIsNamedInWordsWithItsLimit() {
        String why = SimpleHtmlReport.whyFinding(godClass(1));

        assertTrue(why.contains("<strong>9</strong> uses of other classes' data"), why);
        assertTrue(why.contains("(flagged over 5, <a href=\"#WHY_ATFD\""), why);
        assertTrue(why.contains("<strong>61</strong> complexity of all its methods"), why);
        assertTrue(why.contains("(flagged 47 or more, <a href=\"#WHY_WMC\""), why);
        assertTrue(why.contains("<strong>0.20</strong> cohesion (0 to 1)"), why);
        assertTrue(why.contains("(flagged under 0.33, <a href=\"#WHY_TCC\""), why);
        assertEquals(3, why.split("why\\?</a>", -1).length - 1, "one why link per limit");
    }

    @Test
    void givenAFindingTypeWithNoSingleLimit_whenAskedWhy_thenTheNumbersAreNamedAndNoLimitIsInvented() {
        RankedDisharmony refused =
                ranked(DisharmonyTypes.REFUSED_PARENT_BEQUEST, 1, new DisharmonyMetric("NOM", 12, Direction.ASCENDING));

        String why = SimpleHtmlReport.whyFinding(refused);

        assertEquals("<strong>12</strong> methods", why);
        assertFalse(why.contains("flagged"));
    }

    @Test
    void givenAFindingWithNoNumbers_whenAskedWhy_thenThereIsNothingToSay() {
        assertEquals("", SimpleHtmlReport.whyFinding(ranked(DisharmonyTypes.GOD_CLASS, 1)));
    }

    @Test
    void givenTheDefaultTable_whenRendered_thenItHasTheReasonColumnAndFixFirstOnPriorityOne() {
        String html = report.renderDisharmonyInfo("", spec("GOD"), false, List.of(godClass(1), godClass(2)));

        assertTrue(html.contains("<th>Why it is here</th>"), html);
        assertTrue(html.contains("uses of other classes' data"));
        assertEquals(1, html.split("fix first</span>", -1).length - 1, "only priority 1 is fix first");
        assertTrue(html.contains("background:" + Family.FIX_FIRST.hex()));
    }

    @Test
    void givenTheDetailedTable_whenRendered_thenEachMetricValueCarriesItsLimit() {
        String html = report.renderDisharmonyInfo("", spec("GOD"), true, List.of(godClass(1)));

        assertTrue(html.contains("61 <span"), html);
        assertTrue(html.contains("47 or more"), html);
    }

    @Test
    void givenALongRatio_whenAskedWhy_thenItIsShownToTwoPlaces() {
        RankedDisharmony brain =
                ranked(DisharmonyTypes.BRAIN_CLASS, 1, new DisharmonyMetric("TCC", 1.0 / 3, Direction.DESCENDING));

        assertTrue(SimpleHtmlReport.whyFinding(brain).startsWith("<strong>0.33</strong> cohesion"));
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
