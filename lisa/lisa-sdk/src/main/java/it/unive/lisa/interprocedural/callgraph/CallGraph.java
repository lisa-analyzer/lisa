package it.unive.lisa.interprocedural.callgraph;

import it.unive.lisa.analysis.symbols.SymbolAliasing;
import it.unive.lisa.program.Application;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.collections.workset.VisitOnceFIFOWorkingSet;
import it.unive.lisa.util.collections.workset.VisitOnceWorkingSet;
import it.unive.lisa.util.datastructures.graph.BaseGraph;
import it.unive.lisa.util.datastructures.graph.algorithms.SCCs;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A callgraph of the program to analyze, that knows how to resolve dynamic
 * targets of {@link UnresolvedCall}s.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class CallGraph extends BaseGraph<CallGraph, CallGraphNode, CallGraphEdge> {

	/**
	 * Initializes the call graph of the given program. A call to this method
	 * should effectively re-initialize the call graph as if it is yet to be
	 * used. This is useful when the same instance is used in multiple analyses.
	 *
	 * @param app the application to analyze
	 *
	 * @throws CallGraphConstructionException if an exception happens while
	 *                                            building the call graph
	 */
	public void init(Application app) throws CallGraphConstructionException {
		entrypoints.clear();
		adjacencyMatrix.clear();
	}

	/**
	 * Yields a {@link Call} implementation that corresponds to the resolution
	 * of the given {@link UnresolvedCall}.
	 * 
	 * @param call     the call to resolve
	 * @param types    the runtime types of the parameters of the call
	 * @param aliasing the symbol aliasing information, might be {@code null}
	 * 
	 * @return a collection of all the possible runtime targets
	 * 
	 * @throws CallResolutionException if this call graph is unable to resolve
	 *                                     the given call
	 */
	public abstract Call resolve(UnresolvedCall call, Set<Type>[] types, SymbolAliasing aliasing)
			throws CallResolutionException;

	/**
	 * Registers an already resolved {@link CFGCall} in this {@link CallGraph}.
	 * 
	 * @param call the call to register
	 */
	public abstract void registerCall(CFGCall call);

	/**
	 * Yields all the {@link Call}s that target the given {@link CodeMember}.
	 * The returned collection might contain partial results if this call graph
	 * is not fully built.
	 * 
	 * @param cm the target code member
	 * 
	 * @return the collection of calls that target the code member
	 */
	public abstract Collection<Call> getCallSites(CodeMember cm);

	/**
	 * Yields all the {@link Call}s that target at least one of the given
	 * {@link CodeMember}s. The returned collection might contain partial
	 * results if this call graph is not fully built.
	 * 
	 * @param cms the target code members
	 * 
	 * @return the collection of calls that target the code members
	 */
	public Collection<Call> getCallSites(Collection<? extends CodeMember> cms) {
		Set<Call> result = new HashSet<>();
		cms.forEach(cm -> getCallSites(cm).stream().forEach(result::add));
		return result;
	}

	/**
	 * Yields all the {@link CodeMember}s that call the given ones. The returned
	 * collection might contain partial results if this call graph is not fully
	 * built.
	 * 
	 * @param cms the target code members
	 * 
	 * @return the collection of callers code members
	 */
	public Collection<CodeMember> getCallers(Collection<? extends CodeMember> cms) {
		Set<CodeMember> result = new HashSet<>();
		cms.forEach(cm -> getCallers(cm).stream().forEach(result::add));
		return result;
	}

	/**
	 * Yields the transitive closure of {@link #getCallers(CodeMember)}. The
	 * returned collection might contain partial results if this call graph is
	 * not fully built.
	 * 
	 * @param cm the target code member
	 * 
	 * @return the collection of callers code members computed transitively
	 */
	public Collection<CodeMember> getCallersTransitively(CodeMember cm) {
		VisitOnceWorkingSet<CodeMember> ws = VisitOnceFIFOWorkingSet.mk();
		getCallers(cm).stream().forEach(ws::push);
		while (!ws.isEmpty())
			getCallers(ws.pop()).stream().forEach(ws::push);
		return ws.getSeen();
	}

	/**
	 * Yields the transitive closure of {@link #getCallers(CodeMember)} over
	 * each given code member. The returned collection might contain partial
	 * results if this call graph is not fully built.
	 * 
	 * @param cms the target code members
	 * 
	 * @return the collection of callers code members computed transitively
	 */
	public Collection<CodeMember> getCallersTransitively(Collection<? extends CodeMember> cms) {
		VisitOnceWorkingSet<CodeMember> ws = VisitOnceFIFOWorkingSet.mk();
		cms.forEach(cm -> getCallers(cm).stream().forEach(ws::push));
		while (!ws.isEmpty())
			getCallers(ws.pop()).stream().forEach(ws::push);
		return ws.getSeen();
	}

	/**
	 * Yields all the {@link CodeMember}s that are called by the given ones. The
	 * returned collection might contain partial results if this call graph is
	 * not fully built.
	 * 
	 * @param cms the target code members
	 * 
	 * @return the collection of callees code members
	 */
	public Collection<CodeMember> getCallees(Collection<? extends CodeMember> cms) {
		Set<CodeMember> result = new HashSet<>();
		cms.forEach(cm -> getCallees(cm).stream().forEach(result::add));
		return result;
	}

	/**
	 * Yields the transitive closure of {@link #getCallees(CodeMember)}. The
	 * returned collection might contain partial results if this call graph is
	 * not fully built.
	 * 
	 * @param cm the target code member
	 * 
	 * @return the collection of callees code members computed transitively
	 */
	public Collection<CodeMember> getCalleesTransitively(CodeMember cm) {
		VisitOnceWorkingSet<CodeMember> ws = VisitOnceFIFOWorkingSet.mk();
		getCallees(cm).stream().forEach(ws::push);
		while (!ws.isEmpty())
			getCallees(ws.pop()).stream().forEach(ws::push);
		return ws.getSeen();
	}

	/**
	 * Yields the transitive closure of {@link #getCallees(CodeMember)} of each
	 * given code member. The returned collection might contain partial results
	 * if this call graph is not fully built.
	 * 
	 * @param cms the target code members
	 * 
	 * @return the collection of callees code members computed transitively
	 */
	public Collection<CodeMember> getCalleesTransitively(Collection<? extends CodeMember> cms) {
		VisitOnceWorkingSet<CodeMember> ws = VisitOnceFIFOWorkingSet.mk();
		cms.forEach(cm -> getCallees(cm).stream().forEach(ws::push));
		while (!ws.isEmpty())
			getCallees(ws.pop()).stream().forEach(ws::push);
		return ws.getSeen();
	}

	/**
	 * Yields all the {@link CodeMember}s that are called by the given one. The
	 * returned collection might contain partial results if this call graph is
	 * not fully built.
	 * 
	 * @param cm the target code member
	 * 
	 * @return the collection of called code members
	 */
	public Collection<CodeMember> getCallees(CodeMember cm) {
		return followersOf(new CallGraphNode(this, cm)).stream()
				.map(CallGraphNode::getCodeMember)
				.collect(Collectors.toList());
	}

	/**
	 * Yields all the {@link CodeMember}s that call the given one. The returned
	 * collection might contain partial results if this call graph is not fully
	 * built.
	 * 
	 * @param cm the target code member
	 * 
	 * @return the collection of callers code members
	 */
	public Collection<CodeMember> getCallers(CodeMember cm) {
		return predecessorsOf(new CallGraphNode(this, cm)).stream()
				.map(CallGraphNode::getCodeMember)
				.collect(Collectors.toList());
	}

	/**
	 * Yields all the recursions that happens in the program, in the form of
	 * collections of the {@link CodeMember}s composing them.
	 * 
	 * @return the recursions
	 */
	public Collection<Collection<CodeMember>> getRecursions() {
		Collection<Collection<CallGraphNode>> sccs = new SCCs<
				CallGraph,
				CallGraphNode,
				CallGraphEdge>().buildNonTrivial(this);
		return sccs.stream()
				.map(nodes -> nodes.stream()
						.map(node -> node.getCodeMember())
						.collect(Collectors.toSet()))
				.collect(Collectors.toSet());
	}

	/**
	 * Yields all the recursions happening in the program, in the form of
	 * collections of the {@link CodeMember}s composing them, containing the
	 * given code member.
	 * 
	 * @param cm the target code member
	 * 
	 * @return the recursions
	 */
	public Collection<Collection<CodeMember>> getRecursionsContaining(CodeMember cm) {
		Collection<Collection<CallGraphNode>> sccs = new SCCs<
				CallGraph,
				CallGraphNode,
				CallGraphEdge>().buildNonTrivial(this);
		return sccs.stream()
				.map(nodes -> nodes.stream()
						.map(node -> node.getCodeMember())
						.collect(Collectors.toSet()))
				.filter(members -> members.contains(cm))
				.collect(Collectors.toSet());
	}
}
