package it.unive.lisa.program.language.resolution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.cfg.statement.call.Call.CallType;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class FixedOrderMatchingStrategyTest {

	// records, in order, the positions that matches(pos, ...) was invoked
	// for, and answers according to a caller-supplied per-position script -
	// this lets tests observe whether the template method short-circuits on
	// the first mismatch
	private static class RecordingStrategy
			extends
			FixedOrderMatchingStrategy {

		final List<Integer> checked = new ArrayList<>();
		final boolean[] answers;

		RecordingStrategy(
				boolean... answers) {
			this.answers = answers;
		}

		@Override
		public boolean matches(
				Call call,
				int pos,
				Parameter formal,
				Expression actual,
				Set<Type> types) {
			checked.add(pos);
			return answers[pos];
		}
	}

	private static Expression expr(
			CFG cfg) {
		return new VariableRef(cfg, new SourceCodeLocation("fake", 0, 0), "x");
	}

	@Test
	public void arityMismatchIsRejectedWithoutInvokingThePerPositionCheck() {
		Program p = ResolutionTestFixtures.mkProgram();
		CFG cfg = ResolutionTestFixtures.mkCfg(p);
		RecordingStrategy strategy = new RecordingStrategy(true, true);
		Parameter[] formals = { new Parameter(new SourceCodeLocation("fake", 0, 0), "a", Untyped.INSTANCE) };
		Expression[] actuals = { expr(cfg), expr(cfg) };
		@SuppressWarnings("unchecked")
		Set<Type>[] types = new Set[] { Set.of(), Set.of() };

		assertFalse(strategy.matches(null, formals, actuals, types));
		assertTrue(strategy.checked.isEmpty());
	}

	@Test
	public void allPositionsAreCheckedInOrderWhenEveryOneMatches() {
		Program p = ResolutionTestFixtures.mkProgram();
		CFG cfg = ResolutionTestFixtures.mkCfg(p);
		RecordingStrategy strategy = new RecordingStrategy(true, true, true);
		Parameter[] formals = new Parameter[3];
		Expression[] actuals = new Expression[3];
		@SuppressWarnings("unchecked")
		Set<Type>[] types = new Set[3];
		for (int i = 0; i < 3; i++) {
			formals[i] = new Parameter(new SourceCodeLocation("fake", 0, 0), "p" + i, Untyped.INSTANCE);
			actuals[i] = expr(cfg);
			types[i] = Set.of();
		}

		assertTrue(strategy.matches(null, formals, actuals, types));
		assertEquals(List.of(0, 1, 2), strategy.checked);
	}

	@Test
	public void matchingStopsAtTheFirstFailingPosition() {
		Program p = ResolutionTestFixtures.mkProgram();
		CFG cfg = ResolutionTestFixtures.mkCfg(p);
		RecordingStrategy strategy = new RecordingStrategy(true, false, true);
		Parameter[] formals = new Parameter[3];
		Expression[] actuals = new Expression[3];
		@SuppressWarnings("unchecked")
		Set<Type>[] types = new Set[3];
		for (int i = 0; i < 3; i++) {
			formals[i] = new Parameter(new SourceCodeLocation("fake", 0, 0), "p" + i, Untyped.INSTANCE);
			actuals[i] = expr(cfg);
			types[i] = Set.of();
		}

		assertFalse(strategy.matches(null, formals, actuals, types));
		assertEquals(List.of(0, 1), strategy.checked, "position 2 must not be checked once position 1 already failed");
	}

	@Test
	public void distanceFromPerfectTargetSumsAndPropagatesIncomparability() {
		Program p = ResolutionTestFixtures.mkProgram();
		CFG cfg = ResolutionTestFixtures.mkCfg(p);

		Parameter formal = new Parameter(new SourceCodeLocation("fake", 0, 0), "a", ResolutionTestFixtures.SUPERTYPE);
		Expression actual = new VariableRef(
				cfg, new SourceCodeLocation("fake", 0, 0), "x", ResolutionTestFixtures.NUMBER_TYPE);
		UnresolvedCall call = new UnresolvedCall(
				cfg, new SourceCodeLocation("fake", 1, 0), CallType.STATIC, p.getName(), "target", actual);

		it.unive.lisa.program.cfg.CodeMember target = new CFG(
				new it.unive.lisa.program.cfg.CodeMemberDescriptor(
						new SourceCodeLocation("fake", 0, 0), p, false, "target", formal));

		@SuppressWarnings("unchecked")
		Set<Type>[] types = new Set[] { Set.of(ResolutionTestFixtures.NUMBER_TYPE) };
		assertEquals(1, JavaLikeMatchingStrategy.INSTANCE.distanceFromPerfectTarget(call, types, target, false));

		Expression exactActual = new VariableRef(
				cfg, new SourceCodeLocation("fake", 0, 0), "y", ResolutionTestFixtures.SUPERTYPE);
		UnresolvedCall exactCall = new UnresolvedCall(
				cfg, new SourceCodeLocation("fake", 1, 0), CallType.STATIC, p.getName(), "target", exactActual);
		@SuppressWarnings("unchecked")
		Set<Type>[] exactTypes = new Set[] { Set.of(ResolutionTestFixtures.SUPERTYPE) };
		assertEquals(0,
				JavaLikeMatchingStrategy.INSTANCE.distanceFromPerfectTarget(exactCall, exactTypes, target, false));

		Expression incomparableActual = new VariableRef(
				cfg, new SourceCodeLocation("fake", 0, 0), "z", ResolutionTestFixtures.UNRELATED_TYPE);
		UnresolvedCall incomparableCall = new UnresolvedCall(
				cfg, new SourceCodeLocation("fake", 1, 0), CallType.STATIC, p.getName(), "target",
				incomparableActual);
		@SuppressWarnings("unchecked")
		Set<Type>[] incomparableTypes = new Set[] { Set.of(ResolutionTestFixtures.UNRELATED_TYPE) };
		assertEquals(
				-1,
				JavaLikeMatchingStrategy.INSTANCE
						.distanceFromPerfectTarget(incomparableCall, incomparableTypes, target, false));
	}

}
