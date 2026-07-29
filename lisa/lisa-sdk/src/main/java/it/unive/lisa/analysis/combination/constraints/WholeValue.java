package it.unive.lisa.analysis.combination.constraints;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.value.ValueLattice;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.util.representation.ListRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

/**
 * A lattice element for a {@link WholeValueAnalysis} that is composed of a
 * lattice instance for each component taking part in the analysis. Components
 * are ordered, and each lattice operator is applied to the elements at the same
 * index withouth checking if they are of the same type. No two instances of
 * this class should be created with a different number or arrangement of
 * components and used together in lattice operations, as this would lead to
 * errors.
 * 
 * @author <a href="mailto:luca.negrini@unive.it>">Luca Negrini</a>
 */
public class WholeValue
		implements
		BaseLattice<WholeValue>,
		ValueLattice<WholeValue> {

	private final ValueLattice<?>[] components;

	/**
	 * Builds a value with the given components.
	 * 
	 * @param components the components of this value
	 */
	public WholeValue(
			ValueLattice<?>... components) {
		this.components = components;
	}

	/**
	 * Returns the components of this value.
	 * 
	 * @return the components of this value
	 */
	public ValueLattice<?>[] getComponents() {
		return components;
	}

	/**
	 * Returns the component at the given index.
	 *
	 * @param i the index of the component to return
	 * 
	 * @return the component at the given index
	 */
	public ValueLattice<?> get(
			int i) {
		return components[i];
	}

	/**
	 * Returns the component at the given index, cast to the given type. If the
	 * component at the given index is not of the given type, an exception is
	 * thrown.
	 *
	 * @param <T>   the type of the component to return
	 * @param i     the index of the component to return
	 * @param clazz the class of the component to return
	 * 
	 * @return the component at the given index, cast to the given type
	 * 
	 * @throws SemanticException if the component at the given index is not of
	 *                               the given type
	 */
	@SuppressWarnings("unchecked")
	public <T extends ValueLattice<T>> T get(
			int i,
			Class<T> clazz)
			throws SemanticException {
		try {
			return (T) components[i];
		} catch (ClassCastException e) {
			throw new SemanticException("Component at index " + i + " is not of type " + clazz.getName());
		}
	}

	/**
	 * Returns the first component of the given type. If multiple components of
	 * the same type are present, only the first one is returned. If no
	 * component of the given type is present, an exception is thrown.
	 *
	 * @param <T>   the type of the component to return
	 * @param clazz the class of the component to return
	 * 
	 * @return the first component of the given type
	 * 
	 * @throws SemanticException if no component of the given type is present
	 */
	public <T extends ValueLattice<T>> T get(
			Class<T> clazz)
			throws SemanticException {
		for (ValueLattice<?> c : components)
			if (clazz.isInstance(c))
				return clazz.cast(c);
		throw new SemanticException("No component of type " + clazz.getName() + " found");
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(components);
		return result;
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
		WholeValue other = (WholeValue) obj;
		if (!Arrays.equals(components, other.components))
			return false;
		return true;
	}

	@Override
	public WholeValue top() {
		if (isTop())
			return this;
		ValueLattice<?>[] top = new ValueLattice<?>[components.length];
		for (int i = 0; i < components.length; i++)
			top[i] = components[i].top();
		return new WholeValue(top);
	}

	@Override
	public boolean isTop() {
		for (ValueLattice<?> c : components)
			if (!c.isTop())
				return false;
		return true;
	}

	@Override
	public WholeValue bottom() {
		if (isBottom())
			return this;
		ValueLattice<?>[] bottom = new ValueLattice<?>[components.length];
		for (int i = 0; i < components.length; i++)
			bottom[i] = components[i].bottom();
		return new WholeValue(bottom);
	}

	@Override
	public boolean isBottom() {
		for (ValueLattice<?> c : components)
			if (!c.isBottom())
				return false;
		return true;
	}

	@Override
	public StructuredRepresentation representation() {
		List<StructuredRepresentation> res = new ArrayList<>(components.length);
		for (ValueLattice<?> c : components)
			res.add(c.representation());
		return new ListRepresentation(res);
	}

	@Override
	public WholeValue store(
			Identifier target,
			Identifier source)
			throws SemanticException {
		ValueLattice<?>[] bottom = new ValueLattice<?>[components.length];
		for (int i = 0; i < components.length; i++)
			bottom[i] = components[i].bottom();
		return new WholeValue(bottom);
	}

	@Override
	public boolean knowsIdentifier(
			Identifier id) {
		for (ValueLattice<?> c : components)
			if (c.knowsIdentifier(id))
				return true;
		return false;
	}

	@Override
	public WholeValue forgetIdentifier(
			Identifier id,
			ProgramPoint pp)
			throws SemanticException {
		ValueLattice<?>[] forgotten = new ValueLattice<?>[components.length];
		for (int i = 0; i < components.length; i++)
			forgotten[i] = components[i].forgetIdentifier(id, pp);
		return new WholeValue(forgotten);
	}

	@Override
	public WholeValue forgetIdentifiersIf(
			Predicate<Identifier> test,
			ProgramPoint pp)
			throws SemanticException {
		ValueLattice<?>[] forgotten = new ValueLattice<?>[components.length];
		for (int i = 0; i < components.length; i++)
			forgotten[i] = components[i].forgetIdentifiersIf(test, pp);
		return new WholeValue(forgotten);
	}

	@Override
	public WholeValue forgetIdentifiers(
			Iterable<Identifier> ids,
			ProgramPoint pp)
			throws SemanticException {
		ValueLattice<?>[] forgotten = new ValueLattice<?>[components.length];
		for (int i = 0; i < components.length; i++)
			forgotten[i] = components[i].forgetIdentifiers(ids, pp);
		return new WholeValue(forgotten);
	}

	@Override
	public WholeValue pushScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		ValueLattice<?>[] pushed = new ValueLattice<?>[components.length];
		for (int i = 0; i < components.length; i++)
			pushed[i] = components[i].pushScope(token, pp);
		return new WholeValue(pushed);
	}

	@Override
	public WholeValue popScope(
			ScopeToken token,
			ProgramPoint pp)
			throws SemanticException {
		ValueLattice<?>[] popped = new ValueLattice<?>[components.length];
		for (int i = 0; i < components.length; i++)
			popped[i] = components[i].popScope(token, pp);
		return new WholeValue(popped);
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public boolean lessOrEqualAux(
			WholeValue other)
			throws SemanticException {
		if (components.length != other.components.length)
			throw new SemanticException("Cannot operate on domains with a different number of components");
		for (int i = 0; i < components.length; i++)
			if (!((ValueLattice) components[i]).lessOrEqual(other.components[i]))
				return false;
		return true;
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public WholeValue lubAux(
			WholeValue other)
			throws SemanticException {
		if (components.length != other.components.length)
			throw new SemanticException("Cannot operate on domains with a different number of components");
		ValueLattice<?>[] lub = new ValueLattice<?>[components.length];
		for (int i = 0; i < components.length; i++)
			lub[i] = (ValueLattice) ((ValueLattice) components[i]).lub(other.components[i]);
		return new WholeValue(lub);
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public WholeValue glbAux(
			WholeValue other)
			throws SemanticException {
		if (components.length != other.components.length)
			throw new SemanticException("Cannot operate on domains with a different number of components");
		ValueLattice<?>[] lub = new ValueLattice<?>[components.length];
		for (int i = 0; i < components.length; i++)
			lub[i] = (ValueLattice) ((ValueLattice) components[i]).glb(other.components[i]);
		return new WholeValue(lub);
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public WholeValue upchainAux(
			WholeValue other)
			throws SemanticException {
		if (components.length != other.components.length)
			throw new SemanticException("Cannot operate on domains with a different number of components");
		ValueLattice<?>[] lub = new ValueLattice<?>[components.length];
		for (int i = 0; i < components.length; i++)
			lub[i] = (ValueLattice) ((ValueLattice) components[i]).upchain(other.components[i]);
		return new WholeValue(lub);
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public WholeValue downchainAux(
			WholeValue other)
			throws SemanticException {
		if (components.length != other.components.length)
			throw new SemanticException("Cannot operate on domains with a different number of components");
		ValueLattice<?>[] lub = new ValueLattice<?>[components.length];
		for (int i = 0; i < components.length; i++)
			lub[i] = (ValueLattice) ((ValueLattice) components[i]).downchain(other.components[i]);
		return new WholeValue(lub);
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public WholeValue wideningAux(
			WholeValue other)
			throws SemanticException {
		if (components.length != other.components.length)
			throw new SemanticException("Cannot operate on domains with a different number of components");
		ValueLattice<?>[] lub = new ValueLattice<?>[components.length];
		for (int i = 0; i < components.length; i++)
			lub[i] = (ValueLattice) ((ValueLattice) components[i]).widening(other.components[i]);
		return new WholeValue(lub);
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public WholeValue narrowingAux(
			WholeValue other)
			throws SemanticException {
		if (components.length != other.components.length)
			throw new SemanticException("Cannot operate on domains with a different number of components");
		ValueLattice<?>[] lub = new ValueLattice<?>[components.length];
		for (int i = 0; i < components.length; i++)
			lub[i] = (ValueLattice) ((ValueLattice) components[i]).narrowing(other.components[i]);
		return new WholeValue(lub);
	}

	@Override
	public <D extends Lattice<D>> Collection<D> getAllLatticeInstances(
			Class<D> domain) {
		Collection<D> result = BaseLattice.super.getAllLatticeInstances(domain);
		for (ValueLattice<?> c : components)
			result.addAll(c.getAllLatticeInstances(domain));
		return result;
	}

}
