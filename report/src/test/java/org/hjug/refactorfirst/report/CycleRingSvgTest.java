package org.hjug.refactorfirst.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.hjug.refactorfirst.report.CycleRingSvg.Link;
import org.hjug.refactorfirst.report.ReportPalette.Family;
import org.junit.jupiter.api.Test;

/** A cycle drawn as a ring: its classes round a circle, its links as arrows, the links to cut marked. */
class CycleRingSvgTest {

    @Test
    void givenTwoClassesThatUseEachOther_whenDrawn_thenThereIsOnePillPerClassAndOneArrowPerLink() {
        String svg = CycleRingSvg.render(
                "c1",
                List.of("app.SimpleHtmlReport", "app.StartHereSummary"),
                List.of(
                        new Link("app.SimpleHtmlReport", "app.StartHereSummary", 1, true),
                        new Link("app.StartHereSummary", "app.SimpleHtmlReport", 1, false)));

        assertTrue(svg.startsWith("<svg"), svg);
        assertEquals(2, count(svg, "<rect"));
        assertEquals(2, count(svg, "marker-end="), "one arrow per link");
        assertTrue(svg.contains(">SimpleHtmlReport</text>"));
        assertTrue(svg.contains(">StartHereSummary</text>"));
    }

    @Test
    void givenALinkToCut_whenDrawn_thenItIsThickInTheCycleColourWithScissorsAndItsUses() {
        String svg = CycleRingSvg.render(
                "c1",
                List.of("app.A", "app.B", "app.C"),
                List.of(
                        new Link("app.A", "app.B", 7, true),
                        new Link("app.B", "app.C", 2, false),
                        new Link("app.C", "app.A", 1, false)));

        assertEquals(3, count(svg, "<rect"));
        assertEquals(3, count(svg, "marker-end="), "one arrow per link");
        assertEquals(1, count(svg, "stroke-width=\"2.8\""), "only the link to cut is thick");
        assertTrue(svg.contains("&#9986; 7"), svg);
        assertTrue(svg.contains("stroke=\"" + Family.CYCLE.hex() + "\""));
        assertFalse(svg.contains(Family.FIX_FIRST.hex()), "nothing in a ring is red");
    }

    @Test
    void givenNestedClasses_whenDrawn_thenTheOuterClassNameIsDroppedFromTheLabel() {
        String svg = CycleRingSvg.render(
                "c1",
                List.of("app.CSharpFacts", "app.CSharpFacts$TypeFacts", "app.CSharpFacts$MethodFacts"),
                List.of(new Link("app.CSharpFacts", "app.CSharpFacts$TypeFacts", 2, true)));

        assertTrue(svg.contains(">CSharpFacts</text>"));
        assertTrue(svg.contains(">TypeFacts</text>"));
        assertTrue(svg.contains(">MethodFacts</text>"));
        assertFalse(svg.contains("$"));
    }

    @Test
    void givenFourClasses_whenDrawn_thenAllFourFitInsideTheDrawing() {
        String svg = CycleRingSvg.render(
                "c1",
                List.of(
                        "app.ReportWriter",
                        "app.ReportWriter$SecureDirectoryOps",
                        "app.ReportWriter$SecureDirectoryOpsImpl",
                        "app.ReportWriter$FallbackDirectoryOps"),
                List.of());

        assertEquals(4, count(svg, "<rect"));
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                        "<rect x=\"(-?[0-9.]+)\" y=\"[0-9.]+\" width=\"([0-9.]+)\"")
                .matcher(svg);
        while (m.find()) {
            double left = Double.parseDouble(m.group(1));
            double right = left + Double.parseDouble(m.group(2));
            assertTrue(left >= 0 && right <= CycleRingSvg.WIDTH, "pill from " + left + " to " + right);
        }
    }

    @Test
    void givenMoreClassesThanARingCanShow_whenDrawn_thenTheNamesAreListedInstead() {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < CycleRingSvg.MAX_CLASSES + 3; i++) {
            names.add(String.format("app.K%02d", i));
        }

        String html = CycleRingSvg.render("c1", names, List.of());

        assertFalse(html.contains("<svg"));
        assertTrue(html.contains("K00, K01"), html);
        assertTrue(html.contains("and 3 more"), html);
    }

    @Test
    void givenTwoRingsOnOnePage_whenDrawn_thenTheirArrowheadsDoNotShareIds() {
        List<String> names = List.of("app.A", "app.B");
        List<Link> links = List.of(new Link("app.A", "app.B", 1, true));

        String first = CycleRingSvg.render("c1", names, links);
        String second = CycleRingSvg.render("c2", names, links);

        assertTrue(first.contains("id=\"c1-cut\"") && first.contains("url(#c1-cut)"));
        assertTrue(second.contains("id=\"c2-cut\"") && second.contains("url(#c2-cut)"));
        assertFalse(first.contains("<script"), "no script: it has to work in the simple report");
    }

    @Test
    void givenANameThatIsNotSafeHtml_whenDrawn_thenItIsEscaped() {
        String svg = CycleRingSvg.render("c1", List.of("app.<anonymous>", "app.B"), List.of());
        assertTrue(svg.contains("&lt;anonymous&gt;"));
        assertFalse(svg.contains("<anonymous>"));
    }

    @Test
    void givenTheRing_whenItsSourceIsRead_thenItUsesNoReportClassThatUsesItBack() throws Exception {
        String source = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/org/hjug/refactorfirst/report/CycleRingSvg.java"));
        assertFalse(source.contains("SimpleHtmlReport."));
        assertFalse(source.contains("StartHereSummary."));
        assertFalse(source.contains("HtmlReport."));
    }

    @Test
    void givenSixOrMoreClasses_whenDrawn_thenEachIsADotWithItsNameOutsideTheRing() {
        List<String> names = new ArrayList<>();
        List<Link> links = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            names.add("app.LongTermModelPart" + i);
        }
        for (int i = 0; i < 8; i++) {
            links.add(new Link(names.get(i), names.get((i + 1) % 8), 1, i == 0));
        }

        String svg = CycleRingSvg.render("c1", names, links);

        assertTrue(svg.startsWith("<svg"), svg);
        assertEquals(0, count(svg, "<rect"), "no name boxes to overlap");
        assertEquals(8, count(svg, "class=\"rf-dot\""), svg);
        assertEquals(8, count(svg, "marker-end="), "one arrow per link");
        assertTrue(svg.contains("text-anchor=\"start\"") && svg.contains("text-anchor=\"end\""), svg);
        assertTrue(svg.contains(">LongTermModelPart3</text>"), svg);
    }

    @Test
    void givenFiveClasses_whenDrawn_thenTheyKeepTheirNameBoxes() {
        List<String> names = List.of("app.A", "app.B", "app.C", "app.D", "app.E");
        String svg = CycleRingSvg.render("c1", names, List.of());
        assertEquals(5, count(svg, "<rect"));
        assertEquals(0, count(svg, "class=\"rf-dot\""));
    }

    private static int count(String text, String part) {
        return text.split(java.util.regex.Pattern.quote(part), -1).length - 1;
    }
}
