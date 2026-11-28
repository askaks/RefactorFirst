package org.hjug.graphbuilder.graphbuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.hjug.graphbuilder.GraphDependencyCollector;
import org.hjug.graphbuilder.VertexInfo;
import org.hjug.graphbuilder.metrics.ClassMetrics;
import org.hjug.graphbuilder.metrics.GraphMetricsCollector;
import org.hjug.graphbuilder.metrics.MethodMetrics;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.junit.jupiter.api.Test;

/**
 * Reads the facts file that csharp-fact-extractor produces for its TinyGreeter fixture.
 * The extractor's own ContractTests keep that file in step with the C# side.
 */
class CSharpFactsReplayerTest {

    @Test
    void givenTinyGreeterFacts_whenReplayed_thenGraphHasTheCycleAndMetrics() throws Exception {
        Graph<VertexInfo, DefaultWeightedEdge> classGraph =
                new DefaultDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        Graph<VertexInfo, DefaultWeightedEdge> packageGraph =
                new DefaultDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        GraphDependencyCollector dependencies = new GraphDependencyCollector(classGraph, packageGraph);
        GraphMetricsCollector metrics = new GraphMetricsCollector(classGraph, packageGraph);
        Map<String, String> classToFile = new HashMap<>();

        try (InputStream in = getClass().getResourceAsStream("/csharp/tiny-greeter-facts.json")) {
            CSharpFactsReplayer.replay(CSharpFacts.read(in), dependencies, metrics, classToFile);
        }

        assertEquals(3, classGraph.vertexSet().size());
        assertEquals(4.0, weight(classGraph, "TinyGreeter.Greeter", "TinyGreeter.MessageStore"));
        assertEquals(2.0, weight(classGraph, "TinyGreeter.MessageStore", "TinyGreeter.Greeter"));
        assertEquals(3.0, weight(classGraph, "TinyGreeter.Program", "TinyGreeter.Greeter"));
        assertEquals("Greeter.cs", classToFile.get("TinyGreeter.Greeter"));
        assertTrue(dependencies.getPackagesInCodebase().contains("TinyGreeter"));

        ClassMetrics greeter = metrics.getClassMetrics("TinyGreeter.Greeter");
        assertEquals(2, greeter.getAttributes().size());
        assertEquals(0, greeter.getNumberOfPublicAttributes());
        assertEquals(4, greeter.getNumberOfMethods());
        assertEquals(2, greeter.getNumberOfAccessorMethods());
        assertEquals(5, greeter.getWeightedMethodCount());

        MethodMetrics greet = greeter.getMethods().get("Greet(string)");
        assertEquals(2, greet.getCouplingIntensity());
        assertEquals(2, greet.getNumberOfAccessedVariables());
        assertEquals(1, greet.getMaxNestingDepth());
    }

    /**
     * End to end through the real .NET extractor: methods are kept one per signature, so overloads
     * that differ only by a type parameter or by ref/out, and a static constructor beside an instance
     * one, must each arrive under their own signature. Runs only when the extractor is configured.
     */
    @Test
    void givenRealExtractor_whenOverloadsAreReplayed_thenEveryMethodIsKept() throws Exception {
        String extractor = System.getenv(CSharpExtractorRunner.EXTRACTOR_ENV);
        assumeTrue(extractor != null && Files.exists(Path.of(extractor)), "extractor not configured");
        Path fixture = Path.of("..", "csharp-fact-extractor", "fixtures", "Members")
                .toAbsolutePath()
                .normalize();
        assumeTrue(Files.isDirectory(fixture), "fixture not found");

        Graph<VertexInfo, DefaultWeightedEdge> classGraph =
                new DefaultDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        Graph<VertexInfo, DefaultWeightedEdge> packageGraph =
                new DefaultDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        GraphDependencyCollector dependencies = new GraphDependencyCollector(classGraph, packageGraph);
        GraphMetricsCollector metrics = new GraphMetricsCollector(classGraph, packageGraph);

        Path facts = new CSharpExtractorRunner().run(CSharpExtractorRunner.findTargets(fixture), fixture);
        try (InputStream in = Files.newInputStream(facts)) {
            CSharpFactsReplayer.replay(CSharpFacts.read(in), dependencies, metrics, new HashMap<>());
        } finally {
            Files.deleteIfExists(facts);
        }

        assertEquals(
                Set.of("Add(int)", "Add(ref int)", "Read(out int)", "Save()", "Save<T>()"),
                metrics.getClassMetrics("Members.Overloads").getMethods().keySet());
        assertEquals(
                Set.of("Cache()", "static Cache()", "Size()"),
                metrics.getClassMetrics("Members.Cache").getMethods().keySet());
    }

    @Test
    void givenASubclassInTheFacts_whenReplayed_thenParentOverridesAndUsedMembersReachTheMetrics() throws Exception {
        Graph<VertexInfo, DefaultWeightedEdge> classGraph =
                new DefaultDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        Graph<VertexInfo, DefaultWeightedEdge> packageGraph =
                new DefaultDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        GraphDependencyCollector dependencies = new GraphDependencyCollector(classGraph, packageGraph);
        GraphMetricsCollector metrics = new GraphMetricsCollector(classGraph, packageGraph);

        try (InputStream in = getClass().getResourceAsStream("/csharp/two-projects-facts.json")) {
            CSharpFactsReplayer.replay(CSharpFacts.read(in), dependencies, metrics, new HashMap<>());
        }

        ClassMetrics parent = metrics.getClassMetrics("Beta.BaseStore");
        ClassMetrics child = metrics.getClassMetrics("Beta.BetaStore");
        assertEquals(3, parent.getNumberOfProtectedMembers());
        assertEquals("Beta.BaseStore", child.getParentClass());
        assertEquals(1, child.getNumberOfOverriddenMethods());
        assertEquals(2, child.getNumberOfUsedParentMembers());
    }

    @Test
    void givenUnknownSchemaVersion_whenRead_thenRejectedWithAClearMessage() {
        String json = "{\"schemaVersion\": 99, \"root\": \".\", \"namespaces\": [], \"types\": []}";

        IOException e = assertThrows(
                IOException.class,
                () -> CSharpFacts.read(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))));

        assertTrue(e.getMessage().contains("99"));
    }

    @Test
    void givenUnknownFields_whenRead_thenTheyAreIgnored() throws Exception {
        String json =
                "{\"schemaVersion\": 1, \"root\": \".\", \"somethingNew\": true, \"namespaces\": [], \"types\": []}";

        CSharpFacts facts = CSharpFacts.read(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

        assertEquals(0, facts.types().size());
    }

    /**
     * The class name is what the God Class section is given. It has to be the fqn, the name every other
     * section is given, or the report shows one class under two names; the plain name would also make
     * two file-local types with one name two rows called "Helper".
     */
    @Test
    void givenTypesToldApartOnlyInTheFqn_whenReplayed_thenTheClassNameKeepsWhatTellsThemApart() throws Exception {
        String json = "{\"schemaVersion\": 1, \"root\": \".\", \"namespaces\": [\"Shop\"], \"types\": ["
                + type("Shop.Helper@FirstFile", "Shop", "Helper", "FirstFile.cs") + ","
                + type("Shop.Helper@SecondFile", "Shop", "Helper", "SecondFile.cs") + ","
                + type("Shop.Order$Line", "Shop", "Line", "Order.cs") + ","
                + type("Loose", "", "Loose", "Loose.cs") + "]}";
        Graph<VertexInfo, DefaultWeightedEdge> classGraph =
                new DefaultDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        Graph<VertexInfo, DefaultWeightedEdge> packageGraph =
                new DefaultDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        GraphMetricsCollector metrics = new GraphMetricsCollector(classGraph, packageGraph);

        CSharpFactsReplayer.replay(
                CSharpFacts.read(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))),
                new GraphDependencyCollector(classGraph, packageGraph),
                metrics,
                new HashMap<>());

        assertEquals(
                "Shop.Helper@FirstFile",
                metrics.getClassMetrics("Shop.Helper@FirstFile").getClassName());
        assertEquals(
                "Shop.Helper@SecondFile",
                metrics.getClassMetrics("Shop.Helper@SecondFile").getClassName());
        // A nested type keeps its whole fqn too: that is a key of the source-file map, so the
        // change-history lookup does not take the "$" for a lambda and drop the God Class row.
        assertEquals(
                "Shop.Order$Line", metrics.getClassMetrics("Shop.Order$Line").getClassName());
        assertEquals("Loose", metrics.getClassMetrics("Loose").getClassName());
    }

    /**
     * A tagged file type ("Tool@Dotted_Part~2") and an extension member signed with its receiver ("Twice(Order)")
     * carry characters no Java name has. A call to either still has to find its method: the caller names it
     * exactly as the callee's fqn + "." + signature.
     */
    @Test
    void givenATaggedFileTypeAndAnExtensionMember_whenReplayed_thenCallsToThemFindTheirMethods() throws Exception {
        String json = "{\"schemaVersion\": 1, \"root\": \".\", \"namespaces\": [\"Shop\"], \"types\": ["
                + "{\"fqn\": \"Shop.Tool@Dotted_Part~2\", \"namespace\": \"Shop\", \"name\": \"Tool\","
                + " \"sourceFile\": \"Dotted_Part~2.cs\", \"loc\": 1,"
                + " \"methods\": [{\"name\": \"RunTilde\", \"signature\": \"RunTilde()\"}]},"
                + "{\"fqn\": \"Shop.Ext\", \"namespace\": \"Shop\", \"name\": \"Ext\","
                + " \"sourceFile\": \"Ext.cs\", \"loc\": 1,"
                + " \"methods\": [{\"name\": \"Twice\", \"signature\": \"Twice(Order)\"}]},"
                + "{\"fqn\": \"Shop.Caller\", \"namespace\": \"Shop\", \"name\": \"Caller\","
                + " \"sourceFile\": \"Caller.cs\", \"loc\": 1,"
                + " \"dependencies\": [{\"to\": \"Shop.Tool@Dotted_Part~2\", \"count\": 2},"
                + " {\"to\": \"Shop.Ext\", \"count\": 1}],"
                + " \"methods\": [{\"name\": \"Go\", \"signature\": \"Go()\", \"calledForeignMethods\":"
                + " [\"Shop.Tool@Dotted_Part~2.RunTilde()\", \"Shop.Ext.Twice(Order)\"]}]}]}";
        Graph<VertexInfo, DefaultWeightedEdge> classGraph =
                new DefaultDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        Graph<VertexInfo, DefaultWeightedEdge> packageGraph =
                new DefaultDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        GraphMetricsCollector metrics = new GraphMetricsCollector(classGraph, packageGraph);
        Map<String, String> classToFile = new HashMap<>();

        CSharpFactsReplayer.replay(
                CSharpFacts.read(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))),
                new GraphDependencyCollector(classGraph, packageGraph),
                metrics,
                classToFile);
        metrics.finalizeMetrics();

        assertEquals(3, classGraph.vertexSet().size());
        assertEquals(2.0, weight(classGraph, "Shop.Caller", "Shop.Tool@Dotted_Part~2"));
        assertEquals("Dotted_Part~2.cs", classToFile.get("Shop.Tool@Dotted_Part~2"));

        MethodMetrics runTilde =
                metrics.getClassMetrics("Shop.Tool@Dotted_Part~2").getMethods().get("RunTilde()");
        assertEquals(Set.of("Shop.Caller"), runTilde.getChangingClasses());
        assertEquals(Set.of("Shop.Caller::Go()"), runTilde.getChangingMethods());

        MethodMetrics twice = metrics.getClassMetrics("Shop.Ext").getMethods().get("Twice(Order)");
        assertEquals(Set.of("Shop.Caller"), twice.getChangingClasses());
        assertEquals(Set.of("Shop.Caller::Go()"), twice.getChangingMethods());
    }

    private static String type(String fqn, String namespace, String name, String sourceFile) {
        return "{\"fqn\": \"" + fqn + "\", \"namespace\": \"" + namespace + "\", \"name\": \"" + name
                + "\", \"sourceFile\": \"" + sourceFile + "\", \"loc\": 1}";
    }

    private static double weight(Graph<VertexInfo, DefaultWeightedEdge> graph, String from, String to) {
        VertexInfo source = graph.vertexSet().stream()
                .filter(v -> v.getName().equals(from))
                .findFirst()
                .orElseThrow();
        VertexInfo target = graph.vertexSet().stream()
                .filter(v -> v.getName().equals(to))
                .findFirst()
                .orElseThrow();
        return graph.getEdgeWeight(graph.getEdge(source, target));
    }
}
