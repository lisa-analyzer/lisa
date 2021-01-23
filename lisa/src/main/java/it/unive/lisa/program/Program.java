package it.unive.lisa.program;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.NativeCFG;
import it.unive.lisa.type.Type;

public class Program extends Unit {

	public static final String PROGRAM_NAME = "~LiSAProgram";

	private final Map<String, CompilationUnit> units;
	
	private Collection<Type> types;

	public Program() {
		super(null, -1, -1, PROGRAM_NAME);
		units = new ConcurrentHashMap<>();
		types = new ArrayList<>();
	}

	public boolean registerType(Type type) {
		return types.add(type);
	}
	
	public Collection<Type> getRegisteredTypes() {
		return types;
	}
	
	public boolean addCompilationUnit(CompilationUnit unit) {
		return units.putIfAbsent(unit.getName(), unit) == null;
	}

	public boolean addCompilationUnits(Collection<? extends CompilationUnit> units) {
		AtomicBoolean bool = new AtomicBoolean(true);
		units.forEach(u -> bool.set(bool.get() && addCompilationUnit(u)));
		return bool.get();
	}

	public Collection<CompilationUnit> getUnits() {
		return units.values();
	}

	public CompilationUnit getUnit(String name) {
		return units.get(name);
	}

	@Override
	public Collection<CFG> getAllCFGs() {
		Collection<CFG> all = super.getAllCFGs();
		units.values().stream().flatMap(u -> u.getAllCFGs().stream()).forEach(all::add);
		return all;
	}

	@Override
	public Collection<NativeCFG> getAllConstructs() {
		Collection<NativeCFG> all = super.getAllConstructs();
		units.values().stream().flatMap(u -> u.getAllConstructs().stream()).forEach(all::add);
		return all;
	}

	@Override
	public Collection<Global> getAllGlobals() {
		Collection<Global> all = super.getAllGlobals();
		units.values().stream().flatMap(u -> u.getAllGlobals().stream()).forEach(all::add);
		return all;
	}

	public void validateAndFinalize() throws ProgramValidationException {
		// shrink memory fingerprint
		types = null;
		
		for (CompilationUnit unit : getUnits())
			unit.validateAndFinalize();
	}
}
