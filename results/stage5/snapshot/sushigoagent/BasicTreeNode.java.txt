package sushigoagent;

import core.AbstractGameState;
import core.actions.AbstractAction;

import java.util.*;

import static java.util.stream.Collectors.toList;
import static utilities.Utils.noise;

/**
 * Adapted from TAG players.basicMCTS.BasicTreeNode (see LICENSE-TAG.txt).
 * Retains closed-loop selection, random expansion/rollout and most-visited action selection.
 * Stage 2 adds resource checks and seeded sampling; Stage 4 adds selectable
 * per-player value backup and self-interested opponent selection.
 */
class BasicTreeNode {
    final BasicTreeNode root;
    final BasicTreeNode parent;
    final Map<AbstractAction, BasicTreeNode> children = new LinkedHashMap<>();
    final int depth;
    private final double[] totalValues;
    private long nVisits;
    private final BasicMCTSPlayer player;
    private final Random rnd;
    private final SearchBudget budget;
    private final AbstractGameState state;

    protected BasicTreeNode(BasicMCTSPlayer player, BasicTreeNode parent,
                            AbstractGameState state, Random rnd,
                            List<AbstractAction> actions, SearchBudget budget) {
        this.player = player;
        this.parent = parent;
        this.root = parent == null ? this : parent.root;
        this.depth = parent == null ? 0 : parent.depth + 1;
        this.state = state;
        this.totalValues = new double[state.getNPlayers()];
        this.rnd = rnd;
        this.budget = budget;
        if (state.isNotTerminal()) {
            // The root uses the caller's legal list, including any action restrictions.
            List<AbstractAction> legal = actions != null ? actions
                    : player.getForwardModel().computeAvailableActions(state, player.getParameters().actionSpace);
            for (AbstractAction action : legal) children.put(action, null);
        }
    }

    void mctsSearch() {
        while (budget.canStartIteration()) {
            long started = System.nanoTime();
            long operationsBefore = budget.copyCalls + budget.fmCalls;
            BasicTreeNode selected = treePolicy();
            if (!budget.hasTime()) break;
            AbstractGameState endState = selected.rollOut();
            if (!budget.hasTime()) break;
            double[] values = new double[state.getNPlayers()];
            boolean maxN = player.getParameters().opponentModel == BasicMCTSParams.OpponentModel.MAXN;
            for (int p = 0; p < values.length; p++) {
                if (!maxN && p != player.getPlayerID()) continue;
                // Never back up an incomplete vector when the deadline expires.
                if (!budget.hasTime()) return;
                values[p] = player.getParameters().getStateHeuristic().evaluateState(endState, p);
                if (!Double.isFinite(values[p]))
                    throw new IllegalStateException("State heuristic must return a finite value for player " + p);
            }
            selected.backUp(values);
            budget.completeIteration(System.nanoTime() - started);
            // A fully terminal/depth-limited tree needs no further identical evaluations.
            if (budget.copyCalls + budget.fmCalls == operationsBefore
                    && player.getParameters().budgetType != players.PlayerConstants.BUDGET_ITERATIONS
                    && player.getParameters().budgetType != players.PlayerConstants.BUDGET_TIME) break;
        }
    }

    private BasicTreeNode treePolicy() {
        BasicTreeNode cur = this;
        while (cur.state.isNotTerminal() && cur.depth < player.getParameters().maxTreeDepth
                && !cur.children.isEmpty() && budget.hasTime()) {
            if (!cur.unexpandedActions().isEmpty()) {
                return budget.canSpend(1, 1) ? cur.expand() : cur;
            }
            cur = cur.children.get(cur.ucb());
        }
        return cur;
    }

    private List<AbstractAction> unexpandedActions() {
        return children.keySet().stream().filter(a -> children.get(a) == null).collect(toList());
    }

    private BasicTreeNode expand() {
        List<AbstractAction> unexpanded = unexpandedActions();
        AbstractAction chosen = unexpanded.get(rnd.nextInt(unexpanded.size()));
        AbstractGameState nextState = copyState(state);
        if (nextState == null || !advance(nextState, chosen.copy())) return this;
        BasicTreeNode child = new BasicTreeNode(player, this, nextState, rnd, null, budget);
        children.put(chosen, child);
        return child;
    }

    private AbstractGameState copyState(AbstractGameState original) {
        if (!budget.reserveCopy()) return null;
        AbstractGameState copy = original.copy();
        // Seed chance events on the independent simulation copy; never reseed the observation.
        copy.getRnd().setSeed(rnd.nextLong());
        return copy;
    }

    private boolean advance(AbstractGameState state, AbstractAction action) {
        if (!budget.reserveAdvance()) return false;
        player.getForwardModel().next(state, action);
        return true;
    }

    private AbstractAction ucb() {
        AbstractAction bestAction = null;
        double bestValue = -Double.MAX_VALUE;
        BasicMCTSParams params = player.getParameters();
        for (Map.Entry<AbstractAction, BasicTreeNode> entry : children.entrySet()) {
            BasicTreeNode child = entry.getValue();
            if (child == null) throw new IllegalStateException("UCB requires expanded children");
            boolean maxN = params.opponentModel == BasicMCTSParams.OpponentModel.MAXN;
            int actor = state.getCurrentPlayer();
            int perspective = maxN ? actor : player.getPlayerID();
            double childValue = child.totalValues[perspective] / (child.nVisits + params.epsilon);
            double exploration = params.K * Math.sqrt(Math.log(nVisits + 1) / (child.nVisits + params.epsilon));
            // MaxN selects the mover's own value, including repeated turns (Chopsticks).
            // PARANOID retains Stage 2's root-value minimisation at every opponent node.
            double sign = maxN || actor == player.getPlayerID() ? 1.0 : -1.0;
            double uctValue = sign * childValue + exploration;
            uctValue = noise(uctValue, params.epsilon, rnd.nextDouble());
            if (bestAction == null || uctValue > bestValue) {
                bestAction = entry.getKey();
                bestValue = uctValue;
            }
        }
        return bestAction;
    }

    private AbstractGameState rollOut() {
        int rolloutDepth = 0;
        AbstractGameState rolloutState = state;
        if (!finishRollout(state, rolloutDepth) && budget.canSpend(1, 1)) {
            AbstractGameState copy = copyState(state);
            if (copy == null) return state;
            rolloutState = copy;
            while (!finishRollout(rolloutState, rolloutDepth) && budget.canSpend(0, 1)) {
                List<AbstractAction> actions = player.getForwardModel()
                        .computeAvailableActions(rolloutState, player.getParameters().actionSpace);
                if (actions.isEmpty()) break;
                AbstractAction next = actions.get(rnd.nextInt(actions.size()));
                if (!advance(rolloutState, next.copy())) break;
                rolloutDepth++;
            }
        }
        return rolloutState;
    }

    private boolean finishRollout(AbstractGameState state, int rolloutDepth) {
        return rolloutDepth >= player.getParameters().rolloutLength || !state.isNotTerminal();
    }

    private void backUp(double[] values) {
        BasicTreeNode node = this;
        while (node != null) {
            node.nVisits++;
            for (int p = 0; p < values.length; p++) node.totalValues[p] += values[p];
            node = node.parent;
        }
    }

    AbstractAction bestAction() {
        AbstractAction bestAction = children.keySet().iterator().next();
        double bestValue = -Double.MAX_VALUE;
        for (Map.Entry<AbstractAction, BasicTreeNode> entry : children.entrySet()) {
            BasicTreeNode node = entry.getValue();
            if (node == null || node.nVisits == 0) continue;
            double visits = noise(node.nVisits, player.getParameters().epsilon, rnd.nextDouble());
            if (visits > bestValue) {
                bestValue = visits;
                bestAction = entry.getKey();
            }
        }
        return bestAction;
    }
}
