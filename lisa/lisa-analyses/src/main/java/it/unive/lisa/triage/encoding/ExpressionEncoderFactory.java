package it.unive.lisa.triage.encoding;

/**
 * A factory of {@link ExpressionEncoder}s. Encoders are bound to a
 * {@link SymbolTable}, that changes with each encoding (e.g., the constants of
 * different steps of an {@link it.unive.lisa.triage.bmc.Unrolling} have
 * different names): the components encoding expressions (e.g.,
 * {@link it.unive.lisa.triage.bmc.Unrolling},
 * {@link it.unive.lisa.triage.pdr.PdrEngine}, and the verifiers using them)
 * thus receive a factory, and build a new encoder for each encoding. Frontends
 * encoding their own operators (see {@link ExpressionEncoder}) pass the
 * constructor of their encoder (e.g., {@code MyEncoder::new}), while
 * {@link #DEFAULT} builds encoders knowing only the operators of LiSA.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
@FunctionalInterface
public interface ExpressionEncoderFactory {

	/**
	 * The factory of {@link ExpressionEncoder}s knowing only the operators of
	 * LiSA.
	 */
	ExpressionEncoderFactory DEFAULT = ExpressionEncoder::new;

	/**
	 * Builds a new encoder.
	 *
	 * @param sorts   the policy deciding the sorts of identifiers and of
	 *                    unknown values
	 * @param symbols the provider of the constants used in the encoding
	 *
	 * @return the encoder
	 */
	ExpressionEncoder create(
			SortPolicy sorts,
			SymbolTable symbols);
}
