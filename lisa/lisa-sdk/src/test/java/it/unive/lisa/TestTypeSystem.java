package it.unive.lisa;

import it.unive.lisa.type.BooleanType;
import it.unive.lisa.type.CharacterType;
import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.StringType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Set;

public class TestTypeSystem
		extends
		TypeSystem {

	@Override
	public BooleanType getBooleanType() {
		return new BooleanType() {

			@Override
			public Type commonSupertype(
					Type other) {
				return null;
			}

			@Override
			public boolean canBeAssignedTo(
					Type other) {
				return false;
			}

			@Override
			public Set<Type> allInstances(
					TypeSystem types) {
				return null;
			}

		};
	}

	@Override
	public StringType getStringType() {
		return new StringType() {

			@Override
			public Type commonSupertype(
					Type other) {
				return null;
			}

			@Override
			public boolean canBeAssignedTo(
					Type other) {
				return false;
			}

			@Override
			public Set<Type> allInstances(
					TypeSystem types) {
				return null;
			}

		};
	}

	@Override
	public NumericType getIntegerType() {
		return new NumericType() {

			@Override
			public Type commonSupertype(
					Type other) {
				return null;
			}

			@Override
			public boolean canBeAssignedTo(
					Type other) {
				return false;
			}

			@Override
			public Set<Type> allInstances(
					TypeSystem types) {
				return null;
			}

			@Override
			public boolean isUnsigned() {
				return false;
			}

			@Override
			public boolean isIntegral() {
				return false;
			}

			@Override
			public int getNBits() {
				return 0;
			}
		};
	}

	@Override
	public boolean canBeReferenced(
			Type type) {
		return false;
	}

	@Override
	public int distanceBetweenTypes(
			Type first,
			Type second) {
		return 0;
	}

	@Override
	public CharacterType getCharacterType() {
		return new CharacterType() {

			@Override
			public Type commonSupertype(
					Type other) {
				return null;
			}

			@Override
			public boolean canBeAssignedTo(
					Type other) {
				return false;
			}

			@Override
			public Set<Type> allInstances(
					TypeSystem types) {
				return null;
			}

		};
	}

}
