package org.hjug.cbc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.eclipse.jgit.api.Git;
import org.hjug.graphbuilder.CodebaseGraphDTO;
import org.hjug.graphbuilder.graphbuilder.CSharpSourceFileGraphBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * A C# nested class that is a God Class must keep its row, from the facts the extractor writes
 * ("Shop.Order$Line") through God Class detection to the change history of the file it is in.
 * A "$" in its class name used to send the change-history lookup to a key that does not exist,
 * and the row was dropped.
 */
class CSharpNestedGodClassTest {

    @TempDir
    File repo;

    @TempDir
    Path work;

    private Git git;

    @BeforeEach
    void setUp() throws Exception {
        git = Git.init().setDirectory(repo).call();
    }

    @AfterEach
    void tearDown() {
        git.getRepository().close();
    }

    /** Reaches the facts-file entry point without running .NET. */
    private static final class FromFacts extends CSharpSourceFileGraphBuilder {
        CodebaseGraphDTO build(Path facts) throws IOException {
            return buildGraphFromFacts(facts);
        }
    }

    @Test
    void givenANestedCSharpGodClass_whenRanked_thenItKeepsItsRowAndTheHistoryOfItsFile() throws Exception {
        Files.writeString(
                repo.toPath().resolve("Order.cs"), "namespace Shop; public class Order { public class Line { } }");
        git.add().addFilepattern(".").call();
        git.commit().setMessage("initial commit").call();

        // Line: six methods of complexity 10 (WMC 60), each using another type's data (ATFD 6)
        // and sharing no variable with the others (TCC 0): a God Class.
        String methods = IntStream.range(0, 6)
                .mapToObj(i -> "{\"name\": \"M" + i + "\", \"signature\": \"M" + i + "()\", \"loc\": 20,"
                        + " \"cyclo\": 10, \"accessedVariables\": [\"v" + i + "\"],"
                        + " \"accessedForeignClasses\": [\"Shop.Other" + i + "\"]}")
                .collect(Collectors.joining(","));
        String json = "{\"schemaVersion\": 1, \"root\": \".\", \"namespaces\": [\"Shop\"], \"types\": ["
                + "{\"fqn\": \"Shop.Order\", \"namespace\": \"Shop\", \"name\": \"Order\","
                + " \"sourceFile\": \"Order.cs\", \"loc\": 3},"
                + "{\"fqn\": \"Shop.Order$Line\", \"namespace\": \"Shop\", \"name\": \"Line\","
                + " \"sourceFile\": \"Order.cs\", \"loc\": 120, \"methods\": [" + methods + "]}]}";
        Path facts = work.resolve("facts.json");
        Files.writeString(facts, json, StandardCharsets.UTF_8);

        CodebaseGraphDTO graph = new FromFacts().build(facts);
        try (CostBenefitCalculator calculator =
                new CostBenefitCalculator(repo.getPath(), graph.getClassToSourceFilePathMapping())) {
            List<RankedDisharmony> ranked =
                    calculator.calculateGodClassCostBenefitValues(calculator.getGodClasses(graph));

            assertEquals(1, ranked.size());
            assertEquals("Order.cs", ranked.get(0).getPath());
            assertEquals(1, ranked.get(0).getCommitCount());
        }
    }
}
