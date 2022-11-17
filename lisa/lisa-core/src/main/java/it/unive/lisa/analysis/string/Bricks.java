package it.unive.lisa.analysis.string;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.representation.DomainRepresentation;

public class Bricks extends BaseNonRelationalValueDomain<Bricks> {


    @Override
    public Bricks lubAux(Bricks other) throws SemanticException {
        return null;
    }

    @Override
    public boolean lessOrEqualAux(Bricks other) throws SemanticException {
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public Bricks top() {
        return null;
    }

    @Override
    public Bricks bottom() {
        return null;
    }

    @Override
    public DomainRepresentation representation() {
        return null;
    }
}
