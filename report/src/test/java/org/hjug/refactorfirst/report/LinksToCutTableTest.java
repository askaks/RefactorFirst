package org.hjug.refactorfirst.report;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.hjug.cbc.RankedDisharmony;
import org.hjug.graphbuilder.CodebaseGraphDTO;
import org.hjug.graphbuilder.VertexInfo;
import org.hjug.refactorfirst.report.ReportPalette.Family;
import org.jgrapht.Graph;
import org.jgrapht.graph.AsSubgraph;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.junit.jupiter.api.Test;

/** The "relationships to cut" tables: what is flagged in a row, and that the old star is gone. */
class LinksToCutTableTest {

    private final SimpleHtmlReport report = new SimpleHtmlReport();
    private final CodebaseGraphDTO dto = mock(CodebaseGraphDTO.class);

    @Test
    void givenThePriorityOneRelationship_whenItsRowIsDrawn_thenTheProblemNumbersAreFlaggedAndItSaysWhy() {
        DefaultWeightedEdge ab = twoClassCycle();
        RankedDisharmony ranked = new RankedDisharmony("app.A", ab, 1, 7, true, false, 0, false);
        ranked.setPriority(1);

        String row = report.renderClassEdgeRow(ranked, true, "", dto);

        assertTrue(row.contains("A &#8594; B : 7"), row);
        assertTrue(row.contains("fix first"), "priority 1 is the one to cut first: " + row);
        assertTrue(row.contains("background:" + Family.CYCLE_PALE.hex()), "the cycle count is flagged: " + row);
        assertTrue(row.contains("hardest"), row);
        assertTrue(row.contains("Closes a cycle of 2 classes. 7 uses to move."), row);
        assertTrue(
                row.indexOf("Closes a cycle") < row.indexOf("fix first"),
                "the reason sits next to the name, before the numbers");
    }

    @Test
    void givenAClassOnTheToolsRemovalList_whenItsRowIsDrawn_thenItCarriesNoStar() {
        // The removal list is not a usable answer (it names classes that are in no cycle), so it is not shown.
        DefaultWeightedEdge ab = twoClassCycle();
        report.classesToRemove = Set.of(VertexInfo.forName("app.A"), VertexInfo.forName("app.B"));
        RankedDisharmony ranked = new RankedDisharmony("app.A", ab, 1, 7, true, true, 0, false);
        ranked.setPriority(2);

        String row = report.renderClassEdgeRow(ranked, false, "", dto);

        assertFalse(row.contains("*"), row);
        assertFalse(row.contains("fix first"), "priority 2 is not fix first");
        assertFalse(row.contains("hardest"));
    }

    @Test
    void givenAPackageRelationship_whenItsRowIsDrawn_thenItIsFlaggedTheSameWayAndCarriesNoStar() {
        Graph<VertexInfo, DefaultWeightedEdge> packages = new DefaultDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        packages.addVertex(VertexInfo.forName("app.a"));
        packages.addVertex(VertexInfo.forName("app.b"));
        DefaultWeightedEdge ab = packages.addEdge(VertexInfo.forName("app.a"), VertexInfo.forName("app.b"));
        packages.setEdgeWeight(ab, 11);
        packages.addEdge(VertexInfo.forName("app.b"), VertexInfo.forName("app.a"));
        report.packageGraph = packages;
        report.packageCycles = Map.of(
                VertexInfo.forName("app.a"),
                new AsSubgraph<>(packages, Set.of(VertexInfo.forName("app.a"), VertexInfo.forName("app.b"))));
        report.packagesToRemove = Set.of(VertexInfo.forName("app.a"));
        report.classGraph = new DefaultDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        when(dto.getClassRelationshipsInPackageRelationship()).thenReturn(new HashMap<>());
        RankedDisharmony ranked = new RankedDisharmony("app.a", ab, 1, 11, true, false, 0, false);
        ranked.setPriority(1);

        String row = report.renderPackageEdgeRow(ranked, false, "", dto);

        assertTrue(row.contains("app.a &#8594; app.b : 11"), row);
        assertFalse(row.contains("*"), row);
        assertTrue(row.contains("fix first"));
        assertTrue(row.contains("Closes a cycle of 2 packages. 11 uses to move."), row);
    }

    /** A and B use each other; returns A to B, with 7 uses. */
    private DefaultWeightedEdge twoClassCycle() {
        Graph<VertexInfo, DefaultWeightedEdge> classes = new DefaultDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        classes.addVertex(VertexInfo.forName("app.A"));
        classes.addVertex(VertexInfo.forName("app.B"));
        DefaultWeightedEdge ab = classes.addEdge(VertexInfo.forName("app.A"), VertexInfo.forName("app.B"));
        classes.setEdgeWeight(ab, 7);
        classes.addEdge(VertexInfo.forName("app.B"), VertexInfo.forName("app.A"));
        report.classGraph = classes;
        report.classCycles = Map.of(
                VertexInfo.forName("app.A"),
                new AsSubgraph<>(classes, Set.of(VertexInfo.forName("app.A"), VertexInfo.forName("app.B"))));
        when(dto.getClassToSourceFilePathMapping()).thenReturn(new HashMap<>());
        return ab;
    }
}
