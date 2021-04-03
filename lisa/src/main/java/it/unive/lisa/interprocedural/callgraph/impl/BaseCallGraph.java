package it.unive.lisa.interprocedural.callgraph.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

import it.unive.lisa.interprocedural.callgraph.CallGraph;
import it.unive.lisa.interprocedural.callgraph.CallGraphConstructionException;
import it.unive.lisa.interprocedural.callgraph.CallResolutionException;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.NativeCFG;
import it.unive.lisa.program.cfg.statement.CFGCall;
import it.unive.lisa.program.cfg.statement.Call;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.OpenCall;
import it.unive.lisa.program.cfg.statement.UnresolvedCall;
import it.unive.lisa.type.Type;

/**
 * An instance of {@link CallGraph} that does not handle interprocedurality. In
 * particular:
 * <ul>
 * <li>resolves {@link UnresolvedCall} to all the {@link CFG}s that match the
 * target's signature.
 * {@link BaseCallGraph#getPossibleTypesOfReceiver(Expression)} provides
 * the possible types of the receiver that might be reachable, and from where we
 * might get the method implementation to analyze</li>
 * <li>returns top when asked for the abstract result of a {@link CFGCall}</li>
 * </ul>
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a> and
 *             <a href="mailto:pietro.ferrara@unive.it">Pietro Ferrara</a>
 */
public abstract class BaseCallGraph implements CallGraph {

	private Program program;

	@Override
	public final void build(Program program) throws CallGraphConstructionException {
		this.program = program;
	}

	@Override
	public final Call resolve(UnresolvedCall call) throws CallResolutionException {
		Collection<CodeMember> targets = new ArrayList<>();

		if (call.isInstanceCall()) {
			if (call.getParameters().length == 0)
				throw new CallResolutionException(
						"An instance call should have at least one parameter to be used as the receiver of the call");
			Expression receiver = call.getParameters()[0];
			for (Type recType : getPossibleTypesOfReceiver(receiver)) {
				if (!recType.isUnitType())
					continue;

				CompilationUnit unit = recType.asUnitType().getUnit();
				Collection<CodeMember> candidates = unit.getInstanceCodeMembersByName(call.getTargetName(), true);
				for (CodeMember candidate : candidates)
					if (call.getStrategy().matches(candidate.getDescriptor().getArgs(), call.getParameters()))
						targets.add(candidate);
			}
		} else {
			for (CodeMember cm : program.getAllCodeMembers())
				if (cm.getDescriptor().isInstance() && cm.getDescriptor().getName().equals(call.getTargetName())
						&& call.getStrategy().matches(cm.getDescriptor().getArgs(), call.getParameters()))
					targets.add(cm);
		}

		Call resolved;
		if (targets.isEmpty())
			resolved = new OpenCall(call.getCFG(), call.getLocation(),
					call.getTargetName(), call.getStaticType(), call.getParameters());
		else if (targets.size() == 1 && targets.iterator().next() instanceof NativeCFG)
			resolved = ((NativeCFG) targets.iterator().next()).rewrite(call, call.getParameters());
		else {
			if (targets.stream().anyMatch(t -> t instanceof NativeCFG))
				throw new CallResolutionException(
						"Hybrid resolution is not supported: when more than one target is present, they must all be CFGs and not NativeCFGs");

			resolved = new CFGCall(call.getCFG(), call.getLocation(), call.getTargetName(),
					targets.stream().map(t -> (CFG) t).collect(Collectors.toList()),
					call.getParameters());
		}

		resolved.setOffset(call.getOffset());
		return resolved;
	}

	/**
	 * Returns all the possible types of the given expression, that is a
	 * receiver of a method call. How we choose this set varies from the call
	 * graph algorithm we decide to adopt (e.g., CHA, RTA, 0-CFA, ...)
	 * 
	 * @param receiver an expression
	 * 
	 * @return the possible types of the given expression
	 */
	protected abstract Collection<Type> getPossibleTypesOfReceiver(Expression receiver);

}
