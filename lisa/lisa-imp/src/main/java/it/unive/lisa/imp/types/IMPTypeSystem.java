package it.unive.lisa.imp.types;

import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.program.type.StringType;
import it.unive.lisa.type.BooleanType;
import it.unive.lisa.type.CharacterType;
import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.ReferenceType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import it.unive.lisa.type.UnitType;
import it.unive.lisa.type.Untyped;

/**
 * THe {@link TypeSystem} for the IMP language.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class IMPTypeSystem
		extends
		TypeSystem {

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
	public boolean canBeReferenced(
			Type type) {
		return type.isInMemoryType() || type.isUntyped();
	}

	@Override
	public int distanceBetweenTypes(
			Type first,
			Type second) {
		if (first instanceof Untyped)
			return 0;
		if (second instanceof Untyped)
			return 0;
		if (first.isNumericType() && second.isNumericType())
			if (first.equals(second))
				return 0;
			else
				return 1;
		else if (second.isBooleanType() && first.isBooleanType())
			return 0;
		else if (second.isStringType() && first.isStringType())
			return 0;
		else if (second.isReferenceType() && first.isReferenceType()) {
			ReferenceType refFirst = first.asReferenceType();
			ReferenceType refSecond = second.asReferenceType();
			if (refSecond.getInnerType().isNullType())
				return 0;
			else if (refSecond.getInnerType().isArrayType()
					&& refFirst.getInnerType().isArrayType())
				return refFirst.getInnerType().equals(refSecond.getInnerType()) ? 0 : -1;

			// from here on, we should suppose that the inner types are
			// units
			UnitType firstUnitType = refFirst.getInnerType().asUnitType();
			UnitType secondUnitType = refSecond.getInnerType().asUnitType();
			if (secondUnitType != null && firstUnitType != null) {
				int paramDist = distanceBetweenCompilationUnits(firstUnitType.getUnit(), secondUnitType.getUnit());
				if (paramDist < 0)
					return -1;
				return paramDist;
			} else
				return -1;
		} else
			return -1;
	}

	private int distanceBetweenCompilationUnits(
			CompilationUnit unit1,
			CompilationUnit unit2) {
		if (unit1.equals(unit2))
			return 0;
		for (CompilationUnit ancestor : unit2.getImmediateAncestors()) {
			int dist = distanceBetweenCompilationUnits(unit1, ancestor);
			if (dist < 0)
				continue;
			return 1 + dist;
		}
		return -1;
	}

	@Override
	public CharacterType getCharacterType() {
		// IMP does not have characters
		throw new UnsupportedOperationException("IMP does not support character types");
	}

}
