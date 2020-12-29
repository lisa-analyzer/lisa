package it.unive.lisa.analysis;

import it.unive.lisa.DefaultImplementation;
import it.unive.lisa.analysis.heap.MonolithicHeap;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.HeapExpression;
import it.unive.lisa.symbolic.value.HeapIdentifier;
import it.unive.lisa.symbolic.value.Identifier;

/**
 * A semantic domain that can evaluate the semantic of statements that operate
 * on heap locations, and not on concrete values. A heap domain can handle
 * instances of {@link HeapExpression}s, and manage identifiers that are
 * {@link HeapIdentifier}s.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <D> the concrete type of the {@link HeapDomain}
 */
@DefaultImplementation(MonolithicHeap.class)
public interface HeapDomain<D extends HeapDomain<D>>
		extends SemanticDomain<D, SymbolicExpression, Identifier>, Lattice<D>, HeapSemanticOperation {
}
