package it.unive.lisa.analysis.impl.types;

import it.unive.lisa.analysis.SetLattice;
import it.unive.lisa.caches.Caches;
import it.unive.lisa.cfg.statement.Expression;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.collections.ExternalSet;
import java.util.Set;

/**
 * A {@link SetLattice} holding a set of {@link Type}s, representing the
 * inferred runtime types of an {@link Expression}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class InferredTypes extends SetLattice<InferredTypes, Type> {

	private static final InferredTypes TOP = new InferredTypes();

	private static final InferredTypes BOTTOM = new InferredTypes(Caches.types().mkEmptySet(), false, true);

	private final boolean isTop, isBottom;

	/**
	 * Builds the inferred types. The object built through this constructor
	 * represents the top of the lattice.
	 */
	public InferredTypes() {
		this(Caches.types().mkEmptySet(), true, false);
	}

	/**
	 * Builds the inferred types that is neither top nor bottom.
	 * 
	 * @param types the set of types contained into this instance
	 */
	InferredTypes(ExternalSet<Type> types) {
		this(types, false, false);
	}

	private InferredTypes(ExternalSet<Type> types, boolean isTop, boolean isBottom) {
		super(types);
		this.isTop = isTop;
		this.isBottom = isBottom;
	}

	/**
	 * Yields the {@link ExternalSet} containing the types held by this
	 * instance.
	 * 
	 * @return the set of types inside this instance
	 */
	public ExternalSet<Type> getRuntimeTypes() {
		return (ExternalSet<Type>) elements;
	}

	@Override
	public InferredTypes top() {
		return TOP;
	}

	@Override
	public boolean isTop() {
		return isTop;
	}

	@Override
	public InferredTypes bottom() {
		return BOTTOM;
	}

	@Override
	public boolean isBottom() {
		return isBottom;
	}

	@Override
	protected InferredTypes mk(Set<Type> lub) {
		return new InferredTypes(Caches.types().mkSet(lub), false, false);
	}
}
