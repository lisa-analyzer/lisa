package it.unive.lisa.interprocedural.callgraph.events;

import it.unive.lisa.analysis.symbols.SymbolAliasing;
import it.unive.lisa.events.Event;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.type.Type;
import java.util.Set;

/**
 * An event signaling that a call has been resolved.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class CallResolved
		extends
		Event {

	private final UnresolvedCall original;
	private final Set<Type>[] types;
	private final SymbolAliasing aliasing;
	private final Call resolved;

	/**
	 * Builds the event.
	 * 
	 * @param original the original unresolved call
	 * @param types    the types computed for the call arguments
	 * @param aliasing the symbol aliasing information at the call site
	 * @param resolved the resolved call
	 */
	public CallResolved(
			UnresolvedCall original,
			Set<Type>[] types,
			SymbolAliasing aliasing,
			Call resolved) {
		this.original = original;
		this.types = types;
		this.aliasing = aliasing;
		this.resolved = resolved;
	}

	/**
	 * Yields the original unresolved call.
	 * 
	 * @return the original call
	 */
	public UnresolvedCall getOriginal() {
		return original;
	}

	/**
	 * Yields the types computed for the call arguments.
	 * 
	 * @return the argument types
	 */
	public Set<Type>[] getTypes() {
		return types;
	}

	/**
	 * Yields the symbol aliasing information at the call site.
	 * 
	 * @return the symbol aliasing
	 */
	public SymbolAliasing getAliasing() {
		return aliasing;
	}

	/**
	 * Yields the resolved call.
	 * 
	 * @return the resolved call
	 */
	public Call getResolved() {
		return resolved;
	}

}
