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

	/**
	 * Yields the policy deciding the sorts of the identifiers and of the
	 * unknown values of a model encoded with the encoders of this factory,
	 * given the one computed for the model (e.g., to encode floating point
	 * numbers of a frontend as 64-bits ones, see
	 * {@link SortPolicy#withDoubleFloats()}): the encoders built by this
	 * factory receive the yielded policy. By default, this is the given policy.
	 *
	 * @param sorts the policy computed for the model
	 *
	 * @return the policy to use
	 */
	default SortPolicy sorts(
			SortPolicy sorts) {
		return sorts;
	}
}
