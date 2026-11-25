package org.hjug.mavenreport;

import java.io.File;
import org.apache.maven.model.Reporting;
import org.apache.maven.project.MavenProject;
import org.hjug.refactorfirst.report.ReportWriter;

final class MavenReportOutputDirectory {

    static String resolve(MavenProject project, File buildDirectory) {
        Reporting reporting = project.getModel().getReporting();
        String configuredDirectory = reporting == null ? null : reporting.getOutputDirectory();
        if (isUnset(configuredDirectory)) {
            File effectiveBuildDirectory =
                    buildDirectory == null || isUnset(buildDirectory.getPath()) ? null : buildDirectory;
            if (effectiveBuildDirectory == null
                    && project.getBuild() != null
                    && !isUnset(project.getBuild().getDirectory())) {
                effectiveBuildDirectory = new File(project.getBuild().getDirectory());
            }
            if (effectiveBuildDirectory == null) {
                effectiveBuildDirectory = new File(project.getBasedir(), "target");
            }
            configuredDirectory = new File(effectiveBuildDirectory, "site").getPath();
        }
        return ReportWriter.containReportDirectory(project.getBasedir(), configuredDirectory);
    }

    /**
     * The {@code report} goal takes its directory from Maven's report base class. That directory is kept
     * when Maven resolved it; run without a pom.xml it is not resolved, and the report goes where the
     * other goals write it.
     */
    static String resolveSiteReport(MavenProject project, File reportOutputDirectory) {
        if (reportOutputDirectory == null || isUnset(reportOutputDirectory.getPath())) {
            return resolve(project, null);
        }
        return reportOutputDirectory.getAbsolutePath();
    }

    /**
     * Run in a directory with no pom.xml, Maven supplies a stub project whose super-POM defaults are
     * not interpolated, e.g. "${project.basedir}/target/site". Such a value is treated as not set.
     */
    private static boolean isUnset(String directory) {
        return directory == null || directory.isBlank() || directory.contains("${");
    }

    private MavenReportOutputDirectory() {}
}
