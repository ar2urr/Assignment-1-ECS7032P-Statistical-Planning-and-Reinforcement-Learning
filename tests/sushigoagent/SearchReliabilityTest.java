package sushigoagent;

import core.*;
import core.actions.AbstractAction;
import games.GameType;
import games.sushigo.SGGameState;
import games.sushigo.actions.ChooseCard;
import games.sushigo.cards.SGCard;
import org.junit.Test;
import players.PlayerConstants;
import players.PlayerFactory;
import players.simple.RandomPlayer;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

public class SearchReliabilityTest {
    private Game game(int players) {
        Game game = GameType.SushiGo.createGameInstance(players, 7032L);
        List<AbstractPlayer> opponents = new ArrayList<>();
        for (int p = 0; p < players; p++) opponents.add(new RandomPlayer(new Random(p)));
        game.reset(opponents, 7032L);
        game.getGameState().getCoreGameParameters().setParameterValue("competition mode", true);
        return game;
    }

    private BasicMCTSParams params(PlayerConstants type, int limit) {
        BasicMCTSParams params = new BasicMCTSParams();
        params.setParameterValue("budgetType", type);
        params.setParameterValue("budget", limit);
        params.setParameterValue("randomSeed", 7032);
        return params;
    }

    private BasicMCTSPlayer player(Game game, BasicMCTSParams params) {
        BasicMCTSPlayer player = new BasicMCTSPlayer(params);
        player.setPlayerID(0);
        player.setForwardModel(game.getForwardModel());
        return player;
    }

    @Test
    public void zeroBudgetsAndZeroDepthReturnLegalFallbackWithoutSimulation() {
        Game game = game(3);
        List<AbstractAction> actions = game.getForwardModel().computeAvailableActions(game.getGameState());
        for (PlayerConstants type : PlayerConstants.values()) {
            BasicMCTSPlayer player = player(game, params(type, 0));
            assertSame(actions.get(0), player._getAction(game.getGameState(), actions));
            assertEquals(0, player.getLastSearchStats().fmCalls());
            assertEquals(0, player.getLastSearchStats().copyCalls());
        }
        BasicMCTSParams zeroDepth = params(PlayerConstants.BUDGET_ITERATIONS, 100);
        zeroDepth.setParameterValue("maxTreeDepth", 0);
        BasicMCTSPlayer player = player(game, zeroDepth);
        assertSame(actions.get(0), player._getAction(game.getGameState(), actions));
        assertEquals(0, player.getLastSearchStats().iterations());
    }

    @Test
    public void restrictedRootNeverReturnsAnExcludedAction() {
        Game game = game(3);
        List<AbstractAction> actions = game.getForwardModel().computeAvailableActions(game.getGameState());
        List<AbstractAction> restricted = List.of(actions.get(2), actions.get(4));
        BasicMCTSPlayer player = player(game, params(PlayerConstants.BUDGET_ITERATIONS, 150));
        assertTrue(restricted.contains(player._getAction(game.getGameState(), restricted)));
        assertEquals(150, player.getLastSearchStats().iterations());
    }

    @Test
    public void forcedActionDoesNotUseForwardModel() {
        Game game = game(3);
        AbstractAction action = game.getForwardModel().computeAvailableActions(game.getGameState()).get(0);
        BasicMCTSPlayer player = new BasicMCTSPlayer(7032L);
        assertSame(action, player._getAction(game.getGameState(), List.of(action)));
        assertEquals(0, player.getLastSearchStats().iterations());
    }

    @Test
    public void operationBudgetsCountActualForwardModelCallsAndNeverExceedLimits() {
        Game game = game(3);
        for (PlayerConstants type : List.of(PlayerConstants.BUDGET_ITERATIONS,
                PlayerConstants.BUDGET_FM_CALLS, PlayerConstants.BUDGET_COPY_CALLS,
                PlayerConstants.BUDGET_FMANDCOPY_CALLS)) {
            BasicMCTSPlayer player = player(game, params(type, 17));
            CountingModel model = new CountingModel(game.getForwardModel());
            player.setForwardModel(model);
            AbstractGameState state = game.getGameState().copy();
            List<AbstractAction> actions = model.computeAvailableActions(state);
            assertTrue(actions.contains(player._getAction(state, actions)));
            BasicMCTSPlayer.SearchStats stats = player.getLastSearchStats();
            assertEquals(model.advances, stats.fmCalls());
            assertTrue(stats.iterations() > 0);
            switch (type) {
                case BUDGET_ITERATIONS -> assertEquals(17, stats.iterations());
                case BUDGET_FM_CALLS -> assertTrue(stats.fmCalls() <= 17);
                case BUDGET_COPY_CALLS -> assertTrue(stats.copyCalls() <= 17);
                case BUDGET_FMANDCOPY_CALLS -> assertTrue(stats.fmCalls() + stats.copyCalls() <= 17);
                default -> fail();
            }
        }
    }

    @Test
    public void combinedBudgetTooSmallForAnExpansionReturnsFallback() {
        Game game = game(3);
        BasicMCTSPlayer player = player(game, params(PlayerConstants.BUDGET_FMANDCOPY_CALLS, 1));
        List<AbstractAction> actions = game.getForwardModel().computeAvailableActions(game.getGameState());
        assertSame(actions.get(0), player._getAction(game.getGameState(), actions));
        assertEquals(0, player.getLastSearchStats().iterations());
    }

    @Test
    public void heuristicSetterAndSeedSurviveCopy() {
        BasicMCTSPlayer player = new BasicMCTSPlayer(7032L);
        player.setStateHeuristic((state, id) -> 0.125);
        BasicMCTSPlayer copy = player.copy();
        assertNotSame(player.getParameters(), copy.getParameters());
        assertEquals(7032L, copy.getParameters().getRandomSeed());
        assertEquals(0.125, copy.getParameters().getStateHeuristic().evaluateState(null, 0), 0.0);
        assertEquals(player.getRnd().nextLong(), copy.getRnd().nextLong());
    }

    @Test
    public void fixedObservationAndSeedGiveRepeatableIterationSearch() {
        Game game = game(3);
        BasicMCTSParams params = params(PlayerConstants.BUDGET_ITERATIONS, 60);
        params.setParameterValue("rolloutLength", 80);
        BasicMCTSPlayer first = player(game, params);
        BasicMCTSPlayer second = player(game, (BasicMCTSParams) params.copy());
        for (int i = 0; i < 4; i++) {
            AbstractGameState a = game.getGameState().copy();
            AbstractGameState b = game.getGameState().copy();
            AbstractAction firstAction = first._getAction(a, game.getForwardModel().computeAvailableActions(a));
            AbstractAction secondAction = second._getAction(b, game.getForwardModel().computeAvailableActions(b));
            assertEquals(firstAction, secondAction);
            assertEquals(first.getLastSearchStats().fmCalls(), second.getLastSearchStats().fmCalls());
            assertEquals(first.getLastSearchStats().copyCalls(), second.getLastSearchStats().copyCalls());
        }
    }

    @Test
    public void resetSeedEachGameRestartsTheRegisteredStream() {
        Game game = game(3);
        BasicMCTSParams params = params(PlayerConstants.BUDGET_ITERATIONS, 1);
        params.setParameterValue("resetSeedEachGame", true);
        BasicMCTSPlayer player = player(game, params);
        long expected = player.getRnd().nextLong();
        player.getRnd().nextLong();
        player.initializePlayer(game.getGameState());
        assertEquals(expected, player.getRnd().nextLong());
    }

    @Test
    public void chopsticksSearchPreservesActionKeysAndSelectsADifferentSecondCard() {
        Game game = game(3);
        SGGameState state = (SGGameState) game.getGameState();
        state.getPlayedCardTypes(SGCard.SGCardType.Chopsticks, 0).setValue(1);
        state.getPlayedCards().get(0).add(new SGCard(SGCard.SGCardType.Chopsticks));
        BasicMCTSPlayer player = player(game, params(PlayerConstants.BUDGET_ITERATIONS, 30));
        List<AbstractAction> choices = game.getForwardModel().computeAvailableActions(state).stream()
                .filter(a -> ((ChooseCard) a).useChopsticks).toList();
        List<String> before = choices.stream().map(Object::toString).toList();
        ChooseCard first = (ChooseCard) player._getAction(state, choices);
        assertEquals(before, choices.stream().map(Object::toString).toList());
        assertTrue(state.getCardChoices().get(0).isEmpty());
        game.getForwardModel().next(state, first.copy());
        List<AbstractAction> secondChoices = game.getForwardModel().computeAvailableActions(state);
        ChooseCard second = (ChooseCard) player._getAction(state, secondChoices);
        assertNotEquals(first.cardIdx, second.cardIdx);
        assertFalse(second.useChopsticks);
    }

    @Test
    public void monotonicDeadlineStopsCopiesAndAdvancesInsideAnIteration() {
        AtomicLong time = new AtomicLong(1_000_000);
        BasicMCTSParams params = params(PlayerConstants.BUDGET_TIME, 100);
        params.setParameterValue("breakMS", 20);
        SearchBudget budget = new SearchBudget(params, time.get(), time::get);
        assertTrue(budget.canStartIteration());
        assertTrue(budget.reserveCopy());
        time.addAndGet(79_000_000);
        assertTrue(budget.reserveAdvance());
        time.addAndGet(1_000_000);
        assertFalse(budget.reserveAdvance());
        assertFalse(budget.reserveCopy());
        assertFalse(budget.canStartIteration());
        assertEquals(1, budget.copyCalls);
        assertEquals(1, budget.fmCalls);
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeSearchDepthIsRejected() {
        BasicMCTSParams params = params(PlayerConstants.BUDGET_ITERATIONS, 1);
        params.setParameterValue("maxTreeDepth", -1);
        new BasicMCTSPlayer(params);
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyActionListIsRejected() {
        new BasicMCTSPlayer(7032L)._getAction(game(3).getGameState(), List.of());
    }

    @Test(expected = IllegalStateException.class)
    public void nonFiniteHeuristicIsRejected() {
        Game game = game(3);
        BasicMCTSPlayer player = player(game, params(PlayerConstants.BUDGET_ITERATIONS, 1));
        player.setStateHeuristic((state, id) -> Double.POSITIVE_INFINITY);
        player._getAction(game.getGameState(), game.getForwardModel().computeAvailableActions(game.getGameState()));
    }

    @Test(timeout = 15000)
    public void configuredTimingSmokeCheckInBothPlayerCounts() {
        String config = Path.of(System.getProperty("coursework.project"), "sushigoagent", "baseline_500ms.json").toString();
        for (int n : List.of(3, 4)) {
            Game game = game(n);
            BasicMCTSPlayer player = (BasicMCTSPlayer) PlayerFactory.createPlayer(config);
            player.setPlayerID(0);
            player.setForwardModel(game.getForwardModel());
            for (int i = 0; i < 3; i++) {
                AbstractGameState state = game.getGameState().copy();
                List<AbstractAction> actions = game.getForwardModel().computeAvailableActions(state);
                long started = System.nanoTime();
                assertTrue(actions.contains(player._getAction(state, actions)));
                double millis = (System.nanoTime() - started) / 1_000_000.0;
                System.out.printf(Locale.ROOT, "TIMING baseline players=%d sample=%d elapsed_ms=%.3f fm_calls=%d%n",
                        n, i, millis, player.getLastSearchStats().fmCalls());
                assertTrue("Observed decision exceeded 500 ms: " + millis, millis < 500.0);
            }
        }
    }

    private static class CountingModel extends AbstractForwardModel {
        private final AbstractForwardModel delegate;
        long advances;
        CountingModel(AbstractForwardModel delegate) { this.delegate = delegate; }
        @Override protected void _setup(AbstractGameState state) { throw new UnsupportedOperationException(); }
        @Override protected void _next(AbstractGameState state, AbstractAction action) {
            advances++;
            delegate.next(state, action);
        }
        @Override protected List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
            return delegate.computeAvailableActions(state);
        }
        @Override protected void endPlayerTurn(AbstractGameState state) { throw new UnsupportedOperationException(); }
    }
}
