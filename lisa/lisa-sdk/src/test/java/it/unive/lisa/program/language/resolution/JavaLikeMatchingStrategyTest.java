package it.unive.lisa.program.language.resolution;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.program.cfg.statement.call.Call.CallType;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class JavaLikeMatchingStrategyTest {

	@Test
	public void theReceiverOfAnInstanceCallIsMatchedByRuntimeTypeEvenIfItsStaticTypeMismatches() {
		Program p = ResolutionTestFixtures.mkProgram();
		CFG cfg = ResolutionTestFixtures.mkCfg(p);
		Parameter formal = new Parameter(new SourceCodeLocation("fake", 0, 0), "self",
				ResolutionTestFixtures.SUPERTYPE);
		// static type is STRING (does not match), but its only runtime type
		// is SUPERTYPE (does match): a Java-like strategy must use the
		// runtime type for the receiver of an instance call
		VariableRef receiver = new VariableRef(
				cfg, new SourceCodeLocation("fake", 0, 0), "self", ResolutionTestFixtures.STRING_TYPE);
		UnresolvedCall call = new UnresolvedCall(
				cfg, new SourceCodeLocation("fake", 1, 0), CallType.INSTANCE, p.getName(), "m", receiver);

		assertTrue(
				JavaLikeMatchingStrategy.INSTANCE
						.matches(call, 0, formal, receiver, Set.of(ResolutionTestFixtures.SUPERTYPE)));
	}

	@Test
	public void nonReceiverPositionsAreMatchedByStaticTypeEvenIfARuntimeTypeWouldMatch() {
		Program p = ResolutionTestFixtures.mkProgram();
		CFG cfg = ResolutionTestFixtures.mkCfg(p);
		Parameter formal = new Parameter(new SourceCodeLocation("fake", 0, 0), "arg",
				ResolutionTestFixtures.NUMBER_TYPE);
		// static type STRING does not match NUMBER, and this is not
		// position 0 of an instance call, so the runtime type (which would
		// match, since SUPERTYPE is assignable to itself is irrelevant here -
		// the point is that a matching runtime type must still be ignored)
		VariableRef actual = new VariableRef(
				cfg, new SourceCodeLocation("fake", 0, 0), "arg", ResolutionTestFixtures.STRING_TYPE);
		UnresolvedCall call = new UnresolvedCall(
				cfg, new SourceCodeLocation("fake", 1, 0), CallType.INSTANCE, p.getName(), "m", actual, actual);

		assertFalse(
				JavaLikeMatchingStrategy.INSTANCE
						.matches(call, 1, formal, actual, Set.of(ResolutionTestFixtures.NUMBER_TYPE)));
	}

	@Test
	public void firstPositionOfAStaticCallIsMatchedByStaticTypeNotRuntimeType() {
		Program p = ResolutionTestFixtures.mkProgram();
		CFG cfg = ResolutionTestFixtures.mkCfg(p);
		Parameter formal = new Parameter(new SourceCodeLocation("fake", 0, 0), "arg",
				ResolutionTestFixtures.NUMBER_TYPE);
		VariableRef actual = new VariableRef(
				cfg, new SourceCodeLocation("fake", 0, 0), "arg", ResolutionTestFixtures.STRING_TYPE);
		UnresolvedCall call = new UnresolvedCall(
				cfg, new SourceCodeLocation("fake", 1, 0), CallType.STATIC, p.getName(), "m", actual);

		assertFalse(
				JavaLikeMatchingStrategy.INSTANCE
						.matches(call, 0, formal, actual, Set.of(ResolutionTestFixtures.NUMBER_TYPE)));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void wholeCallMatchesOnlyWhenEveryPositionMatchesAccordingToItsOwnStrategy() {
		Program p = ResolutionTestFixtures.mkProgram();
		CFG cfg = ResolutionTestFixtures.mkCfg(p);
		Parameter self = new Parameter(new SourceCodeLocation("fake", 0, 0), "self", ResolutionTestFixtures.SUPERTYPE);
		Parameter arg = new Parameter(new SourceCodeLocation("fake", 0, 0), "arg", ResolutionTestFixtures.SUPERTYPE);
		VariableRef receiver = new VariableRef(
				cfg, new SourceCodeLocation("fake", 0, 0), "self", ResolutionTestFixtures.STRING_TYPE);
		VariableRef actual = new VariableRef(
				cfg, new SourceCodeLocation("fake", 0, 0), "arg", ResolutionTestFixtures.SUPERTYPE);
		UnresolvedCall call = new UnresolvedCall(
				cfg, new SourceCodeLocation("fake", 1, 0), CallType.INSTANCE, p.getName(), "m", receiver, actual);

		assertTrue(
				JavaLikeMatchingStrategy.INSTANCE.matches(
						call,
						new Parameter[] { self, arg },
						new it.unive.lisa.program.cfg.statement.Expression[] { receiver, actual },
						new Set[] { Set.of(ResolutionTestFixtures.SUPERTYPE),
								Set.of(ResolutionTestFixtures.STRING_TYPE) }));
	}

}
