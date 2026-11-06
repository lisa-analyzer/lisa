package it.unive.lisa.witness.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import it.unive.lisa.witness.v2.metadata.Producer;
import it.unive.lisa.witness.v2.metadata.Task;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Tests rebuilding the examples of the witness format with the witness
 * builders.
 * 
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public class ExamplesFromBuildersTest {

	/**
	 * The producer of the witnesses of the tests.
	 */
	private static final Producer PRODUCER = new Producer("Test", "1");

	/**
	 * Reads an example of the witness format. The older examples have the same
	 * content in version 2.4, and only their version is changed.
	 * 
	 * @param name the name of the example in the test cases
	 * 
	 * @return the witness
	 * 
	 * @throws Exception if the example cannot be read
	 */
	static Witness example(
			String name)
			throws Exception {
		try (InputStream in = Files.newInputStream(Path.of("witness-testcases", name))) {
			String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			return WitnessReader
					.read(text.replaceAll("format_version: (['\"]?)2\\.[0-3]\\1", "format_version: \"2.4\""));
		}
	}

	/**
	 * Returns the task of the given witness.
	 * 
	 * @param witness the witness
	 * 
	 * @return the task of its first entry
	 */
	private static Task task(
			Witness witness) {
		return witness.getEntries().get(0).getMetadata().getTask();
	}

	/**
	 * Checks that the builder builds the segments of the given example.
	 * 
	 * @param example the example
	 * @param builder the builder
	 */
	private static void assertSameSegments(
			Witness example,
			ViolationWitnessBuilder builder) {
		assertEquals(((ViolationWitness) example).getSequence().getSegments(),
				builder.build().getSequence().getSegments());
	}

	/**
	 * Checks that the builder builds the invariants and contracts of the given
	 * example.
	 * 
	 * @param example the example
	 * @param builder the builder
	 */
	private static void assertSameItems(
			Witness example,
			CorrectnessWitnessBuilder builder) {
		assertEquals(((CorrectnessWitness) example).getInvariants().getItems(),
				builder.build().getInvariants().getItems());
	}

	// ---------------------------------------------------------------- Java

	/**
	 * Checks that the builders rebuild the Java violation example
	 * ({@code ExDarko_false}).
	 */
	@Test
	public void javaViolation()
			throws Exception {
		Witness example = example("java/ExDarko_false.witness-2.4.yml");
		String main = "./java/jpf-regression/ExDarko_false/Main.java";
		assertSameSegments(example, new ViolationWitnessBuilder(task(example), PRODUCER)
				.functionReturn(new Location(main, 34, 43), "\\result == 0")
				.functionReturn(new Location(main, 35, 41), "\\result == 0")
				.target(new Location(main, 48, 12)));
	}

	/**
	 * Checks that the builders rebuild the Java correctness example
	 * ({@code ExDarko_true}).
	 */
	@Test
	public void javaCorrectness()
			throws Exception {
		Witness example = example("java/ExDarko_true.witness-2.4.yml");
		String main = "./java/jpf-regression/ExDarko_true/Main.java";
		assertSameItems(example, new CorrectnessWitnessBuilder(task(example), PRODUCER)
				.locationInvariant(new Location(main, 43, 4, "unboxed"), "i != j"));
	}

	// ---------------------------------------------------------------- Python

	/**
	 * Checks that the builders rebuild the Python violation example
	 * ({@code zero_div}).
	 */
	@Test
	public void pythonViolation()
			throws Exception {
		Witness example = example("zero_div.witness-2.4.yml");
		String file = "./zero_div.py";
		assertSameSegments(example, new ViolationWitnessBuilder(task(example), PRODUCER)
				.functionReturn(new Location(file, 17, 8), "\\result == 0")
				.target(new Location(file, 20, 15)));
	}

	/**
	 * Checks that the builders rebuild the Python correctness example
	 * ({@code sum}).
	 */
	@Test
	public void pythonCorrectness()
			throws Exception {
		Witness example = example("sum.witness-2.4.yml");
		assertSameItems(example, new CorrectnessWitnessBuilder(task(example), PRODUCER)
				.loopInvariant(new Location("./sum.py", 11, 5, "average"), "0 <= i and i < len(l)"));
	}

	// ---------------------------------------------------------------- C,
	// violation

	/**
	 * Checks that the builders rebuild the C concurrent violation example
	 * ({@code concurrent-unreach}).
	 */
	@Test
	public void cConcurrentUnreach()
			throws Exception {
		Witness example = example("concurrent-unreach.witness-2.2.yml");
		String file = "./concurrent-unreach.c";
		assertSameSegments(example, new ViolationWitnessBuilder(task(example), PRODUCER)
				.functionEnter(new Location(file, 25, 42, "main"), 0)
				.assumption(new Location(file, 20, 5, "writer"), "x == 1", 1)
				.branching(new Location(file, 26, 5, "main"), true, 0)
				.target(new Location(file, 26, 17, "main"), 0));
	}

	// ---------------------------------------------------------------- C,
	// correctness

	/**
	 * Checks that the builders rebuild the C correctness with contracts example
	 * ({@code multivar_1-1, version 2.1}).
	 */
	@Test
	public void cMultivarWithContracts()
			throws Exception {
		Witness example = example("multivar_1-1.witness-2.1.yml");
		String file = "./multivar_1-1.c";
		assertSameItems(example, new CorrectnessWitnessBuilder(task(example), PRODUCER)
				.loopInvariant(new Location(file, 25, 3, "main"), "( y == x )")
				.contract(new Location(file, 14, 1, "__VERIFIER_assert"), "cond != 0", "1")
				.contract(new Location(file, 21, 1, "main"), "1", "\\return == 0"));
	}

	/**
	 * Checks that the builders rebuild the C correctness with ghost code
	 * example ({@code safe-simple-concurrent}).
	 */
	@Test
	public void cGhostCode()
			throws Exception {
		CorrectnessWitness example = (CorrectnessWitness) example("safe-simple-concurrent.witness-2.1.yml");
		String file = "./safe-simple-concurrent.c";
		CorrectnessWitness built = new CorrectnessWitnessBuilder(task(example), PRODUCER)
				.locationInvariant(new Location(file, 20), "(ghost != 0) || (used == 0)")
				.ghostVariable("ghost", "int", "0")
				.ghostUpdate(new Location(file, 20), "ghost", "1")
				.ghostUpdate(new Location(file, 22), "ghost", "0")
				.ghostUpdate(new Location(file, 9), "ghost", "1")
				.ghostUpdate(new Location(file, 12), "ghost", "0")
				.build();
		assertEquals(example.getInvariants().getItems(), built.getInvariants().getItems());
		assertEquals(example.getGhost().get().getContent(), built.getGhost().get().getContent());
	}
}
