package it.unive.lisa.lattices.string;

import org.junit.jupiter.api.Test;

public class StringConstantTest {

	@Test
	public void testConstructor() {
		new StringConstant();
	}

	@Test
	public void testConstructor1() {
		new StringConstant("Hello World!");
	}

}
