package it.unive.lisa.analysis;

import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import java.util.function.Predicate;

/**
 * A lattice instance that is produced by {@link SemanticDomain}s or
 * {@link SemanticComponent}s. This is a {@link Lattice} that is aware of
 * identifiers, that is able to forget them, and that can manage scopes.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <L> the type of the lattice
 * @param <T> the type of the abstract element that this lattice produces when
 *                forgetting identifiers or managing scopes
 */
public interface DomainLattice<L extends DomainLattice<L, T>,
		T>
		extends
		Lattice<L>,
		ScopedObject<T> {

	/**
	 * Yields {@code true} if this instance is currently tracking abstract
	 * information for the given identifier.
	 * 
	 * @param id the identifier
	 * 
	 * @return whether or not this lattice knows about {@code id}
	 */
	boolean knowsIdentifier(
			Identifier id);

	/**
	 * Forgets an {@link Identifier}. This means that all information regarding
	 * the given {@code id} will be lost.
	 * 
	 * @param id the identifier to forget
	 * @param pp the program point that where this operation is being evaluated
	 * 
	 * @return the lattice without information about the given id
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	T forgetIdentifier(
			Identifier id,
			ProgramPoint pp)
			throws SemanticException;

	/**
	 * Forgets all {@link Identifier}s that match the given predicate. This
	 * means that all information regarding those identifiers will be lost.
	 * 
	 * @param test the test to identify the targets of the removal
	 * @param pp   the program point that where this operation is being
	 *                 evaluated
	 * 
	 * @return the lattice without information about the ids
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	T forgetIdentifiersIf(
			Predicate<Identifier> test,
			ProgramPoint pp)
			throws SemanticException;

	/**
	 * Forgets all the given {@link Identifier}s. This means that all
	 * information regarding all elements of {@code ids} will be lost.
	 * 
	 * @param ids the collection of identifiers to forget
	 * @param pp  the program point that where this operation is being evaluated
	 * 
	 * @return the lattice without information about the given ids
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	T forgetIdentifiers(
			Iterable<Identifier> ids,
			ProgramPoint pp)
			throws SemanticException;

}
