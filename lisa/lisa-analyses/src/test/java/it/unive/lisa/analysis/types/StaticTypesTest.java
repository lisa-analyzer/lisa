package it.unive.lisa.analysis.types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestParameterProvider;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.nonrelational.type.TypeEnvironment;
import it.unive.lisa.imp.types.IMPTypeSystem;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.lattices.types.Supertype;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.program.type.StringType;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.PushInv;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.TypeCast;
import it.unive.lisa.symbolic.value.operator.binary.TypeCheck;
import it.unive.lisa.type.TypeSystem;
import it.unive.lisa.type.TypeTokenType;
import java.util.Collections;
import org.junit.jupiter.api.Test;

public class StaticTypesTest {

	private static final TypeSystem types = new IMPTypeSystem();

	private final StaticTypes domain = new StaticTypes();

	private final ProgramPoint pp = TestParameterProvider.provideParam(null, ProgramPoint.class);

	private final SemanticOracle oracle = TestParameterProvider.provideParam(null, SemanticOracle.class);

	private final SyntheticLocation loc = SyntheticLocation.INSTANCE;

	@Test
	public void evalPushAnyYieldsItsStaticType() {
		Supertype result = domain.evalPushAny(new PushAny(Int32Type.INSTANCE, loc), pp, oracle);
		assertEquals(new Supertype(types, Int32Type.INSTANCE), result);
	}

	@Test
	public void evalPushInvYieldsItsStaticType()
			throws SemanticException {
		Supertype result = domain.evalPushInv(new PushInv(StringType.INSTANCE, loc), pp, oracle);
		assertEquals(new Supertype(types, StringType.INSTANCE), result);
	}

	@Test
	public void evalConstantYieldsItsStaticType() {
		Supertype result = domain.evalConstant(new Constant(BoolType.INSTANCE, true, loc), pp, oracle);
		assertEquals(new Supertype(types, BoolType.INSTANCE), result);
	}

	@Test
	public void evalOfAPlainExpressionYieldsItsStaticType()
			throws SemanticException {
		TypeEnvironment<Supertype> env = new TypeEnvironment<>(domain.bottom());
		Supertype result = domain.eval(env, new Constant(Int32Type.INSTANCE, 5, loc), pp, oracle);
		assertEquals(new Supertype(types, Int32Type.INSTANCE), result);
	}

	@Test
	public void evalOfASuccessfulCastYieldsTheTargetType()
			throws SemanticException {
		TypeEnvironment<Supertype> env = new TypeEnvironment<>(domain.bottom());
		BinaryExpression cast = new BinaryExpression(
				Int32Type.INSTANCE,
				new Constant(Int32Type.INSTANCE, 5, loc),
				new Constant(new TypeTokenType(Collections.singleton(Int32Type.INSTANCE)), Int32Type.INSTANCE, loc),
				TypeCast.INSTANCE,
				loc);
		Supertype result = domain.eval(env, cast, pp, oracle);
		assertEquals(new Supertype(types, Int32Type.INSTANCE), result);
	}

	@Test
	public void evalOfAnImpossibleCastYieldsBottom()
			throws SemanticException {
		TypeEnvironment<Supertype> env = new TypeEnvironment<>(domain.bottom());
		BinaryExpression cast = new BinaryExpression(
				StringType.INSTANCE,
				new Constant(Int32Type.INSTANCE, 5, loc),
				new Constant(new TypeTokenType(Collections.singleton(StringType.INSTANCE)), StringType.INSTANCE, loc),
				TypeCast.INSTANCE,
				loc);
		Supertype result = domain.eval(env, cast, pp, oracle);
		assertTrue(result.isBottom());
	}

	@Test
	public void satisfiesTypeCheckOfTheSameTypeIsSatisfied()
			throws SemanticException {
		Supertype left = new Supertype(types, Int32Type.INSTANCE);
		Supertype right = new Supertype(types, new TypeTokenType(Collections.singleton(Int32Type.INSTANCE)));
		BinaryExpression check = new BinaryExpression(
				BoolType.INSTANCE,
				new Constant(Int32Type.INSTANCE, 5, loc),
				new Constant(new TypeTokenType(Collections.singleton(Int32Type.INSTANCE)), Int32Type.INSTANCE, loc),
				TypeCheck.INSTANCE,
				loc);
		Satisfiability sat = domain.satisfiesBinaryExpression(check, left, right, pp, oracle);
		assertEquals(Satisfiability.SATISFIED, sat);
	}

	@Test
	public void satisfiesTypeCheckOfAnUnrelatedTypeIsNotSatisfied()
			throws SemanticException {
		Supertype left = new Supertype(types, Int32Type.INSTANCE);
		Supertype right = new Supertype(types, new TypeTokenType(Collections.singleton(StringType.INSTANCE)));
		BinaryExpression check = new BinaryExpression(
				BoolType.INSTANCE,
				new Constant(Int32Type.INSTANCE, 5, loc),
				new Constant(new TypeTokenType(Collections.singleton(StringType.INSTANCE)), StringType.INSTANCE, loc),
				TypeCheck.INSTANCE,
				loc);
		Satisfiability sat = domain.satisfiesBinaryExpression(check, left, right, pp, oracle);
		assertEquals(Satisfiability.NOT_SATISFIED, sat);
	}

	@Test
	public void assumeEqComparisonNarrowsToTheExactType()
			throws SemanticException {
		Variable x = new Variable(Int32Type.INSTANCE, "x", loc);
		TypeEnvironment<Supertype> env = new TypeEnvironment<>(domain.bottom()).putState(x,
				new Supertype(types, Int32Type.INSTANCE));

		BinaryExpression eq = new BinaryExpression(
				BoolType.INSTANCE,
				x,
				new Constant(Int32Type.INSTANCE, 5, loc),
				ComparisonEq.INSTANCE,
				loc);
		TypeEnvironment<Supertype> result = domain.assumeBinaryExpression(env, eq, pp, pp, oracle);
		assertEquals(new Supertype(types, Int32Type.INSTANCE), result.getState(x));
	}

	@Test
	public void assumeEqComparisonWithIncompatibleTypeIsBottom()
			throws SemanticException {
		Variable x = new Variable(StringType.INSTANCE, "x", loc);
		TypeEnvironment<Supertype> env = new TypeEnvironment<>(domain.bottom()).putState(x,
				new Supertype(types, StringType.INSTANCE));

		BinaryExpression eq = new BinaryExpression(
				BoolType.INSTANCE,
				x,
				new Constant(Int32Type.INSTANCE, 5, loc),
				ComparisonEq.INSTANCE,
				loc);
		TypeEnvironment<Supertype> result = domain.assumeBinaryExpression(env, eq, pp, pp, oracle);
		assertTrue(result.isBottom());
	}

	@Test
	public void topIsTheUntypedSupertype() {
		assertTrue(domain.top().isTop());
	}

	@Test
	public void bottomIsTheBottomSupertype() {
		assertSame(Supertype.BOTTOM, domain.bottom());
		assertTrue(domain.bottom().isBottom());
	}

}
