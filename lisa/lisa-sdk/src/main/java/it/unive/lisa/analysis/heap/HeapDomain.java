package it.unive.lisa.analysis.heap;

import org.apache.commons.lang3.tuple.Pair;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.HeapExpression;
import it.unive.lisa.symbolic.value.HeapLocation;
import it.unive.lisa.symbolic.value.Identifier;

/**
 * A semantic domain that can evaluate the semantic of statements that operate
 * on heap locations, and not on concrete values. A heap domain can handle
 * instances of {@link HeapExpression}s, and manage identifiers that are
 * {@link HeapLocation}s.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <D> the concrete type of the {@link HeapDomain}
 */
public interface HeapDomain<D extends HeapDomain<D>>
		extends
		MemoryOracle,
		SemanticDomain<D, SymbolicExpression, Identifier>,
		Lattice<D>,
		HeapSemanticOperation {
		
	default Pair<D, D> split(SymbolicExpression expr, ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle) throws SemanticException {		
		return Pair.of(this.assume(expr, src, dest, oracle), 
				this.assume(expr, src, dest, oracle));
	}
}
