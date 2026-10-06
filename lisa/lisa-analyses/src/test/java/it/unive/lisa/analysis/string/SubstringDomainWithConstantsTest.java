package it.unive.lisa.analysis.string;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestParameterProvider;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.combination.ValueLatticeProduct;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.lattices.string.StringConstant;
import it.unive.lisa.lattices.string.Substrings;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.type.StringType;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.Variable;
import org.junit.jupiter.api.Test;

public class SubstringDomainWithConstantsTest {

	private final SubstringDomainWithConstants domain = new SubstringDomainWithConstants();

	private final ProgramPoint pp = TestParameterProvider.provideParam(null, ProgramPoint.class);

	private final SemanticOracle oracle = TestParameterProvider.provideParam(null, SemanticOracle.class);

	private final SyntheticLocation loc = SyntheticLocation.INSTANCE;

	private final Identifier x = new Variable(StringType.INSTANCE, "x", loc);

	private final Identifier y = new Variable(StringType.INSTANCE, "y", loc);

	private final ValueExpression hello = new Constant(StringType.INSTANCE, "hello", loc);

	private final ValueExpression world = new Constant(StringType.INSTANCE, "world", loc);

	@Test
	public void assigningTheSameConstantToTwoVariablesLinksThemAsSubstrings()
			throws SemanticException {
		ValueLatticeProduct<ValueEnvironment<StringConstant>, Substrings> state = domain.makeLattice();
		state = domain.assign(state, y, hello, pp, oracle);
		state = domain.assign(state, x, hello, pp, oracle);

		assertTrue(state.second.getState(x).contains(y));
		assertTrue(state.second.getState(y).contains(x));
	}

	@Test
	public void assigningDifferentConstantsDoesNotLinkThem()
			throws SemanticException {
		ValueLatticeProduct<ValueEnvironment<StringConstant>, Substrings> state = domain.makeLattice();
		state = domain.assign(state, y, hello, pp, oracle);
		state = domain.assign(state, x, world, pp, oracle);

		assertFalse(state.second.getState(x).contains(y));
		assertFalse(state.second.getState(y).contains(x));
	}

	@Test
	public void assigningAConstantRecordsItInTheConstantComponent()
			throws SemanticException {
		ValueLatticeProduct<ValueEnvironment<StringConstant>, Substrings> state = domain.makeLattice();
		state = domain.assign(state, x, hello, pp, oracle);

		StringConstant value = state.first.getState(x);
		assertFalse(value.isTop());
		assertFalse(value.isBottom());
		assertTrue(value.value.equals("hello"));
	}

	@Test
	public void smallStepSemanticsLeavesTheStateUnchanged()
			throws SemanticException {
		ValueLatticeProduct<ValueEnvironment<StringConstant>, Substrings> state = domain.makeLattice();
		assertTrue(state == domain.smallStepSemantics(state, hello, pp, oracle));
	}

	@Test
	public void assumeLeavesTheStateUnchanged()
			throws SemanticException {
		ValueLatticeProduct<ValueEnvironment<StringConstant>, Substrings> state = domain.makeLattice();
		assertTrue(state == domain.assume(state, hello, pp, pp, oracle));
	}

}
