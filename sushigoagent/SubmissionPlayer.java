package sushigoagent;

import players.PlayerConstants;

/**
 * Submission entry point: the Stage 5 heuristic with PARANOID selection.
 * The research configurations remain unchanged for reproducibility.
 */
public final class SubmissionPlayer extends BasicMCTSPlayer {
    public SubmissionPlayer() {
        this(7032L);
    }

    /** Explicit seed constructor; TAG stores seeds as signed 32-bit integers. */
    public SubmissionPlayer(long seed) {
        this(submissionParameters(seed));
    }

    private SubmissionPlayer(BasicMCTSParams parameters) {
        super(parameters);
        setName("Sushi Go heuristic MCTS");
    }

    private static BasicMCTSParams submissionParameters(long seed) {
        if (seed < Integer.MIN_VALUE || seed > Integer.MAX_VALUE)
            throw new IllegalArgumentException("randomSeed must fit a signed 32-bit integer");
        BasicMCTSParams parameters = new BasicMCTSParams();
        parameters.setParameterValue("randomSeed", (int) seed);
        parameters.setParameterValue("heuristic", new SushiGoHeuristic());
        parameters.setParameterValue("opponentModel", BasicMCTSParams.OpponentModel.PARANOID);
        parameters.setParameterValue("budgetType", PlayerConstants.BUDGET_TIME);
        // Nominal 375 ms cutoff leaves 125 ms against the stricter 500 ms specification.
        // Framework calls and JVM/OS pauses remain non-interruptible.
        parameters.setParameterValue("budget", 400);
        parameters.setParameterValue("breakMS", 25);
        return parameters;
    }

    @Override
    public SubmissionPlayer copy() {
        SubmissionPlayer copy = new SubmissionPlayer((BasicMCTSParams) getParameters().copy());
        copy.setName(toString());
        return copy;
    }
}
