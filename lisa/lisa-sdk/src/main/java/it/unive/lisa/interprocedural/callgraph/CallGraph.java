package it.unive.lisa.interprocedural.callgraph;

import it.unive.lisa.analysis.symbols.SymbolAliasing;
import it.unive.lisa.program.Application;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.type.Type;
import java.util.Collection;
import java.util.Set;

/**
 * A callgraph of the program to analyze, that knows how to resolve dynamic
 * targets of {@link UnresolvedCall}s.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public interface CallGraph {

	/**
	 * Initializes the call graph of the given program.
	 *
	 * @param app the application to analyze
	 *
	 * @throws CallGraphConstructionException if an exception happens while
	 *                                            building the call graph
	 */
	void init(Application app) throws CallGraphConstructionException;

	/**
	 * Yields a {@link Call} implementation that corresponds to the resolution
	 * of the given {@link UnresolvedCall}.
	 * 
	 * @param call     the call to resolve
	 * @param types    the runtime types of the parameters of the call
	 * @param aliasing the symbol aliasing information
	 * 
	 * @return a collection of all the possible runtime targets
	 * 
	 * @throws CallResolutionException if this call graph is unable to resolve
	 *                                     the given call
	 */
	Call resolve(UnresolvedCall call, Set<Type>[] types, SymbolAliasing aliasing)
			throws CallResolutionException;

	/**
	 * Registers an already resolved {@link CFGCall} in this {@link CallGraph}.
	 * 
	 * @param call the call to register
	 */
	void registerCall(CFGCall call);

	/**
	 * Yields all the {@link CodeMember}s that call the given one. The returned
	 * collection might contain partial results if this call graph is not fully
	 * built.
	 * 
	 * @param cm the target code member
	 * 
	 * @return the collection of callers code members
	 */
	Collection<CodeMember> getCallers(CodeMember cm);

	/**
	 * Yields all the {@link CodeMember}s that are called by the given one. The
	 * returned collection might contain partial results if this call graph is
	 * not fully built.
	 * 
	 * @param cm the target code member
	 * 
	 * @return the collection of called code members
	 */
	Collection<CodeMember> getCallees(CodeMember cm);

	/**
	 * Yields all the {@link Call}s that targets the given {@link CodeMember}.
	 * The returned collection might contain partial results if this call graph
	 * is not fully built.
	 * 
	 * @param cm the target code member
	 * 
	 * @return the collection of calls that target the code member
	 */
	Collection<Call> getCallSites(CodeMember cm);
}
