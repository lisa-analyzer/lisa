package it.unive.lisa;

import static org.junit.Assert.fail;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.SimpleAbstractDomain;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.combination.constraints.WholeValueElement;
import it.unive.lisa.analysis.combination.constraints.WholeValueStringDomain;
import it.unive.lisa.analysis.combination.smash.SmashedSumIntDomain;
import it.unive.lisa.analysis.combination.smash.SmashedSumStringDomain;
import it.unive.lisa.analysis.heap.HeapLattice;
import it.unive.lisa.analysis.heap.MonolithicHeap;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.lattices.SingleHeapLattice;
import it.unive.lisa.analysis.lattices.SingleTypeLattice;
import it.unive.lisa.analysis.lattices.SingleValueLattice;
import it.unive.lisa.analysis.nonRedundantPowerset.NonRedundantSetDomainLattice;
import it.unive.lisa.analysis.nonRedundantPowerset.NonRedundantSetLattice;
import it.unive.lisa.analysis.nonrelational.heap.HeapEnvironment;
import it.unive.lisa.analysis.nonrelational.heap.NonRelationalHeapDomain;
import it.unive.lisa.analysis.nonrelational.type.BaseNonRelationalTypeDomain;
import it.unive.lisa.analysis.nonrelational.type.TypeEnvironment;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.numeric.Interval;
import it.unive.lisa.analysis.numeric.Sign;
import it.unive.lisa.analysis.string.BoundedStringSet;
import it.unive.lisa.analysis.string.Bricks;
import it.unive.lisa.analysis.string.Prefix;
import it.unive.lisa.analysis.traces.TracePartitioning;
import it.unive.lisa.analysis.type.TypeLattice;
import it.unive.lisa.analysis.types.InferredTypes;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.analysis.value.ValueLattice;
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
import it.unive.lisa.lattices.SimpleAbstractState;
import it.unive.lisa.lattices.heap.Monolith;
import it.unive.lisa.lattices.informationFlow.NonInterferenceValue;
import it.unive.lisa.lattices.informationFlow.SimpleTaint;
import it.unive.lisa.lattices.numeric.NonRedundantIntervalSet;
import it.unive.lisa.lattices.numeric.SignLattice;
import it.unive.lisa.lattices.symbolic.DefiniteIdSet;
import it.unive.lisa.lattices.types.TypeSet;
import it.unive.lisa.program.Application;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.cfg.statement.call.Call.CallType;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.program.cfg.statement.evaluation.EvaluationOrder;
import it.unive.lisa.program.cfg.statement.evaluation.LeftToRightEvaluation;
import it.unive.lisa.program.cfg.statement.literal.TypeLiteral;
import it.unive.lisa.program.language.resolution.ParameterMatchingStrategy;
import it.unive.lisa.program.language.resolution.StaticTypesMatchingStrategy;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.program.type.StringType;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.PushInv;
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.ternary.StringReplace;
import it.unive.lisa.symbolic.value.operator.ternary.TernaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.datastructures.graph.GraphVisitor;
import it.unive.lisa.util.numeric.IntInterval;
import java.lang.reflect.Executable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.tuple.Pair;

public class TestParameterProvider {

	private TestParameterProvider() {
	}

	public static class SampleNRVD implements BaseNonRelationalValueDomain<SingleValueLattice> {

		@Override
		public SingleValueLattice top() {
			return SingleValueLattice.SINGLETON;
		}

		@Override
		public SingleValueLattice bottom() {
			return SingleValueLattice.BOTTOM;
		}

	}

	public static class SampleNRTD implements BaseNonRelationalTypeDomain<SingleTypeLattice> {

		@Override
		public SingleTypeLattice top() {
			return SingleTypeLattice.SINGLETON;
		}

		@Override
		public SingleTypeLattice bottom() {
			return SingleTypeLattice.BOTTOM;
		}

	}

	public static class SampleNRHD implements NonRelationalHeapDomain<SingleHeapLattice> {

		@Override
		public ExpressionSet rewrite(
				HeapEnvironment<SingleHeapLattice> state,
				SymbolicExpression expression,
				ProgramPoint pp,
				SemanticOracle oracle)
				throws SemanticException {
			return new ExpressionSet(expression);
		}

		@Override
		public Satisfiability alias(
				HeapEnvironment<SingleHeapLattice> state,
				SymbolicExpression x,
				SymbolicExpression y,
				ProgramPoint pp,
				SemanticOracle oracle)
				throws SemanticException {
			return Satisfiability.UNKNOWN;
		}

		@Override
		public Satisfiability isReachableFrom(
				HeapEnvironment<SingleHeapLattice> state,
				SymbolicExpression x,
				SymbolicExpression y,
				ProgramPoint pp,
				SemanticOracle oracle)
				throws SemanticException {
			return Satisfiability.UNKNOWN;
		}

		@Override
		public HeapEnvironment<SingleHeapLattice> makeLattice() {
			return new HeapEnvironment<>(SingleHeapLattice.SINGLETON);
		}

		@Override
		public Pair<HeapEnvironment<SingleHeapLattice>, List<HeapReplacement>> assign(
				HeapEnvironment<SingleHeapLattice> state,
				Identifier id,
				SymbolicExpression expression,
				ProgramPoint pp,
				SemanticOracle oracle)
				throws SemanticException {
			return Pair.of(state, Collections.emptyList());
		}

		@Override
		public Pair<HeapEnvironment<SingleHeapLattice>, List<HeapReplacement>> smallStepSemantics(
				HeapEnvironment<SingleHeapLattice> state,
				SymbolicExpression expression,
				ProgramPoint pp,
				SemanticOracle oracle)
				throws SemanticException {
			return Pair.of(state, Collections.emptyList());
		}

		@Override
		public Pair<HeapEnvironment<SingleHeapLattice>, List<HeapReplacement>> assume(
				HeapEnvironment<SingleHeapLattice> state,
				SymbolicExpression expression,
				ProgramPoint src,
				ProgramPoint dest,
				SemanticOracle oracle)
				throws SemanticException {
			return Pair.of(state, Collections.emptyList());
		}

		@Override
		public SingleHeapLattice eval(
				HeapEnvironment<SingleHeapLattice> environment,
				SymbolicExpression expression,
				ProgramPoint pp,
				SemanticOracle oracle)
				throws SemanticException {
			return SingleHeapLattice.SINGLETON;
		}

		@Override
		public SingleHeapLattice fixedVariable(
				Identifier id,
				ProgramPoint pp,
				SemanticOracle oracle)
				throws SemanticException {
			return SingleHeapLattice.SINGLETON;
		}

		@Override
		public SingleHeapLattice unknownValue(
				Identifier id) {
			return SingleHeapLattice.SINGLETON;
		}

		@Override
		public boolean canProcess(
				SymbolicExpression expression,
				ProgramPoint pp,
				SemanticOracle oracle) {
			return true;
		}

	}

	public static class FakePP implements ProgramPoint {

		@Override
		public CodeLocation getLocation() {
			return SyntheticLocation.INSTANCE;
		}

		@Override
		public CFG getCFG() {
			return cfg;
		}

		@Override
		public Program getProgram() {
			return new Program(new IMPFeatures(), new IMPTypeSystem());
		}

	}

	public static class FakeOracle implements SemanticOracle {

		@Override
		public Set<Type> getRuntimeTypesOf(
				SymbolicExpression e,
				ProgramPoint pp)
				throws SemanticException {
			return Collections.singleton(e.getStaticType());
		}

		@Override
		public Type getDynamicTypeOf(
				SymbolicExpression e,
				ProgramPoint pp)
				throws SemanticException {
			return e.getStaticType();
		}

		@Override
		public ExpressionSet rewrite(
				SymbolicExpression expression,
				ProgramPoint pp)
				throws SemanticException {
			return new ExpressionSet(expression);
		}

		@Override
		public ExpressionSet rewrite(
				ExpressionSet expressions,
				ProgramPoint pp)
				throws SemanticException {
			return expressions;
		}

		@Override
		public Satisfiability alias(
				SymbolicExpression x,
				SymbolicExpression y,
				ProgramPoint pp)
				throws SemanticException {
			return Satisfiability.UNKNOWN;
		}

		@Override
		public ExpressionSet reachableFrom(
				SymbolicExpression e,
				ProgramPoint pp)
				throws SemanticException {
			return new ExpressionSet(e);
		}

		@Override
		public Satisfiability isReachableFrom(
				SymbolicExpression x,
				SymbolicExpression y,
				ProgramPoint pp)
				throws SemanticException {
			return Satisfiability.UNKNOWN;
		}

		@Override
		public Satisfiability areMutuallyReachable(
				SymbolicExpression x,
				SymbolicExpression y,
				ProgramPoint pp)
				throws SemanticException {
			return Satisfiability.UNKNOWN;
		}

	};

	public static class SimpleNRSDL extends NonRedundantSetDomainLattice<SimpleNRSDL, ValueEnvironment<IntInterval>>
			implements
			ValueLattice<SimpleNRSDL> {

		protected SimpleNRSDL() {
			super(Collections.emptySet(), new ValueEnvironment<>(IntInterval.TOP));
		}

		protected SimpleNRSDL(
				Set<ValueEnvironment<IntInterval>> elements) {
			super(elements, new ValueEnvironment<>(IntInterval.TOP));
		}

		@Override
		public SimpleNRSDL store(
				Identifier target,
				Identifier source)
				throws SemanticException {
			Set<ValueEnvironment<IntInterval>> newEnvs = new HashSet<>();
			for (ValueEnvironment<IntInterval> env : elements)
				newEnvs.add(env.store(target, source));
			return mk(newEnvs);
		}

		@Override
		public boolean lessOrEqual(
				SimpleNRSDL other)
				throws SemanticException {
			outer: for (ValueEnvironment<IntInterval> env : elements)
				for (ValueEnvironment<IntInterval> otherEnv : other.elements) {
					if (env.lessOrEqual(otherEnv))
						continue outer;
					return false;
				}
			return true;
		}

		@Override
		public SimpleNRSDL lub(
				SimpleNRSDL other)
				throws SemanticException {
			return new SimpleNRSDL(SetUtils.union(elements, other.elements));
		}

		@Override
		public SimpleNRSDL mk(
				Set<ValueEnvironment<IntInterval>> set) {
			return new SimpleNRSDL(set);
		}

	}

	public static final ClassUnit unit;

	public static final CFG cfg;

	public static final CallGraph cg;

	public static final InterproceduralAnalysis<
			SimpleAbstractState<Monolith, ValueEnvironment<SignLattice>, TypeEnvironment<TypeSet>>,
			SimpleAbstractDomain<Monolith, ValueEnvironment<SignLattice>, TypeEnvironment<TypeSet>>> interprocedural;

	public static final AnalysisState<
			SimpleAbstractState<Monolith, ValueEnvironment<SignLattice>, TypeEnvironment<TypeSet>>> as;

	public static final StatementStore<
			SimpleAbstractState<Monolith, ValueEnvironment<SignLattice>, TypeEnvironment<TypeSet>>> store;

	public static final Expression expr;

	static {
		SourceCodeLocation unknownLocation = new SourceCodeLocation("unknown", 0, 0);

		Program p = new Program(new IMPFeatures(), new IMPTypeSystem());
		Application app = new Application(p);
		unit = new ClassUnit(unknownLocation, p, "foo", false);
		p.addUnit(unit);
		cfg = new CFG(new CodeMemberDescriptor(unknownLocation, unit, false, "foo"));
		cg = new RTACallGraph();
		interprocedural = new ModularWorstCaseAnalysis<>();
		try {
			cg.init(app);
			interprocedural.init(
				app,
				cg,
				WorstCasePolicy.INSTANCE,
				new Analysis<>(new SimpleAbstractDomain<>(new MonolithicHeap(), new Sign(), new InferredTypes())));
		} catch (CallGraphConstructionException | InterproceduralAnalysisException e) {
			fail("Unable to instantiate test parameters: " + e.getMessage());
		}

		as = new AnalysisState<>(
			new SimpleAbstractState<>(
				Monolith.SINGLETON,
				new ValueEnvironment<>(new SignLattice()),
				new TypeEnvironment<>(new TypeSet())),
			new ExpressionSet());
		store = new StatementStore<>(as);
		expr = new Expression(cfg, unknownLocation) {

			@Override
			public <V> boolean accept(
					GraphVisitor<CFG, Statement, Edge, V> visitor,
					V tool) {
				return false;
			}

			@Override
			public String toString() {
				return "fake";
			}

			@Override
			protected int compareSameClass(
					Statement o) {
				return 0;
			}

			@Override
			public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> forwardSemantics(
					AnalysisState<A> entryState,
					InterproceduralAnalysis<A, D> interprocedural,
					StatementStore<A> expressions)
					throws SemanticException {
				return interprocedural.getAnalysis()
					.smallStepSemantics(
						entryState,
						new Variable(Untyped.INSTANCE, "fake", new SourceCodeLocation("unknown", 0, 0)),
						expr);
			}

		};
	}

	public static Object[] provideParams(
			Executable mtd,
			Class<?>[] params,
			Class<?> envClass,
			AtomicReference<Integer> envPos) {
		Object[] res = new Object[params.length];
		for (int i = 0; i < res.length; i++) {
			res[i] = provideParam(mtd, params[i]);
			if (params[i] == envClass)
				envPos.set(i);
		}
		return res;
	}

	@SuppressWarnings("unchecked")
	public static <R> R provideParam(
			Executable mtd,
			Class<R> param) {
		// java types
		if (param == Object.class)
			return (R) new Object();
		if (param == String.class)
			return (R) "foo";
		if (param == Collection.class)
			return (R) Collections.emptyList();
		if (param == byte.class || param == Byte.class)
			return (R) Byte.valueOf((byte) 0);
		if (param == short.class || param == Short.class)
			return (R) Short.valueOf((short) 0);
		if (param == int.class || param == Integer.class)
			return (R) Integer.valueOf(0);
		if (param == float.class || param == Float.class)
			return (R) Float.valueOf(1f);
		if (param == boolean.class || param == Boolean.class)
			return (R) Boolean.FALSE;
		if (param == long.class || param == Long.class)
			return (R) Long.valueOf(0L);
		if (param == double.class || param == Double.class)
			return (R) Double.valueOf(0.1);

		// program structure
		if (param == Unit.class)
			return (R) unit;
		if (param == CodeLocation.class)
			return (R) SyntheticLocation.INSTANCE;
		if (param == Global.class)
			return (R) new Global(SyntheticLocation.INSTANCE, unit, "foo", false);
		if (param == CFG.class)
			return (R) cfg;

		// expressions and statements
		if (param == Expression.class)
			return (R) expr;
		if (param == Expression[].class)
			return (R) new Expression[] { expr };
		if (param == UnresolvedCall.class || param == Call.class)
			return (R) new UnresolvedCall(cfg, SyntheticLocation.INSTANCE, CallType.STATIC, "fake", "fake");
		if (param == TypeLiteral.class)
			return (R) new TypeLiteral(cfg, SyntheticLocation.INSTANCE, StringType.INSTANCE);

		// features
		if (param == ParameterMatchingStrategy.class)
			return (R) StaticTypesMatchingStrategy.INSTANCE;
		if (param == CallType.class)
			return (R) CallType.STATIC;
		if (param == EvaluationOrder.class)
			return (R) LeftToRightEvaluation.INSTANCE;

		// types
		if (param == Type.class)
			return (R) StringType.INSTANCE;

		// symbolic expressions
		if (param == PushAny.class || param == SymbolicExpression.class)
			return (R) new PushAny(Untyped.INSTANCE, SyntheticLocation.INSTANCE);
		if (param == PushInv.class)
			return (R) new PushInv(Untyped.INSTANCE, SyntheticLocation.INSTANCE);
		if (param == Constant.class || param == ValueExpression.class)
			return (R) new Constant(Int32Type.INSTANCE, 5, SyntheticLocation.INSTANCE);
		if (param == Identifier.class)
			return (R) new Variable(provideParam(mtd, Type.class), "foo", SyntheticLocation.INSTANCE);
		if (param == Skip.class)
			return (R) new Skip(SyntheticLocation.INSTANCE);
		if (param == UnaryExpression.class)
			return (R) new UnaryExpression(
				provideParam(mtd, Type.class),
				provideParam(mtd, Constant.class),
				provideParam(mtd, UnaryOperator.class),
				SyntheticLocation.INSTANCE);
		if (param == BinaryExpression.class)
			return (R) new BinaryExpression(
				provideParam(mtd, Type.class),
				provideParam(mtd, Constant.class),
				provideParam(mtd, Constant.class),
				provideParam(mtd, BinaryOperator.class),
				SyntheticLocation.INSTANCE);
		if (param == TernaryExpression.class)
			return (R) new TernaryExpression(
				provideParam(mtd, Type.class),
				provideParam(mtd, Constant.class),
				provideParam(mtd, Constant.class),
				provideParam(mtd, Constant.class),
				provideParam(mtd, TernaryOperator.class),
				SyntheticLocation.INSTANCE);

		// operators
		if (param == TernaryOperator.class)
			return (R) StringReplace.INSTANCE;
		if (param == BinaryOperator.class)
			return (R) ComparisonEq.INSTANCE;
		if (param == UnaryOperator.class)
			return (R) LogicalNegation.INSTANCE;

		// value domains
		if (param == ValueEnvironment.class)
			return (R) new ValueEnvironment<>(SingleValueLattice.SINGLETON);
		if (param == SampleNRVD.class || param == BaseNonRelationalValueDomain.class)
			return (R) new SampleNRVD();
		if (param == BaseNonRelationalValueDomain[].class)
			return (R) new BaseNonRelationalValueDomain[0];

		// type domains
		if (param == TypeEnvironment.class || param == TypeLattice.class)
			return (R) new TypeEnvironment<>(SingleTypeLattice.SINGLETON);
		if (param == SampleNRTD.class || param == BaseNonRelationalTypeDomain.class)
			return (R) new SampleNRTD();
		if (param == BaseNonRelationalTypeDomain[].class)
			return (R) new BaseNonRelationalTypeDomain[0];

		// heap domains
		if (param == HeapEnvironment.class)
			return (R) new HeapEnvironment<>(SingleHeapLattice.SINGLETON);
		if (param == SampleNRHD.class)
			return (R) new SampleNRHD();

		// params to semantic functions
		if (param == SemanticOracle.class)
			return (R) new FakeOracle();
		if (param == ProgramPoint.class)
			return (R) new FakePP();

		throw new UnsupportedOperationException(mtd + ": No default value for type " + param.getName());
	}

	@SuppressWarnings("unchecked")
	public static <R> R build(
			Class<R> clazz) {
		if (clazz == IntInterval.class)
			return (R) IntInterval.TOP;
		if (clazz == SimpleTaint.class)
			return (R) SimpleTaint.TAINTED;
		if (clazz == Monolith.class)
			return (R) Monolith.SINGLETON;
		if (clazz == NonInterferenceValue.class)
			return (R) NonInterferenceValue.HIGH_LOW;
		if (clazz == SingleHeapLattice.class)
			return (R) SingleHeapLattice.SINGLETON;
		if (clazz == SingleTypeLattice.class)
			return (R) SingleTypeLattice.SINGLETON;
		if (clazz == SingleValueLattice.class)
			return (R) SingleValueLattice.SINGLETON;
		if (clazz == SimpleAbstractDomain.class)
			return (R) DefaultConfiguration.defaultAbstractDomain();
		return null;
	}

	@SuppressWarnings("unchecked")
	public static <R> R domainFor(
			Class<?> root,
			Class<?> param) {
		// cfg and interprocedural
		if (param == CFG.class)
			return (R) cfg;
		if (param == AnalyzedCFG.class)
			return (R) new AnalyzedCFG<>(cfg, new UniqueScope(), as);
		if (param == CFGResults.class)
			return (R) new CFGResults<>(new AnalyzedCFG<>(cfg, new UniqueScope(), as));
		if (param == InterproceduralAnalysis.class)
			return (R) interprocedural;
		if (param == ScopeId.class)
			return (R) new UniqueScope();

		// lattice structures
		if (root == ValueEnvironment.class || param == ValueLattice.class)
			return (R) SingleValueLattice.SINGLETON;
		if (root == HeapEnvironment.class || param == HeapLattice.class)
			return (R) SingleHeapLattice.SINGLETON;
		if (root == TypeEnvironment.class || param == TypeLattice.class)
			return (R) SingleTypeLattice.SINGLETON;
		if (param == AbstractLattice.class)
			return (R) new SimpleAbstractState<>(
				Monolith.SINGLETON,
				new ValueEnvironment<>(IntInterval.TOP),
				new TypeEnvironment<>(new TypeSet()));
		if (param == SymbolicExpression.class)
			return (R) new Skip(SyntheticLocation.INSTANCE);
		if (param == AnalysisState.class)
			return (R) as;
		if (param == Lattice.class)
			return (R) IntInterval.TOP;
		if (root == DefiniteIdSet.class && param == Set.class)
			return (R) Collections.emptySet();
		if (param == Satisfiability.class || param == WholeValueElement.class)
			return (R) Satisfiability.UNKNOWN;
		if (param == NonRedundantSetLattice.class)
			return (R) new NonRedundantIntervalSet();
		if (param == NonRedundantSetDomainLattice.class)
			return (R) new SimpleNRSDL();
		if (param == BaseNonRelationalValueDomain.class
				|| param == ValueDomain.class
				|| param == SmashedSumIntDomain.class)
			return (R) new Interval();
		if (param == SmashedSumStringDomain.class || param == WholeValueStringDomain.class)
			return (R) new Prefix();

		// domains
		if (param == Bricks.class)
			return (R) new Bricks();
		if (param == BoundedStringSet.class)
			return (R) new BoundedStringSet();
		if (root == TracePartitioning.class || root == Analysis.class)
			return (R) DefaultConfiguration.defaultAbstractDomain();

		throw new UnsupportedOperationException(
			"No default domain for domain " + root + " and parameter of type " + param);
	}

}
