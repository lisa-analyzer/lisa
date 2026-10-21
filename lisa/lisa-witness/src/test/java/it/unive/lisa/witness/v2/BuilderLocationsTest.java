package it.unive.lisa.witness.v2;

import static it.unive.lisa.witness.v2.TestValues.HASH;
import static it.unive.lisa.witness.v2.TestValues.at;
import static it.unive.lisa.witness.v2.TestValues.task;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import it.unive.lisa.witness.v2.correctness.Invariant;
import it.unive.lisa.witness.v2.correctness.InvariantType;
import it.unive.lisa.witness.v2.metadata.Producer;
import it.unive.lisa.witness.v2.metadata.Task;
import it.unive.lisa.witness.v2.violation.Action;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class BuilderLocationsTest {

	private static final Producer PRODUCER = new Producer("Test", "1");

	private static final String MAIN = "../../sv-benchmarks/java/jpf-regression/ExDarko_false/Main.java";

	private static final Location OTHER = new Location("Other.java", 2);

	private static Task benchexecTask() {
		return new Task(List.of(MAIN), Map.of(MAIN, HASH), "CHECK( init(Main.main()), LTL(G assert) )", Language.JAVA,
				null);
	}

	@Test
	public void aWrongFileFailsAtTheViolationCall() {
		ViolationWitnessBuilder builder = new ViolationWitnessBuilder(task(Language.JAVA), PRODUCER)
				.target(at(1));
		assertThrows(InvalidWitnessException.class, () -> builder.functionReturn(OTHER, "\\result == 0"));
		assertThrows(InvalidWitnessException.class,
				() -> builder.segment(s -> s.assumption(Action.AVOID, OTHER, "x == 0")));
	}

	@Test
	public void aWrongFileFailsAtTheCorrectnessCall() {
		CorrectnessWitnessBuilder builder = new CorrectnessWitnessBuilder(task(Language.C), PRODUCER);
		assertThrows(InvalidWitnessException.class, () -> builder.loopInvariant(OTHER, "i >= 0"));
		assertThrows(InvalidWitnessException.class, () -> builder.contract(OTHER, "x > 0", null));
		assertThrows(InvalidWitnessException.class, () -> builder.ghostUpdate(OTHER, "g", "1"));
		assertThrows(InvalidWitnessException.class, () -> builder.item(
				new Invariant(InvariantType.LOCATION_INVARIANT, OTHER, "x == 0", Format.C_EXPRESSION)));
	}

	@Test
	public void aPathGivesTheNameAsWrittenInTheTask() {
		ViolationWitnessBuilder builder = new ViolationWitnessBuilder(benchexecTask(), PRODUCER);
		assertEquals(new Location(MAIN, 34, 43), builder.location(Path.of(MAIN), 34, 43));
		assertEquals(new Location(MAIN, 34, 43),
				builder.location(Path.of("../../sv-benchmarks/java/./jpf-regression/ExDarko_false/Main.java"), 34, 43));
		assertEquals(new Location(MAIN, 34, 43, "main"),
				builder.location(Path.of(MAIN).toAbsolutePath(), 34, 43, "main"));
		assertEquals(new Location(MAIN, 48), builder.location(Path.of(MAIN), 48));
	}

	@Test
	public void aPathOutsideTheTaskIsRejected() {
		CorrectnessWitnessBuilder builder = new CorrectnessWitnessBuilder(benchexecTask(), PRODUCER);
		assertThrows(InvalidWitnessException.class, () -> builder.location(Path.of("Main.java"), 1));
	}

	@Test
	public void theLocationsOfAPathBuildAWitness() {
		ViolationWitnessBuilder builder = new ViolationWitnessBuilder(benchexecTask(), PRODUCER);
		Path main = Path.of(MAIN).toAbsolutePath();
		ViolationWitness witness = builder
				.functionReturn(builder.location(main, 34, 43), "\\result == 0")
				.target(builder.location(main, 48, 12))
				.build();
		assertEquals(MAIN,
				witness.getSequence().getSegments().get(1).getWaypoints().get(0).getLocation().getFileName());
	}
}
