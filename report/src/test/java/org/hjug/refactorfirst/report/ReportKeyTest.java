package org.hjug.refactorfirst.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hjug.graphbuilder.VertexInfo;
import org.hjug.refactorfirst.report.ReportPalette.Family;
import org.jgrapht.Graph;
import org.jgrapht.graph.AsSubgraph;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.junit.jupiter.api.Test;

class ReportKeyTest {

    @Test
    void givenTheMapKey_whenRendered_thenItExplainsEveryMarkTheMapsDrawInThePaletteColours() {
        String key = ReportKey.forMaps();

        assertTrue(key.contains("in a cycle"), key);
        assertTrue(key.contains("relationship to cut"), key);
        assertTrue(key.contains("package cycle"), key);
        assertTrue(key.contains("nothing found"), key);
        assertTrue(key.contains(Family.CYCLE.hex()));
        assertTrue(key.contains(Family.CYCLE_PALE.hex()));
        assertTrue(key.contains(Family.CYCLE.mark()));
        assertFalse(key.contains(Family.FIX_FIRST.hex()), "nothing on the maps is red, so the map key has no red");
    }

    @Test
    void givenTheFindingsKey_whenRendered_thenEveryTagFamilyHasALineWithItsMeaning() {
        String key = ReportKey.forFindings();

        for (Family family : List.of(
                Family.CYCLE,
                Family.GOD,
                Family.BRAIN,
                Family.BRAIN_LIGHT,
                Family.ENVY,
                Family.COUPLING,
                Family.QUIET,
                Family.HOTSPOT)) {
            assertTrue(key.contains(family.hex()), family + " colour");
            assertTrue(key.contains(family.meaning()), family + " meaning");
        }
        assertTrue(key.contains("&#9724; a class") && key.contains("&#9670; a method"), "says what the marks mean");
        assertFalse(key.contains("outlined"), "there are no outlined tags any more");
        assertFalse(key.contains(Family.FIX_FIRST.hex()), "fix first names itself and needs no key entry");
    }

    @Test
    void givenASmellTag_whenRendered_thenItIsFilledLeadsWithTheLevelMarkAndNeverWraps() {
        String god = ReportKey.tag(Family.GOD, false, "God Class");
        assertTrue(god.contains("background:#d4a017"), god);
        assertTrue(god.contains("color:#2b2b2b"), "dark text on gold: " + god);
        assertTrue(god.contains("&#9724; God Class"), god);
        assertTrue(god.contains("white-space:nowrap"), god);

        String method = ReportKey.tag(Family.BRAIN_LIGHT, true, "Brain Method");
        assertTrue(method.contains("&#9670; Brain Method"), method);

        String fixFirst = ReportKey.tag(Family.FIX_FIRST, true, "fix first");
        assertTrue(fixFirst.contains("&#9873; fix first"), "fix first keeps its own mark: " + fixFirst);
    }

    @Test
    void givenEveryFamilyThatMarksSomething_whenAskedForItsMeaning_thenItHasOne() {
        for (Family family : Family.values()) {
            assertFalse(family.meaning().isBlank(), family + " needs a meaning for the key");
        }
    }

    @Test
    void givenCycles_whenTheReasonTableIsRendered_thenEachCycleShowsItsClassesAndTheLinksToCut() {
        Graph<VertexInfo, DefaultWeightedEdge> graph = new DefaultDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        for (String name : List.of("app.A", "app.B", "app.C", "app.D", "app.E")) {
            graph.addVertex(v(name));
        }
        DefaultWeightedEdge ab = graph.addEdge(v("app.A"), v("app.B"));
        graph.setEdgeWeight(ab, 2);
        graph.addEdge(v("app.B"), v("app.A"));
        DefaultWeightedEdge cd = graph.addEdge(v("app.C"), v("app.D"));
        graph.addEdge(v("app.D"), v("app.E"));
        graph.addEdge(v("app.E"), v("app.C"));

        Map<VertexInfo, AsSubgraph<VertexInfo, DefaultWeightedEdge>> cycles = new LinkedHashMap<>();
        cycles.put(v("app.A"), new AsSubgraph<>(graph, Set.of(v("app.A"), v("app.B"))));
        cycles.put(v("app.C"), new AsSubgraph<>(graph, Set.of(v("app.C"), v("app.D"), v("app.E"))));

        String table = ReportKey.whyMarked("WHY_CLASSES", "Class", "CLASSEDGES", graph, cycles, Set.of(ab, cd));

        // Largest cycle first.
        assertTrue(table.indexOf("C, D, E") < table.indexOf("A, B"), table);
        assertTrue(table.contains("<td align=\"right\">3</td>"));
        assertTrue(table.contains("A &#8594; B (2 uses)"), table);
        assertTrue(table.contains("C &#8594; D (1 use)"), table);
        assertTrue(table.contains("href=\"#CLASSEDGES\""));
        assertTrue(table.contains(Family.CYCLE.hex()), "the cut is shown in the cycle colour");
    }

    @Test
    void givenNoCycles_whenTheReasonTableIsRendered_thenThereIsNothing() {
        Graph<VertexInfo, DefaultWeightedEdge> graph = new DefaultDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        assertEquals("", ReportKey.whyMarked("WHY_CLASSES", "Class", "CLASSEDGES", graph, Map.of(), Set.of()));
        assertEquals("", ReportKey.whyMarked("WHY_CLASSES", "Class", "CLASSEDGES", graph, null, Set.of()));
    }

    @Test
    void givenAVeryLargeCycle_whenTheReasonTableIsRendered_thenOnlyTheFirstNamesAreListed() {
        Graph<VertexInfo, DefaultWeightedEdge> graph = new DefaultDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        java.util.Set<VertexInfo> members = new java.util.LinkedHashSet<>();
        for (int i = 0; i < 20; i++) {
            graph.addVertex(v(String.format("app.K%02d", i)));
            members.add(v(String.format("app.K%02d", i)));
        }
        Map<VertexInfo, AsSubgraph<VertexInfo, DefaultWeightedEdge>> cycles =
                Map.of(v("app.K00"), new AsSubgraph<>(graph, members));

        String table = ReportKey.whyMarked("WHY_CLASSES", "Class", "CLASSEDGES", graph, cycles, Set.of());

        assertTrue(table.contains("K00, K01"), table);
        assertTrue(table.contains("and 12 more"), table);
        assertFalse(table.contains("K19"));
    }

    @Test
    void givenMoreCyclesThanFit_whenTheReasonTableIsRendered_thenTheLargestAreShownAndTheRestArePointedTo() {
        Graph<VertexInfo, DefaultWeightedEdge> graph = new DefaultDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        Map<VertexInfo, AsSubgraph<VertexInfo, DefaultWeightedEdge>> cycles = new LinkedHashMap<>();
        for (int i = 0; i < 12; i++) {
            VertexInfo a = v(String.format("app.P%02da", i));
            VertexInfo b = v(String.format("app.P%02db", i));
            graph.addVertex(a);
            graph.addVertex(b);
            cycles.put(a, new AsSubgraph<>(graph, Set.of(a, b)));
        }

        String table = ReportKey.whyMarked("WHY_CLASSES", "Class", "CLASSEDGES", graph, cycles, Set.of());

        assertEquals(ReportKey.MAX_ROWS, table.split("<tr>\\n<td", -1).length - 1);
        assertTrue(table.contains("The 10 largest of 12 cycles are shown."), table);
    }

    @Test
    void givenTheKey_whenItsSourceIsRead_thenItUsesNoReportClassThatUsesItBack() throws Exception {
        // The report classes use the key. If the key used them back it would join a dependency cycle.
        String source = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/java/org/hjug/refactorfirst/report/ReportKey.java"));
        assertFalse(source.contains("SimpleHtmlReport."));
        assertFalse(source.contains("StartHereSummary."));
        assertFalse(source.contains("HtmlReport."));
    }

    @Test
    void givenTheLinksToCutKey_whenRendered_thenItExplainsTheFlagsInTheTable() {
        String key = ReportKey.forLinksToCut();

        assertTrue(key.contains(Family.CYCLE_PALE.hex()), "the flagged cycle count");
        assertTrue(key.contains(Family.FIX_FIRST.hex()), "the fix first tag");
        assertTrue(key.contains("hardest"), key);
        assertTrue(key.contains("cut this first"), key);
    }

    @Test
    void givenACycleCount_whenDrawnAsACell_thenAnyCountAboveZeroIsFlaggedInTheCycleColour() {
        String flagged = ReportKey.cycleCountCell("2");
        assertTrue(flagged.contains("background:" + Family.CYCLE_PALE.hex()), flagged);
        assertTrue(flagged.contains("2 <span") && flagged.contains(Family.CYCLE.mark()), flagged);

        assertEquals("<td align=\"right\">0</td>\n", ReportKey.cycleCountCell("0"));
        assertEquals("<td align=\"right\">null</td>\n", ReportKey.cycleCountCell("null"), "no count, no flag");
    }

    @Test
    void givenAPriority_whenDrawnAsACell_thenOnlyPriorityOneCarriesFixFirst() {
        assertTrue(ReportKey.priorityCell("1").contains(Family.FIX_FIRST.hex()));
        assertTrue(ReportKey.priorityCell("1").contains("fix first"));
        assertEquals("<td align=\"right\">2</td>\n", ReportKey.priorityCell("2"));
    }

    @Test
    void givenAStrength_whenDrawnAsACell_thenTheHardestInTheTableSaysSo() {
        assertTrue(ReportKey.strengthCell("7", true).contains("hardest"));
        assertEquals("<td align=\"right\">2</td>\n", ReportKey.strengthCell("2", false));
    }

    @Test
    void givenARelationship_whenAskedWhyItIsListed_thenTheCycleItClosesAndTheWorkToMoveItAreNamed() {
        Graph<VertexInfo, DefaultWeightedEdge> graph = new DefaultDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        for (String name : List.of("app.A", "app.B", "app.C", "app.X")) {
            graph.addVertex(v(name));
        }
        DefaultWeightedEdge ab = graph.addEdge(v("app.A"), v("app.B"));
        graph.setEdgeWeight(ab, 2);
        graph.addEdge(v("app.B"), v("app.C"));
        graph.addEdge(v("app.C"), v("app.A"));
        DefaultWeightedEdge ax = graph.addEdge(v("app.A"), v("app.X"));

        Map<VertexInfo, AsSubgraph<VertexInfo, DefaultWeightedEdge>> one = new LinkedHashMap<>();
        one.put(v("app.A"), new AsSubgraph<>(graph, Set.of(v("app.A"), v("app.B"), v("app.C"))));
        assertEquals("Closes a cycle of 3 classes. 2 uses to move.", ReportKey.whyLink(graph, one, ab, "classes"));

        Map<VertexInfo, AsSubgraph<VertexInfo, DefaultWeightedEdge>> two = new LinkedHashMap<>(one);
        two.put(v("app.B"), new AsSubgraph<>(graph, Set.of(v("app.A"), v("app.B"))));
        assertEquals(
                "Part of 2 cycles; the largest has 3 classes. 2 uses to move.",
                ReportKey.whyLink(graph, two, ab, "classes"));

        assertEquals("1 use to move.", ReportKey.whyLink(graph, one, ax, "classes"), "in no cycle we know of");
        assertEquals("2 uses to move.", ReportKey.whyLink(graph, null, ab, "classes"));
    }

    @Test
    void givenARelationshipToCut_whenMarked_thenItCarriesScissorsInTheCycleColour() {
        String marked = ReportKey.cut("A &#8594; B : 1");
        assertTrue(marked.contains("&#9986; A &#8594; B : 1"));
        assertTrue(marked.contains("color:" + Family.CYCLE.hex()));
    }

    private static VertexInfo v(String name) {
        return VertexInfo.forName(name);
    }
}
