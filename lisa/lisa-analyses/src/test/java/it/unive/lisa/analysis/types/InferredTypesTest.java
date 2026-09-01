package it.unive.lisa.analysis.types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestParameterProvider;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.nonrelational.type.TypeEnvironment;
import it.unive.lisa.imp.types.IMPTypeSystem;
import it.unive.lisa.lattices.types.TypeSet;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.PushInv;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.TypeSystem;
import it.unive.lisa.type.Untyped;
import org.junit.jupiter.api.Test;

public class InferredTypesTest {

	private static final TypeSystem types = new IMPTypeSystem();

	private final InferredTypes domain = new InferredTypes();

	private final ProgramPoint pp = TestParameterProvider.provideParam(null, ProgramPoint.class);

	private final SemanticOracle oracle = TestParameterProvider.provideParam(null, SemanticOracle.class);

	private final SyntheticLocation loc = SyntheticLocation.INSTANCE;

	@Test
	public void evalPushAnyOfATypedExpressionYieldsAllItsInstances()
			throws SemanticException {
		TypeSet result = domain.evalPushAny(new PushAny(Int32Type.INSTANCE, loc), pp, oracle);
		assertEquals(Int32Type.INSTANCE.allInstances(types), result.elements);
	}

	@Test
	public void evalPushAnyOfAnUntypedExpressionIsTop()
			throws SemanticException {
		TypeSet result = domain.evalPushAny(new PushAny(Untyped.INSTANCE, loc), pp, oracle);
		assertTrue(result.isTop());
	}

	@Test
	public void evalPushInvIsAlwaysBottom()
			throws SemanticException {
		TypeSet result = domain.evalPushInv(new PushInv(Int32Type.INSTANCE, loc), pp, oracle);
		assertTrue(result.isBottom());
	}

	@Test
	public void evalConstantYieldsItsStaticType() {
		TypeSet result = domain.evalConstant(new Constant(BoolType.INSTANCE, true, loc), pp, oracle);
		assertEquals(new TypeSet(types, BoolType.INSTANCE), result);
	}

	@Test
	public void evalIdentifierWithNoTrackedInformationFallsBackToItsStaticType()
			throws SemanticException {
		Identifier x = new Variable(Int32Type.INSTANCE, "x", loc);
		TypeEnvironment<TypeSet> env = new TypeEnvironment<>(domain.top());
		TypeSet result = domain.evalIdentifier(x, env, pp, oracle);
		assertEquals(Int32Type.INSTANCE.allInstances(types), result.elements);
	}

	@Test
	public void evalIdentifierWithTrackedInformationReturnsIt()
			throws SemanticException {
		Identifier x = new Variable(Int32Type.INSTANCE, "x", loc);
		TypeSet tracked = new TypeSet(types, BoolType.INSTANCE);
		TypeEnvironment<TypeSet> env = new TypeEnvironment<>(domain.top()).putState(x, tracked);
		TypeSet result = domain.evalIdentifier(x, env, pp, oracle);
		assertEquals(tracked, result);
	}

	@Test
	public void topIsTheTopTypeSet() {
		assertSame(TypeSet.TOP, domain.top());
	}

	@Test
	public void bottomIsTheBottomTypeSet() {
		assertSame(TypeSet.BOTTOM, domain.bottom());
	}

}
