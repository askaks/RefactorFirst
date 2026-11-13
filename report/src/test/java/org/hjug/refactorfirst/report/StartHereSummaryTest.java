package org.hjug.refactorfirst.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hjug.cbc.RankedCycle;
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

        List<String> names = StartHereSummary.pick(SimpleHtmlReport.DISHARMONY_SPECS, findings, List.of()).stream()
                .map(StartHereSummary.Pick::name)
                .toList();

        assertEquals(List.of("Billing", "Mailer", "Aggregator"), names);
    }

    @Test
    void givenACycle_whenRendered_thenTheLargestOneIsListedAfterTheClasses() {
        findings.put("GOD", List.of(god("app.Billing", 1, 24)));
        RankedCycle small = cycle("Small", 2, 1);
        RankedCycle large = cycle("app.models.Large", 8, 2);

        List<String> names =
                StartHereSummary.pick(SimpleHtmlReport.DISHARMONY_SPECS, findings, List.of(small, large)).stream()
                        .map(StartHereSummary.Pick::name)
                        .toList();

        assertEquals(List.of("Billing", "Large cycle"), names);
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

        List<StartHereSummary.Pick> picks =
                StartHereSummary.pick(SimpleHtmlReport.DISHARMONY_SPECS, findings, List.of());

        assertEquals("Dashboard.load()", picks.get(1).name());
        assertTrue(picks.get(1).badge().contains("239 commits"));
    }

    @Test
    void givenOnlyDataClasses_whenRendered_thenNoneIsPickedButTheCountIsLinked() {
        findings.put("DATA_CLASS", List.of(classFinding(DisharmonyTypes.DATA_CLASS, "app.Dto", 1, 4)));

        String html = render(List.of());

        assertTrue(StartHereSummary.pick(SimpleHtmlReport.DISHARMONY_SPECS, findings, List.of())
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

        List<StartHereSummary.Pick> picks =
                StartHereSummary.pick(SimpleHtmlReport.DISHARMONY_SPECS, findings, List.of(cycle("C", 3, 1)));

        assertEquals(5, picks.size());
        // Three classes at most, then the cycle, then the hotspot: one kind of finding never fills the list.
        assertEquals(
                List.of("G1", "G2", "G3", "C cycle", "M1.x()"),
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

        String html =
                StartHereSummary.render("<b>proj</b>", 10, SimpleHtmlReport.DISHARMONY_SPECS, findings, List.of());

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
                "Shop", 2865, SimpleHtmlReport.DISHARMONY_SPECS, findings, List.of(cycle("C", 3, 1)));

        assertTrue(html.contains("id=\"START_HERE\""));
        assertTrue(html.contains("<b>3</b></span><br/><span>classes doing too much</span>"));
        assertTrue(html.contains("<b>1</b></span><br/><span>dependency cycles</span>"));
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
        for (StartHereSummary.Pick pick :
                StartHereSummary.pick(SimpleHtmlReport.DISHARMONY_SPECS, findings, List.of(cycle("app.Loop", 4, 1)))) {
            byName.put(pick.name(), pick);
        }

        assertEquals(ReportPalette.Family.CLASS_FINDING, byName.get("Billing").family());
        assertTrue(byName.get("Billing").serious());
        assertEquals(
                ReportPalette.Family.CLASS_FINDING, byName.get("Aggregator").family());
        assertEquals(ReportPalette.Family.CYCLE, byName.get("Loop cycle").family());
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

        List<StartHereSummary.Pick> picks =
                StartHereSummary.pick(SimpleHtmlReport.DISHARMONY_SPECS, findings, List.of());
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

        List<StartHereSummary.Pick> picks =
                StartHereSummary.pick(SimpleHtmlReport.DISHARMONY_SPECS, findings, List.of(cycle("app.Loop", 4, 1)));

        assertTrue(picks.get(0).fixFirst(), "Billing is priority 1 of the God Classes");
        assertFalse(picks.get(1).fixFirst(), "Mailer is priority 2");
        assertFalse(picks.get(2).fixFirst(), "a cycle is not marked here; the cycle cards decide that");
    }

    @Test
    void givenPicks_whenRendered_thenTagsUseThePaletteAndNoColourIsTypedByHand() {
        findings.put("GOD", List.of(god("app.Billing", 1, 24)));
        findings.put(
                "FEATURE_ENVY",
                List.of(
                        method(DisharmonyTypes.FEATURE_ENVY, "app.Busy", "run()", 1, 101),
                        method(DisharmonyTypes.FEATURE_ENVY, "app.Envious", "look()", 2, 2)));

        String html = render(List.of(cycle("app.Loop", 4, 1)));

        // Solid tag: the family colour is the background, with the family's solid mark.
        assertTrue(html.contains("background:" + ReportPalette.Family.CLASS_FINDING.hex()));
        assertTrue(html.contains(ReportPalette.Family.CLASS_FINDING.mark(true) + " " + DisharmonyTypes.GOD_CLASS));
        // Outline tag: the family colour is the border and the text, with the hollow mark.
        assertTrue(html.contains("border:1.5px solid " + ReportPalette.Family.METHOD_FINDING.hex()));
        assertTrue(html.contains(ReportPalette.Family.METHOD_FINDING.mark(false) + " " + DisharmonyTypes.FEATURE_ENVY));
        assertTrue(html.contains("background:" + ReportPalette.Family.CYCLE.hex()));
        assertTrue(html.contains(ReportPalette.Family.CYCLE.mark(true) + " Cycle"));
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

    private String render(List<RankedCycle> cycles) {
        return StartHereSummary.render("Shop", 100, SimpleHtmlReport.DISHARMONY_SPECS, findings, cycles);
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

    private static RankedCycle cycle(String name, int classes, int priority) {
        Set<String> vertices = new java.util.LinkedHashSet<>();
        for (int i = 0; i < classes; i++) {
            vertices.add(name + i);
        }
        RankedCycle cycle = new RankedCycle(name, vertices, Set.of(), List.of());
        cycle.setPriority(priority);
        return cycle;
    }
}
