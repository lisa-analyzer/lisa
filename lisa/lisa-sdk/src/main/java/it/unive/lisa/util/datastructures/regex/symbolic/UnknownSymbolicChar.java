package it.unive.lisa.util.datastructures.regex.symbolic;

import it.unive.lisa.util.datastructures.regex.TopAtom;

/**
 * An {@link SymbolicChar} representing an unknown character.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class UnknownSymbolicChar extends SymbolicChar {

	/**
	 * The singleton instance.
	 */
	public static final UnknownSymbolicChar INSTANCE = new UnknownSymbolicChar();

	private UnknownSymbolicChar() {
		super(TopAtom.STRING.charAt(0));
	}

	@Override
	public boolean is(
			char ch) {
		return false;
	}
}
