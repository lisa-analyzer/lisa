package it.unive.lisa.program.cfg;

/**
 * A signature control flow graph, that has no graph implementation but just its
 * signature<br>
 * <br>
 * Note that this class does not implement {@link #equals(Object)} nor
 * {@link #hashCode()} since all cfgs are unique.
 * 
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 */
public class SignatureCFG implements CFG {

	/**
	 * The descriptor of this signature control flow graph.
	 */
	private final CFGDescriptor descriptor;

	/**
	 * Builds the signature control flow graph.
	 * 
	 * @param descriptor the descriptor of this signature cfg
	 */
	public SignatureCFG(CFGDescriptor descriptor) {
		this.descriptor = descriptor;
	}

	@Override
	public CFGDescriptor getDescriptor() {
		return descriptor;
	}
}
