package it.unive.lisa.program.cfg;

import static org.junit.Assert.fail;

import it.unive.lisa.LiSA;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SimpleAbstractState;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.impl.heap.MonolithicHeap;
import it.unive.lisa.analysis.impl.numeric.Sign;
import it.unive.lisa.analysis.lattices.ExpressionSet;
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
import it.unive.lisa.symbolic.types.StringType;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.datastructures.graph.GraphVisitor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

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
		SourceCodeLocation unknownLocation = new SourceCodeLocation(null, -1, -1);
		Program p = new Program(unknownLocation);
		unit = new CompilationUnit(unknownLocation, "foo", false);
		p.addCompilationUnit(unit);
		cfg = new CFG(new CFGDescriptor(unknownLocation, unit, false, "foo"));
		cg = new IntraproceduralCallGraph();
		cg.build(p);
		as = new AnalysisState<>(new SimpleAbstractState<>(new MonolithicHeap(), new ValueEnvironment<>(new Sign())),
				new ExpressionSet<>());
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
			return new Global(new SourceCodeLocation(null, -1, -1), "foo");
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
			return new SourceCodeLocation(null, -1, -1);

		throw new UnsupportedOperationException("No default value for parameter of type " + param);
	}

	@Test
	public void testSemanticsOfStatements() {
		Map<Class<? extends Statement>, Map<String, Exception>> failures = new HashMap<>();
		Reflections scanner = new Reflections(LiSA.class, IMPFrontend.class, new SubTypesScanner());
		Set<Class<? extends Statement>> statements = scanner.getSubTypesOf(Statement.class);
		for (Class<? extends Statement> statement : statements)
			if (!Modifier.isAbstract(statement.getModifiers()))
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

		for (Entry<Class<? extends Statement>, Map<String, Exception>> entry : failures.entrySet())
			for (Entry<String, Exception> e : entry.getValue().entrySet()) {
				System.err.println(entry.getKey() + " failed for " + e.getKey() + " due to " + e.getValue());
				e.getValue().printStackTrace(System.err);
			}

		if (!failures.isEmpty())
			fail(failures.size() + "/" + statements.size() + " semantics evaluation failed");
	}
}
