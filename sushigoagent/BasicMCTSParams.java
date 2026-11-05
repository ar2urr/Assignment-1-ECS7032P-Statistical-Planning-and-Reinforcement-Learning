package sushigoagent;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;
import players.PlayerParameters;

import java.util.Arrays;
import java.util.Objects;

/** Search settings adapted from TAG's players.basicMCTS implementation. */
public class BasicMCTSParams extends PlayerParameters {
    public enum OpponentModel { PARANOID, MAXN }
    public OpponentModel opponentModel = OpponentModel.PARANOID;
    public double K = Math.sqrt(2);
    public int rolloutLength = 10;
    public int maxTreeDepth = 5;
    public double epsilon = 1e-6;
    public IStateHeuristic heuristic = AbstractGameState::getHeuristicScore;

    public BasicMCTSParams() {
        addTunableParameter("opponentModel", OpponentModel.PARANOID, Arrays.asList(OpponentModel.values()));
        addTunableParameter("K", Math.sqrt(2), Arrays.asList(0.0, 0.1, 1.0, Math.sqrt(2), 3.0, 10.0));
        addTunableParameter("rolloutLength", 10, Arrays.asList(0, 3, 10, 30, 100));
        addTunableParameter("maxTreeDepth", 5, Arrays.asList(0, 1, 3, 5, 10, 30, 100));
        addTunableParameter("epsilon", 1e-6);
        addTunableParameter("heuristic", (IStateHeuristic) AbstractGameState::getHeuristicScore);
        setParameterValue("budgetType", players.PlayerConstants.BUDGET_TIME);
        setParameterValue("budget", 450);
        setParameterValue("breakMS", 25);
    }

    @Override
    public void _reset() {
        super._reset();
        opponentModel = (OpponentModel) getParameterValue("opponentModel");
        K = (double) getParameterValue("K");
        rolloutLength = (int) getParameterValue("rolloutLength");
        maxTreeDepth = (int) getParameterValue("maxTreeDepth");
        epsilon = (double) getParameterValue("epsilon");
        heuristic = (IStateHeuristic) getParameterValue("heuristic");
    }

    public void validate() {
        if (!Double.isFinite(K) || K < 0 || !Double.isFinite(epsilon) || epsilon <= 0)
            throw new IllegalArgumentException("K must be finite/nonnegative and epsilon finite/positive");
        if (budget < 0 || breakMS < 0 || maxTreeDepth < 0 || rolloutLength < 0)
            throw new IllegalArgumentException("Budgets and search depths must be nonnegative");
        Objects.requireNonNull(budgetType, "budgetType");
        Objects.requireNonNull(opponentModel, "opponentModel");
        Objects.requireNonNull(heuristic, "heuristic");
    }

    @Override
    protected BasicMCTSParams _copy() {
        // TAG copies registered values, then calls _reset to update these fields.
        return new BasicMCTSParams();
    }

    @Override
    public IStateHeuristic getStateHeuristic() {
        return heuristic;
    }

    @Override
    public BasicMCTSPlayer instantiate() {
        return new BasicMCTSPlayer((BasicMCTSParams) copy());
    }
}
