package it.unive.lisa.analysis.combination.constraints;

import java.util.Set;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.NonRelationalValueDomain;
import it.unive.lisa.symbolic.value.BinaryExpression;

public interface WholeValueDomain<D extends WholeValueDomain<D>> 
    extends NonRelationalValueDomain<D> {
    
    Set<BinaryExpression> constraints() throws SemanticException;

    D generate(Set<BinaryExpression> constraints) throws SemanticException;
}
