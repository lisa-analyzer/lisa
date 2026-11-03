package it.unive.lisa.triage.inputs;

import it.unive.lisa.checks.semantic.SemanticTool;
import it.unive.lisa.interprocedural.callgraph.CallGraph;
import it.unive.lisa.interprocedural.callgraph.CallGraphNode;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.NativeCFG;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.UserInput;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.cfg.statement.call.OpenCall;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.PushFromConstraints;
import it.unive.lisa.util.datastructures.graph.GraphVisitor;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Resolves the unknown values introduced by the analysis (that is,
 * {@link PushAny}s, possibly with constraints as {@link PushFromConstraints}s)
 * to the {@link InputSource}s they come from. Resolution is language-agnostic,
 * and relies only on the location of the unknown values:
 * <ul>
 * <li>an unknown value located at a parameter of a cfg is the value of that
 * parameter (e.g., for entrypoints, or for cfgs analyzed without considering
 * their callers);</li>
 * <li>an unknown value located at a call is the value returned by the call: if
 * the call targets {@link NativeCFG}s (where frontends usually model library
 * methods that return unknown values), the source is described through the
 * signature of the native cfgs, otherwise (e.g., for calls that cannot be
 * resolved) through the name of the call's target; if the call might target a
 * cfg of the program (according to the call graph, or since a cfg has the name
 * of the call's target), its result is not a genuine input, but an imprecision
 * of the analysis ({@link InputSource.Kind#IMPRECISE_CALL_RESULT});</li>
 * <li>any other unknown value is described through the program point that
 * introduces it.</li>
 * </ul>
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class InputSourceResolver {

	private static final Comparator<NativeCFG> BY_SIGNATURE = Comparator
			.comparing(cfg -> cfg.getDescriptor().getFullSignature());

	private final Map<CodeLocation, Parameter> parameters;

	private final Map<CodeLocation, CFG> parameterOwners;

	private final Map<CodeLocation, Call> calls;

	private final Map<CodeLocation, Collection<NativeCFG>> natives;

	/**
	 * Builds the resolver for the program whose results are stored in the given
	 * tool.
	 *
	 * @param tool the tool containing the results of the analysis
	 */
	public InputSourceResolver(
			SemanticTool<?, ?> tool) {
		this(tool.getResults().keySet(), tool.getCallGraph());
	}

	/**
	 * Builds the resolver.
	 *
	 * @param cfgs      the cfgs of the program
	 * @param callgraph the call graph built during the analysis (can be
	 *                      {@code null}, in which case calls to native cfgs are
	 *                      not recognized)
	 */
	public InputSourceResolver(
			Collection<CFG> cfgs,
			CallGraph callgraph) {
		this.parameters = new HashMap<>();
		this.parameterOwners = new HashMap<>();
		this.calls = new HashMap<>();
		this.natives = new HashMap<>();

		GraphVisitor<CFG, Statement, Edge, Void> visitor = new GraphVisitor<>() {

			@Override
			public boolean visit(
					Void tool,
					CFG graph,
					Statement node) {
				if (node instanceof Call)
					calls.putIfAbsent(node.getLocation(), (Call) node);
				return true;
			}

		};

		for (CFG cfg : cfgs) {
			for (Parameter formal : cfg.getDescriptor().getFormals()) {
				parameters.putIfAbsent(formal.getLocation(), formal);
				parameterOwners.putIfAbsent(formal.getLocation(), cfg);
			}
			for (Statement node : cfg.getNodes())
				node.accept(visitor, null);
		}

		if (callgraph != null)
			for (CallGraphNode node : callgraph.getNodes()) {
				CodeMember cm = node.getCodeMember();
				if (cm instanceof NativeCFG)
					for (Call call : callgraph.getCallSites(cm))
						natives.computeIfAbsent(call.getLocation(), l -> new TreeSet<>(BY_SIGNATURE))
								.add((NativeCFG) cm);
			}
	}

	/**
	 * Resolves the given unknown value to its source.
	 *
	 * @param value the unknown value
	 * @param pp    the program point where the value is introduced (can be
	 *                  {@code null} if unknown)
	 *
	 * @return the source of the value
	 */
	public InputSource resolve(
			PushAny value,
			ProgramPoint pp) {
		CodeLocation location = value.getCodeLocation();
		Set<BinaryExpression> constraints = value instanceof PushFromConstraints
				? ((PushFromConstraints) value).getConstraints()
				: null;

		Parameter formal = parameters.get(location);
		if (formal != null) {
			String description = "parameter " + formal.getName() + " of "
					+ parameterOwners.get(location).getDescriptor().getFullSignatureWithParNames();
			if (!value.getStaticType().equals(formal.getStaticType()))
				// the unknown value is part of the parameter's value (e.g.,
				// the length of an array)
				description = "unknown value of type " + value.getStaticType() + " in " + description;
			return new InputSource(InputSource.Kind.PARAMETER, location, value.getStaticType(), constraints,
					description);
		}

		Collection<NativeCFG> targets = natives.get(location);
		if (targets != null) {
			String description = "return of " + targets.stream()
					.map(t -> t.getDescriptor().getFullSignatureWithParNames())
					.collect(Collectors.joining(" or "));
			// only natives modeling user inputs produce inputs
			boolean input = targets.stream().anyMatch(t -> UserInput.class.isAssignableFrom(t.getConstruct()));
			return new InputSource(input ? InputSource.Kind.CALL_RESULT : InputSource.Kind.IMPRECISE_CALL_RESULT,
					location, value.getStaticType(), constraints, description);
		}

		// results of other calls (e.g., open calls, whose result is decided
		// by the open call policy) are not user inputs
		Call call = calls.get(location);
		if (call != null)
			return new InputSource(InputSource.Kind.IMPRECISE_CALL_RESULT, location, value.getStaticType(),
					constraints, "return of " + call.getFullTargetName());
		if (pp instanceof OpenCall)
			// the open call policy produced the value, wherever it is located
			return new InputSource(InputSource.Kind.IMPRECISE_CALL_RESULT, location, value.getStaticType(),
					constraints, "return of " + ((OpenCall) pp).getFullTargetName());

		return new InputSource(InputSource.Kind.UNKNOWN, location, value.getStaticType(), constraints,
				"unknown value of type " + value.getStaticType()
						+ (pp == null ? "" : " produced by '" + pp + "'"));
	}
}
