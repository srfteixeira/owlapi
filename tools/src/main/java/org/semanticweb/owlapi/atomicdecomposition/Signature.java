package org.semanticweb.owlapi.atomicdecomposition;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.utilities.OWLAPIStreamUtils;

/**
 * class to hold the signature of a module
 */
public class Signature {

    /**
     * set to keep all the elements in signature
     */
    private final Set<OWLEntity> set = new HashSet<>();
    /**
     * true if concept TOP-locality; false if concept BOTTOM-locality
     */
    private boolean topCLocality = false;
    /**
     * true if role TOP-locality; false if role BOTTOM-locality
     */
    private boolean topRLocality = false;

    /**
     * empty signature
     */
    public Signature() {
        super();
    }

    /**
     * @param sig signature elements
     */
    public Signature(Stream<OWLEntity> sig) {
        addAll(sig);
    }

    /**
     * @param p entity to add to signature
     * @return true if p was not in the signature already
     */
    public boolean add(OWLEntity p) {
        return set.add(p);
    }

    /**
     * @param p all entities to add
     */
    public void addAll(Stream<OWLEntity> p) {
        OWLAPIStreamUtils.add(set, p);
    }

    /**
     * @param top set new locality polarity
     */
    public void setLocality(boolean top) {
        this.setLocality(top, top);
    }

    /**
     * @param topC new concept locality polarity
     * @param topR new role locality polarity
     */
    public void setLocality(boolean topC, boolean topR) {
        topCLocality = topC;
        topRLocality = topR;
    }

    /**
     * @param s signature to copy
     */
    public void setSignature(Signature s) {
        setSignature(s.set.stream());
        topCLocality = s.topCLocality;
        topRLocality = s.topRLocality;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (obj instanceof Signature sig) {
            return set.equals(sig.set);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return set.hashCode() * 37 + (topCLocality ? 1 : 0) + (topCLocality ? 1 : 0);
    }
    // comparison

    /**
     * @param p entity to find
     * @return true iff signature contains p
     */
    public boolean contains(OWLEntity p) {
        return set.contains(p);
    }

    /**
     * @return the set of entities
     */
    public Set<OWLEntity> getSignature() {
        return set;
    }

    /**
     * @param s signature to copy
     */
    public void setSignature(Stream<OWLEntity> s) {
        set.clear();
        addAll(s);
    }

    /**
     * @return true iff concepts are treated as TOPs
     */
    public boolean topCLocal() {
        return topCLocality;
    }

    /**
     * @return true iff roles are treated as TOPs
     */
    public boolean topRLocal() {
        return topRLocality;
    }
}
