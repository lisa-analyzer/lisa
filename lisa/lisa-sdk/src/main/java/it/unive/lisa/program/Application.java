package it.unive.lisa.program;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMember;
import java.util.Collection;
import java.util.HashSet;

/**
 * An complete application, collecting several {@link Program}s that need to be
 * analyzed together. While each program is built through a single programming
 * language, an application is a complete and possibly multilanguage system.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Application {

	private final Program[] programs;

	private Collection<CodeMember> members;

	private Collection<CFG> cfgs;

	private Collection<CFG> entrypoints;

	/**
	 * Builds the application.
	 * 
	 * @param programs the programs composing the application
	 */
	public Application(Program... programs) {
		this.programs = programs;
	}

	/**
	 * Yields the program that build this application.
	 * 
	 * @return the programs
	 */
	public Program[] getPrograms() {
		return programs;
	}

	/**
	 * Yields the lazily computed collection of all {@link CFG}s defined in all
	 * {@link Program}s of this application.
	 * 
	 * @return the cfgs
	 */
	public Collection<CFG> getAllCFGs() {
		if (cfgs != null)
			return cfgs;

		cfgs = new HashSet<>();
		for (Program p : programs)
			cfgs.addAll(p.getAllCFGs());

		return cfgs;
	}

	/**
	 * Yields the lazily computed collection of all {@link CFG}s that are
	 * entrypoints of one of the {@link Program}s of this application.
	 * 
	 * @return the entrypoints
	 */
	public Collection<CFG> getEntryPoints() {
		if (entrypoints != null)
			return entrypoints;

		entrypoints = new HashSet<>();
		for (Program p : programs)
			entrypoints.addAll(p.getEntryPoints());

		return entrypoints;
	}

	/**
	 * Yields the lazily computed collection of all {@link CodeMember}s defined
	 * in all {@link Program}s of this application.
	 * 
	 * @return the code members
	 */
	public Collection<CodeMember> getAllCodeCodeMembers() {
		if (members != null)
			return members;

		members = new HashSet<>();
		for (Program p : programs)
			members.addAll(p.getCodeMembersRecursively());

		return members;
	}
}
