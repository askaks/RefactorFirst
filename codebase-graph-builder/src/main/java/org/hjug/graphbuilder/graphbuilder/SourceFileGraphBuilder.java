package org.hjug.graphbuilder.graphbuilder;

import java.io.IOException;
import java.nio.file.Path;
import org.hjug.graphbuilder.CodebaseGraphDTO;
import org.hjug.graphbuilder.GraphBuilderConfig;

/**
 * Strategy for building a {@link CodebaseGraphDTO} from source files of a
 * specific language (Java, Kotlin, etc.).
 */
public interface SourceFileGraphBuilder {

    /**
     * True when the file sits inside the configured directory. Several directories may be given,
     * separated by commas, for repositories that keep fixtures outside their test folders.
     */
    static boolean isInConfiguredDirectory(Path file, String directory) {
        String normalizedFile = "/" + file.toString().replace('\\', '/') + "/";
        for (String one : directory.split(",")) {
            String normalizedDirectory = one.trim().replace('\\', '/').replaceAll("^/+|/+$", "");
            if (!normalizedDirectory.isEmpty() && normalizedFile.contains("/" + normalizedDirectory + "/")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Build a {@link CodebaseGraphDTO} representing class/package dependency
     * graphs and disharmony metrics for the given source repository.
     *
     * @param repositoryPath  path to the root of the source directory (source root)
     * @param repositoryRoot  path to the Git repository root for URL canonicalization;
     *                        may be empty or equal to repositoryPath for single-module projects
     * @param config          graph-builder configuration
     * @return fully populated CodebaseGraphDTO
     * @throws IOException if source parsing fails due to filesystem issues
     */
    CodebaseGraphDTO buildGraph(String repositoryPath, String repositoryRoot, GraphBuilderConfig config)
            throws IOException;
}
