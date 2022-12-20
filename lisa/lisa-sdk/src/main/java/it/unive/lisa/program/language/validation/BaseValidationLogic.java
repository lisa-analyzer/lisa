package it.unive.lisa.program.language.validation;

import static java.lang.String.format;

import it.unive.lisa.program.AbstractClassUnit;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.CodeUnit;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.ConstantGlobal;
import it.unive.lisa.program.Global;
import it.unive.lisa.program.InterfaceUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.ProgramValidationException;
import it.unive.lisa.program.Unit;
import it.unive.lisa.program.annotations.Annotation;
import it.unive.lisa.program.cfg.AbstractCodeMember;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.controlFlow.ControlFlowStructure;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.util.datastructures.graph.code.NodeList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.lang3.StringUtils;

/**
 * A simple implementation of {@link ProgramValidationLogic}, providing the
 * minimum reasoning for considering a {@link Program} valid for LiSA. Each
 * validation method can be overridden by subclasses to define language-specific
 * validation logic.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class BaseValidationLogic implements ProgramValidationLogic {

	/**
	 * Error message format for duplicated {@link CodeMember}s in the same
	 * {@link Unit}.
	 */
	public static final String DUPLICATE_MEMBER = "%s is duplicated within unit %s";

	/**
	 * Error message format for invalid {@link NodeList}s.
	 */
	public static final String INVALID_NODE_LIST = "The node list behind %s is invalid";

	/**
	 * Error message format for invalid {@link ControlFlowStructure}s.
	 */
	public static final String INVALID_CFSTRUCTURE = "%s has a conditional structure (%s) that contains a node not in the graph: %s";

	/**
	 * Error message format for terminating {@link Statement}s with followers.
	 */
	public static final String EXIT_WITH_FOLLOWERS = "%s contains an execution-stopping node that has followers: %s";

	/**
	 * Error message format for {@link Statement}s with no followers.
	 */
	public static final String NO_FOLLOWERS = "%s contains a node with no followers that is not execution-stopping: %s";

	/**
	 * Error message format for entrypoints not defined inside the
	 * {@link Program}.
	 */
	public static final String UNKNOWN_ENTRYPOINTS = "Program contains entrypoints that are not part of the program: %s";

	/**
	 * Error message format for {@link Global}s inside {@link InterfaceUnit}s.
	 */
	public static final String INTERFACE_WITH_GLOBALS = "%s is an interface and cannot have instange globals";

	/**
	 * Error message format for sealed {@link AbstractClassUnit}s.
	 */
	public static final String ABSTRACT_SEALED_UNIT = "%s is abstract and cannot be sealed";

	/**
	 * Error message format for {@link AbstractCodeMember}s in
	 * non-{@link AbstractClassUnit}s.
	 */
	public static final String ABSTRACT_METHODS = "%s is not abstract and cannot have abstract code members";

	/**
	 * Error message format for inheritance from a sealed
	 * {@link CompilationUnit}.
	 */
	public static final String INHERIT_FROM_SEALED = "%s cannot inherit from the sealed unit %s";

	/**
	 * Error message format for missing override of an
	 * {@link AbstractCodeMember}.
	 */
	public static final String MISSING_OVERRIDE = "%s does not override %s from the non-instantiable unit %s";

	/**
	 * Error message format for the overriding of a non-overridable
	 * {@link CodeMember}.
	 */
	public static final String CANNOT_OVERRIDE = "%s overrides the non-overridable member %s";

	/**
	 * Error message format for multiple overrides of the same
	 * {@link CodeMember} inside the same {@link CompilationUnit}.
	 */
	public static final String MULTIPLE_OVERRIDES = "%s is overriden multiple times in unit %s: %s";

	/**
	 * Error message format for {@link AbstractCodeMember} that is not instance.
	 */
	public static final String ABSTRACT_NON_INSTANCE = "%s is not an instance member and cannot be abstract";

	/**
	 * Error message format for a {@link CodeMember} not matching its own
	 * signature.
	 */
	public static final String MEMBER_MISMATCH = "%s does not match its own signature";

	/**
	 * Error message format for a {@link Global} not matching its instance flag.
	 */
	public static final String GLOBAL_INSTANCE_MISMATCH = "%s has a mismatched instance flag (%s, while being %s)";

	/**
	 * Error message format for a {@link ConstantGlobal} being an instance
	 * global.
	 */
	public static final String CONST_INSTANCE_GLOBAL = "%s is a constant global and cannot be declared as instance";

	/**
	 * The set of {@link CompilationUnit}s, represented by their names, that
	 * have been already processed by
	 * {@link #validateAndFinalize(CompilationUnit)}. This is used to avoid
	 * duplicate validation of the same unit in multiple inheritance chains.
	 */
	public final Set<String> processedUnits = new TreeSet<>();

	/**
	 * {@inheritDoc} <br>
	 * <br>
	 * Validating a program simply causes the validation of all the
	 * {@link Unit}s and {@link CodeMember}s defined inside it, and ensures that
	 * all entrypoints ({@link Program#getEntryPoints()}) are defined.
	 */
	@Override
	public void validateAndFinalize(Program program) throws ProgramValidationException {
		validateAndFinalize((Unit) program);

		// all entrypoints should be within the set of cfgs
		Collection<CFG> baseline = program.getAllCFGs();
		Collection<CFG> entrypoints = program.getEntryPoints();
		if (!baseline.containsAll(entrypoints)) {
			Set<CFG> diff = new HashSet<>(entrypoints);
			diff.retainAll(baseline);
			throw new ProgramValidationException(format(UNKNOWN_ENTRYPOINTS, diff));
		}

		for (Unit unit : program.getUnits())
			validateAndFinalize(unit);
	}

	/**
	 * Validates the given unit, ensuring its consistency. This ensures that all
	 * of its {@link CodeMember}s are valid through
	 * {@link #validate(CodeMember, boolean)}. Further validation logic is
	 * deferred to this method's overloads accepting more specific types of
	 * units (e.g., {@link #validateAndFinalize(ClassUnit)}).
	 * 
	 * @param unit the unit to validate
	 * 
	 * @throws ProgramValidationException if the unit has an invalid structure
	 */
	public void validateAndFinalize(Unit unit) throws ProgramValidationException {
		for (CodeMember member : unit.getCodeMembers())
			validate(member, false);

		for (Global global : unit.getGlobals())
			validate(global, false);

		if (unit instanceof CodeUnit)
			validateAndFinalize((CodeUnit) unit);
		else if (unit instanceof AbstractClassUnit)
			validateAndFinalize((AbstractClassUnit) unit);
		else if (unit instanceof ClassUnit)
			validateAndFinalize((ClassUnit) unit);
		else if (unit instanceof InterfaceUnit)
			validateAndFinalize((InterfaceUnit) unit);
		// we do nothing on unknown units
	}

	/**
	 * Validates the given {@link Global}. This method checks that
	 * {@link ConstantGlobal}s are not instance ones, and then
	 * {@link Global#isInstance()} agrees with {@code isInstance}.
	 * 
	 * @param global     the global to validate
	 * @param isInstance whether or not the given global is part of the instance
	 *                       variables of its container
	 * 
	 * @throws ProgramValidationException if the global has an invalid structure
	 */
	public void validate(Global global, boolean isInstance) throws ProgramValidationException {
		if (isInstance != global.isInstance())
			throw new ProgramValidationException(format(GLOBAL_INSTANCE_MISMATCH, global, global.isInstance(),
					isInstance ? "instance" : "non-instance"));

		if (isInstance && global instanceof ConstantGlobal)
			throw new ProgramValidationException(format(CONST_INSTANCE_GLOBAL, global));
	}

	/**
	 * Validates the given {@link CodeUnit}. This method checks that no instance
	 * {@link Global}s are defined in the unit, and then delegates validation to
	 * {@link #validateAndFinalize(CompilationUnit)}.
	 * 
	 * @param unit the unit to validate
	 * 
	 * @throws ProgramValidationException if the unit has an invalid structure
	 */
	public void validateAndFinalize(CodeUnit unit) throws ProgramValidationException {
		// nothing to do
	}

	/**
	 * Validates the given {@link ClassUnit}. This method checks that no
	 * {@link AbstractCodeMember} is defined in the unit, and then delegates
	 * validation to {@link #validateAndFinalize(CompilationUnit)}.
	 * 
	 * @param unit the unit to validate
	 * 
	 * @throws ProgramValidationException if the unit has an invalid structure
	 */
	public void validateAndFinalize(ClassUnit unit) throws ProgramValidationException {
		if (processedUnits.contains(unit.getName()))
			return;

		if (unit.canBeInstantiated()
				&& !unit.searchCodeMembers(cm -> cm instanceof AbstractCodeMember, false).isEmpty())
			throw new ProgramValidationException(format(ABSTRACT_METHODS, unit));

		validateAndFinalize((CompilationUnit) unit);
	}

	/**
	 * Validates the given {@link AbstractClassUnit}. This method checks the
	 * unit is not sealed ({@link CompilationUnit#isSealed()}), and then
	 * delegates validation to {@link #validateAndFinalize(CompilationUnit)}.
	 * 
	 * @param unit the unit to validate
	 * 
	 * @throws ProgramValidationException if the unit has an invalid structure
	 */
	public void validateAndFinalize(AbstractClassUnit unit) throws ProgramValidationException {
		if (processedUnits.contains(unit.getName()))
			return;

		if (unit.isSealed())
			throw new ProgramValidationException(format(ABSTRACT_SEALED_UNIT, unit));

		validateAndFinalize((ClassUnit) unit);
	}

	/**
	 * Validates the given {@link InterfaceUnit}. This method checks that no
	 * instance {@link Global}s are defined in the unit, and then delegates
	 * validation to {@link #validateAndFinalize(CompilationUnit)}.
	 * 
	 * @param unit the unit to validate
	 * 
	 * @throws ProgramValidationException if the unit has an invalid structure
	 */
	public void validateAndFinalize(InterfaceUnit unit) throws ProgramValidationException {
		if (processedUnits.contains(unit.getName()))
			return;

		if (!unit.getInstanceGlobals(false).isEmpty())
			throw new ProgramValidationException(format(INTERFACE_WITH_GLOBALS, unit));

		validateAndFinalize((CompilationUnit) unit);
	}

	/**
	 * Validates the given {@link CompilationUnit}. This causes the validation
	 * of all its super units transitively and the population of the set of
	 * instances ({@link CompilationUnit#getInstances()}) of each element in its
	 * hierarchy. Moreover, all instance {@link CodeMember}s are validated
	 * through {@link #validate(CodeMember, boolean)}. These are also linked to
	 * other ones in the hierarchy, populating the collections
	 * {@link CodeMemberDescriptor#overriddenBy()} and
	 * {@link CodeMemberDescriptor#overrides()} and raising errors if
	 * {@link AbstractCodeMember}s do not have an implementation in instantiable
	 * ({@link CompilationUnit#canBeInstantiated()}) units. Lastly, annotations
	 * are propagated along the inheritance hierarchy.
	 * 
	 * @param unit the unit to validate
	 * 
	 * @throws ProgramValidationException if the unit has an invalid structure
	 */
	public void validateAndFinalize(CompilationUnit unit) throws ProgramValidationException {
		if (processedUnits.contains(unit.getName()))
			return;

		// recursive invocation
		for (CompilationUnit sup : unit.getImmediateAncestors())
			if (sup.isSealed())
				throw new ProgramValidationException(format(INHERIT_FROM_SEALED, unit, sup));
			else
				validateAndFinalize(sup);

		// check for duplicate cms
		for (CodeMember cm : unit.getInstanceCodeMembers(false))
			validate(cm, true);

		for (Global global : unit.getInstanceGlobals(false))
			validate(global, true);

		unit.addInstance(unit);

		for (CompilationUnit ancestor : unit.getImmediateAncestors()) {
			// check overriders/implementers
			for (CodeMember inherited : ancestor.getInstanceCodeMembers(true)) {
				Collection<CodeMember> localOverrides = unit.getMatchingInstanceCodeMembers(inherited.getDescriptor(),
						false);
				if (localOverrides.isEmpty()) {
					if (inherited instanceof AbstractCodeMember && !ancestor.canBeInstantiated()
							&& unit.canBeInstantiated())
						// this is the first non-abstract child of ancestor, and
						// it must provide an implementation for all abstract
						// code members defined in the inheritance chain
						throw new ProgramValidationException(
								format(MISSING_OVERRIDE, unit, inherited.getDescriptor().getSignature(), ancestor));
				} else if (localOverrides.size() == 1) {
					if (!inherited.getDescriptor().isOverridable()) {
						throw new ProgramValidationException(
								format(CANNOT_OVERRIDE, unit, inherited.getDescriptor().getSignature()));
					} else {
						CodeMember over = localOverrides.iterator().next();
						over.getDescriptor().overrides().addAll(inherited.getDescriptor().overrides());
						over.getDescriptor().overrides().add(inherited);
						over.getDescriptor().overrides().forEach(c -> c.getDescriptor().overriddenBy().add(over));
					}
				} else {
					throw new ProgramValidationException(
							format(MULTIPLE_OVERRIDES, inherited.getDescriptor().getSignature(), unit,
									StringUtils.join(", ", localOverrides)));
				}
			}

			// propagate annotations
			for (Annotation ann : ancestor.getAnnotations())
				if (!ann.isInherited())
					unit.addAnnotation(ann);
		}

		// propagate annotations in cfgs - this has to be done after the
		// override chain has been computed
		for (CodeMember instCfg : unit.getInstanceCodeMembers(false))
			for (CodeMember matching : instCfg.getDescriptor().overrides())
				for (Annotation ann : matching.getDescriptor().getAnnotations()) {
					if (!ann.isInherited())
						instCfg.getDescriptor().addAnnotation(ann);

					Parameter[] args = instCfg.getDescriptor().getFormals();
					Parameter[] superArgs = matching.getDescriptor().getFormals();
					for (int i = 0; i < args.length; i++)
						for (Annotation parAnn : superArgs[i].getAnnotations())
							if (!parAnn.isInherited())
								args[i].addAnnotation(parAnn);
				}

		processedUnits.add(unit.getName());
	}

	/**
	 * Validates the given {@link CodeMember}, checking that no other code
	 * member exists in its containing {@link Unit} with the same signature,
	 * according to
	 * {@link CodeMemberDescriptor#matchesSignature(CodeMemberDescriptor)}. This
	 * avoids ambiguous call resolution. Moreover, this ensures that all
	 * {@link CFG}s are valid, according to {@link CFG#validate()}.
	 * 
	 * @param member   the code member to validate
	 * @param instance if {@code true}, duplicates will be searched in instance
	 *                     members instead
	 * 
	 * @throws ProgramValidationException if the member has an invalid structure
	 */
	public void validate(CodeMember member, boolean instance) throws ProgramValidationException {
		if (!instance && member instanceof AbstractCodeMember)
			throw new ProgramValidationException(format(ABSTRACT_NON_INSTANCE, member));
		Unit container = member.getDescriptor().getUnit();
		Collection<CodeMember> matching = instance
				? ((CompilationUnit) container).getMatchingInstanceCodeMembers(member.getDescriptor(), false)
				: container.getMatchingCodeMember(member.getDescriptor());
		if (matching.isEmpty())
			throw new ProgramValidationException(
					format(MEMBER_MISMATCH, member.getDescriptor().getSignature()));
		else if (matching.size() != 1 || matching.iterator().next() != member)
			throw new ProgramValidationException(
					format(DUPLICATE_MEMBER, member.getDescriptor().getSignature(), container));

		member.validate();
	}
}