package it.unive.lisa.analysis;

import it.unive.lisa.analysis.heap.MemoryOracle;
import it.unive.lisa.analysis.type.TypeOracle;
import it.unive.lisa.analysis.value.ValueOracle;

/**
 * An oracle that can be queried for semantic information the program under
 * analysis. Instances of this class can be used for inter-domain communication.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface SemanticOracle extends MemoryOracle, ValueOracle, TypeOracle {
}
