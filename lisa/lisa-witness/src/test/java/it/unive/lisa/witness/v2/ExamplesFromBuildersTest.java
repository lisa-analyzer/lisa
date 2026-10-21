package it.unive.lisa.witness.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import it.unive.lisa.witness.v2.correctness.InvariantType;
import it.unive.lisa.witness.v2.metadata.Producer;
import it.unive.lisa.witness.v2.metadata.Task;
import it.unive.lisa.witness.v2.violation.Action;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

// every example of the sv-witnesses repository, rebuilt with the builders
public class ExamplesFromBuildersTest {

	private static final Producer PRODUCER = new Producer("Test", "1");

	// the older examples have the same content in 2.4: only their version is
	// changed
	static Witness example(
			String name)
			throws Exception {
		try (InputStream in = Files.newInputStream(Path.of("witness-testcases", name))) {
			String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			return WitnessReader
					.read(text.replaceAll("format_version: (['\"]?)2\\.[0-3]\\1", "format_version: \"2.4\""));
		}
	}

	private static Task task(
			Witness witness) {
		return witness.getEntries().get(0).getMetadata().getTask();
	}

	private static void assertSameSegments(
			Witness example,
			ViolationWitnessBuilder builder) {
		assertEquals(((ViolationWitness) example).getSequence().getSegments(),
				builder.build().getSequence().getSegments());
	}

	private static void assertSameItems(
			Witness example,
			CorrectnessWitnessBuilder builder) {
		assertEquals(((CorrectnessWitness) example).getInvariants().getItems(),
				builder.build().getInvariants().getItems());
	}

	// ---------------------------------------------------------------- Java

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

	@Test
	public void javaCorrectness()
			throws Exception {
		Witness example = example("java/ExDarko_true.witness-2.4.yml");
		String main = "./java/jpf-regression/ExDarko_true/Main.java";
		assertSameItems(example, new CorrectnessWitnessBuilder(task(example), PRODUCER)
				.locationInvariant(new Location(main, 43, 4, "unboxed"), "i != j"));
	}

	// ---------------------------------------------------------------- Python

	@Test
	public void pythonViolation()
			throws Exception {
		Witness example = example("zero_div.witness-2.4.yml");
		String file = "./zero_div.py";
		assertSameSegments(example, new ViolationWitnessBuilder(task(example), PRODUCER)
				.functionReturn(new Location(file, 17, 8), "\\result == 0")
				.target(new Location(file, 20, 15)));
	}

	@Test
	public void pythonCorrectness()
			throws Exception {
		Witness example = example("sum.witness-2.4.yml");
		assertSameItems(example, new CorrectnessWitnessBuilder(task(example), PRODUCER)
				.loopInvariant(new Location("./sum.py", 11, 5, "average"), "0 <= i and i < len(l)"));
	}

	// ---------------------------------------------------------------- C,
	// violation

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

	@Test
	public void cConcurrentNoOverflow()
			throws Exception {
		Witness example = example("concurrent-no-overflow.witness-2.2.yml");
		String file = "./concurrent-no-overflow.c";
		assertSameSegments(example, new ViolationWitnessBuilder(task(example), PRODUCER)
				.functionEnter(new Location(file, 24, 42, "main"), 0)
				.assumption(new Location(file, 19, 5, "writer"), "x == 2147483647", 1)
				.target(new Location(file, 25, 5, "main"), 0));
	}

	@Test
	public void cConcurrentDataRace()
			throws Exception {
		Witness example = example("concurrent-data-race.witness-2.2.yml");
		String file = "./concurrent-data-race.c";
		assertSameSegments(example, new ViolationWitnessBuilder(task(example), PRODUCER)
				.functionEnter(new Location(file, 27, 44, "main"), 0)
				.functionEnter(new Location(file, 28, 44, "main"), 0)
				.segment(s -> s
						.target(new Location(file, 16, 5, "t1_func"), 1)
						.target(new Location(file, 21, 5, "t2_func"), 2)));
	}

	@Test
	public void cNonTerminating()
			throws Exception {
		Witness example = example("nonterminating-program-example.witness-2.1.yml");
		String file = "nonterminating-program-example.c";
		assertSameSegments(example, new ViolationWitnessBuilder(task(example), PRODUCER)
				.assumption(new Location(file, 15, 5), "(i == -1)")
				.segment(s -> s.branching(Action.CYCLE, new Location(file, 16, 9), true))
				.segment(s -> s.branching(Action.CYCLE, new Location(file, 18, 13), false))
				.segment(s -> s.branching(Action.CYCLE, new Location(file, 15, 5), true))
				.segment(s -> s.branching(Action.CYCLE, new Location(file, 16, 9), false))
				.segment(s -> s.branching(Action.CYCLE, new Location(file, 24, 13), false)));
	}

	@Test
	public void cUnsafeFromCPAchecker()
			throws Exception {
		Witness example = example("unsafe-program-example.cpachecker.witness-2.0.yml");
		String file = "unsafe-program-example.c";
		assertSameSegments(example, new ViolationWitnessBuilder(task(example), PRODUCER)
				.branching(new Location(file, 11, 3), false)
				.assumption(new Location(file, 15, 3, "main"), "v == 0")
				.assumption(new Location(file, 16, 3, "main"), "s == 0")
				.branching(new Location(file, 17, 3, "main"), true)
				.target(new Location(file, 23, 5, "main")));
	}

	// this example uses acsl_expression, deprecated since 2.1: only an explicit
	// format gives it
	@Test
	@SuppressWarnings("deprecation")
	public void cUnsafeFromSymbiotic()
			throws Exception {
		Witness example = example("unsafe-program-example.symbiotic.witness-2.0.yml");
		String file = "/home/paula/Downloads/unsafe-program-example.c";
		assertSameSegments(example, new ViolationWitnessBuilder(task(example), PRODUCER)
				.segment(s -> s.functionReturn(Action.FOLLOW, new Location(file, 10, 45), "\\result == 2",
						Format.ACSL_EXPRESSION))
				.segment(s -> s.functionReturn(Action.FOLLOW, new Location(file, 18, 33), "\\result == 1",
						Format.ACSL_EXPRESSION))
				.segment(s -> s.functionReturn(Action.FOLLOW, new Location(file, 18, 33), "\\result == 255",
						Format.ACSL_EXPRESSION))
				.target(new Location(file, 23, 5)));
	}

	// ---------------------------------------------------------------- C,
	// correctness

	@Test
	public void cSafe()
			throws Exception {
		Witness example = example("safe-program-example.witness-2.0.yml");
		assertSameItems(example, new CorrectnessWitnessBuilder(task(example), PRODUCER)
				.loopInvariant(new Location("./safe-program-example.c", 17, 9, "main"),
						"s <= i*255 && 0 <= i && i <= 255 && n <= 255"));
	}

	@Test
	public void cTerminating()
			throws Exception {
		Witness example = example("terminating-program-example.witness-2.1.yml");
		assertSameItems(example, new CorrectnessWitnessBuilder(task(example), PRODUCER)
				.invariant(InvariantType.LOOP_TRANSITION_INVARIANT,
						new Location("terminating-program-example.c", 15, 5, "main"), "\\at(i, AnyPrev) > i"));
	}

	@Test
	public void cMultivar20()
			throws Exception {
		Witness example = example("multivar_1-1.witness-2.0.yml");
		assertSameItems(example, new CorrectnessWitnessBuilder(task(example), PRODUCER)
				.loopInvariant(new Location("./multivar_1-1.c", 25, 9, "main"), "( y == x )"));
	}

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
