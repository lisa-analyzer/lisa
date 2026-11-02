package it.unive.lisa.triage.smt;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * An {@link SmtSolver} that does not solve anything, but records the SMT-LIB 2
 * script corresponding to the commands it receives. It is useful to inspect
 * (or to solve offline) the queries built for a solver. Requests that need an
 * actual solver ({@link #check()}, {@link #getValues(Collection)},
 * {@link #getUnsatCore()}, {@link #getReasonUnknown()}) are recorded as
 * commands, and yield {@link SmtResult#UNKNOWN} or empty results.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class SmtLibScript
		implements
		SmtSolver {

	private final StringBuilder script = new StringBuilder();

	/**
	 * Builds the script, with the options needed by the recorded commands.
	 */
	public SmtLibScript() {
		command("(set-option :produce-models true)");
		command("(set-option :produce-unsat-cores true)");
		command("(set-option :global-declarations true)");
		command("(set-logic ALL)");
	}

	private void command(
			String cmd) {
		script.append(cmd).append("\n");
	}

	/**
	 * Yields the recorded script.
	 *
	 * @return the script
	 */
	public String getScript() {
		return script.toString();
	}

	@Override
	public Sort declareSort(
			String name) {
		Sort sort = Sort.uninterpreted(name);
		command("(declare-sort " + sort.toSmtLib() + " 0)");
		return sort;
	}

	@Override
	public Sort declareDatatype(
			Datatype datatype) {
		command(datatype.toSmtLibDeclaration());
		return datatype.getSort();
	}

	@Override
	public Term declare(
			String name,
			Sort sort) {
		Term constant = Term.constant(name, sort);
		command("(declare-const " + constant.toSmtLib() + " " + sort.toSmtLib() + ")");
		return constant;
	}
	@Override
	public String declareFunction(
			String name,
			List<Sort> arguments,
			Sort result) {
		String symbol = Term.symbol(name);
		StringBuilder sb = new StringBuilder("(declare-fun ").append(symbol).append(" (");
		for (int k = 0; k < arguments.size(); k++)
			sb.append(k == 0 ? "" : " ").append(arguments.get(k).toSmtLib());
		sb.append(") ").append(result.toSmtLib()).append(")");
		command(sb.toString());
		return symbol;
	}


	@Override
	public void assertTerm(
			Term term) {
		command("(assert " + term.toSmtLib() + ")");
	}

	@Override
	public void assertNamed(
			String name,
			Term term) {
		command("(assert (! " + term.toSmtLib() + " :named " + Term.symbol(name) + "))");
	}

	@Override
	public void push() {
		command("(push 1)");
	}

	@Override
	public void pop() {
		command("(pop 1)");
	}

	@Override
	public SmtResult check() {
		command("(check-sat)");
		return SmtResult.UNKNOWN;
	}

	@Override
	public Map<Term, String> getValues(
			Collection<Term> terms) {
		return Map.of();
	}

	@Override
	public List<String> getUnsatCore() {
		return List.of();
	}

	@Override
	public String getReasonUnknown() {
		return "script only";
	}

	@Override
	public void close() {
	}

	@Override
	public String toString() {
		return getScript();
	}
}
