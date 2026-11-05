package sushigoagent;

import core.*;
import core.actions.AbstractAction;
import core.components.Component;
import games.GameType;
import games.sushigo.SGParameters;
import org.junit.Test;
import players.PlayerConstants;
import players.PlayerFactory;
import players.simple.RandomPlayer;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;
import static sushigoagent.BasicMCTSParams.OpponentModel.*;

public class MultiplayerSearchTest {
    private BasicMCTSPlayer player(ToyState state, BasicMCTSParams.OpponentModel mode, int seed) {
        BasicMCTSParams params = new BasicMCTSParams();
        params.setParameterValue("budgetType", PlayerConstants.BUDGET_ITERATIONS);
        params.setParameterValue("budget", 2000);
        params.setParameterValue("randomSeed", seed);
        params.setParameterValue("K", Math.sqrt(2));
        params.setParameterValue("rolloutLength", 2);
        params.setParameterValue("maxTreeDepth", 3);
        params.setParameterValue("opponentModel", mode);
        BasicMCTSPlayer player = params.instantiate();
        player.setPlayerID(state.rootId);
        player.setForwardModel(new ToyModel());
        return player;
    }

    private ToyState state(int n, int root, boolean repeatedTurn) {
        ToyState state = new ToyState(n, root, repeatedTurn);
        new ToyModel().setup(state);
        return state;
    }

    @Test
    public void maxNModelsSelfInterestWhileParanoidModelsHostilityAtEveryRootSeat() {
        // In branch 0, the opponent chooses between (.8,.9) and (-.8,.2), root first.
        // Branch 1 gives the root .1, so the two opponent models favour different branches.
        for (int n : List.of(3, 4)) for (int root = 0; root < n; root++) {
            for (int seed : List.of(7, 88)) for (var mode : BasicMCTSParams.OpponentModel.values()) {
                ToyState state = state(n, root, false);
                BasicMCTSPlayer player = player(state, mode, seed);
                Choice result = (Choice) player._getAction(state,
                        player.getForwardModel().computeAvailableActions(state));
                assertEquals("n=" + n + " root=" + root + " mode=" + mode,
                        mode == MAXN ? 0 : 1, result.id);
                assertEquals(2000, player.getLastSearchStats().iterations());
                assertEquals(0, state.stage);
            }
        }
    }

    @Test
    public void repeatedTurnsUseTheActualMoverInsteadOfAlternatingByDepth() {
        for (var mode : BasicMCTSParams.OpponentModel.values()) {
            ToyState state = state(4, 2, true);
            BasicMCTSPlayer player = player(state, mode, 7032);
            Choice choice = (Choice) player._getAction(state,
                    player.getForwardModel().computeAvailableActions(state));
            assertEquals(0, choice.id);
        }
    }

    @Test
    public void modelConfigurationSurvivesCopyAndDefaultsToParanoid() {
        assertEquals(PARANOID, new BasicMCTSParams().opponentModel);
        for (String name : List.of("multiplayer", "combined")) {
            BasicMCTSPlayer player = configured(name + "_iterations.json");
            assertEquals(MAXN, player.getParameters().opponentModel);
            assertEquals(MAXN, player.copy().getParameters().opponentModel);
            if (name.equals("combined")) assertTrue(player.getParameters().heuristic instanceof SushiGoHeuristic);
        }
    }

    @Test(expected = IllegalStateException.class)
    public void maxNRejectsNonFiniteOpponentValues() {
        ToyState state = state(3, 0, false);
        BasicMCTSPlayer player = player(state, MAXN, 7032);
        player.setStateHeuristic((s, p) -> p == 0 ? 0.0 : Double.NaN);
        player._getAction(state, player.getForwardModel().computeAvailableActions(state));
    }

    @Test
    public void expiredDeadlineDoesNotBackUpAnIncompleteValueVector() {
        ToyState state = state(3, 0, false);
        BasicMCTSPlayer player = player(state, MAXN, 7032);
        player.getParameters().setParameterValue("budgetType", PlayerConstants.BUDGET_TIME);
        player.getParameters().setParameterValue("budget", 100);
        player.getParameters().setParameterValue("breakMS", 0);
        AtomicLong time = new AtomicLong();
        player.setStateHeuristic((s, p) -> { time.addAndGet(101_000_000); return 0.5; });
        SearchBudget budget = new SearchBudget(player.getParameters(), 0, time::get);
        List<AbstractAction> actions = player.getForwardModel().computeAvailableActions(state);
        BasicTreeNode tree = new BasicTreeNode(player, null, state, new Random(7032), actions, budget);
        tree.mctsSearch();
        assertEquals(0, budget.iterations);
        assertSame(actions.get(0), tree.bestAction());
    }

    @Test
    public void multiplayerVariantsCompleteGamesAcrossAllSeats() {
        for (String name : List.of("multiplayer", "combined")) for (int n : List.of(3, 4)) {
            for (int seat = 0; seat < n; seat++) {
                long seed = 704000L + n * 100 + seat;
                BasicMCTSPlayer focal = configured(name + "_iterations.json");
                List<AbstractPlayer> players = new ArrayList<>();
                for (int p = 0; p < n; p++) players.add(p == seat ? focal : new RandomPlayer(new Random(seed + p)));
                Game game = GameType.SushiGo.createGameInstance(n, seed);
                game.getGameState().getCoreGameParameters().setParameterValue("competition mode", true);
                game.reset(players, seed);
                game.run();
                assertFalse(game.getGameState().isNotTerminal());
                assertNotEquals(CoreConstants.GameResult.DISQUALIFY, game.getGameState().getPlayerResults()[seat]);
                assertTrue(Double.isFinite(game.getGameState().getGameScore(seat)));
                System.out.printf(Locale.ROOT, "SMOKE %s players=%d seat=%d score=%.1f%n",
                        name, n, seat, game.getGameState().getGameScore(seat));
            }
        }
    }

    @Test
    public void maxNPreservesSeededSearchAndCombinedOperationLimits() {
        ToyState state = state(3, 1, false);
        BasicMCTSPlayer a = player(state, MAXN, 7032);
        a.getParameters().setParameterValue("budgetType", PlayerConstants.BUDGET_FMANDCOPY_CALLS);
        a.getParameters().setParameterValue("budget", 30);
        BasicMCTSPlayer b = a.copy();
        b.setForwardModel(a.getForwardModel());
        b.setPlayerID(1);
        List<AbstractAction> actions = a.getForwardModel().computeAvailableActions(state);
        assertEquals(a._getAction(state, actions), b._getAction(state, actions));
        assertTrue(a.getLastSearchStats().copyCalls() + a.getLastSearchStats().fmCalls() <= 30);
        assertEquals(a.getLastSearchStats().iterations(), b.getLastSearchStats().iterations());
    }

    private BasicMCTSPlayer configured(String name) {
        return (BasicMCTSPlayer) PlayerFactory.createPlayer(Path.of(
                System.getProperty("coursework.project"), "sushigoagent", name).toString());
    }

    private static final class ToyState extends AbstractGameState {
        final int rootId;
        final boolean repeatedTurn;
        int stage;
        double[] payoff;
        ToyState(int n, int rootId, boolean repeatedTurn) {
            super(new SGParameters(), n);
            this.rootId = rootId;
            this.repeatedTurn = repeatedTurn;
            this.payoff = new double[n];
        }
        @Override protected GameType _getGameType() { return GameType.SushiGo; }
        @Override protected List<Component> _getAllComponents() { return List.of(); }
        @Override protected AbstractGameState _copy(int playerId) {
            ToyState copy = new ToyState(getNPlayers(), rootId, repeatedTurn);
            copy.stage = stage;
            copy.payoff = payoff.clone();
            return copy;
        }
        @Override protected double _getHeuristicScore(int p) { return payoff[p]; }
        @Override public double getGameScore(int p) { return payoff[p]; }
        @Override protected boolean _equals(Object o) {
            return o instanceof ToyState t && stage == t.stage && Arrays.equals(payoff, t.payoff);
        }
    }

    private static final class ToyModel extends AbstractForwardModel {
        @Override protected void _setup(AbstractGameState gs) {
            ToyState state = (ToyState) gs;
            state.stage = 0;
            state.setTurnOwner(state.rootId);
        }
        @Override protected void _next(AbstractGameState gs, AbstractAction action) { action.execute(gs); }
        @Override protected List<AbstractAction> _computeAvailableActions(AbstractGameState gs) {
            return gs.isNotTerminal() ? List.of(new Choice(0), new Choice(1)) : List.of();
        }
        @Override protected void endPlayerTurn(AbstractGameState state) { throw new UnsupportedOperationException(); }
    }

    private static final class Choice extends AbstractAction {
        final int id;
        Choice(int id) { this.id = id; }
        @Override public boolean execute(AbstractGameState gs) {
            ToyState state = (ToyState) gs;
            int opponent = (state.rootId + 1) % state.getNPlayers();
            if (state.stage == 0 && id == 0) {
                state.stage = 1;
                state.setTurnOwner(state.repeatedTurn ? state.rootId : opponent);
            } else {
                Arrays.fill(state.payoff, state.stage == 0 ? 0.0 : id == 0 ? 0.1 : 0.9);
                state.payoff[state.rootId] = state.stage == 0 ? 0.1 : id == 0 ? 0.8 : -0.8;
                state.payoff[opponent] = state.stage == 0 ? 0.6 : id == 0 ? 0.9 : 0.2;
                state.stage = 2;
                state.setGameStatus(CoreConstants.GameResult.GAME_END);
            }
            return true;
        }
        @Override public AbstractAction copy() { return this; }
        @Override public boolean equals(Object o) { return o instanceof Choice c && c.id == id; }
        @Override public int hashCode() { return id; }
        @Override public String getString(AbstractGameState state) { return "choice " + id; }
    }
}
