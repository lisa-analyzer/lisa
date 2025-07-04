package it.unive.lisa.analysis.heap.pointbased;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.heap.HeapEnvironment;
import it.unive.lisa.symbolic.value.Identifier;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * A field-insensitive program point-based {@link AllocationSiteBasedAnalysis}.
 * The implementation follows X. Rival and K. Yi, "Introduction to Static
 * Analysis An Abstract Interpretation Perspective", Section 8.3.4
 * 
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 * 
 * @see <a href=
 *          "https://mitpress.mit.edu/books/introduction-static-analysis">https://mitpress.mit.edu/books/introduction-static-analysis</a>
 */
public class PointBasedHeap extends AllocationSiteBasedAnalysis<PointBasedHeap> {

	/**
	 * Builds a new instance of field-insensitive point-based heap.
	 */
	public PointBasedHeap() {
		this(new HeapEnvironment<>(new AllocationSites()));
	}

	/**
	 * Builds a new instance of field-insensitive point-based heap from its heap
	 * environment.
	 * 
	 * @param heapEnv the heap environment that this instance tracks
	 */
	public PointBasedHeap(
			HeapEnvironment<AllocationSites> heapEnv) {
		this(heapEnv, Collections.emptyList());
	}

	/**
	 * Builds a new instance of field-insensitive point-based heap from its heap
	 * environment.
	 * 
	 * @param heapEnv      the heap environment that this instance tracks
	 * @param replacements the heap replacements of this instance
	 */
	public PointBasedHeap(
			HeapEnvironment<AllocationSites> heapEnv,
			List<HeapReplacement> replacements) {
		super(heapEnv, replacements.isEmpty() ? Collections.emptyList() : replacements);
	}

	@Override
	public PointBasedHeap mk(
			PointBasedHeap reference) {
		return reference;
	}

	@Override
	public PointBasedHeap mk(
			PointBasedHeap reference,
			List<HeapReplacement> replacements) {
		return new PointBasedHeap(reference.heapEnv, replacements);
	}

	@Override
	protected PointBasedHeap mk(
			PointBasedHeap reference,
			HeapEnvironment<AllocationSites> heapEnv) {
		return new PointBasedHeap(heapEnv, reference.replacements);
	}

	@Override
	public PointBasedHeap top() {
		return mk(new PointBasedHeap(heapEnv.top()));
	}

	@Override
	public PointBasedHeap bottom() {
		return mk(new PointBasedHeap(heapEnv.bottom()));
	}

	@Override
	public List<HeapReplacement> getSubstitution() {
		return replacements;
	}

	@Override
	public PointBasedHeap lubAux(
			PointBasedHeap other)
			throws SemanticException {
		return mk(new PointBasedHeap(heapEnv.lub(other.heapEnv)));
	}

	@Override
	public PointBasedHeap glbAux(
			PointBasedHeap other)
			throws SemanticException {
		return mk(new PointBasedHeap(heapEnv.glb(other.heapEnv)));
	}

	@Override
	public boolean lessOrEqualAux(
			PointBasedHeap other)
			throws SemanticException {
		return heapEnv.lessOrEqual(other.heapEnv);
	}

	@Override
	public PointBasedHeap popScope(
			ScopeToken scope)
			throws SemanticException {
		return mk(new PointBasedHeap(heapEnv.popScope(scope)));
	}

	@Override
	public PointBasedHeap pushScope(
			ScopeToken scope)
			throws SemanticException {
		return mk(new PointBasedHeap(heapEnv.pushScope(scope)));
	}

	@Override
	public PointBasedHeap forgetIdentifier(
			Identifier id)
			throws SemanticException {
		return mk(new PointBasedHeap(heapEnv.forgetIdentifier(id)));
	}

	@Override
	public PointBasedHeap forgetIdentifiers(Iterable<Identifier> ids) throws SemanticException {
		return mk(new PointBasedHeap(heapEnv.forgetIdentifiers(ids)));
	}

	@Override
	public PointBasedHeap forgetIdentifiersIf(
			Predicate<Identifier> test)
			throws SemanticException {
		return mk(new PointBasedHeap(heapEnv.forgetIdentifiersIf(test)));
	}
}
