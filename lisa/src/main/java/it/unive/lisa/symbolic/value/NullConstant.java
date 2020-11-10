package it.unive.lisa.symbolic.value;

public class NullConstant extends Constant {

	public static final NullConstant INSTANCE = new NullConstant();
	
	private static final Object NULL_CONST = new Object();
	
	private NullConstant() {
		super(NULL_CONST);
	}

}
