package it.unive.lisa.program.cfg;

import static org.junit.Assert.fail;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticDomain;
import it.unive.lisa.analysis.SemanticDomain.Satisfiability;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SimpleAbstractState;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.combination.ValueCartesianProduct;
import it.unive.lisa.analysis.dataflow.AvailableExpressions;
import it.unive.lisa.analysis.dataflow.DefiniteForwardDataflowDomain;
import it.unive.lisa.analysis.dataflow.PossibleForwardDataflowDomain;
import it.unive.lisa.analysis.dataflow.ReachingDefinitions;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.heap.MonolithicHeap;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.nonInterference.NonInterference;
import it.unive.lisa.analysis.nonRedundantSet.ValueNonRedundantSet;
import it.unive.lisa.analysis.nonrelational.heap.HeapEnvironment;
import it.unive.lisa.analysis.nonrelational.heap.NonRelationalHeapDomain;
import it.unive.lisa.analysis.nonrelational.inference.InferenceSystem;
import it.unive.lisa.analysis.nonrelational.inference.InferredValue.InferredPair;
import it.unive.lisa.analysis.nonrelational.value.TypeEnvironment;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.numeric.Sign;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.analysis.representation.StringRepresentation;
import it.unive.lisa.analysis.symbols.SymbolAliasing;
import it.unive.lisa.analysis.types.InferredTypes;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.imp.IMPFeatures;
import it.unive.lisa.imp.types.IMPTypeSystem;
import it.unive.lisa.interprocedural.CFGResults;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.InterproceduralAnalysisException;
import it.unive.lisa.interprocedural.ModularWorstCaseAnalysis;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.interprocedural.UniqueScope;
import it.unive.lisa.interprocedural.WorstCasePolicy;
import it.unive.lisa.interprocedural.callgraph.CallGraph;
import it.unive.lisa.interprocedural.callgraph.CallGraphConstructionException;
import it.unive.lisa.interprocedural.callgraph.RTACallGraph;
import it.unive.lisa.program.Application;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.fixpoints.CFGFixpoint.CompoundState;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.cfg.statement.call.Call.CallType;
import it.unive.lisa.program.cfg.statement.call.MultiCall;
import it.unive.lisa.program.cfg.statement.call.TruncatedParamsCall;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.program.cfg.statement.evaluation.EvaluationOrder;
import it.unive.lisa.program.cfg.statement.evaluation.LeftToRightEvaluation;
import it.unive.lisa.program.language.resolution.ParameterMatchingStrategy;
import it.unive.lisa.program.language.resolution.StaticTypesMatchingStrategy;
import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.program.type.StringType;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.datastructures.graph.GraphVisitor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.Before;
import org.junit.Test;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

public class SemanticsSanityTest {

	private ClassUnit unit;
	private CFG cfg;
	private CallGraph cg;
	private InterproceduralAnalysis<
			SimpleAbstractState<MonolithicHeap, ValueEnvironment<Sign>, TypeEnvironment<InferredTypes>>,
			MonolithicHeap,
			ValueEnvironment<Sign>,
			TypeEnvironment<InferredTypes>> interprocedural;
	private AnalysisState<
			SimpleAbstractState<MonolithicHeap, ValueEnvironment<Sign>, TypeEnvironment<InferredTypes>>,
			MonolithicHeap,
			ValueEnvironment<Sign>,
			TypeEnvironment<InferredTypes>> as;
	private StatementStore<
			SimpleAbstractState<MonolithicHeap, ValueEnvironment<Sign>, TypeEnvironment<InferredTypes>>,
			MonolithicHeap,
			ValueEnvironment<Sign>,
			TypeEnvironment<InferredTypes>> store;
	private Expression fake;

	@Before
	public void setup() throws CallGraphConstructionException, InterproceduralAnalysisException {
		SourceCodeLocation unknownLocation = new SourceCodeLocation("unknown", 0, 0);

		Program p = new Program(new IMPFeatures(), new IMPTypeSystem());
		Application app = new Application(p);
		unit = new ClassUnit(unknownLocation, p, "foo", false);
		p.addUnit(unit);
		cfg = new CFG(new CodeMemberDescriptor(unknownLocation, unit, false, "foo"));
		cg = new RTACallGraph();
		cg.init(app);
		interprocedural = new ModularWorstCaseAnalysis<>();
		interprocedural.init(app, cg, WorstCasePolicy.INSTANCE);
		as = new AnalysisState<>(
				new SimpleAbstractState<>(new MonolithicHeap(), new ValueEnvironment<>(new Sign()),
						new TypeEnvironment<>(new InferredTypes())),
				new ExpressionSet<>(), new SymbolAliasing());
		store = new StatementStore<>(as);
		fake = new Expression(cfg, unknownLocation) {

			@Override
			public int setOffset(int offset) {
				return 0;
			}

			@Override
			public <V> boolean accept(GraphVisitor<CFG, Statement, Edge, V> visitor, V tool) {
				return false;
			}

			@Override
			public String toString() {
				return "fake";
			}

			@Override
			public <A extends AbstractState<A, H, V, T>,
					H extends HeapDomain<H>,
					V extends ValueDomain<V>,
					T extends TypeDomain<T>> AnalysisState<A, H, V, T> semantics(
							AnalysisState<A, H, V, T> entryState,
							InterproceduralAnalysis<A, H, V, T> interprocedural, StatementStore<A, H, V, T> expressions)
							throws SemanticException {
				return entryState
						.smallStepSemantics(
								new Variable(Untyped.INSTANCE, "fake",
										new SourceCodeLocation("unknown", 0, 0)),
								fake);
			}
		};
	}

	private Object valueFor(Class<?> param) {
		if (param == CFG.class)
			return cfg;
		if (param == String.class)
			return "foo";
		if (param == Expression.class)
			return fake;
		if (param == Global.class)
			return new Global(new SourceCodeLocation("unknown", 0, 0), unit, "foo", false);
		if (param == Object.class)
			return new Object();
		if (param == Type.class)
			return StringType.INSTANCE;
		if (param == Expression[].class)
			return new Expression[] { fake };
		if (param == Collection.class)
			return Collections.emptyList();
		if (param == ParameterMatchingStrategy.class)
			return StaticTypesMatchingStrategy.INSTANCE;
		if (param == Unit.class)
			return unit;
		if (param == CodeLocation.class)
			return new SourceCodeLocation("unknown", 0, 0);
		if (param == CallType.class)
			return CallType.STATIC;
		if (param == EvaluationOrder.class)
			return LeftToRightEvaluation.INSTANCE;
		if (param == UnresolvedCall.class || param == Call.class)
			return new UnresolvedCall(cfg, new SourceCodeLocation("unknown", 0, 0), CallType.STATIC, "fake", "fake");
		if (param == byte.class || param == Byte.class)
			return (byte) 0;
		if (param == short.class || param == Short.class)
			return (short) 0;
		if (param == int.class || param == Integer.class)
			return 0;
		if (param == float.class || param == Float.class)
			return -1f;
		if (param == boolean.class || param == Boolean.class)
			return false;
		if (param == long.class || param == Long.class)
			return 0L;
		if (param == double.class || param == Double.class)
			return 0.1;

		throw new UnsupportedOperationException("No default value for parameter of type " + param);
	}

	@Test
	public void testSemanticsOfStatements() {
		Map<Class<? extends Statement>, Map<String, Exception>> failures = new HashMap<>();
		Reflections scanner = new Reflections("it.unive.lisa", new SubTypesScanner());
		Set<Class<? extends Statement>> statements = scanner.getSubTypesOf(Statement.class);
		int total = 0;
		for (Class<? extends Statement> statement : statements)
			if (!Modifier.isAbstract(statement.getModifiers()) && !excluded(statement)) {
				total++;
				for (Constructor<?> c : statement.getConstructors())
					try {
						Class<?>[] types = c.getParameterTypes();
						Object[] params = new Object[types.length];
						for (int i = 0; i < params.length; i++)
							params[i] = valueFor(types[i]);
						Statement st = (Statement) c.newInstance(params);
						st.semantics(as, interprocedural, store);
					} catch (Exception e) {
						failures.computeIfAbsent(statement, s -> new HashMap<>())
								.put(Arrays.toString(c.getParameterTypes()), e);
					}
			}

		for (Entry<Class<? extends Statement>, Map<String, Exception>> entry : failures.entrySet())
			for (Entry<String, Exception> e : entry.getValue().entrySet()) {
				System.err.println(entry.getKey() + " failed for " + e.getKey() + " due to " + e.getValue());
				e.getValue().printStackTrace(System.err);
			}

		if (!failures.isEmpty())
			fail(failures.size() + "/" + total + " semantics evaluation failed");
	}

	private boolean excluded(Class<? extends Statement> statement) {
		// these just forward the semantics to the inner call
		return statement == MultiCall.class
				|| statement == TruncatedParamsCall.class;
	}

	private static class NRHeap implements NonRelationalHeapDomain<NRHeap> {

		private static final NRHeap BOTTOM = new NRHeap();
		private static final NRHeap TOP = new NRHeap();

		@Override
		public NRHeap eval(SymbolicExpression expression, HeapEnvironment<NRHeap> environment, ProgramPoint pp)
				throws SemanticException {
			return top();
		}

		@Override
		public Satisfiability satisfies(SymbolicExpression expression, HeapEnvironment<NRHeap> environment,
				ProgramPoint pp) throws SemanticException {
			return Satisfiability.UNKNOWN;
		}

		@Override
		public HeapEnvironment<NRHeap> assume(HeapEnvironment<NRHeap> environment, SymbolicExpression expression,
				ProgramPoint pp) throws SemanticException {
			return environment;
		}

		@Override
		public NRHeap glb(NRHeap other) throws SemanticException {
			return bottom();
		}

		@Override
		public DomainRepresentation representation() {
			return new StringRepresentation("");
		}

		@Override
		public NRHeap lub(NRHeap other) throws SemanticException {
			return top();
		}

		@Override
		public boolean lessOrEqual(NRHeap other) throws SemanticException {
			return false;
		}

		@Override
		public NRHeap top() {
			return TOP;
		}

		@Override
		public NRHeap bottom() {
			return BOTTOM;
		}

		@Override
		public boolean tracksIdentifiers(Identifier id) {
			return true;
		}

		@Override
		public boolean canProcess(SymbolicExpression expression) {
			return true;
		}

		@Override
		public List<HeapReplacement> getSubstitution() {
			return Collections.emptyList();
		}

		@Override
		public ExpressionSet<ValueExpression> rewrite(SymbolicExpression expression,
				HeapEnvironment<NRHeap> environment, ProgramPoint pp) throws SemanticException {
			return new ExpressionSet<>();
		}

	}

	private Object domainFor(Class<?> root, Class<?> param) {
		if (root == ValueEnvironment.class)
			return new Sign();
		if (root == HeapEnvironment.class)
			return new NRHeap();
		if (root == TypeEnvironment.class)
			return new InferredTypes();
		if (root == InferenceSystem.class)
			return new NonInterference();
		if (root == PossibleForwardDataflowDomain.class)
			return new ReachingDefinitions();
		if (root == DefiniteForwardDataflowDomain.class)
			return new AvailableExpressions();
		if (root == AnalysisState.class)
			if (param == AbstractState.class)
				return new SimpleAbstractState<>(new MonolithicHeap(), new ValueEnvironment<>(new Sign()),
						new TypeEnvironment<>(new InferredTypes()));
			else if (param == SymbolicExpression.class)
				return new Skip(new SourceCodeLocation("unknown", 0, 0));
			else if (param == ExpressionSet.class)
				return new ExpressionSet<>();
			else if (param == SymbolAliasing.class)
				return new SymbolAliasing();
		if (root == SimpleAbstractState.class)
			if (param == HeapDomain.class)
				return new MonolithicHeap();
			else if (param == ValueDomain.class)
				return new ValueEnvironment<>(new Sign());
			else if (param == TypeDomain.class)
				return new TypeEnvironment<>(new InferredTypes());
		if (root == ValueCartesianProduct.class)
			return new ValueEnvironment<>(new Sign());
		if (root == ValueNonRedundantSet.class)
			if (param == SortedSet.class)
				return new TreeSet<ValueEnvironment<Sign>>();
			else if (param == boolean.class)
				return true;
			else if (param == ValueDomain.class)
				return new ValueEnvironment<>(new Sign());
		if (root == StatementStore.class)
			return as;
		if (root == InferredPair.class)
			return new NonInterference();
		if (param == CFG.class)
			return cfg;
		if (param == AnalysisState.class)
			return as;
		if (param == AnalyzedCFG.class)
			return new AnalyzedCFG<>(cfg, new UniqueScope(), as);
		if (param == CFGResults.class)
			return new CFGResults<>(new AnalyzedCFG<>(cfg, new UniqueScope(), as));
		if (param == InterproceduralAnalysis.class)
			return interprocedural;
		if (param == ScopeId.class)
			return new UniqueScope();

		throw new UnsupportedOperationException(
				"No default domain for domain " + root + " and parameter of type " + param);
	}

	@SuppressWarnings("unchecked")
	private <T> int buildDomainsInstances(Set<Class<? extends T>> classes, Set<T> instances, List<String> failures) {
		int total = 0;
		Constructor<?> nullary, unary, binary, ternary, quaternary;
		nullary = unary = binary = ternary = quaternary = null;
		T instance;
		for (Class<? extends T> clazz : classes)
			if (!Modifier.isAbstract(clazz.getModifiers()) && !Modifier.isInterface(clazz.getModifiers())
					&& !Satisfiability.class.isAssignableFrom(clazz)
					&& !CompoundState.class.isAssignableFrom(clazz)
					// some testing domain that we do not care about end up here
					&& !clazz.getName().contains("Test")) {
				total++;
				for (Constructor<?> c : clazz.getConstructors()) {
					if (c.getParameterCount() == 0)
						nullary = c;
					else if (c.getParameterCount() == 1)
						unary = c;
					else if (c.getParameterCount() == 2)
						binary = c;
					else if (c.getParameterCount() == 3)
						ternary = c;
					else if (c.getParameterCount() == 4)
						quaternary = c;
				}

				try {
					if (nullary != null)
						instance = (T) nullary.newInstance();
					else if (unary != null) {
						Class<?>[] types = unary.getParameterTypes();
						Object param = domainFor(clazz, types[0]);
						instance = (T) unary.newInstance(param);
					} else if (binary != null) {
						Class<?>[] types = binary.getParameterTypes();
						Object param1 = domainFor(clazz, types[0]);
						Object param2 = domainFor(clazz, types[1]);
						instance = (T) binary.newInstance(param1, param2);
					} else if (ternary != null) {
						Class<?>[] types = ternary.getParameterTypes();
						Object param1 = domainFor(clazz, types[0]);
						Object param2 = domainFor(clazz, types[1]);
						Object param3 = domainFor(clazz, types[2]);
						instance = (T) ternary.newInstance(param1, param2, param3);
					} else if (quaternary != null) {
						Class<?>[] types = quaternary.getParameterTypes();
						Object param1 = domainFor(clazz, types[0]);
						Object param2 = domainFor(clazz, types[1]);
						Object param3 = domainFor(clazz, types[2]);
						Object param4 = domainFor(clazz, types[3]);
						instance = (T) quaternary.newInstance(param1, param2, param3, param4);
					} else {
						failures.add(clazz.getName());
						System.err.println("No suitable consturctor found for " + clazz.getName());
						continue;
					}

					instances.add(instance);

					nullary = unary = binary = ternary = null;
				} catch (Exception e) {
					failures.add(clazz.getName());
					System.err.println("Instantiation of class " + clazz.getName() + " failed due to " + e);
					e.printStackTrace(System.err);
				}
			}

		return total;
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void testAssignOnBottom() {
		List<String> failures = new ArrayList<>();

		BoolType bool = BoolType.INSTANCE;

		Reflections scanner = new Reflections("it.unive.lisa", new SubTypesScanner());
		Set<Class<? extends SemanticDomain>> domains = scanner.getSubTypesOf(SemanticDomain.class);
		Set<SemanticDomain> instances = new HashSet<>();
		int total = buildDomainsInstances(domains, instances, failures);

		for (SemanticDomain instance : instances)
			try {
				instance = (SemanticDomain) ((Lattice) instance).bottom();
				Variable v = new Variable(bool, "b", new SourceCodeLocation("unknown", 0, 0));
				instance = instance.assign(v, new PushAny(bool, new SourceCodeLocation("unknown", 0, 0)), fake);
				boolean isBottom = ((Lattice) instance).isBottom();
				if (instance instanceof AnalysisState) {
					AnalysisState state = (AnalysisState) instance;
					isBottom = state.getState().isBottom() && state.getAliasing().isBottom()
					// analysis state keeps the assigned id on the stack
							&& state.getComputedExpressions().size() == 1
							&& state.getComputedExpressions().iterator().next().equals(v);
				}

				if (!isBottom) {
					failures.add(instance.getClass().getName());
					System.err.println("Assigning to the bottom instance of " + instance.getClass().getName()
							+ " returned a non-bottom instance");
				}
			} catch (Exception e) {
				failures.add(instance.getClass().getName());
				System.err.println("assignOnBottom: " + instance.getClass().getName() + " failed due to " + e);
				e.printStackTrace(System.err);
			}

		if (!failures.isEmpty())
			fail(failures.size() + "/" + total + " assignments failed");
	}

	@Test
	@SuppressWarnings({ "rawtypes" })
	public void testIsTopIsBottom() {
		List<String> failures = new ArrayList<>();

		Reflections scanner = new Reflections("it.unive.lisa", new SubTypesScanner());
		Set<Class<? extends Lattice>> domains = scanner.getSubTypesOf(Lattice.class);

		Set<Lattice> instances = new HashSet<>();
		int total = buildDomainsInstances(domains, instances, failures);

		for (Lattice instance : instances)
			try {
				if (!instance.bottom().isBottom()) {
					failures.add(instance.getClass().getName());
					System.err.println("bottom().isBottom() returned false on " + instance.getClass().getName());
				}

				if (!instance.top().isTop()) {
					failures.add(instance.getClass().getName());
					System.err.println("top().isTop() returned false on " + instance.getClass().getName());
				}
			} catch (Exception e) {
				failures.add(instance.getClass().getName());
				System.err.println("isTopIsBottom: " + instance.getClass().getName() + " failed due to " + e);
				e.printStackTrace(System.err);
			}

		if (!failures.isEmpty())
			fail(failures.size() + "/" + total + " tests failed");
	}
}
