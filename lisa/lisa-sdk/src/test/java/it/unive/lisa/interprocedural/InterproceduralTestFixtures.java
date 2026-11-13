package it.unive.lisa.interprocedural;

import it.unive.lisa.TestLanguageFeatures;
import it.unive.lisa.TestTypeSystem;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.type.ErrorType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import it.unive.lisa.type.Untyped;
import java.util.Set;

/**
 * Shared helpers for building minimal {@link CFG}s (and the {@link Program}s
 * they belong to) used across the {@code it.unive.lisa.interprocedural} package
 * tests.
 */
public final class InterproceduralTestFixtures {

	/** A shared, arbitrary source location used across tests. */
	public static final SourceCodeLocation LOC = new SourceCodeLocation("fake", 0, 0);

	private InterproceduralTestFixtures() {
	}

	/**
	 * Builds a minimal, untyped {@link CFG} with the given name, belonging to
	 * its own dedicated {@link ClassUnit} and {@link Program}.
	 *
	 * @param name the name of the cfg
	 *
	 * @return the cfg
	 */
	public static CFG newCfg(
			String name) {
		return newCfg(name, Untyped.INSTANCE);
	}

	/**
	 * Builds a minimal {@link CFG} with the given name and return type,
	 * belonging to its own dedicated {@link ClassUnit} and {@link Program}.
	 *
	 * @param name       the name of the cfg
	 * @param returnType the return type of the cfg
	 *
	 * @return the cfg
	 */
	public static CFG newCfg(
			String name,
			Type returnType) {
		ClassUnit unit = new ClassUnit(
				LOC, new Program(new TestLanguageFeatures(), new TestTypeSystem()), "unit-" + name, false);
		return new CFG(new CodeMemberDescriptor(LOC, unit, false, name, returnType));
	}

	/**
	 * Builds a fresh, distinct {@link ErrorType} named {@code name}, induced by
	 * the {@link CompilationUnit} that {@code cfg} belongs to. The returned
	 * type is <b>not</b> automatically registered in {@code cfg}'s
	 * {@link Program}: callers wishing an error type to be considered by, e.g.,
	 * {@link WorstCasePolicy} must register it themselves through
	 * {@code cfg.getProgram().getTypes().registerType(...)}.
	 *
	 * @param cfg  the cfg whose unit induces the type
	 * @param name the (unique) name of the type
	 *
	 * @return the error type
	 */
	public static Type errorType(
			CFG cfg,
			String name) {
		CompilationUnit unit = (CompilationUnit) cfg.getDescriptor().getUnit();
		return new ErrorType() {

			@Override
			public CompilationUnit getUnit() {
				return unit;
			}

			@Override
			public boolean canBeAssignedTo(
					Type other) {
				return this == other;
			}

			@Override
			public Type commonSupertype(
					Type other) {
				return this == other ? this : null;
			}

			@Override
			public Set<Type> allInstances(
					TypeSystem types) {
				return Set.of(this);
			}

			@Override
			public String toString() {
				return name;
			}
		};
	}

}
