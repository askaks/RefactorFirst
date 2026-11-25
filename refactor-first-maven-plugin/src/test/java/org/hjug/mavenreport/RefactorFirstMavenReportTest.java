package org.hjug.mavenreport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Reporting;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;

class RefactorFirstMavenReportTest {
    @Test
    void completeDocumentUsesExternalReportContract() {
        // Given: the site report that renders a complete HTML document.
        var report = new RefactorFirstMavenReport();
        // When / Then: Maven must use the external report path without site wrapping.
        assertTrue(report.isExternalReport());
    }

    @Test
    void outputDirectory_ignoresUnresolvedPlaceholdersWhenRunWithoutPom() {
        // Given: no pom.xml, so Maven hands the goal its default directory with the placeholder still in it.
        var report = new ReportWithDirectory(new File("${project.basedir}/target/reports"));
        String expected = Path.of("").toAbsolutePath().resolve("target/site").toString();
        // When / Then: both the site resources and the report itself go to target/site.
        assertEquals(expected, report.getOutputDirectory());
        assertEquals(expected, report.getReportOutputDirectory().getPath());
    }

    @Test
    void outputDirectory_keepsTheDirectoryMavenResolved() {
        // Given: a normal build, where Maven resolved the directory.
        File resolved = Path.of("").toAbsolutePath().resolve("target/reports").toFile();
        var report = new ReportWithDirectory(resolved);
        // When / Then: it is used as it is.
        assertEquals(resolved.getAbsolutePath(), report.getOutputDirectory());
    }

    /** Sets the two fields Maven injects, as the stub project in a directory with no pom.xml has them. */
    private static final class ReportWithDirectory extends RefactorFirstMavenReport {
        ReportWithDirectory(File injectedOutputDirectory) {
            Reporting reporting = new Reporting();
            reporting.setOutputDirectory("${project.basedir}/target/site");
            Build build = new Build();
            build.setDirectory("${project.basedir}/target");
            Model model = new Model();
            model.setReporting(reporting);
            model.setBuild(build);
            project = new MavenProject(model);
            outputDirectory = injectedOutputDirectory;
        }

        @Override
        public String getOutputDirectory() {
            return super.getOutputDirectory();
        }
    }
}
