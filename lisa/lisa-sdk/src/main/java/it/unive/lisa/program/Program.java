package it.unive.lisa.program;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.language.LanguageFeatures;
import it.unive.lisa.type.TypeSystem;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * A program that LiSA can analyze. A program is a {@link Unit} that is defined
 * at an unknown program location, and that has a set of {@link ClassUnit}s
 * defined in it.
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
	 * {@link ClassUnit#getName()}.
	 */
	private final Map<String, Unit> units;

	/**
	 * The entry points defined in this program.
	 */
	private final Collection<CFG> entrypoints;

	/**
	 * The language-specific features, algorithms and semantics of this program
	 */
	private final LanguageFeatures features;

	/**
	 * The type system knowing about the types that appear in the program
	 */
	private final TypeSystem types;

	/**
	 * Builds an empty program.
	 * 
	 * @param features the language-specific features, algorithms and semantics
	 *                     of this program
	 * @param types    the type system knowing about the types that appear in
	 *                     the program
	 */
	public Program(LanguageFeatures features, TypeSystem types) {
		super(PROGRAM_NAME);
		this.features = features;
		this.types = types;
		units = new TreeMap<>();
		entrypoints = new LinkedList<>();
	}

	/**
	 * Yields the language-specific features, algorithms and semantics of this
	 * program.
	 * 
	 * @return the features
	 */
	public LanguageFeatures getFeatures() {
		return features;
	}

	/**
	 * Yields the type system knowing about the types that appear in the
	 * program.
	 * 
	 * @return the type system
	 */
	public TypeSystem getTypes() {
		return types;
	}

	/**
	 * Adds a new {@link ClassUnit}, identified by its name
	 * ({@link ClassUnit#getName()}), to this program.
	 * 
	 * @param unit the compilation unit to add
	 * 
	 * @return {@code true} if there was no unit previously associated with the
	 *             same name, {@code false} otherwise. If this method returns
	 *             {@code false}, the given unit is discarded.
	 * 
	 * @throws IllegalArgumentException if the given unit is an instance of this
	 *                                      class
	 */
	public final boolean addUnit(Unit unit) {
		if (unit instanceof Program)
			throw new IllegalArgumentException("Cannot add a program to another one");
		return units.putIfAbsent(unit.getName(), unit) == null;
	}

	/**
	 * Adds a new {@link CFG} to the entry points of this program.
	 *
	 * @param cm the cfg to add
	 *
	 * @return {@code true} if the entry point was successfully added. If this
	 *             method returns {@code false}, the given cfg is discarded.
	 */
	public final boolean addEntryPoint(CFG cm) {
		return entrypoints.add(cm);
	}

	/**
	 * Yields the collection of {@link CFG}s that are entry points in this
	 * program.
	 *
	 * @return the collection of entry points
	 */
	public final Collection<CFG> getEntryPoints() {
		return entrypoints;
	}

	/**
	 * Yields the collection of {@link ClassUnit}s defined in this program. Each
	 * compilation unit is uniquely identified by its name, meaning that there
	 * are no two compilation units having the same name in a program.
	 * 
	 * @return the collection of compilation units
	 */
	public final Collection<Unit> getUnits() {
		return units.values();
	}

	/**
	 * Yields the {@link ClassUnit} defined in this unit having the given name
	 * ({@link ClassUnit#getName()}), if any.
	 * 
	 * @param name the name of the compilation unit to find
	 * 
	 * @return the compilation unit with the given name, or {@code null}
	 */
	public final Unit getUnit(String name) {
		return units.get(name);
	}

	/**
	 * {@inheritDoc}<br>
	 * <br>
	 * This method also returns all the cfgs defined in all the {@link Unit}s in
	 * this program, through {@link Unit#getCodeMembersRecursively()}.
	 */
	@Override
	public Collection<CodeMember> getCodeMembersRecursively() {
		Collection<CodeMember> all = super.getCodeMembersRecursively();
		units.values().stream().flatMap(u -> u.getCodeMembersRecursively().stream()).forEach(all::add);
		return all;
	}

	/**
	 * {@inheritDoc}<br>
	 * <br>
	 * This method also returns all the globals defined in all the
	 * {@link ClassUnit}s in this program, through
	 * {@link ClassUnit#getGlobalsRecursively()}.
	 */
	@Override
	public Collection<Global> getGlobalsRecursively() {
		Collection<Global> all = super.getGlobalsRecursively();
		units.values().stream().flatMap(u -> u.getGlobalsRecursively().stream()).forEach(all::add);
		return all;
	}

	@Override
	public boolean canBeInstantiated() {
		return false;
	}

	@Override
	public Program getProgram() {
		return this;
	}

	/**
	 * Yields all the {@link CFG}s defined in this program, obtained by
	 * filtering the results of {@link #getCodeMembersRecursively()}.
	 * 
	 * @return the cfgs
	 */
	public Collection<CFG> getAllCFGs() {
		return getCodeMembersRecursively().stream().filter(CFG.class::isInstance)
				.map(CFG.class::cast).collect(Collectors.toSet());
	}
}
