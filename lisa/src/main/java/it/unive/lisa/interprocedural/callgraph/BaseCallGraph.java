package it.unive.lisa.interprocedural.callgraph;

import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.NativeCFG;
import it.unive.lisa.program.cfg.statement.CFGCall;
import it.unive.lisa.program.cfg.statement.Call;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.HybridCall;
import it.unive.lisa.program.cfg.statement.OpenCall;
import it.unive.lisa.program.cfg.statement.UnresolvedCall;
import it.unive.lisa.type.Type;
import java.util.ArrayList;
import java.util.Collection;

/**
 * An instance of {@link CallGraph} that does not handle interprocedurality. In
 * particular:
 * <ul>
 * <li>resolves {@link UnresolvedCall} to all the {@link CFG}s that match the
 * target's signature.
 * {@link BaseCallGraph#getPossibleTypesOfReceiver(Expression)} provides the
 * possible types of the receiver that might be reachable, and from where we
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
		Collection<CFG> targets = new ArrayList<>();
		Collection<NativeCFG> nativeTargets = new ArrayList<>();

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
				for (CodeMember cm : candidates)
					if (cm.getDescriptor().isInstance()
							&& call.getStrategy().matches(cm.getDescriptor().getArgs(), call.getParameters()))
						if (cm instanceof CFG)
							targets.add((CFG) cm);
						else
							nativeTargets.add((NativeCFG) cm);
			}
		} else {
			for (CodeMember cm : program.getAllCodeMembers())
				if (!cm.getDescriptor().isInstance() && cm.getDescriptor().getName().equals(call.getTargetName())
						&& call.getStrategy().matches(cm.getDescriptor().getArgs(), call.getParameters()))
					if (cm instanceof CFG)
						targets.add((CFG) cm);
					else
						nativeTargets.add((NativeCFG) cm);
		}

		Call resolved;
		if (targets.isEmpty() && nativeTargets.isEmpty())
			resolved = new OpenCall(call.getCFG(), call.getLocation(), call.getTargetName(), call.getStaticType(),
					call.getParameters());
		else if (nativeTargets.isEmpty())
			resolved = new CFGCall(call.getCFG(), call.getLocation(), call.getTargetName(), targets,
					call.getParameters());
		else
			resolved = new HybridCall(call.getCFG(), call.getLocation(), call.getTargetName(), targets, nativeTargets,
					call.getParameters());

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
