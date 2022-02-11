package it.unive.lisa.program.cfg;

import it.unive.lisa.program.ProgramValidationException;

public interface CFG extends CodeMember {

	public void validate() throws ProgramValidationException;
}
