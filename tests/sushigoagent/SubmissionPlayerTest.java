package sushigoagent;

import org.junit.Test;
import players.PlayerConstants;
import players.PlayerFactory;

import java.nio.file.Path;

import static org.junit.Assert.*;

public class SubmissionPlayerTest {
    @Test
    public void jsonAndDirectConstructorSelectTheSameDeploymentPolicy() {
        SubmissionPlayer direct = new SubmissionPlayer();
        SubmissionPlayer loaded = (SubmissionPlayer) PlayerFactory.createPlayer(Path.of(
                System.getProperty("coursework.project"), "sushigoagent", "competition.json").toString());
        for (SubmissionPlayer player : new SubmissionPlayer[]{direct, loaded}) {
            BasicMCTSParams p = player.getParameters();
            assertEquals(PlayerConstants.BUDGET_TIME, p.budgetType);
            assertEquals(400, p.budget);
            assertEquals(25, p.breakMS);
            assertEquals(BasicMCTSParams.OpponentModel.PARANOID, p.opponentModel);
            assertTrue(p.heuristic instanceof SushiGoHeuristic);
            assertEquals(7032L, p.getRandomSeed());
            assertEquals(Math.sqrt(2), p.K, 0.0);
            assertEquals(10, p.rolloutLength);
            assertEquals(5, p.maxTreeDepth);
            for (String key : new String[]{"setWeight", "wasabiWeight", "makiWeight", "puddingWeight"})
                assertEquals(1.0, (double) ((SushiGoHeuristic) p.heuristic).getParameterValue(key), 0.0);
        }
    }

    @Test
    public void copyPreservesEntryPointSettingsAndIndependentParameters() {
        SubmissionPlayer original = new SubmissionPlayer(42);
        original.getParameters().setParameterValue("budget", 200);
        original.getParameters().setParameterValue("heuristic.wasabiWeight", 0.5);
        original.setName("copied candidate");
        SubmissionPlayer copy = original.copy();
        assertEquals(original.getClass(), copy.getClass());
        assertNotSame(original.getParameters(), copy.getParameters());
        assertNotSame(original.getParameters().heuristic, copy.getParameters().heuristic);
        assertEquals(200, copy.getParameters().budget);
        assertEquals(42L, copy.getParameters().getRandomSeed());
        assertEquals(original.toString(), copy.toString());
        assertEquals(0.5, (double) ((SushiGoHeuristic) copy.getParameters().heuristic)
                .getParameterValue("wasabiWeight"), 0.0);
        copy.getParameters().setParameterValue("heuristic.wasabiWeight", 0.0);
        assertEquals(0.5, (double) ((SushiGoHeuristic) original.getParameters().heuristic)
                .getParameterValue("wasabiWeight"), 0.0);
    }

    @Test
    public void researchBaselineRemainsUnchanged() {
        BasicMCTSParams p = new BasicMCTSPlayer().getParameters();
        assertEquals(450, p.budget);
        assertFalse(p.heuristic instanceof SushiGoHeuristic);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsSeedThatCannotSurviveTagParameterCopy() {
        new SubmissionPlayer((long) Integer.MAX_VALUE + 1);
    }
}
