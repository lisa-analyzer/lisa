package it.unive.lisa.program.language.resolution;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.program.cfg.statement.call.Call.CallType;
import it.unive.lisa.program.cfg.statement.call.NamedParameterExpression;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.type.Type;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class PythonLikeMatchingStrategyTest {

	private static Parameter param(
			String name) {
		return new Parameter(new SourceCodeLocation("fake", 0, 0), name, ResolutionTestFixtures.SUPERTYPE);
	}

	private static Parameter paramWithDefault(
			String name,
			Expression def) {
		return new Parameter(new SourceCodeLocation("fake", 0, 0), name, def);
	}

	private static VariableRef ref(
			CFG cfg,
			String name) {
		return new VariableRef(cfg, new SourceCodeLocation("fake", 0, 0), name, ResolutionTestFixtures.SUPERTYPE);
	}

	private static NamedParameterExpression named(
			CFG cfg,
			String name,
			Expression value) {
		return new NamedParameterExpression(cfg, new SourceCodeLocation("fake", 0, 0), name, value);
	}

	@SuppressWarnings("unchecked")
	private static Set<Type>[] emptyTypes(
			int n) {
		Set<Type>[] types = new Set[n];
		for (int i = 0; i < n; i++)
			types[i] = Set.of();
		return types;
	}

	@Test
	public void positionalArgumentsFillSlotsInOrder() {
		Program p = ResolutionTestFixtures.mkProgram();
		CFG cfg = ResolutionTestFixtures.mkCfg(p);
		Parameter[] formals = { param("a"), param("b") };
		Expression x = ref(cfg, "x");
		Expression y = ref(cfg, "y");
		Expression[] actuals = { x, y };
		Expression[] slots = new Expression[2];
		@SuppressWarnings("unchecked")
		Set<Type>[] slotTypes = new Set[2];

		@SuppressWarnings("unchecked")
		Boolean failure = PythonLikeMatchingStrategy.pythonLogic(
				formals, actuals, actuals, emptyTypes(2), new Expression[2], new Set[2], slots, slotTypes, false);

		assertNull(failure);
		assertSame(x, slots[0]);
		assertSame(y, slots[1]);
	}

	@Test
	public void keywordArgumentsFillSlotsByNameEvenOutOfDeclarationOrder() {
		// regression test: the second-phase search used to start scanning
		// formals from the keyword argument's own position in the actuals
		// array instead of from 0, so a keyword argument targeting an
		// earlier-declared formal than its own position could never be
		// found and the call was wrongly rejected as a TypeError
		Program p = ResolutionTestFixtures.mkProgram();
		CFG cfg = ResolutionTestFixtures.mkCfg(p);
		Parameter[] formals = { param("a"), param("b") };
		Expression bValue = ref(cfg, "bv");
		Expression aValue = ref(cfg, "av");
		// f(b=bv, a=av): "a" is declared before "b", but is given after it
		Expression[] actuals = { named(cfg, "b", bValue), named(cfg, "a", aValue) };
		Expression[] slots = new Expression[2];
		@SuppressWarnings("unchecked")
		Set<Type>[] slotTypes = new Set[2];

		@SuppressWarnings("unchecked")
		Boolean failure = PythonLikeMatchingStrategy.pythonLogic(
				formals, actuals, actuals, emptyTypes(2), new Expression[2], new Set[2], slots, slotTypes, false);

		assertNull(failure);
		// slots hold the given element itself (here, the actual's
		// NamedParameterExpression wrapper - matches production's own
		// pythonLogic(formals, actuals, actuals, ...) call), not an unwrapped
		// value
		assertSame(actuals[1], slots[0]);
		assertSame(actuals[0], slots[1]);
	}

	@Test
	public void aSlotFilledTwiceIsRejected() {
		Program p = ResolutionTestFixtures.mkProgram();
		CFG cfg = ResolutionTestFixtures.mkCfg(p);
		Parameter[] formals = { param("a"), param("b") };
		Expression x = ref(cfg, "x");
		Expression again = ref(cfg, "again");
		// f(x, a=again): positional fills slot 0 ("a"), then the keyword
		// argument targets the same slot again
		Expression[] actuals = { x, named(cfg, "a", again) };
		Expression[] slots = new Expression[2];
		@SuppressWarnings("unchecked")
		Set<Type>[] slotTypes = new Set[2];

		@SuppressWarnings("unchecked")
		Boolean failure = PythonLikeMatchingStrategy.pythonLogic(
				formals, actuals, actuals, emptyTypes(2), new Expression[2], new Set[2], slots, slotTypes, false);

		assertFalse(failure);
	}

	@Test
	public void unfilledSlotsAreFilledFromDefaultsWhenAvailable() {
		Program p = ResolutionTestFixtures.mkProgram();
		CFG cfg = ResolutionTestFixtures.mkCfg(p);
		Expression defaultValue = ref(cfg, "def");
		Parameter[] formals = { param("a"), paramWithDefault("b", defaultValue) };
		Expression x = ref(cfg, "x");
		Expression[] actuals = { x };
		Expression[] defaults = { null, defaultValue };
		@SuppressWarnings("unchecked")
		Set<Type>[] defaultTypes = new Set[] { null, Set.of(ResolutionTestFixtures.SUPERTYPE) };
		Expression[] slots = new Expression[2];
		@SuppressWarnings("unchecked")
		Set<Type>[] slotTypes = new Set[2];

		Boolean failure = PythonLikeMatchingStrategy.pythonLogic(
				formals, actuals, actuals, emptyTypes(1), defaults, defaultTypes, slots, slotTypes, false);

		assertNull(failure);
		assertSame(x, slots[0]);
		assertSame(defaultValue, slots[1]);
	}

	@Test
	public void unfilledSlotsWithoutADefaultAreRejected() {
		Program p = ResolutionTestFixtures.mkProgram();
		CFG cfg = ResolutionTestFixtures.mkCfg(p);
		Parameter[] formals = { param("a"), param("b") };
		Expression x = ref(cfg, "x");
		Expression[] actuals = { x };
		Expression[] slots = new Expression[2];
		@SuppressWarnings("unchecked")
		Set<Type>[] slotTypes = new Set[2];

		@SuppressWarnings("unchecked")
		Boolean failure = PythonLikeMatchingStrategy.pythonLogic(
				formals, actuals, actuals, emptyTypes(1), new Expression[2], new Set[2], slots, slotTypes, false);

		assertFalse(failure);
	}

	@Test
	public void moreActualsThanFormalsAreRejected() {
		Program p = ResolutionTestFixtures.mkProgram();
		CFG cfg = ResolutionTestFixtures.mkCfg(p);
		Parameter[] formals = { param("a") };
		Expression[] actuals = { ref(cfg, "x"), ref(cfg, "y") };
		Expression[] slots = new Expression[1];
		@SuppressWarnings("unchecked")
		Set<Type>[] slotTypes = new Set[1];

		@SuppressWarnings("unchecked")
		Boolean failure = PythonLikeMatchingStrategy.pythonLogic(
				formals, actuals, actuals, emptyTypes(2), new Expression[1], new Set[1], slots, slotTypes, false);

		assertFalse(failure);
	}

	@Test
	public void matchesReordersActualsThenDelegatesToTheWrappedStrategy() {
		Program p = ResolutionTestFixtures.mkProgram();
		CFG cfg = ResolutionTestFixtures.mkCfg(p);
		Parameter[] formals = { param("a"), param("b") };
		Expression bValue = ref(cfg, "bv");
		Expression aValue = ref(cfg, "av");
		Expression[] actuals = { named(cfg, "b", bValue), named(cfg, "a", aValue) };
		UnresolvedCall call = new UnresolvedCall(
				cfg, new SourceCodeLocation("fake", 1, 0), CallType.STATIC, p.getName(), "target", actuals);

		PythonLikeMatchingStrategy strategy = new PythonLikeMatchingStrategy(StaticTypesMatchingStrategy.INSTANCE);
		assertTrue(strategy.matches(call, formals, actuals, emptyTypes(2)));
	}

	@Test
	public void matchesFailsWhenReorderingCannotFillEverySlot() {
		Program p = ResolutionTestFixtures.mkProgram();
		CFG cfg = ResolutionTestFixtures.mkCfg(p);
		Parameter[] formals = { param("a"), param("b") };
		Expression[] actuals = { ref(cfg, "x") };
		UnresolvedCall call = new UnresolvedCall(
				cfg, new SourceCodeLocation("fake", 1, 0), CallType.STATIC, p.getName(), "target", actuals);

		PythonLikeMatchingStrategy strategy = new PythonLikeMatchingStrategy(StaticTypesMatchingStrategy.INSTANCE);
		assertFalse(strategy.matches(call, formals, actuals, emptyTypes(1)));
	}

}
