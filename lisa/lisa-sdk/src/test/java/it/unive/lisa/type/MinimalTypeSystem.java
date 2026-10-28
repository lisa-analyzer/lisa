package it.unive.lisa.type;

// a minimal concrete TypeSystem, used across this package's tests only to
// exercise registerType()/getTypes()/getType() as a fixture for
// allInstances() tests; its getBooleanType()/getStringType()/etc. are
// intentionally left unimplemented since no test in this package needs them
class MinimalTypeSystem
		extends
		TypeSystem {

	@Override
	public BooleanType getBooleanType() {
		throw new UnsupportedOperationException();
	}

	@Override
	public StringType getStringType() {
		throw new UnsupportedOperationException();
	}

	@Override
	public NumericType getIntegerType() {
		throw new UnsupportedOperationException();
	}

	@Override
	public CharacterType getCharacterType() {
		throw new UnsupportedOperationException();
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

}
