package org.hjug.refactorfirst.report;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hjug.cbc.RankedCycle;
import org.hjug.graphbuilder.CodebaseGraphDTO;
import org.hjug.graphbuilder.VertexInfo;
import org.hjug.refactorfirst.report.model.ClassRelationshipDTO;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * A nested C# class reads "Order.Line" in the cycle and relationship tables too, the name the
 * findings tables and the class map show. Java and Kotlin keep "Outer$Inner".
 */
class CSharpNestedClassLabelsTest {

    private static final String NESTED_CSHARP = "Shop.Order$Line";
    private static final String CSHARP = "Shop.Clerk";
    private static final String NESTED_JAVA = "shop.Outer$Inner";

    private final Graph<VertexInfo, DefaultWeightedEdge> classGraph =
            new DefaultDirectedWeightedGraph<>(DefaultWeightedEdge.class);

    private DefaultWeightedEdge addEdge(String from, String to) {
        classGraph.addVertex(VertexInfo.forName(from));
        classGraph.addVertex(VertexInfo.forName(to));
        DefaultWeightedEdge edge = classGraph.addEdge(VertexInfo.forName(from), VertexInfo.forName(to));
        classGraph.setEdgeWeight(edge, 3);
        return edge;
    }

    private static CodebaseGraphDTO dto() {
        Map<String, String> sourcePaths = new HashMap<>();
        sourcePaths.put(NESTED_CSHARP, "src/Shop/Order.cs");
        sourcePaths.put(CSHARP, "src/Shop/Clerk.cs");
        sourcePaths.put(NESTED_JAVA, "src/main/java/shop/Outer.java");
        CodebaseGraphDTO dto = Mockito.mock(CodebaseGraphDTO.class);
        Mockito.when(dto.getClassToSourceFilePathMapping()).thenReturn(sourcePaths);
        return dto;
    }

    private ReportModelBuilder builder(CodebaseGraphDTO dto) {
        ReportModelBuilder builder = new ReportModelBuilder();
        builder.classGraph = classGraph;
        builder.codebaseGraphDTO = dto;
        return builder;
    }

    @DisplayName("the class relationships behind a package relationship name a nested C# class with a dot")
    @Test
    void classRelationshipsToBreakPackage_nameANestedCSharpClassWithADot() {
        DefaultWeightedEdge csharpEdge = addEdge(NESTED_CSHARP, CSHARP);
        DefaultWeightedEdge javaEdge = addEdge(NESTED_JAVA, CSHARP);
        CodebaseGraphDTO dto = dto();

        List<ClassRelationshipDTO> rows =
                builder(dto).buildClassRelationshipsToBreakPackage(Set.of(csharpEdge, javaEdge), dto);

        ClassRelationshipDTO csharpRow = rows.stream()
                .filter(r -> r.getSourceClass().equals(NESTED_CSHARP))
                .findFirst()
                .orElseThrow();
        assertEquals("Order.Line", csharpRow.getSimpleSourceClassName());
        assertEquals("Clerk", csharpRow.getSimpleTargetClassName());
        // the full name the row is keyed on is untouched
        assertEquals(NESTED_CSHARP, csharpRow.getSourceClass());

        ClassRelationshipDTO javaRow = rows.stream()
                .filter(r -> r.getSourceClass().equals(NESTED_JAVA))
                .findFirst()
                .orElseThrow();
        assertEquals("Outer$Inner", javaRow.getSimpleSourceClassName());
    }

    @DisplayName("an edge in the largest cycle names a nested C# class with a dot, on either end")
    @Test
    void renderClassEdge_namesANestedCSharpClassWithADot() {
        DefaultWeightedEdge out = addEdge(NESTED_CSHARP, CSHARP);
        DefaultWeightedEdge back = addEdge(CSHARP, NESTED_CSHARP);
        DefaultWeightedEdge java = addEdge(NESTED_JAVA, CSHARP);
        ReportModelBuilder builder = builder(dto());

        assertEquals("Order.Line &#8594; Clerk : 3", builder.renderClassEdge(out));
        assertEquals("Clerk &#8594; Order.Line : 3", builder.renderClassEdge(back));
        assertEquals("Outer$Inner &#8594; Clerk : 3", builder.renderClassEdge(java));
    }

    @DisplayName("a cycle named after a nested C# class is listed with a dot")
    @Test
    void rankedCycleSummary_namesANestedCSharpClassWithADot() {
        CodebaseGraphDTO dto = dto();

        assertEquals(
                "Order.Line",
                SimpleHtmlReport.getRankedCycleSummaryData(
                        new RankedCycle(NESTED_CSHARP, Set.of(), Set.of(), List.of()), dto)[0]);
        assertEquals(
                "Outer$Inner",
                SimpleHtmlReport.getRankedCycleSummaryData(
                        new RankedCycle(NESTED_JAVA, Set.of(), Set.of(), List.of()), dto)[0]);
    }
}
