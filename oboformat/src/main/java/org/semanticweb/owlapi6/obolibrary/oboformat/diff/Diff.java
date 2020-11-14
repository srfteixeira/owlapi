package org.semanticweb.owlapi6.obolibrary.oboformat.diff;

import static org.semanticweb.owlapi6.utilities.OWLAPIPreconditions.verifyNotNull;

import javax.annotation.Nullable;

import org.semanticweb.owlapi6.obolibrary.oboformat.model.Clause;
import org.semanticweb.owlapi6.obolibrary.oboformat.model.Frame;

/**
 * Diff.
 */
public class Diff {

    final String frameType;
    final int frameNum;
    String type;
    @Nullable
    Frame frame1;
    @Nullable
    Frame frame2;
    @Nullable
    Clause clause1;
    @Nullable
    Clause clause2;

    /**
     * @param ftype frame type
     * @param type  type
     * @param f1    frame 1
     * @param n     n
     */
    public Diff(String ftype, String type, Frame f1, int n) {
        this(ftype, type, f1, null, null, n);
    }

    /**
     * @param ftype frame type
     * @param type  type
     * @param f1    frame 1
     * @param f2    frame 2
     * @param c     c
     * @param n     n
     */
    public Diff(String ftype, String type, Frame f1, @Nullable Frame f2, @Nullable Clause c,
        int n) {
        this.type = type;
        frame1 = f1;
        frame2 = f2;
        clause1 = c;
        frameNum = n;
        frameType = ftype;
    }

    /**
     * @return type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return frame1
     */
    @Nullable
    public Frame getFrame1() {
        return frame1;
    }

    /**
     * @param frame1 frame1
     */
    public void setFrame1(Frame frame1) {
        this.frame1 = frame1;
    }

    /**
     * @return frame2
     */
    @Nullable
    public Frame getFrame2() {
        return frame2;
    }

    /**
     * @param frame2 frame2
     */
    public void setFrame2(Frame frame2) {
        this.frame2 = frame2;
    }

    /**
     * @return clause1
     */
    @Nullable
    public Clause getClause1() {
        return clause1;
    }

    /**
     * @param clause1 clause1
     */
    public void setClause1(Clause clause1) {
        this.clause1 = clause1;
    }

    /**
     * @return clause2
     */
    @Nullable
    public Clause getClause2() {
        return clause2;
    }

    /**
     * @param clause2 clause2
     */
    public void setClause2(Clause clause2) {
        this.clause2 = clause2;
    }

    @Override
    public String toString() {
        return type + ' ' + frameType + " Frame1=" + id(frame1) + " Frame2=" + id(frame2)
            + " Clause1=" + (clause1 == null ? "-" : clause1) + " Clause2="
            + (clause2 == null ? "-" : clause2) + " In=Frame" + frameNum;
    }

    @Nullable
    protected String id(@Nullable Frame f) {
        if (f == null) {
            return "-";
        }
        return verifyNotNull(f).getId();
    }
}