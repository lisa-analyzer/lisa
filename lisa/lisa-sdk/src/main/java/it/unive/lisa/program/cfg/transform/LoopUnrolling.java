package it.unive.lisa.program.cfg.transform;

import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.controlFlow.ControlFlowStructure;
import it.unive.lisa.program.cfg.controlFlow.Loop;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.edge.FalseEdge;
import it.unive.lisa.program.cfg.edge.TrueEdge;
import it.unive.lisa.program.cfg.statement.CloneLocator;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.util.datastructures.graph.code.NodeList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Unwinds the first {@code k} iterations of each innermost {@link Loop} in a
 * {@link CFG}, inserting the straight-line clones in front of a residual copy
 * of the original loop that handles any further iterations. Nested loops are
 * skipped to keep CFG growth linear rather than {@code k^(nesting-depth)}.
 *
 * @author <a href="mailto:giacomo12596@gmail.com">Giacomo Zanatta</a>
 */
public class LoopUnrolling
		implements
		CFGTransformation {

	/**
	 * Number of unwound iterations. Must be {@code >= 0}; {@code 0} makes the
	 * transformation a no-op.
	 */
	public int factor;

	/**
	 * Builds the transformation with the given unroll factor.
	 *
	 * @param factor the number of unwound iterations; must be {@code >= 0}
	 *
	 * @throws IllegalArgumentException if {@code factor < 0}
	 */
	public LoopUnrolling(
			int factor) {
		if (factor < 0)
			throw new IllegalArgumentException("factor must be >= 0, was " + factor);
		this.factor = factor;
	}

	@Override
	public void transform(
			CFG cfg) {
		if (factor <= 0)
			return;

		List<Loop> loops = new ArrayList<>();
		for (ControlFlowStructure cfs : cfg.getDescriptor().getControlFlowStructures())
			if (cfs instanceof Loop)
				loops.add((Loop) cfs);

		for (Loop loop : loops) {
			if (!isInnermost(loop, cfg))
				continue;
			partialUnroll(cfg, loop, factor);
		}
	}

	/**
	 * A loop is innermost when no other loop's condition sits inside its body.
	 */
	private boolean isInnermost(
			Loop loop,
			CFG cfg) {
		Collection<Statement> body = loop.getBody();
		for (ControlFlowStructure cfs : cfg.getDescriptor().getControlFlowStructures())
			if (cfs instanceof Loop && cfs != loop && body.contains(cfs.getCondition()))
				return false;
		return true;
	}

	/**
	 * Unwinds {@code k} copies of the loop body before the (unmodified)
	 * residual loop.
	 */
	private void partialUnroll(
			CFG cfg,
			Loop loop,
			int k) {
		Statement condition = loop.getCondition();
		Statement follower = loop.getFirstFollower();
		// sort body statements deterministically so the unwound CFG shape is
		// reproducible across runs (loop.getBody() may return a Collection
		// whose iteration order is not stable)
		List<Statement> body = new ArrayList<>(loop.getBody());
		body.sort(null);

		NodeList<CFG, Statement, Edge> list = cfg.getNodeList();

		// snapshot predecessors of the condition that come from OUTSIDE the
		// loop (i.e. not the back-edge from body); these must be redirected
		// to the head of the first unwound iteration once it exists.
		List<Edge> outsidePreds = new ArrayList<>();
		for (Edge in : list.getIngoingEdges(condition))
			if (!body.contains(in.getSource()) && in.getSource() != condition)
				outsidePreds.add(in);

		// snapshot internal body edges before we start mutating.
		// An "internal" edge is (u, v) where u is in the body; v is in the
		// body OR is the follower OR is the condition (back-edge).
		List<Edge> internalBodyEdges = new ArrayList<>();
		for (Statement s : body)
			for (Edge out : list.getOutgoingEdges(s))
				if (body.contains(out.getDestination())
						|| out.getDestination() == follower
						|| out.getDestination() == condition)
					internalBodyEdges.add(out);

		// find the body head (the destination of the TrueEdge from condition).
		Statement bodyHead = null;
		for (Edge out : list.getOutgoingEdges(condition))
			if (out instanceof TrueEdge) {
				bodyHead = out.getDestination();
				break;
			}
		if (bodyHead == null)
			// non-standard loop shape (no TrueEdge from condition) — leave
			// the loop untouched.
			return;

		// build all unwound conditions first so continue-like back-edges from
		// iteration i can point at iteration i+1's condition.
		Statement[] unwoundConds = new Statement[k + 1];
		for (int i = 1; i <= k; i++) {
			final int iter = i;
			CloneLocator locator = orig -> new UnrolledLocation(orig.getLocation(), iter);
			unwoundConds[i] = condition.clone(locator);
		}
		// index k+1 slot holds the residual condition (unchanged).
		unwoundConds[0] = null; // unused; iterations are 1-based
		Statement residualCondition = condition;

		// per-iteration body maps
		List<Map<Statement, Statement>> bodyMaps = new ArrayList<>(k + 1);
		bodyMaps.add(null); // 1-based
		for (int i = 1; i <= k; i++) {
			final int iter = i;
			CloneLocator locator = orig -> new UnrolledLocation(orig.getLocation(), iter);
			Map<Statement, Statement> map = new IdentityHashMap<>();
			for (Statement s : body)
				map.put(s, s.clone(locator));
			bodyMaps.add(map);
		}

		// add all unwound nodes to the CFG in the same order as the (sorted)
		// original body so serialization stays deterministic
		for (int i = 1; i <= k; i++) {
			cfg.addNode(unwoundConds[i]);
			Map<Statement, Statement> map = bodyMaps.get(i);
			for (Statement s : body)
				cfg.addNode(map.get(s));
		}

		// wire the unwound iterations
		for (int i = 1; i <= k; i++) {
			Statement condI = unwoundConds[i];
			Map<Statement, Statement> map = bodyMaps.get(i);
			Statement bodyHeadI = map.get(bodyHead);

			// TrueEdge Cᵢ → bodyHeadᵢ
			cfg.addEdge(new TrueEdge(condI, bodyHeadI));
			// FalseEdge Cᵢ → follower (early exit for this iteration)
			cfg.addEdge(new FalseEdge(condI, follower));

			// map body-internal edges to the unwound clones
			Statement nextCond = (i < k) ? unwoundConds[i + 1] : residualCondition;
			for (Edge e : internalBodyEdges) {
				Statement u = e.getSource();
				Statement v = e.getDestination();
				Statement mappedU = map.get(u);
				if (mappedU == null)
					// u not in body (shouldn't happen given how we snapshotted)
					continue;
				Statement mappedV;
				if (v == follower)
					mappedV = follower; // Break-like
				else if (v == condition)
					mappedV = nextCond; // back-edge or Continue-like
				else
					mappedV = map.get(v); // ordinary internal edge
				if (mappedV == null)
					continue;
				cfg.addEdge(e.newInstance(mappedU, mappedV));
			}
		}

		// redirect outside predecessors from the original condition to the
		// first unwound iteration's condition.
		Statement firstUnwoundCond = unwoundConds[1];
		for (Edge pred : outsidePreds) {
			list.removeEdge(pred);
			cfg.addEdge(pred.newInstance(pred.getSource(), firstUnwoundCond));
		}
	}

}
