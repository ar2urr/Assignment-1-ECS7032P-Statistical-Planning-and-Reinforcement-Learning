package sushigoagent;

import players.PlayerConstants;

/**
 * Competition entry point using the Stage 5 heuristic and PARANOID selection.
 * Deployment defaults are kept here so the study configurations stay unchanged.
 */
public final class SubmissionPlayer extends BasicMCTSPlayer {
    public SubmissionPlayer() {
        this(7032L);
    }

    /** Creates a player with a seed in the signed 32-bit range used by TAG. */
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
        // Aim to stop search at 375 ms, leaving 125 ms before the 500 ms limit.
        // A running framework call or JVM/OS pause can still delay the return.
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
