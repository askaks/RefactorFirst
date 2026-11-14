package org.hjug.cbc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.hjug.git.ScmLogInfo;
import org.hjug.metrics.DisharmonyInstance;
import org.junit.jupiter.api.Test;

/** Findings with the same raw priority: the one whose file changes more goes first, then by path. */
class PriorityTieTest {

    @Test
    void givenEqualRawPriority_whenSorted_thenTheFileWithMoreCommitsComesFirst() {
        List<RankedDisharmony> findings =
                new ArrayList<>(List.of(finding("a/Quiet.java", 0, 1), finding("b/Busy.java", 0, 9)));

        findings.sort(CostBenefitCalculator.BY_PRIORITY);

        assertEquals("b/Busy.java", findings.get(0).getPath());
    }

    @Test
    void givenDifferentRawPriority_whenSorted_thenRawPriorityStillDecides() {
        List<RankedDisharmony> findings =
                new ArrayList<>(List.of(finding("a/Busy.java", 0, 9), finding("b/Top.java", 3, 1)));

        findings.sort(CostBenefitCalculator.BY_PRIORITY);

        assertEquals("b/Top.java", findings.get(0).getPath());
    }

    @Test
    void givenEqualRawPriorityAndCommits_whenSorted_thenTheOrderIsByPath() {
        List<RankedDisharmony> findings =
                new ArrayList<>(List.of(finding("b/Two.java", 0, 4), finding("a/One.java", 0, 4)));

        findings.sort(CostBenefitCalculator.BY_PRIORITY);

        assertEquals("a/One.java", findings.get(0).getPath());
    }

    private static RankedDisharmony finding(String path, int rawPriority, int commits) {
        DisharmonyInstance instance = new DisharmonyInstance("God Class", "app.X", path, "app", null, List.of());
        instance.setOverallRank(1);
        ScmLogInfo log = new ScmLogInfo(path, "app.X", 1, 2, commits);
        log.setChangePronenessRank(1);
        RankedDisharmony ranked = new RankedDisharmony(instance, log);
        ranked.setRawPriority(rawPriority);
        return ranked;
    }
}
