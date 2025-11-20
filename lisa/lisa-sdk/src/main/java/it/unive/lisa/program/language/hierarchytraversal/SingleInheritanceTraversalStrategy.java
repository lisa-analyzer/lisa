package it.unive.lisa.program.language.hierarchytraversal;

import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.cfg.statement.Statement;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * A {@link HierarchyTraversalStrategy} that assumes a single super unit per
 * each unit (Java-like).
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class SingleInheritanceTraversalStrategy
		implements
		HierarchyTraversalStrategy {

	/**
	 * The singleton instance of this class.
	 */
	public static final SingleInheritanceTraversalStrategy INSTANCE = new SingleInheritanceTraversalStrategy();

	private SingleInheritanceTraversalStrategy() {
	}

	@Override
	public Iterable<CompilationUnit> traverse(
			Statement st,
			CompilationUnit start) {
		return new Iterable<CompilationUnit>() {

			@Override
			public Iterator<CompilationUnit> iterator() {
				return new SingleInheritanceIterator(start);
			}

		};
	}

	private class SingleInheritanceIterator
			implements
			Iterator<CompilationUnit> {

		private CompilationUnit current;

		private final Deque<CompilationUnit> remaining;

		private final Set<CompilationUnit> seen;

		private SingleInheritanceIterator(
				CompilationUnit start) {
			current = start;
			remaining = new LinkedList<>(start.getImmediateAncestors());
			remaining.addFirst(start);
			seen = new HashSet<>(remaining);
		}

		@Override
		public boolean hasNext() {
			return !remaining.isEmpty();
		}

		@Override
		public CompilationUnit next() {
			if (remaining.isEmpty())
				throw new NoSuchElementException();

			CompilationUnit cu = remaining.pop();

			if (remaining.isEmpty())
				if (current.getImmediateAncestors().isEmpty())
					current = null;
				else {
					// single inheritance!
					current = current.getImmediateAncestors().iterator().next();

					// we avoid re-processing what we already seen
					// note that current will have been visited during the
					// previous iteration
					current.getImmediateAncestors().forEach(su -> {
						if (seen.add(su))
							remaining.add(su);
					});
					remaining.addFirst(current);
				}

			return cu;
		}

	};

}
