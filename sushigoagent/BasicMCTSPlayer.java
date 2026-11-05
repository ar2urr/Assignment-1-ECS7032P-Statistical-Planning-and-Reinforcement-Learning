package sushigoagent;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import core.interfaces.IStateHeuristic;

import java.util.List;
import java.util.Objects;
import java.util.Random;

/** TAG BasicMCTS with legal-action fallbacks, seeded sampling and budget checks. */
public class BasicMCTSPlayer extends AbstractPlayer {
    public record SearchStats(long iterations, long fmCalls, long copyCalls, long elapsedNanos) {}
    private SearchStats lastSearchStats = new SearchStats(0, 0, 0, 0);

    public BasicMCTSPlayer() {
        this(new BasicMCTSParams());
    }

    /** The seed must fit TAG's signed 32-bit randomSeed parameter. */
    public BasicMCTSPlayer(long seed) {
        this(parametersWithSeed(seed));
    }

    private static BasicMCTSParams parametersWithSeed(long seed) {
        if (seed < Integer.MIN_VALUE || seed > Integer.MAX_VALUE)
            throw new IllegalArgumentException("randomSeed must fit a signed 32-bit integer");
        BasicMCTSParams params = new BasicMCTSParams();
        params.setParameterValue("randomSeed", (int) seed);
        return params;
    }

    public BasicMCTSPlayer(BasicMCTSParams params) {
        super(params, "Sushi BasicMCTS");
        params.validate();
        rnd = new Random(params.getRandomSeed());
    }

    @Override
    public AbstractAction _getAction(AbstractGameState gameState, List<AbstractAction> actions) {
        long started = System.nanoTime();
        getParameters().validate();
        if (actions == null || actions.isEmpty())
            throw new IllegalArgumentException("At least one legal action is required");
        SearchBudget budget = new SearchBudget(getParameters(), started, System::nanoTime);
        AbstractAction selected = actions.get(0);
        // Keep a legal move available even if the search cannot expand a child.
        if (actions.size() > 1 && gameState.isNotTerminal()
                && getParameters().maxTreeDepth > 0 && budget.canStartIteration()) {
            Objects.requireNonNull(getForwardModel(), "Forward model must be assigned before search");
            BasicTreeNode root = new BasicTreeNode(this, null, gameState, rnd, actions, budget);
            root.mctsSearch();
            selected = root.bestAction();
        }
        lastSearchStats = new SearchStats(budget.iterations, budget.fmCalls,
                budget.copyCalls, System.nanoTime() - started);
        return selected;
    }

    @Override
    public void initializePlayer(AbstractGameState state) {
        super.initializePlayer(state);
        if (getParameters().resetSeedEachGame)
            rnd.setSeed(getParameters().getRandomSeed());
        lastSearchStats = new SearchStats(0, 0, 0, 0);
    }

    @Override
    public BasicMCTSParams getParameters() {
        return (BasicMCTSParams) parameters;
    }

    public void setStateHeuristic(IStateHeuristic heuristic) {
        getParameters().setParameterValue("heuristic", Objects.requireNonNull(heuristic));
    }

    public SearchStats getLastSearchStats() {
        return lastSearchStats;
    }

    @Override
    public BasicMCTSPlayer copy() {
        BasicMCTSPlayer copy = new BasicMCTSPlayer((BasicMCTSParams) parameters.copy());
        copy.setName(toString());
        return copy;
    }
}
