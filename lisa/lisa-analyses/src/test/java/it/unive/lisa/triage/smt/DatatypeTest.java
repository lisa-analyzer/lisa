package it.unive.lisa.triage.smt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import it.unive.lisa.triage.smt.Datatype.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class DatatypeTest {

	private static Datatype pair() {
		Map<String, List<Field>> constructors = new LinkedHashMap<>();
		constructors.put("mk", List.of(new Field("first", Sort.INT), new Field("second", Sort.STRING)));
		constructors.put("none", List.of());
		return new Datatype("Pair", constructors);
	}

	@Test
	public void declarationAndTerms() {
		Datatype dt = pair();
		assertEquals("(declare-datatypes ((Pair 0)) (((mk (first Int) (second String)) (none))))",
				dt.toSmtLibDeclaration());
		Term mk = dt.construct("mk", Term.integer(1), Term.string("a"));
		assertEquals("(mk 1 \"a\")", mk.toSmtLib());
		assertEquals(dt.getSort(), mk.getSort());
		assertEquals("none", dt.construct("none").toSmtLib());
		assertEquals("((_ is mk) (mk 1 \"a\"))", dt.test("mk", mk).toSmtLib());
		assertEquals(Sort.STRING, dt.select("second", mk).getSort());
	}

	@Test
	public void malformedUses() {
		Datatype dt = pair();
		assertThrows(IllegalArgumentException.class, () -> dt.construct("mk", Term.integer(1)));
		assertThrows(IllegalArgumentException.class, () -> dt.construct("mk", Term.integer(1), Term.integer(2)));
		assertThrows(IllegalArgumentException.class, () -> dt.construct("other"));
		assertThrows(IllegalArgumentException.class, () -> dt.select("third", Term.integer(1)));
		Map<String, List<Field>> duplicated = new LinkedHashMap<>();
		duplicated.put("a", List.of(new Field("f", Sort.INT)));
		duplicated.put("b", List.of(new Field("f", Sort.INT)));
		assertThrows(IllegalArgumentException.class, () -> new Datatype("D", duplicated));
	}
}
