package coursework.validation;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import games.GameType;
import games.sushigo.SGGameState;
import games.sushigo.cards.SGCard;
import sushigoagent.BasicMCTSParams;
import sushigoagent.BasicMCTSPlayer;
import org.junit.Test;
import players.PlayerConstants;
import players.PlayerFactory;
import players.simple.RandomPlayer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

/** Checks JSON loading, copying and complete games for the Stage 1 baseline. */
public class Stage1SmokeTest {
    private String configPath(String name) {
        return Path.of(System.getProperty("coursework.project"), "sushigoagent", name).toString();
    }

    @Test
    public void jsonLoadsAndRegisteredParametersSurviveCopy() {
        BasicMCTSPlayer player = (BasicMCTSPlayer) PlayerFactory.createPlayer(
                configPath("baseline_iterations.json"));
        BasicMCTSParams params = player.getParameters();
        assertEquals(PlayerConstants.BUDGET_ITERATIONS, params.budgetType);
        assertEquals(150, params.budget);
        assertEquals(5, params.maxTreeDepth);
        assertEquals(10, params.rolloutLength);
        assertEquals(Math.sqrt(2), params.K, 1e-12);
        BasicMCTSPlayer copy = player.copy();
        assertNotSame(player.parameters, copy.parameters);
        assertEquals(params.budget, copy.getParameters().budget);
        assertEquals(params.maxTreeDepth, copy.getParameters().maxTreeDepth);

        BasicMCTSPlayer timed = (BasicMCTSPlayer) PlayerFactory.createPlayer(
                configPath("baseline_500ms.json"));
        assertEquals(PlayerConstants.BUDGET_TIME, timed.getParameters().budgetType);
        assertEquals(450, timed.getParameters().budget);
        assertEquals(25, timed.getParameters().breakMS);
    }

    @Test
    public void threePlayerGamesFinishWithBaselineInEverySeat() {
        checkGames(3);
    }

    @Test
    public void fourPlayerGamesFinishWithBaselineInEverySeat() {
        checkGames(4);
    }

    private void checkGames(int nPlayers) {
        for (int seat = 0; seat < nPlayers; seat++) {
            long gameSeed = 703200L + nPlayers * 100L + seat;
            BasicMCTSPlayer loaded = (BasicMCTSPlayer) PlayerFactory.createPlayer(
                    configPath("baseline_iterations.json"));
            CheckedPlayer baseline = new CheckedPlayer(loaded.getParameters());
            List<AbstractPlayer> players = new ArrayList<>();
            for (int p = 0; p < nPlayers; p++) {
                players.add(p == seat ? baseline : new RandomPlayer(new Random(gameSeed + p)));
            }
            Game game = GameType.SushiGo.createGameInstance(nPlayers, gameSeed);
            game.reset(players, gameSeed);
            game.run();
            assertFalse(game.getGameState().isNotTerminal());
            assertTrue("Expected searched decisions", baseline.decisions > 0);
            for (int p = 0; p < nPlayers; p++) {
                assertTrue(Double.isFinite(game.getGameState().getGameScore(p)));
            }
            System.out.printf("SMOKE players=%d seat=%d seed=%d decisions=%d result=%s score=%.1f%n",
                    nPlayers, seat, gameSeed, baseline.decisions,
                    game.getGameState().getPlayerResults()[seat],
                    game.getGameState().getGameScore(seat));
        }
    }

    private static class CheckedPlayer extends BasicMCTSPlayer {
        int decisions;

        CheckedPlayer(BasicMCTSParams params) {
            super(params);
        }

        @Override
        public AbstractAction _getAction(AbstractGameState observation, List<AbstractAction> actions) {
            String before = observableSnapshot((SGGameState) observation);
            AbstractAction selected = super._getAction(observation, actions);
            assertTrue("Returned action must belong to the supplied legal action list", actions.contains(selected));
            assertEquals("Search must preserve the observed game fields", before,
                    observableSnapshot((SGGameState) observation));
            decisions++;
            return selected;
        }

        // In this TAG revision, SGGameState._equals recurses through super.equals.
        // Compare public game fields instead, without patching the framework.
        // Private decks and RNG state are outside this check.
        private String observableSnapshot(SGGameState state) {
            StringBuilder snapshot = new StringBuilder();
            snapshot.append(state.getCurrentPlayer()).append('|')
                    .append(state.getRoundCounter()).append('|')
                    .append(state.getTurnCounter()).append('|')
                    .append(state.getGameStatus()).append('|')
                    .append(state.isActionInProgress()).append('|')
                    .append(state.getCardChoices()).append('|');
            for (int p = 0; p < state.getNPlayers(); p++) {
                snapshot.append(p).append(':').append(state.getGameScore(p)).append(':')
                        .append(state.getPlayerResults()[p]).append(':');
                for (SGCard card : state.getPlayerHands().get(p).getComponents()) {
                    snapshot.append(card.getComponentID()).append('=').append(card).append(',');
                }
                snapshot.append('/');
                for (SGCard card : state.getPlayedCards().get(p).getComponents()) {
                    snapshot.append(card.getComponentID()).append('=').append(card).append(',');
                }
                for (SGCard.SGCardType type : SGCard.SGCardType.values()) {
                    snapshot.append(type).append('=').append(state.getPlayedCardTypes(type, p).getValue())
                            .append(',').append(state.getPlayedCardTypesAllGame()[p].get(type).getValue())
                            .append(',').append(state.getPointsPerCardType()[p].get(type).getValue()).append(';');
                }
                snapshot.append('|');
            }
            return snapshot.toString();
        }
    }
}
