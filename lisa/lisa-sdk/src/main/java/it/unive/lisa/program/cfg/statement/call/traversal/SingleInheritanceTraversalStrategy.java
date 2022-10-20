package it.unive.lisa.program.cfg.statement.call.traversal;

import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Set;

import it.unive.lisa.program.UnitWithSuperUnits;
import it.unive.lisa.program.cfg.statement.Statement;

/**
 * A {@link HierarcyTraversalStrategy} that assumes a single super unit per each
 * unit (Java-like).
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class SingleInheritanceTraversalStrategy implements HierarcyTraversalStrategy {

	/**
	 * The singleton instance of this class.
	 */
	public static final SingleInheritanceTraversalStrategy INSTANCE = new SingleInheritanceTraversalStrategy();

	private SingleInheritanceTraversalStrategy() {
	}

	@Override
	public Iterable<UnitWithSuperUnits> traverse(Statement st, UnitWithSuperUnits start) {
		return new Iterable<UnitWithSuperUnits>() {

			@Override
			public Iterator<UnitWithSuperUnits> iterator() {
				return new SingleInheritanceIterator(start);
			}
		};
	}

	private class SingleInheritanceIterator implements Iterator<UnitWithSuperUnits> {

		private UnitWithSuperUnits current;

		private final Deque<UnitWithSuperUnits> remaining;

		private final Set<UnitWithSuperUnits> seen;

		private SingleInheritanceIterator(UnitWithSuperUnits start) {
			current = start;
			remaining = new LinkedList<>(start.getSuperUnits());
			remaining.addFirst(start);
			seen = new HashSet<>(remaining);
		}

		@Override
		public boolean hasNext() {
			return !remaining.isEmpty();
		}

		@Override
		public UnitWithSuperUnits next() {
			if (remaining.isEmpty())
				throw new NoSuchElementException();

			UnitWithSuperUnits cu = remaining.pop();

			if (remaining.isEmpty())
				if (current.getSuperUnits().isEmpty())
					current = null;
				else {
					// single inheritance!
					current = current.getSuperUnits().iterator().next();

					// we avoid re-processing what we already seen
					// note that current will have been visited during the
					// previous iteration
					current.getSuperUnits().forEach(su -> {
						if (seen.add(su))
							remaining.add(su);
					});
					remaining.addFirst(current);
				}

			return cu;
		}
	};
}
