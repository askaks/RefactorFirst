package org.hjug.refactorfirst.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.hjug.cbc.RankedDisharmony;
import org.hjug.git.ScmLogInfo;
import org.hjug.graphbuilder.metrics.DisharmonyTypes;
import org.hjug.metrics.DisharmonyInstance;
import org.junit.jupiter.api.Test;

class StartHereSummaryTest {

    private final Map<String, List<RankedDisharmony>> findings = new LinkedHashMap<>();

    @Test
    void givenNothingFound_whenRendered_thenThereIsNoSummary() {
        assertEquals("", render(List.of()));
    }

    @Test
    void givenGodAndBrainClasses_whenRendered_thenTheyLeadTheListInThatOrder() {
        findings.put("GOD", List.of(god("app.Billing", 1, 24), god("app.Mailer", 2, 15)));
        findings.put("BRAIN_CLASS", List.of(classFinding(DisharmonyTypes.BRAIN_CLASS, "app.Aggregator", 1, 9)));

        List<String> names = StartHereSummary.pick(SimpleHtmlReport.DISHARMONY_SPECS, findings).stream()
                .map(StartHereSummary.Pick::name)
                .toList();

        assertEquals(List.of("Billing", "Mailer", "Aggregator"), names);
    }

    @Test
    void givenCycles_whenRendered_thenTheyLeadTheBlockAsRingsLargestFirstAndLeaveTheNumberedList() {
        findings.put("GOD", List.of(god("app.Billing", 1, 24)));
        StartHereSummary.CycleCard small = cycle("app.Small", 2, false);
        StartHereSummary.CycleCard large = cycle("app.models.Large", 4, false);

        String html = render(List.of(small, large));

        assertTrue(html.contains("Cycles found: 2"), html);
        assertTrue(html.indexOf("<strong>Large</strong>") < html.indexOf("<strong>Small</strong>"), "largest first");
        assertEquals(2, html.split("<svg", -1).length - 1, "one ring per cycle");
        assertTrue(html.indexOf("Cycles found") < html.indexOf("Other places to look first"), "cycles come first");
        assertTrue(
                html.indexOf("dependency cycles") < html.indexOf("classes doing too much"), "and so does their tile");
        assertEquals(
                List.of("Billing"),
                StartHereSummary.pick(SimpleHtmlReport.DISHARMONY_SPECS, findings).stream()
                        .map(StartHereSummary.Pick::name)
                        .toList(),
                "a cycle is no longer one of the numbered picks");
    }

    @Test
    void givenCycles_whenRendered_thenTheBlockSaysWhatPinkAndRedMeanAndLinksToWhereCyclesShow() {
        String html = StartHereSummary.render(
                "Shop",
                100,
                SimpleHtmlReport.DISHARMONY_SPECS,
                findings,
                new StartHereSummary.Cycles(List.of(cycle("app.Loop", 3, true)), 6, 2, 4, true));

        assertTrue(html.contains("Pink always means a cycle"));
        assertTrue(html.contains("Red means fix first."));
        assertTrue(html.contains("class=\"rf-spin\""), "the cycle mark turns");
        assertTrue(html.contains("prefers-reduced-motion"), "and stands still when asked to");
        for (String anchor : List.of("CLASSMAP", "WHY_CLASSES", "CYCLES", "CLASSEDGES", "PACKAGEMAP", "PACKAGEEDGES")) {
            assertTrue(html.contains("href=\"#" + anchor + "\""), anchor);
        }
        assertTrue(html.contains("Links to cut 6") && html.contains("Package cycles 2"));
        assertEquals(1, html.split("fix first</span>", -1).length - 1, "the cycle holding the priority 1 cut");
    }

    @Test
    void givenTheSimpleReport_whenRendered_thenNoLinkPointsAtAMapItDoesNotHave() {
        String html = StartHereSummary.render(
                "Shop",
                100,
                SimpleHtmlReport.DISHARMONY_SPECS,
                findings,
                new StartHereSummary.Cycles(List.of(cycle("app.Loop", 3, false)), 6, 2, 4, false));

        assertFalse(html.contains("#CLASSMAP"));
        assertFalse(html.contains("#WHY_CLASSES"));
        assertFalse(html.contains("#PACKAGEMAP"));
        assertTrue(html.contains("href=\"#PACKAGEEDGES\""));
    }

    @Test
    void givenMoreCyclesThanFit_whenRendered_thenTheLargestAreDrawnAndTheRestAreCounted() {
        List<StartHereSummary.CycleCard> cards = new java.util.ArrayList<>();
        for (int i = 0; i < StartHereSummary.MAX_CYCLE_CARDS + 3; i++) {
            cards.add(cycle("app.C" + i, 2, false));
        }

        String html = render(cards);

        assertEquals(StartHereSummary.MAX_CYCLE_CARDS, html.split("<svg", -1).length - 1);
        assertTrue(html.contains(">3 more</a>"), html);
        assertTrue(html.contains("Cycles found: 7"));
    }

    @Test
    void givenAPickWhoseClassIsInACycle_whenRendered_thenItSaysSo() {
        findings.put("GOD", List.of(god("app.Loop0", 1, 24), god("app.Elsewhere", 2, 3)));

        String html = render(List.of(cycle("app.Loop", 2, false)));

        assertEquals(1, html.split(" in a cycle</span>", -1).length - 1, "Loop0 is in the cycle; Elsewhere is not");
    }

    @Test
    void givenNoCycles_whenRendered_thenThereIsNoCycleSection() {
        findings.put("GOD", List.of(god("app.Billing", 1, 24)));

        String html = render(List.of());

        assertFalse(html.contains("Cycles found"));
        assertFalse(html.contains("<svg"));
        assertTrue(html.contains("The place to look first"));
    }

    @Test
    void givenAdviceTextWithLineBreakTags_whenRendered_thenNoTagShowsAsText() {
        findings.put("BRAIN_METHOD", List.of(method(DisharmonyTypes.BRAIN_METHOD, "app.Parser", "parse()", 1, 3)));

        String html = render(List.of());

        assertFalse(html.contains("&lt;br"), html);
    }

    @Test
    void givenMethodFindings_whenRendered_thenTheMostChangedFileNotAlreadyListedIsTheHotspot() {
        findings.put("GOD", List.of(god("app.Billing", 1, 500)));
        findings.put(
                "FEATURE_ENVY",
                List.of(
                        method(DisharmonyTypes.FEATURE_ENVY, "app.Billing", "pay()", 1, 500),
                        method(DisharmonyTypes.FEATURE_ENVY, "app.Quiet", "run()", 2, 3),
                        method(DisharmonyTypes.FEATURE_ENVY, "app.Dashboard", "load()", 3, 239)));

        List<StartHereSummary.Pick> picks = StartHereSummary.pick(SimpleHtmlReport.DISHARMONY_SPECS, findings);

        assertEquals("Dashboard.load()", picks.get(1).name());
        assertTrue(picks.get(1).badge().contains("239 commits"));
    }

    @Test
    void givenOnlyDataClasses_whenRendered_thenNoneIsPickedButTheCountIsLinked() {
        findings.put("DATA_CLASS", List.of(classFinding(DisharmonyTypes.DATA_CLASS, "app.Dto", 1, 4)));

        String html = render(List.of());

        assertTrue(StartHereSummary.pick(SimpleHtmlReport.DISHARMONY_SPECS, findings)
                .isEmpty());
        assertTrue(html.contains("href=\"#DATA_CLASS\""));
        assertTrue(html.contains("Data Classes 1"));
    }

    @Test
    void givenManyFindings_whenRendered_thenAtMostFiveArePicked() {
        findings.put(
                "GOD",
                List.of(god("a.G1", 1, 1), god("a.G2", 2, 1), god("a.G3", 3, 1), god("a.G4", 4, 1), god("a.G5", 5, 1)));
        findings.put(
                "BRAIN_METHOD",
                List.of(
                        method(DisharmonyTypes.BRAIN_METHOD, "a.M1", "x()", 1, 9),
                        method(DisharmonyTypes.BRAIN_METHOD, "a.M2", "x()", 2, 8),
                        method(DisharmonyTypes.BRAIN_METHOD, "a.M3", "x()", 3, 7)));

        List<StartHereSummary.Pick> picks = StartHereSummary.pick(SimpleHtmlReport.DISHARMONY_SPECS, findings);

        assertEquals(5, picks.size());
        // Three classes at most, then the hotspot, then the next method: one kind of finding never fills the list.
        assertEquals(
                List.of("G1", "G2", "G3", "M1.x()", "M2.x()"),
                picks.stream().map(StartHereSummary.Pick::name).toList());
    }

    @Test
    void givenAClassFlaggedSeveralWays_whenRendered_thenTheOtherKindsAreNamed() {
        findings.put("BRAIN_CLASS", List.of(classFinding(DisharmonyTypes.BRAIN_CLASS, "app.Aggregator", 1, 9)));
        findings.put("BRAIN_METHOD", List.of(method(DisharmonyTypes.BRAIN_METHOD, "app.Aggregator", "sum()", 1, 9)));
        findings.put("FEATURE_ENVY", List.of(method(DisharmonyTypes.FEATURE_ENVY, "app.Aggregator", "sum()", 1, 9)));

        String html = render(List.of());

        assertTrue(html.contains("Also flagged: Brain Methods, Feature Envy"));
    }

    @Test
    void givenHostileNames_whenRendered_thenTheyAreEscaped() {
        findings.put("GOD", List.of(god("app.<script>alert(1)</script>", 1, 2)));

        String html = StartHereSummary.render(
                "<b>proj</b>", 10, SimpleHtmlReport.DISHARMONY_SPECS, findings, StartHereSummary.Cycles.none());

        assertFalse(html.contains("<script>"));
        assertFalse(html.contains("<b>proj</b>"));
        assertTrue(html.contains("&lt;script&gt;"));
    }

    @Test
    void givenFindings_whenRendered_thenTheHeadlineNumbersAreShown() {
        findings.put("GOD", List.of(god("app.Billing", 1, 24), god("app.Mailer", 2, 15)));
        findings.put("BRAIN_CLASS", List.of(classFinding(DisharmonyTypes.BRAIN_CLASS, "app.Aggregator", 1, 9)));
        findings.put("BRAIN_METHOD", List.of(method(DisharmonyTypes.BRAIN_METHOD, "app.Other", "x()", 1, 1)));

        String html = StartHereSummary.render(
                "Shop",
                2865,
                SimpleHtmlReport.DISHARMONY_SPECS,
                findings,
                new StartHereSummary.Cycles(List.of(cycle("C", 3, false)), 1, 0, 0, false));

        assertTrue(html.contains("id=\"START_HERE\""));
        assertTrue(html.contains("<b>3</b></span><br/><span>classes doing too much</span>"));
        assertTrue(html.contains(" 1</b></span><br/><span>dependency cycles</span>"), html);
        assertTrue(html.contains("<b>1</b></span><br/><span>over-long, tangled methods</span>"));
        assertTrue(html.contains("<b>2,865</b></span><br/><span>classes analysed</span>"));
        assertTrue(html.contains("Changed in 24 commits"));
    }

    @Test
    void givenTheReport_whenRendered_thenSectionsAndBackToTopStayOnScreen() {
        findings.put("GOD", List.of(god("app.Billing", 1, 24)));
        findings.put("DATA_CLASS", List.of());

        String nav = SimpleHtmlReport.renderFloatingNavigation(
                SimpleHtmlReport.DISHARMONY_SPECS, findings, true, false, true);

        assertTrue(nav.contains("position:fixed"));
        assertTrue(nav.contains("Sections</summary>"));
        assertTrue(nav.contains("href=\"#\""), "back to top");
        assertTrue(nav.contains("href=\"#START_HERE\""));
        assertTrue(nav.contains("href=\"#GOD\"") && nav.contains("God Classes (1)"));
        assertTrue(nav.contains("href=\"#CLASSEDGES\""));
        assertTrue(nav.contains("href=\"#CYCLES\""));
        // Sections with nothing in them are not offered.
        assertFalse(nav.contains("href=\"#DATA_CLASS\""));
        assertFalse(nav.contains("href=\"#PACKAGEEDGES\""));
    }

    @Test
    void givenALongTable_whenRendered_thenRowsPastTheFirstTenAreCollapsedBehindAControl() {
        assertEquals("", SimpleHtmlReport.restOfRowsStartsAt(0));
        assertEquals("", SimpleHtmlReport.restOfRowsStartsAt(9));
        assertTrue(SimpleHtmlReport.restOfRowsStartsAt(10).contains("<tbody class=\"rf-more\">"));
        assertEquals("", SimpleHtmlReport.restOfRowsStartsAt(11));

        assertEquals("", SimpleHtmlReport.rowToggleLabel("GOD", 10), "a short table needs no control");
        String label = SimpleHtmlReport.rowToggleLabel("DATA_CLASS", 735);
        assertTrue(label.contains("for=\"more-DATA_CLASS\""));
        assertTrue(label.contains("Show all 735 rows"));
        assertTrue(SimpleHtmlReport.rowToggle("DATA_CLASS").contains("id=\"more-DATA_CLASS\""));
        assertTrue(SimpleHtmlReport.renderCollapsibleTableStyles().contains("tbody.rf-more{display:none;}"));
    }

    @Test
    void givenEachKindOfPick_whenPicked_thenItsFamilyComesFromThePalette() {
        findings.put("GOD", List.of(god("app.Billing", 1, 24)));
        findings.put("BRAIN_CLASS", List.of(classFinding(DisharmonyTypes.BRAIN_CLASS, "app.Aggregator", 1, 9)));
        findings.put("BRAIN_METHOD", List.of(method(DisharmonyTypes.BRAIN_METHOD, "app.Parser", "parse()", 1, 3)));
        findings.put("FEATURE_ENVY", List.of(method(DisharmonyTypes.FEATURE_ENVY, "app.Envious", "peek()", 1, 2)));
        findings.put(
                "INTENSIVE_COUPLING", List.of(method(DisharmonyTypes.INTENSIVE_COUPLING, "app.Busy", "run()", 1, 101)));

        Map<String, StartHereSummary.Pick> byName = new LinkedHashMap<>();
        for (StartHereSummary.Pick pick : StartHereSummary.pick(SimpleHtmlReport.DISHARMONY_SPECS, findings)) {
            byName.put(pick.name(), pick);
        }

        assertEquals(ReportPalette.Family.CLASS_FINDING, byName.get("Billing").family());
        assertTrue(byName.get("Billing").serious());
        assertEquals(
                ReportPalette.Family.CLASS_FINDING, byName.get("Aggregator").family());
        assertEquals(ReportPalette.Family.HOTSPOT, byName.get("Busy.run()").family());
    }

    @Test
    void givenMethodPicks_whenPicked_thenBrainMethodIsSolidAndFeatureEnvyIsAnOutline() {
        findings.put("BRAIN_METHOD", List.of(method(DisharmonyTypes.BRAIN_METHOD, "app.Parser", "parse()", 1, 30)));
        findings.put(
                "FEATURE_ENVY",
                List.of(
                        method(DisharmonyTypes.FEATURE_ENVY, "app.Parser", "peek()", 1, 30),
                        method(DisharmonyTypes.FEATURE_ENVY, "app.Envious", "look()", 2, 2)));

        List<StartHereSummary.Pick> picks = StartHereSummary.pick(SimpleHtmlReport.DISHARMONY_SPECS, findings);
        StartHereSummary.Pick envy = picks.stream()
                .filter(p -> p.name().equals("Envious.look()"))
                .findFirst()
                .orElseThrow();

        assertEquals(ReportPalette.Family.METHOD_FINDING, envy.family());
        assertFalse(envy.serious(), "Feature Envy is a milder finding, so its tag is an outline");
    }

    @Test
    void givenPicks_whenPicked_thenOnlyPriorityOneOfItsKindIsMarkedFixFirst() {
        findings.put("GOD", List.of(god("app.Billing", 1, 24), god("app.Mailer", 2, 15)));

        List<StartHereSummary.Pick> picks = StartHereSummary.pick(SimpleHtmlReport.DISHARMONY_SPECS, findings);

        assertTrue(picks.get(0).fixFirst(), "Billing is priority 1 of the God Classes");
        assertFalse(picks.get(1).fixFirst(), "Mailer is priority 2");
    }

    @Test
    void givenPicks_whenRendered_thenTagsUseThePaletteAndNoColourIsTypedByHand() {
        findings.put("GOD", List.of(god("app.Billing", 1, 24)));
        findings.put(
                "FEATURE_ENVY",
                List.of(
                        method(DisharmonyTypes.FEATURE_ENVY, "app.Busy", "run()", 1, 101),
                        method(DisharmonyTypes.FEATURE_ENVY, "app.Envious", "look()", 2, 2)));

        String html = render(List.of(cycle("app.Loop", 4, false)));

        // Solid tag: the family colour is the background, with the family's solid mark.
        assertTrue(html.contains("background:" + ReportPalette.Family.CLASS_FINDING.hex()));
        assertTrue(html.contains(ReportPalette.Family.CLASS_FINDING.mark(true) + " " + DisharmonyTypes.GOD_CLASS));
        // Outline tag: the family colour is the border and the text, with the hollow mark.
        assertTrue(html.contains("border:1.5px solid " + ReportPalette.Family.METHOD_FINDING.hex()));
        assertTrue(html.contains(ReportPalette.Family.METHOD_FINDING.mark(false) + " " + DisharmonyTypes.FEATURE_ENVY));
        assertTrue(html.contains("color:" + ReportPalette.Family.CYCLE.hex()), "the cycles heading and tile");
        assertTrue(html.contains("background:" + ReportPalette.Family.HOTSPOT.hex()));
        assertTrue(html.contains(ReportPalette.Family.HOTSPOT.mark(true) + " Hotspot"));
        // Fix first sits beside the kind's tag, once: only Billing is priority 1 and not the hotspot.
        assertEquals(1, html.split(ReportPalette.Family.FIX_FIRST.mark(true) + " fix first", -1).length - 1);
        assertTrue(html.contains("background:" + ReportPalette.Family.FIX_FIRST.hex()));
        // The old hand-typed colours are gone: brown for cycles, blue for hotspots, green for advice.
        assertFalse(html.contains("#b26a00"));
        assertFalse(html.contains("#1565c0"));
        assertFalse(html.contains("#2e7d32"));
    }

    private String render(List<StartHereSummary.CycleCard> cycles) {
        return StartHereSummary.render(
                "Shop",
                100,
                SimpleHtmlReport.DISHARMONY_SPECS,
                findings,
                new StartHereSummary.Cycles(cycles, 0, 0, 0, false));
    }

    private static RankedDisharmony god(String className, int priority, int commits) {
        return classFinding(DisharmonyTypes.GOD_CLASS, className, priority, commits);
    }

    private static RankedDisharmony classFinding(String type, String className, int priority, int commits) {
        return method(type, className, null, priority, commits);
    }

    private static RankedDisharmony method(String type, String className, String signature, int priority, int commits) {
        String path = "src/" + className.replace('.', '/') + ".java";
        DisharmonyInstance instance = new DisharmonyInstance(type, className, path, "app", signature, List.of());
        instance.setOverallRank(1);
        ScmLogInfo log = new ScmLogInfo(path, className, 1, 2, commits);
        log.setChangePronenessRank(1);
        RankedDisharmony ranked = new RankedDisharmony(instance, log);
        ranked.setPriority(priority);
        return ranked;
    }

    /** A cycle of classes named name0, name1, ..., each using the next and the last using the first. */
    private static StartHereSummary.CycleCard cycle(String name, int classes, boolean fixFirst) {
        List<String> members = new java.util.ArrayList<>();
        for (int i = 0; i < classes; i++) {
            members.add(name + i);
        }
        List<CycleRingSvg.Link> links = new java.util.ArrayList<>();
        for (int i = 0; i < classes; i++) {
            links.add(new CycleRingSvg.Link(members.get(i), members.get((i + 1) % classes), i + 1, i == 0));
        }
        return new StartHereSummary.CycleCard(name, members, links, fixFirst);
    }
}
