package it.unive.lisa.program.cfg;

import static org.junit.Assert.fail;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import it.unive.lisa.LiSA;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticDomain.Satisfiability;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SimpleAbstractState;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.combination.ValueCartesianProduct;
import it.unive.lisa.analysis.dataflow.DefiniteForwardDataflowDomain;
import it.unive.lisa.analysis.dataflow.PossibleForwardDataflowDomain;
import it.unive.lisa.analysis.dataflow.impl.AvailableExpressions;
import it.unive.lisa.analysis.dataflow.impl.ReachingDefinitions;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.impl.heap.MonolithicHeap;
import it.unive.lisa.analysis.impl.numeric.Sign;
import it.unive.lisa.analysis.impl.types.InferredTypes;
import it.unive.lisa.analysis.inference.InferenceSystem;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.nonrelational.heap.HeapEnvironment;
import it.unive.lisa.analysis.nonrelational.heap.NonRelationalHeapDomain;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.callgraph.CallGraph;
import it.unive.lisa.callgraph.CallGraphConstructionException;
import it.unive.lisa.callgraph.impl.intraproc.IntraproceduralCallGraph;
import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.UnresolvedCall.ResolutionStrategy;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.types.StringType;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.datastructures.graph.GraphVisitor;

public class SemanticsSanityTest {

	private CompilationUnit unit;
	private CFG cfg;
	private CallGraph cg;
	private AnalysisState<SimpleAbstractState<MonolithicHeap, ValueEnvironment<Sign>>, MonolithicHeap,
			ValueEnvironment<Sign>> as;
	private StatementStore<SimpleAbstractState<MonolithicHeap, ValueEnvironment<Sign>>, MonolithicHeap,
			ValueEnvironment<Sign>> store;
	private Expression fake;

	@Before
	public void setup() throws CallGraphConstructionException {
		Program p = new Program();
		unit = new CompilationUnit(null, "foo", false);
		p.addCompilationUnit(unit);
		cfg = new CFG(new CFGDescriptor(unit, false, "foo"));
		cg = new IntraproceduralCallGraph();
		cg.build(p);
		as = new AnalysisState<>(new SimpleAbstractState<>(new MonolithicHeap(), new ValueEnvironment<>(new Sign())),
				new ExpressionSet<>());
		store = new StatementStore<>(as);
		fake = new Expression(cfg, null) {

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
			public <A extends AbstractState<A, H, V>,
					H extends HeapDomain<H>,
					V extends ValueDomain<V>> AnalysisState<A, H, V> semantics(AnalysisState<A, H, V> entryState,
							CallGraph callGraph, StatementStore<A, H, V> expressions) throws SemanticException {
				return entryState;
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
		if (param == int.class || param == Integer.class)
			return -1;
		if (param == float.class || param == Float.class)
			return -1f;
		if (param == boolean.class || param == Boolean.class)
			return false;
		if (param == Global.class)
			return new Global("foo");
		if (param == Object.class)
			return new Object();
		if (param == Type.class)
			return StringType.INSTANCE;
		if (param == Expression[].class)
			return new Expression[] { fake };
		if (param == Collection.class)
			return Collections.emptyList();
		if (param == ResolutionStrategy.class)
			return ResolutionStrategy.STATIC_TYPES;
		if (param == Unit.class)
			return unit;
		if (param == CodeLocation.class)
			return new SourceCodeLocation(null, 0, 0);

		throw new UnsupportedOperationException("No default value for parameter of type " + param);
	}

	@Test
	public void testSemanticsOfStatements() {
		Map<Class<? extends Statement>, Map<String, Exception>> failures = new HashMap<>();
		Reflections scanner = new Reflections(LiSA.class, IMPFrontend.class, new SubTypesScanner());
		Set<Class<? extends Statement>> statements = scanner.getSubTypesOf(Statement.class);
		int total = 0;
		for (Class<? extends Statement> statement : statements)
			if (!Modifier.isAbstract(statement.getModifiers())) {
				total++;
				for (Constructor<?> c : statement.getConstructors())
					try {
						Class<?>[] types = c.getParameterTypes();
						Object[] params = new Object[types.length];
						for (int i = 0; i < params.length; i++)
							params[i] = valueFor(types[i]);
						Statement st = (Statement) c.newInstance(params);
						st.semantics(as, cg, store);
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
		public String representation() {
			return "";
		}

		@Override
		public NRHeap lub(NRHeap other) throws SemanticException {
			return top();
		}

		@Override
		public NRHeap widening(NRHeap other) throws SemanticException {
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
		public ExpressionSet<ValueExpression> getRewrittenExpressions() {
			return new ExpressionSet<>();
		}

		@Override
		public List<HeapReplacement> getSubstitution() {
			return Collections.emptyList();
		}

	}

	private Object domainFor(Class<?> root, Class<?> param) {
		if (root == ValueEnvironment.class)
			return new Sign();
		if (root == HeapEnvironment.class)
			return new NRHeap();
		if (root == InferenceSystem.class)
			return new InferredTypes();
		if (root == PossibleForwardDataflowDomain.class)
			return new ReachingDefinitions();
		if (root == DefiniteForwardDataflowDomain.class)
			return new AvailableExpressions();
		if (root == AnalysisState.class)
			if (param == AbstractState.class)
				return new SimpleAbstractState<>(new MonolithicHeap(), new ValueEnvironment<>(new Sign()));
			else if (param == SymbolicExpression.class)
				return new Skip();
			else if (param == ExpressionSet.class)
				return new ExpressionSet<>();
		if (root == SimpleAbstractState.class)
			if (param == HeapDomain.class)
				return new MonolithicHeap();
			else if (param == ValueDomain.class)
				return new ValueEnvironment<>(new Sign());
		if (root == ValueCartesianProduct.class)
			return new ValueEnvironment<>(new Sign());
		if (root == StatementStore.class)
			return new AnalysisState<>(
					new SimpleAbstractState<>(new MonolithicHeap(), new ValueEnvironment<>(new Sign())), new Skip());

		throw new UnsupportedOperationException("No default domain for parameter of type " + param);
	}

	@Test
	@SuppressWarnings({ "rawtypes" })
	public void testIsTopIsBottom() {
		Set<String> failures = new HashSet<>();

		Reflections scanner = new Reflections(LiSA.class, new SubTypesScanner());
		Set<Class<? extends Lattice>> domains = scanner.getSubTypesOf(Lattice.class);
		Constructor<?> nullary = null, unary = null, binary = null;
		Lattice instance;
		int total = 0;
		for (Class<? extends Lattice> domain : domains)
			if (!Modifier.isAbstract(domain.getModifiers()) && !Modifier.isInterface(domain.getModifiers())
					&& !Satisfiability.class.isAssignableFrom(domain)) {
				total++;
				for (Constructor<?> c : domain.getConstructors()) {
					if (c.getParameterCount() == 0)
						nullary = c;
					else if (c.getParameterCount() == 1)
						unary = c;
					else if (c.getParameterCount() == 2)
						binary = c;
				}

				try {
					if (nullary != null)
						instance = (Lattice) nullary.newInstance();
					else if (unary != null) {
						Class<?>[] types = unary.getParameterTypes();
						Object param = domainFor(domain, types[0]);
						instance = (Lattice) unary.newInstance(param);
					} else if (binary != null) {
						Class<?>[] types = binary.getParameterTypes();
						Object param1 = domainFor(domain, types[0]);
						Object param2 = domainFor(domain, types[1]);
						instance = (Lattice) binary.newInstance(param1, param2);
					} else {
						failures.add(domain.getName());
						System.err.println("No suitable consturctor found for " + domain.getName());
						continue;
					}

					if (!instance.bottom().isBottom()) {
						failures.add(domain.getName());
						System.err.println("bottom().isBottom() returned false on " + domain.getName());
					}

					if (!instance.top().isTop()) {
						failures.add(domain.getName());
						System.err.println("top().isTop() returned false on " + domain.getName());
					}

					nullary = null;
					unary = null;
					binary = null;
				} catch (Exception e) {
					failures.add(domain.getName());
					System.err.println(domain.getName() + " failed due to " + e);
					e.printStackTrace(System.err);
				}
			}

		if (!failures.isEmpty())
			fail(failures.size() + "/" + total + " tests failed");
	}
}
