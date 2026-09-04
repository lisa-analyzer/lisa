package it.unive.lisa.analysis.combination.constraints;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestAbstractDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.nonrelational.value.BooleanPowerset;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Untyped;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link WholeValueAnalysis}, in particular that its {@code constraints}
 * method follows the contract documented in {@link ValueDomain#constraints}: a
 * {@code null} result for a bottom state, and an empty set for a top one.
 */
public class WholeValueAnalysisTest {

	private static final ProgramPoint fake = new ProgramPoint() {

		@Override
		public CodeLocation getLocation() {
			return SyntheticLocation.INSTANCE;
		}

		@Override
		public CFG getCFG() {
			return null;
		}
	};

	private static final SemanticOracle oracle = new TestAbstractDomain().new TestOracle();

	private final Identifier x = new Variable(Untyped.INSTANCE, "x", SyntheticLocation.INSTANCE);

	// a distinct instance so that the "requesting == participant" guard in
	// WholeValueAnalysis#constraints does not skip the only participant
	private final ValueDomain<?> requester = new BooleanPowerset();

	@Test
	public void testConstraintsOfABottomStateIsNull()
			throws SemanticException {
		WholeValueAnalysis analysis = new WholeValueAnalysis(new BooleanPowerset());
		WholeValue bottom = new WholeValue(new ValueEnvironment<Satisfiability>(Satisfiability.UNKNOWN).bottom());

		Set<BinaryExpression> result = analysis.constraints(requester, bottom, x, fake, oracle);

		assertNull(result, "a bottom state must produce a null (not merely empty) set of constraints");
	}

	@Test
	public void testConstraintsOfATopStateIsEmpty()
			throws SemanticException {
		WholeValueAnalysis analysis = new WholeValueAnalysis(new BooleanPowerset());
		WholeValue top = new WholeValue(new ValueEnvironment<Satisfiability>(Satisfiability.UNKNOWN).top());

		Set<BinaryExpression> result = analysis.constraints(requester, top, x, fake, oracle);

		assertTrue(result.isEmpty());
	}

}
