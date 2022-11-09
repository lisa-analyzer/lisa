package it.unive.lisa.imp.types;

import it.unive.lisa.type.BooleanType;
import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import it.unive.lisa.type.common.BoolType;
import it.unive.lisa.type.common.Int32Type;
import it.unive.lisa.type.common.StringType;

/**
 * THe {@link TypeSystem} for the IMP language.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class IMPTypeSystem extends TypeSystem {

	@Override
	public BooleanType getBooleanType() {
		return BoolType.INSTANCE;
	}

	@Override
	public StringType getStringType() {
		return StringType.INSTANCE;
	}

	@Override
	public NumericType getIntegerType() {
		return Int32Type.INSTANCE;
	}

	@Override
	public boolean canBeReferenced(Type type) {
		return type.isInMemoryType();
	}

}
