package it.unive.lisa.program.language.resolution;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.VariableRef;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class StaticTypesMatchingStrategyTest {

	@Test
	public void matchesWhenTheActualsStaticTypeIsAssignableToTheFormal() {
		Program p = ResolutionTestFixtures.mkProgram();
		CFG cfg = ResolutionTestFixtures.mkCfg(p);
		Parameter formal = new Parameter(new SourceCodeLocation("fake", 0, 0), "a", ResolutionTestFixtures.SUPERTYPE);
		VariableRef actual = new VariableRef(
				cfg, new SourceCodeLocation("fake", 0, 0), "x", ResolutionTestFixtures.NUMBER_TYPE);

		assertTrue(StaticTypesMatchingStrategy.INSTANCE.matches(null, 0, formal, actual, Set.of()));
	}

	@Test
	public void doesNotMatchWhenTheActualsStaticTypeIsNotAssignableToTheFormal() {
		Program p = ResolutionTestFixtures.mkProgram();
		CFG cfg = ResolutionTestFixtures.mkCfg(p);
		Parameter formal = new Parameter(new SourceCodeLocation("fake", 0, 0), "a", ResolutionTestFixtures.NUMBER_TYPE);
		VariableRef actual = new VariableRef(
				cfg, new SourceCodeLocation("fake", 0, 0), "x", ResolutionTestFixtures.STRING_TYPE);

		assertFalse(StaticTypesMatchingStrategy.INSTANCE.matches(null, 0, formal, actual, Set.of()));
	}

	@Test
	public void runtimeTypesAreIgnoredEntirely() {
		// the actual's static type (STRING) does not match the formal
		// (NUMBER), even though its runtime types (also NUMBER) would - this
		// strategy must only look at the actual's static type
		Program p = ResolutionTestFixtures.mkProgram();
		CFG cfg = ResolutionTestFixtures.mkCfg(p);
		Parameter formal = new Parameter(new SourceCodeLocation("fake", 0, 0), "a", ResolutionTestFixtures.NUMBER_TYPE);
		VariableRef actual = new VariableRef(
				cfg, new SourceCodeLocation("fake", 0, 0), "x", ResolutionTestFixtures.STRING_TYPE);

		assertFalse(
				StaticTypesMatchingStrategy.INSTANCE
						.matches(null, 0, formal, actual, Set.of(ResolutionTestFixtures.NUMBER_TYPE)));
	}

}
