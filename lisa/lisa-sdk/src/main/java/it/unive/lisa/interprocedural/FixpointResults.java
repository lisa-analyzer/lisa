package it.unive.lisa.interprocedural;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.lattices.FunctionalLattice;
import it.unive.lisa.program.cfg.CFG;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A {@link FunctionalLattice} from {@link CFG}s to {@link CFGResults}s. This
 * class is meant to store all fixpoint results on all token generated during
 * the interprocedural analysis for each cfg under analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the type of {@link AbstractDomain} contained into the analysis
 *                state
 */
public class FixpointResults<
		A extends AbstractLattice<A>>
		extends
		FunctionalLattice<FixpointResults<A>, CFG, CFGResults<A>> {

	/**
	 * Builds a new result.
	 * 
	 * @param lattice a singleton instance used for retrieving top and bottom
	 *                    values
	 */
	public FixpointResults(
			CFGResults<A> lattice) {
		super(lattice);
	}

	private FixpointResults(
			CFGResults<A> lattice,
			Map<CFG, CFGResults<A>> function) {
		super(lattice, function);
	}

	/**
	 * Stores the result of a fixpoint computation on a cfg. This method returns
	 * the result of calling {@link CFGResults#putResult(ScopeId, AnalyzedCFG)}
	 * with the given {@code token} and {@code result} on the {@link CFGResults}
	 * instance corresponding to {@code cfg}.
	 * 
	 * @param cfg    the {@link CFG} on which the result has been computed
	 * @param token  the {@link ScopeId} that identifying the result
	 * @param result the {@link AnalyzedCFG} to store
	 * 
	 * @return the result of the update operation on the individual cfg result
	 * 
	 * @throws SemanticException if something goes wrong during the update
	 */
	public Pair<Boolean, AnalyzedCFG<A>> putResult(
			CFG cfg,
			ScopeId<A> token,
			AnalyzedCFG<A> result)
			throws SemanticException {
		if (function == null)
			function = mkNewFunction(null, false);
		CFGResults<A> res = function.computeIfAbsent(cfg, c -> new CFGResults<>(result.top()));
		String cfgName = cfg.getDescriptor().getUnit().getName() + "::" + cfg.getDescriptor().getName();
		AnalyzedCFG<A> prev = res.get(token);
		Pair<Boolean, AnalyzedCFG<A>> ret = res.putResult(token, result);
		if (Boolean.TRUE.equals(ret.getLeft())) {
			int n = PUT_RESULT_CHANGED_COUNT.computeIfAbsent(cfgName, k -> new java.util.concurrent.atomic.AtomicInteger(0))
					.incrementAndGet();
			String kind;
			if (prev == null) {
				kind = "FIRST";
			} else if (prev.lessOrEqual(result) && !result.lessOrEqual(prev)) {
				kind = "BIGGER";
			} else if (!prev.lessOrEqual(result) && !result.lessOrEqual(prev)) {
				kind = "INCOMP";
			} else {
				kind = "OTHER";
			}
			// Only dump diagnostic info for a tracked stable-leaf CFG: its
			// output shouldn't change, so any change reveals the real culprit.
			if (cfgName.equals("typing::$init") && n >= 2 && n <= 4 && prev != null) {
				diagnose(cfgName, n, prev, result);
			}
			org.apache.logging.log4j.LogManager.getLogger(FixpointResults.class).info(
					"[FR-TRACK] CFG={} change#{} kind={} token={}", cfgName, n, kind, token);
		}
		return ret;
	}

	private static <A extends AbstractLattice<A>> String stateSize(
			AnalyzedCFG<A> acfg) {
		try {
			it.unive.lisa.analysis.AnalysisState<A> exit = acfg.getExitState();
			String s = exit.toString();
			// extract crude component sizes
			int entryLen = s.length();
			it.unive.lisa.analysis.FixpointInfo fi = exit.getExecutionInformation();
			int fiSize = (fi == null || fi.function == null) ? 0 : fi.function.size();
			return "strLen=" + entryLen + " fiSize=" + fiSize;
		} catch (Throwable t) {
			return "err=" + t.getClass().getSimpleName();
		}
	}

	/**
	 * Recursively drill into public fields to find where equals() differs but
	 * toString() is identical, pinpointing the real growth spot.
	 */
	private static void drillDown(
			org.apache.logging.log4j.Logger log,
			String path,
			Object p,
			Object r,
			int depth) {
		if (depth > 6 || p == null || r == null)
			return;
		if (p.equals(r))
			return;
		String pToS = String.valueOf(p);
		String rToS = String.valueOf(r);
		boolean tsEq = pToS.equals(rToS);
		log.info("[DRILL] {} class={} equals=false toStringEq={} pLen={} rLen={}",
				path, p.getClass().getSimpleName(), tsEq, pToS.length(), rToS.length());
		if (!tsEq) {
			int i = 0;
			while (i < pToS.length() && i < rToS.length() && pToS.charAt(i) == rToS.charAt(i))
				i++;
			int s = Math.max(0, i - 30);
			int ep = Math.min(pToS.length(), i + 150);
			int er = Math.min(rToS.length(), i + 150);
			log.info("[DRILL] {} firstDiffAt={}\n  p: ...{}\n  r: ...{}",
					path, i, pToS.substring(s, ep), rToS.substring(s, er));
			// do NOT return — keep drilling into fields anyway, maybe toString
			// abbreviates but a sub-field has richer info
		}
		// Recurse through public fields
		for (java.lang.reflect.Field f : p.getClass().getFields()) {
			try {
				if (java.lang.reflect.Modifier.isStatic(f.getModifiers()))
					continue;
				f.setAccessible(true);
				Object pc = f.get(p);
				Object rc = f.get(r);
				if (pc != null && rc != null && !pc.equals(rc))
					drillDown(log, path + "." + f.getName(), pc, rc, depth + 1);
			} catch (Throwable t) {
				// ignore
			}
		}
	}

	private static <A extends AbstractLattice<A>> void diagnose(
			String cfgName,
			int change,
			AnalyzedCFG<A> prev,
			AnalyzedCFG<A> result) {
		try {
			org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager.getLogger(FixpointResults.class);
			it.unive.lisa.analysis.AnalysisState<A> pExit = prev.getExitState();
			it.unive.lisa.analysis.AnalysisState<A> rExit = result.getExitState();
			boolean pEqR = pExit.toString().equals(rExit.toString());
			boolean pLeR = pExit.lessOrEqual(rExit);
			boolean rLeP = rExit.lessOrEqual(pExit);
			log.info("[FR-DIAG] {} change#{} exit-toStringEq={} pExit.leq(rExit)={} rExit.leq(pExit)={}",
					cfgName, change, pEqR, pLeR, rLeP);
			// dump full exit strings only once (on change#2)
			if (change == 2) {
				String pStr = pExit.toString();
				String rStr = rExit.toString();
				if (!pStr.equals(rStr)) {
					// find first differing char
					int i = 0;
					while (i < pStr.length() && i < rStr.length() && pStr.charAt(i) == rStr.charAt(i))
						i++;
					int start = Math.max(0, i - 50);
					int endP = Math.min(pStr.length(), i + 200);
					int endR = Math.min(rStr.length(), i + 200);
					log.info("[FR-DIAG] first diff at {}\n  prev: ...{}\n  res:  ...{}",
							i, pStr.substring(start, endP), rStr.substring(start, endR));
				} else {
					log.info("[FR-DIAG] toString identical but lessOrEqual differs!");
					A pState = pExit.getExecutionState();
					A rState = rExit.getExecutionState();
					log.info("[FR-DIAG] state eq={} leq={} rleq={}",
							pState.equals(rState), pState.lessOrEqual(rState), rState.lessOrEqual(pState));
					// Drill into SimpleAbstractState subcomponents via reflection
					// (we don't want a compile-time dep on SimpleAbstractState here)
					try {
						drillDown(log, "root", pState, rState, 0);
					} catch (Throwable t) {
						log.info("[FR-DIAG] drill err={}", t);
					}
					try {
						Class<?> sc = pState.getClass();
						// Try NetworkAbstractState fields first, fallback to
						// SimpleAbstractState fields
						String[] networkFields = { "state", "protocols", "handlerMap", "crossEdges" };
						String[] simpleFields = { "heapState", "valueState", "typeState" };
						boolean hasNetwork = false;
						try {
							sc.getField("state");
							hasNetwork = true;
						} catch (NoSuchFieldException nsfe) {
							// ignore
						}
						String[] fieldNames = hasNetwork ? networkFields : simpleFields;
						log.info("[FR-DIAG] pStateClass={} fields={}", sc.getSimpleName(),
								java.util.Arrays.toString(fieldNames));
						for (String field : fieldNames) {
							java.lang.reflect.Field f = sc.getField(field);
							Object p = f.get(pState);
							Object r = f.get(rState);
							boolean eq = p.equals(r);
							java.lang.reflect.Method leqM = p.getClass().getMethod("lessOrEqual", p.getClass().getInterfaces().length > 0 ? it.unive.lisa.analysis.Lattice.class : Object.class);
							leqM.setAccessible(true);
							boolean pLeq;
							boolean rLeq;
							try {
								pLeq = (boolean) p.getClass().getMethod("lessOrEqual", it.unive.lisa.analysis.Lattice.class).invoke(p, r);
								rLeq = (boolean) r.getClass().getMethod("lessOrEqual", it.unive.lisa.analysis.Lattice.class).invoke(r, p);
							} catch (Throwable tx) {
								pLeq = false;
								rLeq = false;
							}
							log.info("[FR-DIAG]   {} eq={} leq={} rleq={} pClass={}", field, eq, pLeq, rLeq,
									p.getClass().getSimpleName());
							if (!eq) {
								String pS = p.toString();
								String rS = r.toString();
								boolean tsEq = pS.equals(rS);
								log.info("[FR-DIAG]   {} toStringEq={} pLen={} rLen={}", field, tsEq, pS.length(),
										rS.length());
								if (!tsEq) {
									int i = 0;
									while (i < pS.length() && i < rS.length() && pS.charAt(i) == rS.charAt(i))
										i++;
									int s = Math.max(0, i - 40);
									int ep = Math.min(pS.length(), i + 200);
									int er = Math.min(rS.length(), i + 200);
									log.info("[FR-DIAG]   {} firstDiffAt={}\n    p: ...{}\n    r: ...{}", field, i,
											pS.substring(s, ep), rS.substring(s, er));
								}
							}
						}
					} catch (Throwable t) {
						log.info("[FR-DIAG] refl err={}", t);
					}
				}
			}
		} catch (Throwable t) {
			org.apache.logging.log4j.LogManager.getLogger(FixpointResults.class).info("[FR-DIAG] err={}",
					t.getClass().getSimpleName() + ":" + t.getMessage());
		}
	}

	private static final java.util.concurrent.ConcurrentHashMap<String,
			java.util.concurrent.atomic.AtomicInteger> PUT_RESULT_CHANGED_COUNT = new java.util.concurrent.ConcurrentHashMap<>();

	/**
	 * Yields {@code true} if a result exists for the given {@code cfg}.
	 * 
	 * @param cfg the {@link CFG} whose result is to be checked
	 * 
	 * @return {@code true} if that condition holds
	 */
	public boolean contains(
			CFG cfg) {
		return function != null && function.containsKey(cfg);
	}

	/**
	 * Yields the recorded result for the given {@code cfg}. This differs from
	 * {@link #getState(Object)} as it returns {@code null} instead of
	 * {@link Lattice#bottom()} if there is no recorded result for the given
	 * cfg.
	 * 
	 * @param cfg the {@link CFG} whose result is to be checked
	 * 
	 * @return the result, or {@code null}
	 */
	public CFGResults<A> get(
			CFG cfg) {
		return function == null ? null : function.get(cfg);
	}

	@Override
	public FixpointResults<A> top() {
		return new FixpointResults<>(lattice.top());
	}

	@Override
	public FixpointResults<A> bottom() {
		return new FixpointResults<>(lattice.bottom());
	}

	/**
	 * Forgets all results about the given {@link CFG}.
	 * 
	 * @param cfg the cfg to forget
	 */
	public void forget(
			CFG cfg) {
		if (function == null)
			return;
		function.remove(cfg);
		if (function.isEmpty())
			function = null;
	}

	@Override
	public FixpointResults<A> mk(
			CFGResults<A> lattice,
			Map<CFG, CFGResults<A>> function) {
		return new FixpointResults<>(lattice, function);
	}

	@Override
	public CFGResults<A> stateOfUnknown(
			CFG key) {
		return lattice.bottom();
	}

}
