package it.unive.lisa.analysis.impl.types;

import java.util.Set;

import it.unive.lisa.analysis.SetLattice;
import it.unive.lisa.caches.Caches;
import it.unive.lisa.cfg.type.Type;
import it.unive.lisa.util.collections.ExternalSet;

public class InferredTypes extends SetLattice<InferredTypes, Type> {

	private static final InferredTypes TOP = new InferredTypes();

	private static final InferredTypes BOTTOM = new InferredTypes(Caches.types().mkEmptySet(), false, true);

	private final boolean isTop, isBottom;

	public InferredTypes() {
		this(Caches.types().mkEmptySet(), true, false);
	}
	
	InferredTypes(ExternalSet<Type> types) {
		this(types, false, false);
	}

	private InferredTypes(ExternalSet<Type> types, boolean isTop, boolean isBottom) {
		super(types);
		this.isTop = isTop;
		this.isBottom = isBottom;
	}

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
