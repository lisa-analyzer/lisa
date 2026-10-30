package it.unive.lisa.cron;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.LiSA;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.string.CharInclusion;
import it.unive.lisa.analysis.string.Prefix;
import it.unive.lisa.analysis.string.Suffix;
import it.unive.lisa.analysis.string.tarsis.Tarsis;
import it.unive.lisa.checks.semantic.SemanticCheck;
import it.unive.lisa.checks.semantic.SemanticTool;
import it.unive.lisa.checks.verification.VerificationOutcome;
import it.unive.lisa.checks.verification.VerificationResult;
import it.unive.lisa.checks.verification.Violation;
import it.unive.lisa.checks.verification.WarningVerifier;
import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.interprocedural.ReturnTopPolicy;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.triage.bmc.BoundedModelChecker;
import it.unive.lisa.triage.model.SymbolicStore;
import it.unive.lisa.triage.pdr.PdrEngine;
import it.unive.lisa.triage.pdr.PdrVerifier;
import it.unive.lisa.triage.reachability.ReachabilityVerifier;
import it.unive.lisa.triage.smt.SmtLibProcessSolver;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

/**
 * The triage benchmark: runs every warning verifier configuration on the
 * alarms of the programs in {@code imp-testcases/triage}, checks that no
 * verdict is unsound, and writes a report ({@code benchmark.md}) and the raw
 * results ({@code results.csv}) to {@code test-outputs/triage}.<br>
 * <br>
 * Each method of the benchmark programs contains one alarm: a call to
 * {@code this.bug()}, checked for reachability. Methods named {@code safe_*}
 * contain unreachable alarms (the right verdict is
 * {@link VerificationOutcome#REFUTED}), methods named {@code bug_*} contain
 * reachable ones (the right verdict is {@link VerificationOutcome#CONFIRMED}).
 * The benchmark is skipped if z3 is not available.<br>
 * <br>
 * By default, the benchmark runs a reduced set of configurations and suites,
 * taking a few minutes. Setting the system property {@value #FULL} to
 * {@code true} runs the full ablation (every generalization strategy with and
 * without the invariants of the analysis, and the CharInclusion string
 * domain), that takes considerably longer.
 */
public class TriageBenchmarkTest {

	private static final String DIR = "imp-testcases/triage/";

	private static final Path OUT = Paths.get("test-outputs", "triage");

	private static final long SOLVER_TIMEOUT = 5_000;

	private static final int BMC_BOUND = 30;

	private static final int PDR_LEVEL = 50;

	private static final int PDR_QUERIES = 3_000;

	private static final long PDR_MILLIS = 5_000;

	/**
	 * The system property enabling the full benchmark.
	 */
	public static final String FULL = "lisa.triage.full";

	private static final boolean full = Boolean.getBoolean(FULL);

	private static SemanticTool<?, ?> captured;

	private static class Capture<A extends AbstractLattice<A>, D extends AbstractDomain<A>>
			implements
			SemanticCheck<A, D> {

		@Override
		public void afterExecution(
				SemanticTool<A, D> tool) {
			captured = tool;
		}

	}

	private static class Row {
		private final String suite, alarm, config, verdict, explanation;

		private final boolean safe;

		private final long millis;

		private final Map<String, Integer> stats;

		private Row(
				String suite,
				String alarm,
				boolean safe,
				String config,
				String verdict,
				String explanation,
				long millis,
				Map<String, Integer> stats) {
			this.suite = suite;
			this.alarm = alarm;
			this.safe = safe;
			this.config = config;
			this.verdict = verdict;
			this.explanation = explanation;
			this.millis = millis;
			this.stats = stats;
		}

		private boolean correct() {
			return safe ? verdict.equals("REFUTED") : verdict.equals("CONFIRMED");
		}

		private boolean unsound() {
			return safe ? verdict.equals("CONFIRMED") : verdict.equals("REFUTED");
		}
	}

	private static Map<String, Supplier<WarningVerifier<?, ?>>> configurations() {
		Supplier<it.unive.lisa.triage.smt.SmtSolver> z3 = () -> SmtLibProcessSolver.z3(SOLVER_TIMEOUT);
		Map<String, Supplier<WarningVerifier<?, ?>>> result = new LinkedHashMap<>();
		result.put("reach", ReachabilityVerifier::new);
		result.put("bmc", () -> new BoundedModelChecker<>(BMC_BOUND, z3, SymbolicStore.DEFAULT_MAX_CASES));
		for (boolean seeding : new boolean[] { true, false })
			for (PdrEngine.Generalization g : PdrEngine.Generalization.values())
				// without the full ablation, only literal generalization is
				// run without the invariants of the analysis
				if (full || seeding || g == PdrEngine.Generalization.LITERALS)
					result.put("pdr-" + g.name().toLowerCase() + (seeding ? "" : "-noseed"),
							() -> new PdrVerifier<>(PDR_LEVEL, PDR_QUERIES, PDR_MILLIS, z3,
									SymbolicStore.DEFAULT_MAX_CASES).withGeneralization(g).withSeeding(seeding));
		return result;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static List<Row> run(
			String suite,
			String file,
			DefaultConfiguration conf)
			throws Exception {
		Program program = IMPFrontend.processFile(DIR + file);
		for (CFG cfg : program.getAllCFGs())
			if (!program.getEntryPoints().contains(cfg))
				program.addEntryPoint(cfg);
		conf.openCallPolicy = ReturnTopPolicy.INSTANCE;
		conf.workdir = Files.createTempDirectory("triage").toString();
		conf.semanticChecks.add(new Capture<>());
		captured = null;
		new LiSA(conf).run(program);
		assertNotNull(captured);
		SemanticTool tool = captured;

		List<Row> rows = new ArrayList<>();
		for (CFG cfg : program.getAllCFGs()) {
			String name = cfg.getDescriptor().getName();
			boolean safe = name.startsWith("safe_");
			if (!safe && !name.startsWith("bug_"))
				continue;
			Statement alarm = null;
			for (Statement st : cfg.getNodes())
				if (st instanceof Call && ((Call) st).getTargetName().equals("bug"))
					alarm = st;
			assertNotNull(alarm, "No alarm in " + name);
			AnalyzedCFG result = (AnalyzedCFG) tool.getResultOf(cfg).iterator().next();
			for (Map.Entry<String, Supplier<WarningVerifier<?, ?>>> c : configurations().entrySet()) {
				WarningVerifier verifier = c.getValue().get();
				verifier.init(tool);
				long start = System.nanoTime();
				String verdict, explanation;
				try {
					VerificationResult res = verifier.verify(result, alarm, Violation.reach());
					verdict = res.getOutcome().name();
					explanation = res.getExplanation();
				} catch (Exception e) {
					verdict = "ERROR";
					explanation = e.toString();
				}
				long millis = (System.nanoTime() - start) / 1_000_000;
				Map<String, Integer> stats = verifier instanceof PdrVerifier
						? ((PdrVerifier) verifier).getLastStatistics()
						: Map.of();
				rows.add(new Row(suite, name, safe, c.getKey(), verdict, explanation, millis, stats));
			}
		}
		return rows;
	}

	@Test
	public void benchmark() throws Exception {
		assumeTrue(SmtLibProcessSolver.isZ3Available(), "z3 is not available");
		List<Row> rows = new ArrayList<>();

		DefaultConfiguration intervals = new DefaultConfiguration();
		intervals.analysis = DefaultConfiguration.defaultAbstractDomain();
		rows.addAll(run("numeric", "numeric.imp", intervals));

		DefaultConfiguration prefix = new DefaultConfiguration();
		prefix.analysis = DefaultConfiguration.simpleDomain(DefaultConfiguration.defaultHeapDomain(), new Prefix(),
				DefaultConfiguration.defaultTypeDomain());
		rows.addAll(run("strings-prefix", "strings.imp", prefix));

		DefaultConfiguration tarsis = new DefaultConfiguration();
		tarsis.analysis = DefaultConfiguration.simpleDomain(DefaultConfiguration.defaultHeapDomain(), new Tarsis(),
				DefaultConfiguration.defaultTypeDomain());
		rows.addAll(run("strings-tarsis", "strings.imp", tarsis));

		DefaultConfiguration suffix = new DefaultConfiguration();
		suffix.analysis = DefaultConfiguration.simpleDomain(DefaultConfiguration.defaultHeapDomain(), new Suffix(),
				DefaultConfiguration.defaultTypeDomain());
		rows.addAll(run("strings-suffix", "strings.imp", suffix));

		if (full) {
			// the most expensive suite, only run in the full benchmark
			DefaultConfiguration chars = new DefaultConfiguration();
			chars.analysis = DefaultConfiguration.simpleDomain(DefaultConfiguration.defaultHeapDomain(),
					new CharInclusion(), DefaultConfiguration.defaultTypeDomain());
			rows.addAll(run("strings-charinclusion", "strings.imp", chars));
		}

		DefaultConfiguration exceptions = new DefaultConfiguration();
		exceptions.analysis = DefaultConfiguration.defaultAbstractDomain();
		rows.addAll(run("exceptions", "exceptions.imp", exceptions));

		write(rows);

		// verdicts that are unsound because the fixpoint itself is (that is,
		// reachability is already unsound) are issues of the analysis, that
		// triage cannot fix: they are reported, but not considered failures
		List<String> unsound = new ArrayList<>();
		for (Row r : rows)
			if ((r.unsound() && !analysisIssue(rows, r)) || r.verdict.equals("ERROR"))
				unsound.add(r.suite + "/" + r.alarm + " with " + r.config + ": " + r.verdict + " (" + r.explanation
						+ ")");
		assertTrue(unsound.isEmpty(), "Unsound or failed verdicts:\n" + String.join("\n", unsound));
	}

	private static boolean analysisIssue(
			List<Row> rows,
			Row row) {
		for (Row r : rows)
			if (r.suite.equals(row.suite) && r.alarm.equals(row.alarm) && r.config.equals("reach"))
				return r.unsound();
		return false;
	}

	private static void write(
			List<Row> rows)
			throws IOException {
		Files.createDirectories(OUT);
		List<String> configs = new ArrayList<>(configurations().keySet());

		StringBuilder csv = new StringBuilder(
				"suite,alarm,expected,config,verdict,correct,millis,queries,level,proposals,accepted,explanation\n");
		for (Row r : rows)
			csv.append(String.join(",", r.suite, r.alarm, r.safe ? "REFUTED" : "CONFIRMED", r.config, r.verdict,
					String.valueOf(r.correct()), String.valueOf(r.millis),
					String.valueOf(r.stats.getOrDefault("queries", 0)),
					String.valueOf(r.stats.getOrDefault("level", 0)),
					String.valueOf(r.stats.getOrDefault("proposals", 0)),
					String.valueOf(r.stats.getOrDefault("accepted", 0)),
					"\"" + String.valueOf(r.explanation).replace("\"", "'").replace("\n", " ") + "\"")).append("\n");
		Files.writeString(OUT.resolve("results.csv"), csv);

		StringBuilder md = new StringBuilder("# Triage benchmark\n\n");
		md.append("Verdicts: R = REFUTED, C = CONFIRMED, P = POSSIBLE; a `*` marks the right verdict. ")
				.append("PDR cells also report the number of queries.\n\n## Summary\n\n");
		md.append("| config | right | possible | unsound | time (ms) | queries |\n|---|---|---|---|---|---|\n");
		for (String c : configs) {
			int right = 0, possible = 0, unsound = 0, queries = 0;
			long time = 0;
			for (Row r : rows)
				if (r.config.equals(c)) {
					right += r.correct() ? 1 : 0;
					possible += r.verdict.equals("POSSIBLE") ? 1 : 0;
					unsound += r.unsound() ? 1 : 0;
					time += r.millis;
					queries += r.stats.getOrDefault("queries", 0);
				}
			md.append("| ").append(c).append(" | ").append(right).append(" | ").append(possible).append(" | ")
					.append(unsound).append(" | ").append(time).append(" | ").append(queries).append(" |\n");
		}

		md.append("\n## Unsound verdicts of the analysis\n\n")
				.append("Alarms where the fixpoint itself (reachability) gives an unsound verdict: ")
				.append("these are issues of the analysis, not of triage.\n\n");
		for (Row r : rows)
			if (r.config.equals("reach") && r.unsound())
				md.append("- ").append(r.suite).append("/").append(r.alarm).append(": ").append(r.verdict)
						.append("\n");

		md.append("\n## Per alarm\n\n| suite | alarm |");
		for (String c : configs)
			md.append(" ").append(c).append(" |");
		md.append("\n|---|---|");
		for (int i = 0; i < configs.size(); i++)
			md.append("---|");
		md.append("\n");
		Map<String, Map<String, Row>> byAlarm = new LinkedHashMap<>();
		for (Row r : rows)
			byAlarm.computeIfAbsent(r.suite + "|" + r.alarm, k -> new LinkedHashMap<>()).put(r.config, r);
		for (Map<String, Row> alarm : byAlarm.values()) {
			Row first = alarm.values().iterator().next();
			md.append("| ").append(first.suite).append(" | ").append(first.alarm).append(" |");
			for (String c : configs) {
				Row r = alarm.get(c);
				md.append(" ").append(r.verdict.charAt(0)).append(r.correct() ? "*" : "");
				if (r.stats.containsKey("queries"))
					md.append(" ").append(r.stats.get("queries"));
				md.append(" |");
			}
			md.append("\n");
		}
		Files.writeString(OUT.resolve("benchmark.md"), md);
	}
}
