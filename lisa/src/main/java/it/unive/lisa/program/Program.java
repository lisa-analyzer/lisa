package it.unive.lisa.program;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.NativeCFG;
import it.unive.lisa.type.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A program that LiSA can analyze. A program is a {@link Unit} that is defined
 * at an unknown program location, and that has a set of
 * {@link CompilationUnit}s defined in it.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Program extends Unit {

	/**
	 * The name of the program.
	 */
	public static final String PROGRAM_NAME = "~LiSAProgram";

	/**
	 * The compilation units defined in this program, indexed by
	 * {@link CompilationUnit#getName()}.
	 */
	private final Map<String, CompilationUnit> units;

	/**
	 * The collection of types registered in this program. This collection will
	 * be erased during {@link #validateAndFinalize()}.
	 */
	private Collection<Type> types;

	/**
	 * Builds an empty program.
	 */
	public Program() {
		super(null, -1, -1, PROGRAM_NAME);
		units = new ConcurrentHashMap<>();
		types = new ArrayList<>();
	}

	/**
	 * Registers a new {@link Type} that appears in the program.
	 * 
	 * @param type the type to add
	 * 
	 * @return {@code true} if the collection of types changed as a result of
	 *             the call
	 */
	public final boolean registerType(Type type) {
		return types.add(type);
	}

	/**
	 * Yields the collection of {@link Type}s that have been registered in this
	 * program.
	 * 
	 * @return the collection of types
	 */
	public final Collection<Type> getRegisteredTypes() {
		return types;
	}

	/**
	 * Adds a new {@link CompilationUnit}, identified by its name
	 * ({@link CompilationUnit#getName()}), to this program.
	 * 
	 * @param unit the compilation unit to add
	 * 
	 * @return {@code true} if there was no unit previously associated with the
	 *             same name, {@code false} otherwise. If this method returns
	 *             {@code false}, the given unit is discarded.
	 */
	public final boolean addCompilationUnit(CompilationUnit unit) {
		return units.putIfAbsent(unit.getName(), unit) == null;
	}

	/**
	 * Yields the collection of {@link CompilationUnit}s defined in this
	 * program. Each compilation unit is uniquely identified by its name,
	 * meaning that there are no two compilation units having the same name in a
	 * program.
	 * 
	 * @return the collection of compilation units
	 */
	public final Collection<CompilationUnit> getUnits() {
		return units.values();
	}

	/**
	 * Yields the {@link CompilationUnit} defined in this unit having the given
	 * name ({@link CompilationUnit#getName()}), if any.
	 * 
	 * @param name the name of the compilation unit to find
	 * 
	 * @return the compilation unit with the given name, or {@code null}
	 */
	public final CompilationUnit getUnit(String name) {
		return units.get(name);
	}

	/**
	 * {@inheritDoc}<br>
	 * <br>
	 * This method also returns all the cfgs defined in all the
	 * {@link CompilationUnit}s in this program, through
	 * {@link CompilationUnit#getAllCFGs()}.
	 */
	@Override
	public Collection<CFG> getAllCFGs() {
		Collection<CFG> all = super.getAllCFGs();
		units.values().stream().flatMap(u -> u.getAllCFGs().stream()).forEach(all::add);
		return all;
	}

	/**
	 * {@inheritDoc}<br>
	 * <br>
	 * This method also returns all the constructs defined in all the
	 * {@link CompilationUnit}s in this program, through
	 * {@link CompilationUnit#getAllConstructs()}.
	 */
	@Override
	public Collection<NativeCFG> getAllConstructs() {
		Collection<NativeCFG> all = super.getAllConstructs();
		units.values().stream().flatMap(u -> u.getAllConstructs().stream()).forEach(all::add);
		return all;
	}

	/**
	 * {@inheritDoc}<br>
	 * <br>
	 * This method also returns all the globals defined in all the
	 * {@link CompilationUnit}s in this program, through
	 * {@link CompilationUnit#getAllGlobals()}.
	 */
	@Override
	public Collection<Global> getAllGlobals() {
		Collection<Global> all = super.getAllGlobals();
		units.values().stream().flatMap(u -> u.getAllGlobals().stream()).forEach(all::add);
		return all;
	}

	/**
	 * {@inheritDoc} <br>
	 * <br>
	 * Validating a program simply causes the validation of all the
	 * {@link CompilationUnit}s defined inside it. Validation also clears (by
	 * setting it to {@code null}) the set of registered types, in order to
	 * shrink the memory fingerprint of the program.
	 */
	@Override
	public final void validateAndFinalize() throws ProgramValidationException {
		// shrink memory fingerprint
		types = null;

		super.validateAndFinalize();

		for (CompilationUnit unit : getUnits())
			unit.validateAndFinalize();
	}
}
