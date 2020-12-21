package it.unive.lisa.test.imp.tutorial;

import it.unive.lisa.analysis.ValueCartesianProduct;
import it.unive.lisa.analysis.nonrelational.ValueEnvironment;
import it.unive.lisa.symbolic.value.Identifier;

public class ParityXSign extends ValueCartesianProduct<ValueEnvironment<Parity>, ValueEnvironment<Sign>> {

	public ParityXSign() {
		super(new ValueEnvironment<Parity>(new Parity()), new ValueEnvironment<Sign>(new Sign()));
	}

	@Override
	public String representation() {
		String result = "";
		for (Identifier x : left.getKeys())
			result += x + ": (" + left.getState(x) + ", " + right.getState(x) + ")";
		return result;
	}
}