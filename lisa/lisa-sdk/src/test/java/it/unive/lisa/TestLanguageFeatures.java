package it.unive.lisa;

import it.unive.lisa.program.language.LanguageFeatures;
import it.unive.lisa.program.language.hierarchytraversal.HierarcyTraversalStrategy;
import it.unive.lisa.program.language.hierarchytraversal.SingleInheritanceTraversalStrategy;
import it.unive.lisa.program.language.parameterassignment.OrderPreservingAssigningStrategy;
import it.unive.lisa.program.language.parameterassignment.ParameterAssigningStrategy;
import it.unive.lisa.program.language.resolution.ParameterMatchingStrategy;
import it.unive.lisa.program.language.resolution.RuntimeTypesMatchingStrategy;
import it.unive.lisa.program.language.validation.BaseValidationLogic;
import it.unive.lisa.program.language.validation.ProgramValidationLogic;

public class TestLanguageFeatures
		extends
		LanguageFeatures {

	@Override
	public ParameterMatchingStrategy getMatchingStrategy() {
		return RuntimeTypesMatchingStrategy.INSTANCE;
	}

	@Override
	public HierarcyTraversalStrategy getTraversalStrategy() {
		return SingleInheritanceTraversalStrategy.INSTANCE;
	}

	@Override
	public ParameterAssigningStrategy getAssigningStrategy() {
		return OrderPreservingAssigningStrategy.INSTANCE;
	}

	@Override
	public ProgramValidationLogic getProgramValidationLogic() {
		return new BaseValidationLogic();
	}

}
