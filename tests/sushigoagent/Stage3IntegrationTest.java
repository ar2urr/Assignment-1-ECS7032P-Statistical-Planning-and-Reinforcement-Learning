package sushigoagent;

import core.*;
import core.actions.AbstractAction;
import games.GameType;
import games.sushigo.SGGameState;
import games.sushigo.cards.SGCard;
import org.junit.Test;
import players.PlayerFactory;
import players.simple.RandomPlayer;

import java.nio.file.Path;
import java.util.*;

import static org.junit.Assert.*;

public class Stage3IntegrationTest {
    private BasicMCTSPlayer configured(String file) {
        return (BasicMCTSPlayer) PlayerFactory.createPlayer(Path.of(
                System.getProperty("coursework.project"), "sushigoagent", file).toString());
    }

    @Test
    public void heuristicCompletesThreeAndFourPlayerGamesInEverySeat() {
        for (int n : List.of(3, 4)) {
            for (int seat = 0; seat < n; seat++) {
                long seed = 703300L + n * 100L + seat;
                CheckingPlayer heuristic = new CheckingPlayer(configured("heuristic_iterations.json").getParameters());
                List<AbstractPlayer> players = new ArrayList<>();
                for (int p = 0; p < n; p++) players.add(p == seat ? heuristic
                        : new RandomPlayer(new Random(seed + p)));
                Game game = GameType.SushiGo.createGameInstance(n, seed);
                game.reset(players, seed);
                game.run();
                assertFalse(game.getGameState().isNotTerminal());
                assertTrue(heuristic.decisions > 0);
                System.out.printf(Locale.ROOT,
                        "SMOKE heuristic players=%d seat=%d seed=%d decisions=%d result=%s score=%.1f%n",
                        n, seat, seed, heuristic.decisions, game.getGameState().getPlayerResults()[seat],
                        game.getGameState().getGameScore(seat));
            }
        }
    }

    @Test(timeout = 15000)
    public void heuristicTimingSmokeCheckInBothPlayerCounts() {
        for (int n : List.of(3, 4)) {
            List<AbstractPlayer> players = new ArrayList<>();
            for (int p = 0; p < n; p++) players.add(new RandomPlayer(new Random(p)));
            Game game = GameType.SushiGo.createGameInstance(n, 7032L);
            game.reset(players, 7032L);
            game.getGameState().getCoreGameParameters().setParameterValue("competition mode", true);
            BasicMCTSPlayer heuristic = configured("heuristic_500ms.json");
            heuristic.setPlayerID(0);
            heuristic.setForwardModel(game.getForwardModel());
            for (int sample = 0; sample < 3; sample++) {
                AbstractGameState observation = game.getGameState().copy();
                List<AbstractAction> actions = game.getForwardModel().computeAvailableActions(observation);
                long started = System.nanoTime();
                assertTrue(actions.contains(heuristic._getAction(observation, actions)));
                double millis = (System.nanoTime() - started) / 1_000_000.0;
                System.out.printf(Locale.ROOT, "TIMING heuristic players=%d sample=%d elapsed_ms=%.3f fm_calls=%d%n",
                        n, sample, millis, heuristic.getLastSearchStats().fmCalls());
                assertTrue("Observed decision exceeded 500 ms: " + millis, millis < 500.0);
            }
        }
    }

    private static class CheckingPlayer extends BasicMCTSPlayer {
        int decisions;
        CheckingPlayer(BasicMCTSParams params) { super(params); }
        @Override public AbstractAction _getAction(AbstractGameState observation, List<AbstractAction> actions) {
            String before = snapshot((SGGameState) observation);
            AbstractAction selected = super._getAction(observation, actions);
            assertTrue(actions.contains(selected));
            assertEquals(before, snapshot((SGGameState) observation));
            assertTrue(Double.isFinite(getParameters().heuristic.evaluateState(observation, getPlayerID())));
            decisions++;
            return selected;
        }
        private String snapshot(SGGameState state) {
            StringBuilder value = new StringBuilder();
            value.append(state.getCurrentPlayer()).append(':').append(state.getRoundCounter())
                    .append(':').append(state.getTurnCounter()).append(':').append(state.getGameStatus())
                    .append(':').append(state.getCardChoices()).append(':').append(state.isActionInProgress());
            for (int p = 0; p < state.getNPlayers(); p++) {
                value.append('|').append(state.getGameScore(p)).append(':').append(state.getPlayerResults()[p]);
                for (var deck : List.of(state.getPlayerHands().get(p), state.getPlayedCards().get(p))) {
                    value.append('/');
                    for (SGCard card : deck.getComponents())
                        value.append(card.getComponentID()).append('=').append(card).append(',');
                }
                for (SGCard.SGCardType type : SGCard.SGCardType.values())
                    value.append(type).append('=').append(state.getPlayedCardTypes(type, p).getValue())
                            .append(',').append(state.getPlayedCardTypesAllGame()[p].get(type).getValue())
                            .append(',').append(state.getPointsPerCardType()[p].get(type).getValue());
            }
            return value.toString();
        }
    }
}
