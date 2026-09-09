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

public class RuntimeTypesMatchingStrategyTest {

	@Test
	public void matchesIfAtLeastOneRuntimeTypeIsAssignableToTheFormal() {
		Program p = ResolutionTestFixtures.mkProgram();
		CFG cfg = ResolutionTestFixtures.mkCfg(p);
		Parameter formal = new Parameter(new SourceCodeLocation("fake", 0, 0), "a", ResolutionTestFixtures.SUPERTYPE);
		VariableRef actual = new VariableRef(cfg, new SourceCodeLocation("fake", 0, 0), "x");

		assertTrue(
				RuntimeTypesMatchingStrategy.INSTANCE.matches(
						null, 0, formal, actual,
						Set.of(ResolutionTestFixtures.STRING_TYPE, ResolutionTestFixtures.NUMBER_TYPE)));
	}

	@Test
	public void doesNotMatchWhenNoRuntimeTypeIsAssignableToTheFormal() {
		Program p = ResolutionTestFixtures.mkProgram();
		CFG cfg = ResolutionTestFixtures.mkCfg(p);
		Parameter formal = new Parameter(new SourceCodeLocation("fake", 0, 0), "a", ResolutionTestFixtures.NUMBER_TYPE);
		VariableRef actual = new VariableRef(cfg, new SourceCodeLocation("fake", 0, 0), "x");

		assertFalse(
				RuntimeTypesMatchingStrategy.INSTANCE
						.matches(null, 0, formal, actual, Set.of(ResolutionTestFixtures.STRING_TYPE)));
	}

	@Test
	public void doesNotMatchWhenThereAreNoRuntimeTypes() {
		Program p = ResolutionTestFixtures.mkProgram();
		CFG cfg = ResolutionTestFixtures.mkCfg(p);
		Parameter formal = new Parameter(new SourceCodeLocation("fake", 0, 0), "a", ResolutionTestFixtures.SUPERTYPE);
		VariableRef actual = new VariableRef(cfg, new SourceCodeLocation("fake", 0, 0), "x");

		assertFalse(RuntimeTypesMatchingStrategy.INSTANCE.matches(null, 0, formal, actual, Set.of()));
	}

}
