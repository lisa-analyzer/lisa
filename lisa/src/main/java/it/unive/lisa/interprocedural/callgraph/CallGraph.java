package it.unive.lisa.interprocedural.callgraph;

import java.util.Collection;

import it.unive.lisa.DefaultImplementation;
import it.unive.lisa.interprocedural.callgraph.impl.RTACallGraph;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.statement.CFGCall;
import it.unive.lisa.program.cfg.statement.Call;
import it.unive.lisa.program.cfg.statement.OpenCall;
import it.unive.lisa.program.cfg.statement.UnresolvedCall;

/**
 * A callgraph of the program to analyze, that knows how to resolve dynamic
 * targets of {@link UnresolvedCall}s.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
@DefaultImplementation(RTACallGraph.class)
public interface CallGraph {

	/**
	 * Initializes the call graph of the given program.
	 *
	 * @param program the program to analyze
	 *
	 * @throws CallGraphConstructionException if an exception happens while
	 *                                            building the call graph
	 */
	void init(Program program) throws CallGraphConstructionException;

	/**
	 * Yields a {@link Call} implementation that corresponds to the resolution
	 * of the given {@link UnresolvedCall}. This method will return:
	 * <ul>
	 * <li>a {@link CFGCall}, if at least one {@link CFG} that matches
	 * {@link UnresolvedCall#getTargetName()} is found. The returned
	 * {@link CFGCall} will be linked to all the possible runtime targets
	 * matching {@link UnresolvedCall#getTargetName()};</li>
	 * <li>an {@link OpenCall}, if no {@link CFG} matching
	 * {@link UnresolvedCall#getTargetName()} is found.</li>
	 * </ul>
	 * 
	 * @param call the call to resolve
	 * 
	 * @return a collection of all the possible runtime targets
	 * 
	 * @throws CallResolutionException if this call graph is unable to resolve
	 *                                     the given call
	 */
	Call resolve(UnresolvedCall call) throws CallResolutionException;

	Collection<CodeMember> getCallers(CodeMember cm);
	
	Collection<CodeMember> getCallees(CodeMember cm);
}
