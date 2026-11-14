package org.hjug.refactorfirst.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.hjug.refactorfirst.report.ReportPalette.Family;
import org.junit.jupiter.api.Test;

class BulletGraphTest {

    private static final List<BulletGraph.Level> WMC = List.of(
            new BulletGraph.Level("average", 14),
            new BulletGraph.Level("high", 31),
            new BulletGraph.Level("very high", 47));

    @Test
    void givenAValueOverItsLimit_whenDrawn_thenTheBarPassesTheLimitLineAndTheLevelsAreLabelled() {
        String html = BulletGraph.render(136, 47, false, WMC, Family.GOD);

        assertTrue(html.contains("background:#d4a017"), "the bar is in the finding's colour");
        assertTrue(html.contains("average<br/>14") && html.contains("very high<br/>47"), html);
        // 136 on a scale of 136 * 1.08 = 146.88: the limit sits at 32.0%, the bar ends at 92.6%.
        assertTrue(html.contains("left:32.0%"), html);
        assertTrue(html.contains("width:92.6%"), html);
        assertTrue(html.contains("#bdbdbd"), "the band beyond the limit is the darkest");
    }

    @Test
    void givenHowFarOver_whenWritten_thenLargeExcessIsTimesTheLimitAndSmallIsJustOver() {
        assertEquals("<strong>2.9&times;</strong> the limit", BulletGraph.overText(136, 47, false, "47"));
        assertEquals("just over the limit of 5", BulletGraph.overText(7, 5, false, "5"));
        assertEquals("at the limit of 5", BulletGraph.overText(5, 5, false, "5"));
    }

    @Test
    void givenAMeasureWhereLowerIsWorse_whenWritten_thenItSaysHowFarShort() {
        assertEquals(
                "<strong>0.01</strong> short of the limit of 0.33", BulletGraph.overText(0.32, 0.33, true, "0.33"));
    }

    @Test
    void givenAMeasureWhereLowerIsWorse_whenDrawn_thenTheDarkBandIsOnTheLeft() {
        String html = BulletGraph.render(0.32, 0.33, true, List.of(), Family.GOD);
        assertTrue(html.indexOf("#bdbdbd") < html.indexOf("#f3f3f3"), html);
    }

    @Test
    void givenNumbers_whenShown_thenWholeOnesArePlainAndRatiosHaveTwoPlaces() {
        assertEquals("136", BulletGraph.number(136));
        assertEquals("0.32", BulletGraph.number(0.32));
        assertEquals("0.33", BulletGraph.number(1.0 / 3));
        assertEquals("0.5", BulletGraph.number(0.5));
    }

    @Test
    void givenTheGraph_whenItsSourceIsRead_thenItUsesNoReportClassThatCouldUseItBack() throws Exception {
        String source = Files.readString(Path.of("src/main/java/org/hjug/refactorfirst/report/BulletGraph.java"));
        for (String other :
                List.of("SimpleHtmlReport", "StartHereSummary", "HtmlReport", "ReportKey", "NumbersGuide")) {
            assertFalse(source.contains(other), "BulletGraph must not use " + other);
        }
    }
}
