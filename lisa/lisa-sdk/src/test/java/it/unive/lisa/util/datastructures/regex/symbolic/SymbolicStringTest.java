package it.unive.lisa.util.datastructures.regex.symbolic;

import static it.unive.lisa.util.datastructures.regex.symbolic.SymbolicString.mkString;
import static it.unive.lisa.util.datastructures.regex.symbolic.SymbolicString.mkTopString;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class SymbolicStringTest {

	@Test
	public void testCollapseAtBeginning() {
		assertEquals(mkTopString(5).concat(mkString("a")).collapseTopChars(), mkTopString(1).concat(mkString("a")));
	}

	@Test
	public void testCollapseAtEnding() {
		assertEquals(mkString("a").concat(mkTopString(5)).collapseTopChars(), mkString("a").concat(mkTopString(1)));
	}

	@Test
	public void testCollapseInside() {
		assertEquals(
				mkString("a").concat(mkTopString(5)).concat(mkString("b")).collapseTopChars(),
				mkString("a").concat(mkTopString(1)).concat(mkString("b")));

		assertEquals(
				mkString("a").concat(mkTopString(5))
						.concat(mkString("b"))
						.concat(mkTopString(9))
						.concat(mkString("c"))
						.collapseTopChars(),
				mkString("a").concat(mkTopString(1)).concat(mkString("b")).concat(mkTopString(1))
						.concat(mkString("c")));
	}

	@Test
	public void testCollapseAll() {
		assertEquals(
				mkTopString(5).concat(mkString("a")).concat(mkTopString(5)).concat(mkString("b")).collapseTopChars(),
				mkTopString(1).concat(mkString("a")).concat(mkTopString(1)).concat(mkString("b")));

		assertEquals(
				mkString("a").concat(mkTopString(5))
						.concat(mkString("b"))
						.concat(mkTopString(9))
						.concat(mkString("c"))
						.concat(mkTopString(5))
						.collapseTopChars(),
				mkString("a").concat(mkTopString(1))
						.concat(mkString("b"))
						.concat(mkTopString(1))
						.concat(mkString("c"))
						.concat(mkTopString(1)));
	}

}
