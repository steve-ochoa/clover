package org.openclover.eclipse.core.ui.editors.java;

import com.atlassian.clover.api.registry.BranchInfo;
import com.atlassian.clover.api.registry.ElementInfo;
import com.atlassian.clover.api.registry.MethodInfo;
import com.atlassian.clover.api.registry.StatementInfo;
import com.atlassian.clover.registry.entities.FullElementInfo;
import com.atlassian.clover.registry.entities.TestCaseInfo;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;

import java.lang.ref.WeakReference;
import java.util.Set;

public class CoverageAnnotation extends Annotation {
    public static enum Kind {
        NOT_COVERED("org.openclover.eclipse.core.coverageannotation.notcovered"),
        COVERED("org.openclover.eclipse.core.coverageannotation.covered"),
        FAILED(
            "org.openclover.eclipse.core.coverageannotation.failed",
            "(This coverage was generated by one or more tests which failed or had errors)"),
        FILTERED("org.openclover.eclipse.core.coverageannotation.filtered"),
        INCIDENTAL(
            "org.openclover.eclipse.core.coverageannotation.incidental",
            "(This coverage was not directly generated by test methods)"),
        PARTIAL_BRANCH("org.openclover.eclipse.core.coverageannotation.partialbranch", ""),
        /** Code was covered by at least one test case which succeeded */
        TEST_PASSED_COVERED("org.openclover.eclipse.core.coverageannotation.covered.testpassed", ""),
        /** Code was covered by at least one test case; none of them was successful */
        TEST_FAILED_COVERED("org.openclover.eclipse.core.coverageannotation.covered.testfailed", "");

        private final String id;
        private final String qualifier;

        Kind(String id, String qualifier) {
            this.id = id;
            this.qualifier = qualifier;
        }

        Kind(String id) {
            this(id, "");
        }

        public String getId() {
            return id;
        }

        public static Kind kindFor(boolean filtered, ElementInfo element, Set<TestCaseInfo> tcis) {
            // annotation priority is as follows:
            if (filtered) {
                // 1st - filtered out
                return FILTERED;
            }

            // 2nd - covered, in different variants
            if (hasCoverage(element)) {
                // partial coverage has priority
                if (element instanceof BranchInfo) {
                    BranchInfo branch = (BranchInfo)element;
                    if ((branch.getFalseHitCount() > 0) ^ (branch.getTrueHitCount() > 0)) {
                        return PARTIAL_BRANCH;
                    }
                }

                if (tcis.isEmpty()) {
                    return INCIDENTAL;
                } else {
                    if (allTestsFail(tcis)) {
                        return FAILED;
                    } else {
                        return COVERED;
                    }
                }
            }

            // 3rd - not covered at all
            return NOT_COVERED;
        }

        private static boolean allTestsFail(Set<TestCaseInfo> tcis) {
            for(TestCaseInfo tci : tcis) {
                if (tci.isSuccess()) {
                    return false;
                }
            }
            return true;
        }

        private static boolean hasCoverage(ElementInfo info) {
            if (info instanceof BranchInfo) {
                return
                    ((BranchInfo)info).getTrueHitCount() > 0
                    || ((BranchInfo)info).getFalseHitCount() > 0;
            } else {
                return info.getHitCount() > 0;
            }
        }

        public CoverageAnnotation newAnnotation(FullElementInfo info, Set<TestCaseInfo> hits, int offset, int length, boolean filtered) {
            return new CoverageAnnotation(this, info, hits, offset, length, filtered);
        }

        public String getQualifier() {
            return qualifier;
        }
    }

    private WeakReference<FullElementInfo> info;
    private Position position;

    private CoverageAnnotation(Kind kind, FullElementInfo info, Set<TestCaseInfo> hits, int offset, int length, boolean filtered) {
        super(kind.getId(), false, textForCoverage(info, kind, hits));
        this.info = new WeakReference<FullElementInfo>(info);
        this.position = new Position(offset, length);
    }

    public static String textForCoverage(ElementInfo info, Kind kind, Set<TestCaseInfo> hits) {
        if (kind != Kind.FILTERED) {
            final String message;
            if (info instanceof BranchInfo) {
                BranchInfo branchInfo = ((BranchInfo) info);
                message =
                    linePrefix(info) + "Expression evaluated to true "
                        + branchInfo.getTrueHitCount()
                        + " time" + (branchInfo.getTrueHitCount() == 1 ? "" : "s") + ", false "
                        + branchInfo.getFalseHitCount()
                        + " time" + (branchInfo.getFalseHitCount() == 1 ? "" : "s") + ".";
            } else if (info instanceof StatementInfo) {
                message =
                    linePrefix(info) + "Statement executed "
                        + info.getHitCount()
                        + " time" + (info.getHitCount() == 1 ? "" : "s") +".";
            } else if (info instanceof MethodInfo) {
                message =
                    linePrefix(info) + (((MethodInfo)info).isTest() ? "Test method" : "Method")
                        + " executed "
                        + info.getHitCount()
                        + " time" + (info.getHitCount() == 1 ? "" : "s") +".";
            } else {
                message = "";
            }
            final String qualifier = kind.getQualifier();
            return qualifier.length() == 0 ? message : (message + "\n" + qualifier);
        } else {
            return linePrefix(info) + "Excluded due to filter settings.";
        }
    }

    private static String linePrefix(ElementInfo info) {
        int start = info.getStartLine();
        int end = info.getEndLine();
        return
            ((end - start) > 0)
                ? ("Lines " + info.getStartLine() + "-" + info.getEndLine() + ": ")
                : ("Line " + info.getStartLine() + ": ");
    }

    public Position getPosition() {
        return position;
    }

    public boolean encloses(int offset) {
        return position.offset <= offset && offset - position.offset <= position.length;
    }

    public void reposition(int newOffset, int newLength) {
        position = new Position(newOffset, newLength);
    }

    public FullElementInfo getInfo() {
        return info.get();
    }
}
