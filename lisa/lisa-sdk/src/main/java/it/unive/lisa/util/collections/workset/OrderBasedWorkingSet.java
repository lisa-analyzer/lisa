package it.unive.lisa.util.collections.workset;

import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.Statement;
import java.util.Collection;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A {@link WorkingSet} for {@link Statement}s that sorts its contents according
 * to their natural order. This is specifically designed for fixpoint algorithms
 * of CFGs: since the natural order of Statements discriminates for their
 * {@link CodeLocation} first, this allows instructions that are exit points of
 * control-flow structures to be analyzed only when all branches of the
 * preceding structure has been fully analyzed. This holds since, unless several
 * GOTO-like instructions are present, contents of ifs and loops always appear
 * earlier in the code w.r.t. the exit points.<br>
 * <br>
 * Note that this working set is backed by a set: it is thus impossible to have
 * duplicates in it.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class OrderBasedWorkingSet
		implements
		WorkingSet<Statement> {

	private final SortedSet<Statement> ws;

	private OrderBasedWorkingSet() {
		ws = new TreeSet<>();
	}

	/**
	 * Yields a new, empty working set.
	 * 
	 * @return the new working set
	 */
	public static OrderBasedWorkingSet mk() {
		return new OrderBasedWorkingSet();
	}

	@Override
	public void push(
			Statement e) {
		ws.add(e);
	}

	@Override
	public Statement pop() {
		Statement first = ws.first();
		ws.remove(first);
		return first;
	}

	@Override
	public Statement peek() {
		return ws.first();
	}

	@Override
	public int size() {
		return ws.size();
	}

	@Override
	public boolean isEmpty() {
		return ws.isEmpty();
	}

	@Override
	public Collection<Statement> getContents() {
		return ws;
	}

	@Override
	public String toString() {
		return ws.toString();
	}

	@Override
	public int hashCode() {
		return Objects.hash(ws);
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OrderBasedWorkingSet other = (OrderBasedWorkingSet) obj;
		return Objects.equals(ws, other.ws);
	}

}
